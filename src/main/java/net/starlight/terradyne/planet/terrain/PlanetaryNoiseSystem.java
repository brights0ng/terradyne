package net.starlight.terradyne.planet.terrain;

import net.minecraft.util.math.noise.SimplexNoiseSampler;
import net.minecraft.util.math.random.Random;
import net.starlight.terradyne.planet.mapping.TectonicVolatilityManager;
import net.starlight.terradyne.planet.physics.PlanetConfig;
import net.starlight.terradyne.planet.physics.PlanetData;
import net.starlight.terradyne.Terradyne;

/**
 * Unified noise generation system with 5 specialized maps
 * UPDATED: Now includes volatility system for geological activity
 * Each map has layers that blend using different techniques
 * Maps influence each other through cross-map effects
 */
public class PlanetaryNoiseSystem {

    private final PlanetConfig config;
    private final PlanetData planetData;

    // === MASTER NOISE SAMPLER ===
    private final SimplexNoiseSampler masterNoise;

    // === SPECIALIZED NOISE MAPS ===
    private final TerrainNoiseMap terrainMap;
    private final TectonicNoiseMap tectonicMap;
    private final TemperatureNoiseMap temperatureMap;    // Phase 5
    private final WindNoiseMap windMap;                  // Phase 5
    private final MoistureNoiseMap moistureMap;          // Phase 5
    private final BiomeNoiseMap biomeMap;                // Phase 7

    // === PHASE 2: VOLATILITY SYSTEM ===
    private final TectonicVolatilityManager volatilityManager;

    /**
     * Create planetary noise system from planet model data
     * UPDATED: Now includes volatility manager initialization
     */
    public PlanetaryNoiseSystem(PlanetConfig config, PlanetData planetData) {
        this.config = config;
        this.planetData = planetData;

        Terradyne.LOGGER.info("Initializing Planetary Noise System for {}", config.getPlanetName());

        // Create master noise sampler - all terrain uses this base
        this.masterNoise = new SimplexNoiseSampler(Random.create(config.getSeed()));

        // === PHASE C: CORE TERRAIN MAPS ===
        this.tectonicMap = new TectonicNoiseMap(config, planetData, masterNoise);
        this.volatilityManager = new TectonicVolatilityManager(config, planetData, masterNoise);
        this.terrainMap = new TerrainNoiseMap(config, planetData, masterNoise, tectonicMap, volatilityManager);

        // === PHASE 5: CLIMATE MAPS ===
        this.temperatureMap = new TemperatureNoiseMap(config, planetData, masterNoise, terrainMap);
        this.windMap = new WindNoiseMap(config, planetData, masterNoise, terrainMap, this.temperatureMap);
        this.moistureMap = new MoistureNoiseMap(config, planetData, masterNoise, terrainMap, this.windMap);
        this.biomeMap = null;       // Phase 7

        Terradyne.LOGGER.info("✅ Planetary Noise System initialized");
        logNoiseConfiguration();
    }

    // === PRIMARY TERRAIN API ===

    /**
     * Sample terrain height at world coordinates
     * This is the main method called by chunk generators
     */
    public double sampleTerrainHeight(int worldX, int worldZ) {
        return terrainMap.sample(worldX, worldZ);
    }

    /**
     * Sample tectonic activity at world coordinates
     * Used for geological features and terrain modification
     */
    public double sampleTectonicActivity(int worldX, int worldZ) {
        return tectonicMap.sample(worldX, worldZ);
    }

    // === WIND API ===

    /**
     * Sample wind speed (0.0-1.0) at world coordinates
     * Based on terrain elevation gradients and atmospheric density
     */
    public double sampleWindSpeed(int worldX, int worldZ) {
        if (windMap != null) {
            return windMap.sampleSpeed(worldX, worldZ);
        }

        // FALLBACK: Simple atmospheric density-based wind
        return planetData.getActualAtmosphericDensity() * 0.3;
    }

    /**
     * Sample wind direction at world coordinates
     * Returns [directionX, directionZ] where direction points toward higher elevation
     */
    public double[] sampleWindDirection(int worldX, int worldZ) {
        if (windMap != null) {
            return windMap.sampleDirection(worldX, worldZ);
        }

        // FALLBACK: No specific direction
        return new double[]{0.0, 0.0};
    }

    /**
     * Sample both wind speed and direction as combined vector
     * Returns [speedX, speedZ] where magnitude is speed, direction is flow direction
     */
    public double[] sampleWindVector(int worldX, int worldZ) {
        if (windMap != null) {
            return windMap.sampleVector(worldX, worldZ);
        }

        // FALLBACK: No wind
        return new double[]{0.0, 0.0};
    }

