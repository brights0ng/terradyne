package net.terradyne.planet.chunk;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.noise.SimplexNoiseSampler;
import net.minecraft.util.math.random.Random;
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
import net.terradyne.planet.config.DesertConfig;
import net.terradyne.planet.model.DesertModel;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class DesertChunkGenerator extends ChunkGenerator {
    public static final Codec<DesertChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource)
            ).apply(instance, (biomeSource) -> new DesertChunkGenerator(null, biomeSource))
    );

    private final DesertModel desertModel;

    // Noise samplers for terrain generation
    private final SimplexNoiseSampler baseTerrainNoise;
    private final SimplexNoiseSampler detailTerrainNoise;
    private final SimplexNoiseSampler largeDuneNoise;
    private final SimplexNoiseSampler mediumDuneNoise;
    private final SimplexNoiseSampler smallDuneNoise;
    private final SimplexNoiseSampler windNoise;
    private final SimplexNoiseSampler erosionNoise;

    public DesertChunkGenerator(DesertModel desertModel, BiomeSource biomeSource) {
        super(biomeSource);
        this.desertModel = desertModel;

        // Initialize noise samplers immediately
        if (desertModel != null) {
            long seed = desertModel.getConfig().getSeed();
            Random random = Random.create(seed);

            this.baseTerrainNoise = new SimplexNoiseSampler(random);
            this.detailTerrainNoise = new SimplexNoiseSampler(random);
            this.largeDuneNoise = new SimplexNoiseSampler(random);
            this.mediumDuneNoise = new SimplexNoiseSampler(random);
            this.smallDuneNoise = new SimplexNoiseSampler(random);
            this.windNoise = new SimplexNoiseSampler(random);
            this.erosionNoise = new SimplexNoiseSampler(random);

            Terradyne.LOGGER.info("Initialized noise samplers for desert planet: " +
                    desertModel.getConfig().getPlanetName());
        } else {
            // Null initialization for codec
            this.baseTerrainNoise = null;
            this.detailTerrainNoise = null;
            this.largeDuneNoise = null;
            this.mediumDuneNoise = null;
            this.smallDuneNoise = null;
            this.windNoise = null;
            this.erosionNoise = null;
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
            generateDesertTerrain(chunk);
            return chunk;
        }, executor);
    }

    // ===== TERRAIN GENERATION =====
    private void generateDesertTerrain(Chunk chunk) {
        ChunkPos chunkPos = chunk.getPos();
        DesertConfig config = desertModel.getConfig();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkPos.getStartX() + x;
                int worldZ = chunkPos.getStartZ() + z;

                // Generate layered terrain
                int bedrockHeight = generateBedrockHeight(worldX, worldZ);
                int targetTerrainHeight = generateTargetTerrainHeight(worldX, worldZ, config); // NEW!
                int finalHeight = Math.max(bedrockHeight, targetTerrainHeight); // Ensure we don't go below bedrock

                // Clamp to world bounds
                finalHeight = Math.max(chunk.getBottomY() + 10,
                        Math.min(finalHeight, chunk.getTopY() - 10));

                // Fill the column
                fillTerrainColumn(chunk, x, z, bedrockHeight, finalHeight);
            }
        }
    }

    // NEW METHOD: Calculate the target terrain height (what the surface should be)
    private int generateTargetTerrainHeight(int x, int z, DesertConfig config) {
        if (config.getSandDensity() < 0.1f) {
            return generateBedrockHeight(x, z); // No sand, terrain follows bedrock
        }

        // Base terrain height (starting point for dunes)
        int baseHeight = 65; // Reasonable desert surface level

        // Use unified dune system if dunes are enabled
        if (config.hasDunes()) {
            return baseHeight + generateDuneHeightContribution(x, z, config);
        } else {
            return baseHeight + generateSimpleSandContribution(x, z, config);
        }
    }

    // RENAMED: This now calculates height contribution, not total sand height
    private int generateDuneHeightContribution(int x, int z, DesertConfig config) {
        float maxDuneHeight = Math.max(desertModel.getDuneHeight(), 60.0f);

        // === PRIMARY DUNE STRUCTURE (Large Scale) ===

        // 1. Large sweeping dune formations - 5x wider in x
        double majorDunes = (largeDuneNoise.sample(x * 0.00016, 0, z * 0.0008) + 1.0) * 0.5 *
                maxDuneHeight * 0.35; // Reduced slightly to make room for details

        // 2. Wave-like ridge patterns - 5x wider in x
        double wave1 = (mediumDuneNoise.sample(x * 0.00028, 0, z * 0.0014) + 1.0) * 0.5 *
                maxDuneHeight * 0.22;
        double wave2 = (smallDuneNoise.sample(x * 0.00048, 0, z * 0.0024) + 1.0) * 0.5 *
                maxDuneHeight * 0.18;

        // === SECONDARY DETAIL LAYERS (Medium Scale) ===

        // 3. Wind-aligned ridge dunes with more variation
        double windStrength = config.getWindStrength();
        double windAngle = windNoise.sample(x * 0.00006, 0, z * 0.0003) * Math.PI;
        double windAlignedX = x * Math.cos(windAngle) - z * Math.sin(windAngle);
        double windAlignedZ = x * Math.sin(windAngle) + z * Math.cos(windAngle);

        double windRidges = (erosionNoise.sample(windAlignedX * 0.00012, 0, windAlignedZ * 0.0006) + 1.0) * 0.5 *
                windStrength * maxDuneHeight * 0.2;

        // 4. Medium-scale variations with multiple octaves
        double mediumDetail1 = (windNoise.sample(x * 0.0012, 0, z * 0.006) + 1.0) * 0.5 *
                maxDuneHeight * 0.12;
        double mediumDetail2 = (erosionNoise.sample(x * 0.0008, 0, z * 0.004) + 1.0) * 0.5 *
                maxDuneHeight * 0.08;

        // === FINE DETAIL LAYERS (Small Scale) ===

        // 5. Fine sand ripples - multiple octaves for naturalism
        double ripples1 = (detailTerrainNoise.sample(x * 0.0032, 0, z * 0.016) + 1.0) * 0.5 *
                maxDuneHeight * 0.08;
        double ripples2 = (baseTerrainNoise.sample(x * 0.0048, 0, z * 0.024) + 1.0) * 0.5 *
                maxDuneHeight * 0.06;
        double ripples3 = (largeDuneNoise.sample(x * 0.0064, 0, z * 0.032) + 1.0) * 0.5 *
                maxDuneHeight * 0.04;

        // 6. Micro-variations (surface texture)
        double microTexture1 = (mediumDuneNoise.sample(x * 0.008, 0, z * 0.04) + 1.0) * 0.5 *
                maxDuneHeight * 0.05;
        double microTexture2 = (smallDuneNoise.sample(x * 0.012, 0, z * 0.06) + 1.0) * 0.5 *
                maxDuneHeight * 0.03;

        // === NATURAL VARIATION LAYERS ===

        // 7. Erosion patterns (break up regular patterns)
        double erosionBreakup = (erosionNoise.sample(x * 0.002, 0, z * 0.01) + 1.0) * 0.5 *
                desertModel.getErosionRate() * maxDuneHeight * 0.1;

        // 8. Wind variation patterns (add directional chaos)
        double windChaos = (windNoise.sample(x * 0.004, 0, z * 0.02) + 1.0) * 0.5 *
                config.getWindStrength() * maxDuneHeight * 0.07;

        // 9. Base undulation layer (ensure no flat spots)
        double baseUndulation = (baseTerrainNoise.sample(x * 0.001, 0, z * 0.005) + 1.0) * 0.5 *
                maxDuneHeight * 0.1;

        // === COMBINE WITH LAYERED APPROACH ===

        // Primary structure (large scale)
        double primaryLayer = majorDunes + Math.max(wave1, wave2) + windRidges;

        // Detail layers (medium scale)
        double detailLayer = mediumDetail1 + mediumDetail2 + erosionBreakup + windChaos;

        // Fine layers (small scale)
        double fineLayer = ripples1 + ripples2 + ripples3 + microTexture1 + microTexture2;

        // Base layer
        double baseLayer = baseUndulation;

        // Combine all layers with slight weighting
        double totalDuneContribution = primaryLayer +
                (detailLayer * 0.8) + // Slightly reduce medium details
                (fineLayer * 0.6) +   // Reduce fine details more
                baseLayer;

        // Ensure minimum variation
        double minContribution = config.getSandDensity() * 12;

        return (int) Math.max(minContribution, totalDuneContribution);
    }

    private int generateSimpleSandContribution(int x, int z, DesertConfig config) {
        // Simple height contribution for non-dune areas
        double baseSand = detailTerrainNoise.sample(x * 0.004, 0, z * 0.004) *
                config.getSandDensity() * 15;

        double windDrift = windNoise.sample(x * 0.008, 0, z * 0.008) *
                config.getWindStrength() * 8;

        double erosionEffect = erosionNoise.sample(x * 0.01, 0, z * 0.01) *
                desertModel.getErosionRate() * 5;

        return (int) Math.max(0, baseSand + windDrift + erosionEffect);
    }

    // UPDATE: Now sand fills the space between bedrock and target height
    private void fillTerrainColumn(Chunk chunk, int x, int z, int bedrockHeight, int finalHeight) {
        for (int y = chunk.getBottomY(); y <= finalHeight + 5; y++) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState blockState = getBlockForHeight(y, bedrockHeight, finalHeight);

            if (blockState != Blocks.AIR.getDefaultState()) {
                chunk.setBlockState(pos, blockState, false);
            }
        }
    }

    private int generateBedrockHeight(int x, int z) {
        DesertConfig config = desertModel.getConfig();

        // Large-scale terrain features
        double baseNoise = baseTerrainNoise.sample(x * 0.003, 0, z * 0.003) * 15;
        double detailNoise = detailTerrainNoise.sample(x * 0.012, 0, z * 0.012) * 6;
        double fineNoise = detailTerrainNoise.sample(x * 0.045, 0, z * 0.045) * 2;

        // Rock type affects terrain roughness
        double roughnessFactor = switch (config.getDominantRock()) {
            case GRANITE -> 1.4;
            case VOLCANIC -> 1.8;
            case LIMESTONE -> 0.9;
            case SANDSTONE -> 0.7;
        };

        double totalNoise = (baseNoise + detailNoise + fineNoise) * roughnessFactor;

        // Erosion smooths terrain
        double erosionFactor = 1.0 - (desertModel.getErosionRate() * 0.25);
        totalNoise *= erosionFactor;

        return (int) (55 + totalNoise);
    }

    private int generateSandHeight(int x, int z, int bedrockHeight) {
        DesertConfig config = desertModel.getConfig();

        if (config.getSandDensity() < 0.1f) {
            return bedrockHeight;
        }

        // Use unified dune system if dunes are enabled
        if (config.hasDunes()) {
            return generateDuneSandLayer(x, z, bedrockHeight, config);
        } else {
            return generateSimpleSandLayer(x, z, bedrockHeight, config);
        }
    }

    private int generateDuneSandLayer(int x, int z, int bedrockHeight, DesertConfig config) {
        float maxDuneHeight = Math.max(desertModel.getDuneHeight(), 60.0f);

        // 1. Large sweeping dune formations - 5x wider in x, ensure positive contribution
        double majorDunes = (largeDuneNoise.sample(x * 0.00016, 0, z * 0.0008) + 1.0) * 0.5 *
                maxDuneHeight * 0.4;

        // 2. Wave-like ridge patterns - 5x wider in x, ensure positive contribution
        double wave1 = (mediumDuneNoise.sample(x * 0.00028, 0, z * 0.0014) + 1.0) * 0.5 *
                maxDuneHeight * 0.27;
        double wave2 = (smallDuneNoise.sample(x * 0.00048, 0, z * 0.0024) + 1.0) * 0.5 *
                maxDuneHeight * 0.2;

        // 3. Wind-aligned ridge dunes - 5x wider in x, ensure positive contribution
        double windStrength = config.getWindStrength();
        double windAngle = windNoise.sample(x * 0.00006, 0, z * 0.0003) * Math.PI;
        double windAlignedX = x * Math.cos(windAngle) - z * Math.sin(windAngle);
        double windAlignedZ = x * Math.sin(windAngle) + z * Math.cos(windAngle);

        double windRidges = (erosionNoise.sample(windAlignedX * 0.00012, 0, windAlignedZ * 0.0006) + 1.0) * 0.5 *
                windStrength * maxDuneHeight * 0.23;

        // 4. Fine sand ripples - 5x wider in x, ensure positive contribution
        double ripples = (detailTerrainNoise.sample(x * 0.0032, 0, z * 0.016) + 1.0) * 0.5 *
                maxDuneHeight * 0.1;

        // 5. Medium-scale sand variations - 5x wider in x, ensure positive contribution
        double mediumSand = (windNoise.sample(x * 0.0012, 0, z * 0.006) + 1.0) * 0.5 *
                config.getSandDensity() * maxDuneHeight * 0.17;

        // 6. Base undulation layer - ensures no completely flat areas
        double baseUndulation = (baseTerrainNoise.sample(x * 0.001, 0, z * 0.005) + 1.0) * 0.5 *
                maxDuneHeight * 0.15;

        // Combine all patterns - all are now guaranteed positive
        double totalDuneHeight = majorDunes +
                Math.max(wave1, wave2) +
                windRidges +
                ripples +
                mediumSand +
                baseUndulation;

        // Ensure minimum sand coverage (higher base to eliminate flat spots)
        double minSandBase = config.getSandDensity() * 12; // Increased from 8 to 12
        int finalSandHeight = (int) Math.max(minSandBase, totalDuneHeight);

        return bedrockHeight + finalSandHeight;
    }

    private int generateSimpleSandLayer(int x, int z, int bedrockHeight, DesertConfig config) {
        // Simple sand patterns for non-dune areas
        double baseSand = detailTerrainNoise.sample(x * 0.004, 0, z * 0.004) *
                config.getSandDensity() * 6;

        double windDrift = windNoise.sample(x * 0.008, 0, z * 0.008) *
                config.getWindStrength() * 3;

        double erosionEffect = erosionNoise.sample(x * 0.01, 0, z * 0.01) *
                desertModel.getErosionRate() * 2;

        int sandThickness = (int) Math.max(0, baseSand + windDrift + erosionEffect);

        return bedrockHeight + sandThickness;
    }

    // ===== BLOCK SELECTION =====

    private BlockState getBlockForHeight(int y, int bedrockHeight, int finalHeight) {
        DesertConfig config = desertModel.getConfig();

        if (y > finalHeight) {
            return Blocks.AIR.getDefaultState();
        }

        // Deep underground - bedrock
        if (y < bedrockHeight - 10) {
            return getBedrockBlock();
        }

        // Bedrock layer
        if (y <= bedrockHeight) {
            return getBedrockBlock();
        }

        // Sand layer with occasional rock exposure
        if (y <= finalHeight) {
            // Mix sand and exposed rock based on erosion
            if (desertModel.getRockExposure() > 0.5 &&
                    Math.sin(y * 0.1) > 0.3) {
                return getWeatheredRockBlock();
            } else {
                return getSandBlock();
            }
        }

        // Surface decoration
        if (y == finalHeight && Math.random() < 0.05) {
            return getDecorationBlock();
        }

        return Blocks.AIR.getDefaultState();
    }

    private BlockState getBedrockBlock() {
        return switch (desertModel.getConfig().getDominantRock()) {
            case GRANITE -> Blocks.GRANITE.getDefaultState();
            case VOLCANIC -> Blocks.BASALT.getDefaultState();
            case LIMESTONE -> Blocks.CALCITE.getDefaultState();
            case SANDSTONE -> Blocks.SANDSTONE.getDefaultState();
        };
    }

    private BlockState getWeatheredRockBlock() {
        return switch (desertModel.getConfig().getDominantRock()) {
            case GRANITE -> Blocks.COBBLESTONE.getDefaultState();
            case VOLCANIC -> Blocks.COBBLED_DEEPSLATE.getDefaultState();
            case LIMESTONE -> Blocks.STONE.getDefaultState();
            case SANDSTONE -> Blocks.RED_SANDSTONE.getDefaultState();
        };
    }

    private BlockState getSandBlock() {
        DesertConfig config = desertModel.getConfig();

        // Different sand types based on temperature
        if (config.getSurfaceTemperature() > 45) {
            return Blocks.RED_SAND.getDefaultState();
        } else {
            return Blocks.SAND.getDefaultState();
        }
    }

    private BlockState getDecorationBlock() {
        DesertConfig config = desertModel.getConfig();

        // Sparse desert decorations
        if (config.getHumidity() > 0.1f) {
            return Blocks.DEAD_BUSH.getDefaultState();
        } else {
            return Blocks.AIR.getDefaultState();
        }
    }

    // ===== INTERFACE IMPLEMENTATIONS =====

    @Override
    public int getHeight(int x, int z, Heightmap.Type heightmapType, HeightLimitView world, NoiseConfig noiseConfig) {
        if (desertModel == null) return 64; // Safe fallback for codec

        int bedrockHeight = generateBedrockHeight(x, z);
        int sandHeight = generateSandHeight(x, z, bedrockHeight);
        return sandHeight;
    }

    @Override
    public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
        int height = getHeight(x, z, Heightmap.Type.WORLD_SURFACE_WG, world, noiseConfig);

        BlockState[] column = new BlockState[world.getHeight()];
        for (int y = 0; y < world.getHeight(); y++) {
            int worldY = world.getBottomY() + y;
            column[y] = worldY <= height ? getSandBlock() : Blocks.AIR.getDefaultState();
        }

        return new VerticalBlockSample(world.getBottomY(), column);
    }

    // ===== UNUSED INTERFACE METHODS =====

    @Override
    public void carve(ChunkRegion chunkRegion, long seed, NoiseConfig noiseConfig,
                      BiomeAccess biomeAccess, StructureAccessor structureAccessor,
                      Chunk chunk, GenerationStep.Carver carverStep) {
        // No carving for desert planets
    }

    @Override
    public void buildSurface(ChunkRegion region, StructureAccessor structures,
                             NoiseConfig noiseConfig, Chunk chunk) {
        // Surface building handled in populateNoise
    }

    @Override
    public void populateEntities(ChunkRegion region) {
        // No special entity population
    }

    @Override
    public int getWorldHeight() {
        return 384;
    }

    @Override
    public int getSeaLevel() {
        return 0;
    }

    @Override
    public int getMinimumY() {
        return -64;
    }

    @Override
    public void getDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {
        if (desertModel != null) {
            text.add("Desert Planet: " + desertModel.getConfig().getPlanetName());
            text.add("Temperature: " + desertModel.getConfig().getSurfaceTemperature() + "°C");
            text.add("Sand Density: " + (desertModel.getConfig().getSandDensity() * 100) + "%");
        }
    }
}