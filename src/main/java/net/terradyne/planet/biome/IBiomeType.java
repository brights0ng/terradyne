package net.terradyne.planet.biome;

import net.terradyne.planet.PlanetType;

import java.util.List;

/**
 * Generic biome interface
 * Biomes now specify which terrain generators they want applied
 */
public interface IBiomeType {
    String getName();
    PlanetType getPlanetType();

    /**
     * Returns the octave types this biome requests for terrain generation
     */
    List<OctaveType> getRequestedOctaveTypes();
}