    // === VOLATILITY API ===

    /**
     * Get geological volatility level (0-5) at world coordinates
     */
    public int sampleVolatility(int worldX, int worldZ) {
        return volatilityManager.getVolatilityAt(worldX, worldZ);
    }

    /**
     * Get volatility for a chunk (convenience method)
     */
    public int sampleChunkVolatility(int chunkX, int chunkZ) {
        return volatilityManager.getChunkVolatility(chunkX, chunkZ);
    }

    // === FUTURE CLIMATE API (Phase 5) ===

    /**
     * Sample temperature at world coordinates
     * Uses realistic physics: base temp + latitude + elevation + noise variation
     */
    public double sampleTemperature(int worldX, int worldZ) {
        if (temperatureMap != null) {
            return temperatureMap.sample(worldX, worldZ);
        }

        // FALLBACK: Should not happen now, but kept for safety
        double latitude = Math.abs(worldZ) / (config.getCircumference() * 0.25); // 0-1 from equator to pole
        double baseTemp = planetData.getAverageSurfaceTemp();
        double latitudeEffect = -latitude * 40.0; // -40°C from equator to pole

        return baseTemp + latitudeEffect;
    }

    /**
     * Sample moisture at world coordinates
     * Uses wind transport and distance from water for realistic humidity patterns
     */
    public double sampleMoisture(int worldX, int worldZ) {
        if (moistureMap != null) {
            return moistureMap.sample(worldX, worldZ);
        }

        // FALLBACK: Should not happen now, but kept for safety
        return planetData.getActualWaterContent();
    }

    /**
     * Sample biome classification at world coordinates
     * PLACEHOLDER: Will be implemented in Phase 7
     */
    public double sampleBiome(int worldX, int worldZ) {
        if (biomeMap != null) {
            return biomeMap.sample(worldX, worldZ);
        }

        // PLACEHOLDER: Simple classification based on temp/moisture
        double temp = sampleTemperature(worldX, worldZ);
        double moisture = sampleMoisture(worldX, worldZ);

        // Return simple biome index (will be properly classified later)
        if (temp < 0) return 0.0; // Cold
        if (moisture < 0.3) return 1.0; // Desert
        if (temp > 25) return 2.0; // Tropical
        return 3.0; // Temperate
    }

    // === NOISE MAP ACCESS ===

    public TerrainNoiseMap getTerrainMap() { return terrainMap; }
    public TectonicNoiseMap getTectonicMap() { return tectonicMap; }
    public TectonicVolatilityManager getVolatilityManager() { return volatilityManager; }
    public SimplexNoiseSampler getMasterNoise() { return masterNoise; }

    // === DIAGNOSTICS ===

    /**
     * Get noise system status for debugging
     */
    public String getSystemStatus() {
        return String.format("PlanetaryNoiseSystem{planet=%s, terrain=%s, tectonic=%s, volatility=%s, temperature=%s, wind=%s, humidity=%s}",
                config.getPlanetName(),
                terrainMap != null ? "ACTIVE" : "NULL",
                tectonicMap != null ? "ACTIVE" : "NULL",
                volatilityManager != null ? "ACTIVE" : "NULL",
                temperatureMap != null ? "ACTIVE" : "NULL",
                windMap != null ? "ACTIVE" : "NULL",
                moistureMap != null ? "ACTIVE" : "NULL");
    }

    /**
     * Sample all maps at coordinates for debugging
     */
    public String sampleAllMaps(int worldX, int worldZ) {
        double[] windVector = sampleWindVector(worldX, worldZ);
        double windSpeed = Math.sqrt(windVector[0] * windVector[0] + windVector[1] * windVector[1]);

        return String.format("NoiseDebug{x=%d,z=%d: terrain=%.2f, tectonic=%.2f, volatility=%d, temp=%.1f°C, wind=%.2f, humidity=%.2f}",
                worldX, worldZ,
                sampleTerrainHeight(worldX, worldZ),
                sampleTectonicActivity(worldX, worldZ),
                sampleVolatility(worldX, worldZ),
                sampleTemperature(worldX, worldZ),
                windSpeed,
                sampleMoisture(worldX, worldZ));
    }

