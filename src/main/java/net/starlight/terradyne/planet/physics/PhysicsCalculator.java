package net.starlight.terradyne.planet.physics;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.starlight.terradyne.Terradyne;

import java.util.HashMap;
import java.util.Map;

/**
 * Calculates realistic planet physics and enforces physical constraints
 * Implements simplified but plausible physics models
 */
public class PhysicsCalculator {

    /**
     * Calculate all derived planet properties from configuration
     * Enforces physical constraints and overrides conflicting parameters
     */
    public static PlanetData calculatePlanetData(PlanetConfig config) {
        Terradyne.LOGGER.info("Calculating physics for planet: {}", config.getPlanetName());
        
        // === PHYSICS CONSTRAINT ENFORCEMENT ===
        PlanetConfig correctedConfig = enforcePhysicsConstraints(config);
        
        // === CORE PHYSICS CALCULATIONS ===
        double gravity = calculateGravity(correctedConfig);
        double temperature = calculateTemperature(correctedConfig);
        PlanetAge age = calculatePlanetAge(correctedConfig);
        
        // === DERIVED PROPERTIES ===
        double habitability = calculateHabitability(correctedConfig, temperature, gravity);
        double waterErosion = calculateWaterErosion(correctedConfig, temperature);
        double windErosion = calculateWindErosion(correctedConfig);
        Block mainRockType = determineMainRockType(correctedConfig);
        int seaLevel = calculateSeaLevel(correctedConfig);
        double volcanism = calculateVolcanism(correctedConfig, age);
        double glacialCoverage = calculateGlacialCoverage(correctedConfig, temperature);
        
        // === TERRAIN GENERATION FACTORS ===
        double continentalScale = calculateContinentalScale(correctedConfig);
        double mountainScale = calculateMountainScale(correctedConfig, age);
        double erosionScale = calculateErosionScale(waterErosion, windErosion);
        
        // === CLIMATE FACTORS ===
        Map<String, Double> climateFactors = calculateClimateFactors(correctedConfig, temperature);
        
        return new PlanetData(
            gravity, age, temperature, habitability, waterErosion, windErosion,
            mainRockType, seaLevel, volcanism, glacialCoverage,
            correctedConfig.getAtmosphericDensity(), // Actual values after correction
            correctedConfig.getWaterContent(),
            correctedConfig.getTectonicActivity(),
            climateFactors, continentalScale, mountainScale, erosionScale
        );
    }

    // === PARAMETER CONSTRAINTS (moved from PlanetConfig) ===
    private static final int MIN_CIRCUMFERENCE = 1000;      // Small asteroid
    private static final int MAX_CIRCUMFERENCE = 100000;    // Super-Earth
    private static final long MIN_STAR_DISTANCE = 30;       // Very close (Mercury-like)
    private static final long MAX_STAR_DISTANCE = 600;      // Far (Jupiter-like)
    private static final double MIN_CRUSTAL_THICKNESS = 2.0;  // Very thin crust
    private static final double MAX_CRUSTAL_THICKNESS = 70.0; // Very thick crust
    private static final double MIN_ROTATION_PERIOD = 0.1;   // Very fast rotation
    private static final double MAX_ROTATION_PERIOD = 20.0;  // Very slow rotation
    private static final double MIN_NOISE_SCALE = 0.0001;    // Large tectonic plates
    private static final double MAX_NOISE_SCALE = 0.01;      // Small tectonic plates

