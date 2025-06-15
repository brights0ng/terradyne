package net.starlight.terradyne.planet.terrain.noise;

import net.minecraft.util.math.noise.SimplexNoiseSampler;
import net.minecraft.util.math.random.Random;
import net.starlight.terradyne.planet.terrain.config.NoiseLayerConfig;
import net.starlight.terradyne.planet.terrain.config.UnifiedTerrainConfig;
import net.starlight.terradyne.planet.terrain.math.TerrainMathUtils;
import net.starlight.terradyne.Terradyne;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified planetary noise system - the single source of all terrain noise
 * Uses ONE master SimplexNoiseSampler with different octave/frequency sampling for all layers
 * Ensures smooth, seamless terrain across all scales with no layering artifacts
 */
public class PlanetaryNoiseSystem {
    
    // THE SINGLE MASTER NOISE FUNCTION - all terrain derives from this
    private final SimplexNoiseSampler masterNoise;
    
    // Layer configurations define how to sample the master noise
    private final Map<NoiseLayerConfig.NoiseLayerType, NoiseLayerConfig> layerConfigs;
    
    // Master configuration
    private final UnifiedTerrainConfig terrainConfig;
    private final long planetSeed;
    
    // Performance caching
    private final Map<String, Double> noiseCache;
    private final boolean enableCaching;
    private final int maxCacheSize;
    
    // Coordinate transformation
    private final double globalCoordinateScale;
    
    // Statistics for debugging and optimization
    private long totalSamples = 0;
    private long cacheHits = 0;
    private long cacheMisses = 0;
    
    /**
     * Initialize the planetary noise system with ONE master noise function
     */
    public PlanetaryNoiseSystem(UnifiedTerrainConfig config) {
        this.terrainConfig = config;
        this.planetSeed = config.getSeed();
        this.enableCaching = config.isEnableCaching();
        this.maxCacheSize = config.getChunkRegionCacheSize() * 100;
        
        // Calculate global coordinate scale based on planet size
        this.globalCoordinateScale = calculateGlobalCoordinateScale();
        
        // CREATE THE SINGLE MASTER NOISE FUNCTION
        Random masterRandom = Random.create(planetSeed);
        this.masterNoise = new SimplexNoiseSampler(masterRandom);
        
        // Initialize layer configurations (these define HOW to sample the master noise)
        this.layerConfigs = initializeLayerConfigurations();
        
        // Initialize cache
        this.noiseCache = enableCaching ? new ConcurrentHashMap<>() : null;
        
        // Log initialization
        Terradyne.LOGGER.info("=== UNIFIED PLANETARY NOISE SYSTEM INITIALIZED ===");
        Terradyne.LOGGER.info("Planet: {}", config.getPlanetName());
        Terradyne.LOGGER.info("Seed: {}", planetSeed);
        Terradyne.LOGGER.info("Coordinate Scale: {}", globalCoordinateScale);
        Terradyne.LOGGER.info("Master Noise: Single SimplexNoiseSampler");
        Terradyne.LOGGER.info("Sampling Layers: {}", layerConfigs.size());
        Terradyne.LOGGER.info("Caching: {}", enableCaching ? "Enabled" : "Disabled");
    }
    
