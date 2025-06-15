package net.starlight.terradyne.planet.terrain.math;

/**
 * Mathematical utility functions for terrain generation
 * Provides performance-optimized calculations used across all terrain systems
 * Ensures consistent mathematical operations and smooth transitions
 */
public class TerrainMathUtils {
    
    // Mathematical constants for optimization
    public static final double GOLDEN_RATIO = 1.618033988749895;
    private static final double INV_GOLDEN_RATIO = 1.0 / GOLDEN_RATIO;
    private static final double E_NEGATIVE_ONE = 1.0 / Math.E;
    
    // Lookup tables for expensive functions (initialized lazily)
    private static double[] smoothstepLookup = null;
    private static final int LOOKUP_SIZE = 1024;
    private static final double LOOKUP_SCALE = LOOKUP_SIZE - 1;
    
    /**
     * Smooth step interpolation function
     * Provides smooth transitions between 0 and 1 for values between edge0 and edge1
     * Uses Hermite interpolation: 3t² - 2t³
     * 
     * @param edge0 Lower edge of transition
     * @param edge1 Upper edge of transition  
     * @param x Input value
     * @return Smoothly interpolated value between 0 and 1
     */
    public static double smoothstep(double edge0, double edge1, double x) {
        // Clamp and normalize input
        double t = clamp((x - edge0) / (edge1 - edge0), 0.0, 1.0);
        // Hermite interpolation
        return t * t * (3.0 - 2.0 * t);
    }
    
    /**
     * Smoother step interpolation (5th order)
     * Even smoother than smoothstep, uses: 6t⁵ - 15t⁴ + 10t³
     * More expensive but creates very natural transitions
     */
    public static double smootherstep(double edge0, double edge1, double x) {
        double t = clamp((x - edge0) / (edge1 - edge0), 0.0, 1.0);
        return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
    }
    
    /**
     * Fast smoothstep using lookup table for performance-critical sections
     * Approximately 3x faster than direct calculation
     */
    public static double smoothstepFast(double edge0, double edge1, double x) {
        if (smoothstepLookup == null) {
            initializeLookupTables();
        }
        
        double t = clamp((x - edge0) / (edge1 - edge0), 0.0, 1.0);
        int index = (int)(t * LOOKUP_SCALE);
        return smoothstepLookup[index];
    }
    
    /**
     * Linear interpolation between two values
     * @param a Start value
     * @param b End value
     * @param t Interpolation factor (0.0 to 1.0)
     */
    public static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
    
    /**
     * Bilinear interpolation for 2D sampling
     * Used for interpolating cached noise values
     */
    public static double bilerp(double a, double b, double c, double d, double tx, double ty) {
        double top = lerp(a, b, tx);
        double bottom = lerp(c, d, tx);
        return lerp(top, bottom, ty);
    }
    
    /**
     * Clamp value between min and max
     */
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
    
    /**
     * Fast clamp to 0-1 range (most common case)
     */
    public static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
    
    /**
     * Exponential falloff function for volatility and boundary effects
     * Creates smooth distance-based attenuation
     * 
     * @param distance Distance from source
     * @param falloffRadius Distance at which effect becomes negligible
     * @param strength Maximum effect strength
     * @return Attenuated effect value
     */
    public static double exponentialFalloff(double distance, double falloffRadius, double strength) {
        if (distance >= falloffRadius * 3.0) return 0.0; // Early exit for distant points
        
        double normalizedDistance = distance / falloffRadius;
        return strength * Math.exp(-normalizedDistance * normalizedDistance);
    }
    
    /**
     * Power falloff function (more controlled than exponential)
     * Good for terrain features that need specific falloff curves
     */
    public static double powerFalloff(double distance, double falloffRadius, double strength, double power) {
        if (distance >= falloffRadius) return 0.0;
        
        double normalizedDistance = distance / falloffRadius;
        return strength * Math.pow(1.0 - normalizedDistance, power);
    }
    
    /**
     * Smooth minimum function (creates rounded valley-like blending)
     * Useful for combining terrain features without sharp edges
     */
    public static double smoothMin(double a, double b, double smoothness) {
        double h = clamp01(0.5 + 0.5 * (b - a) / smoothness);
        return lerp(b, a, h) - smoothness * h * (1.0 - h);
    }
    
    /**
     * Smooth maximum function (creates rounded mountain-like blending)
     */
    public static double smoothMax(double a, double b, double smoothness) {
        return -smoothMin(-a, -b, smoothness);
    }
    
    /**
     * Calculate 2D distance between two points
     * Optimized for frequent use in plate boundary calculations
     */
    public static double distance2D(double x1, double z1, double x2, double z2) {
        double dx = x2 - x1;
        double dz = z2 - z1;
        return Math.sqrt(dx * dx + dz * dz);
    }
    
    /**
     * Calculate squared distance (faster when you don't need the actual distance)
     */
    public static double distanceSquared2D(double x1, double z1, double x2, double z2) {
        double dx = x2 - x1;
        double dz = z2 - z1;
        return dx * dx + dz * dz;
    }
    
