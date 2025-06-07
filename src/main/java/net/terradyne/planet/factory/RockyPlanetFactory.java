package net.terradyne.planet.factory;

import net.minecraft.world.biome.source.BiomeSource;
import net.terradyne.planet.config.RockyConfig;
import net.terradyne.planet.model.RockyModel;
import net.terradyne.util.ModEnums;

// RockyPlanetFactory.java
public class RockyPlanetFactory {

    public static RockyConfig createMoonLikeConfig(String planetName) {
        long seed = System.currentTimeMillis();

        return new RockyConfig(
                planetName,
                seed,
                ModEnums.PlanetAge.ANCIENT,
                0.0f,                                      // No atmosphere
                1.2f,                                      // High crater density
                8.0f,                                      // Moderate regolith depth
                0.4f,                                      // Some exposed bedrock
                RockyConfig.GeologicalActivity.DEAD,       // No geological activity
                0.6f,                                      // Moderate mineral richness
                RockyConfig.SurfaceType.REGOLITH,         // Dusty surface
                120.0f,                                    // High temperature variation
                false,                                     // No caverns
                1.8f                                       // Heavy impact history
        );
    }

    public static RockyConfig createAsteroidConfig(String planetName) {
        long seed = System.currentTimeMillis();

        return new RockyConfig(
                planetName, seed, ModEnums.PlanetAge.ANCIENT,
                0.0f,                                      // No atmosphere
                2.0f,                                      // Maximum crater density
                2.0f,                                      // Very thin regolith
                0.8f,                                      // Mostly exposed bedrock
                RockyConfig.GeologicalActivity.DEAD,       // Dead
                1.5f,                                      // High mineral richness
                RockyConfig.SurfaceType.METALLIC,         // Metal-rich asteroid
                150.0f,                                    // Extreme temperature swings
                true,                                      // Has caverns from impacts
                2.0f                                       // Maximum impact history
        );
    }

    public static RockyConfig createBarrenConfig(String planetName) {
        long seed = System.currentTimeMillis();

        return new RockyConfig(
                planetName, seed, ModEnums.PlanetAge.MATURE,
                0.05f,                                     // Trace atmosphere
                0.8f,                                      // Moderate cratering
                12.0f,                                     // Deeper regolith
                0.3f,                                      // Some bedrock exposure
                RockyConfig.GeologicalActivity.DORMANT,    // Minimal activity
                0.9f,                                      // Good mineral richness
                RockyConfig.SurfaceType.BASALTIC,         // Volcanic surface
                80.0f,                                     // Moderate temperature variation
                false,                                     // No major caverns
                1.2f                                       // Moderate impact history
        );
    }

    public static RockyConfig createShatteredConfig(String planetName) {
        long seed = System.currentTimeMillis();

        return new RockyConfig(
                planetName, seed, ModEnums.PlanetAge.ANCIENT,
                0.02f,                                     // Very thin atmosphere
                1.5f,                                      // High crater density
                4.0f,                                      // Shallow regolith
                0.9f,                                      // Heavily exposed bedrock
                RockyConfig.GeologicalActivity.MINIMAL,    // Some ancient activity
                1.2f,                                      // High mineral richness
                RockyConfig.SurfaceType.FRACTURED,        // Broken, shattered surface
                100.0f,                                    // High temperature variation
                true,                                      // Extensive cavern systems
                1.9f                                       // Very heavy impact history
        );
    }

    public static RockyModel createRockyModel(RockyConfig config) {
        return new RockyModel(config);
    }

//    public static RockyChunkGenerator createRockyChunkGenerator(RockyModel model, BiomeSource biomeSource) {
//        return new RockyChunkGenerator(model, biomeSource);
//    }
}