    /**
     * Initialize layer configurations optimized for this planet
     * Each layer defines how to sample the SAME master noise at different frequencies
     */
    private Map<NoiseLayerConfig.NoiseLayerType, NoiseLayerConfig> initializeLayerConfigurations() {
        Map<NoiseLayerConfig.NoiseLayerType, NoiseLayerConfig> configs = new HashMap<>();
        
        // Create optimized configurations based on planet characteristics
        // All use the same master noise, just at different frequencies/amplitudes
        
        configs.put(NoiseLayerConfig.NoiseLayerType.CONTINENTAL, 
                NoiseLayerConfig.createContinental(terrainConfig.getEffectiveContinentalScale())
                        .withCoordinateScale(globalCoordinateScale));
        
        configs.put(NoiseLayerConfig.NoiseLayerType.REGIONAL,
                NoiseLayerConfig.createRegional(terrainConfig.getContinentalRoughness())
                        .withCoordinateScale(globalCoordinateScale));
        
        configs.put(NoiseLayerConfig.NoiseLayerType.LOCAL,
                NoiseLayerConfig.createLocal(terrainConfig.getLocalTerrainScale() / 15.0)
                        .withCoordinateScale(globalCoordinateScale));
        
        configs.put(NoiseLayerConfig.NoiseLayerType.DETAIL,
                NoiseLayerConfig.createDetail(terrainConfig.getSurfaceRoughness() / 5.0)
                        .withCoordinateScale(globalCoordinateScale));
        
        configs.put(NoiseLayerConfig.NoiseLayerType.RIDGE,
                NoiseLayerConfig.createRidge(terrainConfig.getMaxVolatilityEffect() / 60.0)
                        .withCoordinateScale(globalCoordinateScale));
        
        configs.put(NoiseLayerConfig.NoiseLayerType.EROSION,
                NoiseLayerConfig.createErosion(terrainConfig.getEffectiveErosionStrength())
                        .withCoordinateScale(globalCoordinateScale));
        
        configs.put(NoiseLayerConfig.NoiseLayerType.TURBULENCE,
                NoiseLayerConfig.createDefault(NoiseLayerConfig.NoiseLayerType.TURBULENCE)
                        .withIntensityScale(terrainConfig.getMicroTerrainIntensity() / 2.0)
                        .withCoordinateScale(globalCoordinateScale));
        
        // Prevent interference between layer frequencies
        NoiseLayerConfig[] configArray = configs.values().toArray(new NoiseLayerConfig[0]);
        NoiseLayerConfig.LayerCoordinator.preventInterference(configArray);
        
        Terradyne.LOGGER.info("Initialized {} layer sampling configurations", configs.size());
        return configs;
    }
    
    /**
     * Calculate global coordinate scale based on planet characteristics
     */
    private double calculateGlobalCoordinateScale() {
        double planetSizeModifier = terrainConfig.getPlanetConfig().getPlanetCircumference() / 4.0;
        return 1.0 / planetSizeModifier; // Larger planets = smaller scale factor
    }
    
    // === CORE MASTER NOISE SAMPLING ===
    
    /**
     * Sample the master noise function directly
     * This is the fundamental noise that all terrain derives from
     */
    public double sampleMasterNoise(double x, double y, double z) {
        totalSamples++;
        return masterNoise.sample(x, y, z);
    }
    
    /**
     * Sample master noise with multiple octaves
     * This is the foundation method that all layer sampling uses
     */
    public double sampleMasterOctaves(double x, double y, double z, int octaves, 
                                     double persistence, double lacunarity, double baseFrequency) {
        double total = 0.0;
        double amplitude = 1.0;
        double frequency = baseFrequency;
        double maxAmplitude = 0.0;
        
        for (int i = 0; i < octaves; i++) {
            // Sample the SAME master noise at different frequencies
            double noiseValue = masterNoise.sample(x * frequency, y * frequency, z * frequency);
            total += noiseValue * amplitude;
            maxAmplitude += amplitude;
            
            amplitude *= persistence;
            frequency *= lacunarity;
        }
        
        // Normalize to maintain consistent amplitude
        return maxAmplitude > 0 ? total / maxAmplitude : 0.0;
    }
    
    // === LAYER SAMPLING METHODS (all use the same master noise) ===
    