    private void logNoiseConfiguration() {
        Terradyne.LOGGER.info("Noise Maps Configured:");
        Terradyne.LOGGER.info("  Terrain: {} layers (fractal continental coastlines, smooth volatility-based mountains)", terrainMap.getLayerCount());
        Terradyne.LOGGER.info("  Tectonic: {} layers", tectonicMap.getLayerCount());
        Terradyne.LOGGER.info("  Volatility: {} plates, avg size {:.0f} blocks (drives mountain placement with smooth transitions)",
                volatilityManager.getPlateCount(), volatilityManager.getAveragePlateSize());
        Terradyne.LOGGER.info("  Temperature: {} layers (Base + Latitude + Elevation + Variation)",
                temperatureMap != null ? temperatureMap.getLayerCount() : 0);
        Terradyne.LOGGER.info("  Wind: {} layers (Base + Terrain Gradients + Temperature Gradients + Variation)",
                windMap != null ? windMap.getLayerCount() : 0);
        Terradyne.LOGGER.info("  Humidity: {} layers (Base + Wind Transport + Distance from Water + Variation)",
                moistureMap != null ? moistureMap.getLayerCount() : 0);
        Terradyne.LOGGER.info("  Biome: PLACEHOLDER (Phase 7)");
    }
}

/**
 * Base class for specialized noise maps
 * Each map handles its own layers and blending
 */
abstract class NoiseMap {
    protected final PlanetConfig config;
    protected final PlanetData planetData;
    protected final SimplexNoiseSampler masterNoise;

    public NoiseMap(PlanetConfig config, PlanetData planetData, SimplexNoiseSampler masterNoise) {
        this.config = config;
        this.planetData = planetData;
        this.masterNoise = masterNoise;
    }

    /**
     * Sample this noise map at world coordinates
     */
    public abstract double sample(int worldX, int worldZ);

    /**
     * Get number of layers in this map
     */
    public abstract int getLayerCount();
}

/**
 * TERRAIN NOISE MAP - Primary height generation
 * Layers: Continental (fractal coastlines), Mountain (smooth volatility-based), Valley, Erosion, Surface Detail
 * Influenced By: Tectonic (through domain warping), Volatility (mountain placement)
 * Blending: Domain warping + overlay + SMOOTH TRANSITIONS to eliminate jarring edges
 *
 * COASTLINE REALISM:
 * - Continental layer uses 4 octaves for fractal coastlines (1x, 3x, 9x, 27x frequency)
 * - Creates natural bays, peninsulas, fjords, and irregular coastlines
 * - Eliminates obvious noise-blob continent shapes
 *
 * SMOOTHING FIXES:
 * - Mountain intensity uses smoothstep() instead of discrete volatility levels
 * - Valley flattening uses smooth thresholds instead of hard cutoffs
 * - Domain warping reduced to prevent discontinuities
 * - All transitions use S-curve smoothing for natural blending
 */
class TerrainNoiseMap extends NoiseMap {

    private final TectonicNoiseMap tectonicInfluence;
    private final TectonicVolatilityManager volatilityManager;

    public TerrainNoiseMap(PlanetConfig config, PlanetData planetData, SimplexNoiseSampler masterNoise,
                           TectonicNoiseMap tectonicMap, TectonicVolatilityManager volatilityManager) {
        super(config, planetData, masterNoise);
        this.tectonicInfluence = tectonicMap;
        this.volatilityManager = volatilityManager;
    }

