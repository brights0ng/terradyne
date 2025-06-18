// TestPlanetConfig.java
package net.starlight.terradyne.planet.physics;

/**
 * Test planet configurations for development and testing.
 * Provides several preset planet types for experimentation.
 */
public class TestPlanetConfig {
    
    /**
     * Earth-like planet configuration
     */
    public static class EarthLike extends PlanetConfig {
        @Override
        public int getCircumference() { return 40000; } // Close to Earth
        
        @Override
        public long getDistanceFromStar() { return 150; } // 1 AU
        
        @Override
        public PlanetData.CrustComposition getCrustComposition() { return PlanetData.CrustComposition.SILICATE; }
        
        @Override
        public long getSeed() { return 12345L; }
        
        @Override
        public double getTectonicActivity() { return 0.6; } // Moderate activity
        
        @Override
        public double getWaterContent() { return 0.7; } // Lots of water
        
        @Override
        public PlanetData.AtmosphereComposition getAtmosphereComposition() { return PlanetData.AtmosphereComposition.OXYGEN_RICH; }
        
        @Override
        public int getCrustalThickness() { return 300; } // 30 block average height
        
        @Override
        public double getAtmosphericDensity() { return 0.5; } // Earth-like
        
        @Override
        public double getRotationPeriod() { return 1.0; } // 24 hour day
        
        @Override
        public String getPlanetName() { return "Terra_Prime"; }
    }
    
    /**
     * Mars-like desert planet configuration
     */
    public static class MarsLike extends PlanetConfig {
        @Override
        public int getCircumference() { return 21000; } // Smaller than Earth
        
        @Override
        public long getDistanceFromStar() { return 228; } // Mars distance
        
        @Override
        public PlanetData.CrustComposition getCrustComposition() { return PlanetData.CrustComposition.FERROUS; }
        
        @Override
        public long getSeed() { return 67890L; }
        
        @Override
        public double getTectonicActivity() { return 0.1; } // Low activity
        
        @Override
        public double getWaterContent() { return 0.1; } // Very little water
        
        @Override
        public PlanetData.AtmosphereComposition getAtmosphereComposition() { return PlanetData.AtmosphereComposition.CARBON_DIOXIDE; }
        
        @Override
        public int getCrustalThickness() { return 200; } // Lower elevation
        
        @Override
        public double getAtmosphericDensity() { return 0.2; } // Thin atmosphere
        
        @Override
        public double getRotationPeriod() { return 1.03; } // Slightly longer day
        
        @Override
        public String getPlanetName() { return "Ares_Secundus"; }
    }
    
    /**
     * Venus-like hell world configuration
     */
    public static class VenusLike extends PlanetConfig {
        @Override
        public int getCircumference() { return 38000; } // Similar to Earth
        
        @Override
        public long getDistanceFromStar() { return 108; } // Venus distance
        
        @Override
        public PlanetData.CrustComposition getCrustComposition() { return PlanetData.CrustComposition.BASALT; }
        
        @Override
        public long getSeed() { return 24680L; }
        
        @Override
        public double getTectonicActivity() { return 0.8; } // High activity
        
        @Override
        public double getWaterContent() { return 0.0; } // No water
        
        @Override
        public PlanetData.AtmosphereComposition getAtmosphereComposition() { return PlanetData.AtmosphereComposition.CARBON_DIOXIDE; }
        
        @Override
        public int getCrustalThickness() { return 400; } // Thick crust
        
        @Override
        public double getAtmosphericDensity() { return 0.9; } // Very thick atmosphere
        
        @Override
        public double getRotationPeriod() { return 243.0; } // Very slow rotation
        
        @Override
        public String getPlanetName() { return "Aphrodite_Inferno"; }
    }
    
    /**
     * Moon-like airless world configuration
     */
    public static class MoonLike extends PlanetConfig {
        @Override
        public int getCircumference() { return 11000; } // Small
        
        @Override
        public long getDistanceFromStar() { return 150; } // Earth distance (as moon)
        
        @Override
        public PlanetData.CrustComposition getCrustComposition() { return PlanetData.CrustComposition.REGOLITH; }
        
        @Override
        public long getSeed() { return 13579L; }
        
        @Override
        public double getTectonicActivity() { return 0.0; } // Dead
        
        @Override
        public double getWaterContent() { return 0.05; } // Trace ice
        
        @Override
        public PlanetData.AtmosphereComposition getAtmosphereComposition() { return PlanetData.AtmosphereComposition.VACUUM; }
        
        @Override
        public int getCrustalThickness() { return 100; } // Thin crust
        
        @Override
        public double getAtmosphericDensity() { return 0.0; } // No atmosphere
        
        @Override
        public double getRotationPeriod() { return 27.3; } // Tidally locked
        
        @Override
        public String getPlanetName() { return "Luna_Desolata"; }
    }
    
    /**
     * Hadean hell world (early formation stage)
     */
    public static class HadeanWorld extends PlanetConfig {
        @Override
        public int getCircumference() { return 35000; }
        
        @Override
        public long getDistanceFromStar() { return 120; }
        
        @Override
        public PlanetData.CrustComposition getCrustComposition() { return PlanetData.CrustComposition.HADEAN; }
        
        @Override
        public long getSeed() { return 97531L; }
        
        @Override
        public double getTectonicActivity() { return 1.0; } // Maximum activity
        
        @Override
        public double getWaterContent() { return 0.8; } // Will be overridden to 0.0
        
        @Override
        public PlanetData.AtmosphereComposition getAtmosphereComposition() { return PlanetData.AtmosphereComposition.WATER_VAPOR_RICH; }
        
        @Override
        public int getCrustalThickness() { return 50; } // Very thin, molten
        
        @Override
        public double getAtmosphericDensity() { return 0.7; }
        
        @Override
        public double getRotationPeriod() { return 0.3; } // Fast rotation
        
        @Override
        public String getPlanetName() { return "Hades_Primordial"; }
    }
}