    /**
     * Sample a specific layer by sampling the master noise with layer-specific parameters
     */
    public double sampleLayer(NoiseLayerConfig.NoiseLayerType layerType, double x, double y, double z) {
        // Check cache first
        if (enableCaching) {
            String cacheKey = generateCacheKey(layerType, x, y, z);
            Double cachedValue = noiseCache.get(cacheKey);
            if (cachedValue != null) {
                cacheHits++;
                return cachedValue;
            }
            cacheMisses++;
        }
        
        // Get layer configuration
        NoiseLayerConfig config = layerConfigs.get(layerType);
        if (config == null) {
            Terradyne.LOGGER.warn("Missing configuration for layer type: {}", layerType);
            return 0.0;
        }
        
        // Sample the master noise using this layer's parameters
        double result = sampleMasterOctaves(
            x, y, z,
            config.getOctaves(),
            config.getPersistence(),
            config.getLacunarity(),
            config.getEffectiveFrequency()
        );
        
        // Scale by amplitude
        result *= config.getEffectiveAmplitude();
        
        // Apply transformation
        result = config.applyTransform(result);
        
        // Cache result if enabled
        if (enableCaching && noiseCache.size() < maxCacheSize) {
            String cacheKey = generateCacheKey(layerType, x, y, z);
            noiseCache.put(cacheKey, result);
        }
        
        return result;
    }
    
    // === COMBINED SAMPLING METHODS ===
    
    /**
     * Sample continental-scale terrain (combines continental + regional octaves of same master noise)
     */
    public double sampleContinentalTerrain(double x, double z) {
        // Both sample the SAME master noise, just at different frequencies
        double continental = sampleLayer(NoiseLayerConfig.NoiseLayerType.CONTINENTAL, x, 0, z);
        double regional = sampleLayer(NoiseLayerConfig.NoiseLayerType.REGIONAL, x, 0, z);
        
        // Blend based on configuration weights
        NoiseLayerConfig continentalConfig = layerConfigs.get(NoiseLayerConfig.NoiseLayerType.CONTINENTAL);
        NoiseLayerConfig regionalConfig = layerConfigs.get(NoiseLayerConfig.NoiseLayerType.REGIONAL);
        
        double continentalWeight = continentalConfig.getBlendWeight();
        double regionalWeight = regionalConfig.getBlendWeight();
        
        // Normalize weights
        double totalWeight = continentalWeight + regionalWeight;
        if (totalWeight > 0) {
            continentalWeight /= totalWeight;
            regionalWeight /= totalWeight;
        }
        
        return continental * continentalWeight + regional * regionalWeight;
    }
    
    /**
     * Sample local terrain features (local + detail octaves of same master noise)
     */
    public double sampleLocalTerrain(double x, double z) {
        double local = sampleLayer(NoiseLayerConfig.NoiseLayerType.LOCAL, x, 0, z);
        double detail = sampleLayer(NoiseLayerConfig.NoiseLayerType.DETAIL, x, 0, z);
        
        NoiseLayerConfig localConfig = layerConfigs.get(NoiseLayerConfig.NoiseLayerType.LOCAL);
        NoiseLayerConfig detailConfig = layerConfigs.get(NoiseLayerConfig.NoiseLayerType.DETAIL);
        
        double localWeight = localConfig.getBlendWeight();
        double detailWeight = detailConfig.getBlendWeight();
        
        // Normalize weights
        double totalWeight = localWeight + detailWeight;
        if (totalWeight > 0) {
            localWeight /= totalWeight;
            detailWeight /= totalWeight;
        }
        
        return local * localWeight + detail * detailWeight;
    }
    
    /**
     * Sample ridge patterns for mountain generation (ridge-transformed master noise)
     */
    public double sampleRidgePattern(double x, double z) {
        return sampleLayer(NoiseLayerConfig.NoiseLayerType.RIDGE, x, 0, z);
    }
    
    /**
     * Sample erosion patterns (turbulence-transformed master noise)
     */
    public double sampleErosionPattern(double x, double z) {
        return sampleLayer(NoiseLayerConfig.NoiseLayerType.EROSION, x, 0, z);
    }
    
    /**
     * Sample turbulence for chaotic terrain features (absolute-value master noise)
     */
    public double sampleTurbulence(double x, double z, int customOctaves) {
        NoiseLayerConfig turbulenceConfig = layerConfigs.get(NoiseLayerConfig.NoiseLayerType.TURBULENCE);
        
        // Sample master noise with turbulence parameters
        double result = sampleMasterOctaves(
            x, 0, z,
            customOctaves,
            turbulenceConfig.getPersistence(),
            turbulenceConfig.getLacunarity(),
            turbulenceConfig.getEffectiveFrequency()
        );
        
        // Apply turbulence transformation (absolute value)
        return Math.abs(result) * turbulenceConfig.getEffectiveAmplitude();
    }
    