    @Override
    public double sample(int worldX, int worldZ) {
        // === TECTONIC DOMAIN WARPING - REDUCED for smoother terrain ===
        double tectonicOffset = tectonicInfluence.sample(worldX, worldZ) * planetData.getMountainScale() * 15.0; // Reduced from 25.0
        double warpedX = worldX + tectonicOffset * 0.2; // Reduced from 0.3
        double warpedZ = worldZ + tectonicOffset * 0.5; // Reduced from 0.7

        // === CONTINENTAL LAYER - ENHANCED with fractal coastlines ===
        double continentalFreq = config.getNoiseScale() * 0.2;

        // Base continental shape (largest scale)
        double continental1 = masterNoise.sample(warpedX * continentalFreq, 0, warpedZ * continentalFreq);

        // Add coastal detail octaves for fractal coastlines
        double coastal2 = masterNoise.sample(warpedX * continentalFreq * 3, 7, warpedZ * continentalFreq * 3);
        double coastal3 = masterNoise.sample(warpedX * continentalFreq * 9, 8, warpedZ * continentalFreq * 9);
        double coastal4 = masterNoise.sample(warpedX * continentalFreq * 27, 9, warpedZ * continentalFreq * 27);

        // Combine with decreasing amplitude for fractal detail
        double continental = continental1 +
                coastal2 * 0.3 +
                coastal3 * 0.1 +
                coastal4 * 0.03;

        continental *= planetData.getContinentalScale() * 60.0;

        // === MOUNTAIN LAYER - VOLATILITY-BASED with SMOOTH transitions ===
        double mountainFreq = config.getNoiseScale() * 1.2;
        double mountainNoise = masterNoise.sample(warpedX * mountainFreq, 1, warpedZ * mountainFreq);
        mountainNoise = Math.abs(mountainNoise); // Ridge noise for mountain ranges

        // Sample volatility to determine mountain placement (mountains form at plate boundaries!)
        int volatility = volatilityManager.getVolatilityAt(worldX, worldZ);

        // SMOOTH mountain intensity based on volatility (0-5 scale) - NO HARD BOUNDARIES
        double mountainIntensity = Math.max(0.0, (volatility - 1.0) / 4.0); // Smooth 0.0-1.0 scale from volatility 1-5
        mountainIntensity = smoothstep(mountainIntensity); // Apply smoothing function for even gentler transitions

        // Apply mountain noise with smoothed volatility-based intensity
        double mountains = mountainNoise * mountainIntensity * planetData.getMountainScale() * 50.0;

        // === VALLEY LAYER - REDUCED frequency for fewer, larger valleys ===
        double valleyFreq = config.getNoiseScale() * 0.8; // Reduced from 1.5 for larger valleys
        double valleyNoise = masterNoise.sample(warpedX * valleyFreq, 2, warpedZ * valleyFreq);

        // Calculate valley center strength (0.0 = valley wall, 1.0 = valley center)
        double valleyStrength = Math.abs(valleyNoise);
        double valleyCenter = Math.max(0, (valleyStrength - 0.3) / 0.7); // 0-1 scale

        // Traditional valley carving for walls - REDUCED amplitude
        double valleys = valleyStrength * -1.0; // Negative for carving
        valleys *= planetData.getErosionScale() * 8.0; // Reduced from 15.0 for gentler valleys

        // === REDUCED SURFACE DETAIL for smoother coastlines ===
        double baseDetailFreq = config.getNoiseScale() * 6.0; // Reduced from 8.0 for less noise

        // Octave 1: Medium surface features (~80 block wavelength) - REDUCED amplitude
        double detail1 = masterNoise.sample(worldX * baseDetailFreq, 3, worldZ * baseDetailFreq) * 2.0; // Reduced from 3.0

        // Octave 3: Finer detail (~20 block wavelength) - REDUCED amplitude
        double detail3 = masterNoise.sample(worldX * baseDetailFreq * 4, 5, worldZ * baseDetailFreq * 4) * 0.5; // Reduced from 0.75

        double detail = detail1 + detail3;

        // === DOMAIN WARPING + OVERLAY BLENDING ===
        double baseHeight = planetData.getSeaLevel();
        double terrainHeight = continental;

        // Add mountains directly (volatility-based placement already calculated above)
        terrainHeight += mountains;

        // Apply valleys with soft blending
        terrainHeight = blendOverlay(terrainHeight, valleys);

        // Add surface detail
        terrainHeight += detail;

        // === SMOOTH VALLEY FLOOR FLATTENING - NO HARD THRESHOLDS ===
        // Create flat valley floors for realistic river valleys with smooth transitions
        if (valleyCenter > 0.05) { // Very low threshold to start smooth transition early
            // Calculate target floor height (flattened to base continental level)
            double targetFloorHeight = continental * 0.8;

            // SMOOTH flattening strength - no hard boundaries
            double smoothValleyCenter = smoothstep(Math.max(0.0, (valleyCenter - 0.05) / 0.35)); // Smooth 0-1 from valleyCenter 0.05-0.4
            double flatteningStrength = smoothValleyCenter * 0.4; // Gentle maximum flattening

            terrainHeight = lerp(terrainHeight, targetFloorHeight, flatteningStrength);
        }

        return baseHeight + terrainHeight;
    }

    /**
     * Linear interpolation helper for valley floor flattening
     */
    private double lerp(double a, double b, double t) {
        return a + t * (b - a);
    }

    /**
     * Smooth step function for eliminating harsh transitions
     * Creates smooth S-curve transition from 0 to 1
     */
    private double smoothstep(double t) {
        t = Math.max(0.0, Math.min(1.0, t)); // Clamp to 0-1
        return t * t * (3.0 - 2.0 * t); // Smooth S-curve
    }

    @Override
    public int getLayerCount() { return 8; } // Continental(4 octaves), Mountain(smooth volatility-based), Valley+SmoothFlattening, Detail1(reduced), Detail3(reduced)

