package net.terradyne.planet.chunk;

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
import net.terradyne.planet.PlanetType;
import net.terradyne.planet.biome.IBiomeType;
import net.terradyne.planet.biome.DesertBiomeType;
import net.terradyne.planet.biome.DesertBiomeSource;
import net.terradyne.planet.config.DesertConfig;
import net.terradyne.planet.model.IPlanetModel;
import net.terradyne.planet.model.DesertModel;
import net.terradyne.planet.terrain.*;
import net.terradyne.planet.terrain.octave.IUnifiedOctave;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Updated Universal Chunk Generator - Now Uses Configuration-Based Octave System!
 *
 * This generator:
 * 1. Creates ONE MasterNoiseProvider per planet
 * 2. Gets biome-configured octaves from OctaveRegistry
 * 3. Applies octaves with their custom parameters
 * 4. Results in smooth, customizable terrain per biome
 */
public class UniversalChunkGenerator extends ChunkGenerator {
    public static final Codec<UniversalChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource)
            ).apply(instance, (biomeSource) -> new UniversalChunkGenerator(null, biomeSource))
    );

    private final IPlanetModel planetModel;
    private final MasterNoiseProvider masterNoiseProvider;

    public UniversalChunkGenerator(IPlanetModel planetModel, BiomeSource biomeSource) {
        super(biomeSource);
        this.planetModel = planetModel;

        if (planetModel != null) {
            // Create THE master noise provider that all octaves will share
            this.masterNoiseProvider = new MasterNoiseProvider(planetModel.getConfig().getSeed());

            // Ensure octave registry is initialized
            OctaveRegistry.initialize();

            Terradyne.LOGGER.info("=== UNIVERSAL CHUNK GENERATOR INITIALIZED ===");
            Terradyne.LOGGER.info("Planet: {}", planetModel.getConfig().getPlanetName());
            Terradyne.LOGGER.info("Type: {}", planetModel.getType().getDisplayName());
            Terradyne.LOGGER.info("Seed: {}", planetModel.getConfig().getSeed());

            // Log available octaves for this planet type
            List<IUnifiedOctave> availableOctaves = OctaveRegistry.getOctavesForPlanetType(planetModel.getType());
            Terradyne.LOGGER.info("Available octaves: {}",
                    availableOctaves.stream().map(IUnifiedOctave::getOctaveName).toList());

        } else {
            // Codec constructor
            this.masterNoiseProvider = null;
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
            generateConfiguredTerrain(chunk, noiseConfig);
            return chunk;
        }, executor);
    }

    /**
     * NEW CONFIGURATION-BASED TERRAIN GENERATION
     */
    private void generateConfiguredTerrain(Chunk chunk, NoiseConfig noiseConfig) {
        if (planetModel == null || masterNoiseProvider == null) {
            generateFallbackTerrain(chunk);
            return;
        }

        ChunkPos chunkPos = chunk.getPos();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkPos.getStartX() + x;
                int worldZ = chunkPos.getStartZ() + z;

                // Get biome type at this location
                IBiomeType biomeType = getBiomeTypeAt(worldX, worldZ);

                // Generate height using configured octaves
                ConfiguredTerrainResult result = generateHeightUsingConfiguredOctaves(worldX, worldZ, biomeType);

                // Fill the terrain column
                fillTerrainColumn(chunk, x, z, result);
            }
        }
    }

    /**
     * Generate height using biome-configured octaves with their parameters
     */
    private ConfiguredTerrainResult generateHeightUsingConfiguredOctaves(int x, int z, IBiomeType biomeType) {
        // Get configured octaves for this biome
        List<OctaveRegistry.ConfiguredOctave> configuredOctaves =
                OctaveRegistry.getConfiguredOctavesForBiome(biomeType, planetModel.getType());

        if (configuredOctaves.isEmpty()) {
            Terradyne.LOGGER.warn("No configured octaves available for biome {} on planet type {}",
                    biomeType.getName(), planetModel.getType());
            return createFallbackResult();
        }

        // Create unified context with master noise provider
        UnifiedOctaveContext context = new UnifiedOctaveContext(
                planetModel,
                biomeType,
                masterNoiseProvider,
                getBaseHeight()
        );

        // Apply all configured octaves
        double totalHeight = context.getBaseFoundationHeight();

        for (OctaveRegistry.ConfiguredOctave configuredOctave : configuredOctaves) {
            try {
                double contribution = configuredOctave.octave.generateHeightContribution(
                        x, z, context, configuredOctave.config);
                totalHeight += contribution;

                // Debug logging for first few chunks
                if (Math.abs(x) < 32 && Math.abs(z) < 32 && Math.abs(contribution) > 0.1) {
                    Terradyne.LOGGER.debug("Octave {} contributed {} at {},{} (total: {})",
                            configuredOctave.octave.getOctaveName(),
                            String.format("%.2f", contribution),
                            x, z,
                            String.format("%.2f", totalHeight));
                }

            } catch (Exception e) {
                Terradyne.LOGGER.error("Error in configured octave {}: {}",
                        configuredOctave.octave.getOctaveName(), e.getMessage());
                e.printStackTrace();
            }
        }

        int finalHeight = Math.max(getMinHeight(), (int) totalHeight);
        int bedrockHeight = (int) context.getBaseFoundationHeight();
        BlockState primaryBlock = getPrimaryBlockForBiome(biomeType);

        return new ConfiguredTerrainResult(bedrockHeight, finalHeight, primaryBlock, configuredOctaves);
    }

    /**
     * Get base height for the planet type
     */
    private double getBaseHeight() {
        return switch (planetModel.getType()) {
            case DESERT, HOTHOUSE -> 55.0;
            case OCEANIC -> 45.0;  // Lower for ocean worlds
            case ROCKY -> 60.0;    // Higher for rocky worlds
            case VOLCANIC -> 70.0; // Higher for volcanic buildup
            case ICY -> 50.0;      // Lower for ice worlds
            default -> 55.0;
        };
    }

    /**
     * Get minimum world height
     */
    private int getMinHeight() {
        return switch (planetModel.getType()) {
            case OCEANIC -> 20;  // Allow for deep oceans
            case VOLCANIC -> 30; // Lava flow channels
            default -> 35;
        };
    }

    /**
     * Get biome type at world coordinates
     */
    private IBiomeType getBiomeTypeAt(int worldX, int worldZ) {
        try {
            if (biomeSource instanceof DesertBiomeSource desertSource) {
                return desertSource.getBiomeTypeAt(worldX, worldZ);
            }

            // Fallback based on planet type
            return switch (planetModel.getType()) {
                case DESERT, HOTHOUSE -> DesertBiomeType.DUNE_SEA;
                // TODO: Add other planet type fallbacks when you create their biome types
                default -> DesertBiomeType.DUNE_SEA;
            };

        } catch (Exception e) {
            Terradyne.LOGGER.warn("Error getting biome type at {},{}: {}", worldX, worldZ, e.getMessage());
            return DesertBiomeType.DUNE_SEA;
        }
    }

    /**
     * Select appropriate block for biome
     */
    private BlockState getPrimaryBlockForBiome(IBiomeType biomeType) {
        if (planetModel.getType() == PlanetType.DESERT || planetModel.getType() == PlanetType.HOTHOUSE) {
            DesertConfig config = ((DesertModel) planetModel).getConfig();

            if (biomeType instanceof DesertBiomeType desertBiome) {
                return switch (desertBiome) {
                    case GRANITE_MESAS -> Blocks.STONE.getDefaultState();
                    case LIMESTONE_CANYONS -> Blocks.CALCITE.getDefaultState();
                    case SALT_FLATS -> Blocks.WHITE_CONCRETE_POWDER.getDefaultState();
                    case DUNE_SEA -> Blocks.SANDSTONE.getDefaultState();
                    default -> config.getSurfaceTemperature() > 45 ?
                            Blocks.RED_SAND.getDefaultState() : Blocks.SAND.getDefaultState();
                };
            }

            return config.getSurfaceTemperature() > 45 ?
                    Blocks.RED_SAND.getDefaultState() : Blocks.SAND.getDefaultState();
        }

        return switch (planetModel.getType()) {
            case OCEANIC -> Blocks.STONE.getDefaultState();
            case ROCKY -> Blocks.COBBLESTONE.getDefaultState();
            case VOLCANIC -> Blocks.BASALT.getDefaultState();
            case ICY -> Blocks.SNOW_BLOCK.getDefaultState();
            default -> Blocks.STONE.getDefaultState();
        };
    }

    /**
     * Fill terrain column with blocks
     */
    private void fillTerrainColumn(Chunk chunk, int x, int z, ConfiguredTerrainResult result) {
        for (int y = chunk.getBottomY(); y <= result.finalHeight + 5; y++) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState blockState;

            // CREATE GRADUAL BEDROCK TRANSITION instead of hard cutoff
            double bedrockTransitionZone = 4.0; // 4 block transition zone
            double distanceAboveBedrock = y - result.bedrockHeight;

            if (distanceAboveBedrock <= 0) {
                // Pure bedrock
                blockState = getBedrockBlock();
            } else if (distanceAboveBedrock < bedrockTransitionZone) {
                // Transition zone - mix bedrock and surface material based on noise
                MasterNoiseProvider noise = masterNoiseProvider;
                double transitionNoise = noise.sampleAt(x * 0.1, y * 0.05, z * 0.1);
                double transitionFactor = distanceAboveBedrock / bedrockTransitionZone;

                // Smooth the transition with noise
                if (transitionNoise < (transitionFactor - 0.5)) {
                    blockState = getBedrockBlock();
                } else {
                    blockState = result.primaryBlock;
                }
            } else if (y <= result.finalHeight) {
                // Pure surface material
                blockState = result.primaryBlock;
            } else {
                blockState = Blocks.AIR.getDefaultState();
            }

            if (blockState != Blocks.AIR.getDefaultState()) {
                chunk.setBlockState(pos, blockState, false);
            }
        }
    }

    /**
     * Get bedrock block based on planet type
     */
    private BlockState getBedrockBlock() {
        return switch (planetModel.getType()) {
            case VOLCANIC -> Blocks.BASALT.getDefaultState();
            case ICY -> Blocks.PACKED_ICE.getDefaultState();
            case DESERT, HOTHOUSE -> Blocks.SANDSTONE.getDefaultState();
            case OCEANIC -> Blocks.DEEPSLATE.getDefaultState();
            case ROCKY -> Blocks.COBBLED_DEEPSLATE.getDefaultState();
            default -> Blocks.DEEPSLATE.getDefaultState();
        };
    }

    /**
     * Fallback terrain generation if system fails
     */
    private void generateFallbackTerrain(Chunk chunk) {
        Terradyne.LOGGER.warn("Using fallback terrain generation");

        ChunkPos chunkPos = chunk.getPos();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = chunk.getBottomY(); y <= 65; y++) {
                    BlockPos pos = new BlockPos(x, y, z);

                    if (y <= 50) {
                        chunk.setBlockState(pos, Blocks.STONE.getDefaultState(), false);
                    } else if (y <= 60) {
                        chunk.setBlockState(pos, Blocks.DIRT.getDefaultState(), false);
                    }
                }
            }
        }
    }

    /**
     * Create fallback result if system fails
     */
    private ConfiguredTerrainResult createFallbackResult() {
        return new ConfiguredTerrainResult(50, 60, Blocks.STONE.getDefaultState(), List.of());
    }

    /**
     * Data class for configured terrain results
     */
    private static class ConfiguredTerrainResult {
        final int bedrockHeight;
        final int finalHeight;
        final BlockState primaryBlock;
        final List<OctaveRegistry.ConfiguredOctave> appliedOctaves;

        ConfiguredTerrainResult(int bedrockHeight, int finalHeight, BlockState primaryBlock,
                                List<OctaveRegistry.ConfiguredOctave> appliedOctaves) {
            this.bedrockHeight = bedrockHeight;
            this.finalHeight = finalHeight;
            this.primaryBlock = primaryBlock;
            this.appliedOctaves = appliedOctaves;
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
    public int getSeaLevel() {
        return switch (planetModel != null ? planetModel.getType() : PlanetType.ROCKY) {
            case OCEANIC -> 60;
            case DESERT, HOTHOUSE -> 45;
            default -> 50;
        };
    }

    @Override
    public int getMinimumY() { return -64; }

    @Override
    public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
        if (planetModel == null) return 60;

        IBiomeType biomeType = getBiomeTypeAt(x, z);
        ConfiguredTerrainResult result = generateHeightUsingConfiguredOctaves(x, z, biomeType);
        return result.finalHeight;
    }

    @Override
    public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
        IBiomeType biomeType = getBiomeTypeAt(x, z);
        ConfiguredTerrainResult result = generateHeightUsingConfiguredOctaves(x, z, biomeType);

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
        text.add("=== CONFIGURED OCTAVE TERRAIN ===");
        if (planetModel != null) {
            text.add("Planet: " + planetModel.getConfig().getPlanetName());
            text.add("Type: " + planetModel.getType().getDisplayName());

            IBiomeType biome = getBiomeTypeAt(pos.getX(), pos.getZ());
            text.add("Biome: " + biome.getName());

            List<OctaveRegistry.ConfiguredOctave> configuredOctaves =
                    OctaveRegistry.getConfiguredOctavesForBiome(biome, planetModel.getType());
            text.add("Configured Octaves: " + configuredOctaves.size());

            for (OctaveRegistry.ConfiguredOctave configuredOctave : configuredOctaves) {
                text.add("  " + configuredOctave.octave.getOctaveName() +
                        " " + configuredOctave.config.getAllParameters().toString());
            }
        } else {
            text.add("No planet model loaded");
        }
    }
}