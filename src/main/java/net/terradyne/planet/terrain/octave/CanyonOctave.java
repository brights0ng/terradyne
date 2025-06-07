package net.terradyne.planet.terrain.octave;

import net.terradyne.planet.PlanetType;
import net.terradyne.planet.terrain.MasterNoiseProvider;
import net.terradyne.planet.terrain.OctaveConfiguration;
import net.terradyne.planet.terrain.UnifiedOctaveContext;

import java.util.Set;

/**
 * Updated Canyon octave - carves canyons and channels with configuration
 */
public class CanyonOctave implements IUnifiedOctave {

    @Override
    public double generateHeightContribution(int x, int z, UnifiedOctaveContext context,
                                             OctaveConfiguration config) {
        MasterNoiseProvider noise = context.getNoiseProvider();

        // Get configuration parameters
        double depth = config.getDouble("depth", 20.0);
        double width = config.getDouble("width", 0.005);
        double complexity = config.getDouble("complexity", 0.5);
        double networkDensity = config.getDouble("networkDensity", 1.0);

        // Primary canyon system using ridge noise
        double primaryCanyon = noise.sampleRidge(x * width, 0, z * width);

        // Secondary canyon network
        double secondaryCanyon = noise.sampleRidge(x * width * 2.0, 0, z * width * 2.0) * 0.6;

        // Tertiary detail channels
        double detailChannels = noise.sampleRidge(x * width * 4.0, 0, z * width * 4.0) * 0.3;

        // Combine canyon systems
        double combinedRidge = Math.min(primaryCanyon, secondaryCanyon * networkDensity);
        combinedRidge = Math.min(combinedRidge, detailChannels * complexity);

        // Invert and scale for canyon depth (negative contribution)
        double canyonDepth = (1.0 - combinedRidge) * depth;

        // Sharp canyon walls vs gentle slopes
        double steepness = config.getDouble("steepness", 1.0);
        if (steepness != 1.0) {
            canyonDepth = Math.pow(canyonDepth / depth, steepness) * depth;
        }

        // Return negative value to carve terrain
        return -canyonDepth;
    }

    @Override
    public double getPrimaryFrequency() { return 0.005; }

    @Override
    public Set<PlanetType> getSupportedPlanetTypes() {
        return Set.of(PlanetType.DESERT, PlanetType.HOTHOUSE, PlanetType.ROCKY, PlanetType.VOLCANIC);
    }

    @Override
    public String getOctaveName() { return "Canyon"; }

    @Override
    public String getParameterDocumentation() {
        return """
            Canyon Octave Parameters:
            - depth (double, default 20.0): Maximum canyon depth
            - width (double, default 0.005): Canyon width frequency
            - complexity (double, default 0.5): Detail channel complexity
            - networkDensity (double, default 1.0): Secondary canyon density
            - steepness (double, default 1.0): Canyon wall steepness
            """;
    }
}