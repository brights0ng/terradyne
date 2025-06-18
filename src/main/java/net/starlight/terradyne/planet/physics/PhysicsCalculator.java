// PhysicsCalculator.java
package net.starlight.terradyne.planet.physics;

import net.minecraft.block.Block;

/**
 * Calculates derived planetary parameters and enforces physical constraints.
 * Handles the physics simulation and parameter validation to ensure realistic planet configurations.
 */
public class PhysicsCalculator {

    // Physical constants
    private static final double EARTH_GRAVITY = 9.81; // m/s²
    private static final double EARTH_CIRCUMFERENCE = 40075.0; // km
    private static final double ASTRONOMICAL_UNIT = 149597870.7; // km (Earth-Sun distance)
    private static final double STEFAN_BOLTZMANN = 5.67e-8; // Stefan-Boltzmann constant

    /**
     * Calculate all derived parameters from input configuration.
     * Also handles constraint validation and parameter adjustment.
     */
    public PlanetData calculateDerivedParameters(PlanetData inputData) {
        // First, handle constraint validation and create adjusted parameters
        ConstraintResult constraints = validateAndAdjustConstraints(inputData);

        // Calculate derived parameters using adjusted values
        double gravity = calculateGravity(inputData.getCircumference());
        PlanetData.PlanetAge planetAge = calculatePlanetAge(inputData.getCrustComposition(), constraints.adjustedTectonicActivity);
        double averageTemp = calculateAverageSurfaceTemp(
                inputData.getDistanceFromStar(),
                constraints.adjustedAtmosphericDensity,
                inputData.getAtmosphereComposition(),
                planetAge
        );
        double habitability = calculateHabitability(
                averageTemp,
                inputData.getAtmosphereComposition(),
                constraints.adjustedWaterContent,
                inputData.getCrustComposition(),
                constraints.adjustedAtmosphericDensity
        );
        double waterErosion = calculateWaterErosion(averageTemp, constraints.adjustedWaterContent, planetAge);
        double windErosion = calculateWindErosion(constraints.adjustedAtmosphericDensity, planetAge);
        String mainRockType = determineMainRockType(inputData.getCrustComposition());

        // Return new PlanetData with all calculated parameters
        return inputData.withCalculatedParameters(
                gravity, planetAge, averageTemp, habitability,
                waterErosion, windErosion, mainRockType,
                constraints.adjustedWaterContent,
                constraints.adjustedAtmosphericDensity,
                constraints.adjustedTectonicActivity
        );
    }

    /**
     * Validate constraints and adjust conflicting parameters
     */
    private ConstraintResult validateAndAdjustConstraints(PlanetData data) {
        double adjustedWater = data.getWaterContent();
        double adjustedAtmosphere = data.getAtmosphericDensity();
        double adjustedTectonic = data.getTectonicActivity();

        // HADEAN planets cannot have water (too hot/molten)
        if (data.getCrustComposition() == PlanetData.CrustComposition.HADEAN) {
            if (adjustedWater > 0.1) {
                System.out.println("WARNING: HADEAN planet cannot have significant water content. Adjusting to 0.0");
                adjustedWater = 0.0;
            }
            // HADEAN planets have high tectonic activity
            adjustedTectonic = Math.max(adjustedTectonic, 0.8);
        }

        // VACUUM atmosphere means no atmospheric density
        if (data.getAtmosphereComposition() == PlanetData.AtmosphereComposition.VACUUM) {
            if (adjustedAtmosphere > 0.1) {
                System.out.println("WARNING: VACUUM atmosphere cannot have significant density. Adjusting to 0.0");
                adjustedAtmosphere = 0.0;
            }
        }

        // REGOLITH composition implies old, weathered planet with low tectonic activity
        if (data.getCrustComposition() == PlanetData.CrustComposition.REGOLITH) {
            if (adjustedTectonic > 0.3) {
                System.out.println("WARNING: REGOLITH planets typically have low tectonic activity. Adjusting to 0.2");
                adjustedTectonic = 0.2;
            }
        }

        // Very thick atmospheres imply certain compositions
        if (adjustedAtmosphere > 0.8) {
            if (data.getAtmosphereComposition() == PlanetData.AtmosphereComposition.TRACE_ATMOSPHERE) {
                System.out.println("WARNING: TRACE_ATMOSPHERE inconsistent with high density. Adjusting density to 0.1");
                adjustedAtmosphere = 0.1;
            }
        }

        return new ConstraintResult(adjustedWater, adjustedAtmosphere, adjustedTectonic);
    }

