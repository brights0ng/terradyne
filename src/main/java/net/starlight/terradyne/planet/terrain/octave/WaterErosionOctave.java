package net.starlight.terradyne.planet.terrain.octave;

import net.starlight.terradyne.planet.PlanetType;
import net.starlight.terradyne.planet.terrain.MasterNoiseProvider;
import net.starlight.terradyne.planet.terrain.OctaveConfiguration;
import net.starlight.terradyne.planet.terrain.OctaveContext;

import java.util.Set;

/**
 * PHYSICS: Water Erosion Octave
 * Simulates water flow and erosion patterns
 * Returns NEGATIVE values where terrain should be carved away
 */
public class WaterErosionOctave implements IUnifiedOctave {

    @Override
    public double generateHeightContribution(int x, int z, OctaveContext context, OctaveConfiguration config) {
        MasterNoiseProvider noise = context.getNoiseProvider();

        // Configuration matching original canyon carving
        double maxDepth = config.getDouble("maxDepth", 40.0);
        double channelFrequency = config.getDouble("channelFrequency", 0.003);
        double meandering = config.getDouble("meandering", 1.2);
        double wallSteepness = config.getDouble("wallSteepness", 3.0);
        double tributaryDensity = config.getDouble("tributaryDensity", 0.8);

        // === CANYON PATTERN FROM ORIGINAL ===
        double canyonPattern = createTightCanyonPattern(x, z, context, config);
        canyonPattern = applyCanyonSharpness(canyonPattern, wallSteepness);

        // Add edge roughening (from original)
        double threshold = config.getDouble("threshold", 0.05);
        double roughenedPattern = addSimpleEdgeRoughening(canyonPattern, x, z, context, threshold);

        if (roughenedPattern < threshold) {
            double canyonStrength = (threshold - roughenedPattern) / threshold;
            return -canyonStrength * maxDepth; // Negative for carving
        }

        return 0.0;
    }

    /**
     * Original tight canyon pattern algorithm
     */
    private double createTightCanyonPattern(int x, int z, OctaveContext context, OctaveConfiguration config) {
        MasterNoiseProvider noise = context.getNoiseProvider();

        double widthFactor = config.getDouble("canyonWidth", 0.8);
        double densityFactor = config.getDouble("canyonDensity", 0.6);
        double branchingFactor = config.getDouble("branchingFactor", 0.4);
        double noiseScale = config.getDouble("noiseScale", 0.3);

        // Primary canyon with natural irregularities (original algorithm)
        double primaryCanyon = createTightChannel(noise, x, z,
                (0.0018 / densityFactor) * noiseScale, 1.5, widthFactor, noiseScale);

        double secondaryCanyon = 1.0;
        if (densityFactor > 0.5) {
            secondaryCanyon = createTightChannel(noise, x, z,
                    (0.003 / densityFactor) * noiseScale, 1.2, widthFactor * 0.8, noiseScale);
        }

        double branches = 1.0;
        if (branchingFactor > 0.3) {
            branches = createIrregularBranches(noise, x, z, widthFactor, branchingFactor, noiseScale);
        }

        double connections = createNaturalConnections(noise, x, z, widthFactor, noiseScale);
        double combinedCanyon = Math.min(primaryCanyon, Math.min(secondaryCanyon, Math.min(branches, connections)));

        // Natural variation (original)
        double naturalVariation = noise.sampleAt(x * 0.02 * noiseScale, 0, z * 0.025 * noiseScale) * 0.06;
        double microVariation = noise.sampleAt(x * 0.08 * noiseScale, 0, z * 0.09 * noiseScale) * 0.03;

        return combinedCanyon + naturalVariation + microVariation;
    }

