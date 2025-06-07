package net.starlight.terradyne.planet.terrain.octave;

import net.starlight.terradyne.planet.PlanetType;
import net.starlight.terradyne.planet.terrain.OctaveConfiguration;
import net.starlight.terradyne.planet.terrain.OctaveContext;

import java.util.Set;

/**
 * Updated octave interface - now receives configuration from biomes
 */
public interface IUnifiedOctave {
    /**
     * Generate height contribution using the shared master noise and biome configuration
     * @param x World X coordinate
     * @param z World Z coordinate
     * @param context Unified context with shared noise samplers
     * @param config Biome-specific configuration for this octave
     * @return Height contribution (can be positive or negative)
     */
    double generateHeightContribution(int x, int z, OctaveContext context,
                                      OctaveConfiguration config);

    /**
     * Get the primary frequency this octave operates at (used for sorting)
     */
    double getPrimaryFrequency();

    /**
     * What planet types this octave supports
     */
    Set<PlanetType> getSupportedPlanetTypes();

    /**
     * Get a descriptive name for this octave (for debugging)
     */
    String getOctaveName();

    /**
     * Get parameter documentation for this octave (for debugging/tooling)
     */
    default String getParameterDocumentation() {
        return "No parameters documented";
    }
}