    /**
     * Overlay blending for smooth terrain transitions
     */
    private double blendOverlay(double base, double overlay) {
        if (overlay >= 0) {
            return base + overlay * 0.7; // Additive for positive
        } else {
            return base * (1.0 + overlay * 0.1); // Multiplicative for negative
        }
    }
}

/**
 * TECTONIC NOISE MAP - Geological activity & plate boundaries
 * Layers: Plate boundaries, Volcanism, Seismic zones
 * Influenced By: Mostly independent (drives other maps)
 * Blending: Hard boundaries (min/max) for plate edges
 */
class TectonicNoiseMap extends NoiseMap {

    public TectonicNoiseMap(PlanetConfig config, PlanetData planetData, SimplexNoiseSampler masterNoise) {
        super(config, planetData, masterNoise);
    }

    @Override
    public double sample(int worldX, int worldZ) {
        // === PLATE BOUNDARY LAYER ===
        double plateFreq = config.getNoiseScale();
        double plates = masterNoise.sample(worldX * plateFreq, 10, worldZ * plateFreq);

        // Create sharp plate boundaries using ridge noise
        double boundaries = Math.abs(plates);
        boundaries = 1.0 - boundaries; // Invert so boundaries are high
        boundaries = Math.max(0, boundaries - 0.7) / 0.3; // Sharp cutoff

        // === VOLCANISM LAYER ===
        double volcanismFreq = config.getNoiseScale() * 3.0;
        double volcanism = masterNoise.sample(worldX * volcanismFreq, 11, worldZ * volcanismFreq);
        volcanism = Math.max(0, volcanism) * planetData.getVolcanismLevel();

        // === SEISMIC ZONES LAYER ===
        double seismicFreq = config.getNoiseScale() * 0.8;
        double seismic = masterNoise.sample(worldX * seismicFreq, 12, worldZ * seismicFreq);
        seismic = Math.abs(seismic) * planetData.getActualTectonicActivity();

        // === HARD BOUNDARY BLENDING ===
        double tectonicActivity = Math.max(boundaries, Math.max(volcanism, seismic));

        // Scale by planet's overall tectonic activity
        return tectonicActivity * planetData.getActualTectonicActivity();
    }

    @Override
    public int getLayerCount() { return 3; }
}

// Add these classes to the bottom of PlanetaryNoiseSystem.java file

/**
 * TEMPERATURE NOISE MAP - Realistic temperature distribution
 * Layers: Base + Latitude + Elevation + Local Variation
 * Physics: -40°C equator to pole, -20°C per 100 blocks elevation, ±5°C noise
 */
class TemperatureNoiseMap extends NoiseMap {

    private final TerrainNoiseMap terrainMap; // Need terrain height for elevation cooling

    public TemperatureNoiseMap(PlanetConfig config, PlanetData planetData, SimplexNoiseSampler masterNoise, TerrainNoiseMap terrainMap) {
        super(config, planetData, masterNoise);
        this.terrainMap = terrainMap;
    }

    @Override
    public double sample(int worldX, int worldZ) {
        // === BASE TEMPERATURE ===
        double baseTemp = planetData.getAverageSurfaceTemp();

        // === LATITUDE EFFECT (0-1 from equator to poles) ===
        double latitude = Math.abs(worldZ) / (config.getCircumference() * 0.25);
        latitude = Math.min(1.0, latitude); // Clamp to prevent overflow at extreme coordinates
        double latitudeEffect = -latitude * 40.0; // -40°C from equator to pole

        // === ELEVATION COOLING ===
        double terrainHeight = terrainMap.sample(worldX, worldZ);
        double seaLevel = planetData.getSeaLevel();
        double elevationAboveSeaLevel = Math.max(0, terrainHeight - seaLevel); // Only positive elevation counts
        double elevationEffect = -(elevationAboveSeaLevel / 100.0) * 20.0; // -20°C per 100 blocks

        // === LOCAL TEMPERATURE VARIATION ===
        double tempNoiseFreq = config.getNoiseScale() * 1.2; // Medium-scale temperature variation
        double temperatureNoise = masterNoise.sample(worldX * tempNoiseFreq, 20, worldZ * tempNoiseFreq);
        double temperatureVariation = temperatureNoise * 5.0; // ±5°C local variation

        // === COMBINE ALL EFFECTS ===
        double finalTemperature = baseTemp + latitudeEffect + elevationEffect + temperatureVariation;

        return finalTemperature;
    }

    @Override
    public int getLayerCount() {
        return 4; // Base + Latitude + Elevation + Local Variation
    }
}

