package net.starlight.terradyne.planet.biome;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.starlight.terradyne.Terradyne;

/**
 * Registry keys for custom Terradyne biomes
 */
public class ModBiomes {
    
    /**
     * Debug biome - completely gray, no spawns, no features
     */
    public static final RegistryKey<Biome> DEBUG = RegistryKey.of(
            RegistryKeys.BIOME,
            new Identifier(Terradyne.MOD_ID, "debug")
    );
    
    /**
     * Initialize biome registry keys (called from main mod class)
     */
    public static void init() {
        Terradyne.LOGGER.info("Terradyne biome keys initialized:");
        Terradyne.LOGGER.info("  {} - Debug biome (gray, no spawns/features)", DEBUG.getValue());
    }
}