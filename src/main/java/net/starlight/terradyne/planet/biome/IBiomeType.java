package net.starlight.terradyne.planet.biome;

import net.starlight.terradyne.planet.PlanetType;
import net.starlight.terradyne.planet.terrain.OctaveConfiguration;
import java.util.List;

/**
 * Updated biome interface - now biomes directly configure their octaves
 */
public interface IBiomeType {
    String getName();
    PlanetType getPlanetType();

    /**
     * Get the configured octaves this biome wants applied
     * Each configuration specifies the octave class and its parameters
     */
    List<OctaveConfiguration> getOctaveConfigurations();
}