package net.terradyne.planet.dimension;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.dimension.DimensionType;
import net.terradyne.Terradyne;

/**
 * Registry keys for custom planet dimension types
 */
public class ModDimensionTypes {

    // Desert planet dimension type
    public static final RegistryKey<DimensionType> DESERT_PLANET = RegistryKey.of(
            RegistryKeys.DIMENSION_TYPE,
            new Identifier(Terradyne.MOD_ID, "desert_planet")
    );

    // Volcanic planet dimension type
    public static final RegistryKey<DimensionType> VOLCANIC_PLANET = RegistryKey.of(
            RegistryKeys.DIMENSION_TYPE,
            new Identifier(Terradyne.MOD_ID, "volcanic_planet")
    );

    // Icy planet dimension type
    public static final RegistryKey<DimensionType> ICY_PLANET = RegistryKey.of(
            RegistryKeys.DIMENSION_TYPE,
            new Identifier(Terradyne.MOD_ID, "icy_planet")
    );

    // Rocky/airless planet dimension type
    public static final RegistryKey<DimensionType> ROCKY_PLANET = RegistryKey.of(
            RegistryKeys.DIMENSION_TYPE,
            new Identifier(Terradyne.MOD_ID, "rocky_planet")
    );

    // Oceanic planet dimension type
    public static final RegistryKey<DimensionType> OCEANIC_PLANET = RegistryKey.of(
            RegistryKeys.DIMENSION_TYPE,
            new Identifier(Terradyne.MOD_ID, "oceanic_planet")
    );

    // Iron/metallic planet dimension type
    public static final RegistryKey<DimensionType> IRON_PLANET = RegistryKey.of(
            RegistryKeys.DIMENSION_TYPE,
            new Identifier(Terradyne.MOD_ID, "iron_planet")
    );

    // Carbon planet dimension type
    public static final RegistryKey<DimensionType> CARBON_PLANET = RegistryKey.of(
            RegistryKeys.DIMENSION_TYPE,
            new Identifier(Terradyne.MOD_ID, "carbon_planet")
    );

    /**
     * Initialize dimension types (called from main mod class)
     */
    public static void init() {
        Terradyne.LOGGER.info("Registered custom planet dimension type keys");
        // The actual dimension type instances are created dynamically
        // when planets are generated, based on planet characteristics
    }
}