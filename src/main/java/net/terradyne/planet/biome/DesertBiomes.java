// DesertBiomes.java - CUSTOM BIOME CREATION for the 4 desert biomes

package net.terradyne.planet.biome;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeEffects;
import net.minecraft.world.biome.GenerationSettings;
import net.minecraft.world.biome.SpawnSettings;

public class DesertBiomes {

    /**
     * DUNE SEA - Keep this exactly as it was (user said it was perfect)
     */
    public static Biome createDuneSeaBiome() {
        return new Biome.Builder()
                .temperature(0.8f)               // Hot but not extreme
                .downfall(0.1f)                  // Very low rainfall
                .precipitation(false)            // No rain
                .effects(new BiomeEffects.Builder()
                        .skyColor(0xE6D07A)          // Sandy sky
                        .fogColor(0xC4A875)          // Sandy fog
                        .waterColor(0x3F76E4)        // Standard water
                        .waterFogColor(0x050533)     // Dark water fog
                        .grassColor(0xBFA755)        // Sandy grass
                        .foliageColor(0x9E8A47)      // Sandy foliage
                        .loopSound(SoundEvents.AMBIENT_CAVE)
                        .build())
                .spawnSettings(new SpawnSettings.Builder()
                        .spawn(SpawnGroup.MONSTER, new SpawnSettings.SpawnEntry(
                                EntityType.HUSK, 5, 1, 2))      // Desert zombies
                        .spawn(SpawnGroup.CREATURE, new SpawnSettings.SpawnEntry(
                                EntityType.RABBIT, 1, 1, 1))    // Desert animals
                        .build())
                .generationSettings(new GenerationSettings.Builder()
                        .build())
                .build();
    }

    /**
     * GRANITE MESAS - Rocky plateau biome with earth tones
     */
    public static Biome createGraniteMesasBiome() {
        return new Biome.Builder()
                .temperature(0.9f)               // Hot and dry
                .downfall(0.05f)                 // Almost no rainfall
                .precipitation(false)            // No rain
                .effects(new BiomeEffects.Builder()
                        .skyColor(0xD4A574)          // Dusty orange sky
                        .fogColor(0xB8860B)          // Golden brown fog
                        .waterColor(0x3F76E4)        // Standard water
                        .waterFogColor(0x050533)
                        .grassColor(0xA0522D)        // Brownish grass
                        .foliageColor(0x8B4513)      // Brown foliage
                        .loopSound(SoundEvents.AMBIENT_CAVE)
                        .build())
                .spawnSettings(new SpawnSettings.Builder()
                        .spawn(SpawnGroup.CREATURE, new SpawnSettings.SpawnEntry(
                                EntityType.GOAT, 2, 1, 3))      // Mountain goats on cliffs
                        .spawn(SpawnGroup.MONSTER, new SpawnSettings.SpawnEntry(
                                EntityType.HUSK, 3, 1, 2))      // Fewer monsters on cliffs
                        .build())
                .generationSettings(new GenerationSettings.Builder()
                        .build())
                .build();
    }

    /**
     * LIMESTONE CANYONS - Deep valley biome with layered colors
     */
    public static Biome createLimestoneCanyonsBiome() {
        return new Biome.Builder()
                .temperature(0.7f)               // Cooler in canyon depths
                .downfall(0.15f)                 // Slightly more moisture
                .precipitation(false)            // Still no rain
                .effects(new BiomeEffects.Builder()
                        .skyColor(0xCFBF9A)          // Muted canyon sky
                        .fogColor(0xD2B48C)          // Tan fog
                        .waterColor(0x4A5D23)        // Muddy water
                        .waterFogColor(0x2F3A0F)     // Dark muddy fog
                        .grassColor(0x9ACD32)        // Yellowish green
                        .foliageColor(0x8FBC8F)      // Pale green
                        .loopSound(SoundEvents.AMBIENT_CAVE)
                        .build())
                .spawnSettings(new SpawnSettings.Builder()
                        .spawn(SpawnGroup.CREATURE, new SpawnSettings.SpawnEntry(
                                EntityType.BAT, 3, 2, 5))       // Bats in caves
                        .spawn(SpawnGroup.MONSTER, new SpawnSettings.SpawnEntry(
                                EntityType.SPIDER, 4, 1, 3))    // Cave spiders
                        .spawn(SpawnGroup.MONSTER, new SpawnSettings.SpawnEntry(
                                EntityType.HUSK, 2, 1, 2))      // Some desert zombies
                        .build())
                .generationSettings(new GenerationSettings.Builder()
                        .build())
                .build();
    }

    /**
     * SALT FLATS - Stark white crystalline biome
     */
    public static Biome createSaltFlatsBiome() {
        return new Biome.Builder()
                .temperature(1.1f)               // Very hot, no shade
                .downfall(0.0f)                  // Absolutely no rainfall
                .precipitation(false)            // No precipitation
                .effects(new BiomeEffects.Builder()
                        .skyColor(0xF0F8FF)          // Bright white sky
                        .fogColor(0xF5F5DC)          // Beige white fog
                        .waterColor(0xE0FFFF)        // Light cyan water (salt pools)
                        .waterFogColor(0xB0E0E6)     // Powder blue water fog
                        .grassColor(0xF5F5F5)        // White grass
                        .foliageColor(0xE6E6FA)      // Lavender white foliage
                        .loopSound(SoundEvents.AMBIENT_CAVE)
                        .build())
                .spawnSettings(new SpawnSettings.Builder()
                        // Very sparse spawning in harsh salt environment
                        .spawn(SpawnGroup.MONSTER, new SpawnSettings.SpawnEntry(
                                EntityType.HUSK, 1, 1, 1))      // Rare desert zombies
                        .build())
                .generationSettings(new GenerationSettings.Builder()
                        .build())
                .build();
    }
}