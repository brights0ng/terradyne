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
import net.starlight.terradyne.planet.physics.PlanetModel;
import net.starlight.terradyne.planet.terrain.TerrainHeightMapper;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * UPDATED Universal Chunk Generator with Physics-Based Generation
 * Replaces pass-based system with planetary physics and noise system
 */
public class UniversalChunkGenerator extends ChunkGenerator {
    public static final Codec<UniversalChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource)
            ).apply(instance, (biomeSource) -> new UniversalChunkGenerator(null, biomeSource))
    );

    private final PlanetModel planetModel;

    public UniversalChunkGenerator(PlanetModel planetModel, BiomeSource biomeSource) {
        super(biomeSource);
        this.planetModel = planetModel;

        if (planetModel != null) {
            Terradyne.LOGGER.info("=== UNIVERSAL CHUNK GENERATOR INITIALIZED ===");
            Terradyne.LOGGER.info("Planet: {}", planetModel.getConfig().getPlanetName());
            Terradyne.LOGGER.info("Type: {}", planetModel.getPlanetData().getPlanetAge().getDisplayName());
            Terradyne.LOGGER.info("Classification: {}", planetModel.getPlanetClassification());
            Terradyne.LOGGER.info("Generation System: PHYSICS-BASED");
        } else {
            // Codec constructor
            Terradyne.LOGGER.warn("UniversalChunkGenerator created without planet model (codec)");
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
            generatePhysicsBasedTerrain(chunk, noiseConfig);
            return chunk;
        }, executor);
    }

    /**
     * NEW PHYSICS-BASED TERRAIN GENERATION
     * Replaces the old pass-based system with planetary physics
     */
    private void generatePhysicsBasedTerrain(Chunk chunk, NoiseConfig noiseConfig) {
        if (planetModel == null) {
            generateFallbackTerrain(chunk);
            return;
        }

        try {
            ChunkPos chunkPos = chunk.getPos();
            
            Terradyne.LOGGER.debug("Generating physics-based terrain for chunk {}", chunkPos);

            // Generate terrain using physics system
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    int worldX = chunkPos.getStartX() + x;
                    int worldZ = chunkPos.getStartZ() + z;

                    // Generate complete terrain column using physics
                    TerrainHeightMapper.TerrainColumn terrainColumn = 
                        planetModel.generateTerrainColumn(worldX, worldZ);

                    // Apply terrain column to chunk
                    applyTerrainColumnToChunk(chunk, x, z, terrainColumn);
                }
            }

            Terradyne.LOGGER.debug("✅ Physics-based generation completed for chunk {}", chunkPos);

        } catch (Exception e) {
            Terradyne.LOGGER.error("Critical error in physics-based terrain generation for chunk {}: {}",
                    chunk.getPos(), e.getMessage());
            e.printStackTrace();
            generateFallbackTerrain(chunk);
        }
    }

    /**
     * Apply a terrain column to the chunk at the specified x,z position
     */
    private void applyTerrainColumnToChunk(Chunk chunk, int x, int z, TerrainHeightMapper.TerrainColumn column) {
        BlockState[] blocks = column.getAllBlocks();
        
        for (int y = chunk.getBottomY(); y <= chunk.getTopY(); y++) {
            int arrayIndex = y - chunk.getBottomY();
            if (arrayIndex >= 0 && arrayIndex < blocks.length) {
                BlockPos pos = new BlockPos(x, y, z);
                BlockState blockState = blocks[arrayIndex];
                
                if (blockState != null && !blockState.isAir()) {
                    chunk.setBlockState(pos, blockState, false);
                } else if (y <= planetModel.getPlanetData().getSeaLevel() && 
                          planetModel.getPlanetData().hasLiquidWater()) {
                    // Fill with water up to sea level
                    chunk.setBlockState(pos, Blocks.WATER.getDefaultState(), false);
                }
                // Otherwise leave as air (default)
            }
        }
    }

    /**
     * Fallback terrain generation if physics system fails
     */
    private void generateFallbackTerrain(Chunk chunk) {
        Terradyne.LOGGER.warn("Using fallback terrain generation for chunk {}", chunk.getPos());

        // Simple flat terrain as emergency fallback
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = chunk.getBottomY(); y <= 75; y++) {
                    BlockPos pos = new BlockPos(x, y, z);

                    if (y <= 50) {
                        chunk.setBlockState(pos, Blocks.DEEPSLATE.getDefaultState(), false);
                    } else if (y <= 70) {
                        chunk.setBlockState(pos, Blocks.STONE.getDefaultState(), false);
                    } else if (y <= 75) {
                        chunk.setBlockState(pos, Blocks.DIRT.getDefaultState(), false);
                    }
                }
            }
        }
    }

    // ============================================================================
    // REQUIRED CHUNK GENERATOR METHODS (updated for physics system)
    // ============================================================================

    @Override
    public void carve(ChunkRegion chunkRegion, long seed, NoiseConfig noiseConfig, BiomeAccess biomeAccess, StructureAccessor structureAccessor, Chunk chunk, GenerationStep.Carver carverStep) {
        // Carving disabled for now - tectonic system handles erosion
    }

    @Override
    public void buildSurface(ChunkRegion region, StructureAccessor structures, NoiseConfig noiseConfig, Chunk chunk) {
        // Surface building handled by physics system
    }

    @Override
    public void populateEntities(ChunkRegion region) {
        // Default entity population
    }

    @Override
    public int getWorldHeight() { 
        return 384; 
    }

    @Override
    public int getSeaLevel() {
        if (planetModel != null) {
            return planetModel.getPlanetData().getSeaLevel();
        }
        return 63; // Minecraft default
    }

    @Override
    public int getMinimumY() { 
        return -64; 
    }

    @Override
    public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
        if (planetModel == null) {
            return 75; // Fallback
        }

        double terrainHeight = planetModel.getTerrainHeight(x, z);
        return Math.max(getMinimumY(), Math.min(getMinimumY() + getWorldHeight() - 1, (int) terrainHeight));
    }

    @Override
    public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
        if (planetModel == null) {
            // Fallback column
            BlockState[] column = new BlockState[world.getHeight()];
            for (int y = 0; y < world.getHeight(); y++) {
                int worldY = world.getBottomY() + y;
                if (worldY <= 70) {
                    column[y] = Blocks.STONE.getDefaultState();
                } else {
                    column[y] = Blocks.AIR.getDefaultState();
                }
            }
            return new VerticalBlockSample(world.getBottomY(), column);
        }

        // Generate terrain column using physics
        TerrainHeightMapper.TerrainColumn terrainColumn = planetModel.generateTerrainColumn(x, z);
        BlockState[] physicsBlocks = terrainColumn.getAllBlocks();
        
        // Convert to Minecraft's format
        BlockState[] column = new BlockState[world.getHeight()];
        for (int y = 0; y < world.getHeight(); y++) {
            int worldY = world.getBottomY() + y;
            int arrayIndex = worldY + 64; // Offset for our array indexing
            
            if (arrayIndex >= 0 && arrayIndex < physicsBlocks.length) {
                column[y] = physicsBlocks[arrayIndex];
            } else {
                column[y] = Blocks.AIR.getDefaultState();
            }
        }

        return new VerticalBlockSample(world.getBottomY(), column);
    }

    @Override
    public void getDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {
        text.add("=== PHYSICS-BASED TERRAIN GENERATION ===");
        if (planetModel != null) {
            text.add("Planet: " + planetModel.getConfig().getPlanetName());
            text.add("Classification: " + planetModel.getPlanetClassification());
            
            // Sample terrain at cursor position
            text.add("");
            text.add("=== TERRAIN ANALYSIS ===");
            text.add(planetModel.analyzeTerrainAt(pos.getX(), pos.getZ()));
            
            text.add("");
            text.add("=== NOISE SAMPLING ===");
            text.add(planetModel.sampleAllNoiseAt(pos.getX(), pos.getZ()));
            
            text.add("");
            text.add("=== PHYSICS DATA ===");
            text.add("Temperature: " + String.format("%.1f°C", planetModel.getTemperature(pos.getX(), pos.getZ())));
            text.add("Moisture: " + String.format("%.2f", planetModel.getMoisture(pos.getX(), pos.getZ())));
            text.add("Tectonic Activity: " + String.format("%.2f", planetModel.getTectonicActivity(pos.getX(), pos.getZ())));
            
        } else {
            text.add("No planet model loaded");
        }
    }

    // === ACCESSORS ===

    public PlanetModel getPlanetModel() {
        return planetModel;
    }
}