package net.terradyne.planet.biome;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeEffects;
import net.minecraft.world.biome.GenerationSettings;
import net.minecraft.world.biome.SpawnSettings;

public class DesertBiomes {

    public static Biome createDuneSeaBiome() {
        // Your existing implementation
        return new Biome.Builder()
                .temperature(0.8f)
                .downfall(0.1f)
                .precipitation(false)
                .effects(new BiomeEffects.Builder()
                        .skyColor(0xE6D07A)
                        .fogColor(0xC4A875)
                        .waterColor(0x3F76E4)
                        .waterFogColor(0x050533)
                        .grassColor(0xBFA755)
                        .foliageColor(0x9E8A47)
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

    public static Biome createScorchingWasteBiome() {
        return new Biome.Builder()
                .temperature(1.2f)        // Hotter than dune sea
                .downfall(0.0f)          // No water at all
                .precipitation(false)
                .effects(new BiomeEffects.Builder()
                        .skyColor(0xFFB347)    // Orange sky
                        .fogColor(0xFF8C42)    // Orange fog
                        .waterColor(0x3F76E4)
                        .waterFogColor(0x050533)
                        .grassColor(0x8B4513)  // Brown
                        .foliageColor(0x654321) // Dark brown
                        .loopSound(SoundEvents.AMBIENT_CAVE)
                        .build())
                .spawnSettings(new SpawnSettings.Builder()
                        .spawn(SpawnGroup.MONSTER, new SpawnSettings.SpawnEntry(
                                EntityType.HUSK, 8, 2, 4)) // More hostile
                        .build())
                .generationSettings(new GenerationSettings.Builder()
                        .build())
                .build();
    }

    public static Biome createGraniteMesasBiome() {
        return new Biome.Builder()
                .temperature(0.6f)       // Slightly cooler due to elevation
                .downfall(0.2f)          // Slightly more moisture
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
                        .build())
                .generationSettings(new GenerationSettings.Builder()
                        .build())
                .build();
    }

    // Add more biome creation methods as needed...
}