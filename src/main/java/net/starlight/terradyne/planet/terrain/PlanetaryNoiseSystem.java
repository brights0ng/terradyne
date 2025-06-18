// PlanetaryNoiseSystem.java
package net.starlight.terradyne.planet.terrain;

import net.minecraft.util.math.noise.SimplexNoiseSampler;
import net.minecraft.util.math.random.Random;
import net.starlight.terradyne.planet.physics.PlanetData;
import net.starlight.terradyne.planet.terrain.config.MasterTerrainConfig;
import net.starlight.terradyne.planet.terrain.config.NoiseLayerConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * Unified noise generation system for realistic planetary terrain.
 * Uses domain warping to create flowing, natural terrain features
 * instead of obvious additive noise layers.
 */
public class PlanetaryNoiseSystem {
    
    private final MasterTerrainConfig terrainConfig;
    private final PlanetData planetData;
    private final long seed;
    
    // Noise samplers for each layer type
    private final Map<NoiseLayerConfig.LayerType, LayerNoiseSampler> layerSamplers;
    
    // Additional noise samplers for domain warping
    private final SimplexNoiseSampler warpNoiseX;
    private final SimplexNoiseSampler warpNoiseZ;
    
    // Performance caching
    private final Map<Long, Double> noiseCache;
    private static final int MAX_CACHE_SIZE = 10000;
    
    public PlanetaryNoiseSystem(MasterTerrainConfig terrainConfig) {
        this.terrainConfig = terrainConfig;
        this.planetData = terrainConfig.getPlanetData();
        this.seed = terrainConfig.getNoiseSeed();
        
        // Initialize noise samplers for each layer
        this.layerSamplers = new HashMap<>();
        initializeLayerSamplers();
        
        // Initialize domain warp noise samplers
        Random warpRandom1 = Random.create(seed + 1000L);
        Random warpRandom2 = Random.create(seed + 2000L);
        this.warpNoiseX = new SimplexNoiseSampler(warpRandom1);
        this.warpNoiseZ = new SimplexNoiseSampler(warpRandom2);
        
        // Initialize cache
        this.noiseCache = new HashMap<>();
        
        System.out.println("Initialized PlanetaryNoiseSystem for " + planetData.getPlanetName());
        System.out.println("Active layers: " + terrainConfig.getEnabledLayers().length);
    }
    
    /**
     * Initialize noise samplers for each configured layer
     */
    private void initializeLayerSamplers() {
        NoiseLayerConfig[] layers = terrainConfig.getNoiseLayers();
        
        for (NoiseLayerConfig layer : layers) {
            if (layer.isEnabled()) {
                LayerNoiseSampler sampler = new LayerNoiseSampler(layer, seed);
                layerSamplers.put(layer.getLayerType(), sampler);
                
                System.out.println("  Initialized " + layer.getLayerType() + 
                                 " layer: freq=" + layer.getFrequency() + 
                                 ", amp=" + layer.getAmplitude());
            }
        }
    }
    
    /**
     * Main terrain sampling method - returns unified noise value for given coordinates
     */
    public double sampleTerrain(int x, int z) {
        // Check cache first
        long cacheKey = ((long) x << 32) | (z & 0xffffffffL);
        if (noiseCache.containsKey(cacheKey)) {
            return noiseCache.get(cacheKey);
        }
        
        // Apply global coordinate scaling
        double scaledX = x * terrainConfig.getModifiers().coordinateScale;
        double scaledZ = z * terrainConfig.getModifiers().coordinateScale;
        
        // Process through unified noise chain with domain warping
        double result = sampleUnifiedNoise(scaledX, scaledZ);
        
        // Cache result if cache isn't too large
        if (noiseCache.size() < MAX_CACHE_SIZE) {
            noiseCache.put(cacheKey, result);
        }
        
        return result;
    }
    
