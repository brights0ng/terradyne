package net.starlight.terradyne.planet.terrain.config;

import net.starlight.terradyne.planet.terrain.math.TerrainMathUtils;

/**
 * Configuration for individual noise layers in the unified terrain system
 * Manages frequency relationships, amplitude scaling, and octave settings
 * Ensures coordinated noise generation across all scales without interference patterns
 */
public class NoiseLayerConfig {
    
    // === NOISE LAYER TYPES ===
    public enum NoiseLayerType {
        CONTINENTAL("Continental", 0.00001, 40.0),      // Massive continent-scale features
        REGIONAL("Regional", 0.0001, 20.0),             // Large regional variations
        LOCAL("Local", 0.001, 10.0),                    // Local terrain features
        DETAIL("Detail", 0.01, 5.0),                    // Surface detail and texture
        EROSION("Erosion", 0.005, 8.0),                 // Erosion patterns
        RIDGE("Ridge", 0.002, 15.0),                    // Mountain ridges and valleys
        TURBULENCE("Turbulence", 0.02, 3.0);            // Chaotic fine details
        
        private final String displayName;
        private final double defaultFrequency;
        private final double defaultAmplitude;
        
        NoiseLayerType(String displayName, double defaultFrequency, double defaultAmplitude) {
            this.displayName = displayName;
            this.defaultFrequency = defaultFrequency;
            this.defaultAmplitude = defaultAmplitude;
        }
        
        public String getDisplayName() { return displayName; }
        public double getDefaultFrequency() { return defaultFrequency; }
        public double getDefaultAmplitude() { return defaultAmplitude; }
    }
    
    // === NOISE TRANSFORMATION TYPES ===
    public enum NoiseTransform {
        NORMAL,         // Standard noise output
        RIDGE,          // Absolute value for ridges
        BILLOW,         // Squared for puffy features
        TURBULENCE,     // Absolute with multiple octaves
        TERRACE,        // Stepped for mesa-like terrain
        SMOOTH_MIN,     // Valley-like blending
        SMOOTH_MAX      // Mountain-like blending
    }
    
    // Core layer properties
    private final NoiseLayerType layerType;
    private final double baseFrequency;
    private final double baseAmplitude;
    private final int octaves;
    private final double persistence;
    private final double lacunarity;
    private final NoiseTransform transform;
    
    // Advanced properties
    private final double frequencyMultiplier;
    private final double amplitudeMultiplier;
    private final boolean enableNormalization;
    private final double noiseOffset;
    private final double noiseScale;
    
    // Interaction properties
    private final double blendWeight;
    private final double influenceRadius;
    private final boolean enableSmoothing;
    private final double smoothingRadius;
    
    // Performance properties
    private final boolean enableCaching;
    private final int cacheResolution;
    private final boolean useFastMath;
    
    private NoiseLayerConfig(Builder builder) {
        this.layerType = builder.layerType;
        this.baseFrequency = builder.baseFrequency;
        this.baseAmplitude = builder.baseAmplitude;
        this.octaves = builder.octaves;
        this.persistence = builder.persistence;
        this.lacunarity = builder.lacunarity;
        this.transform = builder.transform;
        
        this.frequencyMultiplier = builder.frequencyMultiplier;
        this.amplitudeMultiplier = builder.amplitudeMultiplier;
        this.enableNormalization = builder.enableNormalization;
        this.noiseOffset = builder.noiseOffset;
        this.noiseScale = builder.noiseScale;
        
        this.blendWeight = builder.blendWeight;
        this.influenceRadius = builder.influenceRadius;
        this.enableSmoothing = builder.enableSmoothing;
        this.smoothingRadius = builder.smoothingRadius;
        
        this.enableCaching = builder.enableCaching;
        this.cacheResolution = builder.cacheResolution;
        this.useFastMath = builder.useFastMath;
    }
    
    // === FACTORY METHODS ===
    
    /**
     * Create default configuration for a specific layer type
     */
    public static NoiseLayerConfig createDefault(NoiseLayerType layerType) {
        return new Builder(layerType).build();
    }
    
