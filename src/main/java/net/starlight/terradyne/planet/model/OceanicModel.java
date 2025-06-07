package net.starlight.terradyne.planet.model;

import net.starlight.terradyne.planet.PlanetType;
import net.starlight.terradyne.planet.config.OceanicConfig;
import net.starlight.terradyne.util.ModEnums;

// OceanicModel.java
public class OceanicModel implements IPlanetModel {
    private final OceanicConfig config;

    // Calculated oceanic properties
    private final float gravity;
    private final float atmosphericPressure;
    private final int seaLevel;
    private final float thermalRegulation;
    private final float biodiversityIndex;
    private final float stormFrequency;
    private final float coastlineComplexity;

    public OceanicModel(OceanicConfig config) {
        this.config = config;
        this.gravity = calculateGravity();
        this.atmosphericPressure = calculateAtmosphericPressure();
        this.seaLevel = calculateSeaLevel();
        this.thermalRegulation = calculateThermalRegulation();
        this.biodiversityIndex = calculateBiodiversityIndex();
        this.stormFrequency = calculateStormFrequency();
        this.coastlineComplexity = calculateCoastlineComplexity();
    }

    private float calculateGravity() {
        // Oceanic worlds tend to be larger (need mass to retain atmosphere and water)
        float baseGravity = 0.9f;

        // Higher ocean coverage suggests larger planet
        baseGravity += config.getOceanCoverage() * 0.3f;

        // Older planets may have lost some mass
        if (config.getAge() == ModEnums.PlanetAge.ANCIENT) {
            baseGravity -= 0.1f;
        } else if (config.getAge() == ModEnums.PlanetAge.YOUNG) {
            baseGravity += 0.1f;
        }

        return Math.max(0.6f, Math.min(baseGravity, 1.4f));
    }

    private float calculateAtmosphericPressure() {
        // Dense atmosphere needed to support liquid water
        float basePressure = 0.8f;

        // Higher humidity = denser atmosphere
        basePressure += config.getAtmosphericHumidity() * 0.4f;

        // Ocean coverage affects atmospheric density
        basePressure += config.getOceanCoverage() * 0.3f;

        // Weather intensity affects pressure systems
        basePressure += config.getWeatherIntensity() * 0.2f;

        return Math.max(0.5f, Math.min(basePressure, 1.5f));
    }

    private int calculateSeaLevel() {
        // Base sea level around standard Minecraft level
        int baseLevel = 62;

        // Deeper oceans = higher relative sea level
        int depthAdjustment = (int) (config.getAverageOceanDepth() * 0.3f);

        // Tidal range affects apparent sea level
        int tidalAdjustment = (int) (config.getTidalRange() * 2);

        return baseLevel + depthAdjustment - tidalAdjustment;
    }

    private float calculateThermalRegulation() {
        // Oceans moderate temperature - larger oceans = more stable climate
        float regulation = config.getOceanCoverage() * 0.8f;

        // Deep oceans store more heat
        regulation += (config.getAverageOceanDepth() / 100.0f) * 0.4f;

        // Atmospheric humidity helps regulate temperature
        regulation += config.getAtmosphericHumidity() * 0.3f;

        return Math.max(0.1f, Math.min(regulation, 1.0f));
    }

    private float calculateBiodiversityIndex() {
        // Oceanic worlds can support diverse life
        float diversity = 0.6f;

        // More ocean coverage = more marine life potential
        diversity += config.getOceanCoverage() * 0.3f;

        // Continental shelves support biodiversity
        diversity += (config.getContinentalShelfWidth() / 50.0f) * 0.2f;

        // Moderate crustal activity creates diverse habitats
        if (config.getCrustalActivity() > 0.3f && config.getCrustalActivity() < 1.5f) {
            diversity += 0.2f;
        }

        // Thermal regulation supports stable ecosystems
        diversity += thermalRegulation * 0.2f;

        return Math.max(0.1f, Math.min(diversity, 1.0f));
    }

    private float calculateStormFrequency() {
        // Weather intensity base
        float storms = config.getWeatherIntensity() * 0.5f;

        // Large oceans generate more weather systems
        storms += config.getOceanCoverage() * 0.3f;

        // High humidity increases storm potential
        storms += config.getAtmosphericHumidity() * 0.4f;

        // Temperature regulation reduces extreme weather
        storms -= thermalRegulation * 0.2f;

        return Math.max(0.0f, Math.min(storms, 1.0f));
    }

    private float calculateCoastlineComplexity() {
        // More continents = more complex coastlines
        float complexity = (config.getContinentCount() / 8.0f) * 0.4f;

        // Tectonic activity creates complex geography
        complexity += config.getCrustalActivity() * 0.3f;

        // Ocean type affects coastline features
        complexity += switch (config.getDominantOceanType()) {
            case ARCHIPELAGO -> 0.8f;
            case TROPICAL -> 0.6f;
            case TEMPERATE -> 0.5f;
            case POLAR -> 0.3f;
            case DEEP_ABYSS -> 0.2f;
        };

        return Math.max(0.1f, Math.min(complexity, 1.0f));
    }

    // Interface implementations
    @Override public OceanicConfig getConfig() { return config; }
    @Override public PlanetType getType() { return PlanetType.OCEANIC; }
    @Override public float getGravity() { return gravity; }
    @Override public float getAtmosphericPressure() { return atmosphericPressure; }

    @Override
    public float getErosionRate() {
        return 0;
    }

    @Override
    public boolean hasLooseMaterialFormations() {
        return false;
    }

    @Override
    public float getLooseMaterialFormationHeight() {
        return 0;
    }

    @Override
    public float getSolidMaterialExposure() {
        return 0;
    }

    // Oceanic-specific getters
    public int getSeaLevel() { return seaLevel; }
    public float getThermalRegulation() { return thermalRegulation; }
    public float getBiodiversityIndex() { return biodiversityIndex; }
    public float getStormFrequency() { return stormFrequency; }
    public float getCoastlineComplexity() { return coastlineComplexity; }
}