    /**
     * Calculate surface gravity based on planet size
     * Assumes uniform density across all planets for simplicity
     */
    private double calculateGravity(int circumference) {
        // Gravity scales with planet radius (assuming constant density)
        double radiusRatio = (circumference / EARTH_CIRCUMFERENCE);
        return EARTH_GRAVITY * radiusRatio;
    }

    /**
     * Determine planet age based on crust composition and tectonic activity
     */
    private PlanetData.PlanetAge calculatePlanetAge(PlanetData.CrustComposition crust, double tectonicActivity) {
        switch (crust) {
            case REGOLITH:
                return PlanetData.PlanetAge.DEAD; // Heavily weathered = old and dead
            case HADEAN:
                return PlanetData.PlanetAge.INFANT; // Still forming
            default:
                if (tectonicActivity > 0.7) {
                    return PlanetData.PlanetAge.YOUNG;
                } else if (tectonicActivity > 0.2) {
                    return PlanetData.PlanetAge.OLD;
                } else {
                    return PlanetData.PlanetAge.DEAD;
                }
        }
    }

    /**
     * Calculate average surface temperature
     */
    private double calculateAverageSurfaceTemp(long distanceFromStar, double atmosphericDensity,
                                               PlanetData.AtmosphereComposition atmosphere,
                                               PlanetData.PlanetAge age) {
        // Base temperature from stellar distance (inverse square law)
        // distanceFromStar is in millions of km, so convert to AU
        double distanceAU = (distanceFromStar * 1_000_000.0) / ASTRONOMICAL_UNIT;
        double baseTemp = 279.0 / Math.sqrt(distanceAU); // Earth gets ~279K from Sun

        // Atmospheric greenhouse effect
        double greenhouseEffect = calculateGreenhouseEffect(atmosphere, atmosphericDensity);
        baseTemp += greenhouseEffect;

        // Age effects (young planets are hotter from formation heat)
        if (age == PlanetData.PlanetAge.INFANT) {
            baseTemp += 200.0; // Formation heat
        } else if (age == PlanetData.PlanetAge.YOUNG) {
            baseTemp += 50.0; // Some residual heat
        }

        // Convert to Celsius
        return baseTemp - 273.15;
    }

    /**
     * Calculate greenhouse effect contribution to temperature
     */
    private double calculateGreenhouseEffect(PlanetData.AtmosphereComposition atmosphere, double density) {
        double baseEffect = 0.0;

        switch (atmosphere) {
            case CARBON_DIOXIDE:
                baseEffect = 100.0; // Strong greenhouse gas
                break;
            case WATER_VAPOR_RICH:
                baseEffect = 80.0; // Very strong greenhouse
                break;
            case METHANE:
                baseEffect = 60.0; // Moderate greenhouse
                break;
            case OXYGEN_RICH:
            case NITROGEN_RICH:
                baseEffect = 15.0; // Weak greenhouse (like Earth)
                break;
            case NOBLE_GAS_MIXTURE:
                baseEffect = 5.0; // Very weak
                break;
            case HYDROGEN_SULFIDE:
                baseEffect = 25.0; // Moderate
                break;
            case TRACE_ATMOSPHERE:
                baseEffect = 2.0; // Minimal
                break;
            case VACUUM:
                baseEffect = 0.0; // No greenhouse effect
                break;
        }

        // Scale by atmospheric density
        return baseEffect * density;
    }

