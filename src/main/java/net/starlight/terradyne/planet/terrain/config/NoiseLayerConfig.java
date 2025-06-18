// NoiseLayerConfig.java
package net.starlight.terradyne.planet.terrain.config;

/**
 * Configuration for individual noise layers in the unified terrain generation system.
 * Each layer represents a different scale of terrain features, from continental-scale
 * down to fine surface detail.
 */
public class NoiseLayerConfig {
    
    private final LayerType layerType;
    private final boolean enabled;
    private final double frequency;
    private final double amplitude;
    private final double warpStrength;
    private final BlendMode blendMode;
    private final ShapeMode shapeMode;
    private final double blendWeight;
    private final int octaves;
    private final double persistence;
    private final double lacunarity;
    
    private NoiseLayerConfig(Builder builder) {
        this.layerType = builder.layerType;
        this.enabled = builder.enabled;
        this.frequency = builder.frequency;
        this.amplitude = builder.amplitude;
        this.warpStrength = builder.warpStrength;
        this.blendMode = builder.blendMode;
        this.shapeMode = builder.shapeMode;
        this.blendWeight = builder.blendWeight;
        this.octaves = builder.octaves;
        this.persistence = builder.persistence;
        this.lacunarity = builder.lacunarity;
    }
    
    // Getters
    public LayerType getLayerType() { return layerType; }
    public boolean isEnabled() { return enabled; }
    public double getFrequency() { return frequency; }
    public double getAmplitude() { return amplitude; }
    public double getWarpStrength() { return warpStrength; }
    public BlendMode getBlendMode() { return blendMode; }
    public ShapeMode getShapeMode() { return shapeMode; }
    public double getBlendWeight() { return blendWeight; }
    public int getOctaves() { return octaves; }
    public double getPersistence() { return persistence; }
    public double getLacunarity() { return lacunarity; }
    
    /**
     * Types of terrain layers, from largest to smallest scale
     */
    public enum LayerType {
        CONTINENTAL,    // Major landmass shape (500-1000 block frequency)
        MOUNTAIN_RANGES, // Large elevation features (200-400 block frequency)
        HILLS,          // Medium terrain variation (50-100 block frequency)
        VALLEYS,        // Local terrain carving (20-40 block frequency)
        SURFACE_DETAIL  // Fine surface roughness (5-10 block frequency)
    }
    
    /**
     * How this layer blends with the previous layers
     */
    public enum BlendMode {
        DOMAIN_WARP,    // Use this layer to warp coordinates for next layer (primary mode)
        WEIGHTED,       // Linear blend with weight
        OVERLAY,        // Complex overlay blend for natural combinations
        MULTIPLICATIVE, // Multiply for masking effects
        POWER,          // Power blend for sharp features
        REPLACE         // Completely replace previous value (rarely used)
    }
    
    /**
     * How to shape the raw noise values
     */
    public enum ShapeMode {
        NORMAL,         // Use raw noise values
        RIDGED,         // Create sharp ridges (good for mountains)
        BILLOWY,        // Create soft, puffy terrain (good for hills)
        TERRACE,        // Create stepped terrain (sedimentary layers)
        PLATEAU,        // Flatten high areas (mesa effect)
        EXPONENTIAL     // Emphasize peaks with exponential curve
    }
    
    /**
     * Create default layer configurations for different planet types
     */
    public static class DefaultConfigs {
        
        /**
         * Create default continental layer configuration
         */
        public static NoiseLayerConfig continental() {
            return new Builder(LayerType.CONTINENTAL)
                .frequency(750.0)           // Large features
                .amplitude(1.0)             // Full amplitude
                .warpStrength(50.0)         // Moderate warping
                .blendMode(BlendMode.DOMAIN_WARP)
                .shapeMode(ShapeMode.NORMAL)
                .blendWeight(1.0)
                .octaves(3)
                .persistence(0.5)
                .lacunarity(2.0)
                .build();
        }
        
        /**
         * Create default mountain range layer configuration
         */
        public static NoiseLayerConfig mountainRanges() {
            return new Builder(LayerType.MOUNTAIN_RANGES)
                .frequency(300.0)           // Medium-large features
                .amplitude(0.8)             // Strong contribution
                .warpStrength(30.0)         // Strong warping for dramatic features
                .blendMode(BlendMode.DOMAIN_WARP)
                .shapeMode(ShapeMode.RIDGED) // Sharp mountain ridges
                .blendWeight(0.7)
                .octaves(4)
                .persistence(0.6)
                .lacunarity(2.1)
                .build();
        }
        
        /**
         * Create default hills layer configuration
         */
        public static NoiseLayerConfig hills() {
            return new Builder(LayerType.HILLS)
                .frequency(75.0)            // Medium features
                .amplitude(0.5)             // Moderate contribution
                .warpStrength(15.0)         // Gentle warping
                .blendMode(BlendMode.OVERLAY)
                .shapeMode(ShapeMode.BILLOWY) // Soft, rolling hills
                .blendWeight(0.5)
                .octaves(3)
                .persistence(0.5)
                .lacunarity(2.0)
                .build();
        }
        
        /**
         * Create default valleys layer configuration
         */
        public static NoiseLayerConfig valleys() {
            return new Builder(LayerType.VALLEYS)
                .frequency(30.0)            // Smaller features
                .amplitude(0.4)             // Moderate contribution
                .warpStrength(10.0)         // Subtle warping
                .blendMode(BlendMode.MULTIPLICATIVE) // Carve into existing terrain
                .shapeMode(ShapeMode.NORMAL)
                .blendWeight(0.6)
                .octaves(2)
                .persistence(0.4)
                .lacunarity(2.2)
                .build();
        }
        
