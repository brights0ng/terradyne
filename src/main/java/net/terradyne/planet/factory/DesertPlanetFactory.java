package net.terradyne.planet.factory;

import net.minecraft.world.biome.source.BiomeSource;
import net.terradyne.planet.chunk.DesertChunkGenerator;
import net.terradyne.planet.config.DesertConfig;
import net.terradyne.planet.model.DesertModel;
import net.terradyne.util.ModEnums;

// DesertPlanetFactory.java - NEW FILE
public class DesertPlanetFactory {

    public static DesertConfig createDefaultDesertConfig(String planetName) {
        long seed = System.currentTimeMillis();

        // Create a balanced desert world
        return new DesertConfig(
                planetName,
                seed,
                ModEnums.PlanetAge.MATURE,
                35.0f,                           // Moderate desert temperature
                0.15f,                           // Low humidity
                1.2f,                            // Moderate wind
                0.7f,                            // Mostly sandy
                true,                            // Has dunes
                25.0f,                           // High day/night variation
                0.3f,                            // Some dust storms
                DesertConfig.RockType.SANDSTONE  // Classic desert rock
        );
    }

    public static DesertConfig createHotDesertConfig(String planetName) {
        long seed = System.currentTimeMillis();

        return new DesertConfig(
                planetName, seed, ModEnums.PlanetAge.YOUNG,
                65.0f,    // Very hot
                0.05f,    // Extremely dry
                1.8f,     // Strong winds
                0.9f,     // Mostly sand
                true,     // Large dunes
                40.0f,    // Extreme temperature swings
                0.6f,     // Frequent dust storms
                DesertConfig.RockType.VOLCANIC
        );
    }

    public static DesertConfig createRockyDesertConfig(String planetName) {
        long seed = System.currentTimeMillis();

        return new DesertConfig(
                planetName, seed, ModEnums.PlanetAge.ANCIENT,
                25.0f,    // Cooler desert
                0.25f,    // Slightly more humid
                0.8f,     // Lower winds
                0.3f,     // Mostly rocky
                false,    // No major dunes
                15.0f,    // Moderate temperature variation
                0.1f,     // Few dust storms
                DesertConfig.RockType.GRANITE
        );
    }

    public static DesertModel createDesertModel(DesertConfig config) {
        return new DesertModel(config);
    }

    public static DesertChunkGenerator createDesertChunkGenerator(DesertModel model, BiomeSource biomeSource) {
        return new DesertChunkGenerator(model, biomeSource);
    }
}