    /**
     * Calculate habitability index (0.0 to 1.0)
     */
    private double calculateHabitability(double temperature, PlanetData.AtmosphereComposition atmosphere,
                                         double waterContent, PlanetData.CrustComposition crust,
                                         double atmosphericDensity) {
        double habitability = 0.0;

        // Temperature range (optimal around 0-30°C)
        if (temperature >= -10 && temperature <= 40) {
            habitability += 0.4; // Base habitability for good temperature

            // Bonus for optimal temperature range
            if (temperature >= 0 && temperature <= 30) {
                habitability += 0.2;
            }
        } else if (temperature >= -50 && temperature <= 80) {
            habitability += 0.1; // Marginal temperature
        }

        // Water content (life needs water)
        habitability += waterContent * 0.3;

        // Atmosphere composition
        switch (atmosphere) {
            case OXYGEN_RICH:
                habitability += 0.2;
                break;
            case NITROGEN_RICH:
                habitability += 0.15;
                break;
            case CARBON_DIOXIDE:
                if (atmosphericDensity < 0.5) habitability += 0.05; // Thin CO2 okay
                break;
            case WATER_VAPOR_RICH:
                habitability += 0.1;
                break;
            case VACUUM:
            case HYDROGEN_SULFIDE:
                habitability = 0.0; // Lethal
                break;
            default:
                habitability += 0.02; // Minimal for other atmospheres
        }

        // Crust composition effects
        switch (crust) {
            case SILICATE:
                habitability += 0.1; // Earth-like
                break;
            case HADEAN:
            case METAL:
                habitability = Math.min(habitability, 0.1); // Hostile
                break;
            default:
                // No bonus or penalty
        }

        return Math.max(0.0, Math.min(1.0, habitability));
    }

    /**
     * Calculate water erosion factor
     */
    private double calculateWaterErosion(double temperature, double waterContent, PlanetData.PlanetAge age) {
        if (waterContent < 0.1) return 0.0;

        double erosion = waterContent * 0.5;

        // Temperature affects erosion rate (liquid water is most erosive)
        if (temperature >= 0 && temperature <= 100) {
            erosion *= 1.5; // Liquid water
        } else if (temperature < 0) {
            erosion *= 0.3; // Ice erosion is slower
        } else {
            erosion *= 0.1; // Steam/vapor erosion is minimal
        }

        // Age affects total erosion accumulated
        switch (age) {
            case INFANT:
                erosion *= 0.1;
                break;
            case YOUNG:
                erosion *= 0.5;
                break;
            case OLD:
                erosion *= 1.0;
                break;
            case DEAD:
                erosion *= 1.5; // Had lots of time
                break;
        }

        return Math.min(1.0, erosion);
    }

    /**
     * Calculate wind erosion factor
     */
    private double calculateWindErosion(double atmosphericDensity, PlanetData.PlanetAge age) {
        if (atmosphericDensity < 0.1) return 0.0;

        double erosion = atmosphericDensity * 0.6;

        // Age affects accumulation
        switch (age) {
            case INFANT:
                erosion *= 0.2;
                break;
            case YOUNG:
                erosion *= 0.7;
                break;
            case OLD:
                erosion *= 1.0;
                break;
            case DEAD:
                erosion *= 0.5; // May have lost atmosphere
                break;
        }

        return Math.min(1.0, erosion);
    }

    /**
     * Determine primary rock type based on crust composition
     */
    private String determineMainRockType(PlanetData.CrustComposition crust) {
        switch (crust) {
            case SILICATE:
                return "minecraft:stone";
            case FERROUS:
                return "minecraft:iron_ore";
            case BASALT:
                return "minecraft:basalt";
            case REGOLITH:
                return "minecraft:gravel";
            case HADEAN:
                return "minecraft:magma_block";
            case CARBON:
                return "minecraft:coal_block";
            case SULFUR:
                return "minecraft:yellow_terracotta"; // Closest to sulfur
            case HALIDE:
                return "minecraft:white_terracotta"; // Salt-like
            case METAL:
                return "minecraft:iron_block";
            default:
                return "minecraft:stone";
        }
    }

    /**
     * Helper class to hold constraint adjustment results
     */
    private static class ConstraintResult {
        final double adjustedWaterContent;
        final double adjustedAtmosphericDensity;
        final double adjustedTectonicActivity;

        ConstraintResult(double water, double atmosphere, double tectonic) {
            this.adjustedWaterContent = water;
            this.adjustedAtmosphericDensity = atmosphere;
            this.adjustedTectonicActivity = tectonic;
        }
    }
}