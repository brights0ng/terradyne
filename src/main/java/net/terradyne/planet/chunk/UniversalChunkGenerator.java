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
import net.terradyne.planet.PlanetType;
import net.terradyne.planet.biome.IBiomeType;
import net.terradyne.planet.biome.DesertBiomeType;
import net.terradyne.planet.biome.DesertBiomeSource;
import net.terradyne.planet.config.DesertConfig;
import net.terradyne.planet.model.IPlanetModel;
import net.terradyne.planet.model.DesertModel;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Unified Octave Terrain Generator
 * Uses ONE master noise function sampled at different frequencies
 * Biomes control the amplitude of each frequency band
 */
public class UniversalChunkGenerator extends ChunkGenerator {
    public static final Codec<UniversalChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource)
            ).apply(instance, (biomeSource) -> new UniversalChunkGenerator(null, biomeSource))
    );

    private final IPlanetModel planetModel;

    // SINGLE master noise sampler for all terrain features
    private final SimplexNoiseSampler masterNoise;

    // Additional noise for specialized features (but still coordinated)
    private final SimplexNoiseSampler windDirectionNoise;

    public UniversalChunkGenerator(IPlanetModel planetModel, BiomeSource biomeSource) {
        super(biomeSource);
        this.planetModel = planetModel;

        if (planetModel != null) {
            Random random = Random.create(planetModel.getConfig().getSeed());
            this.masterNoise = new SimplexNoiseSampler(random);
            this.windDirectionNoise = new SimplexNoiseSampler(random);
        } else {
            // Codec constructor
            this.masterNoise = null;
            this.windDirectionNoise = null;
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
            generateUnifiedTerrain(chunk, noiseConfig);
            return chunk;
        }, executor);
    }

    private void generateUnifiedTerrain(Chunk chunk, NoiseConfig noiseConfig) {
        ChunkPos chunkPos = chunk.getPos();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkPos.getStartX() + x;
                int worldZ = chunkPos.getStartZ() + z;

                // Get biome type
                IBiomeType biomeType = getBiomeTypeAt(worldX, worldZ);

                // Generate unified terrain height using octaves of the same noise
                UnifiedTerrainResult result = generateUnifiedHeight(worldX, worldZ, biomeType);

                // Fill terrain column
                fillUnifiedTerrainColumn(chunk, x, z, result);
            }
        }
    }

    private UnifiedTerrainResult generateUnifiedHeight(int x, int z, IBiomeType biomeType) {
        if (planetModel == null) {
            return new UnifiedTerrainResult(60, 65, Blocks.STONE.getDefaultState());
        }

        // Get biome-specific octave settings
        BiomeOctaveSettings settings = getBiomeOctaveSettings(biomeType);

        // Generate base foundation height (lowest frequency)
        double foundationOctave = masterNoise.sample(x * 0.0008, 0, z * 0.0008) * settings.foundationAmplitude;

        // Generate large-scale features (low frequency)
        double largeOctave = masterNoise.sample(x * 0.002, 0, z * 0.002) * settings.largeFeatureAmplitude;

        // Generate medium-scale features (medium frequency)
        double mediumOctave = masterNoise.sample(x * 0.008, 0, z * 0.008) * settings.mediumFeatureAmplitude;

        // Generate fine details (high frequency)
        double fineOctave = masterNoise.sample(x * 0.03, 0, z * 0.03) * settings.fineDetailAmplitude;

        // Generate carving/erosion (inverted noise at specific frequency)
        double carvingOctave = generateCarvingOctave(x, z, settings);

        // Wind-aligned features (rotated sampling of same noise)
        double windOctave = generateWindAlignedOctave(x, z, settings);

        // Combine all octaves smoothly
        double totalHeight = settings.baseHeight +
                foundationOctave +
                largeOctave +
                mediumOctave +
                fineOctave +
                carvingOctave +
                windOctave;

        int finalHeight = (int) Math.max(settings.baseHeight - 30, totalHeight);
        int bedrockHeight = (int) (settings.baseHeight + foundationOctave);

        BlockState material = getPrimaryBlockForBiome(biomeType);

        return new UnifiedTerrainResult(bedrockHeight, finalHeight, material);
    }

    private double generateCarvingOctave(int x, int z, BiomeOctaveSettings settings) {
        if (settings.carvingAmplitude == 0) {
            return 0; // No carving for this biome
        }

        // Use ridge noise (absolute value) to create channels
        double ridgeNoise = Math.abs(masterNoise.sample(x * 0.001, 0, z * 0.0003));

        // Only carve narrow channels
        if (ridgeNoise < 0.3) {
            double carvingIntensity = (0.3 - ridgeNoise) / 0.3; // 0-1
            return -carvingIntensity * settings.carvingAmplitude; // Negative = carve down
        }

        return 0;
    }

    private double generateWindAlignedOctave(int x, int z, BiomeOctaveSettings settings) {
        if (settings.windAmplitude == 0) {
            return 0; // No wind features for this biome
        }

        // Get wind direction for this area
        double windAngle = windDirectionNoise.sample(x * 0.0001, 0, z * 0.0001) * Math.PI;

        // Rotate coordinates to align with wind
        double windX = x * Math.cos(windAngle) - z * Math.sin(windAngle);
        double windZ = x * Math.sin(windAngle) + z * Math.cos(windAngle);

        // Sample master noise along wind direction (elongated features)
        return masterNoise.sample(windX * 0.0004, 0, windZ * 0.006) * settings.windAmplitude;
    }

    private BiomeOctaveSettings getBiomeOctaveSettings(IBiomeType biomeType) {
        if (planetModel.getType() != PlanetType.DESERT && planetModel.getType() != PlanetType.HOTHOUSE) {
            // Non-desert planets - simple settings
            return new BiomeOctaveSettings(55, 12, 8, 4, 2, 0, 0);
        }

        DesertModel desertModel = (DesertModel) planetModel;
        DesertConfig config = desertModel.getConfig();

        // Base values for desert planets
        double baseHeight = 50;
        double maxDuneHeight = Math.max(desertModel.getDuneHeight(), 60.0f);

        if (biomeType instanceof DesertBiomeType desertBiome) {
            return switch (desertBiome) {
                case DUNE_SEA -> new BiomeOctaveSettings(
                        baseHeight,                    // baseHeight
                        8,                            // foundationAmplitude - gentle base
                        maxDuneHeight * 0.4,          // largeFeatureAmplitude - major dunes
                        maxDuneHeight * 0.25,         // mediumFeatureAmplitude - dune ridges
                        maxDuneHeight * 0.15,         // fineDetailAmplitude - sand ripples
                        0,                            // carvingAmplitude - NO canyons
                        maxDuneHeight * 0.2           // windAmplitude - wind patterns
                );

                case GRANITE_MESAS -> new BiomeOctaveSettings(
                        baseHeight + 10,              // baseHeight - higher rocky terrain
                        15,                           // foundationAmplitude - rocky foundation
                        8,                            // largeFeatureAmplitude - mesa tops
                        4,                            // mediumFeatureAmplitude - rock variation
                        2,                            // fineDetailAmplitude - surface texture
                        25,                           // carvingAmplitude - deep canyons
                        0                             // windAmplitude - no wind features
                );

                case LIMESTONE_CANYONS -> new BiomeOctaveSettings(
                        baseHeight + 5,               // baseHeight
                        12,                           // foundationAmplitude
                        6,                            // largeFeatureAmplitude
                        3,                            // mediumFeatureAmplitude
                        1,                            // fineDetailAmplitude
                        30,                           // carvingAmplitude - very deep canyons
                        0                             // windAmplitude
                );

                case SCRUBLAND -> new BiomeOctaveSettings(
                        baseHeight,                   // baseHeight
                        10,                           // foundationAmplitude
                        maxDuneHeight * 0.2,          // largeFeatureAmplitude - small dunes
                        maxDuneHeight * 0.15,         // mediumFeatureAmplitude
                        maxDuneHeight * 0.1,          // fineDetailAmplitude
                        12,                           // carvingAmplitude - shallow channels
                        maxDuneHeight * 0.1           // windAmplitude - light wind features
                );

                case SCORCHING_WASTE -> new BiomeOctaveSettings(
                        baseHeight - 5,               // baseHeight - heat sink
                        6,                            // foundationAmplitude - flatter
                        maxDuneHeight * 0.3,          // largeFeatureAmplitude - heat dunes
                        maxDuneHeight * 0.2,          // mediumFeatureAmplitude
                        maxDuneHeight * 0.1,          // fineDetailAmplitude
                        0,                            // carvingAmplitude - no water erosion
                        maxDuneHeight * 0.25          // windAmplitude - strong heat winds
                );

                case DUST_BOWL -> new BiomeOctaveSettings(
                        baseHeight - 3,               // baseHeight - wind-blown flat
                        4,                            // foundationAmplitude - very flat
                        2,                            // largeFeatureAmplitude - minimal
                        1,                            // mediumFeatureAmplitude - minimal
                        3,                            // fineDetailAmplitude - surface texture only
                        0,                            // carvingAmplitude - no erosion
                        5                             // windAmplitude - light wind patterns
                );

                case SALT_FLATS -> new BiomeOctaveSettings(
                        baseHeight - 8,               // baseHeight - dried lake bed
                        2,                            // foundationAmplitude - very flat
                        1,                            // largeFeatureAmplitude - nearly flat
                        0.5,                          // mediumFeatureAmplitude - nearly flat
                        0.5,                          // fineDetailAmplitude - salt crystals
                        8,                            // carvingAmplitude - drainage channels
                        0                             // windAmplitude - no wind features
                );

                case VOLCANIC_WASTELAND -> new BiomeOctaveSettings(
                        baseHeight + 15,              // baseHeight - volcanic buildup
                        20,                           // foundationAmplitude - rough volcanic terrain
                        12,                           // largeFeatureAmplitude - lava domes
                        8,                            // mediumFeatureAmplitude - volcanic features
                        4,                            // fineDetailAmplitude - rough surface
                        18,                           // carvingAmplitude - lava channels
                        0                             // windAmplitude - no wind features
                );
            };
        }

        // Fallback for unknown biomes
        return new BiomeOctaveSettings(baseHeight, 8, 6, 3, 1, 0, 0);
    }

    private IBiomeType getBiomeTypeAt(int worldX, int worldZ) {
        try {
            if (biomeSource instanceof DesertBiomeSource desertSource) {
                return desertSource.getBiomeTypeAt(worldX, worldZ);
            }

            if (planetModel != null) {
                return switch (planetModel.getType()) {
                    case DESERT -> DesertBiomeType.DUNE_SEA;
                    default -> DesertBiomeType.DUNE_SEA;
                };
            }

            return DesertBiomeType.DUNE_SEA;
        } catch (Exception e) {
            return DesertBiomeType.DUNE_SEA;
        }
    }

    private BlockState getPrimaryBlockForBiome(IBiomeType biomeType) {
        if (planetModel.getType() == PlanetType.DESERT) {
            DesertConfig config = ((DesertModel) planetModel).getConfig();
            if (config.getSurfaceTemperature() > 45) {
                return Blocks.RED_SAND.getDefaultState();
            } else {
                return Blocks.SAND.getDefaultState();
            }
        }

        return Blocks.STONE.getDefaultState();
    }

    private void fillUnifiedTerrainColumn(Chunk chunk, int x, int z, UnifiedTerrainResult result) {
        for (int y = chunk.getBottomY(); y <= result.finalHeight + 5; y++) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState blockState;

            if (y <= result.bedrockHeight) {
                blockState = getBedrockBlock();
            } else if (y <= result.finalHeight) {
                blockState = result.primaryBlock;
            } else {
                blockState = Blocks.AIR.getDefaultState();
            }

            if (blockState != Blocks.AIR.getDefaultState()) {
                chunk.setBlockState(pos, blockState, false);
            }
        }
    }

    private BlockState getBedrockBlock() {
        return switch (planetModel.getType()) {
            case VOLCANIC -> Blocks.BASALT.getDefaultState();
            case ICY -> Blocks.PACKED_ICE.getDefaultState();
            case DESERT -> Blocks.SANDSTONE.getDefaultState();
            default -> Blocks.DEEPSLATE.getDefaultState();
        };
    }

    // Data classes
    private static class BiomeOctaveSettings {
        final double baseHeight;
        final double foundationAmplitude;      // Large-scale base terrain
        final double largeFeatureAmplitude;    // Major features (big dunes, mesas)
        final double mediumFeatureAmplitude;   // Medium features (dune ridges)
        final double fineDetailAmplitude;     // Fine details (ripples, texture)
        final double carvingAmplitude;        // Carving/erosion depth
        final double windAmplitude;           // Wind-aligned features

        BiomeOctaveSettings(double baseHeight, double foundationAmplitude, double largeFeatureAmplitude,
                            double mediumFeatureAmplitude, double fineDetailAmplitude,
                            double carvingAmplitude, double windAmplitude) {
            this.baseHeight = baseHeight;
            this.foundationAmplitude = foundationAmplitude;
            this.largeFeatureAmplitude = largeFeatureAmplitude;
            this.mediumFeatureAmplitude = mediumFeatureAmplitude;
            this.fineDetailAmplitude = fineDetailAmplitude;
            this.carvingAmplitude = carvingAmplitude;
            this.windAmplitude = windAmplitude;
        }
    }

    private static class UnifiedTerrainResult {
        final int bedrockHeight;
        final int finalHeight;
        final BlockState primaryBlock;

        UnifiedTerrainResult(int bedrockHeight, int finalHeight, BlockState primaryBlock) {
            this.bedrockHeight = bedrockHeight;
            this.finalHeight = finalHeight;
            this.primaryBlock = primaryBlock;
        }
    }

    // Standard ChunkGenerator methods
    @Override
    public void carve(ChunkRegion chunkRegion, long seed, NoiseConfig noiseConfig, BiomeAccess biomeAccess, StructureAccessor structureAccessor, Chunk chunk, GenerationStep.Carver carverStep) {}

    @Override
    public void buildSurface(ChunkRegion region, StructureAccessor structures, NoiseConfig noiseConfig, Chunk chunk) {}

    @Override
    public void populateEntities(ChunkRegion region) {}

    @Override
    public int getWorldHeight() { return 384; }

    @Override
    public int getSeaLevel() { return 50; }

    @Override
    public int getMinimumY() { return -64; }

    @Override
    public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
        IBiomeType biomeType = getBiomeTypeAt(x, z);
        UnifiedTerrainResult result = generateUnifiedHeight(x, z, biomeType);
        return result.finalHeight;
    }

    @Override
    public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
        IBiomeType biomeType = getBiomeTypeAt(x, z);
        UnifiedTerrainResult result = generateUnifiedHeight(x, z, biomeType);

        BlockState[] column = new BlockState[world.getHeight()];
        for (int y = 0; y < world.getHeight(); y++) {
            int worldY = world.getBottomY() + y;
            if (worldY <= result.bedrockHeight) {
                column[y] = getBedrockBlock();
            } else if (worldY <= result.finalHeight) {
                column[y] = result.primaryBlock;
            } else {
                column[y] = Blocks.AIR.getDefaultState();
            }
        }

        return new VerticalBlockSample(world.getBottomY(), column);
    }

    @Override
    public void getDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {
        text.add("Unified Octave Terrain Generation");
        text.add("Planet: " + (planetModel != null ? planetModel.getConfig().getPlanetName() : "Unknown"));
        text.add("Single master noise function with biome-controlled octaves");
    }
}