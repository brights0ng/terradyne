package net.starlight.terradyne.starsystem;

import net.minecraft.util.Identifier;
import net.starlight.terradyne.planet.physics.PlanetConfig;
import net.starlight.terradyne.planet.physics.PlanetModel;

import java.util.HashMap;
import java.util.Map;

/**
 * Complete star system model with physics calculations
 * Built from StarSystemConfig and loaded celestial objects
 */
public class StarSystemModel {
    
    // === SCALE FACTORS FOR CELESTIAL RENDERING ===
    /**
     * TIME_SCALE_FACTOR controls how fast in-game time maps to orbital motion.
     * 1.0 = Perfect realism (24 real hours per MC day)
     * 72.0 = Minecraft default (20 real minutes per MC day)
     */
    public static final double TIME_SCALE_FACTOR = 72.0;
    
    /**
     * SIZE_SCALE_FACTOR controls visual size of celestial objects in the sky.
     * 1.0 = Perfect realism (barely visible)
     * 100.0 = 100x scale for visibility
     */
    public static final double SIZE_SCALE_FACTOR = 1000.0;
    
    // Physics constants
    private static final double AU_TO_KM = 149597870.7; // 1 AU in kilometers
    private static final double TICKS_PER_DAY = 24000.0; // Minecraft day length
    private static final double SECONDS_PER_TICK = 0.05; // Minecraft tick = 50ms

    private final Identifier identifier;
    private final String name;
    private final StarSystemConfig.HierarchyNode rootStar;
    private final Map<Identifier, CelestialObject> objects;

    public StarSystemModel(Identifier identifier, String name,
                          StarSystemConfig.HierarchyNode rootStar,
                          Map<Identifier, CelestialObject> objects) {
        this.identifier = identifier;
        this.name = name;
        this.rootStar = rootStar;
        this.objects = objects;
    }

    public Identifier getIdentifier() {
        return identifier;
    }

    public String getName() {
        return name;
    }

    public StarSystemConfig.HierarchyNode getRootStar() {
        return rootStar;
    }

    public Map<Identifier, CelestialObject> getObjects() {
        return objects;
    }

    public CelestialObject getObject(Identifier objectId) {
        return objects.get(objectId);
    }

    /**
     * Get the root star object
     */
    public CelestialObject getStar() {
        return objects.get(rootStar.objectId);
    }
    
    // === CELESTIAL SKY EQUATION GENERATION ===
    
    /**
     * Generates the rotation equation for the entire skybox from an observer's perspective.
     * This rotates all celestial objects together to simulate the observer planet's rotation.
     * 
     * @param observerId The observer planet's identifier
     * @return Equation string for degrees rotation (e.g., "(worldTime / 1000) * 360")
     */
    public String generateSkyRotationEquation(Identifier observerId) {
        CelestialObject observer = objects.get(observerId);
        if (observer == null || observer.planetConfig == null) {
            return "0"; // No rotation if observer not found
        }
        
        double rotationPeriod = observer.planetConfig.getRotationPeriod(); // In MC days
        if (rotationPeriod <= 0) {
            return "0"; // No rotation if period is invalid
        }
        
        // Calculate ticks per full rotation, accounting for TIME_SCALE_FACTOR
        double ticksPerRotation = rotationPeriod * TICKS_PER_DAY;
        
        // Generate equation: (worldTime / ticksPerRotation) * 360
        // This gives us degrees of rotation based on current world time
        return String.format("(worldTime / %.2f) * 360", ticksPerRotation);
    }

    /**
     * Generates position equations for a target object as seen from an observer.
     * Returns three equation strings for X, Y, Z coordinates in 3D space.
     *
     * @param observerId The observer planet's identifier
     * @param targetId The target object's identifier
     * @return Array of equation strings [posX, posY, posZ]
     */
    public String[] generatePositionEquations(Identifier observerId, Identifier targetId) {
        CelestialObject observer = objects.get(observerId);
        CelestialObject target = objects.get(targetId);

        if (observer == null || target == null) {
            return new String[]{"0", "0", "0"};
        }

        // Get orbital data for both objects
        OrbitalData observerOrbit = observer.orbitalData;
        OrbitalData targetOrbit = target.orbitalData;

        if (observerOrbit == null || targetOrbit == null) {
            return new String[]{"0", "0", "0"};
        }

        // Validate orbital periods
        if (observerOrbit.orbitalPeriod <= 0 || targetOrbit.orbitalPeriod <= 0) {
            System.err.println("WARNING: Invalid orbital period for " + targetId + ", using static position");
            return new String[]{"0", "0", "0"};
        }

        // Convert distances to AU
        double observerDistanceAU = observerOrbit.distanceFromStar / AU_TO_KM;
        double targetDistanceAU = targetOrbit.distanceFromStar / AU_TO_KM;

        // Calculate relative distance
        double relativeDistance = Math.abs(targetDistanceAU - observerDistanceAU);

        // Handle case where objects are at same orbital distance
        if (relativeDistance < 0.001) {
            System.err.println("WARNING: " + targetId + " at same distance as " + observerId + ", using small offset");
            relativeDistance = 0.1; // Small offset to prevent divide by zero
        }

        // Calculate mean motion (angular velocity) for both objects
        double observerPeriod = observerOrbit.orbitalPeriod; // days
        double targetPeriod = targetOrbit.orbitalPeriod; // days

        // Angular velocity in degrees per tick
        double observerAngularVelocity = (360.0 / observerPeriod) / TICKS_PER_DAY;
        double targetAngularVelocity = (360.0 / targetPeriod) / TICKS_PER_DAY;

        // Relative angular velocity (how fast target moves relative to observer)
        double relativeAngularVelocity = targetAngularVelocity - observerAngularVelocity;

        // Apply TIME_SCALE_FACTOR
        relativeAngularVelocity /= TIME_SCALE_FACTOR;

        // Validate result
        if (Double.isNaN(relativeAngularVelocity) || Double.isInfinite(relativeAngularVelocity)) {
            System.err.println("ERROR: Invalid angular velocity for " + targetId + ", using 0");
            return new String[]{"0", "0", "0"};
        }

        // Apply SIZE_SCALE_FACTOR for visibility
        double scaledDistance = relativeDistance * SIZE_SCALE_FACTOR;

        // Validate scaled distance
        if (Double.isNaN(scaledDistance) || Double.isInfinite(scaledDistance)) {
            System.err.println("ERROR: Invalid scaled distance for " + targetId + ", using 0");
            return new String[]{"0", "0", "0"};
        }

        // Generate position equations using circular orbit formula
        // X = distance * cos(angle), Y = 0 (coplanar), Z = distance * sin(angle)
        // Angle = angular_velocity * time

        String angleEquation = String.format("worldTime * %.8f", relativeAngularVelocity);

        String posX = String.format("%.4f * cos(%s)", scaledDistance, angleEquation);
        String posY = "0"; // Assume coplanar orbits for now
        String posZ = String.format("%.4f * sin(%s)", scaledDistance, angleEquation);

        return new String[]{posX, posY, posZ};
    }
    
