package net.starlight.terradyne.planet.terrain.pass;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.starlight.terradyne.planet.biome.IBiomeType;
import net.starlight.terradyne.planet.terrain.OctaveConfiguration;
import net.starlight.terradyne.planet.terrain.OctaveContext;
import net.starlight.terradyne.planet.terrain.OctaveRegistry;
import net.starlight.terradyne.planet.terrain.octave.*;
import net.starlight.terradyne.planet.terrain.pass.IGenerationPass;
import net.starlight.terradyne.planet.terrain.pass.PassConfiguration;

/**
 * PLACEMENT: Terrain Foundation Pass
 * Uses FoundationOctave physics to place base terrain blocks
 */
public class TerrainFoundationPass implements IGenerationPass {

    @Override
    public void applyPass(Chunk chunk, IBiomeType biome, OctaveContext context, PassConfiguration config) {
        // Get physics octaves (original foundation octaves)
        FoundationOctave foundationOctave = (FoundationOctave) OctaveRegistry.getOctave(FoundationOctave.class);
        RollingTerrainOctave rollingOctave = (RollingTerrainOctave) OctaveRegistry.getOctave(RollingTerrainOctave.class);
        MesaFormationOctave mesaOctave = (MesaFormationOctave) OctaveRegistry.getOctave(MesaFormationOctave.class);

        if (foundationOctave == null || rollingOctave == null) {
            throw new RuntimeException("Required physics octaves not found");
        }

        // Block placement configuration (original parameters)
        BlockState blockType = config.getBlockState("blockType", Blocks.SAND.getDefaultState());
        int baseSeaLevel = config.getInt("baseSeaLevel", 64);
        boolean createMesaMounds = config.getBoolean("createMesaMounds", false);

        // Physics configuration (original foundation parameters)
        OctaveConfiguration foundationConfig = new OctaveConfiguration(FoundationOctave.class)
                .withParameter("amplitude", config.getDouble("foundation.amplitude", 10.0))
                .withParameter("frequency", config.getDouble("foundation.frequency", 0.001));

        OctaveConfiguration rollingConfig = new OctaveConfiguration(RollingTerrainOctave.class)
                .withParameter("hillHeight", config.getDouble("rolling.hillHeight", 8.0))
                .withParameter("hillFrequency", config.getDouble("rolling.hillFrequency", 0.003))
                .withParameter("rockOutcropIntensity", config.getDouble("rolling.rockOutcropIntensity", 0.3))
                .withParameter("washDepth", config.getDouble("rolling.washDepth", 2.0))
                .withParameter("undulationStrength", config.getDouble("rolling.undulationStrength", 1.0));

        // Mesa configuration (if enabled)
        OctaveConfiguration mesaMoundConfig = null;
        if (createMesaMounds && mesaOctave != null) {
            mesaMoundConfig = new OctaveConfiguration(MesaFormationOctave.class)
                    .withParameter("mesaHeight", config.getDouble("mesa.mesaHeight", 80.0))
                    .withParameter("plateauFrequency", config.getDouble("mesa.plateauFrequency", 0.003))
                    .withParameter("steepness", config.getDouble("mesa.steepness", 8.0))
                    .withParameter("erosionIntensity", config.getDouble("mesa.erosionIntensity", 0.3))
                    .withParameter("layering", config.getDouble("mesa.layering", 1.0));
        }

        // Generate terrain using exact original algorithm
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunk.getPos().getStartX() + x;
                int worldZ = chunk.getPos().getStartZ() + z;

                // Calculate terrain height using original logic
                double foundationHeight = foundationOctave.generateHeightContribution(worldX, worldZ, context, foundationConfig);
                double rollingHeight = rollingOctave.generateHeightContribution(worldX, worldZ, context, rollingConfig);

                // Create gentle dune pattern (original algorithm)
                double gentleDuneHeight = createGentleDunePattern(foundationHeight, rollingHeight, worldX, worldZ, context);
                double baseTerrainHeight = gentleDuneHeight;
                double finalHeight = baseTerrainHeight;

                // Add mesa-influenced sand (if enabled) - original algorithm
                if (createMesaMounds && mesaMoundConfig != null) {
                    double mesaInfluence = calculateSeamlessSandInfluence(worldX, worldZ, context, config, mesaMoundConfig, mesaOctave);
                    finalHeight = blendWithExistingTerrain(baseTerrainHeight, gentleDuneHeight, mesaInfluence, worldX, worldZ, context);
                }

                // Apply original height calculation
                double naturalTerrainHeight = finalHeight;
                int absoluteHeight = baseSeaLevel + (int) naturalTerrainHeight;

                // Fill terrain column with original logic
                fillFlexibleTerrainColumn(chunk, x, z, absoluteHeight, baseSeaLevel, blockType);
            }
        }
    }

    /**
     * Original gentle dune pattern from TerrainFoundationPass
     */
    private double createGentleDunePattern(double foundation, double rolling, int worldX, int worldZ, OctaveContext context) {
        double largeDunes = context.getNoiseProvider().sampleAt(worldX * 0.0008, 0, worldZ * 0.0008) * 4.0;
        double mediumDunes = context.getNoiseProvider().sampleAt(worldX * 0.002, 0, worldZ * 0.002) * 2.5;
        double smallDunes = context.getNoiseProvider().sampleAt(worldX * 0.008, 0, worldZ * 0.008) * 1.0;
        double microRipples = context.getNoiseProvider().sampleAt(worldX * 0.02, 0, worldZ * 0.02) * 0.5;

        double combinedHeight = foundation + (rolling * 0.6) + largeDunes + mediumDunes + smallDunes + microRipples;
        return Math.tanh(combinedHeight / 12.0) * 12.0;
    }

    /**
     * Original mesa sand influence calculation
     */
    private double calculateSeamlessSandInfluence(int worldX, int worldZ, OctaveContext context,
                                                  PassConfiguration config, OctaveConfiguration mesaConfig, MesaFormationOctave mesaOctave) {
        double mesaStrength = mesaOctave.generateHeightContribution(worldX, worldZ, context, mesaConfig);
        double maxMesaHeight = config.getDouble("mesa.mesaHeight", 80.0);
        double sharpnessThreshold = Math.pow(Math.max(0, mesaStrength / maxMesaHeight), 1.5);
        double moundThreshold = config.getDouble("mesaMounds.threshold", 0.015);

        if (sharpnessThreshold > moundThreshold) {
            double sandInfluenceFactor;
            if (sharpnessThreshold > 0.3) {
                sandInfluenceFactor = 0.6;
            } else if (sharpnessThreshold > 0.1) {
                sandInfluenceFactor = 0.4 + (sharpnessThreshold * 1.0);
            } else {
                double falloffFactor = sharpnessThreshold / 0.1;
                sandInfluenceFactor = falloffFactor * 0.4;
            }

            double baseInfluence = mesaStrength * config.getDouble("mesaMounds.heightScale", 0.8);
            double broadSandNoise = context.getNoiseProvider().sampleAt(worldX * 0.005, 0, worldZ * 0.005) * 3.0;
            double mediumSandNoise = context.getNoiseProvider().sampleAt(worldX * 0.015, 0, worldZ * 0.015) * 1.5;

            return (baseInfluence * sandInfluenceFactor) + (broadSandNoise + mediumSandNoise) * 0.3;
        }

        return 0;
    }

    /**
     * Original terrain blending logic
     */
    private double blendWithExistingTerrain(double baseHeight, double rollingHeight, double mesaInfluence,
                                            int worldX, int worldZ, OctaveContext context) {
        if (mesaInfluence <= 0) {
            return baseHeight;
        }

        double transitionNoise1 = context.getNoiseProvider().sampleAt(worldX * 0.006, 0, worldZ * 0.006) * 2.0;
        double transitionNoise2 = context.getNoiseProvider().sampleAt(worldX * 0.015, 0, worldZ * 0.015) * 1.0;
        double combinedTransition = transitionNoise1 + transitionNoise2;

        double baseFoundation = baseHeight - rollingHeight;
        double enhancedRolling = rollingHeight + (mesaInfluence * 0.4);
        double sandAccumulation = mesaInfluence * 0.6;

        double blendFactor = Math.max(0.3, Math.min(0.7, (combinedTransition + 3.0) / 6.0));

        double continuousHeight = baseFoundation +
                Math.max(rollingHeight, enhancedRolling * blendFactor + rollingHeight * (1.0 - blendFactor)) +
                (sandAccumulation * blendFactor);

        return Math.max(baseHeight, continuousHeight);
    }

    /**
     * Original flexible terrain column filling
     */
    private void fillFlexibleTerrainColumn(Chunk chunk, int x, int z, int surfaceHeight, int seaLevel, BlockState blockType) {
        int terrainBase = Math.max(chunk.getBottomY() + 2, surfaceHeight - 30);

        for (int y = chunk.getBottomY(); y <= Math.max(surfaceHeight, seaLevel + 20) && y < 256; y++) {
            BlockPos pos = new BlockPos(x, y, z);

            if (y <= terrainBase) {
                chunk.setBlockState(pos, Blocks.SANDSTONE.getDefaultState(), false);
            } else if (y <= surfaceHeight) {
                chunk.setBlockState(pos, blockType, false);
            }
        }
    }

    @Override
    public int getPassPriority() { return 0; }

    @Override
    public String getPassName() { return "TerrainPlacement"; }

    @Override
    public String getParameterDocumentation() {
        return """
            Terrain Placement Pass - Uses foundation physics to place base terrain
            - bedrockBlock (BlockState): Block type for bedrock layer
            - terrainBlock (BlockState): Block type for main terrain
            - baseSeaLevel (int): Base height for terrain calculations
            - foundation.amplitude/frequency: Physics parameters for foundation octave
            - rolling.hillHeight/hillFrequency: Physics parameters for rolling terrain
            """;
    }
}