    /**
     * Core unified noise sampling with domain warping
     */
    private double sampleUnifiedNoise(double x, double z) {
        // Start with coordinates
        double currentX = x;
        double currentZ = z;
        double accumulatedNoise = 0.0;
        double totalWeight = 0.0;
        
        // Process each enabled layer in sequence
        NoiseLayerConfig[] enabledLayers = terrainConfig.getEnabledLayers();
        
        for (int i = 0; i < enabledLayers.length; i++) {
            NoiseLayerConfig layer = enabledLayers[i];
            LayerNoiseSampler sampler = layerSamplers.get(layer.getLayerType());
            
            if (sampler == null) continue;
            
            // Sample current layer at current coordinates
            double layerNoise = sampler.sampleLayer(currentX, currentZ);
            
            // Apply layer blending based on blend mode
            switch (layer.getBlendMode()) {
                case DOMAIN_WARP:
                    // Use this layer to warp coordinates for next layer
                    if (i < enabledLayers.length - 1) { // Don't warp on last layer
                        TerrainMathUtils.CoordinatePair warpedCoords = applyDomainWarp(
                            currentX, currentZ, layerNoise, layer.getWarpStrength()
                        );
                        currentX = warpedCoords.x;
                        currentZ = warpedCoords.z;
                    }
                    
                    // Also contribute to final result
                    accumulatedNoise = TerrainMathUtils.LayerBlending.weightedBlend(
                        accumulatedNoise, layerNoise, layer.getBlendWeight()
                    );
                    totalWeight += layer.getBlendWeight();
                    break;
                    
                case WEIGHTED:
                    accumulatedNoise = TerrainMathUtils.LayerBlending.weightedBlend(
                        accumulatedNoise, layerNoise, layer.getBlendWeight()
                    );
                    totalWeight += layer.getBlendWeight();
                    break;
                    
                case OVERLAY:
                    accumulatedNoise = TerrainMathUtils.LayerBlending.overlayBlend(
                        accumulatedNoise, layerNoise, layer.getBlendWeight()
                    );
                    break;
                    
                case MULTIPLICATIVE:
                    accumulatedNoise = TerrainMathUtils.LayerBlending.multiplicativeBlend(
                        accumulatedNoise, layerNoise, layer.getBlendWeight()
                    );
                    break;
                    
                case POWER:
                    accumulatedNoise = TerrainMathUtils.LayerBlending.powerBlend(
                        accumulatedNoise, layerNoise, layer.getBlendWeight()
                    );
                    break;
                    
                case REPLACE:
                    accumulatedNoise = layerNoise;
                    totalWeight = 1.0;
                    break;
            }
        }
        
        // Normalize if we have weighted blending
        if (totalWeight > 0.0 && totalWeight != 1.0) {
            accumulatedNoise /= totalWeight;
        }
        
        // Apply final terrain modifiers
        accumulatedNoise = applyFinalModifiers(accumulatedNoise);
        
        // Ensure result stays in -1 to 1 range
        return TerrainMathUtils.clamp(accumulatedNoise, -1.0, 1.0);
    }
    
    /**
     * Apply domain warping to coordinates based on noise value
     */
    private TerrainMathUtils.CoordinatePair applyDomainWarp(double x, double z, double noiseValue, double warpStrength) {
        // Sample additional warp noise at current coordinates
        double warpX = warpNoiseX.sample(x * 0.01, z * 0.01); // Low frequency for broad warping
        double warpZ = warpNoiseZ.sample(x * 0.01, z * 0.01);
        
        // Combine primary noise with warp noise
        double combinedWarpX = (noiseValue * 0.7) + (warpX * 0.3);
        double combinedWarpZ = (noiseValue * 0.7) + (warpZ * 0.3);
        
        // Apply warping
        return TerrainMathUtils.DomainWarp.warpCoordinates(
            x, z, warpStrength, combinedWarpX, combinedWarpZ
        );
    }
    
    /**
     * Apply final terrain modifiers based on planet properties
     */
    private double applyFinalModifiers(double noiseValue) {
        // Apply tectonic scaling
        double tectonicScale = terrainConfig.getModifiers().tectonicScale;
        noiseValue *= tectonicScale;
        
        // Apply erosion smoothing
        double erosionScale = terrainConfig.getModifiers().erosionScale;
        if (erosionScale < 1.0) {
            // Smooth the noise based on erosion
            double smoothingFactor = 1.0 - erosionScale;
            noiseValue = TerrainMathUtils.smoothLerp(noiseValue, 0.0, smoothingFactor * 0.3);
        }
        
        return noiseValue;
    }
    
    /**
     * Sample individual layer for debugging/testing
     */
    public double sampleLayer(NoiseLayerConfig.LayerType layerType, int x, int z) {
        LayerNoiseSampler sampler = layerSamplers.get(layerType);
        if (sampler == null) return 0.0;
        
        double scaledX = x * terrainConfig.getModifiers().coordinateScale;
        double scaledZ = z * terrainConfig.getModifiers().coordinateScale;
        
        return sampler.sampleLayer(scaledX, scaledZ);
    }
    
    /**
     * Sample temperature variation for biome placement (future use)
     */
    public double sampleTemperature(int x, int z) {
        // Simple temperature gradient based on Z coordinate (latitude effect)
        double latitudeEffect = Math.sin(z * 0.001) * 0.5; // -0.5 to 0.5
        
        // Add some noise variation
        double tempNoise = warpNoiseX.sample(x * 0.005, z * 0.005) * 0.3;
        
        return TerrainMathUtils.clamp(latitudeEffect + tempNoise, -1.0, 1.0);
    }
    
