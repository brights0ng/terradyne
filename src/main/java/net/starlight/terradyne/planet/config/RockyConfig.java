package net.starlight.terradyne.planet.config;

import net.starlight.terradyne.planet.PlanetType;
import net.starlight.terradyne.util.ModEnums;

// RockyConfig.java
public class RockyConfig implements IPlanetConfig {
    private final String planetName;
    private final long seed;
    private final ModEnums.PlanetAge age;

    // Rocky-specific parameters
    private final float atmosphericDensity;    // Very thin atmosphere (0.0-0.3)
    private final float craterDensity;         // Impact crater frequency (0.0-2.0)
    private final float regolithDepth;         // Loose rock/dust layer depth
    private final float exposedBedrockRatio;   // How much bedrock is visible (0.0-1.0)
    private final GeologicalActivity activity; // Tectonic/volcanic activity level
    private final float mineralRichness;       // Ore/mineral concentration (0.0-2.0)
    private final SurfaceType dominantSurface; // Primary surface composition
    private final float temperatureVariation;  // Day/night temperature swing
    private final boolean hasSubsurfaceCaverns; // Underground cave systems
    private final float impactHistory;         // How heavily cratered (0.0-2.0)

    public enum GeologicalActivity {
        DEAD,           // No activity - like our Moon
        DORMANT,        // Very low activity
        MINIMAL,        // Some ancient features
        MODERATE        // Some ongoing processes
    }

    public enum SurfaceType {
        REGOLITH,       // Loose rock fragments and dust
        BASALTIC,       // Volcanic rock surfaces
        ANORTHOSITIC,   // Light-colored ancient crust
        METALLIC,       // Iron/metal rich surfaces
        FRACTURED       // Heavily broken/shattered rock
    }

    public RockyConfig(String planetName, long seed, ModEnums.PlanetAge age,
                       float atmosphericDensity, float craterDensity, float regolithDepth,
                       float exposedBedrockRatio, GeologicalActivity activity, float mineralRichness,
                       SurfaceType dominantSurface, float temperatureVariation,
                       boolean hasSubsurfaceCaverns, float impactHistory) {
        this.planetName = planetName;
        this.seed = seed;
        this.age = age;
        this.atmosphericDensity = Math.max(0.0f, Math.min(0.3f, atmosphericDensity));
        this.craterDensity = Math.max(0.0f, Math.min(2.0f, craterDensity));
        this.regolithDepth = Math.max(0.0f, Math.min(20.0f, regolithDepth));
        this.exposedBedrockRatio = Math.max(0.0f, Math.min(1.0f, exposedBedrockRatio));
        this.activity = activity;
        this.mineralRichness = Math.max(0.0f, Math.min(2.0f, mineralRichness));
        this.dominantSurface = dominantSurface;
        this.temperatureVariation = Math.max(0.0f, Math.min(200.0f, temperatureVariation));
        this.hasSubsurfaceCaverns = hasSubsurfaceCaverns;
        this.impactHistory = Math.max(0.0f, Math.min(2.0f, impactHistory));
    }

    // Interface implementations
    @Override public String getPlanetName() { return planetName; }
    @Override public long getSeed() { return seed; }
    @Override public ModEnums.PlanetAge getAge() { return age; }
    @Override public PlanetType getType() { return PlanetType.ROCKY; }

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

    // Rocky-specific getters
    public float getAtmosphericDensity() { return atmosphericDensity; }
    public float getCraterDensity() { return craterDensity; }
    public float getRegolithDepth() { return regolithDepth; }
    public float getExposedBedrockRatio() { return exposedBedrockRatio; }
    public GeologicalActivity getActivity() { return activity; }
    public float getMineralRichness() { return mineralRichness; }
    public SurfaceType getDominantSurface() { return dominantSurface; }
    public float getTemperatureVariation() { return temperatureVariation; }
    public boolean hasSubsurfaceCaverns() { return hasSubsurfaceCaverns; }
    public float getImpactHistory() { return impactHistory; }
}