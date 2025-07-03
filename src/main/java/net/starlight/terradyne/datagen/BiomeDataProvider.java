package net.starlight.terradyne.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.biome.*;
import net.starlight.terradyne.planet.biome.ModBiomes;

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
        // Register our debug biome
        entries.add(ModBiomes.DEBUG, createDebugBiome());
    }

    @Override
    public String getName() {
        return "Terradyne Biomes";
    }

    /**
     * Create the debug biome - completely gray with no spawns or features
     */
    private Biome createDebugBiome() {
        return new Biome.Builder()
                .temperature(0.5f)         // Neutral temperature
                .downfall(0.0f)           // No rainfall
                .precipitation(false)      // No precipitation
                .effects(new BiomeEffects.Builder()
                        .skyColor(0x78A7FF)        // Normal blue sky
                        .fogColor(0xC0D8FF)        // Light blue fog
                        .waterColor(0x5F7F9F)      // Muted blue water
                        .waterFogColor(0x4F6F8F)   // Darker muted blue water fog
                        .grassColor(0x6B8E5A)      // Muted green grass
                        .foliageColor(0x7F9F6B)    // Muted green foliage
                        .build())
                .spawnSettings(new SpawnSettings.Builder()
                        // No spawn entries = no mob spawns
                        .build())
                .generationSettings(new GenerationSettings.Builder()
                        // No generation features = no structures, ores, lakes, etc.
                        .build())
                .build();
    }
}