package net.terradyne.planet.model;

import net.terradyne.planet.PlanetType;
import net.terradyne.planet.config.RockyConfig;
import net.terradyne.util.ModEnums;

// RockyModel.java
public class RockyModel implements IPlanetModel {
    private final RockyConfig config;

    // Calculated rocky properties
    private final float gravity;
    private final float atmosphericPressure;
    private final float thermalInertia;
    private final float surfaceRoughness;
    private final float seismicActivity;
    private final int averageCraterSize;
    private final float resourceAccessibility;

    public RockyModel(RockyConfig config) {
        this.config = config;
        this.gravity = calculateGravity();
        this.atmosphericPressure = calculateAtmosphericPressure();
        this.thermalInertia = calculateThermalInertia();
        this.surfaceRoughness = calculateSurfaceRoughness();
        this.seismicActivity = calculateSeismicActivity();
        this.averageCraterSize = calculateAverageCraterSize();
        this.resourceAccessibility = calculateResourceAccessibility();
    }

    private float calculateGravity() {
        // Rocky planets tend to be smaller (lost atmosphere = lower mass retention)
        float baseGravity = 0.4f;

        // Older planets may be larger (survived longer)
        if (config.getAge() == ModEnums.PlanetAge.ANCIENT) {
            baseGravity += 0.2f;
        } else if (config.getAge() == ModEnums.PlanetAge.YOUNG) {
            baseGravity += 0.1f; // Still forming
        }

        // Higher mineral richness suggests denser core
        baseGravity += config.getMineralRichness() * 0.15f;

        // Surface type affects apparent gravity
        if (config.getDominantSurface() == RockyConfig.SurfaceType.METALLIC) {
            baseGravity += 0.2f; // Dense metallic surface
        }

        return Math.max(0.1f, Math.min(baseGravity, 0.8f));
    }

    private float calculateAtmosphericPressure() {
        // Very thin atmosphere - rocky planets lose atmosphere easily
        float pressure = config.getAtmosphericDensity() * 0.3f;

        // Geological activity can maintain some atmosphere
        pressure += switch (config.getActivity()) {
            case DEAD -> 0.0f;
            case DORMANT -> 0.02f;
            case MINIMAL -> 0.05f;
            case MODERATE -> 0.1f;
        };

        // Age affects atmosphere retention
        if (config.getAge() == ModEnums.PlanetAge.YOUNG) {
            pressure += 0.05f; // Still has some primordial atmosphere
        }

        return Math.max(0.0f, Math.min(pressure, 0.3f));
    }

    private float calculateThermalInertia() {
        // How quickly surface temperature changes
        float inertia = 0.1f; // Very low base (no atmosphere buffer)

        // Regolith acts as insulation
        inertia += (config.getRegolithDepth() / 20.0f) * 0.2f;

        // Bedrock exposure increases thermal conductivity
        inertia += config.getExposedBedrockRatio() * 0.3f;

        // Surface type affects heat retention
        inertia += switch (config.getDominantSurface()) {
            case METALLIC -> 0.4f;        // High thermal conductivity
            case BASALTIC -> 0.3f;        // Moderate
            case ANORTHOSITIC -> 0.2f;    // Lower
            case REGOLITH -> 0.1f;        // Very low
            case FRACTURED -> 0.05f;      // Minimal
        };

        return Math.max(0.05f, Math.min(inertia, 0.8f));
    }

    private float calculateSurfaceRoughness() {
        // How rough/jagged the terrain is
        float roughness = 0.5f;

        // Crater density increases roughness
        roughness += config.getCraterDensity() * 0.3f;

        // Impact history creates rough terrain
        roughness += config.getImpactHistory() * 0.2f;

        // Geological activity affects surface texture
        roughness += switch (config.getActivity()) {
            case DEAD -> 0.0f;           // Smooth, worn down
            case DORMANT -> 0.1f;
            case MINIMAL -> 0.2f;
            case MODERATE -> 0.4f;       // Active processes create roughness
        };

        // Exposed bedrock is typically rougher
        roughness += config.getExposedBedrockRatio() * 0.2f;

        return Math.max(0.2f, Math.min(roughness, 2.0f));
    }

    private float calculateSeismicActivity() {
        // Earthquake/moonquake frequency
        float activity = switch (config.getActivity()) {
            case DEAD -> 0.0f;
            case DORMANT -> 0.1f;
            case MINIMAL -> 0.3f;
            case MODERATE -> 0.7f;
        };

        // Young planets more seismically active
        if (config.getAge() == ModEnums.PlanetAge.YOUNG) {
            activity += 0.2f;
        }

        return Math.max(0.0f, Math.min(activity, 1.0f));
    }

    private int calculateAverageCraterSize() {
        // Crater size in blocks
        int baseSize = 15;

        // Higher impact history = larger craters
        baseSize += (int)(config.getImpactHistory() * 10);

        // Age affects crater preservation and size
        baseSize += switch (config.getAge()) {
            case YOUNG -> 5;      // Recent large impacts
            case MATURE -> 0;     // Mixed sizes
            case ANCIENT -> -5;   // Older craters eroded/buried
        };

        return Math.max(8, Math.min(baseSize, 40));
    }

    private float calculateResourceAccessibility() {
        // How easy it is to access mineral resources
        float accessibility = config.getMineralRichness() * 0.5f;

        // Exposed bedrock makes resources more accessible
        accessibility += config.getExposedBedrockRatio() * 0.3f;

        // Crater impacts expose subsurface materials
        accessibility += config.getCraterDensity() * 0.2f;

        // Regolith depth reduces accessibility
        accessibility -= (config.getRegolithDepth() / 20.0f) * 0.3f;

        return Math.max(0.1f, Math.min(accessibility, 1.0f));
    }

    // Interface implementations
    @Override public RockyConfig getConfig() { return config; }
    @Override public PlanetType getType() { return PlanetType.ROCKY; }
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

    // Rocky-specific getters
    public float getThermalInertia() { return thermalInertia; }
    public float getSurfaceRoughness() { return surfaceRoughness; }
    public float getSeismicActivity() { return seismicActivity; }
    public int getAverageCraterSize() { return averageCraterSize; }
    public float getResourceAccessibility() { return resourceAccessibility; }
}