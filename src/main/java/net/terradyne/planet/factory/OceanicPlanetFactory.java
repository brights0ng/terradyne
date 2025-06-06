package net.terradyne.planet.factory;

import net.minecraft.world.biome.source.BiomeSource;
import net.terradyne.planet.chunk.OceanicChunkGenerator;
import net.terradyne.planet.config.OceanicConfig;
import net.terradyne.planet.model.OceanicModel;
import net.terradyne.util.ModEnums;

// OceanicPlanetFactory.java
public class OceanicPlanetFactory {

    public static OceanicConfig createEarthLikeConfig(String planetName) {
        long seed = System.currentTimeMillis();

        return new OceanicConfig(
                planetName,
                seed,
                ModEnums.PlanetAge.MATURE,
                0.71f,                                    // Earth-like ocean coverage
                25.0f,                                    // Moderate ocean depth
                15.0f,                                    // Continental shelf width
                2.0f,                                     // Moderate tidal range
                4,                                        // Several continents
                0.7f,                                     // High atmospheric humidity
                true,                                     // Has polar ice caps
                0.8f,                                     // Moderate tectonic activity
                OceanicConfig.OceanType.TEMPERATE,       // Earth-like oceans
                0.6f                                      // Moderate weather
        );
    }

    public static OceanicConfig createTropicalOceanConfig(String planetName) {
        long seed = System.currentTimeMillis();

        return new OceanicConfig(
                planetName, seed, ModEnums.PlanetAge.MATURE,
                0.85f,                                    // Very high ocean coverage
                18.0f,                                    // Shallower warm seas
                25.0f,                                    // Wide coral reef zones
                1.2f,                                     // Lower tidal range
                2,                                        // Fewer, smaller continents
                0.9f,                                     // Very humid atmosphere
                false,                                    // No ice caps (too warm)
                0.4f,                                     // Low tectonic activity
                OceanicConfig.OceanType.TROPICAL,        // Warm, clear waters
                0.8f                                      // High storm activity
        );
    }

    public static OceanicConfig createArchipelagoConfig(String planetName) {
        long seed = System.currentTimeMillis();

        return new OceanicConfig(
                planetName, seed, ModEnums.PlanetAge.YOUNG,
                0.9f,                                     // Mostly ocean
                12.0f,                                    // Shallow seas
                30.0f,                                    // Extensive shallows
                3.0f,                                     // High tidal range
                8,                                        // Many small landmasses
                0.8f,                                     // High humidity
                false,                                    // No ice caps
                1.2f,                                     // High volcanic activity
                OceanicConfig.OceanType.ARCHIPELAGO,     // Island chains
                0.5f                                      // Moderate weather
        );
    }

    public static OceanicConfig createDeepOceanConfig(String planetName) {
        long seed = System.currentTimeMillis();

        return new OceanicConfig(
                planetName, seed, ModEnums.PlanetAge.ANCIENT,
                0.95f,                                    // Almost entirely ocean
                60.0f,                                    // Very deep oceans
                5.0f,                                     // Narrow continental shelves
                4.0f,                                     // Strong tidal forces
                1,                                        // Single supercontinent
                0.6f,                                     // Moderate humidity
                true,                                     // Small ice caps
                0.2f,                                     // Low tectonic activity
                OceanicConfig.OceanType.DEEP_ABYSS,      // Abyssal depths
                0.4f                                      // Calmer weather
        );
    }

    public static OceanicModel createOceanicModel(OceanicConfig config) {
        return new OceanicModel(config);
    }

    public static OceanicChunkGenerator createOceanicChunkGenerator(OceanicModel model, BiomeSource biomeSource) {
        return new OceanicChunkGenerator(model, biomeSource);
    }
}