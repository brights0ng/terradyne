package net.starlight.terradyne.planet.terrain.pass;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.starlight.terradyne.planet.biome.IBiomeType;
import net.starlight.terradyne.planet.terrain.OctaveConfiguration;
import net.starlight.terradyne.planet.terrain.OctaveContext;
import net.starlight.terradyne.planet.terrain.OctaveRegistry;
import net.starlight.terradyne.planet.terrain.octave.MesaOctave;

/**
 * HEAVILY ERODED Mesa Override Pass - Pure stone mesas with dramatic erosion
 */
public class MesaOverridePass implements IGenerationPass {

    @Override
    public void applyPass(Chunk chunk, IBiomeType biome, OctaveContext context, PassConfiguration config) {
        MesaOctave mesaOctave = (MesaOctave) OctaveRegistry.getOctave(MesaOctave.class);
        if (mesaOctave == null) {
            throw new RuntimeException("MesaOctave not found for MesaOverridePass");
        }

        // Get configuration
        BlockState mesaBlock = config.getBlockState("blockType", Blocks.STONE.getDefaultState());
        BlockState weatheredBlock = config.getBlockState("weatheredBlock", Blocks.COBBLESTONE.getDefaultState());
        double threshold = config.getDouble("threshold", 0.3);
        double maxMesaHeight = config.getDouble("mesa.mesaHeight", 80.0);
        boolean addSurfaceTexture = config.getBoolean("addSurfaceTexture", true);

        OctaveConfiguration mesaConfig = new OctaveConfiguration(MesaOctave.class)
                .withParameter("mesaHeight", maxMesaHeight)
                .withParameter("plateauFrequency", config.getDouble("mesa.plateauFrequency", 0.005))
                .withParameter("steepness", config.getDouble("mesa.steepness", 12.0))
                .withParameter("erosionIntensity", config.getDouble("mesa.erosionIntensity", 0.5))
                .withParameter("layering", config.getDouble("mesa.layering", 1.0));

        // Generate heavily eroded mesas
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunk.getPos().getStartX() + x;
                int worldZ = chunk.getPos().getStartZ() + z;

                double mesaStrength = mesaOctave.generateHeightContribution(worldX, worldZ, context, mesaConfig);
                double sharpnessThreshold = Math.pow(Math.max(0, mesaStrength / maxMesaHeight), 2.0);

                if (sharpnessThreshold > 0.15) {
                    int existingSurface = findSurfaceHeight(chunk, x, z);

                    // HEAVY EROSION - much more aggressive
                    double erosionEffect = calculateHeavyErosion(worldX, worldZ, context);
                    int totalMesaHeight = existingSurface + (int) Math.max(0, mesaStrength + erosionEffect);

                    // Place PURE STONE mesa (no sand intrusions)
                    for (int y = 0; y <= totalMesaHeight && y < 256; y++) {
                        BlockPos pos = new BlockPos(x, y, z);

                        // Only stone and weathered stone - no sand in mesas!
                        BlockState blockToPlace = selectErodedBlockType(
                                y, existingSurface, totalMesaHeight,
                                worldX, worldZ, context,
                                mesaBlock, weatheredBlock,
                                addSurfaceTexture
                        );

                        chunk.setBlockState(pos, blockToPlace, false);
                    }

                    // Add dramatic rock debris
                    if (config.getBoolean("addRockDebris", true)) {
                        addHeavyRockDebris(chunk, x, z, totalMesaHeight, worldX, worldZ, context, weatheredBlock);
                    }
                }
            }
        }
    }

    /**
     * MUCH MORE AGGRESSIVE erosion for super noisy mesa sides
     */
    private double calculateHeavyErosion(int worldX, int worldZ, OctaveContext context) {
        // MASSIVE SCALE erosion channels
        double hugeChannels = context.getNoiseProvider().sampleRidge(worldX * 0.004, 0, worldZ * 0.004) * -15.0; // HUGE deep channels

        // Large scale erosion channels
        double largeChannels = context.getNoiseProvider().sampleRidge(worldX * 0.008, 0, worldZ * 0.008) * -10.0;

        // Medium scale weathering
        double mediumWeathering = context.getNoiseProvider().sampleRidge(worldX * 0.025, 0, worldZ * 0.025) * -6.0;

        // Fine scale surface erosion
        double fineErosion = context.getNoiseProvider().sampleAt(worldX * 0.08, 0, worldZ * 0.08) * -4.0;

        // EXTRA FINE detail erosion for very noisy surfaces
        double extraFineErosion = context.getNoiseProvider().sampleAt(worldX * 0.15, 0, worldZ * 0.15) * -2.0;
        double microErosion = context.getNoiseProvider().sampleAt(worldX * 0.25, 0, worldZ * 0.25) * -1.0;

        // Dramatic cliff faces and overhangs
        double cliffFaces = context.getNoiseProvider().sampleRidge(worldX * 0.12, 0, worldZ * 0.12);
        double cliffEffect = (cliffFaces < -0.2) ? cliffFaces * -18.0 : 0; // DEEPER vertical cuts

        // Wind erosion patterns (multiple scales)
        double windErosion1 = context.getNoiseProvider().sampleAt(worldX * 0.04, 0, worldZ * 0.04);
        windErosion1 = (windErosion1 > 0.3) ? -(windErosion1 * 8.0) : 0;

        double windErosion2 = context.getNoiseProvider().sampleAt(worldX * 0.09, 0, worldZ * 0.09);
        windErosion2 = (windErosion2 > 0.4) ? -(windErosion2 * 5.0) : 0;

        // Vertical variation (different erosion at different heights)
        double verticalVariation = context.getNoiseProvider().sampleAt(worldX * 0.02, 0, worldZ * 0.02) * 6.0;

        // NEW: Chaotic surface noise for extremely rough texture
        double chaoticNoise1 = context.getNoiseProvider().sampleAt(worldX * 0.18, 0, worldZ * 0.18) * -3.0;
        double chaoticNoise2 = context.getNoiseProvider().sampleAt(worldX * 0.32, 0, worldZ * 0.32) * -1.5;

        return hugeChannels + largeChannels + mediumWeathering + fineErosion + extraFineErosion + microErosion +
                cliffEffect + windErosion1 + windErosion2 + verticalVariation + chaoticNoise1 + chaoticNoise2;
    }

    /**
     * ENHANCED block selection with more variation throughout mesa height
     */
    private BlockState selectErodedBlockType(int y, int existingSurface, int totalMesaHeight,
                                             int worldX, int worldZ, OctaveContext context,
                                             BlockState mesaBlock, BlockState weatheredBlock,
                                             boolean addSurfaceTexture) {

        double heightFactor = (double)(y - existingSurface) / Math.max(1, totalMesaHeight - existingSurface);

        // MULTIPLE scales of erosion variation for super noisy surfaces
        double erosionNoise1 = context.getNoiseProvider().sampleAt(worldX * 0.12, y * 0.08, worldZ * 0.12);
        double erosionNoise2 = context.getNoiseProvider().sampleAt(worldX * 0.20, y * 0.15, worldZ * 0.20);
        double erosionNoise3 = context.getNoiseProvider().sampleAt(worldX * 0.35, y * 0.25, worldZ * 0.35);

        double weatheringNoise1 = context.getNoiseProvider().sampleAt(worldX * 0.18, y * 0.12, worldZ * 0.18);
        double weatheringNoise2 = context.getNoiseProvider().sampleAt(worldX * 0.28, y * 0.20, worldZ * 0.28);

        // Combine all noise for very complex surface
        double combinedErosion = erosionNoise1 + erosionNoise2 * 0.6 + erosionNoise3 * 0.3;
        double combinedWeathering = weatheringNoise1 + weatheringNoise2 * 0.5;

        // Base section (0% - 40%) - Heavy mix of granite and terracotta
        if (heightFactor < 0.4) {
            if (combinedErosion > 0.1 || combinedWeathering > 0.2) { // LOWER thresholds = more variation
                return weatheredBlock; // Terracotta weathering
            }
            return mesaBlock; // Granite
        }

        // Middle section (40% - 80%) - Alternating bands with lots of noise
        else if (heightFactor < 0.8) {
            if (combinedErosion > 0.2 || combinedWeathering > 0.3) { // More weathering
                return weatheredBlock; // Terracotta erosion bands
            }
            return mesaBlock; // Granite
        }

        // Top section (80% - 100%) - VERY heavy surface weathering
        else {
            if (addSurfaceTexture) {
                if (combinedWeathering > -0.1) { // Almost all terracotta on top surfaces
                    return weatheredBlock; // Terracotta
                }
            }
            return mesaBlock; // Granite
        }
    }

    /**
     * Much more dramatic rock debris
     */
    private void addHeavyRockDebris(Chunk chunk, int x, int z, int mesaHeight, int worldX, int worldZ,
                                    OctaveContext context, BlockState debrisBlock) {
        if (mesaHeight > 110) { // More debris on tall mesas
            double debrisNoise = context.getNoiseProvider().sampleAt(worldX * 0.1, 0, worldZ * 0.1);
            if (debrisNoise > 0.3) { // Higher chance of debris
                int debrisHeight = 1 + (int)(debrisNoise * 4); // Up to 4 blocks of debris
                for (int i = 0; i < debrisHeight; i++) {
                    BlockPos debrisPos = new BlockPos(x, mesaHeight + 1 + i, z);
                    if (mesaHeight + 1 + i < 256) {
                        chunk.setBlockState(debrisPos, debrisBlock, false);
                    }
                }
            }
        }
    }

    private int findSurfaceHeight(Chunk chunk, int x, int z) {
        for (int y = 255; y >= 0; y--) {
            if (!chunk.getBlockState(new BlockPos(x, y, z)).isAir()) {
                return y;
            }
        }
        return 0;
    }

    @Override
    public int getPassPriority() { return 10; }

    @Override
    public String getPassName() { return "HeavilyErodedMesaOverride"; }

    @Override
    public String getParameterDocumentation() {
        return """
            Heavily Eroded Mesa Override Pass:
            - Pure stone mesas with NO sand intrusions
            - MUCH more aggressive erosion patterns
            - Deep channels, cliff faces, and wind erosion
            - Heavy surface weathering and debris
            """;
    }
}