package net.starlight.terradyne.planet.config;

import net.starlight.terradyne.planet.PlanetType;
import net.starlight.terradyne.util.ModEnums;

// DesertConfig.java - NEW FILE
public class DesertConfig implements IPlanetConfig {
    private final String planetName;
    private final long seed;
    private final ModEnums.PlanetAge age;

    // Desert-specific parameters
    private final float surfaceTemperature;    // Average temperature (Celsius)
    private final float humidity;              // Atmospheric moisture (0.0-0.3)
    private final float windStrength;          // Wind erosion factor (0.0-2.0)
    private final float sandDensity;           // How much sand vs rock (0.0-1.0)
    private final boolean hasDunes;            // Large dune formations
    private final float dayNightTempDiff;      // Temperature variation
    private final float dustStormFrequency;    // Atmospheric dust (0.0-1.0)
    private final RockType dominantRock;       // Underlying geology
    @Override public float getWindStrength() { return windStrength; }
    @Override public float getSurfaceTemperature() { return surfaceTemperature; }
    @Override public float getLooseMaterialDensity() { return sandDensity; } // Maps to sandDensity
    @Override public LooseMaterialType getLooseMaterialType() {
        return LooseMaterialType.SAND; // Desert = sand
    }

    public enum RockType {
        SANDSTONE,    // Sedimentary desert
        GRANITE,      // Rocky desert
        LIMESTONE,    // Calcified desert
        VOLCANIC      // Volcanic desert
    }

    public DesertConfig(String planetName, long seed, ModEnums.PlanetAge age,
                        float surfaceTemperature, float humidity, float windStrength,
                        float sandDensity, boolean hasDunes, float dayNightTempDiff,
                        float dustStormFrequency, RockType dominantRock) {
        this.planetName = planetName;
        this.seed = seed;
        this.age = age;
        this.surfaceTemperature = surfaceTemperature;
        this.humidity = humidity;
        this.windStrength = windStrength;
        this.sandDensity = sandDensity;
        this.hasDunes = hasDunes;
        this.dayNightTempDiff = dayNightTempDiff;
        this.dustStormFrequency = dustStormFrequency;
        this.dominantRock = dominantRock;
    }

    // Interface implementations
    @Override public String getPlanetName() { return planetName; }
    @Override public long getSeed() { return seed; }
    @Override public ModEnums.PlanetAge getAge() { return age; }
    @Override public PlanetType getType() { return PlanetType.DESERT; }

    public float getHumidity() { return humidity; }
    public float getSandDensity() { return sandDensity; }
    public boolean hasDunes() { return hasDunes; }
    public float getDayNightTempDiff() { return dayNightTempDiff; }
    public float getDustStormFrequency() { return dustStormFrequency; }
    public RockType getDominantRock() { return dominantRock; }
}