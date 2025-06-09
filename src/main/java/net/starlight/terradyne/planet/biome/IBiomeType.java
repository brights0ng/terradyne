package net.starlight.terradyne.planet.biome;

import net.starlight.terradyne.planet.PlanetType;
import net.starlight.terradyne.planet.terrain.OctaveConfiguration;
import net.starlight.terradyne.planet.terrain.pass.PassConfiguration;
import java.util.List;

/**
 * Updated biome interface - now supports both octave configurations (legacy)
 * and generation passes (new system)
 */
public interface IBiomeType {
    String getName();
    PlanetType getPlanetType();

    /**
     * LEGACY: Get the configured octaves this biome wants applied
     * @deprecated Use getGenerationPasses() instead
     */
    @Deprecated
    default List<OctaveConfiguration> getOctaveConfigurations() {
        return List.of(); // Default empty implementation
    }

    /**
     * NEW: Get the generation passes this biome uses
     * Each pass can use multiple octaves and place/modify blocks
     */
    List<PassConfiguration> getGenerationPasses();
}