package net.starlight.terradyne.datagen;

import net.starlight.terradyne.planet.physics.*;

import java.util.Map;

/**
 * Hardcoded planet definitions for data generation
 * These planets will be generated as datapacks and always available
 */
public class HardcodedPlanets {

    /**
     * Earth - Default habitable planet
     */
    public static final PlanetConfig EARTH = new PlanetConfig("Earth", "earth".hashCode())
            .setCircumference(40000)
            .setDistanceFromStar(150)
            .setCrustComposition(CrustComposition.SILICATE)
            .setAtmosphereComposition(AtmosphereComposition.OXYGEN_RICH)
            .setTectonicActivity(0.6)
            .setWaterContent(0.7)
            .setCrustalThickness(35.0)
            .setAtmosphericDensity(1.0)
            .setRotationPeriod(1.0)
            .setNoiseScale(0.002);

    /**
     * Mars - Cold, thin atmosphere planet
     */
    public static final PlanetConfig MARS = new PlanetConfig("Mars", "mars".hashCode())
            .setCircumference(21300)
            .setDistanceFromStar(228)
            .setCrustComposition(CrustComposition.FERROUS)
            .setAtmosphereComposition(AtmosphereComposition.TRACE_ATMOSPHERE)
            .setTectonicActivity(0.1)
            .setWaterContent(0.1)
            .setCrustalThickness(50.0)
            .setAtmosphericDensity(0.01)
            .setRotationPeriod(1.03)
            .setNoiseScale(0.003);

    /**
     * Venus - Hot greenhouse planet
     */
    public static final PlanetConfig VENUS = new PlanetConfig("Venus", "venus".hashCode())
            .setCircumference(38000)
            .setDistanceFromStar(108)
            .setCrustComposition(CrustComposition.BASALTIC)
            .setAtmosphereComposition(AtmosphereComposition.CARBON_DIOXIDE)
            .setTectonicActivity(0.3)
            .setWaterContent(0.0)
            .setCrustalThickness(40.0)
            .setAtmosphericDensity(1.0)
            .setRotationPeriod(243.0)
            .setNoiseScale(0.002);

    /**
     * Kepler-442b - Potentially habitable exoplanet
     */
    public static final PlanetConfig KEPLER_442B = new PlanetConfig("Kepler-442b", "kepler442b".hashCode())
            .setCircumference(45000)
            .setDistanceFromStar(180)
            .setCrustComposition(CrustComposition.SILICATE)
            .setAtmosphereComposition(AtmosphereComposition.NITROGEN_RICH)
            .setTectonicActivity(0.7)
            .setWaterContent(0.8)
            .setCrustalThickness(30.0)
            .setAtmosphericDensity(1.2)
            .setRotationPeriod(0.8)
            .setNoiseScale(0.0018);

    /**
     * Proxima-Centauri-b - Tidally locked exoplanet
     */
    public static final PlanetConfig PROXIMA_CENTAURI_B = new PlanetConfig("Proxima-Centauri-b", "proximacentaurib".hashCode())
            .setCircumference(38500)
            .setDistanceFromStar(7)  // Very close to red dwarf
            .setCrustComposition(CrustComposition.REGOLITHIC)
            .setAtmosphereComposition(AtmosphereComposition.TRACE_ATMOSPHERE)
            .setTectonicActivity(0.2)
            .setWaterContent(0.3)
            .setCrustalThickness(25.0)
            .setAtmosphericDensity(0.1)
            .setRotationPeriod(11.2)  // Tidally locked
            .setNoiseScale(0.0025);

    /**
     * Get all hardcoded planets as a map
     * FIXED: Add debugging to ensure all planets are included
     */
    public static Map<String, PlanetConfig> getAllPlanets() {
        Map<String, PlanetConfig> planets = Map.of(
                "earth", EARTH,
                "mars", MARS,
                "venus", VENUS,
                "kepler442b", KEPLER_442B,
                "proximacentaurib", PROXIMA_CENTAURI_B
        );

        // Debug logging
        System.out.println("HardcodedPlanets.getAllPlanets() called");
        System.out.println("Returning " + planets.size() + " planets: " + planets.keySet());
        for (var entry : planets.entrySet()) {
            System.out.println("  " + entry.getKey() + " -> " + entry.getValue().getPlanetName());
        }

        return planets;
    }

    /**
     * Get all planet names for iteration
     */
    public static java.util.Set<String> getAllPlanetNames() {
        return getAllPlanets().keySet();
    }

    /**
     * Get planet config by name (normalized) - IMPROVED normalization
     */
    public static PlanetConfig getPlanet(String planetName) {
        String normalized = normalizePlanetName(planetName);
        return getAllPlanets().get(normalized);
    }

    /**
     * Check if a planet name is one of our hardcoded planets - IMPROVED normalization
     */
    public static boolean isHardcodedPlanet(String planetName) {
        String normalized = normalizePlanetName(planetName);
        return getAllPlanets().containsKey(normalized);
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