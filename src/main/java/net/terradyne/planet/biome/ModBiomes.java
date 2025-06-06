package net.terradyne.planet.biome;

import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;

public class ModBiomes {

    // Registry keys for your desert biomes
    public static final RegistryKey<Biome> DUNE_SEA = RegistryKey.of(RegistryKeys.BIOME,
            new Identifier("terradyne", "dune_sea"));
    public static final RegistryKey<Biome> SCORCHING_WASTE = RegistryKey.of(RegistryKeys.BIOME,
            new Identifier("terradyne", "scorching_waste"));
    public static final RegistryKey<Biome> GRANITE_MESAS = RegistryKey.of(RegistryKeys.BIOME,
            new Identifier("terradyne", "granite_mesas"));
    // ... other biome keys ...

    // Registration method
    public static void init() {
        // Register using your existing DesertBiomes class
//        Registry.register(BuiltinRegistries.BIOME, DUNE_SEA.getValue(),
//                DesertBiomes.createDuneSeaBiome());

        // You can add more biome creation methods to DesertBiomes class:
        // Registry.register(BuiltinRegistries.BIOME, SCORCHING_WASTE.getValue(),
        //     DesertBiomes.createScorchingWasteBiome());
        // etc.
    }
}