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
import net.starlight.terradyne.planet.PlanetType;
import net.starlight.terradyne.planet.biome.IBiomeType;
import net.starlight.terradyne.planet.biome.DesertBiomeType;
import net.starlight.terradyne.planet.biome.DesertBiomeSource;
import net.starlight.terradyne.planet.config.DesertConfig;
import net.starlight.terradyne.planet.model.IPlanetModel;
import net.starlight.terradyne.planet.model.DesertModel;
import net.starlight.terradyne.planet.terrain.MasterNoiseProvider;
import net.starlight.terradyne.planet.terrain.OctaveContext;
import net.starlight.terradyne.planet.terrain.OctaveRegistry;
import net.starlight.terradyne.planet.terrain.octave.IUnifiedOctave;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * COMPLETE UPDATED Universal Chunk Generator with all mesa fixes
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
     * Generate height using biome-configured octaves with their parameters - UPDATED
     */
    private ConfiguredTerrainResult generateHeightUsingConfiguredOctaves(int x, int z, IBiomeType biomeType) {
        // Get configured octaves for this biome
        List<OctaveRegistry.ConfiguredOctave> configuredOctaves =
                OctaveRegistry.getConfiguredOctavesForBiome(biomeType, planetModel.getType());

        if (configuredOctaves.isEmpty()) {
            Terradyne.LOGGER.warn("No configured octaves available for biome {} on planet type {}",
                    biomeType.getName(), planetModel.getType());
            return createFallbackResult(biomeType);
        }

        // Create unified context with master noise provider
        OctaveContext context = new OctaveContext(
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
        BlockState primaryBlock = Blocks.SAND.getDefaultState(); // Temporary - will be calculated in fillTerrainColumn

        return new ConfiguredTerrainResult(bedrockHeight, finalHeight, primaryBlock, biomeType, configuredOctaves);
    }

    /**
     * Get base height for the planet type - UPDATED to 75 for desert
     */
    private double getBaseHeight() {
        return switch (planetModel.getType()) {
            case DESERT, HOTHOUSE -> 75.0;  // CHANGED: 55.0 -> 75.0 to match dune sea
            case OCEANIC -> 45.0;  // Lower for ocean worlds
            case ROCKY -> 60.0;    // Higher for rocky worlds
            case VOLCANIC -> 70.0; // Higher for volcanic buildup
            case ICY -> 50.0;      // Lower for ice worlds
            default -> 75.0;       // CHANGED: 55.0 -> 75.0
        };
    }

    /**
     * Get minimum world height - FIXED to prevent cutoff
     */
    private int getMinHeight() {
        return switch (planetModel.getType()) {
            case OCEANIC -> 20;  // Allow for deep oceans
            case VOLCANIC -> 30; // Lava flow channels
            default -> 30;       // CHANGED: 35 -> 30 to avoid cutoff at 41
        };
    }

    /**
     * Check if a location is within a mesa formation using the same logic as MesaOctave
     */
    private boolean isInMesaFormation(int x, int z) {
        if (masterNoiseProvider == null) return false;

        // Use the same mesa detection logic as MesaOctave
        double plateauFrequency = 0.006; // Same as MesaOctave default
        double steepness = 6.0; // Same as MesaOctave config

        // Same mesa placement logic as in MesaOctave
        double mesaPlacement = masterNoiseProvider.sampleAt(x * plateauFrequency * 0.6, 0, z * plateauFrequency * 0.4);
        double secondaryMesas = masterNoiseProvider.sampleAt(x * plateauFrequency * 1.2, 0, z * plateauFrequency * 0.8) * 0.7;
        double combinedMesaPattern = mesaPlacement + secondaryMesas * 0.6;

        // Same plateau mask calculation
        double plateauMask = Math.pow(Math.max(0.0, combinedMesaPattern + 0.6), steepness);
        plateauMask = Math.min(1.0, plateauMask);

        // Return true if we're in a significant mesa area
        return plateauMask > 0.2;
    }

    /**
     * Select appropriate block for biome - NOW USES MESA PRESENCE, NOT HEIGHT
     */
    private BlockState getPrimaryBlockForBiome(IBiomeType biomeType, int worldX, int worldZ, int worldY) {
        if (planetModel.getType() == PlanetType.DESERT || planetModel.getType() == PlanetType.HOTHOUSE) {
            DesertConfig config = ((DesertModel) planetModel).getConfig();

            if (biomeType instanceof DesertBiomeType desertBiome) {
                // GRANITE MESAS: Use mesa presence to determine block type
                if (desertBiome == DesertBiomeType.GRANITE_MESAS) {
                    // Check if we're actually IN a mesa formation
                    boolean inMesa = isInMesaFormation(worldX, worldZ);

                    if (inMesa) {
                        return Blocks.STONE.getDefaultState(); // Stone for mesa areas
                    } else {
                        // Rolling hills areas = sand, regardless of height
                        return config.getSurfaceTemperature() > 45 ?
                                Blocks.RED_SAND.getDefaultState() : Blocks.SAND.getDefaultState();
                    }
                }

                // Other biome types
                return switch (desertBiome) {
                    case LIMESTONE_CANYONS -> Blocks.CALCITE.getDefaultState();
                    case SALT_FLATS -> Blocks.WHITE_CONCRETE_POWDER.getDefaultState();
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
     * Fill terrain column with blocks - UPDATED to use new block selection
     */
    private void fillTerrainColumn(Chunk chunk, int x, int z, ConfiguredTerrainResult result) {
        int worldX = chunk.getPos().getStartX() + x;
        int worldZ = chunk.getPos().getStartZ() + z;

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
                    // USE NEW BLOCK SELECTION METHOD
                    blockState = getPrimaryBlockForBiome(result.biomeType, worldX, worldZ, y);
                }
            } else if (y <= result.finalHeight) {
                // USE NEW BLOCK SELECTION METHOD
                blockState = getPrimaryBlockForBiome(result.biomeType, worldX, worldZ, y);
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
     * Create fallback result if system fails - UPDATED
     */
    private ConfiguredTerrainResult createFallbackResult(IBiomeType biomeType) {
        return new ConfiguredTerrainResult(50, 60, Blocks.STONE.getDefaultState(), biomeType, List.of());
    }

    /**
     * Data class for configured terrain results - UPDATED to include biome type
     */
    private static class ConfiguredTerrainResult {
        final int bedrockHeight;
        final int finalHeight;
        final BlockState primaryBlock;
        final IBiomeType biomeType;  // NEW: Include biome type for block selection
        final List<OctaveRegistry.ConfiguredOctave> appliedOctaves;

        ConfiguredTerrainResult(int bedrockHeight, int finalHeight, BlockState primaryBlock,
                                IBiomeType biomeType, List<OctaveRegistry.ConfiguredOctave> appliedOctaves) {
            this.bedrockHeight = bedrockHeight;
            this.finalHeight = finalHeight;
            this.primaryBlock = primaryBlock;
            this.biomeType = biomeType;
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
                column[y] = getPrimaryBlockForBiome(result.biomeType, x, z, worldY);
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

            // NEW DEBUG INFO
            if (biome instanceof DesertBiomeType && ((DesertBiomeType) biome) == DesertBiomeType.GRANITE_MESAS) {
                boolean inMesa = isInMesaFormation(pos.getX(), pos.getZ());
                text.add("In Mesa Formation: " + (inMesa ? "YES (Stone)" : "NO (Sand)"));
            }
        } else {
            text.add("No planet model loaded");
        }
    }
}