/**
 * WIND NOISE MAP - Terrain-based wind patterns
 * Layers: Base + Elevation Gradients + Temperature Gradients + Local Variation
 * Physics: Wind flows from low elevation to high elevation, influenced by atmospheric density
 */
class WindNoiseMap extends NoiseMap {

    private final TerrainNoiseMap terrainMap;
    private final TemperatureNoiseMap temperatureMap;

    // Sampling distance for calculating gradients (in blocks)
    private static final int GRADIENT_SAMPLE_DISTANCE = 64; // Sample 64 blocks in each direction

    public WindNoiseMap(PlanetConfig config, PlanetData planetData, SimplexNoiseSampler masterNoise,
                        TerrainNoiseMap terrainMap, TemperatureNoiseMap temperatureMap) {
        super(config, planetData, masterNoise);
        this.terrainMap = terrainMap;
        this.temperatureMap = temperatureMap;
    }

    @Override
    public double sample(int worldX, int worldZ) {
        // This returns wind speed magnitude - use sampleSpeed() for the same result
        return sampleSpeed(worldX, worldZ);
    }

    /**
     * Sample wind speed (0.0-1.0) at world coordinates
     */
    public double sampleSpeed(int worldX, int worldZ) {
        // === BASE WIND FROM ATMOSPHERIC DENSITY ===
        double baseWind = planetData.getActualAtmosphericDensity() * 0.3;

        // === ELEVATION GRADIENT WIND ===
        double elevationGradientStrength = calculateElevationGradientStrength(worldX, worldZ);
        double elevationWind = elevationGradientStrength * 0.4; // Scale factor for terrain effect

        // === TEMPERATURE GRADIENT WIND ===
        double temperatureGradientStrength = calculateTemperatureGradientStrength(worldX, worldZ);
        double temperatureWind = temperatureGradientStrength * 0.2; // Weaker than terrain effect

        // === LOCAL WIND VARIATION ===
        double windNoiseFreq = config.getNoiseScale() * 2.0; // Higher frequency for local variation
        double windNoise = masterNoise.sample(worldX * windNoiseFreq, 30, worldZ * windNoiseFreq);
        double windVariation = windNoise * 0.15; // ±0.15 local variation

        // === COMBINE ALL EFFECTS ===
        double totalWindSpeed = baseWind + elevationWind + temperatureWind + windVariation;

        // Clamp to 0.0-1.0 range
        return Math.max(0.0, Math.min(1.0, totalWindSpeed));
    }

    /**
     * Sample wind direction at world coordinates
     * Returns [directionX, directionZ] normalized vector pointing toward higher elevation
     */
    public double[] sampleDirection(int worldX, int worldZ) {
        // Calculate elevation gradients in X and Z directions
        double[] elevationGradient = calculateElevationGradient(worldX, worldZ);

        // Calculate temperature gradients (weaker influence)
        double[] temperatureGradient = calculateTemperatureGradient(worldX, worldZ);

        // Combine gradients (elevation is primary, temperature is secondary)
        double directionX = elevationGradient[0] * 0.8 + temperatureGradient[0] * 0.2;
        double directionZ = elevationGradient[1] * 0.8 + temperatureGradient[1] * 0.2;

        // Normalize direction vector
        double magnitude = Math.sqrt(directionX * directionX + directionZ * directionZ);
        if (magnitude > 0.001) { // Avoid division by zero
            directionX /= magnitude;
            directionZ /= magnitude;
        } else {
            // No significant gradient - no preferred direction
            directionX = 0.0;
            directionZ = 0.0;
        }

        return new double[]{directionX, directionZ};
    }

    /**
     * Sample wind vector (speed and direction combined)
     * Returns [speedX, speedZ] where magnitude is speed, direction is flow direction
     */
    public double[] sampleVector(int worldX, int worldZ) {
        double speed = sampleSpeed(worldX, worldZ);
        double[] direction = sampleDirection(worldX, worldZ);

        return new double[]{direction[0] * speed, direction[1] * speed};
    }

    /**
     * Calculate elevation gradient strength (used for wind speed calculation)
     */
    private double calculateElevationGradientStrength(int worldX, int worldZ) {
        double[] gradient = calculateElevationGradient(worldX, worldZ);
        return Math.sqrt(gradient[0] * gradient[0] + gradient[1] * gradient[1]);
    }

