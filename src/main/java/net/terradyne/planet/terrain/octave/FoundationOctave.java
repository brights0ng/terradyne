package net.terradyne.planet.terrain.octave;

import net.terradyne.planet.PlanetType;
import net.terradyne.planet.terrain.MasterNoiseProvider;
import net.terradyne.planet.terrain.OctaveConfiguration;
import net.terradyne.planet.terrain.UnifiedOctaveContext;

import java.util.Set;

/**
 * Updated Foundation octave - now uses configuration parameters
 */
public class FoundationOctave implements IUnifiedOctave {

    @Override
    public double generateHeightContribution(int x, int z, UnifiedOctaveContext context,
                                             OctaveConfiguration config) {
        MasterNoiseProvider noise = context.getNoiseProvider();

        // Get biome-configured parameters
        double amplitude = config.getDouble("amplitude", 10.0);
        double frequency = config.getDouble("frequency", 0.001);

        // Large-scale continental features using configured parameters
        double largeTerrain = noise.sampleAt(x * frequency, 0, z * frequency) * amplitude;
        double mediumTerrain = noise.sampleAt(x * frequency * 2.5, 0, z * frequency * 2.5) * amplitude * 0.5;

        return largeTerrain + mediumTerrain;
    }

    @Override
    public double getPrimaryFrequency() {
        return 0.001; // Default, overridden by config
    }

    @Override
    public Set<PlanetType> getSupportedPlanetTypes() {
        return Set.of(PlanetType.values()); // All planets need foundation
    }

    @Override
    public String getOctaveName() {
        return "Foundation";
    }

    @Override
    public String getParameterDocumentation() {
        return """
            Foundation Octave Parameters:
            - amplitude (double, default 10.0): Height variation strength
            - frequency (double, default 0.001): Scale of continental features
            """;
    }
}