package net.starlight.terradyne.datagen;

import net.starlight.terradyne.planet.physics.*;
import net.starlight.terradyne.starsystem.StarSystemModel;
import net.starlight.terradyne.starsystem.PlanetaryBody;

import java.util.Map;

/**
 * Hardcoded planet definitions integrated with StarSystemModel
 * Contains the 5 main solar system planets with realistic atmospheric and geological properties
 * UPDATED: Now connects with StarSystemModel for orbital mechanics
 */
public class HardcodedPlanets {

    /**
     * Mercury - Closest to sun, airless, extreme temperatures
     */
    public static final PlanetConfig MERCURY = new PlanetConfig("Mercury", "mercury".hashCode())
            .setCircumference(15329)              // Real circumference: ~15,329 km
            .setDistanceFromStar(58)              // Average distance: 58 million km from sun
            .setCrustComposition(CrustComposition.REGOLITHIC)  // Heavily cratered, rocky surface
            .setAtmosphereComposition(AtmosphereComposition.VACUUM)  // Essentially no atmosphere
            .setTectonicActivity(0.0)             // Geologically dead
            .setWaterContent(0.0)                 // No water
            .setCrustalThickness(50.0)            // Thick crust relative to size
            .setAtmosphericDensity(0.0)           // No atmosphere
            .setRotationPeriod(58.6)              // 58.6 Earth days (tidally locked 3:2)
            .setNoiseScale(0.004);                // Small, fractured terrain features

    /**
     * Venus - Hot greenhouse world with thick atmosphere
     */
    public static final PlanetConfig VENUS = new PlanetConfig("Venus", "venus".hashCode())
            .setCircumference(38025)              // Real circumference: ~38,025 km
            .setDistanceFromStar(108)             // Average distance: 108 million km from sun
            .setCrustComposition(CrustComposition.BASALTIC)  // Volcanic basaltic surface
            .setAtmosphereComposition(AtmosphereComposition.CARBON_DIOXIDE)  // 96% CO2, extreme greenhouse
            .setTectonicActivity(0.3)             // Some volcanic activity
            .setWaterContent(0.0)                 // All water evaporated/lost
            .setCrustalThickness(40.0)            // Moderate crust thickness
            .setAtmosphericDensity(1.0)           // 90x Earth's atmospheric pressure
            .setRotationPeriod(243.0)             // 243 Earth days (retrograde!)
            .setNoiseScale(0.002);                // Large volcanic features

    /**
     * Earth - Our reference habitable planet
     */
    public static final PlanetConfig EARTH = new PlanetConfig("Earth", "earth".hashCode())
            .setCircumference(40075)              // Real circumference: 40,075 km
            .setDistanceFromStar(150)             // Average distance: 150 million km (1 AU)
            .setCrustComposition(CrustComposition.SILICATE)  // Silicate continental and oceanic crust
            .setAtmosphereComposition(AtmosphereComposition.OXYGEN_RICH)  // 21% O2, 78% N2
            .setTectonicActivity(0.6)             // Active plate tectonics
            .setWaterContent(0.7)                 // ~70% surface covered by water
            .setCrustalThickness(35.0)            // Average continental crust thickness
            .setAtmosphericDensity(1.0)           // Reference atmosphere (1.0 by definition)
            .setRotationPeriod(1.0)               // 24 hours (1.0 by definition)
            .setNoiseScale(0.002);                // Moderate tectonic plate size

    /**
     * Mars - Cold, thin atmosphere, evidence of past water
     */
    public static final PlanetConfig MARS = new PlanetConfig("Mars", "mars".hashCode())
            .setCircumference(21344)              // Real circumference: ~21,344 km
            .setDistanceFromStar(228)             // Average distance: 228 million km from sun
            .setCrustComposition(CrustComposition.FERROUS)  // Iron-rich, oxidized (rusty) surface
            .setAtmosphereComposition(AtmosphereComposition.TRACE_ATMOSPHERE)  // Thin CO2 atmosphere
            .setTectonicActivity(0.1)             // Mostly geologically inactive
            .setWaterContent(0.1)                 // Polar ice caps, subsurface ice
            .setCrustalThickness(50.0)            // Thick crust, no active tectonics
            .setAtmosphericDensity(0.01)          // ~1% of Earth's atmospheric pressure
            .setRotationPeriod(1.03)              // 24.6 hours
            .setNoiseScale(0.003);                // Large impact craters and old features

    /**
     * Pluto - Distant dwarf planet, frozen and exotic
     */
    public static final PlanetConfig PLUTO = new PlanetConfig("Pluto", "pluto".hashCode())
            .setCircumference(7445)               // Real circumference: ~7,445 km
            .setDistanceFromStar(5900)            // Average distance: ~5.9 billion km (very far!)
            .setCrustComposition(CrustComposition.REGOLITHIC)  // Rocky core with icy surface
            .setAtmosphereComposition(AtmosphereComposition.TRACE_ATMOSPHERE)  // Very thin nitrogen atmosphere
            .setTectonicActivity(0.0)             // Geologically inactive
            .setWaterContent(0.8)                 // Mostly water ice (frozen solid)
            .setCrustalThickness(25.0)            // Thin rocky crust under ice
            .setAtmosphericDensity(0.001)         // Extremely thin atmosphere
            .setRotationPeriod(6.4)               // 6.4 Earth days
            .setNoiseScale(0.005);                // Small, varied surface features