    /**
     * Calculate elevation gradient vector [dX, dZ]
     */
    private double[] calculateElevationGradient(int worldX, int worldZ) {
        // Sample terrain height at current position and in 4 cardinal directions
        double centerHeight = terrainMap.sample(worldX, worldZ);
        double eastHeight = terrainMap.sample(worldX + GRADIENT_SAMPLE_DISTANCE, worldZ);
        double westHeight = terrainMap.sample(worldX - GRADIENT_SAMPLE_DISTANCE, worldZ);
        double northHeight = terrainMap.sample(worldX, worldZ - GRADIENT_SAMPLE_DISTANCE);
        double southHeight = terrainMap.sample(worldX, worldZ + GRADIENT_SAMPLE_DISTANCE);

        // Calculate gradients (positive values point toward higher elevation)
        double gradientX = (eastHeight - westHeight) / (2.0 * GRADIENT_SAMPLE_DISTANCE);
        double gradientZ = (southHeight - northHeight) / (2.0 * GRADIENT_SAMPLE_DISTANCE);

        return new double[]{gradientX, gradientZ};
    }

    /**
     * Calculate temperature gradient strength (used for wind speed calculation)
     */
    private double calculateTemperatureGradientStrength(int worldX, int worldZ) {
        double[] gradient = calculateTemperatureGradient(worldX, worldZ);
        return Math.sqrt(gradient[0] * gradient[0] + gradient[1] * gradient[1]);
    }

    /**
     * Calculate temperature gradient vector [dX, dZ]
     * Wind flows from hot to cold (opposite of temperature gradient)
     */
    private double[] calculateTemperatureGradient(int worldX, int worldZ) {
        // Sample temperature at current position and in 4 cardinal directions
        double centerTemp = temperatureMap.sample(worldX, worldZ);
        double eastTemp = temperatureMap.sample(worldX + GRADIENT_SAMPLE_DISTANCE, worldZ);
        double westTemp = temperatureMap.sample(worldX - GRADIENT_SAMPLE_DISTANCE, worldZ);
        double northTemp = temperatureMap.sample(worldX, worldZ - GRADIENT_SAMPLE_DISTANCE);
        double southTemp = temperatureMap.sample(worldX, worldZ + GRADIENT_SAMPLE_DISTANCE);

        // Calculate gradients (positive values point toward higher temperature)
        double gradientX = (eastTemp - westTemp) / (2.0 * GRADIENT_SAMPLE_DISTANCE);
        double gradientZ = (southTemp - northTemp) / (2.0 * GRADIENT_SAMPLE_DISTANCE);

        // Wind flows from hot to cold, so reverse the gradient
        return new double[]{-gradientX * 0.1, -gradientZ * 0.1}; // Scale down temperature effect
    }

    @Override
    public int getLayerCount() {
        return 4; // Base + Elevation Gradients + Temperature Gradients + Local Variation
    }
}

/**
 * MOISTURE NOISE MAP - Wind-transported humidity with distance from water effects
 * Layers: Base + Wind Transport + Distance from Water + Local Variation
 * Physics: Wind carries moisture from water sources, blocked naturally by mountains
 */
class MoistureNoiseMap extends NoiseMap {

    private final TerrainNoiseMap terrainMap;
    private final WindNoiseMap windMap;

    // Transport distance for moisture calculations (in blocks)
    private static final int MOISTURE_TRANSPORT_DISTANCE = 128; // How far moisture travels via wind
    private static final int WATER_DISTANCE_SAMPLE = 256; // How far to check for water sources

    public MoistureNoiseMap(PlanetConfig config, PlanetData planetData, SimplexNoiseSampler masterNoise,
                            TerrainNoiseMap terrainMap, WindNoiseMap windMap) {
        super(config, planetData, masterNoise);
        this.terrainMap = terrainMap;
        this.windMap = windMap;
    }

    @Override
    public double sample(int worldX, int worldZ) {
        // === BASE HUMIDITY ===
        double baseHumidity = planetData.getActualWaterContent() * 0.7; // 70% of planet water content

        // === WIND TRANSPORT EFFECT ===
        double windTransportHumidity = calculateWindTransportHumidity(worldX, worldZ);

        // === DISTANCE FROM WATER EFFECT ===
        double distanceFromWaterEffect = calculateDistanceFromWaterEffect(worldX, worldZ);

        // === LOCAL HUMIDITY VARIATION ===
        double humidityNoiseFreq = config.getNoiseScale() * 1.5;
        double humidityNoise = masterNoise.sample(worldX * humidityNoiseFreq, 40, worldZ * humidityNoiseFreq);
        double humidityVariation = humidityNoise * 0.1; // ±0.1 local variation

        // === COMBINE ALL EFFECTS ===
        double totalHumidity = baseHumidity + windTransportHumidity + distanceFromWaterEffect + humidityVariation;

        // Clamp to 0.0-1.0 range
        return Math.max(0.0, Math.min(1.0, totalHumidity));
    }

