// Stage01Test.java
package net.starlight.terradyne.test;

import net.starlight.terradyne.planet.physics.*;


/**
 * Comprehensive test suite for Terradyne Stage 0.1 implementation.
 * Tests planet creation, physics calculations, and constraint validation.
 */
public class Stage0Test {

    public static void main(String[] args) {
        System.out.println("=== TERRADYNE STAGE 0.1 TEST SUITE ===\n");

        // Test all planet configurations
        testAllPlanetConfigs();

        // Test constraint validation specifically
        testConstraintValidation();

        // Test physics calculations in detail
        testPhysicsCalculations();

        // Test error handling
        testErrorHandling();

        System.out.println("\n=== ALL TESTS COMPLETED ===");
    }

    /**
     * Test all predefined planet configurations
     */
    public static void testAllPlanetConfigs() {
        System.out.println("--- TESTING ALL PLANET CONFIGURATIONS ---\n");

        PlanetConfig[] testPlanets = {
                new TestPlanetConfig.EarthLike(),
                new TestPlanetConfig.MarsLike(),
                new TestPlanetConfig.VenusLike(),
                new TestPlanetConfig.MoonLike(),
                new TestPlanetConfig.HadeanWorld()
        };

        for (PlanetConfig config : testPlanets) {
            testSinglePlanet(config);
            System.out.println(); // Blank line between planets
        }
    }

