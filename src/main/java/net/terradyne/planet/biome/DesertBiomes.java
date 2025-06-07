package net.terradyne.planet.biome;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeEffects;
import net.minecraft.world.biome.GenerationSettings;
import net.minecraft.world.biome.SpawnSettings;

/**
 * Updated DesertBiomes - Creates the 4 core desert biomes
 */
public class DesertBiomes {

    /**
     * Dune Sea - Rolling sand dunes, classic desert
     */
    public static Biome createDuneSeaBiome() {
        return new Biome.Builder()
                .temperature(0.8f)
                .downfall(0.1f)
                .precipitation(false)
                .effects(new BiomeEffects.Builder()
                        .skyColor(0xE6D07A)        // Warm yellow sky
                        .fogColor(0xC4A875)        // Sandy fog
                        .waterColor(0x3F76E4)
                        .waterFogColor(0x050533)
                        .grassColor(0xBFA755)      // Dry grass
                        .foliageColor(0x9E8A47)    // Desert foliage
                        .loopSound(SoundEvents.AMBIENT_CAVE)
                        .build())
                .spawnSettings(new SpawnSettings.Builder()
                        .spawn(SpawnGroup.MONSTER, new SpawnSettings.SpawnEntry(
                                EntityType.HUSK, 5, 1, 2))
                        .spawn(SpawnGroup.CREATURE, new SpawnSettings.SpawnEntry(
                                EntityType.RABBIT, 1, 1, 1))
                        .build())
                .generationSettings(new GenerationSettings.Builder()
                        .build())
                .build();
    }

    /**
     * Granite Mesas - Elevated rocky plateaus
     */
    public static Biome createGraniteMesasBiome() {
        return new Biome.Builder()
                .temperature(0.6f)       // Cooler due to elevation
                .downfall(0.2f)          // Slightly more moisture at altitude
                .precipitation(false)
                .effects(new BiomeEffects.Builder()
                        .skyColor(0xC0D8FF)    // Clearer sky at altitude
                        .fogColor(0xB8860B)    // Golden fog
                        .waterColor(0x3F76E4)
                        .waterFogColor(0x050533)
                        .grassColor(0xA0522D)  // Rocky brown
                        .foliageColor(0x8B4513) // Sienna
                        .loopSound(SoundEvents.AMBIENT_CAVE)
                        .build())
                .spawnSettings(new SpawnSettings.Builder()
                        .spawn(SpawnGroup.CREATURE, new SpawnSettings.SpawnEntry(
                                EntityType.GOAT, 2, 1, 3)) // Mountain goats
                        .spawn(SpawnGroup.MONSTER, new SpawnSettings.SpawnEntry(
                                EntityType.HUSK, 2, 1, 2)) // Fewer hostiles
                        .build())
                .generationSettings(new GenerationSettings.Builder()
                        .build())
                .build();
    }

    /**
     * Limestone Canyons - Deep carved canyons with limestone walls
     */
    public static Biome createLimestoneCanyonsBiome() {
        return new Biome.Builder()
                .temperature(0.7f)       // Moderate temperature in canyons
                .downfall(0.3f)          // Higher humidity in protected canyons
                .precipitation(false)
                .effects(new BiomeEffects.Builder()
                        .skyColor(0xDEB887)    // Burlywood sky
                        .fogColor(0xD2B48C)    // Tan fog
                        .waterColor(0x4682B4)  // Steel blue water
                        .waterFogColor(0x050533)
                        .grassColor(0xDAA520)  // Goldenrod
                        .foliageColor(0xB8860B) // Dark goldenrod
                        .loopSound(SoundEvents.AMBIENT_CAVE)
                        .build())
                .spawnSettings(new SpawnSettings.Builder()
                        .spawn(SpawnGroup.CREATURE, new SpawnSettings.SpawnEntry(
                                EntityType.BAT, 3, 2, 4))    // Bats in caves
                        .spawn(SpawnGroup.CREATURE, new SpawnSettings.SpawnEntry(
                                EntityType.LLAMA, 1, 2, 4))  // Canyon dwellers
                        .spawn(SpawnGroup.MONSTER, new SpawnSettings.SpawnEntry(
                                EntityType.HUSK, 3, 1, 2))
                        .build())
                .generationSettings(new GenerationSettings.Builder()
                        .build())
                .build();
    }

    /**
     * Salt Flats - Flat crystalline salt deposits
     */
    public static Biome createSaltFlatsBiome() {
        return new Biome.Builder()
                .temperature(0.9f)       // Hot due to salt reflection
                .downfall(0.0f)          // Extremely dry
                .precipitation(false)
                .effects(new BiomeEffects.Builder()
                        .skyColor(0xF5F5DC)    // Beige sky (salt haze)
                        .fogColor(0xFFFFE0)    // Light yellow fog
                        .waterColor(0x00CED1)  // Dark turquoise (salt pools)
                        .waterFogColor(0x050533)
                        .grassColor(0xF5DEB3)  // Wheat color
                        .foliageColor(0xDEB887) // Burlywood
                        .loopSound(SoundEvents.AMBIENT_CAVE)
                        .build())
                .spawnSettings(new SpawnSettings.Builder()
                        // Very sparse spawning due to harsh conditions
                        .spawn(SpawnGroup.MONSTER, new SpawnSettings.SpawnEntry(
                                EntityType.HUSK, 1, 1, 1))   // Rare husks
                        .build())
                .generationSettings(new GenerationSettings.Builder()
                        .build())
                .build();
    }
}