package net.starlight.terradyne.planet.terrain;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
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
import net.starlight.terradyne.planet.physics.PlanetModelRegistry;
import net.starlight.terradyne.planet.biome.PhysicsBasedBiomeSource;
import net.starlight.terradyne.planet.physics.PlanetConfig;
import net.starlight.terradyne.planet.physics.PlanetModel;
import net.starlight.terradyne.starsystem.StarSystemModel;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static net.starlight.terradyne.Terradyne.server;

/**
 * Universal Chunk Generator with Physics-Based Generation
 * UPDATED: Now gets PlanetModel from server context via WorldPlanetManager
 */
public class UniversalChunkGenerator extends ChunkGenerator {

    // Updated height constraints for all Terradyne dimensions
    private static final int MIN_WORLD_Y = 0;
    private static final int MAX_WORLD_Y = 255;
    private static final int WORLD_HEIGHT = 256;

    public static final Codec<UniversalChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource),
                    PlanetConfig.CODEC.fieldOf("planet_config").forGetter(generator -> generator.planetModel.getConfig())
            ).apply(instance, UniversalChunkGenerator::fromCodec)
    );

    private final Identifier planetId; // Derived from config
    private PlanetModel planetModel; // Created from embedded config


    /**
     * Constructor for direct creation (used by WorldPlanetManager)
     */
    public UniversalChunkGenerator(PlanetModel planetModel, BiomeSource biomeSource, Identifier planetId) {
        super(biomeSource);
        this.planetModel = planetModel;
        this.planetId = planetId != null ? planetId : 
            (planetModel != null ? new Identifier("terradyne", planetModel.getConfig().getPlanetName().toLowerCase()) : null);

        if (planetModel != null) {
            Terradyne.LOGGER.info("=== UNIVERSAL CHUNK GENERATOR INITIALIZED (DIRECT) ===");
            Terradyne.LOGGER.info("Planet: {} ({})", planetId, planetModel.getConfig().getPlanetName());
            Terradyne.LOGGER.info("Classification: {}", planetModel.getPlanetClassification());
            Terradyne.LOGGER.info("Generation: PHYSICS-BASED");
            Terradyne.LOGGER.info("Height Range: Y {} to {}", MIN_WORLD_Y, MAX_WORLD_Y);
            Terradyne.LOGGER.info("Noise System: {}", planetModel.getNoiseSystem().getSystemStatus());
        } else {
            Terradyne.LOGGER.warn("UniversalChunkGenerator created without planet model");
        }
    }


    /**
     * Constructor for codec deserialization - NOW WITH EMBEDDED CONFIG
     * PlanetConfig is embedded directly in the dimension JSON!
     * No external registries, no timing issues - everything just works!
     */
    private static UniversalChunkGenerator fromCodec(BiomeSource biomeSource, PlanetConfig planetConfig) {
        Terradyne.LOGGER.info("=== DESERIALIZING CHUNK GENERATOR WITH EMBEDDED CONFIG ===");
        Terradyne.LOGGER.info("Planet: {}", planetConfig.getPlanetName());
        
        try {
            // Create PlanetModel directly from embedded config
            PlanetModel planetModel = new PlanetModel(planetConfig);
            
            Terradyne.LOGGER.info("✅ Created PlanetModel: {} ({})", 
                planetModel.getConfig().getPlanetName(), 
                planetModel.getPlanetClassification());
            
            // Create identifier for this planet
            String planetName = planetConfig.getPlanetName().toLowerCase().replace(" ", "_");
            Identifier planetId = new Identifier("terradyne", planetName);
            
            // Initialize the biome source with the planet model
            if (biomeSource instanceof PhysicsBasedBiomeSource physicsSource) {
                physicsSource.setPlanetModel(planetModel);
                
                Terradyne.LOGGER.info("✅ Initialized physics-based biome source");
            } else {
                Terradyne.LOGGER.warn("⚠️  BiomeSource is not physics-based (type: {})",
                        biomeSource.getClass().getSimpleName());
            }
            
            // Create the generator
            UniversalChunkGenerator generator = new UniversalChunkGenerator(planetModel, biomeSource, planetId);
            
            Terradyne.LOGGER.info("✅ Successfully created UniversalChunkGenerator with embedded config");
            
            return generator;
            
        } catch (Exception e) {
            Terradyne.LOGGER.error("❌ CRITICAL: Failed to create chunk generator from embedded config: {}", 
                e.getMessage(), e);
            throw new RuntimeException("Failed to create chunk generator from embedded config", e);
        }
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
     * Main terrain generation method - uses physics system (PERFORMANCE FIXED)
     * NOW SAMPLES CLIMATE ONCE PER CHUNK FOR PERFORMANCE
     */
    private void generateTerrain(Chunk chunk, NoiseConfig noiseConfig) {
        // Lazy registration on first chunk generation
        ensurePlanetModelRegistered();

        // No more lazy loading - planet model should be loaded from codec
        if (planetModel == null) {
            Terradyne.LOGGER.warn("No planet model available for chunk {} - using fallback terrain", chunk.getPos());
            generateFallbackTerrain(chunk);
            return;
        }

        try {
            ChunkPos chunkPos = chunk.getPos();

            Terradyne.LOGGER.debug("Generating physics-based terrain for chunk {} on planet {}",
                    chunkPos, planetModel.getConfig().getPlanetName());

            // === PERFORMANCE FIX: Sample climate ONCE per chunk at chunk center ===
            int chunkCenterX = chunkPos.getStartX() + 8;
            int chunkCenterZ = chunkPos.getStartZ() + 8;

            double chunkTemperature = planetModel.getTemperature(chunkCenterX, chunkCenterZ);
            double chunkMoisture = planetModel.getMoisture(chunkCenterX, chunkCenterZ);
            double chunkWindSpeed = planetModel.getNoiseSystem().sampleWindSpeed(chunkCenterX, chunkCenterZ);

            Terradyne.LOGGER.debug("Chunk climate: temp={:.1f}°C, moisture={:.2f}, wind={:.2f}",
                    chunkTemperature, chunkMoisture, chunkWindSpeed);

            // Generate terrain using physics system with cached climate data
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    int worldX = chunkPos.getStartX() + x;
                    int worldZ = chunkPos.getStartZ() + z;

                    // Generate complete terrain column using physics - adapted for 0-256 range
                    generateTerrainColumn(chunk, x, z, worldX, worldZ, planetModel,
                            chunkTemperature, chunkMoisture, chunkWindSpeed);
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
     * PERFORMANCE FIX: Now accepts cached climate data instead of sampling per column
     */
    private void generateTerrainColumn(Chunk chunk, int x, int z, int worldX, int worldZ,
                                       PlanetModel planetModel, double temperature, double moisture, double windSpeed) {
        // Sample terrain height (this is still needed per column for height variation)
        double terrainHeight = planetModel.getTerrainHeight(worldX, worldZ);

        // Use cached climate data instead of sampling again
        // double temperature = planetModel.getTemperature(worldX, worldZ);  // REMOVED - now cached!
        // double moisture = planetModel.getMoisture(worldX, worldZ);        // REMOVED - now cached!

        // Clamp terrain height to our 0-256 range
        int surfaceY = Math.max(MIN_WORLD_Y, Math.min(MAX_WORLD_Y, (int) terrainHeight));

        // Adjust sea level for 0-256 range
        int seaLevel = Math.max(MIN_WORLD_Y, Math.min(MAX_WORLD_Y, planetModel.getPlanetData().getSeaLevel()));

        // Generate column from bottom to top
        for (int y = MIN_WORLD_Y; y <= MAX_WORLD_Y; y++) {
            BlockPos pos = new BlockPos(x, y, z);

            if (y <= surfaceY) {
                // Use physics system with cached climate data to determine block type
                BlockState blockState = planetModel.getTerrainBlockState(worldX, worldZ, y, temperature, moisture);
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

// UPDATE these methods to not try lazy loading:

    @Override
    public int getSeaLevel() {
        if (planetModel != null) {
            return Math.max(MIN_WORLD_Y, Math.min(MAX_WORLD_Y, planetModel.getPlanetData().getSeaLevel()));
        }
        return 63; // Minecraft default fallback
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

    // UPDATE debug method:
    @Override
    public void getDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {
        text.add("=== TERRADYNE PHYSICS-BASED GENERATION ===");
        text.add("Height Range: Y " + MIN_WORLD_Y + " to " + MAX_WORLD_Y);

        if (planetModel != null) {
            text.add("Planet: " + planetModel.getConfig().getPlanetName());
            text.add("Classification: " + planetModel.getPlanetClassification());
            text.add("Physics Status: " + (planetModel.isValid() ? "ACTIVE" : "ERROR"));

            // NEW: Add biome source info
            text.add("Biome Source: " + getBiomeSource().getClass().getSimpleName());
            if (getBiomeSource() instanceof PhysicsBasedBiomeSource physicsSource) {
                text.add("Biome Initialization: " + (physicsSource.isInitialized() ? "✅" : "❌"));
            }

            text.add("");
            text.add("=== TERRAIN ANALYSIS ===");
            try {
                text.add(planetModel.analyzeTerrainAt(pos.getX(), pos.getZ()));
            } catch (Exception e) {
                text.add("Error analyzing terrain: " + e.getMessage());
            }

            // NEW: Add biome classification info at current position
            try {
                if (getBiomeSource() instanceof PhysicsBasedBiomeSource physicsSource &&
                        physicsSource.isInitialized()) {

                    // Sample biome at current position (convert to biome coordinates)
                    var biomeEntry = getBiomeSource().getBiome(pos.getX() >> 2, pos.getY() >> 2, pos.getZ() >> 2, null);
                    text.add("Current Biome: " + biomeEntry.getKey().map(key -> key.getValue().toString()).orElse("Unknown"));

                    // Show physics conditions that led to this biome
                    double temperature = planetModel.getTemperature(pos.getX(), pos.getZ());
                    double humidity = planetModel.getMoisture(pos.getX(), pos.getZ());
                    int volatility = planetModel.getVolatilityAt(pos.getX(), pos.getZ());
                    double habitability = planetModel.getPlanetData().getHabitability();

                    text.add(String.format("Biome Conditions: T=%.1f°C, H=%.2f, V=%d, Hab=%.2f",
                            temperature, humidity, volatility, habitability));
                }
            } catch (Exception e) {
                text.add("Error analyzing biome: " + e.getMessage());
            }

        } else {
            text.add("No planet model loaded");
            text.add("Planet ID: " + planetId);
            text.add("Status: FALLBACK GENERATION");
            text.add("Biome Source: " + getBiomeSource().getClass().getSimpleName());
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
    public int getMinimumY() {
        return MIN_WORLD_Y; // Y=0
    }

    // === ACCESSORS ===
    /**
     * Get the planet identifier
     */
    public Identifier getPlanetId() {
        return planetId;
    }

    /**
     * Check if physics-based generation is available
     */
    public boolean hasPhysicsGeneration() {
        return planetModel != null && planetModel.isValid();
    }

    /**
     * Get the planet model for external access (map exports, etc.)
     */
    public PlanetModel getPlanetModel() {
        return planetModel;
    }

    // In UniversalChunkGenerator.java, update this method:
    private void ensurePlanetModelRegistered() {
        if (planetModel != null && planetId != null) {
            try {
                // Always register (overwrite if exists) to handle world reloads
                PlanetModelRegistry.register(planetId, planetModel);

            } catch (Exception e) {
                Terradyne.LOGGER.error("Failed to register PlanetModel: {}", e.getMessage(), e);
            }
        }
    }
}