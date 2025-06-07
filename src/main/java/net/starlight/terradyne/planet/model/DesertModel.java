package net.starlight.terradyne.planet.model;

import net.starlight.terradyne.planet.PlanetType;
import net.starlight.terradyne.planet.config.DesertConfig;
import net.starlight.terradyne.util.ModEnums;

// DesertModel.java - NEW FILE
public class DesertModel implements IPlanetModel {
    private final DesertConfig config;

    // Calculated desert properties
    private final float gravity;
    private final float atmosphericPressure;
    private final float erosionRate;
    private final float duneHeight;
    private final float rockExposure;
    private final float thermalInertia;

    public DesertModel(DesertConfig config) {
        this.config = config;
        this.gravity = calculateGravity();
        this.atmosphericPressure = calculateAtmosphericPressure();
        this.erosionRate = calculateErosionRate();
        this.duneHeight = calculateDuneHeight();
        this.rockExposure = calculateRockExposure();
        this.thermalInertia = calculateThermalInertia();
    }

    private float calculateGravity() {
        // Desert planets tend to be smaller (less water retention)
        // Adjust based on age and temperature
        float baseGravity = 0.7f;

        if (config.getAge() == ModEnums.PlanetAge.YOUNG) {
            baseGravity += 0.1f; // Young planets may be larger
        }

        if (config.getSurfaceTemperature() > 50) {
            baseGravity -= 0.1f; // Very hot = lost atmosphere/water = smaller
        }

        return Math.max(0.3f, Math.min(baseGravity, 1.2f));
    }

    private float calculateAtmosphericPressure() {
        // Thin atmosphere due to low humidity and high temperature
        float basePressure = 0.3f;

        // Lower humidity = thinner atmosphere
        basePressure *= (0.5f + config.getHumidity() * 0.5f);

        // Higher temperature = more atmosphere loss
        if (config.getSurfaceTemperature() > 40) {
            basePressure *= 0.7f;
        }

        // Dust storms increase apparent pressure
        basePressure += config.getDustStormFrequency() * 0.2f;

        return Math.max(0.1f, Math.min(basePressure, 1.0f));
    }

    private float calculateErosionRate() {
        // Wind erosion based on wind strength and sand density
        float erosion = config.getWindStrength() * config.getSandDensity();

        // Low humidity increases erosion (no vegetation protection)
        erosion *= (2.0f - config.getHumidity());

        return Math.min(erosion, 3.0f);
    }

    private float calculateDuneHeight() {
        if (!config.hasDunes()) return 0.0f;

        // Dune height based on sand density and wind strength
        float height = config.getSandDensity() * config.getWindStrength() * 15.0f;

        // Age affects dune development
        if (config.getAge() == ModEnums.PlanetAge.ANCIENT) {
            height *= 1.5f; // More time for dune formation
        }

        return Math.min(height, 40.0f); // Max 40 block high dunes
    }

    private float calculateRockExposure() {
        // How much bedrock is exposed (inverse of sand coverage)
        float exposure = 1.0f - config.getSandDensity();

        // High erosion exposes more rock
        exposure += erosionRate * 0.2f;

        // Age affects exposure
        if (config.getAge() == ModEnums.PlanetAge.YOUNG) {
            exposure *= 0.7f; // Less time for sand accumulation
        }

        return Math.max(0.0f, Math.min(exposure, 1.0f));
    }

    private float calculateThermalInertia() {
        // How quickly temperature changes (affects day/night cycle)
        float inertia = 0.5f;

        // More sand = lower thermal inertia (faster heating/cooling)
        inertia -= config.getSandDensity() * 0.3f;

        // More rock exposure = higher thermal inertia
        inertia += rockExposure * 0.4f;

        return Math.max(0.1f, Math.min(inertia, 1.0f));
    }

    // Interface implementations
    @Override public DesertConfig getConfig() { return config; }
    @Override public PlanetType getType() { return PlanetType.DESERT; }
    @Override public float getGravity() { return gravity; }
    @Override public float getAtmosphericPressure() { return atmosphericPressure; }
    @Override public boolean hasLooseMaterialFormations() { return config.hasDunes(); }
    @Override public float getLooseMaterialFormationHeight() { return duneHeight; }
    @Override public float getSolidMaterialExposure() { return rockExposure; }

    // Desert-specific getters
    public float getErosionRate() { return erosionRate; }
    public float getDuneHeight() { return duneHeight; }
    public float getRockExposure() { return rockExposure; }
    public float getThermalInertia() { return thermalInertia; }
}