    /**
     * Enforce physical constraints and override conflicting parameters
     * Includes both validation and physics-based corrections
     */
    private static PlanetConfig enforcePhysicsConstraints(PlanetConfig config) {
        // === STEP 1: PARAMETER VALIDATION (clamp to reasonable ranges) ===
        PlanetConfig corrected = new PlanetConfig(config.getPlanetName(), config.getSeed())
                .setCircumference(clamp(config.getCircumference(), MIN_CIRCUMFERENCE, MAX_CIRCUMFERENCE))
                .setDistanceFromStar(clamp(config.getDistanceFromStar(), MIN_STAR_DISTANCE, MAX_STAR_DISTANCE))
                .setCrustComposition(config.getCrustComposition())
                .setAtmosphereComposition(config.getAtmosphereComposition())
                .setTectonicActivity(clamp(config.getTectonicActivity(), 0.0, 1.0))
                .setWaterContent(clamp(config.getWaterContent(), 0.0, 1.0))
                .setCrustalThickness(clamp(config.getCrustalThickness(), MIN_CRUSTAL_THICKNESS, MAX_CRUSTAL_THICKNESS))
                .setAtmosphericDensity(clamp(config.getAtmosphericDensity(), 0.0, 1.0))
                .setRotationPeriod(clamp(config.getRotationPeriod(), MIN_ROTATION_PERIOD, MAX_ROTATION_PERIOD))
                .setNoiseScale(clamp(config.getNoiseScale(), MIN_NOISE_SCALE, MAX_NOISE_SCALE));

        // Log parameter clamping warnings
        logClampingWarnings(config, corrected);

        // === STEP 2: PHYSICS-BASED CONSTRAINTS ===
        double estimatedTemp = calculateTemperature(corrected);
        
        // If too hot for liquid water, reduce water content
        if (estimatedTemp > 100 && corrected.getWaterContent() > 0.1) {
            corrected.setWaterContent(Math.max(0.0, corrected.getWaterContent() - 0.7));
            Terradyne.LOGGER.warn("Water content reduced due to high temperature ({}°C)", estimatedTemp);
        }
        
        // If too cold for liquid water, reduce erosion
        if (estimatedTemp < -20 && corrected.getWaterContent() > 0.5) {
            corrected.setWaterContent(corrected.getWaterContent() * 0.3); // Most water frozen
            Terradyne.LOGGER.warn("Water content reduced due to low temperature ({}°C)", estimatedTemp);
        }

        // === ATMOSPHERE-BASED CONSTRAINTS ===
        
        // Vacuum atmosphere forces atmospheric density to zero
        if (corrected.getAtmosphereComposition() == AtmosphereComposition.VACUUM) {
            corrected.setAtmosphericDensity(0.0);
            Terradyne.LOGGER.warn("Atmospheric density set to 0 for vacuum atmosphere");
        }
        
        // Very small planets can't retain thick atmospheres
        if (corrected.getCircumference() < 3000 && corrected.getAtmosphericDensity() > 0.3) {
            corrected.setAtmosphericDensity(0.1);
            Terradyne.LOGGER.warn("Atmospheric density reduced for small planet ({}km)", corrected.getCircumference());
        }

        // === COMPOSITION-BASED CONSTRAINTS ===
        
        // Ice-rich planets must be cold
//        if (corrected.getCrustComposition() == CrustComposition.BASALTIC && estimatedTemp > 0) { // This section uses outdated crust types
//            // Can't easily change temperature, so warn but allow
//            Terradyne.LOGGER.warn("Ice-rich crust may not be stable at {}°C", estimatedTemp);
//        }

        return corrected;
    }

    /**
     * Log warnings for parameter clamping
     */
    private static void logClampingWarnings(PlanetConfig original, PlanetConfig clamped) {
        if (original.getCircumference() != clamped.getCircumference()) {
            Terradyne.LOGGER.warn("Circumference clamped from {} to {} km", 
                                original.getCircumference(), clamped.getCircumference());
        }
        if (original.getDistanceFromStar() != clamped.getDistanceFromStar()) {
            Terradyne.LOGGER.warn("Star distance clamped from {} to {} Mkm", 
                                original.getDistanceFromStar(), clamped.getDistanceFromStar());
        }
        if (Math.abs(original.getCrustalThickness() - clamped.getCrustalThickness()) > 0.01) {
            Terradyne.LOGGER.warn("Crustal thickness clamped from {:.1f} to {:.1f} km", 
                                original.getCrustalThickness(), clamped.getCrustalThickness());
        }
        if (Math.abs(original.getRotationPeriod() - clamped.getRotationPeriod()) > 0.01) {
            Terradyne.LOGGER.warn("Rotation period clamped from {:.2f} to {:.2f} days", 
                                original.getRotationPeriod(), clamped.getRotationPeriod());
        }
        if (Math.abs(original.getNoiseScale() - clamped.getNoiseScale()) > 0.0001) {
            Terradyne.LOGGER.warn("Noise scale clamped from {:.4f} to {:.4f}", 
                                original.getNoiseScale(), clamped.getNoiseScale());
        }
    }

