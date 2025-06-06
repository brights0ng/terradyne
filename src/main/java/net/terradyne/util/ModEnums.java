package net.terradyne.util;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringIdentifiable;

public class ModEnums {

    // Enums for primary inputs
    public enum PlanetAge {
        YOUNG, MATURE, ANCIENT;

        public static final Codec<PlanetAge> CODEC = Codec.STRING.xmap(
                name -> PlanetAge.valueOf(name.toUpperCase()),
                age -> age.name().toLowerCase()
        );
    }

    // Updated CrustComposition enum with all types
    public enum CrustComposition {
        SILICATE_RICH,    // Rocky, moderate reflectivity
        IRON_RICH,        // Metallic, low reflectivity
        CARBON_RICH,      // Dark, very low reflectivity
        ICE_RICH,         // Frozen water/methane, high reflectivity
        SULFUR_RICH,      // Volcanic, yellow tints, moderate reflectivity
        SALT_RICH,        // Dried ocean beds, high reflectivity
        BASALTIC,         // Volcanic rock, dark, low reflectivity
        GRANITE,          // Light colored rock, high reflectivity
        SANDSTONE;         // Sedimentary, moderate-high reflectivity

        public static final Codec<CrustComposition> CODEC = Codec.STRING.xmap(
                name -> CrustComposition.valueOf(name.toUpperCase()),
                comp -> comp.name().toLowerCase()
        );
    }

    // Enums for derived characteristics
    public enum DiurnalTemperatureSwing {
        MILD, MODERATE, EXTREME;

        public static final Codec<DiurnalTemperatureSwing> CODEC = Codec.STRING.xmap(
                name -> DiurnalTemperatureSwing.valueOf(name.toUpperCase()),
                swing -> swing.name().toLowerCase()
        );
    }

    public enum WaterScarcity {
        SEMI_ARID_FRINGES, ARID, HYPER_ARID;

        public static final Codec<WaterScarcity> CODEC = Codec.STRING.xmap(
                name -> WaterScarcity.valueOf(name.toUpperCase()),
                scarcity -> scarcity.name().toLowerCase()
        );
    }

    // Data class for sky colors
    public static class SkyColorProfile {
        public final int zenithColor;    // RGB color for sky at zenith
        public final int horizonColor;   // RGB color for sky at horizon
        public final int fogColor;       // RGB color for fog/haze

        public SkyColorProfile(int zenithColor, int horizonColor, int fogColor) {
            this.zenithColor = zenithColor;
            this.horizonColor = horizonColor;
            this.fogColor = fogColor;
        }
    }
}
