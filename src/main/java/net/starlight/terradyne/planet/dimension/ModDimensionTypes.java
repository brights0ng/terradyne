package net.starlight.terradyne.planet.dimension;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.dimension.DimensionType;
import net.starlight.terradyne.Terradyne;

/**
 * Registry keys for custom planet dimension types
 */
public class ModDimensionTypes {

    /**
     * Enum for Terradyne dimension type variants
     */
    public enum TerradyneDimensionType {
        HABITABLE("habitable"),                 // Oxygen-rich, good living conditions
        ULTRAWARM("ultrawarm"),                 // Hadean/extremely hot planets  
        THICK_ATMOSPHERE("thick_atmosphere"),   // Dense greenhouse effect
        STANDARD("standard"),                   // Vacuum/thin/non-breathable atmosphere
        TOXIC("toxic");                         // Hostile atmosphere composition

        private final String name;

        TerradyneDimensionType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    // Registry keys for all dimension types
    public static final RegistryKey<DimensionType> TERRADYNE_HABITABLE = RegistryKey.of(
            RegistryKeys.DIMENSION_TYPE,
            new Identifier(Terradyne.MOD_ID, "habitable")
    );

    public static final RegistryKey<DimensionType> TERRADYNE_ULTRAWARM = RegistryKey.of(
            RegistryKeys.DIMENSION_TYPE,
            new Identifier(Terradyne.MOD_ID, "ultrawarm")
    );

    public static final RegistryKey<DimensionType> TERRADYNE_THICK_ATMOSPHERE = RegistryKey.of(
            RegistryKeys.DIMENSION_TYPE,
            new Identifier(Terradyne.MOD_ID, "thick_atmosphere")
    );

    public static final RegistryKey<DimensionType> TERRADYNE_STANDARD = RegistryKey.of(
            RegistryKeys.DIMENSION_TYPE,
            new Identifier(Terradyne.MOD_ID, "standard")
    );

    public static final RegistryKey<DimensionType> TERRADYNE_TOXIC = RegistryKey.of(
            RegistryKeys.DIMENSION_TYPE,
            new Identifier(Terradyne.MOD_ID, "toxic")
    );

    /**
     * Get registry key for dimension type
     */
    public static RegistryKey<DimensionType> getRegistryKey(TerradyneDimensionType type) {
        return switch (type) {
            case HABITABLE -> TERRADYNE_HABITABLE;
            case ULTRAWARM -> TERRADYNE_ULTRAWARM;
            case THICK_ATMOSPHERE -> TERRADYNE_THICK_ATMOSPHERE;
            case STANDARD -> TERRADYNE_STANDARD;
            case TOXIC -> TERRADYNE_TOXIC;
        };
    }
    
    /**
     * Initialize dimension types (called from main mod class)
     * NOTE: Actual registration happens at server startup via DimensionTypeRegistrar
     */
    public static void init() {
        Terradyne.LOGGER.info("Terradyne dimension type keys initialized:");
        Terradyne.LOGGER.info("  {} - {}", TERRADYNE_HABITABLE.getValue(),
                DimensionTypeFactory.getDimensionTypeDescription(TerradyneDimensionType.HABITABLE));
        Terradyne.LOGGER.info("  {} - {}", TERRADYNE_ULTRAWARM.getValue(),
                DimensionTypeFactory.getDimensionTypeDescription(TerradyneDimensionType.ULTRAWARM));
        Terradyne.LOGGER.info("  {} - {}", TERRADYNE_THICK_ATMOSPHERE.getValue(),
                DimensionTypeFactory.getDimensionTypeDescription(TerradyneDimensionType.THICK_ATMOSPHERE));
        Terradyne.LOGGER.info("  {} - {}", TERRADYNE_STANDARD.getValue(),
                DimensionTypeFactory.getDimensionTypeDescription(TerradyneDimensionType.STANDARD));
        Terradyne.LOGGER.info("  {} - {}", TERRADYNE_TOXIC.getValue(),
                DimensionTypeFactory.getDimensionTypeDescription(TerradyneDimensionType.TOXIC));

        Terradyne.LOGGER.info("Dimension types will be registered at server startup via DimensionTypeRegistrar");
    }
}