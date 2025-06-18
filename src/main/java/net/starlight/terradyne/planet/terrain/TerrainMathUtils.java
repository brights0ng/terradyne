// TerrainMathUtils.java
package net.starlight.terradyne.planet.terrain;

/**
 * Mathematical utilities for terrain generation.
 * Provides interpolation, domain warping, and coordinate transformation functions
 * for realistic terrain noise generation.
 */
public class TerrainMathUtils {
    
    /**
     * Linear interpolation between two values
     */
    public static double lerp(double a, double b, double t) {
        return a + t * (b - a);
    }
    
    /**
     * Cubic interpolation for smoother curves
     * Uses Hermite interpolation for smooth transitions
     */
    public static double smoothLerp(double a, double b, double t) {
        // Smooth step function: 3t² - 2t³
        double smoothT = t * t * (3.0 - 2.0 * t);
        return lerp(a, b, smoothT);
    }
    
    /**
     * Smoother cubic interpolation with even better curves
     * Uses quintic interpolation: 6t⁵ - 15t⁴ + 10t³
     */
    public static double smootherLerp(double a, double b, double t) {
        double smoothT = t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
        return lerp(a, b, smoothT);
    }
    
    /**
     * Clamp value between min and max
     */
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
    
    /**
     * Map value from input range to output range
     */
    public static double map(double value, double inMin, double inMax, double outMin, double outMax) {
        double t = (value - inMin) / (inMax - inMin);
        return lerp(outMin, outMax, t);
    }
    
    /**
     * Map noise value (-1 to 1) to height range (0 to maxHeight)
     * This is our basic linear height mapping
     */
    public static double noiseToHeight(double noiseValue, double maxHeight) {
        return map(noiseValue, -1.0, 1.0, 0.0, maxHeight);
    }
    
    /**
     * Domain warping: offset sampling coordinates based on noise
     * This creates the natural, flowing terrain we want
     */
    public static class DomainWarp {
        
        /**
         * Simple 2D domain warp
         * Offsets x,z coordinates based on noise values to create flowing terrain
         */
        public static CoordinatePair warpCoordinates(double x, double z, double warpStrength, 
                                                    double warpNoiseX, double warpNoiseZ) {
            double warpedX = x + (warpNoiseX * warpStrength);
            double warpedZ = z + (warpNoiseZ * warpStrength);
            return new CoordinatePair(warpedX, warpedZ);
        }
        
        /**
         * Multi-octave domain warp for more complex distortion
         * Creates more natural, varied terrain features
         */
        public static CoordinatePair multiWarpCoordinates(double x, double z, 
                                                         double[] warpStrengths,
                                                         double[] warpNoiseX, 
                                                         double[] warpNoiseZ) {
            double totalWarpX = 0.0;
            double totalWarpZ = 0.0;
            
            for (int i = 0; i < warpStrengths.length && i < warpNoiseX.length && i < warpNoiseZ.length; i++) {
                totalWarpX += warpNoiseX[i] * warpStrengths[i];
                totalWarpZ += warpNoiseZ[i] * warpStrengths[i];
            }
            
            return new CoordinatePair(x + totalWarpX, z + totalWarpZ);
        }
    }
    
    /**
     * Noise coordinate scaling utilities
     */
    public static class NoiseCoordinates {
        
        /**
         * Scale coordinates by frequency
         * Higher frequency = more detail, smaller features
         */
        public static CoordinatePair scaleByFrequency(double x, double z, double frequency) {
            return new CoordinatePair(x / frequency, z / frequency);
        }
        
        /**
         * Apply planetary scaling to coordinates
         * Larger planets should have proportionally larger features
         */
        public static CoordinatePair scaleByPlanetSize(double x, double z, int planetCircumference) {
            // Scale factor based on planet size relative to Earth-like (40,000 km)
            double scaleFactor = planetCircumference / 40000.0;
            return new CoordinatePair(x * scaleFactor, z * scaleFactor);
        }
        
        /**
         * Combine frequency and planetary scaling
         */
        public static CoordinatePair scaleCoordinates(double x, double z, double frequency, int planetCircumference) {
            CoordinatePair planetScaled = scaleByPlanetSize(x, z, planetCircumference);
            return scaleByFrequency(planetScaled.x, planetScaled.z, frequency);
        }
    }
    
    /**
     * Layer blending utilities
     */
    public static class LayerBlending {
        
        /**
         * Weighted blend between two noise values
         * Weight of 0.0 = pure value1, weight of 1.0 = pure value2
         */
        public static double weightedBlend(double value1, double value2, double weight) {
            return lerp(value1, value2, clamp(weight, 0.0, 1.0));
        }
        
        /**
         * Multiplicative blend - good for masking effects
         * Results in value1 modulated by value2
         */
        public static double multiplicativeBlend(double value1, double value2, double strength) {
            double modulator = lerp(1.0, value2, strength);
            return value1 * modulator;
        }
        