    // === ADVANCED SAMPLING METHODS ===
    
    /**
     * Sample unified multi-scale terrain using the master noise at different frequencies
     */
    public double sampleUnifiedTerrain(double x, double z, 
                                      double continentalWeight, 
                                      double localWeight, 
                                      double ridgeWeight) {
        // All these sample the SAME master noise, just at different frequencies
        double continental = sampleContinentalTerrain(x, z);
        double local = sampleLocalTerrain(x, z);
        double ridge = sampleRidgePattern(x, z);
        
        // Normalize weights
        double totalWeight = continentalWeight + localWeight + ridgeWeight;
        if (totalWeight > 0) {
            continentalWeight /= totalWeight;
            localWeight /= totalWeight;
            ridgeWeight /= totalWeight;
        }
        
        return continental * continentalWeight + 
               local * localWeight + 
               ridge * ridgeWeight;
    }
    
    /**
     * Sample terrain height at world coordinates (main interface for terrain generation)
     * Uses the master noise at multiple coordinated frequencies
     */
    public double sampleTerrainHeight(int worldX, int worldZ) {
        // Transform world coordinates to noise space
        double noiseX = worldToNoiseX(worldX);
        double noiseZ = worldToNoiseZ(worldZ);
        
        // Sample the master noise at multiple scales
        double continental = sampleContinentalTerrain(noiseX, noiseZ);
        double local = sampleLocalTerrain(noiseX, noiseZ);
        
        // Combine additive (not weighted - they're different frequency octaves)
        return continental + local * 0.3; // Local adds 30% variation to continental base
    }
    
    /**
     * Sample with smooth distance-based blending (all from same master noise)
     */
    public double sampleWithFalloff(double x, double z, double centerX, double centerZ, double radius) {
        double distance = TerrainMathUtils.distance2D(x, z, centerX, centerZ);
        
        if (distance > radius) {
            // Far from center - use only continental frequency
            return sampleContinentalTerrain(x, z);
        }
        
        // Close to center - blend continental and local frequencies
        double falloffWeight = TerrainMathUtils.calculateBlendWeight(distance, radius * 0.3, radius);
        double localWeight = 1.0 - falloffWeight;
        
        double continental = sampleContinentalTerrain(x, z);
        double local = sampleLocalTerrain(x, z);
        
        return continental + local * localWeight; // Additive, not replacement
    }
    
    // === SPECIALIZED NOISE TRANSFORMATIONS ===
    
    /**
     * Sample master noise with ridge transformation (for mountain ridges)
     */
    public double sampleRidgeNoise(double x, double z, double frequency, double amplitude) {
        double noise = masterNoise.sample(x * frequency, 0, z * frequency);
        return TerrainMathUtils.ridgeTransform(noise) * amplitude;
    }
    
    /**
     * Sample master noise with billow transformation (for puffy features)
     */
    public double sampleBillowNoise(double x, double z, double frequency, double amplitude) {
        double noise = masterNoise.sample(x * frequency, 0, z * frequency);
        return TerrainMathUtils.billowTransform(noise) * amplitude;
    }
    
    /**
     * Sample master noise with terrace transformation (for mesa-like terrain)
     */
    public double sampleTerraceNoise(double x, double z, double frequency, double amplitude, int steps) {
        double noise = masterNoise.sample(x * frequency, 0, z * frequency);
        return TerrainMathUtils.terraceTransform(noise, steps) * amplitude;
    }
    
    // === COORDINATE TRANSFORMATION UTILITIES ===
    
    /**
     * Transform world coordinates to noise coordinates
     */
    public double worldToNoiseX(int worldX) {
        return TerrainMathUtils.normalizeWorldCoordinate(worldX, globalCoordinateScale);
    }
    
    /**
     * Transform world coordinates to noise coordinates
     */
    public double worldToNoiseZ(int worldZ) {
        return TerrainMathUtils.normalizeWorldCoordinate(worldZ, globalCoordinateScale);
    }
    
