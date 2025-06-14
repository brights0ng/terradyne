package net.starlight.terradyne.planet.physics;

import net.starlight.terradyne.planet.PlanetType;

/**
 * Interface for planet models
 * Models calculate derived properties from configurations
 */
public interface IPlanetModel {
    
    /**
     * Get the configuration this model was created from
     */
    IPlanetConfig getConfig();
    
    /**
     * Get the planet type (emergent from physics)
     */
    PlanetType getType();
    
    /**
     * Get gravity in Earth gravities (1.0 = Earth gravity)
     */
    float getGravity();
    
    /**
     * Get atmospheric pressure in Earth atmospheres
     */
    float getAtmosphericPressure();
    
    /**
     * Get erosion rate (will be calculated from wind/water)
     */
    float getErosionRate();
    
    /**
     * Check if planet has loose material formations (dunes, snow drifts, etc)
     */
    boolean hasLooseMaterialFormations();
    
    /**
     * Get maximum height of loose material formations
     */
    float getLooseMaterialFormationHeight();
    
    /**
     * Get solid material exposure ratio (0.0 = all covered, 1.0 = all exposed)
     */
    float getSolidMaterialExposure();
}