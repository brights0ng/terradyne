package net.starlight.terradyne.planet.physics;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.starlight.terradyne.planet.PlanetType;
import net.starlight.terradyne.util.ModEnums;

/**
 * Physics-based planet configuration
 * All planet properties that drive the simulation
 */
public class PlanetPhysicsConfig implements IPlanetConfig {
    private final String planetName;
    private final long seed;
    private final ModEnums.PlanetAge age;
    
    // Tectonic parameters
    private final float tectonicScale;        // 0.0 = no plates, 1.0 = Earth-like
    private final float tectonicActivity;     // 0.0 = dead, 1.0 = extreme activity
    
    // Environmental parameters (for future use)
    private final float waterHeight;          // 0.0 = no water, 1.0 = water world
    private final float atmosphereDensity;    // 0.0 = vacuum, 1.0 = Earth-like
    private final float distanceFromSun;      // In hundreds of millions of km
    private final float planetCircumference;  // In tens of thousands of km
    private final float rotationPeriod;       // In Earth days
    private final float crustalThickness;     // In km (1km = 30 blocks depth)
    
    // Calculated values
    private final float gravity;
    private final float surfaceTempMin;
    private final float surfaceTempMax;
    
    public PlanetPhysicsConfig(String planetName, long seed, ModEnums.PlanetAge age,
                               float tectonicScale, float tectonicActivity,
                               float waterHeight, float atmosphereDensity,
                               float distanceFromSun, float planetCircumference,
                               float rotationPeriod, float crustalThickness) {
        this.planetName = planetName;
        this.seed = seed;
        this.age = age;
        this.tectonicScale = Math.max(0.0f, Math.min(1.0f, tectonicScale));
        this.tectonicActivity = Math.max(0.0f, Math.min(1.0f, tectonicActivity));
        this.waterHeight = Math.max(0.0f, Math.min(1.0f, waterHeight));
        this.atmosphereDensity = Math.max(0.0f, atmosphereDensity); // Can go above 1.0
        this.distanceFromSun = Math.max(0.1f, distanceFromSun);
        this.planetCircumference = Math.max(0.5f, planetCircumference);
        this.rotationPeriod = Math.max(0.1f, rotationPeriod);
        this.crustalThickness = Math.max(1.0f, crustalThickness);
        
        // Calculate derived values
        this.gravity = calculateGravity();
        this.surfaceTempMin = calculateMinTemperature();
        this.surfaceTempMax = calculateMaxTemperature();
    }
    
    private float calculateGravity() {
        // Simplified: gravity proportional to planet size
        // Earth (circumference 4.0) = 1.0g
        return planetCircumference / 4.0f;
    }
    
    private float calculateMinTemperature() {
        // Base temperature from distance to sun
        float baseTemp = 280.0f / distanceFromSun; // Kelvin
        
        // Greenhouse effect from atmosphere
        float greenhouseEffect = atmosphereDensity * 30.0f;
        
        // Tectonic heating
        float tectonicHeat = tectonicActivity * 10.0f;
        
        // Convert to Celsius and apply modifiers
        return baseTemp - 273.0f + greenhouseEffect + tectonicHeat - 50.0f; // -50 for daily minimum
    }
    
    private float calculateMaxTemperature() {
        // Similar to min but with daily maximum
        float baseTemp = 280.0f / distanceFromSun;
        float greenhouseEffect = atmosphereDensity * 30.0f;
        float tectonicHeat = tectonicActivity * 10.0f;
        
        return baseTemp - 273.0f + greenhouseEffect + tectonicHeat + 50.0f; // +50 for daily maximum
    }
    
    // IPlanetConfig implementation
    @Override
    public String getPlanetName() { return planetName; }
    
    @Override
    public long getSeed() { return seed; }
    
    @Override
    public ModEnums.PlanetAge getAge() { return age; }
    
    @Override
    public PlanetType getType() {
        // Type emerges from physics!
        if (waterHeight > 0.7f) return PlanetType.OCEANIC;
        if (atmosphereDensity < 0.1f) return PlanetType.ROCKY;
        if (tectonicActivity > 0.7f) return PlanetType.VOLCANIC;
        if (waterHeight < 0.1f && surfaceTempMax > 40.0f) return PlanetType.DESERT;
        if (surfaceTempMax < -20.0f) return PlanetType.ICY;
        return PlanetType.ROCKY; // Default
    }
    
    // Wind/erosion stub implementations for IPlanetConfig
    @Override
    public float getWindStrength() {
        return atmosphereDensity > 0 ? 0.5f : 0.0f; // Placeholder
    }
    
    @Override
    public float getSurfaceTemperature() {
        return (surfaceTempMin + surfaceTempMax) / 2.0f;
    }
    
    @Override
    public float getLooseMaterialDensity() {
        return 0.5f; // Placeholder
    }
    
    @Override
    public LooseMaterialType getLooseMaterialType() {
        if (surfaceTempMax < 0) return LooseMaterialType.SNOW;
        if (waterHeight < 0.2f) return LooseMaterialType.SAND;
        return LooseMaterialType.REGOLITH;
    }
    
    // Getters for physics parameters
    public float getTectonicScale() { return tectonicScale; }
    public float getTectonicActivity() { return tectonicActivity; }
    public float getWaterHeight() { return waterHeight; }
    public float getAtmosphereDensity() { return atmosphereDensity; }
    public float getDistanceFromSun() { return distanceFromSun; }
    public float getPlanetCircumference() { return planetCircumference; }
    public float getRotationPeriod() { return rotationPeriod; }
    public float getCrustalThickness() { return crustalThickness; }
    public float getGravity() { return gravity; }
    public float getSurfaceTempMin() { return surfaceTempMin; }
    public float getSurfaceTempMax() { return surfaceTempMax; }
}