    // === LEGACY COMPATIBILITY (all now use the single master noise) ===
    
    /**
     * Legacy compatibility - replaces MasterNoiseProvider.sampleAt()
     */
    public double sampleAt(double x, double y, double z) {
        return sampleMasterNoise(x, y, z);
    }
    
    /**
     * Legacy compatibility - replaces MasterNoiseProvider.sampleOctaves()
     */
    public double sampleOctaves(double x, double y, double z, int octaves, double persistence, double lacunarity) {
        // Use continental layer's base frequency as default
        NoiseLayerConfig continentalConfig = layerConfigs.get(NoiseLayerConfig.NoiseLayerType.CONTINENTAL);
        double baseFrequency = continentalConfig != null ? continentalConfig.getEffectiveFrequency() : 0.0001;
        
        return sampleMasterOctaves(x, y, z, octaves, persistence, lacunarity, baseFrequency);
    }
    
    // === CACHING AND PERFORMANCE ===
    
    /**
     * Generate cache key for noise values
     */
    private String generateCacheKey(NoiseLayerConfig.NoiseLayerType layerType, double x, double y, double z) {
        // Round coordinates to reduce cache key space
        int roundedX = (int)(x * 1000);
        int roundedY = (int)(y * 1000);
        int roundedZ = (int)(z * 1000);
        return layerType.name() + ":" + roundedX + ":" + roundedY + ":" + roundedZ;
    }
    
    /**
     * Clear noise cache
     */
    public void clearCache() {
        if (noiseCache != null) {
            noiseCache.clear();
            Terradyne.LOGGER.debug("Cleared unified noise cache");
        }
    }
    
    /**
     * Get cache statistics for performance monitoring
     */
    public CacheStatistics getCacheStatistics() {
        return new CacheStatistics(totalSamples, cacheHits, cacheMisses, 
                noiseCache != null ? noiseCache.size() : 0);
    }
    
    /**
     * Optimize cache by removing entries
     */
    public void optimizeCache() {
        if (noiseCache != null && noiseCache.size() > maxCacheSize) {
            String[] keys = noiseCache.keySet().toArray(new String[0]);
            for (int i = 0; i < keys.length / 2; i++) {
                noiseCache.remove(keys[i]);
            }
            Terradyne.LOGGER.debug("Optimized unified noise cache, removed {} entries", keys.length / 2);
        }
    }
    
    // === GETTERS ===
    
    public long getSeed() { return planetSeed; }
    public UnifiedTerrainConfig getTerrainConfig() { return terrainConfig; }
    public NoiseLayerConfig getLayerConfig(NoiseLayerConfig.NoiseLayerType layerType) { 
        return layerConfigs.get(layerType); 
    }
    public double getGlobalCoordinateScale() { return globalCoordinateScale; }
    public SimplexNoiseSampler getMasterNoise() { return masterNoise; }
    
    // === UTILITY CLASSES ===
    
    /**
     * Cache statistics for performance monitoring
     */
    public static class CacheStatistics {
        public final long totalSamples;
        public final long cacheHits;
        public final long cacheMisses;
        public final int cacheSize;
        public final double hitRate;
        
        public CacheStatistics(long totalSamples, long cacheHits, long cacheMisses, int cacheSize) {
            this.totalSamples = totalSamples;
            this.cacheHits = cacheHits;
            this.cacheMisses = cacheMisses;
            this.cacheSize = cacheSize;
            this.hitRate = totalSamples > 0 ? (double)cacheHits / totalSamples : 0.0;
        }
        
        @Override
        public String toString() {
            return String.format("CacheStats{samples=%d, hits=%d, misses=%d, size=%d, hitRate=%.2f%%}", 
                    totalSamples, cacheHits, cacheMisses, cacheSize, hitRate * 100);
        }
    }
    
    /**
     * Cleanup method for mod unloading
     */
    public void cleanup() {
        if (noiseCache != null) {
            noiseCache.clear();
        }
        Terradyne.LOGGER.info("Unified planetary noise system cleaned up");
    }
}