    /**
     * Test a single planet configuration
     */
    private static void testSinglePlanet(PlanetConfig config) {
        try {
            System.out.println("Testing: " + config.getPlanetName());
            System.out.println("Config: " + config.getClass().getSimpleName());

            // Create planet model
            PlanetModel planet = PlanetModel.fromConfig(config);

            // Show initial vs adjusted parameters
            PlanetData data = planet.getPlanetData();

            System.out.println("Original Parameters:");
            System.out.println("  Water Content: " + config.getWaterContent());
            System.out.println("  Atmospheric Density: " + config.getAtmosphericDensity());
            System.out.println("  Tectonic Activity: " + config.getTectonicActivity());

            System.out.println("Adjusted Parameters:");
            System.out.println("  Water Content: " + data.getAdjustedWaterContent());
            System.out.println("  Atmospheric Density: " + data.getAdjustedAtmosphericDensity());
            System.out.println("  Tectonic Activity: " + data.getAdjustedTectonicActivity());

            System.out.println("Calculated Properties:");
            System.out.println("  Gravity: " + String.format("%.2f m/s²", data.getGravity()));
            System.out.println("  Age: " + data.getPlanetAge());
            System.out.println("  Temperature: " + String.format("%.1f°C", data.getAverageSurfaceTemp()));
            System.out.println("  Habitability: " + String.format("%.3f", data.getHabitability()));
            System.out.println("  Water Erosion: " + String.format("%.3f", data.getWaterErosion()));
            System.out.println("  Wind Erosion: " + String.format("%.3f", data.getWindErosion()));
            System.out.println("  Main Rock: " + data.getMainRockType());

            // Test planet creation
            System.out.println("\nCreating planet systems...");
            planet.create();

            System.out.println("Planet Summary:");
            System.out.println(planet.getSummary());

            System.out.println("✓ " + config.getPlanetName() + " test PASSED");

        } catch (Exception e) {
            System.err.println("✗ " + config.getPlanetName() + " test FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test constraint validation system
     */
    public static void testConstraintValidation() {
        System.out.println("\n--- TESTING CONSTRAINT VALIDATION ---\n");

        // Test HADEAN water override
        System.out.println("Test 1: HADEAN planet water override");
        TestPlanetConfig.HadeanWorld hadeanConfig = new TestPlanetConfig.HadeanWorld();
        System.out.println("Input water content: " + hadeanConfig.getWaterContent());

        PlanetModel hadeanPlanet = PlanetModel.fromConfig(hadeanConfig);
        PlanetData hadeanData = hadeanPlanet.getPlanetData();
        System.out.println("Adjusted water content: " + hadeanData.getAdjustedWaterContent());

        if (hadeanData.getAdjustedWaterContent() == 0.0) {
            System.out.println("✓ HADEAN water constraint test PASSED");
        } else {
            System.out.println("✗ HADEAN water constraint test FAILED");
        }

        // Test VACUUM atmosphere override
        System.out.println("\nTest 2: VACUUM atmosphere density constraint");
        VacuumTestPlanet vacuumConfig = new VacuumTestPlanet();
        System.out.println("Input atmospheric density: " + vacuumConfig.getAtmosphericDensity());

        PlanetModel vacuumPlanet = PlanetModel.fromConfig(vacuumConfig);
        PlanetData vacuumData = vacuumPlanet.getPlanetData();
        System.out.println("Adjusted atmospheric density: " + vacuumData.getAdjustedAtmosphericDensity());

        if (vacuumData.getAdjustedAtmosphericDensity() == 0.0) {
            System.out.println("✓ VACUUM atmosphere constraint test PASSED");
        } else {
            System.out.println("✗ VACUUM atmosphere constraint test FAILED");
        }

        // Test REGOLITH tectonic activity override
        System.out.println("\nTest 3: REGOLITH tectonic activity constraint");
        RegolithTestPlanet regolithConfig = new RegolithTestPlanet();
        System.out.println("Input tectonic activity: " + regolithConfig.getTectonicActivity());

        PlanetModel regolithPlanet = PlanetModel.fromConfig(regolithConfig);
        PlanetData regolithData = regolithPlanet.getPlanetData();
        System.out.println("Adjusted tectonic activity: " + regolithData.getAdjustedTectonicActivity());

        if (regolithData.getAdjustedTectonicActivity() <= 0.3) {
            System.out.println("✓ REGOLITH tectonic constraint test PASSED");
        } else {
            System.out.println("✗ REGOLITH tectonic constraint test FAILED");
        }
    }

    /**
     * Test physics calculations in detail
     */
    public static void testPhysicsCalculations() {
        System.out.println("\n--- TESTING PHYSICS CALCULATIONS ---\n");

        // Test gravity calculation
        System.out.println("Test 1: Gravity scaling");
        TestPlanetConfig.EarthLike earthConfig = new TestPlanetConfig.EarthLike();
        PlanetModel earthPlanet = PlanetModel.fromConfig(earthConfig);
        double earthGravity = earthPlanet.getPlanetData().getGravity();
        System.out.println("Earth-like gravity: " + String.format("%.2f m/s²", earthGravity));

        TestPlanetConfig.MoonLike moonConfig = new TestPlanetConfig.MoonLike();
        PlanetModel moonPlanet = PlanetModel.fromConfig(moonConfig);
        double moonGravity = moonPlanet.getPlanetData().getGravity();
        System.out.println("Moon-like gravity: " + String.format("%.2f m/s²", moonGravity));

        if (moonGravity < earthGravity) {
            System.out.println("✓ Gravity scaling test PASSED (smaller planet has less gravity)");
        } else {
            System.out.println("✗ Gravity scaling test FAILED");
        }

        // Test temperature calculation
        System.out.println("\nTest 2: Temperature calculation");
        TestPlanetConfig.VenusLike venusConfig = new TestPlanetConfig.VenusLike();
        PlanetModel venusPlanet = PlanetModel.fromConfig(venusConfig);
        double venusTemp = venusPlanet.getPlanetData().getAverageSurfaceTemp();
        System.out.println("Venus-like temperature: " + String.format("%.1f°C", venusTemp));

        double earthTemp = earthPlanet.getPlanetData().getAverageSurfaceTemp();
        System.out.println("Earth-like temperature: " + String.format("%.1f°C", earthTemp));

        if (venusTemp > earthTemp) {
            System.out.println("✓ Temperature calculation test PASSED (closer planet is hotter)");
        } else {
            System.out.println("✗ Temperature calculation test FAILED");
        }

        // Test habitability calculation
        System.out.println("\nTest 3: Habitability calculation");
        double earthHabitability = earthPlanet.getPlanetData().getHabitability();
        double venusHabitability = venusPlanet.getPlanetData().getHabitability();
        double moonHabitability = moonPlanet.getPlanetData().getHabitability();

        System.out.println("Earth-like habitability: " + String.format("%.3f", earthHabitability));
        System.out.println("Venus-like habitability: " + String.format("%.3f", venusHabitability));
        System.out.println("Moon-like habitability: " + String.format("%.3f", moonHabitability));

        if (earthHabitability > venusHabitability && earthHabitability > moonHabitability) {
            System.out.println("✓ Habitability calculation test PASSED (Earth-like most habitable)");
        } else {
            System.out.println("✗ Habitability calculation test FAILED");
        }
    }

    /**
     * Test error handling and validation
     */
    public static void testErrorHandling() {
        System.out.println("\n--- TESTING ERROR HANDLING ---\n");

        // Test invalid parameters
        System.out.println("Test 1: Invalid parameter validation");
        try {
            InvalidTestPlanet invalidConfig = new InvalidTestPlanet();
            PlanetModel.fromConfig(invalidConfig); // Should throw exception
            System.out.println("✗ Invalid parameter test FAILED (no exception thrown)");
        } catch (IllegalArgumentException e) {
            System.out.println("✓ Invalid parameter test PASSED (exception: " + e.getMessage() + ")");
        }

        // Test double initialization
        System.out.println("\nTest 2: Double initialization prevention");
        try {
            TestPlanetConfig.EarthLike config = new TestPlanetConfig.EarthLike();
            PlanetModel planet = PlanetModel.fromConfig(config);
            planet.create();
            planet.create(); // Should throw exception
            System.out.println("✗ Double initialization test FAILED (no exception thrown)");
        } catch (IllegalStateException e) {
            System.out.println("✓ Double initialization test PASSED (exception: " + e.getMessage() + ")");
        }

        // Test accessing uninitialized systems
        System.out.println("\nTest 3: Uninitialized system access");
        try {
            TestPlanetConfig.MarsLike config = new TestPlanetConfig.MarsLike();
            PlanetModel planet = PlanetModel.fromConfig(config);
            // Don't call create()
            System.out.println("Accessing noise system on uninitialized planet...");
//            planet.getNoiseSystem(); // Should throw exception
            System.out.println("✗ Uninitialized access test FAILED (no exception thrown)");
        } catch (IllegalStateException e) {
            System.out.println("✓ Uninitialized access test PASSED (exception: " + e.getMessage() + ")");
        } catch (Exception e) {
            System.out.println("✗ Uninitialized access test FAILED (unexpected exception: " + e.getMessage() + ")");
        }
    }

    // Test planet configurations for constraint validation
    static class VacuumTestPlanet extends PlanetConfig {
        @Override public int getCircumference() { return 20000; }
        @Override public long getDistanceFromStar() { return 300; }
        @Override public PlanetData.CrustComposition getCrustComposition() { return PlanetData.CrustComposition.REGOLITH; }
        @Override public long getSeed() { return 99999L; }
        @Override public double getTectonicActivity() { return 0.5; }
        @Override public double getWaterContent() { return 0.3; }
        @Override public PlanetData.AtmosphereComposition getAtmosphereComposition() { return PlanetData.AtmosphereComposition.VACUUM; }
        @Override public int getCrustalThickness() { return 150; }
        @Override public double getAtmosphericDensity() { return 0.8; } // Should be overridden to 0.0
        @Override public double getRotationPeriod() { return 2.0; }
        @Override public String getPlanetName() { return "Vacuum_Test"; }
    }

    static class RegolithTestPlanet extends PlanetConfig {
        @Override public int getCircumference() { return 15000; }
        @Override public long getDistanceFromStar() { return 200; }
        @Override public PlanetData.CrustComposition getCrustComposition() { return PlanetData.CrustComposition.REGOLITH; }
        @Override public long getSeed() { return 88888L; }
        @Override public double getTectonicActivity() { return 0.9; } // Should be overridden to lower value
        @Override public double getWaterContent() { return 0.1; }
        @Override public PlanetData.AtmosphereComposition getAtmosphereComposition() { return PlanetData.AtmosphereComposition.TRACE_ATMOSPHERE; }
        @Override public int getCrustalThickness() { return 100; }
        @Override public double getAtmosphericDensity() { return 0.1; }
        @Override public double getRotationPeriod() { return 5.0; }
        @Override public String getPlanetName() { return "Regolith_Test"; }
    }

    static class InvalidTestPlanet extends PlanetConfig {
        @Override public int getCircumference() { return -1000; } // Invalid!
        @Override public long getDistanceFromStar() { return 150; }
        @Override public PlanetData.CrustComposition getCrustComposition() { return PlanetData.CrustComposition.SILICATE; }
        @Override public long getSeed() { return 77777L; }
        @Override public double getTectonicActivity() { return 1.5; } // Invalid!
        @Override public double getWaterContent() { return 0.5; }
        @Override public PlanetData.AtmosphereComposition getAtmosphereComposition() { return PlanetData.AtmosphereComposition.OXYGEN_RICH; }
        @Override public int getCrustalThickness() { return 200; }
        @Override public double getAtmosphericDensity() { return 0.5; }
        @Override public double getRotationPeriod() { return 1.0; }
        @Override public String getPlanetName() { return "Invalid_Test"; }
    }
}