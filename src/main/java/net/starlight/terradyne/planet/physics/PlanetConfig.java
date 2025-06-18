// PlanetConfig.java
package net.starlight.terradyne.planet.physics;

/**
 * Abstract base class for planet configuration.
 * This is the primary API interface for mod users to define planetary parameters.
 * Users should extend this class to create custom planet configurations.
 */
public abstract class PlanetConfig {

    // Required abstract methods for planet parameters
    public abstract int getCircumference(); // in km/blocks (1km = 1 block)
    public abstract long getDistanceFromStar(); // in millions of km
    public abstract PlanetData.CrustComposition getCrustComposition();
    public abstract long getSeed();
    public abstract double getTectonicActivity(); // 0.0 to 1.0
    public abstract double getWaterContent(); // 0.0 to 1.0
    public abstract PlanetData.AtmosphereComposition getAtmosphereComposition();
    public abstract int getCrustalThickness(); // in km, average surface height = thickness/10
    public abstract double getAtmosphericDensity(); // 0.0 to 1.0
    public abstract double getRotationPeriod(); // in MC days

    /**
     * Optional method for planet name/identifier
     */
    public String getPlanetName() {
        return "Planet_" + getSeed();
    }

    /**
     * Validates that all parameters are within acceptable ranges
     */
    public void validate() {
        if (getCircumference() <= 0) {
            throw new IllegalArgumentException("Planet circumference must be positive");
        }
        if (getDistanceFromStar() < 0) {
            throw new IllegalArgumentException("Distance from star cannot be negative");
        }
        if (getTectonicActivity() < 0.0 || getTectonicActivity() > 1.0) {
            throw new IllegalArgumentException("Tectonic activity must be between 0.0 and 1.0");
        }
        if (getWaterContent() < 0.0 || getWaterContent() > 1.0) {
            throw new IllegalArgumentException("Water content must be between 0.0 and 1.0");
        }
        if (getCrustalThickness() <= 0) {
            throw new IllegalArgumentException("Crustal thickness must be positive");
        }
        if (getAtmosphericDensity() < 0.0 || getAtmosphericDensity() > 1.0) {
            throw new IllegalArgumentException("Atmospheric density must be between 0.0 and 1.0");
        }
        if (getRotationPeriod() <= 0.0) {
            throw new IllegalArgumentException("Rotation period must be positive");
        }
    }
}