    /**
     * Calculate humidity transported by wind from upwind areas
     * Samples humidity from the opposite direction of wind flow
     */
    private double calculateWindTransportHumidity(int worldX, int worldZ) {
        // Get wind direction at current location
        double[] windDirection = windMap.sampleDirection(worldX, worldZ);
        double windSpeed = windMap.sampleSpeed(worldX, worldZ);

        // If no wind, no transport
        if (windSpeed < 0.1) {
            return 0.0;
        }

        // Sample humidity from upwind location (opposite of wind direction)
        double transportDistance = MOISTURE_TRANSPORT_DISTANCE * windSpeed; // Stronger wind = further transport
        int upwindX = (int) (worldX - windDirection[0] * transportDistance);
        int upwindZ = (int) (worldZ - windDirection[1] * transportDistance);

        // Calculate base humidity at upwind location (without wind transport to avoid recursion)
        double upwindBaseHumidity = planetData.getActualWaterContent() * 0.7;
        double upwindDistanceEffect = calculateDistanceFromWaterEffect(upwindX, upwindZ);
        double upwindHumidity = upwindBaseHumidity + upwindDistanceEffect;

        // Transport efficiency decreases with distance and increases with wind speed
        double transportEfficiency = windSpeed * 0.3; // Max 30% of upwind humidity transported

        return Math.max(0.0, upwindHumidity * transportEfficiency);
    }

    /**
     * Calculate humidity effect based on distance from water sources (sea level areas)
     */
    private double calculateDistanceFromWaterEffect(int worldX, int worldZ) {
        double seaLevel = planetData.getSeaLevel();
        double minDistanceToWater = Double.MAX_VALUE;

        // Sample terrain in a grid around current location to find nearest water
        int sampleStep = WATER_DISTANCE_SAMPLE / 8; // Sample 8x8 grid
        for (int dx = -WATER_DISTANCE_SAMPLE; dx <= WATER_DISTANCE_SAMPLE; dx += sampleStep) {
            for (int dz = -WATER_DISTANCE_SAMPLE; dz <= WATER_DISTANCE_SAMPLE; dz += sampleStep) {
                double sampleHeight = terrainMap.sample(worldX + dx, worldZ + dz);

                // Check if this location is at or below sea level (water source)
                if (sampleHeight <= seaLevel + 5) { // Small tolerance for near-water areas
                    double distance = Math.sqrt(dx * dx + dz * dz);
                    minDistanceToWater = Math.min(minDistanceToWater, distance);
                }
            }
        }

        // Convert distance to humidity effect (closer = more humid)
        if (minDistanceToWater == Double.MAX_VALUE) {
            return -0.2; // No water found nearby - reduce humidity
        }

        // Humidity decreases with distance from water
        double normalizedDistance = minDistanceToWater / WATER_DISTANCE_SAMPLE; // 0-1 scale
        double waterEffect = (1.0 - normalizedDistance) * 0.3; // Max +0.3 humidity near water

        return Math.max(-0.2, waterEffect); // Minimum -0.2 humidity effect
    }

    @Override
    public int getLayerCount() {
        return 4; // Base + Wind Transport + Distance from Water + Local Variation
    }
}

/**
 * PLACEHOLDER - Biome Noise Map (Phase 7)
 * TODO: Implement proper biome classification based on temperature, moisture, elevation, and geology
 */
class BiomeNoiseMap extends NoiseMap {

    public BiomeNoiseMap(PlanetConfig config, PlanetData planetData, SimplexNoiseSampler masterNoise) {
        super(config, planetData, masterNoise);
    }

    @Override
    public double sample(int worldX, int worldZ) {
        // PLACEHOLDER: Simple biome classification based on temperature/moisture
        // This will be replaced with proper Whittaker biome classification in Phase 7

        // Get temperature and moisture (will use proper maps in Phase 7)
        double temp = planetData.getAverageSurfaceTemp();
        double moisture = planetData.getActualWaterContent();

        // Simple biome index classification
        if (temp < 0) return 0.0;           // Cold/Tundra
        if (moisture < 0.3) return 1.0;     // Desert
        if (temp > 25) return 2.0;          // Tropical
        return 3.0;                         // Temperate
    }

    @Override
    public int getLayerCount() {
        return 1; // Will have 2-3 layers in Phase 7: Classification + Transition smoothing + Local variations
    }
}