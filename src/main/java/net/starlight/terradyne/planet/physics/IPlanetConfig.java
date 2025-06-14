package net.starlight.terradyne.planet.physics;

import net.starlight.terradyne.planet.PlanetType;
import net.starlight.terradyne.util.ModEnums;

/**
 * Interface for planet configurations
 * Configurations store the input parameters for planets
 */
public interface IPlanetConfig {
    
    /**
     * Get the planet's name
     */
    String getPlanetName();
    
    /**
     * Get the seed for generation
     */
    long getSeed();
    
    /**
     * Get the planet's age
     */
    ModEnums.PlanetAge getAge();
    
    /**
     * Get the planet type (for physics configs, this is calculated)
     */
    PlanetType getType();
    
    // Generic parameters that all planets have
    
    /**
     * Get wind strength (0.0 = no wind, 2.0 = extreme wind)
     */
    float getWindStrength();
    
    /**
     * Get surface temperature in Celsius
     */
    float getSurfaceTemperature();


    float getTectonicScale();
    
    /**
     * Get loose material density (0.0 = none, 1.0 = fully covered)
     */
    float getLooseMaterialDensity();
    
    /**
     * Get the type of loose material on this planet
     */
    LooseMaterialType getLooseMaterialType();
    
    /**
     * Types of loose materials found on planets
     */
    enum LooseMaterialType {
        SAND,           // Desert planets
        SNOW,           // Ice planets
        VOLCANIC_ASH,   // Volcanic planets
        DUST,           // Rocky/barren planets
        REGOLITH,       // Moon-like planets
        ORGANIC_MATTER  // Forest planets
    }
}