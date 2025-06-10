package net.starlight.terradyne.planet.terrain.octave;

import net.starlight.terradyne.planet.PlanetType;
import net.starlight.terradyne.planet.terrain.MasterNoiseProvider;
import net.starlight.terradyne.planet.terrain.OctaveConfiguration;
import net.starlight.terradyne.planet.terrain.OctaveContext;

import java.util.Set;

/**
 * RENAMED from ScrublandOctave - Rolling Terrain Octave
 *
 * Creates gentle rolling base terrain that mesas and other dramatic features
 * can rise from. This provides the realistic "badlands" foundation that you
 * see underneath dramatic rock formations in real deserts.
 */
public class RollingTerrainOctave implements IUnifiedOctave {

    @Override
    public double generateHeightContribution(int x, int z, OctaveContext context,
                                             OctaveConfiguration config) {
        MasterNoiseProvider noise = context.getNoiseProvider();

        // Configuration parameters
        double hillHeight = config.getDouble("hillHeight", 8.0);
        double hillFrequency = config.getDouble("hillFrequency", 0.003);
        double rockOutcropIntensity = config.getDouble("rockOutcropIntensity", 0.3);
        double washDepth = config.getDouble("washDepth", 2.0);
        double undulationStrength = config.getDouble("undulationStrength", 1.0);

        // === YOUR ORIGINAL ALGORITHM ===
        // Very large-scale broad undulation - ensures coverage everywhere
        double broadSweeps = noise.sampleAt(x * hillFrequency * 0.3, 0, z * hillFrequency * 0.4) * hillHeight * 0.8;

        // Large-scale gentle rolling - always contributes
        double largeRolling = noise.sampleAt(x * hillFrequency * 0.8, 0, z * hillFrequency * 0.6) * hillHeight * 0.6;

        // Medium-scale terrain variation - always contributes
        double mediumRolling = noise.sampleAt(x * hillFrequency * 1.5, 0, z * hillFrequency * 1.2) * hillHeight * 0.4;

        // Combine for consistent rolling terrain EVERYWHERE
        double baseRolling = (broadSweeps + largeRolling + mediumRolling) * undulationStrength;

        // === SCATTERED ROCK OUTCROPS ===
        double outcropNoise = (noise.sampleAt(x * hillFrequency * 8.0, 0, z * hillFrequency * 6.0) + 1.0) * 0.5;
        double rockOutcrops = 0.0;

        if (outcropNoise > 0.7) {
            double outcropHeight = (outcropNoise - 0.7) / 0.3;
            rockOutcrops = Math.pow(outcropHeight, 1.2) * hillHeight * rockOutcropIntensity;
        }

        // === DRAINAGE PATTERNS ===
        double washPattern = noise.sampleRidge(x * hillFrequency * 2.0, 0, z * hillFrequency * 1.5);
        double washes = 0.0;

        if (washPattern < 0.4) {
            double washIntensity = (0.4 - washPattern) / 0.4;
            washes = -washIntensity * washDepth * 0.5;
        }

        // === ALLUVIAL AREAS ===
        double depositNoise = (noise.sampleAt(x * hillFrequency * 1.2, 0, z * hillFrequency * 1.8) + 1.0) * 0.5;
        double deposits = depositNoise * hillHeight * 0.3;

        // === SURFACE TEXTURE ===
        double surfaceTexture = noise.sampleAt(x * 0.025, 0, z * 0.02) * 1.2;

        return baseRolling + rockOutcrops + washes + deposits + surfaceTexture;
    }

    @Override
    public double getPrimaryFrequency() { return 0.003; }

    @Override
    public Set<PlanetType> getSupportedPlanetTypes() {
        return Set.of(PlanetType.DESERT, PlanetType.HOTHOUSE, PlanetType.ROCKY);
    }

    @Override
    public String getOctaveName() { return "RollingTerrainPhysics"; }
}