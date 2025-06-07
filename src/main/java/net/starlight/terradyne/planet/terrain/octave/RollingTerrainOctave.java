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
        double hillFrequency = config.getDouble("hillFrequency", 0.003);  // MUCH lower for broader features
        double rockOutcropIntensity = config.getDouble("rockOutcropIntensity", 0.3);
        double washDepth = config.getDouble("washDepth", 2.0);
        double undulationStrength = config.getDouble("undulationStrength", 1.0);

        // === STEP 1: CREATE BROAD SWEEPING HILLS EVERYWHERE ===
        // Very large-scale broad undulation - ensures coverage everywhere
        double broadSweeps = noise.sampleAt(x * hillFrequency * 0.3, 0, z * hillFrequency * 0.4) * hillHeight * 0.8;

        // Large-scale gentle rolling - always contributes
        double largeRolling = noise.sampleAt(x * hillFrequency * 0.8, 0, z * hillFrequency * 0.6) * hillHeight * 0.6;

        // Medium-scale terrain variation - always contributes
        double mediumRolling = noise.sampleAt(x * hillFrequency * 1.5, 0, z * hillFrequency * 1.2) * hillHeight * 0.4;

        // Combine for consistent rolling terrain EVERYWHERE
        // Note: Using (noise + 1.0) * 0.5 to ensure positive contribution everywhere
        double baseRolling = (
                (broadSweeps) +
                        (largeRolling) +
                        (mediumRolling)
        ) * undulationStrength;

        // === STEP 2: ADD SCATTERED ROCK OUTCROPS EVERYWHERE ===
        // Rock outcrops scattered across ALL rolling terrain (not just certain zones)
        double outcropNoise = (noise.sampleAt(x * hillFrequency * 8.0, 0, z * hillFrequency * 6.0) + 1.0) * 0.5; // 0-1 range
        double rockOutcrops = 0.0;

        if (outcropNoise > 0.7) { // Lowered threshold so more areas get rocks
            double outcropHeight = (outcropNoise - 0.7) / 0.3; // 0-1 range
            rockOutcrops = Math.pow(outcropHeight, 1.2) * hillHeight * rockOutcropIntensity;
        }

        // === STEP 3: CREATE BROAD DRAINAGE PATTERNS ===
        // Very broad, sweeping drainage that doesn't create sharp channels
        double washPattern = noise.sampleRidge(x * hillFrequency * 2.0, 0, z * hillFrequency * 1.5);
        double washes = 0.0;

        if (washPattern < 0.4) { // More generous threshold
            double washIntensity = (0.4 - washPattern) / 0.4;
            washes = -washIntensity * washDepth * 0.5; // Gentler carving
        }

        // === STEP 4: ADD BROAD ALLUVIAL AREAS ===
        // Broad sediment accumulation that adds to the sweeping character
        double depositNoise = (noise.sampleAt(x * hillFrequency * 1.2, 0, z * hillFrequency * 1.8) + 1.0) * 0.5;
        double deposits = depositNoise * hillHeight * 0.3; // Always contributes something

        // === STEP 5: COMBINE ALL FEATURES ===
        double totalHeight = baseRolling + rockOutcrops + washes + deposits;

        // === STEP 6: ADD FINE SURFACE TEXTURE EVERYWHERE ===
        // Gentle surface variation that ensures no completely flat areas
        double surfaceTexture = noise.sampleAt(x * 0.025, 0, z * 0.02) * 1.2;
        totalHeight += surfaceTexture;

        return totalHeight;
    }

    @Override
    public double getPrimaryFrequency() { return 0.003; }  // MUCH lower for broad sweeping features

    @Override
    public Set<PlanetType> getSupportedPlanetTypes() {
        return Set.of(PlanetType.DESERT, PlanetType.HOTHOUSE, PlanetType.ROCKY);
    }

    @Override
    public String getOctaveName() { return "RollingTerrain"; }

    @Override
    public String getParameterDocumentation() {
        return """
            Rolling Terrain Octave Parameters:
            - hillHeight (double, default 8.0): Height of gentle rolling hills
            - hillFrequency (double, default 0.012): Frequency/size of rolling features
            - rockOutcropIntensity (double, default 0.3): Intensity of scattered rock outcrops
            - washDepth (double, default 2.0): Depth of gentle drainage channels
            - undulationStrength (double, default 1.0): Overall strength of rolling terrain
            """;
    }
}