    /**
     * Get all hardcoded planets as a map
     * UPDATED: Now only contains our 5 solar system planets
     */
    public static Map<String, PlanetConfig> getAllPlanets() {
        Map<String, PlanetConfig> planets = Map.of(
                "mercury", MERCURY,
                "venus", VENUS,
                "earth", EARTH,
                "mars", MARS,
                "pluto", PLUTO
        );

        // Debug logging
        System.out.println("HardcodedPlanets.getAllPlanets() - Solar System Edition");
        System.out.println("Returning " + planets.size() + " planets: " + planets.keySet());

        // Validate integration with StarSystemModel
        validateStarSystemIntegration(planets);

        return planets;
    }

    /**
     * Validate that all HardcodedPlanets exist in StarSystemModel
     * IMPORTANT: This ensures our planet keys match between systems
     */
    private static void validateStarSystemIntegration(Map<String, PlanetConfig> planets) {
        try {
            StarSystemModel starSystem = StarSystemModel.getInstance();

            System.out.println("=== VALIDATING STAR SYSTEM INTEGRATION ===");

            boolean allValid = true;
            for (String planetKey : planets.keySet()) {
                if (starSystem.hasPlanet(planetKey)) {
                    PlanetaryBody body = starSystem.getPlanet(planetKey);
                    System.out.println("✅ " + planetKey + " -> " + body.getName() + " (orbital period: " +
                            String.format("%.2f", body.getOrbitalPeriodMCDays()) + " MC days)");
                } else {
                    System.err.println("❌ " + planetKey + " NOT FOUND in StarSystemModel!");
                    allValid = false;
                }
            }

            if (allValid) {
                System.out.println("✅ All planets successfully integrated with StarSystemModel");
            } else {
                System.err.println("❌ INTEGRATION ERRORS - check planet keys match between systems");
            }

            System.out.println("Star system status: " + starSystem.getSystemStatus());
            System.out.println("=== VALIDATION COMPLETE ===");

        } catch (Exception e) {
            System.err.println("Error validating star system integration: " + e.getMessage());
        }
    }

    /**
     * Get all planet names for iteration
     */
    public static java.util.Set<String> getAllPlanetNames() {
        return getAllPlanets().keySet();
    }

    /**
     * Get planet config by name (normalized)
     */
    public static PlanetConfig getPlanet(String planetName) {
        String normalized = normalizePlanetName(planetName);
        return getAllPlanets().get(normalized);
    }

    /**
     * Check if a planet name is one of our hardcoded planets
     */
    public static boolean isHardcodedPlanet(String planetName) {
        String normalized = normalizePlanetName(planetName);
        return getAllPlanets().containsKey(normalized);
    }

    /**
     * NEW: Get orbital data for a planet via StarSystemModel integration
     * This connects our planet configs with their orbital mechanics
     */
    public static PlanetaryBody getOrbitalData(String planetName) {
        String normalized = normalizePlanetName(planetName);
        try {
            StarSystemModel starSystem = StarSystemModel.getInstance();
            return starSystem.getPlanet(normalized);
        } catch (Exception e) {
            System.err.println("Error getting orbital data for " + planetName + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * NEW: Get complete planet summary including orbital mechanics
     * Useful for debugging and development
     */
    public static String getPlanetSummary(String planetName) {
        PlanetConfig config = getPlanet(planetName);
        PlanetaryBody orbital = getOrbitalData(planetName);

        if (config == null) {
            return "Planet not found: " + planetName;
        }

        StringBuilder summary = new StringBuilder();
        summary.append("=== ").append(planetName.toUpperCase()).append(" SUMMARY ===\n");
        summary.append("Config: ").append(config.toString()).append("\n");

        if (orbital != null) {
            summary.append("Orbital: ").append(orbital.toString()).append("\n");
            summary.append("Period: ").append(String.format("%.2f MC days", orbital.getOrbitalPeriodMCDays()));
            summary.append(" (").append(String.format("%.1f real days", orbital.getOrbitalPeriodMCDays() * 100)).append(")\n");
            summary.append("Distance: ").append(String.format("%.3f AU", orbital.getSemiMajorAxisAU())).append("\n");
            summary.append("Inclination: ").append(String.format("%.1f°", orbital.getOrbitalInclinationDeg())).append("\n");
        } else {
            summary.append("ERROR: No orbital data found in StarSystemModel\n");
        }

        return summary.toString();
    }

    /**
     * NEW: Generate visibility report from one planet to all others
     * Useful for testing the orbital mechanics
     */
    public static String getVisibilityReport(String observerPlanetName, long worldTimeTicks) {
        String normalized = normalizePlanetName(observerPlanetName);

        try {
            StarSystemModel starSystem = StarSystemModel.getInstance();
            return starSystem.getVisibilityReport(normalized, worldTimeTicks);
        } catch (Exception e) {
            return "Error generating visibility report: " + e.getMessage();
        }
    }

    /**
     * NEW: Quick test method to verify orbital mechanics are working
     * Can be called from mod initialization for debugging
     */
    public static void testOrbitalMechanics() {
        System.out.println("=== TESTING ORBITAL MECHANICS ===");

        try {
            // Test at a few different time points
            long[] testTimes = {0, 24000, 240000, 2400000}; // 0, 1 day, 10 days, 100 days

            for (long time : testTimes) {
                System.out.println("\n--- Time: " + (time / 24000) + " MC days ---");

                // Test Earth's view of other planets
                System.out.println(getVisibilityReport("earth", time));

                // Test Mars's view of other planets
                System.out.println(getVisibilityReport("mars", time));
            }

        } catch (Exception e) {
            System.err.println("Error testing orbital mechanics: " + e.getMessage());
        }

        System.out.println("=== ORBITAL MECHANICS TEST COMPLETE ===");
    }

    /**
     * Normalize planet name for consistent lookup
     */
    private static String normalizePlanetName(String planetName) {
        if (planetName == null) return "";
        return planetName.toLowerCase()
                .replace("-", "")
                .replace("_", "")
                .replace(" ", "")
                .replace(".", "");
    }
}