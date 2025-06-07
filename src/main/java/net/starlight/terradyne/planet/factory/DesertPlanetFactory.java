// DesertPlanetFactory.java - SIMPLIFIED VERSION

package net.starlight.terradyne.planet.factory;

import net.starlight.terradyne.planet.config.DesertConfig;
import net.starlight.terradyne.planet.model.DesertModel;
import net.starlight.terradyne.util.ModEnums;

public class DesertPlanetFactory {

    /**
     * Standard desert config - all desert planets get the same 4 biomes
     */
    public static DesertConfig createDesertConfig(String planetName) {
        long seed = System.currentTimeMillis();

        return new DesertConfig(
                planetName,
                seed,
                ModEnums.PlanetAge.MATURE,
                42.0f,                           // Standard temperature
                0.15f,                           // Standard humidity
                1.2f,                            // Standard wind
                0.5f,                            // Balanced sand/rock
                true,                            // Has dunes
                25.0f,                           // Temperature variation
                0.3f,                            // Some dust storms
                DesertConfig.RockType.GRANITE    // Standard rock type
        );
        // All desert planets get: DUNE_SEA, GRANITE_MESAS, LIMESTONE_CANYONS, SCRUBLAND
    }

    // Keep these for backward compatibility, but they all do the same thing now
    public static DesertConfig createDefaultDesertConfig(String planetName) {
        return createDesertConfig(planetName);
    }

    public static DesertConfig createDiverseDesertConfig(String planetName) {
        return createDesertConfig(planetName);
    }

    public static DesertConfig createHotDesertConfig(String planetName) {
        return createDesertConfig(planetName);
    }

    public static DesertConfig createRockyDesertConfig(String planetName) {
        return createDesertConfig(planetName);
    }

    public static DesertModel createDesertModel(DesertConfig config) {
        return new DesertModel(config);
    }
}