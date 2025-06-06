package net.terradyne.planet.chunk;

// RockyChunkGenerator.java
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
import net.terradyne.planet.config.RockyConfig;
import net.terradyne.planet.model.RockyModel;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class RockyChunkGenerator extends ChunkGenerator {
    public static final Codec<RockyChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource)
            ).apply(instance, (biomeSource) -> new RockyChunkGenerator(null, biomeSource))
    );

    private final RockyModel rockyModel;

    public RockyChunkGenerator(RockyModel rockyModel, BiomeSource biomeSource) {
        super(biomeSource);
        this.rockyModel = rockyModel;
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
            generateRockyTerrain(chunk);
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

    private void generateRockyTerrain(Chunk chunk) {
        ChunkPos chunkPos = chunk.getPos();
        RockyConfig config = rockyModel.getConfig();

        Terradyne.LOGGER.info("Generating rocky terrain for chunk: " + chunkPos);

        int blocksSet = 0;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkPos.getStartX() + x;
                int worldZ = chunkPos.getStartZ() + z;

                // Generate base terrain height
                int baseHeight = generateBaseHeight(worldX, worldZ);

                // Check for crater modification
                int craterModification = generateCraterModification(worldX, worldZ);

                // Final terrain height
                int finalHeight = Math.max(40, baseHeight + craterModification);

                // Generate terrain column
                generateRockyColumn(chunk, x, z, finalHeight, baseHeight);
                blocksSet += (finalHeight - chunk.getBottomY() + 10);
            }
        }

        Terradyne.LOGGER.info("Set " + blocksSet + " blocks in rocky chunk " + chunkPos);
    }

    private int generateBaseHeight(int x, int z) {
        RockyConfig config = rockyModel.getConfig();

        // Base terrain - generally flatter than other planet types
        double terrain1 = Math.sin(x * 0.003) * Math.cos(z * 0.004) * 8;
        double terrain2 = Math.sin(x * 0.008) * Math.cos(z * 0.006) * 4;
        double detail = Math.sin(x * 0.02) * Math.cos(z * 0.025) * 2;

        // Surface roughness affects terrain variation
        double roughnessFactor = rockyModel.getSurfaceRoughness() * 0.5;
        double totalNoise = (terrain1 + terrain2 + detail) * (0.5 + roughnessFactor);

        // Geological activity creates more varied terrain
        if (config.getActivity() != RockyConfig.GeologicalActivity.DEAD) {
            double activityNoise = Math.sin(x * 0.001) * Math.cos(z * 0.0015) * 6;
            totalNoise += activityNoise * switch (config.getActivity()) {
                case DORMANT -> 0.2;
                case MINIMAL -> 0.5;
                case MODERATE -> 1.0;
                default -> 0.0;
            };
        }

        return (int) (60 + totalNoise);
    }

    private int generateCraterModification(int x, int z) {
        RockyConfig config = rockyModel.getConfig();

        if (config.getCraterDensity() < 0.1f) {
            return 0; // No significant cratering
        }

        // Multiple crater noise layers for realistic distribution
        double crater1 = generateCraterNoise(x, z, 200, rockyModel.getAverageCraterSize());
        double crater2 = generateCraterNoise(x + 1000, z + 1000, 300, rockyModel.getAverageCraterSize() * 0.7);
        double crater3 = generateCraterNoise(x + 2000, z + 2000, 150, rockyModel.getAverageCraterSize() * 1.3);

        double totalCraterEffect = (crater1 + crater2 * 0.7 + crater3 * 0.5) * config.getCraterDensity();

        return (int) Math.max(-25, Math.min(totalCraterEffect, 15));
    }

    private double generateCraterNoise(int x, int z, int scale, double craterSize) {
        // Generate circular crater patterns
        double centerX = Math.floor(x / scale) * scale + scale / 2.0;
        double centerZ = Math.floor(z / scale) * scale + scale / 2.0;

        double distanceFromCenter = Math.sqrt(Math.pow(x - centerX, 2) + Math.pow(z - centerZ, 2));
        double craterRadius = craterSize * (0.7 + Math.random() * 0.6); // Vary crater sizes

        if (distanceFromCenter < craterRadius) {
            // Inside crater - create depression with raised rim
            double normalizedDist = distanceFromCenter / craterRadius;

            if (normalizedDist < 0.1) {
                // Crater center - deepest
                return -craterSize * 0.8;
            } else if (normalizedDist < 0.8) {
                // Crater floor - sloped
                return -craterSize * (0.8 - normalizedDist * 0.5);
            } else {
                // Crater rim - raised
                return craterSize * 0.3 * (1.0 - normalizedDist) * 5;
            }
        }

        return 0; // Outside crater
    }

    private void generateRockyColumn(Chunk chunk, int x, int z, int finalHeight, int baseHeight) {
        RockyConfig config = rockyModel.getConfig();

        for (int y = chunk.getBottomY(); y <= finalHeight + 5; y++) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState blockState = getBlockForHeight(y, finalHeight, baseHeight);

            if (blockState != Blocks.AIR.getDefaultState()) {
                chunk.setBlockState(pos, blockState, false);
            }
        }
    }

    private BlockState getBlockForHeight(int y, int finalHeight, int baseHeight) {
        RockyConfig config = rockyModel.getConfig();

        if (y > finalHeight) {
            return Blocks.AIR.getDefaultState();
        }

        // Deep subsurface - solid bedrock
        if (y < finalHeight - 15) {
            return getBedrockBlock();
        }

        // Regolith layer (loose material on surface)
        int regolithThickness = (int) config.getRegolithDepth();
        if (y > finalHeight - regolithThickness) {
            // Check if bedrock is exposed based on ratio
            if (Math.random() < config.getExposedBedrockRatio()) {
                return getExposedBedrockBlock();
            } else {
                return getRegolithBlock();
            }
        }

        // Subsurface rock
        return getSubsurfaceBlock();
    }

    private BlockState getBedrockBlock() {
        return switch (rockyModel.getConfig().getDominantSurface()) {
            case BASALTIC -> Blocks.BASALT.getDefaultState();
            case ANORTHOSITIC -> Blocks.CALCITE.getDefaultState();  // Light colored
            case METALLIC -> Blocks.IRON_BLOCK.getDefaultState();
            case FRACTURED -> Blocks.COBBLESTONE.getDefaultState();
            case REGOLITH -> Blocks.STONE.getDefaultState();
        };
    }

    private BlockState getExposedBedrockBlock() {
        // Weathered/exposed version of bedrock
        return switch (rockyModel.getConfig().getDominantSurface()) {
            case BASALTIC -> Blocks.COBBLED_DEEPSLATE.getDefaultState();
            case ANORTHOSITIC -> Blocks.DIORITE.getDefaultState();
            case METALLIC -> Blocks.RAW_IRON_BLOCK.getDefaultState();
            case FRACTURED -> Blocks.STONE.getDefaultState();
            case REGOLITH -> Blocks.COBBLESTONE.getDefaultState();
        };
    }

    private BlockState getRegolithBlock() {
        // Loose surface material
        return switch (rockyModel.getConfig().getDominantSurface()) {
            case BASALTIC -> Blocks.GRAVEL.getDefaultState();
            case ANORTHOSITIC -> Blocks.WHITE_CONCRETE_POWDER.getDefaultState();
            case METALLIC -> Blocks.GRAY_CONCRETE_POWDER.getDefaultState();
            case FRACTURED -> Blocks.COBBLESTONE.getDefaultState();
            case REGOLITH -> Blocks.GRAVEL.getDefaultState();
        };
    }

    private BlockState getSubsurfaceBlock() {
        // Underground rock layers
        RockyConfig config = rockyModel.getConfig();

        // Add mineral deposits based on richness
        if (Math.random() < config.getMineralRichness() * 0.1) {
            return getOreBlock();
        }

        return getBedrockBlock();
    }

    private BlockState getOreBlock() {
        // Different ore types based on surface composition
        return switch (rockyModel.getConfig().getDominantSurface()) {
            case METALLIC -> Math.random() < 0.5 ? Blocks.IRON_ORE.getDefaultState() : Blocks.COPPER_ORE.getDefaultState();
            case BASALTIC -> Math.random() < 0.3 ? Blocks.COAL_ORE.getDefaultState() : Blocks.IRON_ORE.getDefaultState();
            case ANORTHOSITIC -> Math.random() < 0.4 ? Blocks.NETHER_QUARTZ_ORE.getDefaultState() : Blocks.DIAMOND_ORE.getDefaultState();
            default -> Blocks.STONE.getDefaultState();
        };
    }

    @Override
    public int getHeight(int x, int z, Heightmap.Type heightmapType, HeightLimitView world, NoiseConfig noiseConfig) {
        int baseHeight = generateBaseHeight(x, z);
        int craterMod = generateCraterModification(x, z);
        return Math.max(40, baseHeight + craterMod);
    }

    @Override
    public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
        int height = getHeight(x, z, Heightmap.Type.WORLD_SURFACE_WG, world, noiseConfig);

        BlockState[] column = new BlockState[world.getHeight()];

        for (int y = 0; y < world.getHeight(); y++) {
            int worldY = world.getBottomY() + y;
            column[y] = getBlockForHeight(worldY, height, height);
        }

        return new VerticalBlockSample(world.getBottomY(), column);
    }

    @Override
    public void getDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {

    }
}