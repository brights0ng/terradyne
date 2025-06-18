// PlanetData.java
package net.starlight.terradyne.planet.physics;

import net.minecraft.block.Block;

/**
 * Immutable data container holding all planet parameters.
 * Contains both input parameters from PlanetConfig and calculated parameters from PhysicsCalculator.
 */
public final class PlanetData {

    // Input parameters (from PlanetConfig)
    private final int circumference;
    private final long distanceFromStar;
    private final CrustComposition crustComposition;
    private final long seed;
    private final double tectonicActivity;
    private final double waterContent;
    private final AtmosphereComposition atmosphereComposition;
    private final int crustalThickness;
    private final double atmosphericDensity;
    private final double rotationPeriod;
    private final String planetName;

    // Calculated parameters (filled by PhysicsCalculator)
    private final double gravity;
    private final PlanetAge planetAge;
    private final double averageSurfaceTemp;
    private final double habitability;
    private final double waterErosion;
    private final double windErosion;
    private final String mainRockType; // Changed from Block to String for testing

    // Adjusted parameters (after constraint validation)
    private final double adjustedWaterContent;
    private final double adjustedAtmosphericDensity;
    private final double adjustedTectonicActivity;

    private PlanetData(Builder builder) {
        // Input parameters
        this.circumference = builder.circumference;
        this.distanceFromStar = builder.distanceFromStar;
        this.crustComposition = builder.crustComposition;
        this.seed = builder.seed;
        this.tectonicActivity = builder.tectonicActivity;
        this.waterContent = builder.waterContent;
        this.atmosphereComposition = builder.atmosphereComposition;
        this.crustalThickness = builder.crustalThickness;
        this.atmosphericDensity = builder.atmosphericDensity;
        this.rotationPeriod = builder.rotationPeriod;
        this.planetName = builder.planetName;

        // Calculated parameters
        this.gravity = builder.gravity;
        this.planetAge = builder.planetAge;
        this.averageSurfaceTemp = builder.averageSurfaceTemp;
        this.habitability = builder.habitability;
        this.waterErosion = builder.waterErosion;
        this.windErosion = builder.windErosion;
        this.mainRockType = builder.mainRockType;

        // Adjusted parameters
        this.adjustedWaterContent = builder.adjustedWaterContent;
        this.adjustedAtmosphericDensity = builder.adjustedAtmosphericDensity;
        this.adjustedTectonicActivity = builder.adjustedTectonicActivity;
    }

    // Input parameter getters
    public int getCircumference() { return circumference; }
    public long getDistanceFromStar() { return distanceFromStar; }
    public CrustComposition getCrustComposition() { return crustComposition; }
    public long getSeed() { return seed; }
    public double getTectonicActivity() { return tectonicActivity; }
    public double getWaterContent() { return waterContent; }
    public AtmosphereComposition getAtmosphereComposition() { return atmosphereComposition; }
    public int getCrustalThickness() { return crustalThickness; }
    public double getAtmosphericDensity() { return atmosphericDensity; }
    public double getRotationPeriod() { return rotationPeriod; }
    public String getPlanetName() { return planetName; }

    // Calculated parameter getters
    public double getGravity() { return gravity; }
    public PlanetAge getPlanetAge() { return planetAge; }
    public double getAverageSurfaceTemp() { return averageSurfaceTemp; }
    public double getHabitability() { return habitability; }
    public double getWaterErosion() { return waterErosion; }
    public double getWindErosion() { return windErosion; }
    public String getMainRockType() { return mainRockType; }

    // Adjusted parameter getters (use these for actual generation)
    public double getAdjustedWaterContent() { return adjustedWaterContent; }
    public double getAdjustedAtmosphericDensity() { return adjustedAtmosphericDensity; }
    public double getAdjustedTectonicActivity() { return adjustedTectonicActivity; }

    /**
     * Creates a new PlanetData from a PlanetConfig with unset calculated parameters.
     * PhysicsCalculator will fill in the calculated values.
     */
    public static PlanetData fromConfig(PlanetConfig config) {
        config.validate();
        return new Builder()
                .setInputParameters(config)
                .build();
    }

    /**
     * Creates a copy of this PlanetData with calculated parameters filled in.
     */
    public PlanetData withCalculatedParameters(double gravity, PlanetAge planetAge,
                                               double averageSurfaceTemp, double habitability,
                                               double waterErosion, double windErosion,
                                               String mainRockType,
                                               double adjustedWaterContent,
                                               double adjustedAtmosphericDensity,
                                               double adjustedTectonicActivity) {
        return new Builder()
                .copyFrom(this)
                .setCalculatedParameters(gravity, planetAge, averageSurfaceTemp, habitability,
                        waterErosion, windErosion, mainRockType,
                        adjustedWaterContent, adjustedAtmosphericDensity,
                        adjustedTectonicActivity)
                .build();
    }