    /**
     * Generates a scale equation for a target object's visual size.
     * Scale depends on the object's physical radius and distance.
     * 
     * @param targetId The target object's identifier
     * @return Equation string for scale value
     */
    public String generateScaleEquation(Identifier targetId) {
        CelestialObject target = objects.get(targetId);
        
        if (target == null || target.planetConfig == null) {
            return "20"; // Default scale
        }
        
        // Get physical radius from planet model
        PlanetModel model = target.getPlanetModel();
        double radiusKm = model.getConfig().getCircumference() / (2000 * Math.PI); // Convert meters to km
        
        // Calculate angular size (simplified)
        // For a sphere at distance d with radius r: angular_size ≈ 2 * r / d
        double distanceKm = target.orbitalData.distanceFromStar;
        double angularSize = (2.0 * radiusKm) / distanceKm;
        
        // Convert to visual scale with SIZE_SCALE_FACTOR
        double visualScale = angularSize * SIZE_SCALE_FACTOR * 1000.0;
        
        // Clamp to reasonable range
        visualScale = Math.max(0.5, Math.min(100.0, visualScale));
        
        return String.format("%.2f", visualScale);
    }
    
    /**
     * Generates an alpha (transparency) equation for a target object.
     * Alpha can depend on distance, daylight, etc.
     * 
     * @param targetId The target object's identifier
     * @return Equation string for alpha value (0-1)
     */
    public String generateAlphaEquation(Identifier targetId) {
        CelestialObject target = objects.get(targetId);
        
        if (target == null) {
            return "1"; // Fully opaque by default
        }
        
        // Stars are always visible, planets fade during day
        if (target.type == ObjectType.SOLAR) {
            return "1 - rainAlpha"; // Star always visible except in rain
        } else {
            // Planets visible mostly at night
            return "max(0.5, (1 - dayLight * 0.5)) * (1 - rainAlpha)";
        }
    }
    
    /**
     * Gets all objects visible from an observer's perspective.
     * Excludes the observer itself.
     * 
     * @param observerId The observer's identifier
     * @return List of all visible celestial objects
     */
    public java.util.List<CelestialObject> getVisibleObjects(Identifier observerId) {
        java.util.List<CelestialObject> visible = new java.util.ArrayList<>();
        for (CelestialObject obj : objects.values()) {
            if (!obj.identifier.equals(observerId)) {
                visible.add(obj);
            }
        }
        return visible;
    }

    /**
     * Represents a celestial object in a star system
     */
    public static class CelestialObject {
        private final Identifier identifier;
        private final String name;
        private final ObjectType type;
        private final PlanetConfig planetConfig;
        private final OrbitalData orbitalData;
        private PlanetModel planetModel; // Lazy-loaded

        public CelestialObject(Identifier identifier, String name, ObjectType type,
                              PlanetConfig planetConfig, OrbitalData orbitalData) {
            this.identifier = identifier;
            this.name = name;
            this.type = type;
            this.planetConfig = planetConfig;
            this.orbitalData = orbitalData;
        }

        public Identifier getIdentifier() {
            return identifier;
        }

        public String getName() {
            return name;
        }

        public ObjectType getType() {
            return type;
        }

        public PlanetConfig getPlanetConfig() {
            return planetConfig;
        }

        public OrbitalData getOrbitalData() {
            return orbitalData;
        }

        /**
         * Get or create the planet model (lazy initialization)
         */
        public PlanetModel getPlanetModel() {
            if (planetModel == null) {
                planetModel = new PlanetModel(planetConfig);
            }
            return planetModel;
        }
    }

    /**
     * Orbital mechanics data
     */
    public static class OrbitalData {
        public final double distanceFromParent; // km
        public final double distanceFromStar;   // million km
        public final double orbitalPeriod;      // days
        public final double eccentricity;

        public OrbitalData(double distanceFromParent, double distanceFromStar,
                          double orbitalPeriod, double eccentricity) {
            this.distanceFromParent = distanceFromParent;
            this.distanceFromStar = distanceFromStar;
            this.orbitalPeriod = orbitalPeriod;
            this.eccentricity = eccentricity;
        }
    }

    /**
     * Object type determines dimension generation strategy
     */
    public enum ObjectType {
        TERRESTRIAL,  // Rocky planets/moons with terrain
        SOLAR,        // Stars with lava dimensions
        GASEOUS       // Gas giants with skybox-only dimensions
    }
}
