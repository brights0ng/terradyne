// ModBiomes.java - FIXED for Fabric 1.20.1 server-based registration

package net.terradyne.planet.biome;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.terradyne.Terradyne;

import java.util.HashMap;
import java.util.Map;

public class ModBiomes {

    // Registry keys for custom desert biomes
    public static final RegistryKey<Biome> DUNE_SEA = RegistryKey.of(RegistryKeys.BIOME,
            new Identifier(Terradyne.MOD_ID, "dune_sea"));

    public static final RegistryKey<Biome> GRANITE_MESAS = RegistryKey.of(RegistryKeys.BIOME,
            new Identifier(Terradyne.MOD_ID, "granite_mesas"));

    public static final RegistryKey<Biome> LIMESTONE_CANYONS = RegistryKey.of(RegistryKeys.BIOME,
            new Identifier(Terradyne.MOD_ID, "limestone_canyons"));

    public static final RegistryKey<Biome> SALT_FLATS = RegistryKey.of(RegistryKeys.BIOME,
            new Identifier(Terradyne.MOD_ID, "salt_flats"));

    // Cache registered biome entries to avoid re-registration
    private static final Map<RegistryKey<Biome>, RegistryEntry<Biome>> registeredBiomes = new HashMap<>();
    private static boolean biomesRegistered = false;

    /**
     * Register custom biomes with server registry - called when server is available
     */
    public static void init(MinecraftServer server) {
        if (biomesRegistered) {
            Terradyne.LOGGER.debug("Biomes already registered, skipping");
            return;
        }

        Terradyne.LOGGER.info("=== REGISTERING CUSTOM DESERT BIOMES WITH SERVER ===");

        try {
            // Get the biome registry from the server
            Registry<Biome> biomeRegistry = server.getRegistryManager().get(RegistryKeys.BIOME);

            if (!(biomeRegistry instanceof net.minecraft.registry.MutableRegistry)) {
                Terradyne.LOGGER.error("❌ Biome registry is not mutable - cannot register custom biomes");
                return;
            }

            net.minecraft.registry.MutableRegistry<Biome> mutableRegistry =
                    (net.minecraft.registry.MutableRegistry<Biome>) biomeRegistry;

            // Register the 4 custom desert biomes
            registerBiome(mutableRegistry, DUNE_SEA, DesertBiomes.createDuneSeaBiome());
            registerBiome(mutableRegistry, GRANITE_MESAS, DesertBiomes.createGraniteMesasBiome());
            registerBiome(mutableRegistry, LIMESTONE_CANYONS, DesertBiomes.createLimestoneCanyonsBiome());
            registerBiome(mutableRegistry, SALT_FLATS, DesertBiomes.createSaltFlatsBiome());

            biomesRegistered = true;
            Terradyne.LOGGER.info("✅ All {} custom desert biomes registered successfully", registeredBiomes.size());

        } catch (Exception e) {
            Terradyne.LOGGER.error("❌ Failed to register custom biomes: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Register a single biome and cache the entry
     */
    private static void registerBiome(net.minecraft.registry.MutableRegistry<Biome> registry,
                                      RegistryKey<Biome> key, Biome biome) {
        try {
            RegistryEntry<Biome> entry = registry.add(key, biome, com.mojang.serialization.Lifecycle.stable());
            registeredBiomes.put(key, entry);
            Terradyne.LOGGER.info("✅ Registered {} biome", key.getValue());
        } catch (Exception e) {
            Terradyne.LOGGER.error("❌ Failed to register biome {}: {}", key.getValue(), e.getMessage());
        }
    }

    /**
     * Get a registered biome entry (call after registerWithServer)
     */
    public static RegistryEntry<Biome> getBiomeEntry(RegistryKey<Biome> key) {
        RegistryEntry<Biome> entry = registeredBiomes.get(key);
        if (entry == null) {
            throw new RuntimeException("Biome " + key.getValue() + " not registered! Call registerWithServer first.");
        }
        return entry;
    }

    /**
     * Check if biomes have been registered
     */
    public static boolean areBiomesRegistered() {
        return biomesRegistered;
    }

    /**
     * Get all registered custom biome entries
     */
    public static Map<RegistryKey<Biome>, RegistryEntry<Biome>> getAllRegisteredBiomes() {
        return new HashMap<>(registeredBiomes);
    }
}