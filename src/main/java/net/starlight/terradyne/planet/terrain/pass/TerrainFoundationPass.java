package net.starlight.terradyne.planet.terrain.pass;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.starlight.terradyne.planet.biome.IBiomeType;
import net.starlight.terradyne.planet.terrain.OctaveConfiguration;
import net.starlight.terradyne.planet.terrain.OctaveContext;
import net.starlight.terradyne.planet.terrain.OctaveRegistry;
import net.starlight.terradyne.planet.terrain.octave.FoundationOctave;
import net.starlight.terradyne.planet.terrain.octave.RollingTerrainOctave;
import net.starlight.terradyne.planet.terrain.octave.MesaOctave;

/**
 * SMOOTH WAVY Terrain Foundation Pass - No ugly bumps, natural hills
 */
public class TerrainFoundationPass implements IGenerationPass {

    @Override
    public void applyPass(Chunk chunk, IBiomeType biome, OctaveContext context, PassConfiguration config) {
        // Get octaves
        FoundationOctave foundationOctave = (FoundationOctave) OctaveRegistry.getOctave(FoundationOctave.class);
        RollingTerrainOctave rollingOctave = (RollingTerrainOctave) OctaveRegistry.getOctave(RollingTerrainOctave.class);
        MesaOctave mesaOctave = (MesaOctave) OctaveRegistry.getOctave(MesaOctave.class);

        if (foundationOctave == null || rollingOctave == null) {
            throw new RuntimeException("Required octaves not found for TerrainFoundationPass");
        }

        // Get configuration
        BlockState blockType = config.getBlockState("blockType", Blocks.SAND.getDefaultState());
        int baseSeaLevel = config.getInt("baseSeaLevel", 64);
        boolean createMesaMounds = config.getBoolean("createMesaMounds", false);

        // GENTLE DUNE-LIKE terrain configurations
        OctaveConfiguration foundationConfig = new OctaveConfiguration(FoundationOctave.class)
                .withParameter("amplitude", config.getDouble("foundation.amplitude", 6.0))  // REDUCED for gentler base
                .withParameter("frequency", config.getDouble("foundation.frequency", 0.0008)); // HIGHER frequency for smaller features

        OctaveConfiguration rollingConfig = new OctaveConfiguration(RollingTerrainOctave.class)
                .withParameter("hillHeight", config.getDouble("rolling.hillHeight", 15.0))    // MUCH REDUCED for gentle dunes
                .withParameter("hillFrequency", config.getDouble("rolling.hillFrequency", 0.006)) // HIGHER frequency for smaller dunes
                .withParameter("rockOutcropIntensity", config.getDouble("rolling.rockOutcropIntensity", 0.05)) // MINIMAL outcrops
                .withParameter("washDepth", config.getDouble("rolling.washDepth", 1.0))       // Gentle washes
                .withParameter("undulationStrength", config.getDouble("rolling.undulationStrength", 0.8)); // Reduced undulation

        // Mesa mound configuration (WIDER mesas)
        OctaveConfiguration mesaMoundConfig = null;
        if (createMesaMounds && mesaOctave != null) {
            mesaMoundConfig = new OctaveConfiguration(MesaOctave.class)
                    .withParameter("mesaHeight", config.getDouble("mesa.mesaHeight", 80.0))
                    .withParameter("plateauFrequency", config.getDouble("mesa.plateauFrequency", 0.003)) // LOWER = WIDER mesas
                    .withParameter("steepness", config.getDouble("mesa.steepness", 8.0))      // LOWER = broader slopes
                    .withParameter("erosionIntensity", config.getDouble("mesa.erosionIntensity", 0.3))
                    .withParameter("layering", config.getDouble("mesa.layering", 1.0));
        }

        // Generate gentle dune terrain
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunk.getPos().getStartX() + x;
                int worldZ = chunk.getPos().getStartZ() + z;

                // Calculate gentle dune terrain
                double foundationHeight = foundationOctave.generateHeightContribution(worldX, worldZ, context, foundationConfig);
                double rollingHeight = rollingOctave.generateHeightContribution(worldX, worldZ, context, rollingConfig);

                // Create gentle dune patterns
                double gentleDuneHeight = createGentleDunePattern(foundationHeight, rollingHeight, worldX, worldZ, context);

                double baseTerrainHeight = gentleDuneHeight;

                // Add mesa-influenced sand
                double finalHeight = baseTerrainHeight;

                if (createMesaMounds && mesaMoundConfig != null) {
                    double mesaInfluence = calculateSeamlessSandInfluence(worldX, worldZ, context, config, mesaMoundConfig);
                    finalHeight = blendWithExistingTerrain(baseTerrainHeight, gentleDuneHeight, mesaInfluence, worldX, worldZ, context);
                }

                // CRITICAL FIX: Ensure no hard cutoffs, allow natural variation
                double naturalTerrainHeight = finalHeight; // No forced minimum!
                int absoluteHeight = baseSeaLevel + (int) naturalTerrainHeight; // Can go below sea level

                // FIXED: Fill terrain with natural variation (no y 75 cutoff)
                fillFlexibleTerrainColumn(chunk, x, z, absoluteHeight, baseSeaLevel, blockType);
            }
        }
    }

    /**
     * NEW: Create gentle dune-like patterns similar to Dune Sea but less dramatic
     */
    private double createGentleDunePattern(double foundation, double rolling, int worldX, int worldZ, OctaveContext context) {
        // Multiple scales of gentle dune patterns
        double largeDunes = context.getNoiseProvider().sampleAt(worldX * 0.0008, 0, worldZ * 0.0008) * 4.0;   // Large gentle swells
        double mediumDunes = context.getNoiseProvider().sampleAt(worldX * 0.002, 0, worldZ * 0.002) * 2.5;    // Medium dune ridges
        double smallDunes = context.getNoiseProvider().sampleAt(worldX * 0.008, 0, worldZ * 0.008) * 1.0;     // Small ripples
        double microRipples = context.getNoiseProvider().sampleAt(worldX * 0.02, 0, worldZ * 0.02) * 0.5;     // Tiny surface details

        // Combine foundation and rolling with dune patterns
        double combinedHeight = foundation + (rolling * 0.6) + // Reduce sharp rolling
                largeDunes + mediumDunes + smallDunes + microRipples;

        // Apply gentle smoothing to eliminate any remaining spikes
        return Math.tanh(combinedHeight / 12.0) * 12.0;
    }

    /**
     * FIXED: Fill terrain column with complete flexibility (no cutoffs)
     */
    private void fillFlexibleTerrainColumn(Chunk chunk, int x, int z, int surfaceHeight, int seaLevel, BlockState blockType) {
        // Calculate natural terrain base (can vary significantly)
        int terrainBase = Math.max(chunk.getBottomY() + 2, surfaceHeight - 30); // Up to 30 blocks variation

        // ENSURE no artificial cutoffs anywhere
        for (int y = chunk.getBottomY(); y <= Math.max(surfaceHeight, seaLevel + 20) && y < 256; y++) {
            BlockPos pos = new BlockPos(x, y, z);

            if (y <= terrainBase) {
                // Natural bedrock/sandstone base
                chunk.setBlockState(pos, Blocks.SANDSTONE.getDefaultState(), false);
            } else if (y <= surfaceHeight) {
                // Sand layer (can extend below OR above sea level naturally)
                chunk.setBlockState(pos, blockType, false);
            }
            // Above surface = air (don't place blocks)
        }
    }

    // ... (keep existing mesa influence and blending methods but update them for broader mesas)

    /**
     * UPDATED: Mesa sand influence for BROADER mesas
     */
    private double calculateSeamlessSandInfluence(int worldX, int worldZ, OctaveContext context,
                                                  PassConfiguration config, OctaveConfiguration mesaConfig) {
        double mesaStrength = ((MesaOctave) OctaveRegistry.getOctave(MesaOctave.class))
                .generateHeightContribution(worldX, worldZ, context, mesaConfig);

        double maxMesaHeight = config.getDouble("mesa.mesaHeight", 80.0);
        double sharpnessThreshold = Math.pow(Math.max(0, mesaStrength / maxMesaHeight), 1.5); // REDUCED power for broader influence
        double moundThreshold = config.getDouble("mesaMounds.threshold", 0.015); // LOWER for broader influence

        if (sharpnessThreshold > moundThreshold) {
            // Calculate sand influence with broader patterns
            double sandInfluenceFactor;

            if (sharpnessThreshold > 0.3) {
                sandInfluenceFactor = 0.6; // Broader mesa center influence
            } else if (sharpnessThreshold > 0.1) {
                // MUCH broader mesa edge zone
                sandInfluenceFactor = 0.4 + (sharpnessThreshold * 1.0); // 0.4 to 1.4
            } else {
                // Extended broader influence
                double falloffFactor = sharpnessThreshold / 0.1;
                sandInfluenceFactor = falloffFactor * 0.4; // 0 to 0.4
            }

            // Base sand influence
            double baseInfluence = mesaStrength * config.getDouble("mesaMounds.heightScale", 0.8);

            // Gentle sand variation for broader patterns
            double broadSandNoise = context.getNoiseProvider().sampleAt(worldX * 0.005, 0, worldZ * 0.005) * 3.0;
            double mediumSandNoise = context.getNoiseProvider().sampleAt(worldX * 0.015, 0, worldZ * 0.015) * 1.5;

            return (baseInfluence * sandInfluenceFactor) + (broadSandNoise + mediumSandNoise) * 0.3;
        }

        return 0;
    }

    /**
     * UPDATED: Terrain blending (same logic but cleaner)
     */
    private double blendWithExistingTerrain(double baseHeight, double rollingHeight, double mesaInfluence,
                                            int worldX, int worldZ, OctaveContext context) {
        if (mesaInfluence <= 0) {
            return baseHeight;
        }

        // Gentle transition zones
        double transitionNoise1 = context.getNoiseProvider().sampleAt(worldX * 0.006, 0, worldZ * 0.006) * 2.0;
        double transitionNoise2 = context.getNoiseProvider().sampleAt(worldX * 0.015, 0, worldZ * 0.015) * 1.0;

        double combinedTransition = transitionNoise1 + transitionNoise2;

        // Ensure terrain continuity
        double baseFoundation = baseHeight - rollingHeight;
        double enhancedRolling = rollingHeight + (mesaInfluence * 0.4);
        double sandAccumulation = mesaInfluence * 0.6;

        double blendFactor = Math.max(0.3, Math.min(0.7, (combinedTransition + 3.0) / 6.0));

        double continuousHeight = baseFoundation +
                Math.max(rollingHeight, enhancedRolling * blendFactor + rollingHeight * (1.0 - blendFactor)) +
                (sandAccumulation * blendFactor);

        return Math.max(baseHeight, continuousHeight);
    }

    @Override
    public int getPassPriority() { return 0; }

    @Override
    public String getPassName() { return "SmoothWavyTerrainFoundation"; }

    @Override
    public String getParameterDocumentation() {
        return """
            Smooth Wavy Terrain Foundation Pass:
            - baseSeaLevel (int, default 64): Base sea level (replaces hard minHeight)
            - Generates smooth, wavy terrain with no ugly bumps
            - Natural terrain bottom (no hard cutoff at y 76)
            - Enhanced rolling terrain for more hills and waves
            - Smooth terrain filtering to eliminate spikes
            """;
    }
}