    public enum PlanetAge {
        INFANT,  // Very young, still forming
        YOUNG,   // Recently formed, high activity
        OLD,     // Mature, moderate activity
        DEAD     // Ancient, little to no activity
    }

    public enum CrustComposition {
        SILICATE,    // Rocky, Earth-like
        FERROUS,     // Iron-rich
        BASALT,      // Volcanic
        REGOLITH,    // Dusty, weathered
        HADEAN,      // Molten, early formation
        CARBON,      // Carbon-rich
        SULFUR,      // Sulfur compounds
        HALIDE,      // Salt-like minerals
        METAL        // Metallic surface
    }

    public enum AtmosphereComposition {
        OXYGEN_RICH,           // Earth-like
        CARBON_DIOXIDE,        // Venus/Mars-like
        METHANE,              // Titan-like
        NITROGEN_RICH,        // Thick nitrogen
        NOBLE_GAS_MIXTURE,    // Inert gases
        WATER_VAPOR_RICH,     // Steam atmosphere
        HYDROGEN_SULFIDE,     // Toxic gases
        TRACE_ATMOSPHERE,     // Very thin
        VACUUM                // No atmosphere
    }

    public static class Builder {
        // Input parameters
        private int circumference;
        private long distanceFromStar;
        private CrustComposition crustComposition;
        private long seed;
        private double tectonicActivity;
        private double waterContent;
        private AtmosphereComposition atmosphereComposition;
        private int crustalThickness;
        private double atmosphericDensity;
        private double rotationPeriod;
        private String planetName;

        // Calculated parameters
        private double gravity = 0.0;
        private PlanetAge planetAge = PlanetAge.YOUNG;
        private double averageSurfaceTemp = 0.0;
        private double habitability = 0.0;
        private double waterErosion = 0.0;
        private double windErosion = 0.0;
        private String mainRockType = "minecraft:stone"; // Default to stone identifier

        // Adjusted parameters
        private double adjustedWaterContent = 0.0;
        private double adjustedAtmosphericDensity = 0.0;
        private double adjustedTectonicActivity = 0.0;

        public Builder setInputParameters(PlanetConfig config) {
            this.circumference = config.getCircumference();
            this.distanceFromStar = config.getDistanceFromStar();
            this.crustComposition = config.getCrustComposition();
            this.seed = config.getSeed();
            this.tectonicActivity = config.getTectonicActivity();
            this.waterContent = config.getWaterContent();
            this.atmosphereComposition = config.getAtmosphereComposition();
            this.crustalThickness = config.getCrustalThickness();
            this.atmosphericDensity = config.getAtmosphericDensity();
            this.rotationPeriod = config.getRotationPeriod();
            this.planetName = config.getPlanetName();

            // Initially, adjusted parameters match input parameters
            this.adjustedWaterContent = config.getWaterContent();
            this.adjustedAtmosphericDensity = config.getAtmosphericDensity();
            this.adjustedTectonicActivity = config.getTectonicActivity();

            return this;
        }

        public Builder setCalculatedParameters(double gravity, PlanetAge planetAge,
                                               double averageSurfaceTemp, double habitability,
                                               double waterErosion, double windErosion,
                                               String mainRockType,
                                               double adjustedWaterContent,
                                               double adjustedAtmosphericDensity,
                                               double adjustedTectonicActivity) {
            this.gravity = gravity;
            this.planetAge = planetAge;
            this.averageSurfaceTemp = averageSurfaceTemp;
            this.habitability = habitability;
            this.waterErosion = waterErosion;
            this.windErosion = windErosion;
            this.mainRockType = mainRockType;
            this.adjustedWaterContent = adjustedWaterContent;
            this.adjustedAtmosphericDensity = adjustedAtmosphericDensity;
            this.adjustedTectonicActivity = adjustedTectonicActivity;
            return this;
        }

        public Builder copyFrom(PlanetData other) {
            this.circumference = other.circumference;
            this.distanceFromStar = other.distanceFromStar;
            this.crustComposition = other.crustComposition;
            this.seed = other.seed;
            this.tectonicActivity = other.tectonicActivity;
            this.waterContent = other.waterContent;
            this.atmosphereComposition = other.atmosphereComposition;
            this.crustalThickness = other.crustalThickness;
            this.atmosphericDensity = other.atmosphericDensity;
            this.rotationPeriod = other.rotationPeriod;
            this.planetName = other.planetName;
            this.gravity = other.gravity;
            this.planetAge = other.planetAge;
            this.averageSurfaceTemp = other.averageSurfaceTemp;
            this.habitability = other.habitability;
            this.waterErosion = other.waterErosion;
            this.windErosion = other.windErosion;
            this.mainRockType = other.mainRockType;
            this.adjustedWaterContent = other.adjustedWaterContent;
            this.adjustedAtmosphericDensity = other.adjustedAtmosphericDensity;
            this.adjustedTectonicActivity = other.adjustedTectonicActivity;
            return this;
        }

        public PlanetData build() {
            return new PlanetData(this);
        }
    }
}