    // === UTILITY METHODS (moved from PlanetConfig) ===
    
    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Calculate surface gravity based on planet size
     * Simplified: larger planets = more gravity (roughly)
     */
    private static double calculateGravity(PlanetConfig config) {
        // Earth reference: 40,000 km = 1.0g
        double baseGravity = (double) config.getCircumference() / 40000.0;
        
        // Crust composition affects density
        double densityModifier = switch (config.getCrustComposition()) {
            case METALLIC -> 1.4;        // Much denser
            case CARBONACEOUS -> 0.6;      // Diamond is less dense than rock
            case REGOLITHIC -> 0.7;         // REGOLITHIC is less dense
            case HALLIDE -> 1.1;        // Slightly denser
            default -> 1.0;               // Standard rocky density
        };
        
        return Math.max(0.1, Math.min(3.0, baseGravity * densityModifier));
    }

    /**
     * Calculate average surface temperature
     * FIXED: Enhanced greenhouse effect for Venus-like conditions
     */
    private static double calculateTemperature(PlanetConfig config) {
        // Base temperature from star distance (Earth reference: 150M km = 15°C)
        double baseTemp = 15.0 - (config.getDistanceFromStar() - 150) * 0.12;

        // Enhanced atmospheric greenhouse effect
        double greenhouse = 0.0;
        if (config.getAtmosphericDensity() > 0.1) {
            greenhouse = switch (config.getAtmosphereComposition()) {
                case CARBON_DIOXIDE -> {
                    // FIXED: Much stronger greenhouse for Venus-like conditions
                    double baseCO2Effect = config.getAtmosphericDensity() * 120.0; // Increased from 80.0
                    // Extra greenhouse for very dense CO2 atmospheres (Venus effect)
                    if (config.getAtmosphericDensity() > 0.8) {
                        baseCO2Effect += (config.getAtmosphericDensity() - 0.8) * 300.0; // Runaway greenhouse
                    }
                    yield baseCO2Effect;
                }
                case WATER_VAPOR_RICH -> {
                    double baseWaterEffect = config.getAtmosphericDensity() * 80.0;
                    // Steam greenhouse runaway effect
                    if (config.getAtmosphericDensity() > 0.7) {
                        baseWaterEffect += (config.getAtmosphericDensity() - 0.7) * 200.0;
                    }
                    yield baseWaterEffect;
                }
                case METHANE -> config.getAtmosphericDensity() * 50.0;           // Strong but not as extreme
                case OXYGEN_RICH, NITROGEN_RICH -> config.getAtmosphericDensity() * 25.0; // Mild greenhouse
                case NOBLE_GAS_MIXTURE -> config.getAtmosphericDensity() * 15.0; // Minimal greenhouse
                case HYDROGEN_SULFIDE -> config.getAtmosphericDensity() * 40.0;  // Moderate greenhouse + toxic
                case TRACE_ATMOSPHERE -> config.getAtmosphericDensity() * 5.0;   // Very weak
                case VACUUM -> 0.0;                                              // No greenhouse
            };
        }

        // Crust composition affects albedo (reflectivity)
        double albedoEffect = switch (config.getCrustComposition()) {
            case SULFURIC -> 8.0;        // Sulfur compounds = warmer
            case CARBONACEOUS -> 12.0;   // Low reflectivity = warmer
            case METALLIC -> 10.0;       // Dark metal = warmer
            case HADEAN -> 15.0;         // Active volcanism = much warmer
            case HALLIDE -> -12.0;       // White salt = cooler
            case REGOLITHIC -> -5.0;     // Rocky rubble = slightly cooler
            default -> 0.0;
        };

        double finalTemp = baseTemp + greenhouse + albedoEffect;

        // Log temperature calculation for debugging
        Terradyne.LOGGER.debug("Temperature calculation for {}: base={:.1f}°C + greenhouse={:.1f}°C + albedo={:.1f}°C = {:.1f}°C",
                config.getPlanetName(), baseTemp, greenhouse, albedoEffect, finalTemp);

        return finalTemp;
    }

