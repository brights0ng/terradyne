package net.starlight.terradyne.starsystem;

/**
 * Represents a planetary body in the star system with orbital and physical properties
 * Uses real astronomical data scaled for Minecraft gameplay
 */
public class PlanetaryBody {

    // === IDENTIFICATION ===
    private final String name;
    private final String planetKey;  // For dimension/config lookup

    // === ORBITAL PARAMETERS (Real astronomical data with 1/100 time scaling) ===
    private final double semiMajorAxisAU;        // Distance from sun in Astronomical Units
    private final double orbitalPeriodMCDays;    // Orbital period in Minecraft days (1/100 real scale)
    private final double orbitalInclinationDeg;  // Inclination relative to ecliptic plane (degrees)
    private final double longitudeOfAscendingNode; // Orbital orientation parameter (degrees)
    private final double argumentOfPeriapsis;     // Orbital orientation parameter (degrees)
    private final double meanAnomalyAtEpoch;      // Starting position in orbit (degrees)

    // === PHYSICAL PROPERTIES ===
    private final double radiusKm;               // Physical radius in kilometers
    private final double massEarths;             // Mass relative to Earth
    private final double albedo;                 // Reflectivity (0.0 to 1.0)

    // === DERIVED ORBITAL PROPERTIES ===
    private final double angularVelocityDegPerTick; // How fast it moves in degrees per MC tick
    private final double orbitalSpeedCoefficient;   // For Celestial expressions: worldTime * coefficient

    /**
     * Create a planetary body with real astronomical data
     */
    public PlanetaryBody(String name, String planetKey,
                         double semiMajorAxisAU, double orbitalPeriodRealDays, double orbitalInclinationDeg,
                         double longitudeOfAscendingNode, double argumentOfPeriapsis, double meanAnomalyAtEpoch,
                         double radiusKm, double massEarths, double albedo) {

        this.name = name;
        this.planetKey = planetKey;
        this.semiMajorAxisAU = semiMajorAxisAU;
        this.orbitalInclinationDeg = orbitalInclinationDeg;
        this.longitudeOfAscendingNode = longitudeOfAscendingNode;
        this.argumentOfPeriapsis = argumentOfPeriapsis;
        this.meanAnomalyAtEpoch = meanAnomalyAtEpoch;
        this.radiusKm = radiusKm;
        this.massEarths = massEarths;
        this.albedo = albedo;

        // Apply 1/100 time scaling
        this.orbitalPeriodMCDays = orbitalPeriodRealDays / StarSystemModel.getTimeScaleFactor();

        // Calculate derived orbital properties
        // One MC day = 24000 ticks, full orbit = 360 degrees
        double ticksPerOrbit = this.orbitalPeriodMCDays * 24000.0;
        this.angularVelocityDegPerTick = 360.0 / ticksPerOrbit;

        // For Celestial expressions: coefficient such that worldTime * coefficient gives current angle
        this.orbitalSpeedCoefficient = this.angularVelocityDegPerTick;
    }

    // === ORBITAL POSITION CALCULATIONS ===

    /**
     * Calculate the mean anomaly (position in orbit) at a given world time
     * @param worldTimeTicks Current Minecraft world time in ticks
     * @return Mean anomaly in degrees (0-360)
     */
    public double calculateMeanAnomaly(long worldTimeTicks) {
        double timeSinceEpoch = worldTimeTicks * angularVelocityDegPerTick;
        return (meanAnomalyAtEpoch + timeSinceEpoch) % 360.0;
    }

    /**
     * Calculate orbital position in 3D space relative to the sun
     * Simplified to circular orbits for now (no eccentricity)
     * @param worldTimeTicks Current Minecraft world time in ticks
     * @return 3D position as [x, y, z] where sun is at origin, distances in AU
     */
    public double[] calculateOrbitalPosition(long worldTimeTicks) {
        double meanAnomaly = Math.toRadians(calculateMeanAnomaly(worldTimeTicks));
        double inclination = Math.toRadians(orbitalInclinationDeg);
        double longitudeAscending = Math.toRadians(longitudeOfAscendingNode);
        double argumentPeri = Math.toRadians(argumentOfPeriapsis);

        // Position in orbital plane (simplified circular orbit)
        double orbitalX = semiMajorAxisAU * Math.cos(meanAnomaly);
        double orbitalY = semiMajorAxisAU * Math.sin(meanAnomaly);
        double orbitalZ = 0.0;

        // Apply orbital inclination and orientation
        // This is simplified orbital mechanics - for now just apply inclination
        double x = orbitalX * Math.cos(longitudeAscending) - orbitalY * Math.sin(longitudeAscending);
        double y = orbitalX * Math.sin(longitudeAscending) + orbitalY * Math.cos(longitudeAscending);
        double z = orbitalZ + orbitalY * Math.sin(inclination);

        return new double[]{x, y, z};
    }