    /**
     * Create continental noise configuration
     */
    public static NoiseLayerConfig createContinental(double scale) {
        return new Builder(NoiseLayerType.CONTINENTAL)
                .withAmplitudeMultiplier(scale / 40.0)
                .withOctaves(5)
                .withPersistence(0.6)
                .withLacunarity(TerrainMathUtils.GOLDEN_RATIO)
                .withNormalization(true)
                .withCaching(true, 4)
                .build();
    }
    
    /**
     * Create regional variation configuration
     */
    public static NoiseLayerConfig createRegional(double intensity) {
        return new Builder(NoiseLayerType.REGIONAL)
                .withAmplitudeMultiplier(intensity)
                .withOctaves(4)
                .withPersistence(0.5)
                .withLacunarity(2.2)
                .withBlendWeight(0.6)
                .build();
    }
    
    /**
     * Create local terrain configuration
     */
    public static NoiseLayerConfig createLocal(double roughness) {
        return new Builder(NoiseLayerType.LOCAL)
                .withAmplitudeMultiplier(roughness)
                .withOctaves(3)
                .withPersistence(0.4)
                .withTransform(NoiseTransform.BILLOW)
                .withSmoothing(true, 50.0)
                .build();
    }
    
    /**
     * Create detail noise configuration
     */
    public static NoiseLayerConfig createDetail(double intensity) {
        return new Builder(NoiseLayerType.DETAIL)
                .withAmplitudeMultiplier(intensity)
                .withOctaves(2)
                .withPersistence(0.3)
                .withBlendWeight(0.15)
                .build();
    }
    
    /**
     * Create ridge noise for mountains
     */
    public static NoiseLayerConfig createRidge(double strength) {
        return new Builder(NoiseLayerType.RIDGE)
                .withAmplitudeMultiplier(strength)
                .withOctaves(3)
                .withPersistence(0.5)
                .withTransform(NoiseTransform.RIDGE)
                .withBlendWeight(0.8)
                .build();
    }
    
    /**
     * Create erosion pattern configuration
     */
    public static NoiseLayerConfig createErosion(double intensity) {
        return new Builder(NoiseLayerType.EROSION)
                .withAmplitudeMultiplier(intensity)
                .withOctaves(2)
                .withPersistence(0.3)
                .withTransform(NoiseTransform.TURBULENCE)
                .withSmoothing(true, 100.0)
                .build();
    }
    
    // === GETTERS ===
    
    public NoiseLayerType getLayerType() { return layerType; }
    public double getBaseFrequency() { return baseFrequency; }
    public double getBaseAmplitude() { return baseAmplitude; }
    public int getOctaves() { return octaves; }
    public double getPersistence() { return persistence; }
    public double getLacunarity() { return lacunarity; }
    public NoiseTransform getTransform() { return transform; }
    
    public double getFrequencyMultiplier() { return frequencyMultiplier; }
    public double getAmplitudeMultiplier() { return amplitudeMultiplier; }
    public boolean isEnableNormalization() { return enableNormalization; }
    public double getNoiseOffset() { return noiseOffset; }
    public double getNoiseScale() { return noiseScale; }
    
    public double getBlendWeight() { return blendWeight; }
    public double getInfluenceRadius() { return influenceRadius; }
    public boolean isEnableSmoothing() { return enableSmoothing; }
    public double getSmoothingRadius() { return smoothingRadius; }
    
    public boolean isEnableCaching() { return enableCaching; }
    public int getCacheResolution() { return cacheResolution; }
    public boolean isUseFastMath() { return useFastMath; }
    
    // === CALCULATED PROPERTIES ===
    
    /**
     * Get effective frequency for a specific octave
     */
    public double getOctaveFrequency(int octave) {
        return TerrainMathUtils.getOctaveFrequency(getEffectiveFrequency(), octave);
    }
    
    /**
     * Get effective amplitude for a specific octave
     */
    public double getOctaveAmplitude(int octave) {
        return TerrainMathUtils.getOctaveAmplitude(getEffectiveAmplitude(), octave, persistence);
    }
    
    /**
     * Get the effective frequency (base frequency × multiplier)
     */
    public double getEffectiveFrequency() {
        return baseFrequency * frequencyMultiplier;
    }
    
