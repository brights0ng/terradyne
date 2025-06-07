package net.terradyne.planet.terrain;

import net.terradyne.planet.PlanetType;
import net.terradyne.planet.biome.IBiomeType;
import java.util.Set;

/**
 * Interface for octaves that use the unified master noise approach
 * All octaves receive the SAME noise samplers but sample at different frequencies
 */
public interface IUnifiedOctave {
    /**
     * Generate height contribution using the shared master noise
     * @param x World X coordinate
     * @param z World Z coordinate
     * @param context Unified context with shared noise samplers
     * @return Height contribution (can be positive or negative)
     */
    double generateHeightContribution(int x, int z, UnifiedOctaveContext context);

    /**
     * Get the primary frequency this octave operates at
     */
    double getPrimaryFrequency();

    /**
     * Get the octave category for sorting and selection
     */
    UnifiedOctaveType getOctaveType();

    /**
     * What planet types this octave supports
     */
    Set<PlanetType> getSupportedPlanetTypes();

    /**
     * Check if this octave should be applied in the given biome
     * (This allows octaves to have their own biome-specific logic)
     */
    boolean appliesToBiome(IBiomeType biome);

    /**
     * Get a descriptive name for this octave (for debugging)
     */
    String getOctaveName();
}