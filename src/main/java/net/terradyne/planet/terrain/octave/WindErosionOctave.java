package net.terradyne.planet.terrain.octave;

import net.terradyne.planet.PlanetType;
import net.terradyne.planet.terrain.MasterNoiseProvider;
import net.terradyne.planet.terrain.OctaveConfiguration;
import net.terradyne.planet.terrain.UnifiedOctaveContext;

import java.util.Set;

/**
 * SUPER EXTREME Wind Erosion - should be very obvious
 */
public class WindErosionOctave implements IUnifiedOctave {

    @Override
    public double generateHeightContribution(int x, int z, UnifiedOctaveContext context,
                                             OctaveConfiguration config) {
        MasterNoiseProvider noise = context.getNoiseProvider();

        double erosionStrength = config.getDouble("erosionStrength", 8.0); // MUCH higher

        // === SIMPLE BUT EXTREME APPROACH ===
        // Create obvious streaks going south (negative Z)

        // Sample the "height" that would exist without erosion
        double baseHeight = noise.sampleAt(x * 0.003, 0, z * 0.003) * 20.0;

        // Create erosion streaks - areas that get carved out
        double streakPattern = Math.abs(noise.sampleAt(x * 0.02, 0, z * 0.008)); // Stretched in Z

        // If we're in a "streak" area, create dramatic erosion
        if (streakPattern < 0.4) { // 40% of area gets eroded
            // Create gradient from north to south
            double northSouthGradient = Math.sin(z * 0.01) * 0.5 + 0.5; // 0-1 from north to south

            // More erosion toward the south
            double erosionAmount = -erosionStrength * (1.0 + northSouthGradient) * 2.0;

            // Make it even more dramatic based on base height
            if (baseHeight > 10.0) {
                erosionAmount *= 2.0; // Double erosion on high areas
            }

            return erosionAmount;
        }

        // Areas not in streaks get slight deposition (material blown from eroded areas)
        return erosionStrength * 0.3;
    }

    @Override
    public double getPrimaryFrequency() { return 0.02; }

    @Override
    public Set<PlanetType> getSupportedPlanetTypes() {
        return Set.of(PlanetType.DESERT, PlanetType.HOTHOUSE, PlanetType.OCEANIC, PlanetType.CARBON);
    }

    @Override
    public String getOctaveName() { return "SuperExtremeWindErosion"; }

    @Override
    public String getParameterDocumentation() {
        return """
            Super Extreme Wind Erosion Parameters:
            - erosionStrength (double, default 8.0): INSANE erosion strength
            Creates obvious south-going erosion streaks
            """;
    }
}