        /**
         * Create default surface detail layer configuration
         */
        public static NoiseLayerConfig surfaceDetail() {
            return new Builder(LayerType.SURFACE_DETAIL)
                .frequency(7.5)             // Fine details
                .amplitude(0.2)             // Small contribution
                .warpStrength(2.0)          // Minimal warping
                .blendMode(BlendMode.WEIGHTED)
                .shapeMode(ShapeMode.NORMAL)
                .blendWeight(0.3)
                .octaves(2)
                .persistence(0.3)
                .lacunarity(2.5)
                .build();
        }
        
        /**
         * Create a complete set of default layers
         */
        public static NoiseLayerConfig[] createDefaultLayerSet() {
            return new NoiseLayerConfig[] {
                continental(),
                mountainRanges(),
                hills(),
                valleys(),
                surfaceDetail()
            };
        }
    }
    
    /**
     * Builder for creating customized layer configurations
     */
    public static class Builder {
        private final LayerType layerType;
        private boolean enabled = true;
        private double frequency = 100.0;
        private double amplitude = 0.5;
        private double warpStrength = 10.0;
        private BlendMode blendMode = BlendMode.DOMAIN_WARP;
        private ShapeMode shapeMode = ShapeMode.NORMAL;
        private double blendWeight = 0.5;
        private int octaves = 3;
        private double persistence = 0.5;
        private double lacunarity = 2.0;
        
        public Builder(LayerType layerType) {
            this.layerType = layerType;
        }
        
        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }
        
        public Builder frequency(double frequency) {
            this.frequency = frequency;
            return this;
        }
        
        public Builder amplitude(double amplitude) {
            this.amplitude = amplitude;
            return this;
        }
        
        public Builder warpStrength(double warpStrength) {
            this.warpStrength = warpStrength;
            return this;
        }
        
        public Builder blendMode(BlendMode blendMode) {
            this.blendMode = blendMode;
            return this;
        }
        
        public Builder shapeMode(ShapeMode shapeMode) {
            this.shapeMode = shapeMode;
            return this;
        }
        
        public Builder blendWeight(double blendWeight) {
            this.blendWeight = blendWeight;
            return this;
        }
        
        public Builder octaves(int octaves) {
            this.octaves = octaves;
            return this;
        }
        
        public Builder persistence(double persistence) {
            this.persistence = persistence;
            return this;
        }
        
        public Builder lacunarity(double lacunarity) {
            this.lacunarity = lacunarity;
            return this;
        }
        
        public NoiseLayerConfig build() {
            return new NoiseLayerConfig(this);
        }
    }
    
    /**
     * Modify this layer config based on planet parameters
     */
    public NoiseLayerConfig adjustForPlanet(int circumference, int crustalThickness, 
                                           double tectonicActivity, double waterErosion) {
        Builder builder = new Builder(this.layerType)
            .enabled(this.enabled)
            .blendMode(this.blendMode)
            .shapeMode(this.shapeMode)
            .blendWeight(this.blendWeight)
            .octaves(this.octaves)
            .persistence(this.persistence)
            .lacunarity(this.lacunarity);
        
        // Adjust frequency based on planet size
        double sizeScale = circumference / 40000.0; // Relative to Earth-like
        double adjustedFrequency = this.frequency * sizeScale;
        
        // Adjust amplitude based on crustal thickness
        double thicknessScale = crustalThickness / 300.0; // Relative to default
        double adjustedAmplitude = this.amplitude * thicknessScale;
        
        // Adjust warp strength based on tectonic activity
        double tectonicScale = 0.5 + (tectonicActivity * 0.5); // 0.5 to 1.0 range
        double adjustedWarpStrength = this.warpStrength * tectonicScale;
        
        // Apply layer-specific adjustments
        switch (this.layerType) {
            case CONTINENTAL:
                // Continental features scale most with planet size
                adjustedFrequency *= 1.2;
                break;
            case MOUNTAIN_RANGES:
                // Mountains are most affected by tectonics
                adjustedAmplitude *= (1.0 + tectonicActivity * 0.5);
                adjustedWarpStrength *= (1.0 + tectonicActivity * 0.3);
                break;
            case HILLS:
                // Hills are moderately affected by erosion
                adjustedAmplitude *= (1.0 - waterErosion * 0.2);
                break;
            case VALLEYS:
                // Valleys are carved by water erosion
                adjustedAmplitude *= (1.0 + waterErosion * 0.4);
                break;
            case SURFACE_DETAIL:
                // Surface detail is most affected by erosion
                adjustedAmplitude *= (1.0 - waterErosion * 0.3);
                adjustedWarpStrength *= (1.0 - waterErosion * 0.4);
                break;
        }
        
        return builder
            .frequency(adjustedFrequency)
            .amplitude(adjustedAmplitude)
            .warpStrength(adjustedWarpStrength)
            .build();
    }
    
    /**
     * Debug string representation
     */
    @Override
    public String toString() {
        return String.format("%s: freq=%.1f, amp=%.2f, warp=%.1f, %s+%s", 
            layerType, frequency, amplitude, warpStrength, blendMode, shapeMode);
    }
    
    /**
     * Create a disabled version of this layer
     */
    public NoiseLayerConfig disabled() {
        return new Builder(this.layerType)
            .enabled(false)
            .frequency(this.frequency)
            .amplitude(this.amplitude)
            .warpStrength(this.warpStrength)
            .blendMode(this.blendMode)
            .shapeMode(this.shapeMode)
            .blendWeight(this.blendWeight)
            .octaves(this.octaves)
            .persistence(this.persistence)
            .lacunarity(this.lacunarity)
            .build();
    }
}