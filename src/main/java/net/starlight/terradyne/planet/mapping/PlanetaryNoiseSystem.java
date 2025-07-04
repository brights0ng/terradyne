package net.starlight.terradyne.planet.mapping;

import net.minecraft.util.math.noise.SimplexNoiseSampler;
import net.minecraft.util.math.random.Random;
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
    private final HabitabilityNoiseMap habitabilityMap;

    // === PHASE 2: VOLATILITY SYSTEM ===
    private final TectonicVolatilityManager volatilityManager;

    private final RegionCompletionTracker cacheTracker;

    /**
     * Create planetary noise system from planet model data
     * UPDATED: Now includes volatility manager initialization
     */
    public PlanetaryNoiseSystem(PlanetConfig config, PlanetData planetData, RegionCompletionTracker cacheTracker) {
        this.config = config;
        this.planetData = planetData;
        this.cacheTracker = cacheTracker;

        Terradyne.LOGGER.info("Initializing Planetary Noise System for {}", config.getPlanetName());

        // Create master noise sampler - all terrain uses this base
        this.masterNoise = new SimplexNoiseSampler(Random.create(config.getSeed()));

        // === PHASE C: CORE TERRAIN MAPS ===
        this.tectonicMap = new TectonicNoiseMap(config, planetData, masterNoise);
        this.volatilityManager = new TectonicVolatilityManager(config, planetData, masterNoise);
        this.terrainMap = new TerrainNoiseMap(config, planetData, masterNoise, tectonicMap, volatilityManager);

        // === PHASE 5: CLIMATE MAPS (now with caching) ===
        this.temperatureMap = new TemperatureNoiseMap(config, planetData, masterNoise, terrainMap, cacheTracker);
        this.windMap = new WindNoiseMap(config, planetData, masterNoise, terrainMap, temperatureMap, cacheTracker);
        this.moistureMap = new MoistureNoiseMap(config, planetData, masterNoise, terrainMap, windMap, cacheTracker);
        this.habitabilityMap = new HabitabilityNoiseMap(config, planetData, masterNoise, terrainMap, temperatureMap, windMap, moistureMap, cacheTracker);

        this.biomeMap = new BiomeNoiseMap(config, planetData,masterNoise);

        Terradyne.LOGGER.info("✅ Planetary Noise System initialized with caching");
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
     * Sample habitability at world coordinates
     * Combines temperature, elevation, humidity, and wind factors for livability assessment
     */
    public double sampleHabitability(int worldX, int worldZ) {
        if (habitabilityMap != null) {
            return habitabilityMap.sample(worldX, worldZ);
        }
        return planetData.getHabitability(); // Fallback
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
                moistureMap != null ? "ACTIVE" : "NULL",
                habitabilityMap != null ? "ACTIVE" : "NULL");
    }

    /**
     * Sample all maps at coordinates for debugging
     */
    public String sampleAllMaps(int worldX, int worldZ) {
        double[] windVector = sampleWindVector(worldX, worldZ);
        double windSpeed = Math.sqrt(windVector[0] * windVector[0] + windVector[1] * windVector[1]);

        return String.format("NoiseDebug{x=%d,z=%d: terrain=%.2f, tectonic=%.2f, volatility=%d, temp=%.1f°C, wind=%.2f, humidity=%.2f, habitability=%.2f\n",
                worldX, worldZ,
                sampleTerrainHeight(worldX, worldZ),
                sampleTectonicActivity(worldX, worldZ),
                sampleVolatility(worldX, worldZ),
                sampleTemperature(worldX, worldZ),
                windSpeed,
                sampleMoisture(worldX, worldZ),
                sampleHabitability(worldX, worldZ));
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
        Terradyne.LOGGER.info("  Habitability: {} layers (Temperature + Elevation + Humidity + Wind + Variation)",
                habitabilityMap != null ? habitabilityMap.getLayerCount() : 0);
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
 * Layers: Continental (fractal coastlines), Mountain (smooth volatility-based), Erosion (atmospheric flattening), Valley, Surface Detail
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
 *
 * NEW EROSION LAYER:
 * - Atmospheric density drives erosion intensity
 * - Medium-scale noise creates scattered plains within mountainous areas
 * - Smooth blending prevents harsh erosion boundaries
 * - Up to 75% terrain reduction in high-erosion zones
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

        // === MOUNTAIN LAYER - SIMPLE NOISE-BASED (volatility disabled for diagnosis) ===
        double mountainFreq = config.getNoiseScale() * 1.2;
        double mountainNoise = masterNoise.sample(warpedX * mountainFreq, 1, warpedZ * mountainFreq);
        mountainNoise = Math.abs(mountainNoise); // Ridge noise for mountain ranges

        // TEMPORARY: Use simple noise-based mountain intensity instead of volatility
        double mountainIntensityFreq = config.getNoiseScale() * 0.8; // Larger scale for mountain placement
        double mountainIntensityNoise = masterNoise.sample(warpedX * mountainIntensityFreq, 10, warpedZ * mountainIntensityFreq);
        double mountainIntensity = Math.max(0.0, mountainIntensityNoise * 0.7 + 0.3); // 0.3-1.0 range

        // Apply mountain noise with smooth noise-based intensity
        double mountains = mountainNoise * mountainIntensity * planetData.getMountainScale() * 50.0;

        // === NEW: EROSION LAYER - ATMOSPHERIC FLATTENING ===
        double erosionFreq = config.getNoiseScale() * 1.5; // Medium-scale erosion patterns
        double erosionNoise = masterNoise.sample(warpedX * erosionFreq, 6, warpedZ * erosionFreq);

        // Linear scaling with atmospheric density
        double erosionIntensity = planetData.getActualAtmosphericDensity() * Math.abs(erosionNoise);

        // Calculate current terrain height before erosion
        double preErosionHeight = continental + mountains;

        // Apply erosion in specific zones with smooth blending
        if (erosionIntensity > 0.3) { // Erosion threshold - only in specific zones
            // Smooth erosion strength calculation
            double rawErosionStrength = (erosionIntensity - 0.3) / 0.7; // 0-1 scale above threshold
            double smoothErosionStrength = smoothstep(rawErosionStrength); // Smooth S-curve

            // Apply up to 75% terrain reduction with smooth blending
            double erosionMultiplier = 1.0 - (smoothErosionStrength * 0.75);
            preErosionHeight *= erosionMultiplier;

            // Log debug info occasionally (every 1000th sample to avoid spam)
            if (worldX % 1000 == 0 && worldZ % 1000 == 0) {
                // Debug logging for erosion effects
            }
        }

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
        double terrainHeight = preErosionHeight; // Use post-erosion height

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
    public int getLayerCount() { return 9; } // Continental(4 octaves), Mountain(smooth volatility-based), Erosion(atmospheric), Valley+SmoothFlattening, Detail1(reduced), Detail3(reduced)

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
 * TEMPERATURE NOISE MAP - Realistic temperature distribution WITH CACHING
 * Layers: Base + Latitude + Elevation + Local Variation
 * Physics: -40°C equator to pole, -20°C per 100 blocks elevation, ±5°C noise
 * Performance: Chunk-level caching for ~250x speedup
 */
class TemperatureNoiseMap extends NoiseMap {

    private final TerrainNoiseMap terrainMap;
    private final RegionCompletionTracker cacheTracker;

    public TemperatureNoiseMap(PlanetConfig config, PlanetData planetData, SimplexNoiseSampler masterNoise,
                               TerrainNoiseMap terrainMap, RegionCompletionTracker cacheTracker) {
        super(config, planetData, masterNoise);
        this.terrainMap = terrainMap;
        this.cacheTracker = cacheTracker;
    }

    @Override
    public double sample(int worldX, int worldZ) {
        // Convert to chunk coordinates
        int chunkX = worldX >> 4;
        int chunkZ = worldZ >> 4;

        // Get region key
        RegionCompletionTracker.RegionKey regionKey = RegionCompletionTracker.RegionKey.fromChunkCoords(config.getPlanetName(), chunkX, chunkZ);

        // Check cache first
        Double cached = cacheTracker.getCachedTemperature(regionKey, chunkX, chunkZ);
        if (cached != null) {
            return cached;
        }

        // Not cached - compute at chunk center for consistency
        int chunkCenterX = (chunkX << 4) + 8;
        int chunkCenterZ = (chunkZ << 4) + 8;

        double temperature = computeTemperatureAt(chunkCenterX, chunkCenterZ);

        // Cache the result
        cacheTracker.setCachedTemperature(regionKey, chunkX, chunkZ, temperature);

        return temperature;
    }

    /**
     * Actual temperature computation (moved to separate method for clarity)
     */
    private double computeTemperatureAt(int worldX, int worldZ) {
        // === BASE TEMPERATURE ===
        double baseTemp = planetData.getAverageSurfaceTemp();

        // === LATITUDE EFFECT (0-1 from equator to poles) ===
        double latitude = Math.abs(worldZ) / (config.getCircumference() * 0.25);
        latitude = Math.min(1.0, latitude);
        double latitudeEffect = -latitude * 40.0;

        // === ELEVATION COOLING ===
        double terrainHeight = terrainMap.sample(worldX, worldZ);
        double seaLevel = planetData.getSeaLevel();
        double elevationAboveSeaLevel = Math.max(0, terrainHeight - seaLevel);
        double elevationEffect = -(elevationAboveSeaLevel / 100.0) * 20.0;

        // === LOCAL TEMPERATURE VARIATION ===
        double tempNoiseFreq = config.getNoiseScale() * 1.2;
        double temperatureNoise = masterNoise.sample(worldX * tempNoiseFreq, 20, worldZ * tempNoiseFreq);
        double temperatureVariation = temperatureNoise * 5.0;

        return baseTemp + latitudeEffect + elevationEffect + temperatureVariation;
    }

    @Override
    public int getLayerCount() {
        return 4;
    }
}

/**
 * WIND NOISE MAP - Terrain-based wind patterns WITH CACHING
 * Layers: Base + Elevation Gradients + Temperature Gradients + Local Variation
 * Physics: Wind flows from low elevation to high elevation, influenced by atmospheric density
 * Performance: Chunk-level caching for ~250x speedup on expensive gradient calculations
 */
class WindNoiseMap extends NoiseMap {

    private final TerrainNoiseMap terrainMap;
    private final TemperatureNoiseMap temperatureMap;
    private final RegionCompletionTracker cacheTracker;

    // Sampling distance for calculating gradients (in blocks)
    private static final int GRADIENT_SAMPLE_DISTANCE = 64;

    public WindNoiseMap(PlanetConfig config, PlanetData planetData, SimplexNoiseSampler masterNoise,
                        TerrainNoiseMap terrainMap, TemperatureNoiseMap temperatureMap, RegionCompletionTracker cacheTracker) {
        super(config, planetData, masterNoise);
        this.terrainMap = terrainMap;
        this.temperatureMap = temperatureMap;
        this.cacheTracker = cacheTracker;
    }

    @Override
    public double sample(int worldX, int worldZ) {
        return sampleSpeed(worldX, worldZ);
    }

    /**
     * Sample wind speed (0.0-1.0) at world coordinates WITH CACHING
     */
    public double sampleSpeed(int worldX, int worldZ) {
        // Convert to chunk coordinates
        int chunkX = worldX >> 4;
        int chunkZ = worldZ >> 4;

        // Get region key
        RegionCompletionTracker.RegionKey regionKey = RegionCompletionTracker.RegionKey.fromChunkCoords(config.getPlanetName(), chunkX, chunkZ);

        // Check cache first
        Double cached = cacheTracker.getCachedWindSpeed(regionKey, chunkX, chunkZ);
        if (cached != null) {
            return cached;
        }

        // Not cached - compute at chunk center
        int chunkCenterX = (chunkX << 4) + 8;
        int chunkCenterZ = (chunkZ << 4) + 8;

        double windSpeed = computeWindSpeedAt(chunkCenterX, chunkCenterZ);

        // Cache the result
        cacheTracker.setCachedWindSpeed(regionKey, chunkX, chunkZ, windSpeed);

        return windSpeed;
    }

    /**
     * Actual wind speed computation (separated for caching)
     */
    private double computeWindSpeedAt(int worldX, int worldZ) {
        // === BASE WIND FROM ATMOSPHERIC DENSITY ===
        double baseWind = planetData.getActualAtmosphericDensity() * 0.3;

        // === ELEVATION GRADIENT WIND ===
        double elevationGradientStrength = calculateElevationGradientStrength(worldX, worldZ);
        double elevationWind = elevationGradientStrength * 0.4;

        // === TEMPERATURE GRADIENT WIND ===
        double temperatureGradientStrength = calculateTemperatureGradientStrength(worldX, worldZ);
        double temperatureWind = temperatureGradientStrength * 0.2;

        // === LOCAL WIND VARIATION ===
        double windNoiseFreq = config.getNoiseScale() * 2.0;
        double windNoise = masterNoise.sample(worldX * windNoiseFreq, 30, worldZ * windNoiseFreq);
        double windVariation = windNoise * 0.15;

        double totalWindSpeed = baseWind + elevationWind + temperatureWind + windVariation;
        return Math.max(0.0, Math.min(1.0, totalWindSpeed));
    }

    // NOTE: Direction methods not cached since they're less expensive and called less frequently
    public double[] sampleDirection(int worldX, int worldZ) {
        double[] elevationGradient = calculateElevationGradient(worldX, worldZ);
        double[] temperatureGradient = calculateTemperatureGradient(worldX, worldZ);

        double directionX = elevationGradient[0] * 0.8 + temperatureGradient[0] * 0.2;
        double directionZ = elevationGradient[1] * 0.8 + temperatureGradient[1] * 0.2;

        double magnitude = Math.sqrt(directionX * directionX + directionZ * directionZ);
        if (magnitude > 0.001) {
            directionX /= magnitude;
            directionZ /= magnitude;
        } else {
            directionX = 0.0;
            directionZ = 0.0;
        }

        return new double[]{directionX, directionZ};
    }

    public double[] sampleVector(int worldX, int worldZ) {
        double speed = sampleSpeed(worldX, worldZ); // Uses cached speed
        double[] direction = sampleDirection(worldX, worldZ);
        return new double[]{direction[0] * speed, direction[1] * speed};
    }

    // Keep existing gradient calculation methods unchanged...
    private double calculateElevationGradientStrength(int worldX, int worldZ) {
        double[] gradient = calculateElevationGradient(worldX, worldZ);
        return Math.sqrt(gradient[0] * gradient[0] + gradient[1] * gradient[1]);
    }

    private double[] calculateElevationGradient(int worldX, int worldZ) {
        double centerHeight = terrainMap.sample(worldX, worldZ);
        double eastHeight = terrainMap.sample(worldX + GRADIENT_SAMPLE_DISTANCE, worldZ);
        double westHeight = terrainMap.sample(worldX - GRADIENT_SAMPLE_DISTANCE, worldZ);
        double northHeight = terrainMap.sample(worldX, worldZ - GRADIENT_SAMPLE_DISTANCE);
        double southHeight = terrainMap.sample(worldX, worldZ + GRADIENT_SAMPLE_DISTANCE);

        double gradientX = (eastHeight - westHeight) / (2.0 * GRADIENT_SAMPLE_DISTANCE);
        double gradientZ = (southHeight - northHeight) / (2.0 * GRADIENT_SAMPLE_DISTANCE);

        return new double[]{gradientX, gradientZ};
    }

    private double calculateTemperatureGradientStrength(int worldX, int worldZ) {
        double[] gradient = calculateTemperatureGradient(worldX, worldZ);
        return Math.sqrt(gradient[0] * gradient[0] + gradient[1] * gradient[1]);
    }

    private double[] calculateTemperatureGradient(int worldX, int worldZ) {
        double centerTemp = temperatureMap.sample(worldX, worldZ); // Uses cached temperature!
        double eastTemp = temperatureMap.sample(worldX + GRADIENT_SAMPLE_DISTANCE, worldZ);
        double westTemp = temperatureMap.sample(worldX - GRADIENT_SAMPLE_DISTANCE, worldZ);
        double northTemp = temperatureMap.sample(worldX, worldZ - GRADIENT_SAMPLE_DISTANCE);
        double southTemp = temperatureMap.sample(worldX, worldZ + GRADIENT_SAMPLE_DISTANCE);

        double gradientX = (eastTemp - westTemp) / (2.0 * GRADIENT_SAMPLE_DISTANCE);
        double gradientZ = (southTemp - northTemp) / (2.0 * GRADIENT_SAMPLE_DISTANCE);

        return new double[]{-gradientX * 0.1, -gradientZ * 0.1};
    }

    @Override
    public int getLayerCount() {
        return 4;
    }
}

/**
 * MOISTURE NOISE MAP - Wind-transported humidity WITH SIMPLIFIED CACHING
 * Layers: Base + Wind Transport + Distance from Water + Local Variation
 * Physics: Wind carries moisture from water sources, blocked naturally by mountains
 * Performance: Chunk-level caching + simplified water distance calculation for huge speedup
 */
class MoistureNoiseMap extends NoiseMap {

    private final TerrainNoiseMap terrainMap;
    private final WindNoiseMap windMap;
    private final RegionCompletionTracker cacheTracker;

    // REDUCED transport distance for better performance
    private static final int MOISTURE_TRANSPORT_DISTANCE = 64; // Reduced from 128
    private static final int WATER_DISTANCE_SAMPLE = 128; // Reduced from 256

    public MoistureNoiseMap(PlanetConfig config, PlanetData planetData, SimplexNoiseSampler masterNoise,
                            TerrainNoiseMap terrainMap, WindNoiseMap windMap, RegionCompletionTracker cacheTracker) {
        super(config, planetData, masterNoise);
        this.terrainMap = terrainMap;
        this.windMap = windMap;
        this.cacheTracker = cacheTracker;
    }

    @Override
    public double sample(int worldX, int worldZ) {
        // Convert to chunk coordinates
        int chunkX = worldX >> 4;
        int chunkZ = worldZ >> 4;

        // Get region key
        RegionCompletionTracker.RegionKey regionKey = RegionCompletionTracker.RegionKey.fromChunkCoords(config.getPlanetName(), chunkX, chunkZ);

        // Check cache first
        Double cached = cacheTracker.getCachedMoisture(regionKey, chunkX, chunkZ);
        if (cached != null) {
            return cached;
        }

        // Not cached - compute at chunk center
        int chunkCenterX = (chunkX << 4) + 8;
        int chunkCenterZ = (chunkZ << 4) + 8;

        double moisture = computeMoistureAt(chunkCenterX, chunkCenterZ);

        // Cache the result
        cacheTracker.setCachedMoisture(regionKey, chunkX, chunkZ, moisture);

        return moisture;
    }

    /**
     * Actual moisture computation with performance optimizations
     */
    private double computeMoistureAt(int worldX, int worldZ) {
        // === BASE HUMIDITY ===
        double baseHumidity = planetData.getActualWaterContent() * 0.7;

        // === SIMPLIFIED WIND TRANSPORT (reduced recursion) ===
        double windTransportHumidity = calculateSimplifiedWindTransport(worldX, worldZ);

        // === SIMPLIFIED WATER DISTANCE (4 samples instead of 64) ===
        double distanceFromWaterEffect = calculateSimplifiedWaterDistance(worldX, worldZ);

        // === LOCAL HUMIDITY VARIATION ===
        double humidityNoiseFreq = config.getNoiseScale() * 1.5;
        double humidityNoise = masterNoise.sample(worldX * humidityNoiseFreq, 40, worldZ * humidityNoiseFreq);
        double humidityVariation = humidityNoise * 0.1;

        double totalHumidity = baseHumidity + windTransportHumidity + distanceFromWaterEffect + humidityVariation;
        return Math.max(0.0, Math.min(1.0, totalHumidity));
    }

    /**
     * SIMPLIFIED wind transport - less recursive, better performance
     */
    private double calculateSimplifiedWindTransport(int worldX, int worldZ) {
        double windSpeed = windMap.sampleSpeed(worldX, worldZ); // Uses cached wind speed!

        if (windSpeed < 0.1) {
            return 0.0;
        }

        // Simplified transport - just add base humidity effect scaled by wind
        double transportEffect = windSpeed * 0.2; // Much simpler than original recursive method
        return Math.max(0.0, transportEffect);
    }

    /**
     * SIMPLIFIED water distance - 4 cardinal samples instead of 8x8 grid
     */
    private double calculateSimplifiedWaterDistance(int worldX, int worldZ) {
        double seaLevel = planetData.getSeaLevel();
        double minDistanceToWater = Double.MAX_VALUE;

        // PERFORMANCE FIX: Only sample 4 cardinal directions instead of 8x8 grid
        int[] sampleDistances = {WATER_DISTANCE_SAMPLE/4, WATER_DISTANCE_SAMPLE/2, WATER_DISTANCE_SAMPLE};

        for (int distance : sampleDistances) {
            // Check 4 cardinal directions
            int[][] directions = {{distance, 0}, {-distance, 0}, {0, distance}, {0, -distance}};

            for (int[] dir : directions) {
                double sampleHeight = terrainMap.sample(worldX + dir[0], worldZ + dir[1]);

                if (sampleHeight <= seaLevel + 5) {
                    double actualDistance = Math.sqrt(dir[0] * dir[0] + dir[1] * dir[1]);
                    minDistanceToWater = Math.min(minDistanceToWater, actualDistance);
                }
            }

            // Early exit if we found nearby water
            if (minDistanceToWater < WATER_DISTANCE_SAMPLE / 2) {
                break;
            }
        }

        if (minDistanceToWater == Double.MAX_VALUE) {
            return -0.2; // No water found
        }

        double normalizedDistance = minDistanceToWater / WATER_DISTANCE_SAMPLE;
        double waterEffect = (1.0 - normalizedDistance) * 0.3;
        return Math.max(-0.2, waterEffect);
    }

    @Override
    public int getLayerCount() {
        return 4;
    }
}

/**
 * HABITABILITY NOISE MAP - Livability assessment WITH CACHING
 * Layers: Base + Temperature Factor + Environmental Factors + Wind Factor + Local Variation
 * Physics: Optimal at 10-30°C, sea level, moderate humidity, calm winds
 * Performance: Chunk-level caching leverages cached temperature/wind/moisture for massive speedup
 */
class HabitabilityNoiseMap extends NoiseMap {

    private final TerrainNoiseMap terrainMap;
    private final TemperatureNoiseMap temperatureMap;
    private final WindNoiseMap windMap;
    private final MoistureNoiseMap moistureMap;
    private final RegionCompletionTracker cacheTracker;

    // Factor weights
    private static final double TEMPERATURE_WEIGHT = 0.35;
    private static final double ELEVATION_WEIGHT = 0.25;
    private static final double HUMIDITY_WEIGHT = 0.20;
    private static final double WIND_WEIGHT = 0.10;
    private static final double VARIATION_WEIGHT = 0.10;

    public HabitabilityNoiseMap(PlanetConfig config, PlanetData planetData, SimplexNoiseSampler masterNoise,
                                TerrainNoiseMap terrainMap, TemperatureNoiseMap temperatureMap,
                                WindNoiseMap windMap, MoistureNoiseMap moistureMap, RegionCompletionTracker cacheTracker) {
        super(config, planetData, masterNoise);
        this.terrainMap = terrainMap;
        this.temperatureMap = temperatureMap;
        this.windMap = windMap;
        this.moistureMap = moistureMap;
        this.cacheTracker = cacheTracker;
    }

    @Override
    public double sample(int worldX, int worldZ) {
        // Convert to chunk coordinates
        int chunkX = worldX >> 4;
        int chunkZ = worldZ >> 4;

        // Get region key
        RegionCompletionTracker.RegionKey regionKey = RegionCompletionTracker.RegionKey.fromChunkCoords(config.getPlanetName(), chunkX, chunkZ);

        // Check cache first
        Double cached = cacheTracker.getCachedHabitability(regionKey, chunkX, chunkZ);
        if (cached != null) {
            return cached;
        }

        // Not cached - compute at chunk center
        int chunkCenterX = (chunkX << 4) + 8;
        int chunkCenterZ = (chunkZ << 4) + 8;

        double habitability = computeHabitabilityAt(chunkCenterX, chunkCenterZ);

        // Cache the result
        cacheTracker.setCachedHabitability(regionKey, chunkX, chunkZ, habitability);

        return habitability;
    }

    /**
     * Actual habitability computation - leverages cached inputs for speed!
     */
    private double computeHabitabilityAt(int worldX, int worldZ) {
        double baseHabitability = planetData.getHabitability();

        // All of these are now cached and fast!
        double temperatureFactor = calculateTemperatureFactor(worldX, worldZ);
        double elevationFactor = calculateElevationFactor(worldX, worldZ);
        double humidityFactor = calculateHumidityFactor(worldX, worldZ);
        double windFactor = calculateWindFactor(worldX, worldZ);
        double localVariation = calculateLocalVariation(worldX, worldZ);

        double factorSum = (temperatureFactor * TEMPERATURE_WEIGHT) +
                (elevationFactor * ELEVATION_WEIGHT) +
                (humidityFactor * HUMIDITY_WEIGHT) +
                (windFactor * WIND_WEIGHT) +
                (localVariation * VARIATION_WEIGHT);

        double finalHabitability = baseHabitability * factorSum;
        return Math.max(0.0, Math.min(1.20, finalHabitability));
    }

    // Keep existing factor calculation methods - they now use cached inputs!
    private double calculateTemperatureFactor(int worldX, int worldZ) {
        double temperature = temperatureMap.sample(worldX, worldZ); // CACHED!

        if (temperature >= 10.0 && temperature <= 30.0) {
            return 1.0;
        } else if (temperature >= 0.0 && temperature <= 40.0) {
            if (temperature < 10.0) {
                return 0.8 + (temperature / 10.0) * 0.2;
            } else {
                return 0.8 + ((40.0 - temperature) / 10.0) * 0.2;
            }
        } else if (temperature >= -10.0 && temperature <= 50.0) {
            if (temperature < 0.0) {
                return 0.5 + ((temperature + 10.0) / 10.0) * 0.3;
            } else {
                return 0.5 + ((50.0 - temperature) / 10.0) * 0.3;
            }
        } else if (temperature >= -20.0 && temperature <= 60.0) {
            if (temperature < -10.0) {
                return 0.1 + ((temperature + 20.0) / 10.0) * 0.4;
            } else {
                return 0.1 + ((60.0 - temperature) / 10.0) * 0.4;
            }
        } else {
            return 0.0;
        }
    }

    private double calculateElevationFactor(int worldX, int worldZ) {
        double terrainHeight = terrainMap.sample(worldX, worldZ);
        double seaLevel = planetData.getSeaLevel();
        double elevationDifference = Math.abs(terrainHeight - seaLevel);

        if (elevationDifference <= 100.0) {
            return 1.0 - (elevationDifference / 100.0) * 0.1;
        } else if (elevationDifference <= 200.0) {
            return 0.9 - ((elevationDifference - 100.0) / 100.0) * 0.2;
        } else {
            return Math.max(0.0, 0.7 - ((elevationDifference - 200.0) / 100.0) * 0.1);
        }
    }

    private double calculateHumidityFactor(int worldX, int worldZ) {
        double humidity = moistureMap.sample(worldX, worldZ); // CACHED!
        return Math.max(0.0, Math.min(1.2, humidity * 1.2));
    }

    private double calculateWindFactor(int worldX, int worldZ) {
        double windSpeed = windMap.sampleSpeed(worldX, worldZ); // CACHED!

        if (windSpeed < 0.6) {
            return 1.0;
        } else if (windSpeed < 0.7) {
            return 1.0 - ((windSpeed - 0.6) / 0.1) * 0.2;
        } else if (windSpeed < 0.9) {
            return 0.8 - ((windSpeed - 0.7) / 0.2) * 0.3;
        } else {
            double severePenalty = Math.min(1.0, (windSpeed - 0.9) / 0.1);
            return 0.5 - (severePenalty * 0.4);
        }
    }

    private double calculateLocalVariation(int worldX, int worldZ) {
        double habitabilityNoiseFreq = config.getNoiseScale() * 2.5;
        double habitabilityNoise = masterNoise.sample(worldX * habitabilityNoiseFreq, 50, worldZ * habitabilityNoiseFreq);
        return 1.0 + (habitabilityNoise * 0.5);
    }

    @Override
    public int getLayerCount() {
        return 5;
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

