package net.starlight.terradyne.starsystem;

import net.starlight.terradyne.Terradyne;
import net.starlight.terradyne.datagen.HardcodedPlanets;
import net.starlight.terradyne.planet.physics.PlanetConfig;

import java.util.*;

/**
 * Singleton model of our star system containing all planetary bodies
 * Provides orbital mechanics calculations for realistic planet positioning in Celestial skyboxes
 * Uses real astronomical data scaled for Minecraft gameplay
 */
public class StarSystemModel {

    private static StarSystemModel INSTANCE;

    // === CONFIGURATION ===
    private static final double TIME_SCALE_FACTOR = 1000000.0;      // 1/100 real time (100 real days = 1 MC day)
    private static final double SIZE_SCALE_FACTOR = 2000.0;      // 100x larger apparent size for visibility
    private static final double BRIGHTNESS_THRESHOLD = 0.001;   // Minimum brightness to be visible
    private static final double MIN_APPARENT_SIZE = 0.1;        // Minimum apparent size in degrees

    // === PLANETARY BODIES ===
    private final Map<String, PlanetaryBody> planets;
    private final List<String> planetOrder; // For consistent iteration

    /**
     * Private constructor - use getInstance()
     */
    private StarSystemModel() {
        this.planets = new LinkedHashMap<>();
        this.planetOrder = new ArrayList<>();
        initializePlanets();

        Terradyne.LOGGER.info("=== STAR SYSTEM MODEL INITIALIZED ===");
        Terradyne.LOGGER.info("Time scale: 1/{} (real days per MC day)", TIME_SCALE_FACTOR);
        Terradyne.LOGGER.info("Size scale: {}x apparent size", SIZE_SCALE_FACTOR);
        Terradyne.LOGGER.info("Planets: {}", planetOrder);
        logOrbitalSummary();
    }

