package net.terradyne.planet.chunk;

// OceanicChunkGenerator.java
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
import net.terradyne.Terradyne;
import net.terradyne.planet.config.OceanicConfig;
import net.terradyne.planet.model.OceanicModel;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class OceanicChunkGenerator extends ChunkGenerator {
    public static final Codec<OceanicChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource)
            ).apply(instance, (biomeSource) -> new OceanicChunkGenerator(null, biomeSource))
    );

    private final OceanicModel oceanicModel;

    public OceanicChunkGenerator(OceanicModel oceanicModel, BiomeSource biomeSource) {
        super(biomeSource);
        this.oceanicModel = oceanicModel;
    }

    @Override
    protected Codec<? extends ChunkGenerator> getCodec() {
        return CODEC;
    }

    @Override
    public void carve(ChunkRegion chunkRegion, long seed, NoiseConfig noiseConfig, BiomeAccess biomeAccess, StructureAccessor structureAccessor, Chunk chunk, GenerationStep.Carver carverStep) {

    }

    @Override
    public void buildSurface(ChunkRegion region, StructureAccessor structures, NoiseConfig noiseConfig, Chunk chunk) {

    }

    @Override
    public void populateEntities(ChunkRegion region) {

    }

    @Override
    public int getWorldHeight() {
        return 0;
    }

    @Override
    public CompletableFuture<Chunk> populateNoise(Executor executor, Blender blender,
                                                  NoiseConfig noiseConfig, StructureAccessor structureAccessor,
                                                  Chunk chunk) {
        return CompletableFuture.supplyAsync(() -> {
            generateOceanicTerrain(chunk);
            return chunk;
        }, executor);
    }

    @Override
    public int getSeaLevel() {
        return 0;
    }

    @Override
    public int getMinimumY() {
        return 0;
    }

    private void generateOceanicTerrain(Chunk chunk) {
        ChunkPos chunkPos = chunk.getPos();
        OceanicConfig config = oceanicModel.getConfig();

        Terradyne.LOGGER.info("Generating oceanic terrain for chunk: " + chunkPos);

        int blocksSet = 0;
        int seaLevel = oceanicModel.getSeaLevel();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkPos.getStartX() + x;
                int worldZ = chunkPos.getStartZ() + z;

                // Determine if this location is ocean, continental shelf, or land
                TerrainType terrainType = determineTerrainType(worldX, worldZ, config);
                int terrainHeight = generateTerrainHeight(worldX, worldZ, terrainType, seaLevel);

                // Generate the column based on terrain type
                generateTerrainColumn(chunk, x, z, terrainHeight, seaLevel, terrainType);
                blocksSet += (terrainHeight - chunk.getBottomY() + 10);
            }
        }

        Terradyne.LOGGER.info("Set " + blocksSet + " blocks in oceanic chunk " + chunkPos);
    }

    private enum TerrainType {
        DEEP_OCEAN,       // Deep water, far from land
        SHALLOW_OCEAN,    // Continental shelf
        COASTAL,          // Near-shore areas
        LOWLAND,          // Low elevation land
        HIGHLAND,         // Elevated continental areas
        MOUNTAIN,         // High elevation peaks
        ISLAND            // Isolated land in ocean
    }

    private TerrainType determineTerrainType(int x, int z, OceanicConfig config) {
        // Use multiple noise layers to create realistic continent/ocean distribution

        // Large-scale continental noise
        double continentalNoise = Math.sin(x * 0.0008) * Math.cos(z * 0.0012) * 0.7;
        double continentalNoise2 = Math.sin(x * 0.0015 + 1000) * Math.cos(z * 0.0008 + 1000) * 0.5;
        double continentalPattern = (continentalNoise + continentalNoise2) / 1.2;

        // Adjust continental threshold based on ocean coverage
        double landThreshold = 1.0 - (config.getOceanCoverage() * 2.0);

        // Island noise for archipelago effects
        double islandNoise = Math.sin(x * 0.01) * Math.cos(z * 0.015) * 0.3;

        // Coastal complexity
        double coastalNoise = Math.sin(x * 0.05) * Math.cos(z * 0.04) * 0.1;

        double totalLandValue = continentalPattern + islandNoise + coastalNoise;

        if (totalLandValue < landThreshold - 0.4) {
            return TerrainType.DEEP_OCEAN;
        } else if (totalLandValue < landThreshold - 0.2) {
            return TerrainType.SHALLOW_OCEAN;
        } else if (totalLandValue < landThreshold) {
            return TerrainType.COASTAL;
        } else if (totalLandValue < landThreshold + 0.3) {
            return TerrainType.LOWLAND;
        } else if (totalLandValue < landThreshold + 0.6) {
            return TerrainType.HIGHLAND;
        } else if (islandNoise > 0.2 && continentalPattern < landThreshold - 0.1) {
            return TerrainType.ISLAND;
        } else {
            return TerrainType.MOUNTAIN;
        }
    }

    private int generateTerrainHeight(int x, int z, TerrainType type, int seaLevel) {
        OceanicConfig config = oceanicModel.getConfig();

        // Base height noise
        double heightNoise1 = Math.sin(x * 0.01) * Math.cos(z * 0.012) * 8;
        double heightNoise2 = Math.sin(x * 0.025) * Math.cos(z * 0.02) * 4;
        double detailNoise = Math.sin(x * 0.08) * Math.cos(z * 0.075) * 2;

        double baseHeight = heightNoise1 + heightNoise2 + detailNoise;

        // Modify height based on terrain type
        return switch (type) {
            case DEEP_OCEAN -> seaLevel - (int)(config.getAverageOceanDepth() * 0.8 + baseHeight * 0.3);
            case SHALLOW_OCEAN -> seaLevel - (int)(config.getContinentalShelfWidth() * 0.5 + baseHeight * 0.2);
            case COASTAL -> seaLevel + (int)(baseHeight * 0.5) + 2;
            case LOWLAND -> seaLevel + (int)(baseHeight + 8);
            case HIGHLAND -> seaLevel + (int)(baseHeight * 1.5 + 20);
            case MOUNTAIN -> seaLevel + (int)(baseHeight * 2.5 + 35);
            case ISLAND -> seaLevel + (int)(Math.abs(baseHeight) + 12);
        };
    }

    private void generateTerrainColumn(Chunk chunk, int x, int z, int terrainHeight, int seaLevel, TerrainType type) {
        for (int y = chunk.getBottomY(); y <= Math.max(terrainHeight, seaLevel) + 5; y++) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState blockState = getBlockForPosition(y, terrainHeight, seaLevel, type);

            if (blockState != Blocks.AIR.getDefaultState()) {
                chunk.setBlockState(pos, blockState, false);
            }
        }
    }

    private BlockState getBlockForPosition(int y, int terrainHeight, int seaLevel, TerrainType type) {
        // Below terrain - use appropriate subsurface material
        if (y < terrainHeight - 5) {
            return getSubsurfaceBlock(type);
        }

        // Near surface terrain
        if (y <= terrainHeight) {
            return getSurfaceBlock(type, y, terrainHeight, seaLevel);
        }

        // Above terrain but below sea level - water
        if (y <= seaLevel && (type == TerrainType.DEEP_OCEAN || type == TerrainType.SHALLOW_OCEAN)) {
            return Blocks.WATER.getDefaultState();
        }

        return Blocks.AIR.getDefaultState();
    }

    private BlockState getSurfaceBlock(TerrainType type, int y, int terrainHeight, int seaLevel) {
        OceanicConfig config = oceanicModel.getConfig();

        return switch (type) {
            case DEEP_OCEAN, SHALLOW_OCEAN -> {
                if (y == terrainHeight) {
                    yield Blocks.SAND.getDefaultState(); // Ocean floor
                } else {
                    yield Blocks.STONE.getDefaultState();
                }
            }
            case COASTAL -> {
                if (y <= seaLevel + 2) {
                    yield Blocks.SAND.getDefaultState(); // Beach
                } else {
                    yield Blocks.GRASS_BLOCK.getDefaultState();
                }
            }
            case LOWLAND -> Blocks.GRASS_BLOCK.getDefaultState();
            case HIGHLAND -> {
                if (y > terrainHeight - 2) {
                    yield Blocks.GRASS_BLOCK.getDefaultState();
                } else {
                    yield Blocks.DIRT.getDefaultState();
                }
            }
            case MOUNTAIN -> {
                if (y > seaLevel + 40) {
                    yield Blocks.STONE.getDefaultState(); // Rocky peaks
                } else {
                    yield Blocks.GRASS_BLOCK.getDefaultState();
                }
            }
            case ISLAND -> Blocks.GRASS_BLOCK.getDefaultState();
        };
    }

    private BlockState getSubsurfaceBlock(TerrainType type) {
        return switch (type) {
            case DEEP_OCEAN, SHALLOW_OCEAN -> Blocks.STONE.getDefaultState();
            case COASTAL -> Blocks.SANDSTONE.getDefaultState();
            case LOWLAND, HIGHLAND, ISLAND -> Blocks.STONE.getDefaultState();
            case MOUNTAIN -> Blocks.GRANITE.getDefaultState();
        };
    }

    @Override
    public int getHeight(int x, int z, Heightmap.Type heightmapType, HeightLimitView world, NoiseConfig noiseConfig) {
        TerrainType type = determineTerrainType(x, z, oceanicModel.getConfig());
        return generateTerrainHeight(x, z, type, oceanicModel.getSeaLevel());
    }

    @Override
    public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
        TerrainType type = determineTerrainType(x, z, oceanicModel.getConfig());
        int height = generateTerrainHeight(x, z, type, oceanicModel.getSeaLevel());

        BlockState[] column = new BlockState[world.getHeight()];

        for (int y = 0; y < world.getHeight(); y++) {
            int worldY = world.getBottomY() + y;
            column[y] = getBlockForPosition(worldY, height, oceanicModel.getSeaLevel(), type);
        }

        return new VerticalBlockSample(world.getBottomY(), column);
    }

    @Override
    public void getDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {

    }
}