    /**
     * Calculate habitability score
     * FIXED: Much stricter criteria for extreme conditions
     */
    private static double calculateHabitability(PlanetConfig config, double temperature, double gravity) {
        double habitability = 0.0;

        // FIXED: Stricter temperature range for habitability
        if (temperature > 0 && temperature < 40) {          // Narrowed from 50°C
            habitability += 0.4;
        } else if (temperature > -10 && temperature < 60) { // Narrowed from -20 to 80°C
            habitability += 0.1;                             // Reduced from 0.2
        }
        // FIXED: Extreme temperatures are completely uninhabitable
        if (temperature > 100 || temperature < -50) {
            habitability = 0.0; // Override any other factors
        }

        // FIXED: Water requirement is more strict
        if (config.getWaterContent() > 0.5) {               // Increased from 0.3
            habitability += 0.3;
        } else if (config.getWaterContent() > 0.2) {         // Increased from 0.1
            habitability += 0.1;
        }
        // FIXED: No water = major habitability penalty
        if (config.getWaterContent() < 0.05) {
            habitability *= 0.2; // 80% penalty for no water
        }

        // Reasonable gravity (unchanged)
        if (gravity > 0.3 && gravity < 2.0) {
            habitability += 0.2;
        }

        // FIXED: Atmosphere requirements are stricter
        if (config.getAtmosphereComposition() == AtmosphereComposition.OXYGEN_RICH &&
                config.getAtmosphericDensity() > 0.3) {
            habitability += 0.2; // Only oxygen-rich gets bonus
        } else if (config.getAtmosphereComposition() == AtmosphereComposition.NITROGEN_RICH &&
                config.getAtmosphericDensity() > 0.3) {
            habitability += 0.1; // Nitrogen is okay but not great
        }
        // FIXED: Toxic atmospheres are uninhabitable
        if (config.getAtmosphereComposition() == AtmosphereComposition.HYDROGEN_SULFIDE ||
                config.getAtmosphereComposition() == AtmosphereComposition.CARBON_DIOXIDE ||
                config.getAtmosphereComposition() == AtmosphereComposition.VACUUM) {
            habitability = Math.min(habitability, 0.1); // Cap at 10% for toxic/no atmosphere
        }

        double finalHabitability = Math.max(0.0, Math.min(1.0, habitability));

        // Log habitability calculation for debugging
        Terradyne.LOGGER.debug("Habitability calculation for {}: temp={:.1f}°C, water={:.2f}, atm={}, result={:.2f}",
                config.getPlanetName(), temperature, config.getWaterContent(),
                config.getAtmosphereComposition().getDisplayName(), finalHabitability);

        return finalHabitability;
    }

    /**
     * Determine planet age based on multiple factors
     */
    private static PlanetAge calculatePlanetAge(PlanetConfig config) {
        // Young planets: high tectonic activity + hot temperatures
        if (config.getTectonicActivity() > 0.8) {
            return PlanetAge.YOUNG;
        }
        
        // Dead planets: no tectonic activity + extreme conditions
        if (config.getTectonicActivity() < 0.1) {
            return PlanetAge.DEAD;
        }
        
        // Infant planets: extreme star distance or unusual composition
        double temp = calculateTemperature(config);
        if (temp > 200 || temp < -100) {
            return PlanetAge.INFANT; // Still forming/extreme conditions
        }
        
        // Old planets: low-moderate tectonic activity
        if (config.getTectonicActivity() < 0.4) {
            return PlanetAge.OLD;
        }
        
        // Default: mature
        return PlanetAge.MATURE;
    }

    /**
     * Calculate water erosion factor
     */
    private static double calculateWaterErosion(PlanetConfig config, double temperature) {
        if (config.getWaterContent() < 0.1) return 0.0;
        
        double erosion = config.getWaterContent() * 0.8;
        
        // Temperature affects erosion rate
        if (temperature > 10 && temperature < 30) {
            erosion *= 1.2; // Optimal temperature for water erosion
        } else if (temperature < 0) {
            erosion *= 0.3; // Ice reduces liquid water erosion
        }
        
        return Math.max(0.0, Math.min(1.0, erosion));
    }

    /**
     * Calculate wind erosion factor
     */
    private static double calculateWindErosion(PlanetConfig config) {
        double erosion = config.getAtmosphericDensity() * 0.6;
        
        // Atmosphere composition affects erosion
        erosion *= switch (config.getAtmosphereComposition()) {
            case CARBON_DIOXIDE, WATER_VAPOR_RICH -> 1.3; // Dense, erosive atmospheres
            case VACUUM, TRACE_ATMOSPHERE -> 0.0;         // No wind erosion
            default -> 1.0;
        };
        
        return Math.max(0.0, Math.min(1.0, erosion));
    }

