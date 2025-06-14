package net.starlight.terradyne.planet.terrain.octave;

import net.starlight.terradyne.planet.PlanetType;

import java.util.Set;

/**
 * Interface for terrain octaves
 * Will be phased out in favor of passes
 */
@Deprecated
public interface IUnifiedOctave {

    /**
     * Generate height contribution at the given position
     */
    double generateHeightContribution(int x, int z, OctaveContext context,
                                      OctaveConfiguration config);

    /**
     * Get the primary frequency this octave operates at
     */
    double getPrimaryFrequency();

    /**
     * Get planet types this octave supports
     */
    Set<PlanetType> getSupportedPlanetTypes();

    /**
     * Get the name of this octave
     */
    String getOctaveName();

    /**
     * Get parameter documentation
     */
    default String getParameterDocumentation() {
        return "No parameters documented";
    }
}