    /**
     * Get the effective amplitude (base amplitude × multiplier)
     */
    public double getEffectiveAmplitude() {
        return baseAmplitude * amplitudeMultiplier;
    }
    
    /**
     * Get the maximum possible amplitude for this layer (for normalization)
     */
    public double getMaxAmplitude() {
        if (enableNormalization) {
            return TerrainMathUtils.getTotalAmplitude(octaves, persistence) * getEffectiveAmplitude();
        }
        return getEffectiveAmplitude();
    }
    
    /**
     * Apply noise transformation to a raw noise value
     */
    public double applyTransform(double noiseValue) {
        double transformed = switch (transform) {
            case NORMAL -> noiseValue;
            case RIDGE -> TerrainMathUtils.ridgeTransform(noiseValue);
            case BILLOW -> TerrainMathUtils.billowTransform(noiseValue);
            case TURBULENCE -> TerrainMathUtils.turbulence(noiseValue, 1.0);
            case TERRACE -> TerrainMathUtils.terraceTransform(noiseValue, 8);
            case SMOOTH_MIN -> noiseValue; // Applied externally with another value
            case SMOOTH_MAX -> noiseValue; // Applied externally with another value
        };
        
        // Apply offset and scaling
        return (transformed + noiseOffset) * noiseScale;
    }
    
    /**
     * Check if this layer should be cached at the given resolution
     */
    public boolean shouldCache(int targetResolution) {
        return enableCaching && (cacheResolution >= targetResolution);
    }
    
    /**
     * Get the weight for blending this layer with others
     */
    public double getEffectiveBlendWeight(double distance) {
        if (influenceRadius <= 0) return blendWeight;
        
        double falloff = TerrainMathUtils.exponentialFalloff(distance, influenceRadius, 1.0);
        return blendWeight * falloff;
    }
    
    // === UTILITY METHODS ===
    
    /**
     * Create a coordinate-scaled version of this configuration
     */
    public NoiseLayerConfig withCoordinateScale(double scale) {
        return new Builder(this)
                .withFrequencyMultiplier(frequencyMultiplier / scale)
                .build();
    }
    
    /**
     * Create an intensity-scaled version of this configuration
     */
    public NoiseLayerConfig withIntensityScale(double scale) {
        return new Builder(this)
                .withAmplitudeMultiplier(amplitudeMultiplier * scale)
                .build();
    }
    
    // === BUILDER PATTERN ===
    
    public static class Builder {
        private final NoiseLayerType layerType;
        private double baseFrequency;
        private double baseAmplitude;
        private int octaves = 3;
        private double persistence = 0.5;
        private double lacunarity = 2.0;
        private NoiseTransform transform = NoiseTransform.NORMAL;
        
        private double frequencyMultiplier = 1.0;
        private double amplitudeMultiplier = 1.0;
        private boolean enableNormalization = true;
        private double noiseOffset = 0.0;
        private double noiseScale = 1.0;
        
        private double blendWeight = 1.0;
        private double influenceRadius = 0.0;
        private boolean enableSmoothing = false;
        private double smoothingRadius = 100.0;
        
        private boolean enableCaching = false;
        private int cacheResolution = 1;
        private boolean useFastMath = false;
        
        public Builder(NoiseLayerType layerType) {
            this.layerType = layerType;
            this.baseFrequency = layerType.getDefaultFrequency();
            this.baseAmplitude = layerType.getDefaultAmplitude();
        }
        
        // Copy constructor
        public Builder(NoiseLayerConfig config) {
            this.layerType = config.layerType;
            this.baseFrequency = config.baseFrequency;
            this.baseAmplitude = config.baseAmplitude;
            this.octaves = config.octaves;
            this.persistence = config.persistence;
            this.lacunarity = config.lacunarity;
            this.transform = config.transform;
            
            this.frequencyMultiplier = config.frequencyMultiplier;
            this.amplitudeMultiplier = config.amplitudeMultiplier;
            this.enableNormalization = config.enableNormalization;
            this.noiseOffset = config.noiseOffset;
            this.noiseScale = config.noiseScale;
            
            this.blendWeight = config.blendWeight;
            this.influenceRadius = config.influenceRadius;
            this.enableSmoothing = config.enableSmoothing;
            this.smoothingRadius = config.smoothingRadius;
            
            this.enableCaching = config.enableCaching;
            this.cacheResolution = config.cacheResolution;
            this.useFastMath = config.useFastMath;
        }
        
