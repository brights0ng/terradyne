package net.terradyne.planet.biome;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;

/**
 * Registry keys for custom biomes
 * Biomes are registered on server start via BiomeRegistrationManager
 */
public class ModBiomes {

    // === DESERT PLANET BIOMES (4 core biomes) ===

    public static final RegistryKey<Biome> DUNE_SEA = RegistryKey.of(RegistryKeys.BIOME,
            new Identifier("terradyne", "dune_sea"));

    public static final RegistryKey<Biome> GRANITE_MESAS = RegistryKey.of(RegistryKeys.BIOME,
            new Identifier("terradyne", "granite_mesas"));

    public static final RegistryKey<Biome> LIMESTONE_CANYONS = RegistryKey.of(RegistryKeys.BIOME,
            new Identifier("terradyne", "limestone_canyons"));

    public static final RegistryKey<Biome> SALT_FLATS = RegistryKey.of(RegistryKeys.BIOME,
            new Identifier("terradyne", "salt_flats"));

    // === FUTURE BIOMES (when other planets are added) ===

    // Oceanic biomes
    public static final RegistryKey<Biome> TROPICAL_OCEAN = RegistryKey.of(RegistryKeys.BIOME,
            new Identifier("terradyne", "tropical_ocean"));
    public static final RegistryKey<Biome> DEEP_ABYSS = RegistryKey.of(RegistryKeys.BIOME,
            new Identifier("terradyne", "deep_abyss"));
    public static final RegistryKey<Biome> CONTINENTAL_SHELF = RegistryKey.of(RegistryKeys.BIOME,
            new Identifier("terradyne", "continental_shelf"));
    public static final RegistryKey<Biome> POLAR_ICE = RegistryKey.of(RegistryKeys.BIOME,
            new Identifier("terradyne", "polar_ice"));

    // Rocky biomes
    public static final RegistryKey<Biome> CRATER_HIGHLANDS = RegistryKey.of(RegistryKeys.BIOME,
            new Identifier("terradyne", "crater_highlands"));
    public static final RegistryKey<Biome> REGOLITH_PLAINS = RegistryKey.of(RegistryKeys.BIOME,
            new Identifier("terradyne", "regolith_plains"));
    public static final RegistryKey<Biome> SHATTERED_BADLANDS = RegistryKey.of(RegistryKeys.BIOME,
            new Identifier("terradyne", "shattered_badlands"));
    public static final RegistryKey<Biome> METALLIC_FIELDS = RegistryKey.of(RegistryKeys.BIOME,
            new Identifier("terradyne", "metallic_fields"));

    /**
     * No longer registers biomes here - done on server start in BiomeRegistrationManager
     */
    public static void init() {
        // Registry keys are created above, actual registration happens in BiomeRegistrationManager
        // when server starts and we have access to the server's registry manager
    }
}