package net.starlight.terradyne.planet.physics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Ultra-simple user input parameters for physics-based generation
 * No validation - all constraint enforcement handled by PhysicsCalculator
 */
public class PlanetConfig {
    
    // Codec for serialization/deserialization
    public static final Codec<PlanetConfig> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.STRING.fieldOf("planet_name").forGetter(PlanetConfig::getPlanetName),
            Codec.LONG.fieldOf("seed").forGetter(PlanetConfig::getSeed),
            Codec.INT.fieldOf("circumference").forGetter(PlanetConfig::getCircumference),
            Codec.LONG.fieldOf("distance_from_star").forGetter(PlanetConfig::getDistanceFromStar),
            Codec.STRING.fieldOf("crust_composition").forGetter(c -> c.getCrustComposition().name()),
            Codec.STRING.fieldOf("atmosphere_composition").forGetter(c -> c.getAtmosphereComposition().name()),
            Codec.DOUBLE.fieldOf("tectonic_activity").forGetter(PlanetConfig::getTectonicActivity),
            Codec.DOUBLE.fieldOf("water_content").forGetter(PlanetConfig::getWaterContent),
            Codec.DOUBLE.fieldOf("crustal_thickness").forGetter(PlanetConfig::getCrustalThickness),
            Codec.DOUBLE.fieldOf("atmospheric_density").forGetter(PlanetConfig::getAtmosphericDensity),
            Codec.DOUBLE.fieldOf("rotation_period").forGetter(PlanetConfig::getRotationPeriod),
            Codec.DOUBLE.fieldOf("noise_scale").forGetter(PlanetConfig::getNoiseScale)
        ).apply(instance, PlanetConfig::fromCodec)
    );
    
    /**
     * Codec constructor - reconstructs PlanetConfig from JSON
     */
    private static PlanetConfig fromCodec(String planetName, long seed, int circumference,
                                          long distanceFromStar, String crustComposition,
                                          String atmosphereComposition, double tectonicActivity,
                                          double waterContent, double crustalThickness,
                                          double atmosphericDensity, double rotationPeriod,
                                          double noiseScale) {
        PlanetConfig config = new PlanetConfig(planetName, seed);
        config.circumference = circumference;
        config.distanceFromStar = distanceFromStar;
        config.crustComposition = CrustComposition.valueOf(crustComposition);
        config.atmosphereComposition = AtmosphereComposition.valueOf(atmosphereComposition);
        config.tectonicActivity = tectonicActivity;
        config.waterContent = waterContent;
        config.crustalThickness = crustalThickness;
        config.atmosphericDensity = atmosphericDensity;
        config.rotationPeriod = rotationPeriod;
        config.noiseScale = noiseScale;
        return config;
    }
    
    // === CORE PHYSICAL PROPERTIES ===
    private final String planetName;
    private final long seed;
    private int circumference;              // Planet diameter in km (1km = 1 block)
    private long distanceFromStar;          // Millions of km from star
    private CrustComposition crustComposition;
    private AtmosphereComposition atmosphereComposition;
    
    // === DERIVED PROPERTIES (user-configurable, physics-constrained) ===
    private double tectonicActivity;        // 0.0-1.0: Geological activity level
    private double waterContent;            // 0.0-1.0: Planetary water abundance  
    private double crustalThickness;        // km: Average crust thickness
    private double atmosphericDensity;      // 0.0-1.0: Atmosphere thickness
    private double rotationPeriod;          // MC days: Day/night cycle length
    
    // === GENERATION PARAMETERS ===
    private double noiseScale;              // Noise scale for tectonic plate size

    /**
     * Create a new planet configuration with Earth-like defaults
     */
    public PlanetConfig(String planetName, long seed) {
        this.planetName = planetName;
        this.seed = seed;
        
        // Set Earth-like defaults - no validation here
        this.circumference = 40000;  // Earth-like
        this.distanceFromStar = 150; // Earth-like (150M km)
        this.crustComposition = CrustComposition.SILICATE;
        this.atmosphereComposition = AtmosphereComposition.OXYGEN_RICH;
        this.tectonicActivity = 0.6;
        this.waterContent = 0.7;
        this.crustalThickness = 35.0; // Continental crust thickness
        this.atmosphericDensity = 1.0; // Earth-like
        this.rotationPeriod = 1.0;     // 24-hour day
        this.noiseScale = 0.002;       // Medium-sized plates
    }

    // === SIMPLE SETTERS (no validation) ===

    public PlanetConfig setCircumference(int circumference) {
        this.circumference = circumference;
        return this;
    }

    public PlanetConfig setDistanceFromStar(long distanceFromStar) {
        this.distanceFromStar = distanceFromStar;
        return this;
    }

    public PlanetConfig setCrustComposition(CrustComposition crustComposition) {
        this.crustComposition = crustComposition;
        return this;
    }

    public PlanetConfig setAtmosphereComposition(AtmosphereComposition atmosphereComposition) {
        this.atmosphereComposition = atmosphereComposition;
        return this;
    }

    public PlanetConfig setTectonicActivity(double tectonicActivity) {
        this.tectonicActivity = tectonicActivity;
        return this;
    }

    public PlanetConfig setWaterContent(double waterContent) {
        this.waterContent = waterContent;
        return this;
    }

    public PlanetConfig setCrustalThickness(double crustalThickness) {
        this.crustalThickness = crustalThickness;
        return this;
    }

    public PlanetConfig setAtmosphericDensity(double atmosphericDensity) {
        this.atmosphericDensity = atmosphericDensity;
        return this;
    }

    public PlanetConfig setRotationPeriod(double rotationPeriod) {
        this.rotationPeriod = rotationPeriod;
        return this;
    }

    public PlanetConfig setNoiseScale(double noiseScale) {
        this.noiseScale = noiseScale;
        return this;
    }

    // === GETTERS ===

    public String getPlanetName() { return planetName; }
    public long getSeed() { return seed; }
    public int getCircumference() { return circumference; }
    public long getDistanceFromStar() { return distanceFromStar; }
    public CrustComposition getCrustComposition() { return crustComposition; }
    public AtmosphereComposition getAtmosphereComposition() { return atmosphereComposition; }
    public double getTectonicActivity() { return tectonicActivity; }
    public double getWaterContent() { return waterContent; }
    public double getCrustalThickness() { return crustalThickness; }
    public double getAtmosphericDensity() { return atmosphericDensity; }
    public double getRotationPeriod() { return rotationPeriod; }
    public double getNoiseScale() { return noiseScale; }

    @Override
    public String toString() {
        return String.format("PlanetConfig{name='%s', circumference=%d km, starDistance=%d Mkm, crust=%s}",
                planetName, circumference, distanceFromStar, crustComposition.getDisplayName());
    }
}