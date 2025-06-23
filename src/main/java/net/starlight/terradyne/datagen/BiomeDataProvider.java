package net.starlight.terradyne.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.biome.*;

import java.util.concurrent.CompletableFuture;

/**
 * Generates custom biome JSON files using Fabric's data generation system
 */
public class BiomeDataProvider extends FabricDynamicRegistryProvider {

    public BiomeDataProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup registries, Entries entries) {
        // Register our 4 core desert
    }

    @Override
    public String getName() {
        return "Terradyne Biomes";
    }

    // === BIOME CREATION METHODS ===

    private Biome createDuneSeaBiome() {
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
                        .build())
                .spawnSettings(new SpawnSettings.Builder()
                        .spawn(SpawnGroup.MONSTER, new SpawnSettings.SpawnEntry(EntityType.HUSK, 5, 1, 2))
                        .spawn(SpawnGroup.CREATURE, new SpawnSettings.SpawnEntry(EntityType.RABBIT, 1, 1, 1))
                        .build())
                .generationSettings(new GenerationSettings.Builder().build())
                .build();
    }

    private Biome createGraniteMesasBiome() {
        return new Biome.Builder()
                .temperature(0.6f)
                .downfall(0.2f)
                .precipitation(false)
                .effects(new BiomeEffects.Builder()
                        .skyColor(0xE6D07A)        // Clearer sky at altitude
                        .fogColor(0xB8860B)        // Golden fog
                        .waterColor(0x3F76E4)
                        .waterFogColor(0x050533)
                        .grassColor(0xA0522D)      // Rocky brown
                        .foliageColor(0x8B4513)    // Sienna
                        .build())
                .spawnSettings(new SpawnSettings.Builder()
                        .spawn(SpawnGroup.CREATURE, new SpawnSettings.SpawnEntry(EntityType.GOAT, 2, 1, 3))
                        .spawn(SpawnGroup.MONSTER, new SpawnSettings.SpawnEntry(EntityType.HUSK, 2, 1, 2))
                        .build())
                .generationSettings(new GenerationSettings.Builder().build())
                .build();
    }

    private Biome createLimestoneCanyonsBiome() {
        return new Biome.Builder()
                .temperature(0.7f)
                .downfall(0.3f)
                .precipitation(false)
                .effects(new BiomeEffects.Builder()
                        .skyColor(0xDEB887)        // Burlywood sky
                        .fogColor(0xD2B48C)        // Tan fog
                        .waterColor(0x4682B4)      // Steel blue water
                        .waterFogColor(0x050533)
                        .grassColor(0xDAA520)      // Goldenrod
                        .foliageColor(0xB8860B)    // Dark goldenrod
                        .build())
                .spawnSettings(new SpawnSettings.Builder()
                        .spawn(SpawnGroup.CREATURE, new SpawnSettings.SpawnEntry(EntityType.BAT, 3, 2, 4))
                        .spawn(SpawnGroup.CREATURE, new SpawnSettings.SpawnEntry(EntityType.LLAMA, 1, 2, 4))
                        .spawn(SpawnGroup.MONSTER, new SpawnSettings.SpawnEntry(EntityType.HUSK, 3, 1, 2))
                        .build())
                .generationSettings(new GenerationSettings.Builder().build())
                .build();
    }

    private Biome createSaltFlatsBiome() {
        return new Biome.Builder()
                .temperature(0.9f)
                .downfall(0.0f)
                .precipitation(false)
                .effects(new BiomeEffects.Builder()
                        .skyColor(0xF5F5DC)        // Beige sky (salt haze)
                        .fogColor(0xFFFFE0)        // Light yellow fog
                        .waterColor(0x00CED1)      // Dark turquoise (salt pools)
                        .waterFogColor(0x050533)
                        .grassColor(0xF5DEB3)      // Wheat color
                        .foliageColor(0xDEB887)    // Burlywood
                        .build())
                .spawnSettings(new SpawnSettings.Builder()
                        .spawn(SpawnGroup.MONSTER, new SpawnSettings.SpawnEntry(EntityType.HUSK, 1, 1, 1))
                        .build())
                .generationSettings(new GenerationSettings.Builder().build())
                .build();
    }
}