    /**
     * Sample moisture variation for biome placement (future use)
     */
    public double sampleMoisture(int x, int z) {
        // Simple moisture pattern with some noise
        double moistureNoise = warpNoiseZ.sample(x * 0.003, z * 0.003);
        
        // Modify based on planet water content
        double waterContentEffect = (planetData.getAdjustedWaterContent() - 0.5) * 2.0; // -1 to 1
        
        return TerrainMathUtils.clamp(moistureNoise + waterContentEffect * 0.5, -1.0, 1.0);
    }
    
    /**
     * Clear the noise cache (useful for testing or memory management)
     */
    public void clearCache() {
        noiseCache.clear();
        System.out.println("Cleared noise cache for " + planetData.getPlanetName());
    }
    
    /**
     * Get cache statistics for debugging
     */
    public String getCacheStats() {
        return String.format("Cache: %d/%d entries", noiseCache.size(), MAX_CACHE_SIZE);
    }
    
    /**
     * Get debug information about the noise system
     */
    public String getDebugInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== PLANETARY NOISE SYSTEM DEBUG ===\n");
        sb.append("Planet: ").append(planetData.getPlanetName()).append("\n");
        sb.append("Seed: ").append(seed).append("\n");
        sb.append("Coordinate Scale: ").append(terrainConfig.getModifiers().coordinateScale).append("\n");
        sb.append("Active Samplers: ").append(layerSamplers.size()).append("\n");
        sb.append(getCacheStats()).append("\n");
        sb.append("\nLayer Details:\n");
        
        for (NoiseLayerConfig layer : terrainConfig.getEnabledLayers()) {
            LayerNoiseSampler sampler = layerSamplers.get(layer.getLayerType());
            if (sampler != null) {
                sb.append("  ").append(layer.getLayerType())
                  .append(": ").append(layer.toString()).append("\n");
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Individual layer noise sampler with octave support
     */
    private static class LayerNoiseSampler {
        private final NoiseLayerConfig config;
        private final SimplexNoiseSampler[] octaveSamplers;
        
        public LayerNoiseSampler(NoiseLayerConfig config, long baseSeed) {
            this.config = config;
            this.octaveSamplers = new SimplexNoiseSampler[config.getOctaves()];
            
            // Create samplers for each octave with different seeds
            for (int i = 0; i < config.getOctaves(); i++) {
                long octaveSeed = baseSeed + (config.getLayerType().ordinal() * 1000L) + (i * 100L);
                Random octaveRandom = Random.create(octaveSeed);
                octaveSamplers[i] = new SimplexNoiseSampler(octaveRandom);
            }
        }
        
        /**
         * Sample this layer with proper octave combination
         */
        public double sampleLayer(double x, double z) {
            // Scale coordinates by frequency
            TerrainMathUtils.CoordinatePair scaledCoords = 
                TerrainMathUtils.NoiseCoordinates.scaleByFrequency(x, z, config.getFrequency());
            
            double result = 0.0;
            double amplitude = config.getAmplitude();
            double frequency = 1.0;
            double maxValue = 0.0; // For normalization
            
            // Combine octaves
            for (int i = 0; i < octaveSamplers.length; i++) {
                double octaveValue = octaveSamplers[i].sample(
                    scaledCoords.x * frequency, 
                    scaledCoords.z * frequency
                );
                
                result += octaveValue * amplitude;
                maxValue += amplitude;
                
                // Prepare for next octave
                amplitude *= config.getPersistence();
                frequency *= config.getLacunarity();
            }
            
            // Normalize to maintain -1 to 1 range
            if (maxValue > 0.0) {
                result /= maxValue;
            }
            
            // Apply shape mode
            result = applyShapeMode(result);
            
            return TerrainMathUtils.clamp(result, -1.0, 1.0);
        }
        
        /**
         * Apply shape mode transformation to noise value
         */
        private double applyShapeMode(double noiseValue) {
            switch (config.getShapeMode()) {
                case RIDGED:
                    return TerrainMathUtils.TerrainShaping.ridgedNoise(noiseValue);
                    
                case BILLOWY:
                    return TerrainMathUtils.TerrainShaping.billowyNoise(noiseValue);
                    
                case TERRACE:
                    return TerrainMathUtils.TerrainShaping.terraceNoise(noiseValue, 8); // 8 steps
                    
                case PLATEAU:
                    return TerrainMathUtils.TerrainShaping.plateauEffect(noiseValue, 0.3, 0.5);
                    
                case EXPONENTIAL:
                    return TerrainMathUtils.TerrainShaping.exponentialCurve(noiseValue, 2.0);
                    
                case NORMAL:
                default:
                    return noiseValue;
            }
        }
    }
}