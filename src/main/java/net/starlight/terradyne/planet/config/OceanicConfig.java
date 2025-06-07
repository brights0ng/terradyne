package net.starlight.terradyne.planet.config;

import net.starlight.terradyne.planet.PlanetType;
import net.starlight.terradyne.util.ModEnums;

// OceanicConfig.java
public class OceanicConfig implements IPlanetConfig {
    private final String planetName;
    private final long seed;
    private final ModEnums.PlanetAge age;

    // Oceanic-specific parameters
    private final float oceanCoverage;         // % of surface covered by water (0.0-1.0)
    private final float averageOceanDepth;     // Average ocean depth in blocks
    private final float continentalShelfWidth; // How far shallow water extends
    private final float tidalRange;            // Tidal variation strength
    private final int continentCount;          // Number of major landmasses
    private final float atmosphericHumidity;   // Moisture in atmosphere (0.0-1.0)
    private final boolean hasIceCaps;          // Polar ice formations
    private final float crustalActivity;       // Tectonic activity (0.0-2.0)
    private final OceanType dominantOceanType; // Primary ocean characteristics
    private final float weatherIntensity;      // Storm/weather frequency

    public enum OceanType {
        TROPICAL,     // Warm, clear waters
        TEMPERATE,    // Earth-like moderate oceans
        POLAR,        // Cold, ice-prone waters
        DEEP_ABYSS,   // Very deep oceanic trenches
        ARCHIPELAGO   // Many islands and shallow seas
    }

    public OceanicConfig(String planetName, long seed, ModEnums.PlanetAge age,
                         float oceanCoverage, float averageOceanDepth, float continentalShelfWidth,
                         float tidalRange, int continentCount, float atmosphericHumidity,
                         boolean hasIceCaps, float crustalActivity, OceanType dominantOceanType,
                         float weatherIntensity) {
        this.planetName = planetName;
        this.seed = seed;
        this.age = age;
        this.oceanCoverage = Math.max(0.0f, Math.min(1.0f, oceanCoverage));
        this.averageOceanDepth = Math.max(5.0f, Math.min(100.0f, averageOceanDepth));
        this.continentalShelfWidth = Math.max(0.0f, Math.min(50.0f, continentalShelfWidth));
        this.tidalRange = Math.max(0.0f, Math.min(10.0f, tidalRange));
        this.continentCount = Math.max(1, Math.min(8, continentCount));
        this.atmosphericHumidity = Math.max(0.0f, Math.min(1.0f, atmosphericHumidity));
        this.hasIceCaps = hasIceCaps;
        this.crustalActivity = Math.max(0.0f, Math.min(2.0f, crustalActivity));
        this.dominantOceanType = dominantOceanType;
        this.weatherIntensity = Math.max(0.0f, Math.min(2.0f, weatherIntensity));
    }

    // Interface implementations
    @Override public String getPlanetName() { return planetName; }
    @Override public long getSeed() { return seed; }
    @Override public ModEnums.PlanetAge getAge() { return age; }
    @Override public PlanetType getType() { return PlanetType.OCEANIC; }

    @Override
    public float getWindStrength() {
        return 0;
    }

    @Override
    public float getSurfaceTemperature() {
        return 0;
    }

    @Override
    public float getLooseMaterialDensity() {
        return 0;
    }

    @Override
    public LooseMaterialType getLooseMaterialType() {
        return null;
    }

    // Oceanic-specific getters
    public float getOceanCoverage() { return oceanCoverage; }
    public float getAverageOceanDepth() { return averageOceanDepth; }
    public float getContinentalShelfWidth() { return continentalShelfWidth; }
    public float getTidalRange() { return tidalRange; }
    public int getContinentCount() { return continentCount; }
    public float getAtmosphericHumidity() { return atmosphericHumidity; }
    public boolean hasIceCaps() { return hasIceCaps; }
    public float getCrustalActivity() { return crustalActivity; }
    public OceanType getDominantOceanType() { return dominantOceanType; }
    public float getWeatherIntensity() { return weatherIntensity; }
}