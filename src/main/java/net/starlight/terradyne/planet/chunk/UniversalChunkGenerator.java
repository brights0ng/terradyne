package net.starlight.terradyne.planet.chunk;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.noise.NoiseConfig;

import net.starlight.terradyne.Terradyne;
import net.starlight.terradyne.planet.dimension.PlanetDimensionManager;
import net.starlight.terradyne.planet.physics.PlanetModel;
import net.starlight.terradyne.planet.terrain.TerrainHeightMapper;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Universal Chunk Generator with Physics-Based Generation
 * Updated for 0-256 height range (pre-1.18 style)
 */
public class UniversalChunkGenerator extends ChunkGenerator {
    
    // Updated height constraints for all Terradyne dimensions
    private static final int MIN_WORLD_Y = 0;
    private static final int MAX_WORLD_Y = 255;
    private static final int WORLD_HEIGHT = 256;
    
    public static final Codec<UniversalChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource),
                    Codec.STRING.optionalFieldOf("planet_name", "").forGetter(generator -> 
                        generator.planetModel != null ? generator.planetModel.getConfig().getPlanetName() : "")
            ).apply(instance, UniversalChunkGenerator::fromCodec)
    );

    private final PlanetModel planetModel;
    private final String planetName;

    /**
     * Constructor for direct creation (used by PlanetDimensionManager)
     */
    public UniversalChunkGenerator(PlanetModel planetModel, BiomeSource biomeSource) {
        super(biomeSource);
        this.planetModel = planetModel;
        this.planetName = planetModel != null ? planetModel.getConfig().getPlanetName() : "";

        if (planetModel != null) {
            Terradyne.LOGGER.info("=== UNIVERSAL CHUNK GENERATOR INITIALIZED ===");
            Terradyne.LOGGER.info("Planet: {}", planetModel.getConfig().getPlanetName());
            Terradyne.LOGGER.info("Classification: {}", planetModel.getPlanetClassification());
            Terradyne.LOGGER.info("Generation: PHYSICS-BASED");
            Terradyne.LOGGER.info("Height Range: Y {} to {}", MIN_WORLD_Y, MAX_WORLD_Y);
            Terradyne.LOGGER.info("Noise System: {}", planetModel.getNoiseSystem().getSystemStatus());
        } else {
            Terradyne.LOGGER.warn("UniversalChunkGenerator created without planet model");
        }
    }

    /**
     * Constructor for codec deserialization
     */
    private static UniversalChunkGenerator fromCodec(BiomeSource biomeSource, String planetName) {
        // Look up planet model by name when loading from codec
        PlanetModel planetModel = null;
        if (!planetName.isEmpty()) {
            planetModel = PlanetDimensionManager.getPlanetModel(planetName);
            if (planetModel == null) {
                Terradyne.LOGGER.warn("Failed to find planet model for '{}' during codec deserialization", planetName);
            }
        }
        
        return new UniversalChunkGenerator(planetModel, biomeSource);
    }

    @Override
    protected Codec<? extends ChunkGenerator> getCodec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<Chunk> populateNoise(Executor executor, Blender blender,
                                                  NoiseConfig noiseConfig, StructureAccessor structureAccessor,
                                                  Chunk chunk) {
        return CompletableFuture.supplyAsync(() -> {
            generateTerrain(chunk, noiseConfig);
            return chunk;
        }, executor);
    }

    /**
     * Main terrain generation method - uses physics system
     */
    private void generateTerrain(Chunk chunk, NoiseConfig noiseConfig) {
        if (planetModel == null) {
            generateFallbackTerrain(chunk);
            return;
        }

        try {
            ChunkPos chunkPos = chunk.getPos();
            
            Terradyne.LOGGER.debug("Generating physics-based terrain for chunk {} on planet {}", 
                                 chunkPos, planetModel.getConfig().getPlanetName());

            // Generate terrain using physics system
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    int worldX = chunkPos.getStartX() + x;
                    int worldZ = chunkPos.getStartZ() + z;

                    // Generate complete terrain column using physics - adapted for 0-256 range
                    generateTerrainColumn(chunk, x, z, worldX, worldZ);
                }
            }

            Terradyne.LOGGER.debug("✅ Physics-based generation completed for chunk {}", chunkPos);

        } catch (Exception e) {
            Terradyne.LOGGER.error("Critical error in physics-based terrain generation for chunk {}: {}",
                    chunk.getPos(), e.getMessage(), e);
            generateFallbackTerrain(chunk);
        }
    }

    /**
     * Generate terrain column adapted for 0-256 height range
     */
    private void generateTerrainColumn(Chunk chunk, int x, int z, int worldX, int worldZ) {
        // Sample terrain and environmental conditions from physics system
        double terrainHeight = planetModel.getTerrainHeight(worldX, worldZ);
        double temperature = planetModel.getTemperature(worldX, worldZ);
        double moisture = planetModel.getMoisture(worldX, worldZ);
        
        // Clamp terrain height to our 0-256 range
        int surfaceY = Math.max(MIN_WORLD_Y, Math.min(MAX_WORLD_Y, (int) terrainHeight));
        
        // Adjust sea level for 0-256 range
        int seaLevel = Math.max(MIN_WORLD_Y, Math.min(MAX_WORLD_Y, planetModel.getPlanetData().getSeaLevel()));
        
        // Generate column from bottom to top
        for (int y = MIN_WORLD_Y; y <= MAX_WORLD_Y; y++) {
            BlockPos pos = new BlockPos(x, y, z);
            
            if (y <= surfaceY) {
                // Use physics system to determine block type
                BlockState blockState = planetModel.getTerrainBlockState(worldX, worldZ, y);
                chunk.setBlockState(pos, blockState, false);
            } else if (y <= seaLevel && planetModel.getPlanetData().hasLiquidWater()) {
                // Fill with water up to sea level
                chunk.setBlockState(pos, Blocks.WATER.getDefaultState(), false);
            } else {
                // Air above surface/water
                chunk.setBlockState(pos, Blocks.AIR.getDefaultState(), false);
            }
        }
    }

    /**
     * Fallback terrain generation if physics system fails or is unavailable
     */
    private void generateFallbackTerrain(Chunk chunk) {
        Terradyne.LOGGER.warn("Using fallback terrain generation for chunk {} (planet model unavailable)", 
                            chunk.getPos());

        // Simple flat terrain as emergency fallback - adapted for 0-256 range
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = MIN_WORLD_Y; y <= 150; y++) { // Up to Y=150 for fallback
                    BlockPos pos = new BlockPos(x, y, z);

                    if (y <= 50) {
                        chunk.setBlockState(pos, Blocks.DEEPSLATE.getDefaultState(), false);
                    } else if (y <= 120) {
                        chunk.setBlockState(pos, Blocks.STONE.getDefaultState(), false);
                    } else if (y <= 150) {
                        chunk.setBlockState(pos, Blocks.DIRT.getDefaultState(), false);
                    }
                }
            }
        }
    }

    // ============================================================================
    // REQUIRED CHUNK GENERATOR METHODS - Updated for 0-256 height
    // ============================================================================

    @Override
    public void carve(ChunkRegion chunkRegion, long seed, NoiseConfig noiseConfig, BiomeAccess biomeAccess, 
                     StructureAccessor structureAccessor, Chunk chunk, GenerationStep.Carver carverStep) {
        // Carving disabled - physics system handles erosion through noise
        // TODO: Implement cave/cavern generation in future phases
    }

    @Override
    public void buildSurface(ChunkRegion region, StructureAccessor structures, NoiseConfig noiseConfig, Chunk chunk) {
        // Surface building handled by physics system in terrain generation
        // The physics system already places appropriate surface blocks
    }

    @Override
    public void populateEntities(ChunkRegion region) {
        // Default entity population - no custom entity spawning yet
    }

    @Override
    public int getWorldHeight() { 
        return WORLD_HEIGHT; // 256 blocks tall
    }

    @Override
    public int getSeaLevel() {
        if (planetModel != null) {
            // Clamp sea level to our height range
            return Math.max(MIN_WORLD_Y, Math.min(MAX_WORLD_Y, planetModel.getPlanetData().getSeaLevel()));
        }
        return 63; // Minecraft default fallback (adjusted for 0-256)
    }

    @Override
    public int getMinimumY() { 
        return MIN_WORLD_Y; // Y=0
    }

    @Override
    public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
        if (planetModel == null) {
            return 120; // Fallback height for 0-256 range
        }

        double terrainHeight = planetModel.getTerrainHeight(x, z);
        return Math.max(MIN_WORLD_Y, Math.min(MAX_WORLD_Y, (int) terrainHeight));
    }

    @Override
    public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
        if (planetModel == null) {
            // Fallback column for 0-256 range
            BlockState[] column = new BlockState[WORLD_HEIGHT];
            for (int y = 0; y < WORLD_HEIGHT; y++) {
                int worldY = MIN_WORLD_Y + y;
                if (worldY <= 120) {
                    column[y] = Blocks.STONE.getDefaultState();
                } else {
                    column[y] = Blocks.AIR.getDefaultState();
                }
            }
            return new VerticalBlockSample(MIN_WORLD_Y, column);
        }

        // Generate physics-based column for 0-256 range
        BlockState[] column = new BlockState[WORLD_HEIGHT];
        double terrainHeight = planetModel.getTerrainHeight(x, z);
        int surfaceY = Math.max(MIN_WORLD_Y, Math.min(MAX_WORLD_Y, (int) terrainHeight));
        int seaLevel = Math.max(MIN_WORLD_Y, Math.min(MAX_WORLD_Y, planetModel.getPlanetData().getSeaLevel()));
        
        for (int y = 0; y < WORLD_HEIGHT; y++) {
            int worldY = MIN_WORLD_Y + y;
            
            if (worldY <= surfaceY) {
                column[y] = planetModel.getTerrainBlockState(x, z, worldY);
            } else if (worldY <= seaLevel && planetModel.getPlanetData().hasLiquidWater()) {
                column[y] = Blocks.WATER.getDefaultState();
            } else {
                column[y] = Blocks.AIR.getDefaultState();
            }
        }

        return new VerticalBlockSample(MIN_WORLD_Y, column);
    }

    @Override
    public void getDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {
        text.add("=== TERRADYNE PHYSICS-BASED GENERATION ===");
        text.add("Height Range: Y " + MIN_WORLD_Y + " to " + MAX_WORLD_Y);
        
        if (planetModel != null) {
            text.add("Planet: " + planetModel.getConfig().getPlanetName());
            text.add("Classification: " + planetModel.getPlanetClassification());
            text.add("Physics Status: " + (planetModel.isValid() ? "ACTIVE" : "ERROR"));
            
            text.add("");
            text.add("=== TERRAIN ANALYSIS ===");
            try {
                text.add(planetModel.analyzeTerrainAt(pos.getX(), pos.getZ()));
            } catch (Exception e) {
                text.add("Error analyzing terrain: " + e.getMessage());
            }
            
            text.add("");
            text.add("=== NOISE SAMPLING ===");
            try {
                text.add(planetModel.sampleAllNoiseAt(pos.getX(), pos.getZ()));
            } catch (Exception e) {
                text.add("Error sampling noise: " + e.getMessage());
            }
            
            text.add("");
            text.add("=== PHYSICS DATA ===");
            try {
                text.add("Temperature: " + String.format("%.1f°C", planetModel.getTemperature(pos.getX(), pos.getZ())));
                text.add("Moisture: " + String.format("%.2f", planetModel.getMoisture(pos.getX(), pos.getZ())));
                text.add("Tectonic Activity: " + String.format("%.2f", planetModel.getTectonicActivity(pos.getX(), pos.getZ())));
                text.add("Sea Level: Y=" + getSeaLevel());
                text.add("Habitability: " + String.format("%.2f", planetModel.getPlanetData().getHabitability()));
            } catch (Exception e) {
                text.add("Error reading physics data: " + e.getMessage());
            }
            
        } else {
            text.add("No planet model loaded");
            text.add("Planet Name: " + planetName);
            text.add("Status: FALLBACK GENERATION");
            text.add("");
            text.add("This may indicate:");
            text.add("- Planet not properly registered");
            text.add("- Codec deserialization failed");
            text.add("- World loaded before planet creation");
        }
    }

    // === ACCESSORS ===

    /**
     * Get the planet model (may be null if unavailable)
     */
    public PlanetModel getPlanetModel() {
        return planetModel;
    }

    /**
     * Get the planet name
     */
    public String getPlanetName() {
        return planetName;
    }

    /**
     * Check if physics-based generation is available
     */
    public boolean hasPhysicsGeneration() {
        return planetModel != null && planetModel.isValid();
    }
}