    /**
     * Determine main rock type from crust composition
     */
    private static Block determineMainRockType(PlanetConfig config) {
        return switch (config.getCrustComposition()) {
            case SILICATE -> Blocks.STONE;
            case FERROUS -> Blocks.RED_SANDSTONE;
            case CARBONACEOUS -> Blocks.COAL_BLOCK;
            case REGOLITHIC -> Blocks.ANDESITE;
            case SULFURIC -> Blocks.YELLOW_TERRACOTTA; // Approximation
            case HALLIDE -> Blocks.WHITE_CONCRETE_POWDER;    // Approximation
            case BASALTIC -> Blocks.BASALT;
            case METALLIC -> Blocks.DEEPSLATE_IRON_ORE;
            case HADEAN -> Blocks.MAGMA_BLOCK;
        };
    }

    /**
     * Calculate sea level based on water content and crust thickness
     * Updated for 0-256 height range
     */
    private static int calculateSeaLevel(PlanetConfig config) {
        int baseSeaLevel = 128; // Middle of 0-256 range (was 63 for -64-319 range)

        // Crustal thickness affects elevation
        int elevationOffset = (int) ((config.getCrustalThickness() - 35.0) * 0.5);

        // Water content affects sea level
        int waterOffset = (int) ((config.getWaterContent() - 0.5) * 20);

        // Clamp to valid height range for 0-256 world
        return Math.max(10, Math.min(200, baseSeaLevel + elevationOffset + waterOffset));
    }

    /**
     * Calculate volcanism level
     */
    private static double calculateVolcanism(PlanetConfig config, PlanetAge age) {
        double volcanism = config.getTectonicActivity() * 0.8;
        
        // Age affects volcanism
        volcanism *= switch (age) {
            case INFANT, YOUNG -> 1.5;
            case MATURE -> 1.0;
            case OLD -> 0.5;
            case DEAD -> 0.1;
        };
        
        return Math.max(0.0, Math.min(1.0, volcanism));
    }

    /**
     * Calculate glacial coverage
     */
    private static double calculateGlacialCoverage(PlanetConfig config, double temperature) {
        if (temperature > 10) return 0.0;
        
        double coverage = 0.0;
        if (temperature < -10) {
            coverage = 0.8;
        } else if (temperature < 0) {
            coverage = 0.3;
        }
        
        // Water content affects ice formation
        coverage *= config.getWaterContent();
        
        return Math.max(0.0, Math.min(1.0, coverage));
    }

    /**
     * Calculate continental scale factor
     */
    private static double calculateContinentalScale(PlanetConfig config) {
        // Larger planets = larger continental features
        return Math.max(0.5, Math.min(2.0, config.getCircumference() / 40000.0));
    }

    /**
     * Calculate mountain scale factor
     */
    private static double calculateMountainScale(PlanetConfig config, PlanetAge age) {
        double scale = config.getTectonicActivity() * 1.5;
        
        // Age affects mountain height
        scale *= switch (age) {
            case INFANT, YOUNG -> 1.3;
            case MATURE -> 1.0;
            case OLD -> 0.7;
            case DEAD -> 0.3;
        };
        
        return Math.max(0.2, Math.min(3.0, scale));
    }

    /**
     * Calculate erosion scale factor
     */
    private static double calculateErosionScale(double waterErosion, double windErosion) {
        return Math.max(0.1, Math.min(2.0, (waterErosion + windErosion) * 1.2));
    }

    /**
     * Calculate climate factor modifiers
     */
    private static Map<String, Double> calculateClimateFactors(PlanetConfig config, double temperature) {
        Map<String, Double> factors = new HashMap<>();
        
        factors.put("temperatureVariation", config.getRotationPeriod() * 0.3);
        factors.put("seasonalStrength", Math.abs(temperature) * 0.02);
        factors.put("stormIntensity", config.getAtmosphericDensity() * 0.8);
        factors.put("weatherStability", 1.0 - config.getTectonicActivity() * 0.4);
        
        return factors;
    }
}