        /**
         * Overlay blend for more complex interactions
         * Creates natural-looking terrain combinations
         */
        public static double overlayBlend(double base, double overlay, double strength) {
            double blended;
            if (base < 0.0) {
                // Multiply mode for dark values
                blended = 2.0 * base * overlay;
            } else {
                // Screen mode for light values  
                blended = 1.0 - 2.0 * (1.0 - base) * (1.0 - overlay);
            }
            
            return lerp(base, blended, strength);
        }
        
        /**
         * Power blend - raises base to power of overlay
         * Good for creating sharp peaks and valleys
         */
        public static double powerBlend(double base, double overlay, double strength) {
            // Normalize base to 0-1 range for power operation
            double normalizedBase = (base + 1.0) * 0.5;
            double normalizedOverlay = (overlay + 1.0) * 0.5;
            
            double powered = Math.pow(normalizedBase, 1.0 + normalizedOverlay * strength);
            
            // Convert back to -1 to 1 range
            return (powered * 2.0) - 1.0;
        }
    }
    
    /**
     * Terrain shaping utilities
     */
    public static class TerrainShaping {
        
        /**
         * Apply ridged noise effect - creates sharp ridges and valleys
         * Good for mountain ranges and dramatic terrain
         */
        public static double ridgedNoise(double noiseValue) {
            return 1.0 - Math.abs(noiseValue);
        }
        
        /**
         * Apply billowy noise effect - creates puffy, cloud-like terrain
         * Good for rolling hills and soft features
         */
        public static double billowyNoise(double noiseValue) {
            return Math.abs(noiseValue);
        }
        
        /**
         * Apply terrace effect - creates stepped terrain
         * Good for sedimentary rock formations
         */
        public static double terraceNoise(double noiseValue, int steps) {
            double stepped = Math.floor(noiseValue * steps) / steps;
            return stepped;
        }
        
        /**
         * Apply exponential curve to noise - emphasizes peaks
         * Good for making mountains more dramatic
         */
        public static double exponentialCurve(double noiseValue, double exponent) {
            double normalized = (noiseValue + 1.0) * 0.5; // 0 to 1
            double curved = Math.pow(normalized, exponent);
            return (curved * 2.0) - 1.0; // Back to -1 to 1
        }
        
        /**
         * Apply plateau effect - flattens high areas
         * Good for mesa-like terrain features
         */
        public static double plateauEffect(double noiseValue, double plateauLevel, double plateauWidth) {
            if (noiseValue > plateauLevel) {
                double overshoot = noiseValue - plateauLevel;
                double damped = overshoot * plateauWidth;
                return plateauLevel + damped;
            }
            return noiseValue;
        }
    }
    
    /**
     * Distance and gradient utilities
     */
    public static class DistanceUtils {
        
        /**
         * 2D distance between two points
         */
        public static double distance2D(double x1, double z1, double x2, double z2) {
            double dx = x2 - x1;
            double dz = z2 - z1;
            return Math.sqrt(dx * dx + dz * dz);
        }
        
        /**
         * Gradient magnitude from noise derivatives
         * Useful for erosion calculations and slope-based effects
         */
        public static double gradientMagnitude(double dX, double dZ) {
            return Math.sqrt(dX * dX + dZ * dZ);
        }
        
        /**
         * Normalize gradient vector
         */
        public static CoordinatePair normalizeGradient(double dX, double dZ) {
            double magnitude = gradientMagnitude(dX, dZ);
            if (magnitude > 0.0) {
                return new CoordinatePair(dX / magnitude, dZ / magnitude);
            }
            return new CoordinatePair(0.0, 0.0);
        }
    }
    
    /**
     * Simple coordinate pair class for returning multiple values
     */
    public static class CoordinatePair {
        public final double x;
        public final double z;
        
        public CoordinatePair(double x, double z) {
            this.x = x;
            this.z = z;
        }
        
        @Override
        public String toString() {
            return String.format("(%.3f, %.3f)", x, z);
        }
    }
    
    /**
     * Random number utilities with deterministic seeding
     */
    public static class SeededRandom {
        
        /**
         * Generate pseudo-random value from coordinates and seed
         * Useful for consistent noise patterns across chunks
         */
        public static double pseudoRandom(int x, int z, long seed) {
            long hash = seed;
            hash ^= x * 374761393L;
            hash ^= z * 668265263L;
            hash = (hash ^ (hash >> 13)) * 1274126177L;
            hash ^= hash >> 16;
            
            // Convert to 0.0 - 1.0 range
            return (hash & 0x7FFFFFFFL) / (double) 0x7FFFFFFFL;
        }
        
        /**
         * Generate pseudo-random value in -1.0 to 1.0 range
         */
        public static double pseudoRandomBipolar(int x, int z, long seed) {
            return (pseudoRandom(x, z, seed) * 2.0) - 1.0;
        }
    }
}