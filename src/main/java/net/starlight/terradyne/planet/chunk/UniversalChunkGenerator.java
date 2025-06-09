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
import net.starlight.terradyne.planet.terrain.pass.PassRegistry;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * UPDATED Universal Chunk Generator with Pass-Based Generation System
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

            // Initialize BOTH systems
            OctaveRegistry.initialize();  // For octaves used by passes
            PassRegistry.initialize();    // NEW: For pass system

            Terradyne.LOGGER.info("=== UNIVERSAL CHUNK GENERATOR INITIALIZED ===");
            Terradyne.LOGGER.info("Planet: {}", planetModel.getConfig().getPlanetName());
            Terradyne.LOGGER.info("Type: {}", planetModel.getType().getDisplayName());
            Terradyne.LOGGER.info("Seed: {}", planetModel.getConfig().getSeed());
            Terradyne.LOGGER.info("Generation System: PASS-BASED"); // NEW

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
            generatePassBasedTerrain(chunk, noiseConfig);  // CHANGED: Use pass system
            return chunk;
        }, executor);
    }

    /**
     * NEW PASS-BASED TERRAIN GENERATION
     * Replaces the old octave-based height calculation system
     */
    private void generatePassBasedTerrain(Chunk chunk, NoiseConfig noiseConfig) {
        if (planetModel == null || masterNoiseProvider == null) {
            generateFallbackTerrain(chunk);
            return;
        }

        try {
            // Get the biome for this chunk (sample center point)
            ChunkPos chunkPos = chunk.getPos();
            int centerX = chunkPos.getStartX() + 8;
            int centerZ = chunkPos.getStartZ() + 8;
            IBiomeType biomeType = getBiomeTypeAt(centerX, centerZ);

            // Create unified context with master noise provider
            OctaveContext context = new OctaveContext(
                    planetModel,
                    biomeType,
                    masterNoiseProvider,
                    getBaseHeight()
            );

            // Get configured passes for this biome
            List<PassRegistry.ConfiguredPass> passes = PassRegistry.getConfiguredPassesForBiome(biomeType);

            if (passes.isEmpty()) {
                Terradyne.LOGGER.warn("No passes configured for biome {} - using fallback", biomeType.getName());
                generateFallbackTerrain(chunk);
                return;
            }

            Terradyne.LOGGER.debug("Generating chunk {} with {} passes for biome {}",
                    chunkPos, passes.size(), biomeType.getName());

            // Apply each pass in priority order
            for (PassRegistry.ConfiguredPass configuredPass : passes) {
                try {
                    configuredPass.pass.applyPass(chunk, biomeType, context, configuredPass.config);

                    Terradyne.LOGGER.debug("Applied pass: {} (priority {})",
                            configuredPass.pass.getPassName(),
                            configuredPass.config.getPriority());

                } catch (Exception e) {
                    Terradyne.LOGGER.error("Error applying pass {} to chunk {}: {}",
                            configuredPass.pass.getPassName(),
                            chunkPos,
                            e.getMessage());
                    e.printStackTrace();
                    // Continue with other passes even if one fails
                }
            }

        } catch (Exception e) {
            Terradyne.LOGGER.error("Critical error in pass-based terrain generation for chunk {}: {}",
                    chunk.getPos(), e.getMessage());
            e.printStackTrace();
            generateFallbackTerrain(chunk);
        }
    }

    /**
     * LEGACY METHOD - kept for compatibility with other methods that need height
     * This is used by getHeight() and getColumnSample() methods
     */
    private ConfiguredTerrainResult generateHeightUsingConfiguredOctaves(int x, int z, IBiomeType biomeType) {
        // Get configured octaves for this biome
        List<OctaveRegistry.ConfiguredOctave> configuredOctaves =
                OctaveRegistry.getConfiguredOctavesForBiome(biomeType, planetModel.getType());

        if (configuredOctaves.isEmpty()) {
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

            } catch (Exception e) {
                Terradyne.LOGGER.error("Error in configured octave {}: {}",
                        configuredOctave.octave.getOctaveName(), e.getMessage());
            }
        }

        int finalHeight = Math.max(getMinHeight(), (int) totalHeight);
        int bedrockHeight = (int) context.getBaseFoundationHeight();
        BlockState primaryBlock = Blocks.SAND.getDefaultState();

        return new ConfiguredTerrainResult(bedrockHeight, finalHeight, primaryBlock, biomeType, configuredOctaves);
    }

    /**
     * Get base height for the planet type
     */
    private double getBaseHeight() {
        return switch (planetModel.getType()) {
            case DESERT, HOTHOUSE -> 75.0;
            case OCEANIC -> 45.0;
            case ROCKY -> 60.0;
            case VOLCANIC -> 70.0;
            case ICY -> 50.0;
            default -> 75.0;
        };
    }

    /**
     * Get minimum world height
     */
    private int getMinHeight() {
        return switch (planetModel.getType()) {
            case OCEANIC -> 20;
            case VOLCANIC -> 30;
            default -> 30;
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
                default -> DesertBiomeType.DUNE_SEA;
            };

        } catch (Exception e) {
            Terradyne.LOGGER.warn("Error getting biome type at {},{}: {}", worldX, worldZ, e.getMessage());
            return DesertBiomeType.DUNE_SEA;
        }
    }

    /**
     * Fallback terrain generation if system fails
     */
    private void generateFallbackTerrain(Chunk chunk) {
        Terradyne.LOGGER.warn("Using fallback terrain generation for chunk {}", chunk.getPos());

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = chunk.getBottomY(); y <= 75; y++) {
                    BlockPos pos = new BlockPos(x, y, z);

                    if (y <= 50) {
                        chunk.setBlockState(pos, Blocks.STONE.getDefaultState(), false);
                    } else if (y <= 70) {
                        chunk.setBlockState(pos, Blocks.SAND.getDefaultState(), false);
                    }
                }
            }
        }
    }

    /**
     * Create fallback result if system fails
     */
    private ConfiguredTerrainResult createFallbackResult(IBiomeType biomeType) {
        return new ConfiguredTerrainResult(50, 75, Blocks.SAND.getDefaultState(), biomeType, List.of());
    }

    /**
     * Data class for configured terrain results (used by legacy methods)
     */
    private static class ConfiguredTerrainResult {
        final int bedrockHeight;
        final int finalHeight;
        final BlockState primaryBlock;
        final IBiomeType biomeType;
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

    // ============================================================================
    // REQUIRED CHUNK GENERATOR METHODS (use legacy system for compatibility)
    // ============================================================================

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
        if (planetModel == null) return 75;

        IBiomeType biomeType = getBiomeTypeAt(x, z);
        ConfiguredTerrainResult result = generateHeightUsingConfiguredOctaves(x, z, biomeType);
        return result.finalHeight;
    }

    @Override
    public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
        if (planetModel == null) {
            // Fallback column
            BlockState[] column = new BlockState[world.getHeight()];
            for (int y = 0; y < world.getHeight(); y++) {
                int worldY = world.getBottomY() + y;
                if (worldY <= 70) {
                    column[y] = Blocks.SAND.getDefaultState();
                } else {
                    column[y] = Blocks.AIR.getDefaultState();
                }
            }
            return new VerticalBlockSample(world.getBottomY(), column);
        }

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

    @Override
    public void getDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {
        text.add("=== PASS-BASED TERRAIN GENERATION ===");  // UPDATED
        if (planetModel != null) {
            text.add("Planet: " + planetModel.getConfig().getPlanetName());
            text.add("Type: " + planetModel.getType().getDisplayName());

            IBiomeType biome = getBiomeTypeAt(pos.getX(), pos.getZ());
            text.add("Biome: " + biome.getName());

            // NEW: Show pass information instead of octave information
            List<PassRegistry.ConfiguredPass> passes = PassRegistry.getConfiguredPassesForBiome(biome);
            text.add("Generation Passes: " + passes.size());

            for (PassRegistry.ConfiguredPass pass : passes) {
                text.add("  [" + pass.config.getPriority() + "] " + pass.pass.getPassName());
            }

        } else {
            text.add("No planet model loaded");
        }
    }
}