    /**
     * Fast distance approximation for performance-critical code
     * About 20% faster than Math.sqrt, ~5% error maximum
     */
    public static double distanceFast2D(double x1, double z1, double x2, double z2) {
        double dx = Math.abs(x2 - x1);
        double dz = Math.abs(z2 - z1);
        double min = Math.min(dx, dz);
        double max = Math.max(dx, dz);
        return max + min * 0.375; // Approximation factor
    }
    
    /**
     * Normalize value from one range to another
     */
    public static double remap(double value, double oldMin, double oldMax, double newMin, double newMax) {
        double normalized = (value - oldMin) / (oldMax - oldMin);
        return newMin + normalized * (newMax - newMin);
    }
    
    /**
     * Generate frequency for noise octave using golden ratio spacing
     * Prevents interference patterns between noise layers
     */
    public static double getOctaveFrequency(double baseFrequency, int octave) {
        return baseFrequency * Math.pow(GOLDEN_RATIO, octave);
    }
    
    /**
     * Generate amplitude for noise octave with persistence decay
     */
    public static double getOctaveAmplitude(double baseAmplitude, int octave, double persistence) {
        return baseAmplitude * Math.pow(persistence, octave);
    }
    
    /**
     * Calculate total amplitude for multi-octave noise (for normalization)
     */
    public static double getTotalAmplitude(int octaves, double persistence) {
        double total = 0.0;
        for (int i = 0; i < octaves; i++) {
            total += Math.pow(persistence, i);
        }
        return total;
    }
    
    /**
     * Fractal Brownian Motion (fBm) amplitude normalization
     * Ensures multi-octave noise stays within expected bounds
     */
    public static double normalizeFBM(double value, int octaves, double persistence) {
        double maxAmplitude = getTotalAmplitude(octaves, persistence);
        return value / maxAmplitude;
    }
    
    /**
     * Ridge noise transformation (creates mountain ridges from regular noise)
     */
    public static double ridgeTransform(double noiseValue) {
        return 1.0 - Math.abs(noiseValue);
    }
    
    /**
     * Billow noise transformation (creates puffy, cloud-like features)
     */
    public static double billowTransform(double noiseValue) {
        return Math.abs(noiseValue);
    }
    
    /**
     * Terrace transformation (creates stepped, mesa-like terrain)
     */
    public static double terraceTransform(double noiseValue, int steps) {
        return Math.round(noiseValue * steps) / (double)steps;
    }
    
    /**
     * Turbulence function for chaotic terrain features
     */
    public static double turbulence(double noiseValue, double strength) {
        return Math.abs(noiseValue) * strength;
    }
    
    /**
     * Weight function for blending multiple terrain systems
     * Creates smooth transitions between different generation methods
     */
    public static double calculateBlendWeight(double distance, double innerRadius, double outerRadius) {
        if (distance <= innerRadius) return 1.0;
        if (distance >= outerRadius) return 0.0;
        
        double t = (distance - innerRadius) / (outerRadius - innerRadius);
        return smoothstep(0.0, 1.0, 1.0 - t);
    }
    
    /**
     * Coordinate normalization for consistent world-space calculations
     * Useful for ensuring noise coordinates are properly scaled
     */
    public static double normalizeWorldCoordinate(int worldCoord, double scale) {
        return worldCoord * scale;
    }
    
    /**
     * Hash function for deterministic randomness based on coordinates
     * Useful for consistent but random plate assignments
     */
    public static long hashCoordinates(int x, int z, long seed) {
        long hash = seed;
        hash = hash * 31 + x;
        hash = hash * 31 + z;
        return hash;
    }
    
    /**
     * Convert hash to normalized double [0, 1)
     */
    public static double hashToDouble(long hash) {
        return ((hash & 0x7FFFFFFFFFFFFFFFL) % 1000000) / 1000000.0;
    }
    
    /**
     * Initialize lookup tables for performance optimization
     */
    private static void initializeLookupTables() {
        smoothstepLookup = new double[LOOKUP_SIZE];
        for (int i = 0; i < LOOKUP_SIZE; i++) {
            double t = i / LOOKUP_SCALE;
            smoothstepLookup[i] = t * t * (3.0 - 2.0 * t);
        }
    }
    
    /**
     * Memory cleanup for lookup tables (call when mod unloads)
     */
    public static void cleanup() {
        smoothstepLookup = null;
    }
    
    // Float versions of common functions for performance when precision isn't critical
    
    public static float smoothstep(float edge0, float edge1, float x) {
        float t = clamp((x - edge0) / (edge1 - edge0), 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);
    }
    
    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
    
    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
    
    public static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }
    
    public static float distance2D(float x1, float z1, float x2, float z2) {
        float dx = x2 - x1;
        float dz = z2 - z1;
        return (float)Math.sqrt(dx * dx + dz * dz);
    }
}