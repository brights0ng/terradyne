package net.starlight.terradyne.planet.terrain.pass;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.starlight.terradyne.planet.biome.IBiomeType;
import net.starlight.terradyne.planet.terrain.MasterNoiseProvider;
import net.starlight.terradyne.planet.terrain.OctaveContext;

public class CanyonCarvingPass implements IGenerationPass {

    @Override
    public void applyPass(Chunk chunk, IBiomeType biome, OctaveContext context, PassConfiguration config) {
        int canyonFloor = config.getInt("canyonFloor", 25);
        double threshold = config.getDouble("threshold", 0.05);
        double canyonWidth = config.getDouble("canyonWidth", 0.8);
        double canyonDensity = config.getDouble("canyonDensity", 0.6);
        double branchingFactor = config.getDouble("branchingFactor", 0.4);
        double sharpness = config.getDouble("sharpness", 2.0);
        double noiseScale = config.getDouble("noiseScale", 0.3);  // Fixed comment: lower = larger features
        BlockState floorBlock = config.getBlockState("floorBlock", Blocks.SANDSTONE.getDefaultState());

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunk.getPos().getStartX() + x;
                int worldZ = chunk.getPos().getStartZ() + z;

                int surfaceHeight = findSurfaceHeight(chunk, x, z);

                double canyonPattern = createTightCanyonPattern(worldX, worldZ, context,
                        canyonWidth, canyonDensity, branchingFactor, noiseScale);

                canyonPattern = applyCanyonSharpness(canyonPattern, sharpness);

                // ADD SIMPLE EDGE ROUGHENING
                double roughenedPattern = addSimpleEdgeRoughening(canyonPattern, worldX, worldZ, context, threshold);

                if (roughenedPattern < threshold) {
                    double canyonStrength = (threshold - roughenedPattern) / threshold;
                    int depthVariation = (int)(canyonStrength * 5);
                    int actualFloor = Math.max(canyonFloor, canyonFloor + depthVariation - 2);

                    for (int y = surfaceHeight; y >= actualFloor; y--) {
                        chunk.setBlockState(new BlockPos(x, y, z), Blocks.AIR.getDefaultState(), false);
                    }

                    chunk.setBlockState(new BlockPos(x, actualFloor, z), floorBlock, false);

                    if (canyonStrength > 0.8 && context.getNoiseProvider().sampleAt(worldX * 0.1 * noiseScale, 0, worldZ * 0.1 * noiseScale) > 0.8) {
                        chunk.setBlockState(new BlockPos(x, actualFloor + 1, z), Blocks.COBBLESTONE.getDefaultState(), false);
                    }
                }
            }
        }
    }

    /**
     * Add simple edge roughening to break up perfect curves - 2X ROUGHER
     */
    private double addSimpleEdgeRoughening(double canyonPattern, int worldX, int worldZ,
                                           OctaveContext context, double threshold) {
        // Only roughen edges near the canyon threshold - slightly wider area
        double distanceFromEdge = Math.abs(canyonPattern - threshold);

        // Expanded roughening zone for more noticeable effect
        if (distanceFromEdge < 0.05) {  // Increased from 0.03
            // DOUBLED roughening intensities
            double edgeRoughness1 = context.getNoiseProvider().sampleAt(worldX * 0.05, 0, worldZ * 0.05) * 0.030;  // Was 0.015
            double edgeRoughness2 = context.getNoiseProvider().sampleAt(worldX * 0.08, 0, worldZ * 0.09) * 0.016;  // Was 0.008
            double edgeRoughness3 = context.getNoiseProvider().sampleAt(worldX * 0.12, 0, worldZ * 0.11) * 0.010;  // Was 0.005

            // Add an extra layer of medium-scale roughening
            double mediumRoughness = context.getNoiseProvider().sampleAt(worldX * 0.035, 0, worldZ * 0.04) * 0.020;

            // Combine all roughening
            double totalRoughness = edgeRoughness1 + edgeRoughness2 + edgeRoughness3 + mediumRoughness;

            // Apply roughening more strongly the closer we are to the edge
            double roughnessStrength = (0.05 - distanceFromEdge) / 0.05; // 0 to 1, wider zone

            return canyonPattern + (totalRoughness * roughnessStrength);
        }

        // Far from edges, no roughening needed
        return canyonPattern;
    }

    /**
     * Updated with configurable noise scale
     */
    private double createTightCanyonPattern(int worldX, int worldZ, OctaveContext context,
                                            double widthFactor, double densityFactor, double branchingFactor, double noiseScale) {
        MasterNoiseProvider noise = context.getNoiseProvider();

        // Pass noise scale to all channel creation
        double primaryCanyon = createTightChannel(noise, worldX, worldZ,
                (0.0018 / densityFactor) * noiseScale, 1.5, widthFactor, noiseScale);

        double secondaryCanyon = 1.0;
        if (densityFactor > 0.5) {
            secondaryCanyon = createTightChannel(noise, worldX, worldZ,
                    (0.003 / densityFactor) * noiseScale, 1.2, widthFactor * 0.8, noiseScale);
        }

        double branches = 1.0;
        if (branchingFactor > 0.3) {
            branches = createIrregularBranches(noise, worldX, worldZ, widthFactor, branchingFactor, noiseScale);
        }

        double connections = createNaturalConnections(noise, worldX, worldZ, widthFactor, noiseScale);

        double combinedCanyon = Math.min(primaryCanyon,
                Math.min(secondaryCanyon,
                        Math.min(branches, connections)));

        // Scale the natural variation frequencies
        double naturalVariation = noise.sampleAt(worldX * 0.02 * noiseScale, 0, worldZ * 0.025 * noiseScale) * 0.06;
        double microVariation = noise.sampleAt(worldX * 0.08 * noiseScale, 0, worldZ * 0.09 * noiseScale) * 0.03;

        return combinedCanyon + naturalVariation + microVariation;
    }

    /**
     * Updated createTightChannel with configurable noise scale
     */
    private double createTightChannel(MasterNoiseProvider noise, int worldX, int worldZ,
                                      double frequency, double meandering, double widthFactor, double noiseScale) {
        // Scale ALL noise frequencies by noiseScale
        double meander1 = noise.sampleAt(worldX * frequency * 0.4 * noiseScale, 0, worldZ * frequency * 0.3 * noiseScale) * meandering;
        double meander2 = noise.sampleAt(worldX * frequency * 0.7 * noiseScale, 0, worldZ * frequency * 0.6 * noiseScale) * meandering * 0.5;

        // Scale irregularity frequencies
        double irregularity1 = noise.sampleAt(worldX * frequency * 2.0 * noiseScale, 0, worldZ * frequency * 1.8 * noiseScale) * meandering * 0.3;
        double irregularity2 = noise.sampleAt(worldX * frequency * 3.5 * noiseScale, 0, worldZ * frequency * 2.9 * noiseScale) * meandering * 0.2;
        double irregularity3 = noise.sampleAt(worldX * frequency * 6.0 * noiseScale, 0, worldZ * frequency * 5.2 * noiseScale) * meandering * 0.1;

        double totalMeanderX = meander1 + meander2 * 0.5 + irregularity1 + irregularity2 + irregularity3;
        double totalMeanderZ = meander1 * 0.8 + meander2 + irregularity1 * 0.7 + irregularity2 + irregularity3;

        double offsetX = worldX + totalMeanderX * 30.0;
        double offsetZ = worldZ + totalMeanderZ * 25.0;

        double baseRidge = noise.sampleRidge(offsetX * frequency, 0, offsetZ * frequency);

        // Scale edge irregularity frequencies
        double edgeNoise1 = noise.sampleAt(worldX * frequency * 8.0 * noiseScale, 0, worldZ * frequency * 7.0 * noiseScale) * 0.15;
        double edgeNoise2 = noise.sampleAt(worldX * frequency * 15.0 * noiseScale, 0, worldZ * frequency * 12.0 * noiseScale) * 0.08;
        double edgeNoise3 = noise.sampleAt(worldX * frequency * 25.0 * noiseScale, 0, worldZ * frequency * 22.0 * noiseScale) * 0.04;

        double edgeIrregularity = edgeNoise1 + edgeNoise2 + edgeNoise3;

        // Scale width variation frequency
        double widthVariation = noise.sampleAt(worldX * frequency * 4.0 * noiseScale, 0, worldZ * frequency * 3.5 * noiseScale) * 0.3 + 0.85;
        double variableWidthFactor = widthFactor * widthVariation;

        // Scale depth variation frequency
        double depthVariation = noise.sampleAt(worldX * frequency * 6.0 * noiseScale, 0, worldZ * frequency * 5.5 * noiseScale) * 0.2;

        double irregularRidge = (baseRidge + edgeIrregularity + depthVariation) / variableWidthFactor;

        return irregularRidge;
    }

    /**
     * Updated helper methods with noise scale
     */
    private double createIrregularBranches(MasterNoiseProvider noise, int worldX, int worldZ,
                                           double widthFactor, double branchingFactor, double noiseScale) {
        double branch1 = createTightChannel(noise, worldX, worldZ, 0.008 * noiseScale, 0.6, widthFactor * branchingFactor, noiseScale);
        double branch2 = createTightChannel(noise, worldX, worldZ, 0.012 * noiseScale, 0.4, widthFactor * branchingFactor * 0.8, noiseScale);

        double branchNoise = noise.sampleAt(worldX * 0.015 * noiseScale, 0, worldZ * 0.018 * noiseScale) * 0.2;

        return Math.min(branch1, branch2) + branchNoise;
    }

    private double createNaturalConnections(MasterNoiseProvider noise, int worldX, int worldZ, double widthFactor, double noiseScale) {
        double connection1 = noise.sampleRidge(worldX * 0.005 * noiseScale, 0, worldZ * 0.0065 * noiseScale) * 0.9;
        double connection2 = noise.sampleRidge(worldX * 0.0075 * noiseScale, 0, worldZ * 0.009 * noiseScale) * 0.8;

        double connectionNoise = noise.sampleAt(worldX * 0.025 * noiseScale, 0, worldZ * 0.03 * noiseScale) * 0.15;

        double baseConnection = Math.min(connection1, connection2);
        return (baseConnection + connectionNoise) / widthFactor;
    }

    // ... (rest of methods stay the same)

    @Override
    public String getParameterDocumentation() {
        return """
        Tight Controlled Canyon Pass Parameters:
        - threshold (double, default 0.05): Canyon carving threshold
        - canyonWidth (double, default 0.8): Width multiplier (0.1-2.0, lower = narrower)
        - canyonDensity (double, default 0.6): Canyon frequency (0.1-1.0, lower = fewer canyons)  
        - branchingFactor (double, default 0.4): Branching amount (0.0-1.0, lower = less branching)
        - sharpness (double, default 2.0): Edge sharpness (1.0-4.0, higher = sharper edges)
        - noiseScale (double, default 0.3): Scale of natural irregularities (LOWER = larger features, HIGHER = finer detail)
        
        Note: Simple edge roughening automatically applied to break up perfect curves
        """;
    }


    /**
     * Create a single tight channel with NATURAL IRREGULARITIES
     */
    private double createTightChannel(MasterNoiseProvider noise, int worldX, int worldZ,
                                      double frequency, double meandering, double widthFactor) {
        // === IRREGULAR MEANDERING ===
        // Base meandering with natural variation
        double meander1 = noise.sampleAt(worldX * frequency * 0.4, 0, worldZ * frequency * 0.3) * meandering;
        double meander2 = noise.sampleAt(worldX * frequency * 0.7, 0, worldZ * frequency * 0.6) * meandering * 0.5;

        // ADD NATURAL IRREGULARITY to meandering
        double irregularity1 = noise.sampleAt(worldX * frequency * 2.0, 0, worldZ * frequency * 1.8) * meandering * 0.3;
        double irregularity2 = noise.sampleAt(worldX * frequency * 3.5, 0, worldZ * frequency * 2.9) * meandering * 0.2;
        double irregularity3 = noise.sampleAt(worldX * frequency * 6.0, 0, worldZ * frequency * 5.2) * meandering * 0.1;

        // Combine smooth meandering with irregular variations
        double totalMeanderX = meander1 + meander2 * 0.5 + irregularity1 + irregularity2 + irregularity3;
        double totalMeanderZ = meander1 * 0.8 + meander2 + irregularity1 * 0.7 + irregularity2 + irregularity3;

        // Irregular offset calculation
        double offsetX = worldX + totalMeanderX * 30.0;
        double offsetZ = worldZ + totalMeanderZ * 25.0;

        // Sample ridge noise
        double baseRidge = noise.sampleRidge(offsetX * frequency, 0, offsetZ * frequency);

        // === ADD CANYON EDGE IRREGULARITY ===
        // Make canyon edges jagged and natural instead of smooth
        double edgeNoise1 = noise.sampleAt(worldX * frequency * 8.0, 0, worldZ * frequency * 7.0) * 0.15;
        double edgeNoise2 = noise.sampleAt(worldX * frequency * 15.0, 0, worldZ * frequency * 12.0) * 0.08;
        double edgeNoise3 = noise.sampleAt(worldX * frequency * 25.0, 0, worldZ * frequency * 22.0) * 0.04;

        double edgeIrregularity = edgeNoise1 + edgeNoise2 + edgeNoise3;

        // === ADD VARIABLE CANYON WIDTH ===
        // Canyon width varies naturally along its length
        double widthVariation = noise.sampleAt(worldX * frequency * 4.0, 0, worldZ * frequency * 3.5) * 0.3 + 0.85;
        double variableWidthFactor = widthFactor * widthVariation;

        // === ADD CANYON DEPTH IRREGULARITY ===
        // Some parts of canyon are deeper/shallower naturally
        double depthVariation = noise.sampleAt(worldX * frequency * 6.0, 0, worldZ * frequency * 5.5) * 0.2;

        // Combine all irregularities
        double irregularRidge = (baseRidge + edgeIrregularity + depthVariation) / variableWidthFactor;

        return irregularRidge;
    }

    /**
     * Create TIGHT, controlled canyon patterns with NATURAL IRREGULARITIES
     */
    private double createTightCanyonPattern(int worldX, int worldZ, OctaveContext context,
                                            double widthFactor, double densityFactor, double branchingFactor) {
        MasterNoiseProvider noise = context.getNoiseProvider();

        // === PRIMARY CANYON with natural irregularities ===
        double primaryCanyon = createTightChannel(noise, worldX, worldZ,
                0.0018 / densityFactor, 1.5, widthFactor);

        // === SECONDARY CHANNEL with different irregular pattern ===
        double secondaryCanyon = 1.0;
        if (densityFactor > 0.5) {
            secondaryCanyon = createTightChannel(noise, worldX, worldZ,
                    0.003 / densityFactor, 1.2, widthFactor * 0.8);
        }

        // === IRREGULAR BRANCHING ===
        double branches = 1.0;
        if (branchingFactor > 0.3) {
            branches = createIrregularBranches(noise, worldX, worldZ, widthFactor, branchingFactor);
        }

        // === NATURAL CANYON CONNECTIONS ===
        // Add irregular connections between canyon systems
        double connections = createNaturalConnections(noise, worldX, worldZ, widthFactor);

        // Combine all canyon elements
        double combinedCanyon = Math.min(primaryCanyon,
                Math.min(secondaryCanyon,
                        Math.min(branches, connections)));

        // === ADD OVERALL NATURAL VARIATION ===
        // Fine-scale natural variation across entire canyon system
        double naturalVariation = noise.sampleAt(worldX * 0.02, 0, worldZ * 0.025) * 0.06;
        double microVariation = noise.sampleAt(worldX * 0.08, 0, worldZ * 0.09) * 0.03;

        return combinedCanyon + naturalVariation + microVariation;
    }

    /**
     * Create irregular branch canyons with natural shapes
     */
    private double createIrregularBranches(MasterNoiseProvider noise, int worldX, int worldZ,
                                           double widthFactor, double branchingFactor) {
        // Multiple irregular branch patterns
        double branch1 = createTightChannel(noise, worldX, worldZ, 0.008, 0.6, widthFactor * branchingFactor);
        double branch2 = createTightChannel(noise, worldX, worldZ, 0.012, 0.4, widthFactor * branchingFactor * 0.8);

        // Add branching irregularity
        double branchNoise = noise.sampleAt(worldX * 0.015, 0, worldZ * 0.018) * 0.2;

        return Math.min(branch1, branch2) + branchNoise;
    }

    /**
     * Create natural connections between canyon systems
     */
    private double createNaturalConnections(MasterNoiseProvider noise, int worldX, int worldZ, double widthFactor) {
        // Natural connecting channels between main canyons
        double connection1 = noise.sampleRidge(worldX * 0.005, 0, worldZ * 0.0065) * 0.9;
        double connection2 = noise.sampleRidge(worldX * 0.0075, 0, worldZ * 0.009) * 0.8;

        // Add connection irregularity
        double connectionNoise = noise.sampleAt(worldX * 0.025, 0, worldZ * 0.03) * 0.15;

        double baseConnection = Math.min(connection1, connection2);
        return (baseConnection + connectionNoise) / widthFactor;
    }

    /**
     * Apply natural sharpness variation instead of uniform sharpness
     */
    private double applyCanyonSharpness(double canyonValue, double sharpness) {
        if (canyonValue < 0.5) {
            // Vary sharpness naturally along canyon
            double sharpnessVariation = Math.sin(canyonValue * Math.PI * 4.0) * 0.3 + 1.0;
            double adjustedSharpness = sharpness * sharpnessVariation;
            return Math.pow(canyonValue, adjustedSharpness);
        } else {
            return canyonValue;
        }
    }

    private int findSurfaceHeight(Chunk chunk, int x, int z) {
        for (int y = 85; y >= 65; y--) {
            if (!chunk.getBlockState(new BlockPos(x, y, z)).isAir()) {
                return y;
            }
        }
        return 75;
    }

    @Override
    public int getPassPriority() { return 20; }

    @Override
    public String getPassName() { return "TightControlledCanyon"; }
}