    /**
     * Original tight channel creation with natural irregularities
     */
    private double createTightChannel(MasterNoiseProvider noise, int x, int z,
                                      double frequency, double meandering, double widthFactor, double noiseScale) {
        // Irregular meandering (original algorithm)
        double meander1 = noise.sampleAt(x * frequency * 0.4 * noiseScale, 0, z * frequency * 0.3 * noiseScale) * meandering;
        double meander2 = noise.sampleAt(x * frequency * 0.7 * noiseScale, 0, z * frequency * 0.6 * noiseScale) * meandering * 0.5;

        // Natural irregularity (original)
        double irregularity1 = noise.sampleAt(x * frequency * 2.0 * noiseScale, 0, z * frequency * 1.8 * noiseScale) * meandering * 0.3;
        double irregularity2 = noise.sampleAt(x * frequency * 3.5 * noiseScale, 0, z * frequency * 2.9 * noiseScale) * meandering * 0.2;
        double irregularity3 = noise.sampleAt(x * frequency * 6.0 * noiseScale, 0, z * frequency * 5.2 * noiseScale) * meandering * 0.1;

        double totalMeanderX = meander1 + meander2 * 0.5 + irregularity1 + irregularity2 + irregularity3;
        double totalMeanderZ = meander1 * 0.8 + meander2 + irregularity1 * 0.7 + irregularity2 + irregularity3;

        double offsetX = x + totalMeanderX * 30.0;
        double offsetZ = z + totalMeanderZ * 25.0;

        double baseRidge = noise.sampleRidge(offsetX * frequency, 0, offsetZ * frequency);

        // Canyon edge irregularity (original)
        double edgeNoise1 = noise.sampleAt(x * frequency * 8.0 * noiseScale, 0, z * frequency * 7.0 * noiseScale) * 0.15;
        double edgeNoise2 = noise.sampleAt(x * frequency * 15.0 * noiseScale, 0, z * frequency * 12.0 * noiseScale) * 0.08;
        double edgeNoise3 = noise.sampleAt(x * frequency * 25.0 * noiseScale, 0, z * frequency * 22.0 * noiseScale) * 0.04;
        double edgeIrregularity = edgeNoise1 + edgeNoise2 + edgeNoise3;

        // Variable canyon width (original)
        double widthVariation = noise.sampleAt(x * frequency * 4.0 * noiseScale, 0, z * frequency * 3.5 * noiseScale) * 0.3 + 0.85;
        double variableWidthFactor = widthFactor * widthVariation;

        // Canyon depth irregularity (original)
        double depthVariation = noise.sampleAt(x * frequency * 6.0 * noiseScale, 0, z * frequency * 5.5 * noiseScale) * 0.2;

        return (baseRidge + edgeIrregularity + depthVariation) / variableWidthFactor;
    }

    // Additional helper methods from original (createIrregularBranches, createNaturalConnections, etc.)
    private double createIrregularBranches(MasterNoiseProvider noise, int x, int z, double widthFactor, double branchingFactor, double noiseScale) {
        double branch1 = createTightChannel(noise, x, z, 0.008 * noiseScale, 0.6, widthFactor * branchingFactor, noiseScale);
        double branch2 = createTightChannel(noise, x, z, 0.012 * noiseScale, 0.4, widthFactor * branchingFactor * 0.8, noiseScale);
        double branchNoise = noise.sampleAt(x * 0.015 * noiseScale, 0, z * 0.018 * noiseScale) * 0.2;
        return Math.min(branch1, branch2) + branchNoise;
    }

    private double createNaturalConnections(MasterNoiseProvider noise, int x, int z, double widthFactor, double noiseScale) {
        double connection1 = noise.sampleRidge(x * 0.005 * noiseScale, 0, z * 0.0065 * noiseScale) * 0.9;
        double connection2 = noise.sampleRidge(x * 0.0075 * noiseScale, 0, z * 0.009 * noiseScale) * 0.8;
        double connectionNoise = noise.sampleAt(x * 0.025 * noiseScale, 0, z * 0.03 * noiseScale) * 0.15;
        double baseConnection = Math.min(connection1, connection2);
        return (baseConnection + connectionNoise) / widthFactor;
    }

    private double applyCanyonSharpness(double canyonValue, double sharpness) {
        if (canyonValue < 0.5) {
            double sharpnessVariation = Math.sin(canyonValue * Math.PI * 4.0) * 0.3 + 1.0;
            double adjustedSharpness = sharpness * sharpnessVariation;
            return Math.pow(canyonValue, adjustedSharpness);
        }
        return canyonValue;
    }

    private double addSimpleEdgeRoughening(double canyonPattern, int x, int z, OctaveContext context, double threshold) {
        double distanceFromEdge = Math.abs(canyonPattern - threshold);
        if (distanceFromEdge < 0.05) {
            double edgeRoughness1 = context.getNoiseProvider().sampleAt(x * 0.05, 0, z * 0.05) * 0.030;
            double edgeRoughness2 = context.getNoiseProvider().sampleAt(x * 0.08, 0, z * 0.09) * 0.016;
            double edgeRoughness3 = context.getNoiseProvider().sampleAt(x * 0.12, 0, z * 0.11) * 0.010;
            double mediumRoughness = context.getNoiseProvider().sampleAt(x * 0.035, 0, z * 0.04) * 0.020;
            double totalRoughness = edgeRoughness1 + edgeRoughness2 + edgeRoughness3 + mediumRoughness;
            double roughnessStrength = (0.05 - distanceFromEdge) / 0.05;
            return canyonPattern + (totalRoughness * roughnessStrength);
        }
        return canyonPattern;
    }

    @Override
    public double getPrimaryFrequency() { return 0.003; }

    @Override
    public Set<PlanetType> getSupportedPlanetTypes() {
        return Set.of(PlanetType.DESERT, PlanetType.ROCKY, PlanetType.VOLCANIC);
    }
    
    @Override
    public String getOctaveName() { return "WaterErosionPhysics"; }
}