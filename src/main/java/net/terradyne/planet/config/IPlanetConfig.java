package net.terradyne.planet.config;

import net.terradyne.planet.PlanetType;
import net.terradyne.util.ModEnums;

public interface IPlanetConfig {
    String getPlanetName();
    long getSeed();
    ModEnums.PlanetAge getAge();
    PlanetType getType();

    // === UNIVERSAL TERRAIN PARAMETERS ===
    float getWindStrength();                    // Wind erosion factor (0.0-2.0)
    float getSurfaceTemperature();             // Average temperature (affects material behavior)
    float getLooseMaterialDensity();           // How much loose material vs solid (0.0-1.0)
    LooseMaterialType getLooseMaterialType();  // What kind of loose material

    // Enum for different loose materials
    enum LooseMaterialType {
        SAND,          // Desert planets
        SNOW,          // Ice planets
        VOLCANIC_ASH,  // Volcanic planets
        DUST,          // Rocky/barren planets
        REGOLITH,      // Moon-like planets
        ORGANIC_MATTER // Forest planets (leaf litter, etc.)
    }
}