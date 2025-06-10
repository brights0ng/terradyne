package net.starlight.terradyne.planet.terrain.octave;

import net.starlight.terradyne.planet.PlanetType;
import net.starlight.terradyne.planet.terrain.MasterNoiseProvider;
import net.starlight.terradyne.planet.terrain.OctaveConfiguration;
import net.starlight.terradyne.planet.terrain.OctaveContext;
import net.starlight.terradyne.planet.terrain.octave.IUnifiedOctave;

import java.util.Set;

/**
 * PHYSICS: Smooth Flowing Dune Formation Octave
 * Creates gently flowing, majestic sand dunes with realistic wind physics
 * Focus: Smooth transitions, natural dune shapes, flowing patterns
 */
public class DuneFormationOctave implements IUnifiedOctave {

    @Override
    public double generateHeightContribution(int x, int z, OctaveContext context, OctaveConfiguration config) {
        MasterNoiseProvider noise = context.getNoiseProvider();

        // Configuration matching original DuneOctave
        double maxHeight = config.getDouble("maxHeight", 35.0);
        double minHeight = config.getDouble("minHeight", 5.0);
        double duneSpacing = config.getDouble("duneSpacing", 0.004);
        double sharpness = config.getDouble("sharpness", 2.0);
        double elevationVariation = config.getDouble("elevationVariation", 30.0);

        // === LARGE-SCALE ELEVATION CHANGES (from original) ===
        double largeElevation = (noise.sampleAt(x * 0.0002, 0, z * 0.0002) + 1.0) * 0.5 * elevationVariation;
        double mediumElevation = noise.sampleAt(x * 0.0006, 0, z * 0.0006) * elevationVariation * 0.5;
        double baseElevation = largeElevation + mediumElevation;

        // === CREATE SMOOTHER DUNE PATTERN (exact original algorithm) ===
        double largeScale = noise.sampleAt(x * duneSpacing * 0.5, 0, z * duneSpacing * 0.3) * 0.4;
        double mediumScale = noise.sampleAt(x * duneSpacing, 0, z * duneSpacing * 0.6) * 0.6;
        double smallScale = noise.sampleAt(x * duneSpacing * 1.8, 0, z * duneSpacing * 1.2) * 0.3;
        double combinedDunes = largeScale + mediumScale + smallScale;

        // === SMOOTHER HEIGHT CONVERSION (original algorithm) ===
        double duneHeight = (combinedDunes + 1.2) / 2.4;
        duneHeight = Math.max(0.0, Math.min(1.0, duneHeight));
        duneHeight = smoothstep(0.0, 1.0, duneHeight);

        // === WIND FEATURES (original algorithm) ===
        double windAngle = noise.getWindDirection(x, z);
        double windX = x * Math.cos(windAngle) - z * Math.sin(windAngle);
        double windZ = x * Math.sin(windAngle) + z * Math.cos(windAngle);

        double windChannels = Math.abs(noise.sampleAt(windX * duneSpacing * 2.0, 0, windZ * duneSpacing * 0.6));
        windChannels = smoothstep(0.0, 1.0, windChannels);
        duneHeight = duneHeight * (0.4 + windChannels * 0.6);

        // === COMBINE ELEVATION AND DUNE HEIGHT (original) ===
        double duneContribution = minHeight + (duneHeight * (maxHeight - minHeight));
        double finalHeight = baseElevation + duneContribution;

        // === SURFACE DETAIL (original) ===
        if (duneContribution > minHeight + 5.0) {
            double surfaceRipples = noise.sampleAt(x * 0.02, 0, z * 0.015) * 1.0;
            double microDetail = noise.sampleAt(x * 0.08, 0, z * 0.06) * 0.5;
            double detailStrength = Math.min(1.0, (duneContribution - minHeight) / 15.0);
            finalHeight += (surfaceRipples + microDetail) * detailStrength;
        }

        return finalHeight;
    }

    private double smoothstep(double edge0, double edge1, double x) {
        double t = Math.max(0, Math.min(1, (x - edge0) / (edge1 - edge0)));
        return t * t * (3.0 - 2.0 * t);
    }

    @Override
    public double getPrimaryFrequency() { return 0.004; }

    @Override
    public Set<PlanetType> getSupportedPlanetTypes() {
        return Set.of(PlanetType.DESERT, PlanetType.HOTHOUSE);
    }

    @Override
    public String getOctaveName() { return "SmoothDuneFormationPhysics"; }

    @Override
    public String getParameterDocumentation() {
        return """
            Smooth Dune Formation Physics Parameters:
            - windStrength (double, default 1.0): Overall wind effect strength
            - sandAvailability (double, default 1.0): Amount of sand available for dune formation
            - duneSpacing (double, default 0.003): Size/frequency of dune formations (smaller = larger dunes)
            - flowSmoothness (double, default 0.8): How smooth and flowing the dunes are (0.0-1.0)
            
            Creates large, flowing, majestic sand dunes with:
            - Smooth transitions between dune ridges and valleys
            - Realistic wind shadow effects
            - Natural asymmetrical dune profiles
            - Flowing, river-like sand movement patterns
            """;
    }
}