        // Core noise properties
        public Builder withFrequency(double frequency) { this.baseFrequency = frequency; return this; }
        public Builder withAmplitude(double amplitude) { this.baseAmplitude = amplitude; return this; }
        public Builder withOctaves(int octaves) { this.octaves = Math.max(1, octaves); return this; }
        public Builder withPersistence(double persistence) { this.persistence = TerrainMathUtils.clamp01(persistence); return this; }
        public Builder withLacunarity(double lacunarity) { this.lacunarity = Math.max(1.0, lacunarity); return this; }
        public Builder withTransform(NoiseTransform transform) { this.transform = transform; return this; }
        
        // Multipliers
        public Builder withFrequencyMultiplier(double multiplier) { this.frequencyMultiplier = multiplier; return this; }
        public Builder withAmplitudeMultiplier(double multiplier) { this.amplitudeMultiplier = multiplier; return this; }
        
        // Normalization and scaling
        public Builder withNormalization(boolean enable) { this.enableNormalization = enable; return this; }
        public Builder withOffset(double offset) { this.noiseOffset = offset; return this; }
        public Builder withScale(double scale) { this.noiseScale = scale; return this; }
        
        // Blending
        public Builder withBlendWeight(double weight) { this.blendWeight = TerrainMathUtils.clamp01(weight); return this; }
        public Builder withInfluenceRadius(double radius) { this.influenceRadius = Math.max(0, radius); return this; }
        
        // Smoothing
        public Builder withSmoothing(boolean enable, double radius) { 
            this.enableSmoothing = enable; 
            this.smoothingRadius = radius; 
            return this; 
        }
        
        // Performance
        public Builder withCaching(boolean enable, int resolution) { 
            this.enableCaching = enable; 
            this.cacheResolution = Math.max(1, resolution); 
            return this; 
        }
        public Builder withFastMath(boolean enable) { this.useFastMath = enable; return this; }
        
        public NoiseLayerConfig build() {
            return new NoiseLayerConfig(this);
        }
    }
    
    // === UTILITY CLASS FOR COORDINATING MULTIPLE LAYERS ===
    
    /**
     * Utility class for managing relationships between noise layers
     */
    public static class LayerCoordinator {
        
        /**
         * Ensure frequency separation between layers to prevent interference
         */
        public static void preventInterference(NoiseLayerConfig... layers) {
            for (int i = 0; i < layers.length - 1; i++) {
                for (int j = i + 1; j < layers.length; j++) {
                    double freq1 = layers[i].getEffectiveFrequency();
                    double freq2 = layers[j].getEffectiveFrequency();
                    
                    // Ensure frequencies are separated by at least golden ratio
                    double ratio = Math.max(freq1, freq2) / Math.min(freq1, freq2);
                    if (ratio < TerrainMathUtils.GOLDEN_RATIO) {
                        // Adjust the higher frequency layer
                        if (freq1 > freq2) {
                            layers[i] = layers[i].withCoordinateScale(1.0 / TerrainMathUtils.GOLDEN_RATIO);
                        } else {
                            layers[j] = layers[j].withCoordinateScale(1.0 / TerrainMathUtils.GOLDEN_RATIO);
                        }
                    }
                }
            }
        }
        
        /**
         * Normalize amplitude weights so they sum to 1.0
         */
        public static NoiseLayerConfig[] normalizeWeights(NoiseLayerConfig... layers) {
            double totalWeight = 0.0;
            for (NoiseLayerConfig layer : layers) {
                totalWeight += layer.getBlendWeight();
            }
            
            if (totalWeight > 0) {
                NoiseLayerConfig[] normalized = new NoiseLayerConfig[layers.length];
                for (int i = 0; i < layers.length; i++) {
                    double normalizedWeight = layers[i].getBlendWeight() / totalWeight;
                    normalized[i] = new Builder(layers[i])
                            .withBlendWeight(normalizedWeight)
                            .build();
                }
                return normalized;
            }
            
            return layers;
        }
    }
}