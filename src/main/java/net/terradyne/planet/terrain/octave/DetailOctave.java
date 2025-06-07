package net.terradyne.planet.terrain.octave;

import net.terradyne.planet.PlanetType;
import net.terradyne.planet.terrain.MasterNoiseProvider;
import net.terradyne.planet.terrain.OctaveConfiguration;
import net.terradyne.planet.terrain.UnifiedOctaveContext;

import java.util.Set;

/**
 * Updated Detail octave - provides fine surface texture with configuration
 */
public class DetailOctave implements IUnifiedOctave {

    @Override
    public double generateHeightContribution(int x, int z, UnifiedOctaveContext context,
                                             OctaveConfiguration config) {
        MasterNoiseProvider noise = context.getNoiseProvider();

        // Get configuration parameters
        double intensity = config.getDouble("intensity", 0.1);
        double frequency = config.getDouble("frequency", 0.01);
        boolean saltPatterns = config.getBoolean("saltPatterns", false);
        boolean volcanic = config.getBoolean("volcanic", false);

        // Base fine detail texture
        double fineDetail = noise.sampleAt(x * frequency, 0, z * frequency) * intensity;

        // Additional detail layer
        double microDetail = noise.sampleAt(x * frequency * 3.0, 0, z * frequency * 3.0) *
                intensity * 0.3;

        double totalDetail = fineDetail + microDetail;

        // Special patterns for salt flats
        if (saltPatterns) {
            double saltHexagons = noise.sampleAt(x * frequency * 8.0, 0, z * frequency * 8.0) *
                    intensity * 0.5;
            totalDetail += Math.abs(saltHexagons);  // Absolute for crystalline patterns
        }

        // Rough volcanic texture
        if (volcanic) {
            double roughness = noise.sampleRidge(x * frequency * 2.0, 0, z * frequency * 2.0) *
                    intensity * 0.8;
            totalDetail += roughness;
        }

        return totalDetail;
    }

    @Override
    public double getPrimaryFrequency() { return 0.01; }

    @Override
    public Set<PlanetType> getSupportedPlanetTypes() {
        return Set.of(PlanetType.values()); // All planets can have surface detail
    }

    @Override
    public String getOctaveName() { return "Detail"; }

    @Override
    public String getParameterDocumentation() {
        return """
            Detail Octave Parameters:
            - intensity (double, default 0.1): Overall detail strength
            - frequency (double, default 0.01): Detail feature scale
            - saltPatterns (boolean, default false): Enable salt crystal patterns
            - volcanic (boolean, default false): Enable rough volcanic texture
            """;
    }
}