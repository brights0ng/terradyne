package net.starlight.terradyne.planet.factory;

import net.starlight.terradyne.planet.physics.PlanetPhysicsConfig;
import net.starlight.terradyne.planet.physics.PlanetPhysicsModel;
import net.starlight.terradyne.util.ModEnums;

/**
 * Factory for creating physics-based planets
 */
public class PhysicsPlanetFactory {
    
    /**
     * Create a test planet with visible tectonic plates
     */
    public static PlanetPhysicsConfig createTectonicTestConfig(String planetName) {
        long seed = System.currentTimeMillis();
        
        return new PlanetPhysicsConfig(
            planetName,
            seed,
            ModEnums.PlanetAge.MATURE,
            0.8f,    // tectonicScale - lots of plates for testing
            0.1f,    // tectonicActivity - low activity
            0.3f,    // waterHeight - some water (not implemented yet)
            0.5f,    // atmosphereDensity - moderate atmosphere
            1.5f,    // distanceFromSun - Earth-like
            4.0f,    // planetCircumference - Earth-sized
            1.0f,    // rotationPeriod - 1 Earth day
            7.0f     // crustalThickness - Earth-like
        );
    }
    
    /**
     * Create a test planet with visible plate boundaries
     * Useful for debugging the continental noise system
     */
    public static PlanetPhysicsConfig createDebugPlanetConfig(String planetName) {
        long seed = System.currentTimeMillis();
        
        return new PlanetPhysicsConfig(
            planetName,
            seed,
            ModEnums.PlanetAge.MATURE,
            0.6f,    // tectonicScale - medium number of plates
            0.0f,    // tectonicActivity - no activity (flat plates)
            0.0f,    // waterHeight - no water for now
            0.3f,    // atmosphereDensity - thin atmosphere
            1.5f,    // distanceFromSun
            3.0f,    // planetCircumference - smaller planet
            1.0f,    // rotationPeriod
            5.0f     // crustalThickness
        );
    }
    
    /**
     * Create an Earth-like planet
     */
    public static PlanetPhysicsConfig createEarthLikeConfig(String planetName) {
        long seed = System.currentTimeMillis();
        
        return new PlanetPhysicsConfig(
            planetName,
            seed,
            ModEnums.PlanetAge.MATURE,
            1.0f,    // tectonicScale - Earth-like plates
            0.1f,    // tectonicActivity - Earth-like
            0.71f,   // waterHeight - 71% ocean
            1.0f,    // atmosphereDensity - 1 atmosphere
            1.5f,    // distanceFromSun - 150 million km
            4.0f,    // planetCircumference - 40,000 km
            1.0f,    // rotationPeriod - 24 hours
            7.0f     // crustalThickness - ~35km average
        );
    }
    
    /**
     * Create a Mars-like planet
     */
    public static PlanetPhysicsConfig createMarsLikeConfig(String planetName) {
        long seed = System.currentTimeMillis();
        
        return new PlanetPhysicsConfig(
            planetName,
            seed,
            ModEnums.PlanetAge.ANCIENT,
            0.3f,    // tectonicScale - few large plates
            0.0f,    // tectonicActivity - dead
            0.02f,   // waterHeight - polar ice only
            0.01f,   // atmosphereDensity - very thin
            2.3f,    // distanceFromSun - 230 million km
            2.1f,    // planetCircumference - 21,000 km
            1.03f,   // rotationPeriod - 24.6 hours
            5.0f     // crustalThickness - thicker than Earth
        );
    }
    
    /**
     * Create a Venus-like planet
     */
    public static PlanetPhysicsConfig createVenusLikeConfig(String planetName) {
        long seed = System.currentTimeMillis();
        
        return new PlanetPhysicsConfig(
            planetName,
            seed,
            ModEnums.PlanetAge.MATURE,
            0.1f,    // tectonicScale - minimal plates
            0.3f,    // tectonicActivity - some volcanism
            0.0f,    // waterHeight - no water
            60.0f,   // atmosphereDensity - extreme greenhouse
            1.1f,    // distanceFromSun - 110 million km
            3.8f,    // planetCircumference - 38,000 km
            243.0f,  // rotationPeriod - very slow
            10.0f    // crustalThickness - thick crust
        );
    }
    
    /**
     * Create the physics model from config
     */
    public static PlanetPhysicsModel createPhysicsModel(PlanetPhysicsConfig config) {
        return new PlanetPhysicsModel(config);
    }
}