    /**
     * Get singleton instance
     */
    public static StarSystemModel getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new StarSystemModel();
        }
        return INSTANCE;
    }

    /**
     * Initialize all planetary bodies with real astronomical data
     */
    private void initializePlanets() {
        // Using real astronomical data with varied orbital orientations for realism

        // MERCURY - Closest, fastest orbit
        addPlanet(new PlanetaryBody(
                "Mercury", "mercury",
                0.387, 88,                    // 0.387 AU, 88 days
                7.0, 48.3, 29.1, 174.8,      // 7° inclination, orbital orientation
                2440, 0.055, 0.14             // 2440 km radius, 0.055 Earth masses, 0.14 albedo
        ));

        // VENUS - Hot, backwards rotation (we're not modeling rotation yet)
        addPlanet(new PlanetaryBody(
                "Venus", "venus",
                0.723, 225,                   // 0.723 AU, 225 days
                3.4, 76.7, 54.9, 50.1,       // 3.4° inclination, orbital orientation
                6052, 0.815, 0.90             // 6052 km radius, 0.815 Earth masses, 0.90 albedo (very bright!)
        ));

        // EARTH - Our reference planet
        addPlanet(new PlanetaryBody(
                "Earth", "earth",
                1.0, 365,                     // 1.0 AU, 365 days (reference)
                0.0, 0.0, 102.9, 100.5,      // 0° inclination (reference plane), orbital orientation
                6371, 1.0, 0.37               // 6371 km radius, 1.0 Earth masses, 0.37 albedo
        ));

        // MARS - The red planet
        addPlanet(new PlanetaryBody(
                "Mars", "mars",
                1.524, 687,                   // 1.524 AU, 687 days
                1.9, 49.6, 286.5, 19.4,      // 1.9° inclination, orbital orientation
                3390, 0.107, 0.25             // 3390 km radius, 0.107 Earth masses, 0.25 albedo
        ));

        // PLUTO - Far out dwarf planet (keeping it for the cool factor)
        addPlanet(new PlanetaryBody(
                "Pluto", "pluto",
                39.5, 90560,                  // 39.5 AU, 90560 days (248 years!)
                17.2, 110.3, 113.8, 14.5,    // 17.2° inclination (highly inclined), orbital orientation
                1188, 0.00218, 0.49           // 1188 km radius, 0.00218 Earth masses, 0.49 albedo
        ));
    }

    /**
     * Add a planet to the system
     */
    private void addPlanet(PlanetaryBody planet) {
        planets.put(planet.getPlanetKey(), planet);
        planetOrder.add(planet.getPlanetKey());
    }

    // === ORBITAL MECHANICS ===

    /**
     * Get all visible planets from a given observer planet
     * @param observerPlanetKey The planet the observer is on
     * @param worldTimeTicks Current world time in ticks
     * @return List of visible planets with visibility data
     */
    public List<VisiblePlanet> getVisiblePlanets(String observerPlanetKey, long worldTimeTicks) {
        PlanetaryBody observer = planets.get(observerPlanetKey);
        if (observer == null) {
            Terradyne.LOGGER.warn("Observer planet not found: {}", observerPlanetKey);
            return Collections.emptyList();
        }

        List<VisiblePlanet> visiblePlanets = new ArrayList<>();

        for (PlanetaryBody planet : planets.values()) {
            // Skip the observer planet itself
            if (planet.getPlanetKey().equals(observerPlanetKey)) {
                continue;
            }

            // Calculate visibility
            double apparentSize = planet.calculateApparentSize(observer, worldTimeTicks, SIZE_SCALE_FACTOR);
            double brightness = planet.calculateApparentBrightness(observer, worldTimeTicks);
            double distance = planet.calculateDistanceTo(observer, worldTimeTicks);

            // Check if planet is visible
            if (apparentSize >= MIN_APPARENT_SIZE && brightness >= BRIGHTNESS_THRESHOLD) {
                double[] observerPos = observer.calculateOrbitalPosition(worldTimeTicks);
                double[] planetPos = planet.calculateOrbitalPosition(worldTimeTicks);

                visiblePlanets.add(new VisiblePlanet(
                        planet, apparentSize, brightness, distance,
                        observerPos, planetPos
                ));
            }
        }

        // Sort by brightness (brightest first)
        visiblePlanets.sort((a, b) -> Double.compare(b.brightness, a.brightness));

        return visiblePlanets;
    }

    /**
     * Get a specific planet by key
     */
    public PlanetaryBody getPlanet(String planetKey) {
        return planets.get(planetKey);
    }

    /**
     * Get all planets
     */
    public Collection<PlanetaryBody> getAllPlanets() {
        return planets.values();
    }

    /**
     * Get planet keys in order
     */
    public List<String> getPlanetOrder() {
        return new ArrayList<>(planetOrder);
    }


    /**
     * Get the number of planets in the system
     */
    public int getPlanetCount() {
        return planets.size();
    }

    // === CELESTIAL INTEGRATION ===

    /**
     * Generate Celestial expressions with planetary rotation
     * UPDATED: Now includes sky rotation based on observer planet's rotation period
     */
    public CelestialExpressions generateCelestialExpressions(String targetPlanetKey, String observerPlanetKey) {
        PlanetaryBody target = planets.get(targetPlanetKey);
        PlanetaryBody observer = planets.get(observerPlanetKey);

        if (target == null || observer == null) {
            return null;
        }

        // Get sky rotation component
        String skyRotation = generateSkyRotationExpression(observerPlanetKey);

        // STEP 1: Geocentric orbital motion
        String baseRotationX = generateGeocentricRotation(target, observer);

        // STEP 2: Add sky rotation to orbital motion
        String rotationX = String.format("(%s) + (%s)", baseRotationX, skyRotation);

        // STEP 3: Position offset to sun's location
        String[] positionOffset = calculateHeliocentricOffset(target, observer, skyRotation);

        // STEP 4: Other properties
        String scale = generateDynamicScale(target, observer);
        String distance = generateDynamicDistance(target, observer);
        String alpha = generateDistanceBasedAlpha(target, observer);

        return new CelestialExpressions(target, rotationX, "0", "0", scale, distance, alpha,
                positionOffset[0], positionOffset[1], "0");
    }

    /**
     * Generate complete Celestial skybox data for a planet using offset-based approach
     * This is the final output for CelestialSkyDataProvider integration
     */
    public Map<String, CelestialExpressions> generateCompleteSkybox(String observerPlanetKey) {
        Map<String, CelestialExpressions> skybox = new HashMap<>();

        for (PlanetaryBody planet : planets.values()) {
            if (!planet.getPlanetKey().equals(observerPlanetKey)) {
                CelestialExpressions expressions = generateCelestialExpressions(planet.getPlanetKey(), observerPlanetKey);
                if (expressions != null) {
                    skybox.put("planet_" + planet.getPlanetKey(), expressions);
                }
            }
        }

        return skybox;
    }

    /**
     * CORRECTED: Generate angular offset FROM the sun's position
     * This is the angle between sun→observer and sun→target as seen by observer
     */
    private String generateGeocentricRotation(PlanetaryBody target, PlanetaryBody observer) {
        double targetSpeed = target.getOrbitalSpeedCoefficient();
        double observerSpeed = observer.getOrbitalSpeedCoefficient();
        double targetPhase = target.getMeanAnomalyAtEpoch();
        double observerPhase = observer.getMeanAnomalyAtEpoch();

        // Angular difference in orbital positions (as seen from sun)
        String orbitalAngleDifference = String.format("(worldTime * %.8f + %.2f) - (worldTime * %.8f + %.2f)",
                targetSpeed, targetPhase, observerSpeed, observerPhase);

        // Convert to angular offset as seen by observer
        double targetDistance = target.getSemiMajorAxisAU();
        double observerDistance = observer.getSemiMajorAxisAU();

        if (targetDistance < observerDistance) {
            // Inner planet - limited angular separation from sun
            double maxSeparation = Math.toDegrees(Math.asin(targetDistance / observerDistance));
            return String.format("%.2f * sin(radians(%s))", maxSeparation, orbitalAngleDifference);
        } else {
            // Outer planet - larger angular separations possible
            double distanceRatio = observerDistance / targetDistance;
            return String.format("%.3f * (%s)", distanceRatio, orbitalAngleDifference);
        }
    }

    /**
     * Calculate position offset with sky rotation
     * UPDATED: Sun's position now includes observer planet's rotation
     */
    private String[] calculateHeliocentricOffset(PlanetaryBody target, PlanetaryBody observer, String skyRotation) {
        double observerSpeed = observer.getOrbitalSpeedCoefficient();
        double observerPhase = observer.getMeanAnomalyAtEpoch();
        double observerDistance = observer.getSemiMajorAxisAU();

        // Observer's orbital position
        String observerOrbitalAngle = String.format("worldTime * %.8f + %.2f", observerSpeed, observerPhase);

        // Sun's position includes both orbital motion AND sky rotation
        String sunAngleInSky = String.format("(%s) + 180 + (%s)", observerOrbitalAngle, skyRotation);

        // Convert to screen coordinates
        String offsetX = String.format("%.3f * cos(radians(%s))", 0.1, sunAngleInSky);
        String offsetY = String.format("%.3f * sin(radians(%s))", 0.1, sunAngleInSky);

        return new String[]{offsetX, offsetY};
    }

    /**
     * Calculate sky rotation speed based on observer planet's rotation period
     * @param observerPlanetKey The planet whose rotation determines day/night
     * @return Rotation speed in degrees per tick
     */
    public double calculateSkyRotationSpeed(String observerPlanetKey) {
        // Get the planet config to find rotation period
        PlanetConfig config = null;
        try {
            config = HardcodedPlanets.getPlanet(observerPlanetKey);
        } catch (Exception e) {
            // Fallback to Earth-like rotation
            return 360.0 / 24000.0; // 1 MC day = 24000 ticks
        }

        if (config == null) {
            return 360.0 / 24000.0; // Earth fallback
        }

        double rotationPeriodMCDays = config.getRotationPeriod();
        double ticksPerRotation = rotationPeriodMCDays * 24000.0;

        return 360.0 / ticksPerRotation; // degrees per tick
    }

    /**
     * Generate sky rotation expression for observer planet
     * This makes the entire sky rotate due to planetary rotation
     */
    public String generateSkyRotationExpression(String observerPlanetKey) {
        double rotationSpeed = calculateSkyRotationSpeed(observerPlanetKey);
        return String.format("worldTime * %.8f", rotationSpeed);
    }

    /**
     * Generate dynamic scale expression that changes with orbital distance
     */
    private String generateDynamicScale(PlanetaryBody target, PlanetaryBody observer) {
        double baseScale = (target.getRadiusKm() / 6371.0) * SIZE_SCALE_FACTOR * 0.002;

        double targetSpeed = target.getOrbitalSpeedCoefficient();
        double observerSpeed = observer.getOrbitalSpeedCoefficient();
        double targetPhase = target.getMeanAnomalyAtEpoch();
        double observerPhase = observer.getMeanAnomalyAtEpoch();
        double targetDistance = target.getSemiMajorAxisAU();
        double observerDistance = observer.getSemiMajorAxisAU();

        // Calculate dynamic distance between planets
        String distanceFormula = String.format(
                "sqrt((" +
                        "%.3f * cos(radians(worldTime * %.8f + %.2f)) - %.3f * cos(radians(worldTime * %.8f + %.2f))" +
                        ")^2 + (" +
                        "%.3f * sin(radians(worldTime * %.8f + %.2f)) - %.3f * sin(radians(worldTime * %.8f + %.2f))" +
                        ")^2)",
                targetDistance, targetSpeed, targetPhase, observerDistance, observerSpeed, observerPhase,
                targetDistance, targetSpeed, targetPhase, observerDistance, observerSpeed, observerPhase
        );

        // Scale inversely with distance
        return String.format("%.4f / max(0.1, %s)", baseScale * 2.0, distanceFormula);
    }

    /**
     * CORRECTED: Generate distance as target planet's distance from sun
     * This represents how far the planet appears from the sun in the sky
     */
    private String generateDynamicDistance(PlanetaryBody target, PlanetaryBody observer) {
        // Distance should be target's orbital radius (distance from sun)
        double sunToTargetDistance = target.getSemiMajorAxisAU();

        // Scale appropriately for Celestial display (keeping planets reasonably visible)
        double scaledDistance = Math.max(50.0, sunToTargetDistance * 100.0);

        return String.format("%.1f", scaledDistance);
    }

    /**
     * Generate distance and brightness-based alpha expression
     */
    private String generateDistanceBasedAlpha(PlanetaryBody target, PlanetaryBody observer) {
        double brightness = Math.min(1.0, target.getAlbedo() * 2.0);

        // Basic visibility with day/night effects
        return String.format("%.2f * max(0.2, (1 - dayLight * 0.7)) * (1 - rainAlpha)", brightness);
    }

    // === UTILITY & DEBUGGING ===

    /**
     * Check if a planet exists in the system
     */
    public boolean hasPlanet(String planetKey) {
        return planets.containsKey(planetKey);
    }

    /**
     * Get detailed visibility report for debugging
     */
    public String getVisibilityReport(String observerPlanetKey, long worldTimeTicks) {
        List<VisiblePlanet> visible = getVisiblePlanets(observerPlanetKey, worldTimeTicks);

        StringBuilder report = new StringBuilder();
        report.append(String.format("=== VISIBILITY FROM %s (time: %d) ===\n", observerPlanetKey.toUpperCase(), worldTimeTicks));

        if (visible.isEmpty()) {
            report.append("No planets currently visible\n");
        } else {
            for (VisiblePlanet vp : visible) {
                report.append(String.format("%s: size=%.3f°, brightness=%.6f, distance=%.2f AU\n",
                        vp.planet.getName(), vp.apparentSize, vp.brightness, vp.distance));
            }
        }

        return report.toString();
    }

    /**
     * Log orbital summary for all planets
     */
    private void logOrbitalSummary() {
        Terradyne.LOGGER.info("=== ORBITAL SUMMARY ===");
        for (PlanetaryBody planet : planets.values()) {
            Terradyne.LOGGER.info(planet.getOrbitalSummary());
        }
        Terradyne.LOGGER.info("========================");
    }

    /**
     * Get system status for debugging
     */
    public String getSystemStatus() {
        return String.format("StarSystemModel{%d planets, time_scale=1/%.0f, size_scale=%.0fx}",
                planets.size(), TIME_SCALE_FACTOR, SIZE_SCALE_FACTOR);
    }

    // === CONFIGURATION ACCESSORS ===

    public static double getTimeScaleFactor() { return TIME_SCALE_FACTOR; }
    public static double getSizeScaleFactor() { return SIZE_SCALE_FACTOR; }
    public static double getBrightnessThreshold() { return BRIGHTNESS_THRESHOLD; }
    public static double getMinApparentSize() { return MIN_APPARENT_SIZE; }

    // === INNER CLASSES ===

    /**
     * Represents a planet that is visible from an observer planet
     */
    public static class VisiblePlanet {
        public final PlanetaryBody planet;
        public final double apparentSize;    // Angular size in degrees
        public final double brightness;      // Relative brightness
        public final double distance;        // Distance in AU
        public final double[] observerPos;   // Observer 3D position
        public final double[] planetPos;     // Planet 3D position

        public VisiblePlanet(PlanetaryBody planet, double apparentSize, double brightness,
                             double distance, double[] observerPos, double[] planetPos) {
            this.planet = planet;
            this.apparentSize = apparentSize;
            this.brightness = brightness;
            this.distance = distance;
            this.observerPos = observerPos.clone();
            this.planetPos = planetPos.clone();
        }
    }

    /**
     * Container for all Celestial expressions for a planet including position offsets
     * UPDATED: Now includes pos_x, pos_y, pos_z for heliocentric positioning
     */
    public static class CelestialExpressions {
        public final PlanetaryBody planet;
        public final String rotationX;     // Geocentric rotation expression
        public final String rotationY;     // Always "0" for now
        public final String rotationZ;     // Always "0" for now (no inclinations)
        public final String scale;         // Dynamic scale expression
        public final String distance;      // Distance expression
        public final String alpha;         // Visibility expression
        public final String posX;          // Position offset X (heliocentric correction)
        public final String posY;          // Position offset Y (heliocentric correction)
        public final String posZ;          // Position offset Z (always "0" for now)

        public CelestialExpressions(PlanetaryBody planet, String rotationX, String rotationY, String rotationZ,
                                    String scale, String distance, String alpha, String posX, String posY, String posZ) {
            this.planet = planet;
            this.rotationX = rotationX;
            this.rotationY = rotationY;
            this.rotationZ = rotationZ;
            this.scale = scale;
            this.distance = distance;
            this.alpha = alpha;
            this.posX = posX;
            this.posY = posY;
            this.posZ = posZ;
        }

        @Override
        public String toString() {
            return String.format("CelestialExpressions for %s:\n" +
                            "  rotationX: %s\n" +
                            "  rotationZ: %s\n" +
                            "  scale: %s\n" +
                            "  distance: %s\n" +
                            "  alpha: %s\n" +
                            "  posX: %s\n" +
                            "  posY: %s",
                    planet.getName(), rotationX, rotationZ, scale, distance, alpha, posX, posY);
        }
    }
}