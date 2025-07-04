package net.starlight.terradyne.planet.biome;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.starlight.terradyne.Terradyne;

/**
 * Registry keys for all Terradyne biomes
 * Physics-based biome classification with ~40 distinct biomes
 */
public class ModBiomes {
    
    // === WATER BIOMES ===
    public static final RegistryKey<Biome> FROZEN_OCEAN = create("frozen_ocean");
    public static final RegistryKey<Biome> FRIGID_OCEAN = create("frigid_ocean");
    public static final RegistryKey<Biome> DEAD_OCEAN = create("dead_ocean");
    public static final RegistryKey<Biome> OCEAN = create("ocean");
    public static final RegistryKey<Biome> WARM_OCEAN = create("warm_ocean");
    public static final RegistryKey<Biome> CORAL_OCEAN = create("coral_ocean");
    public static final RegistryKey<Biome> TROPICAL_OCEAN = create("tropical_ocean");
    public static final RegistryKey<Biome> BOILING_OCEAN = create("boiling_ocean");
    
    // === MOUNTAIN BIOMES (Volatility 4-5) ===
    public static final RegistryKey<Biome> FROZEN_PEAKS = create("frozen_peaks");
    public static final RegistryKey<Biome> MOUNTAIN_FOOTHILLS = create("mountain_foothills");
    public static final RegistryKey<Biome> MOUNTAIN_PEAKS = create("mountain_peaks");
    public static final RegistryKey<Biome> ALPINE_PEAKS = create("alpine_peaks");
    public static final RegistryKey<Biome> VOLCANIC_WASTELAND = create("volcanic_wasteland");
    public static final RegistryKey<Biome> VOLCANIC_MOUNTAINS = create("volcanic_mountains");
    
    // === HIGHLAND BIOMES (Volatility 2-3) ===
    public static final RegistryKey<Biome> BARREN_HIGHLANDS = create("barren_highlands");
    public static final RegistryKey<Biome> WINDSWEPT_HILLS = create("windswept_hills");
    public static final RegistryKey<Biome> ROLLING_HILLS = create("rolling_hills");
    public static final RegistryKey<Biome> HIGHLAND_TUNDRA = create("highland_tundra");
    public static final RegistryKey<Biome> FORESTED_HILLS = create("forested_hills");
    public static final RegistryKey<Biome> TROPICAL_HIGHLANDS = create("tropical_highlands");
    
    // === HOSTILE CONTINENTAL BIOMES (Volatility 0-1, Habitability < 0.4) ===
    public static final RegistryKey<Biome> FROZEN_WASTELAND = create("frozen_wasteland");
    public static final RegistryKey<Biome> ROCKY_DESERT = create("rocky_desert");
    public static final RegistryKey<Biome> SCORCHED_PLAINS = create("scorched_plains");
    public static final RegistryKey<Biome> WINDSWEPT_TUNDRA = create("windswept_tundra");
    public static final RegistryKey<Biome> SANDY_DESERT = create("sandy_desert");
    public static final RegistryKey<Biome> DESERT_MESA = create("desert_mesa");
    public static final RegistryKey<Biome> DUST_BOWL = create("dust_bowl");
    
    // === MARGINAL CONTINENTAL BIOMES (Volatility 0-1, Habitability 0.4-0.7) ===
    public static final RegistryKey<Biome> COLD_STEPPES = create("cold_steppes");
    public static final RegistryKey<Biome> TUNDRA = create("tundra");
    public static final RegistryKey<Biome> BOREAL_PLAINS = create("boreal_plains");
    public static final RegistryKey<Biome> DRY_STEPPES = create("dry_steppes");
    public static final RegistryKey<Biome> TEMPERATE_STEPPES = create("temperate_steppes");
    public static final RegistryKey<Biome> MEADOWS = create("meadows");
    public static final RegistryKey<Biome> SAVANNA = create("savanna");
    public static final RegistryKey<Biome> TROPICAL_GRASSLAND = create("tropical_grassland");
    
    // === THRIVING CONTINENTAL BIOMES (Volatility 0-1, Habitability > 0.7) ===
    // Cold Zone
    public static final RegistryKey<Biome> SNOWY_PLAINS = create("snowy_plains");
    public static final RegistryKey<Biome> TAIGA = create("taiga");
    public static final RegistryKey<Biome> SNOW_FOREST = create("snow_forest");
    public static final RegistryKey<Biome> ALPINE_MEADOWS = create("alpine_meadows");
    
    // Temperate Zone
    public static final RegistryKey<Biome> PLAINS = create("plains");
    public static final RegistryKey<Biome> MIXED_PLAINS = create("mixed_plains");
    public static final RegistryKey<Biome> WETLANDS = create("wetlands");
    public static final RegistryKey<Biome> OAK_FOREST = create("oak_forest");
    public static final RegistryKey<Biome> MIXED_FOREST = create("mixed_forest");
    public static final RegistryKey<Biome> DENSE_FOREST = create("dense_forest");
    public static final RegistryKey<Biome> MOUNTAIN_FOREST = create("mountain_forest");
    
    // Warm Zone
    public static final RegistryKey<Biome> HOT_SHRUBLAND = create("hot_shrubland");
    public static final RegistryKey<Biome> WINDY_STEPPES = create("windy_steppes");
    public static final RegistryKey<Biome> TEMPERATE_RAINFOREST = create("temperate_rainforest");
    public static final RegistryKey<Biome> CLOUD_FOREST = create("cloud_forest");
    public static final RegistryKey<Biome> JUNGLE = create("jungle");
    public static final RegistryKey<Biome> TROPICAL_RAINFOREST = create("tropical_rainforest");
    
    // Hot Zone
    public static final RegistryKey<Biome> HOT_DESERT = create("hot_desert");
    public static final RegistryKey<Biome> TROPICAL_SWAMP = create("tropical_swamp");
    
    // === EXTREME BIOMES ===
    public static final RegistryKey<Biome> EXTREME_FROZEN_WASTELAND = create("extreme_frozen_wasteland");
    public static final RegistryKey<Biome> MOLTEN_WASTELAND = create("molten_wasteland");
    
    // === DEBUG BIOME ===
    public static final RegistryKey<Biome> DEBUG = create("debug");
    
    /**
     * Helper method to create biome registry keys
     */
    private static RegistryKey<Biome> create(String name) {
        return RegistryKey.of(RegistryKeys.BIOME, new Identifier(Terradyne.MOD_ID, name));
    }
    
    /**
     * Initialize biome registry keys (called from main mod class)
     */
    public static void init() {
        Terradyne.LOGGER.info("Terradyne biome keys initialized:");
        Terradyne.LOGGER.info("  Water biomes: 8 types (Frozen Ocean → Boiling Ocean)");
        Terradyne.LOGGER.info("  Mountain biomes: 6 types (Volatility 4-5)");
        Terradyne.LOGGER.info("  Highland biomes: 6 types (Volatility 2-3)");
        Terradyne.LOGGER.info("  Continental biomes: 23 types (Volatility 0-1)");
        Terradyne.LOGGER.info("  Extreme biomes: 2 types (Temperature overrides)");
        Terradyne.LOGGER.info("  Total: ~45 physics-based biomes");
    }
}