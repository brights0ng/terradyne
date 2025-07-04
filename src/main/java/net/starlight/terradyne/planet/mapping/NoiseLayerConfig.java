package net.starlight.terradyne.planet.mapping;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for individual noise processing layers
 * Allows fine-tuning of each layer's behavior within noise maps
 */
public class NoiseLayerConfig {
    
    private final String layerName;
    private double frequency;          // Noise scale (smaller = larger features)
    private double amplitude;          // Layer strength/intensity
    private int octaves;              // Detail level (more octaves = more detail)
    private BlendMode blendMode;      // How layer combines with others
    private boolean enabled;          // Layer activation
    private final Map<String, Object> layerSpecificParams; // Custom parameters per layer type

    /**
     * Blending modes for combining noise layers
     */
    public enum BlendMode {
        ADD,           // Simple addition (base + layer)
        MULTIPLY,      // Multiplication (base * layer) 
        OVERLAY,       // Soft light blending for natural transitions
        DOMAIN_WARP,   // Layer affects sampling coordinates of base
        HARD_MIN,      // Take minimum value (for valleys/carving)
        HARD_MAX,      // Take maximum value (for peaks/plateaus)
        SOFT_LIGHT,    // Gentle overlay for smooth gradients
        RIDGE          // Absolute value for ridge/valley effects
    }

    /**
     * Create a noise layer configuration
     */
    public NoiseLayerConfig(String layerName) {
        this.layerName = layerName;
        this.frequency = 0.001;           // Default: large features
        this.amplitude = 1.0;             // Default: normal strength
        this.octaves = 1;                 // Default: single octave
        this.blendMode = BlendMode.ADD;   // Default: simple addition
        this.enabled = true;              // Default: enabled
        this.layerSpecificParams = new HashMap<>();
    }

    // === BUILDER-STYLE SETTERS ===

    public NoiseLayerConfig setFrequency(double frequency) {
        this.frequency = frequency;
        return this;
    }

    public NoiseLayerConfig setAmplitude(double amplitude) {
        this.amplitude = amplitude;
        return this;
    }

    public NoiseLayerConfig setOctaves(int octaves) {
        this.octaves = Math.max(1, Math.min(8, octaves)); // Clamp 1-8
        return this;
    }

    public NoiseLayerConfig setBlendMode(BlendMode blendMode) {
        this.blendMode = blendMode;
        return this;
    }

    public NoiseLayerConfig setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public NoiseLayerConfig setParameter(String key, Object value) {
        layerSpecificParams.put(key, value);
        return this;
    }

    // === GETTERS ===

    public String getLayerName() { return layerName; }
    public double getFrequency() { return frequency; }
    public double getAmplitude() { return amplitude; }
    public int getOctaves() { return octaves; }
    public BlendMode getBlendMode() { return blendMode; }
    public boolean isEnabled() { return enabled; }

    /**
     * Get layer-specific parameter with default fallback
     */
    public <T> T getParameter(String key, T defaultValue) {
        Object value = layerSpecificParams.get(key);
        if (value != null && defaultValue.getClass().isInstance(value)) {
            return (T) value;
        }
        return defaultValue;
    }

    /**
     * Get all layer-specific parameters
     */
    public Map<String, Object> getAllParameters() {
        return new HashMap<>(layerSpecificParams);
    }

    // === NOISE BLENDING UTILITIES ===

    /**
     * Apply this layer's blending mode to combine with base value
     */
    public double applyBlending(double baseValue, double layerValue) {
        if (!enabled) return baseValue;
        
        // Scale layer value by amplitude
        layerValue *= amplitude;
        
        return switch (blendMode) {
            case ADD -> baseValue + layerValue;
            case MULTIPLY -> baseValue * (1.0 + layerValue);
            case OVERLAY -> blendOverlay(baseValue, layerValue);
            case HARD_MIN -> Math.min(baseValue, layerValue);
            case HARD_MAX -> Math.max(baseValue, layerValue);
            case SOFT_LIGHT -> blendSoftLight(baseValue, layerValue);
            case RIDGE -> baseValue + Math.abs(layerValue);
            case DOMAIN_WARP -> baseValue; // Domain warping handled externally
        };
    }

    /**
     * Overlay blending for natural terrain transitions
     */
    private double blendOverlay(double base, double overlay) {
        if (overlay >= 0) {
            return base + overlay * 0.7; // Soft addition for positive
        } else {
            return base * (1.0 + overlay * 0.15); // Gentle reduction for negative
        }
    }

    /**
     * Soft light blending for smooth gradients
     */
    private double blendSoftLight(double base, double overlay) {
        double normalizedOverlay = (overlay + 1.0) * 0.5; // Normalize to 0-1
        if (normalizedOverlay < 0.5) {
            return base * (1.0 + (2.0 * normalizedOverlay - 1.0) * 0.3);
        } else {
            return base + (2.0 * normalizedOverlay - 1.0) * (Math.sqrt(Math.abs(base)) - base) * 0.3;
        }
    }

    // === PRESET CONFIGURATIONS ===

    /**
     * Create configuration for continental-scale terrain
     */
    public static NoiseLayerConfig createContinentalLayer() {
        return new NoiseLayerConfig("Continental")
                .setFrequency(0.0003)
                .setAmplitude(30.0)
                .setOctaves(2)
                .setBlendMode(BlendMode.ADD);
    }

    /**
     * Create configuration for mountain ranges
     */
    public static NoiseLayerConfig createMountainLayer() {
        return new NoiseLayerConfig("Mountains")
                .setFrequency(0.002)
                .setAmplitude(40.0)
                .setOctaves(3)
                .setBlendMode(BlendMode.RIDGE);
    }

    /**
     * Create configuration for valley carving
     */
    public static NoiseLayerConfig createValleyLayer() {
        return new NoiseLayerConfig("Valleys")
                .setFrequency(0.0015)
                .setAmplitude(-15.0)
                .setOctaves(2)
                .setBlendMode(BlendMode.OVERLAY);
    }

    /**
     * Create configuration for surface detail
     */
    public static NoiseLayerConfig createDetailLayer() {
        return new NoiseLayerConfig("Detail")
                .setFrequency(0.008)
                .setAmplitude(3.0)
                .setOctaves(2)
                .setBlendMode(BlendMode.ADD);
    }

    /**
     * Create configuration for plate boundaries
     */
    public static NoiseLayerConfig createPlateBoundaryLayer() {
        return new NoiseLayerConfig("PlateBoundaries")
                .setFrequency(0.001)
                .setAmplitude(1.0)
                .setOctaves(1)
                .setBlendMode(BlendMode.HARD_MAX);
    }

    @Override
    public String toString() {
        return String.format("NoiseLayer{%s: freq=%.4f, amp=%.2f, octaves=%d, blend=%s, enabled=%s}",
                           layerName, frequency, amplitude, octaves, blendMode, enabled);
    }
}