    /**
     * Calculate distance to another planetary body at a given time
     * @param other The other planetary body
     * @param worldTimeTicks Current world time in ticks
     * @return Distance in AU
     */
    public double calculateDistanceTo(PlanetaryBody other, long worldTimeTicks) {
        double[] thisPos = calculateOrbitalPosition(worldTimeTicks);
        double[] otherPos = other.calculateOrbitalPosition(worldTimeTicks);

        double dx = thisPos[0] - otherPos[0];
        double dy = thisPos[1] - otherPos[1];
        double dz = thisPos[2] - otherPos[2];

        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Calculate apparent angular size as seen from another planet
     * @param observerPlanet The planet observing this one
     * @param worldTimeTicks Current world time in ticks
     * @param sizingFactor Gameplay scaling factor (e.g., 100x for visibility)
     * @return Apparent angular diameter in degrees
     */
    public double calculateApparentSize(PlanetaryBody observerPlanet, long worldTimeTicks, double sizingFactor) {
        double distanceAU = calculateDistanceTo(observerPlanet, worldTimeTicks);
        double distanceKm = distanceAU * 149597870.7; // AU to km conversion

        // Angular diameter = 2 * arctan(radius / distance)
        double angularDiameterRadians = 2.0 * Math.atan(radiusKm / distanceKm);
        double angularDiameterDegrees = Math.toDegrees(angularDiameterRadians);

        // Apply gameplay sizing factor
        return angularDiameterDegrees * sizingFactor;
    }

    /**
     * Calculate brightness/visibility as seen from another planet
     * Based on distance, size, and albedo
     * @param observerPlanet The planet observing this one
     * @param worldTimeTicks Current world time in ticks
     * @return Relative brightness (0.0 to 1.0+)
     */
    public double calculateApparentBrightness(PlanetaryBody observerPlanet, long worldTimeTicks) {
        double distanceAU = calculateDistanceTo(observerPlanet, worldTimeTicks);

        // Brightness proportional to albedo * size^2 / distance^2
        double surfaceArea = 4.0 * Math.PI * radiusKm * radiusKm;
        double brightness = (albedo * surfaceArea) / (distanceAU * distanceAU);

        // Normalize to Earth-from-Earth as 1.0 (this will need tweaking)
        return brightness / 1000000.0; // Rough normalization
    }

    // === CELESTIAL EXPRESSION GENERATION ===

    /**
     * Generate Celestial-compatible expression for geocentric rotation (relative orbital motion)
     * UPDATED: Now generates simple relative orbital motion for offset-based positioning
     * @return String expression using worldTime variable
     */
    public String generateGeocentricRotationExpression() {
        return String.format("worldTime * %.8f + %.2f",
                orbitalSpeedCoefficient, meanAnomalyAtEpoch);
    }

    // === ACCESSORS ===

    public String getName() { return name; }
    public String getPlanetKey() { return planetKey; }
    public double getSemiMajorAxisAU() { return semiMajorAxisAU; }
    public double getOrbitalPeriodMCDays() { return orbitalPeriodMCDays; }
    public double getOrbitalInclinationDeg() { return orbitalInclinationDeg; }
    public double getLongitudeOfAscendingNode() { return longitudeOfAscendingNode; }
    public double getArgumentOfPeriapsis() { return argumentOfPeriapsis; }
    public double getMeanAnomalyAtEpoch() { return meanAnomalyAtEpoch; }
    public double getRadiusKm() { return radiusKm; }
    public double getMassEarths() { return massEarths; }
    public double getAlbedo() { return albedo; }
    public double getAngularVelocityDegPerTick() { return angularVelocityDegPerTick; }
    public double getOrbitalSpeedCoefficient() { return orbitalSpeedCoefficient; }

    // === UTILITY ===

    @Override
    public String toString() {
        return String.format("PlanetaryBody{%s: %.2f AU, %.2f MCdays, %.1f° incl, %.0f km radius}",
                name, semiMajorAxisAU, orbitalPeriodMCDays, orbitalInclinationDeg, radiusKm);
    }

    /**
     * Get a detailed orbital summary for debugging
     */
    public String getOrbitalSummary() {
        return String.format("%s Orbital Data:\n" +
                        "  Distance: %.3f AU\n" +
                        "  Period: %.2f MC days (%.1f real days)\n" +
                        "  Inclination: %.1f°\n" +
                        "  Angular velocity: %.6f deg/tick\n" +
                        "  Celestial coefficient: %.8f\n" +
                        "  Physical: %.0f km radius, %.2f Earth masses, %.2f albedo",
                name, semiMajorAxisAU, orbitalPeriodMCDays, orbitalPeriodMCDays * 100,
                orbitalInclinationDeg, angularVelocityDegPerTick, orbitalSpeedCoefficient,
                radiusKm, massEarths, albedo);
    }
}