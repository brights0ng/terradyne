package net.starlight.terradyne.planet.mapping;

import net.minecraft.util.math.noise.SimplexNoiseSampler;
import net.minecraft.util.math.random.Random;
import net.starlight.terradyne.Terradyne;
import net.starlight.terradyne.planet.physics.PlanetConfig;
import net.starlight.terradyne.planet.physics.PlanetData;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages tectonic plate boundaries using ridge noise for natural fault patterns
 * REWRITTEN: Uses ridge noise instead of Voronoi for uniform thin boundaries
 */
public class TectonicVolatilityManager {

    private final PlanetConfig config;
    private final PlanetData planetData;
    private final SimplexNoiseSampler primaryFaultNoise;   // Major plate boundaries
    private final SimplexNoiseSampler secondaryFaultNoise; // Smaller fractures
    private final SimplexNoiseSampler variationNoise;      // Boundary variation

    // Fault system parameters
    private final double primaryFaultScale;
    private final double secondaryFaultScale;
    private final double volatilityThickness;

    // Volatility cache for performance
    private final ConcurrentHashMap<Long, Integer> volatilityCache = new ConcurrentHashMap<>();

    /**
     * Create tectonic volatility manager using ridge noise fault systems
     */
    public TectonicVolatilityManager(PlanetConfig config, PlanetData planetData, SimplexNoiseSampler masterNoise) {
        this.config = config;
        this.planetData = planetData;

        // Create separate noise samplers for different fault scales
        long baseSeed = config.getSeed();
        this.primaryFaultNoise = new SimplexNoiseSampler(Random.create(baseSeed + 11111));
        this.secondaryFaultNoise = new SimplexNoiseSampler(Random.create(baseSeed + 22222));
        this.variationNoise = new SimplexNoiseSampler(Random.create(baseSeed + 33333));

        // Calculate fault scales based on planet physics
        this.primaryFaultScale = calculatePrimaryFaultScale();
        this.secondaryFaultScale = calculateSecondaryFaultScale();
        this.volatilityThickness = calculateVolatilityThickness();

        Terradyne.LOGGER.info("Ridge-based TectonicVolatilityManager initialized for {}", config.getPlanetName());
        Terradyne.LOGGER.info("Primary fault scale: {:.6f} (major boundaries)", primaryFaultScale);
        Terradyne.LOGGER.info("Secondary fault scale: {:.6f} (minor fractures)", secondaryFaultScale);
        Terradyne.LOGGER.info("Volatility thickness: {:.0f} blocks (uniform thin lines)", volatilityThickness);
    }

    /**
     * Calculate primary fault scale based on planet size and tectonic activity
     */
    private double calculatePrimaryFaultScale() {
        // Scale based on planet circumference for realistic major plate boundaries
        double baseScale = 1.0 / (config.getCircumference() * 0.15); // Major faults every ~15% of planet

        // High tectonic activity = more fragmented plates (smaller scale = more frequent faults)
        double activityMultiplier = 0.7 + planetData.getActualTectonicActivity() * 0.6;

        return baseScale * activityMultiplier;
    }

    /**
     * Calculate secondary fault scale for smaller fractures
     */
    private double calculateSecondaryFaultScale() {
        // Secondary faults are 3-4x more frequent than primary
        return primaryFaultScale * 3.5;
    }

    /**
     * Calculate how thick volatility zones should be
     */
    private double calculateVolatilityThickness() {
        // Base thickness scales with planet size but stays small
        double baseThickness = config.getCircumference() * 0.008; // 0.8% of planet circumference

        // Tectonic activity affects thickness slightly
        double activityMultiplier = 0.8 + planetData.getActualTectonicActivity() * 0.4;

        double thickness = baseThickness * activityMultiplier;

        // Clamp to reasonable geological scales (50-400 blocks)
        return Math.max(50.0, Math.min(400.0, thickness));
    }

    /**
     * Get volatility level (0-5) at world coordinates using ridge noise
     */
    public int getChunkVolatility(int chunkX, int chunkZ) {
        // Convert chunk coords to world coords (chunk center)
        int worldX = (chunkX << 4) + 8;
        int worldZ = (chunkZ << 4) + 8;

        return getVolatilityAt(worldX, worldZ);
    }

    /**
     * Get volatility level at world coordinates with ridge-based calculation
     */
    public int getVolatilityAt(int worldX, int worldZ) {
        // Create cache key (pack coordinates into long)
        long cacheKey = ((long) worldX << 32) | (worldZ & 0xFFFFFFFFL);

        // Check cache first
        Integer cached = volatilityCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // Calculate volatility using ridge noise
        int volatility = calculateRidgeBasedVolatility(worldX, worldZ);

        // Cache result (limit cache size to prevent memory issues)
        if (volatilityCache.size() < 10000) {
            volatilityCache.put(cacheKey, volatility);
        }

        return volatility;
    }

    /**
     * Calculate volatility using ridge noise fault systems
     */
    private int calculateRidgeBasedVolatility(int worldX, int worldZ) {
        // Sample ridge noise at two scales (primary and secondary faults)
        double primaryRidge = Math.abs(primaryFaultNoise.sample(worldX * primaryFaultScale, 0, worldZ * primaryFaultScale));
        double secondaryRidge = Math.abs(secondaryFaultNoise.sample(worldX * secondaryFaultScale, 100, worldZ * secondaryFaultScale));

        // Find distance to nearest fault line (primary or secondary)
        double nearestFaultDistance = Math.min(primaryRidge, secondaryRidge * 0.6); // Secondary faults are weaker

        // Add natural variation to boundaries
        double variationScale = volatilityThickness * 0.5;
        double boundaryVariation = variationNoise.sample(worldX / variationScale, 200, worldZ / variationScale) * 0.15;
        double adjustedDistance = nearestFaultDistance + boundaryVariation;

        // Convert distance to volatility levels with smooth transitions
        return calculateVolatilityFromRidgeDistance(adjustedDistance);
    }

    /**
     * Convert ridge distance to volatility level (0-5)
     */
    private int calculateVolatilityFromRidgeDistance(double ridgeDistance) {
        // Scale distance by tectonic activity
        double activityMultiplier = 0.8 + planetData.getActualTectonicActivity() * 0.4;
        double effectiveThickness = (volatilityThickness / config.getCircumference()) * activityMultiplier;

        // Normalize distance (0.0 = on fault line, 1.0+ = far from any fault)
        double normalizedDistance = ridgeDistance / effectiveThickness;

        // Convert to volatility with thin, precise boundaries
        if (normalizedDistance <= 0.02) {
            return 5; // Very close to fault line (within 2% of thickness)
        } else if (normalizedDistance <= 0.08) {
            return 4; // Close to fault line (within 8% of thickness)
        } else if (normalizedDistance <= 0.20) {
            return 3; // Moderate influence (within 20% of thickness)
        } else if (normalizedDistance <= 0.45) {
            return 2; // Weak influence (within 45% of thickness)
        } else if (normalizedDistance <= 1.0) {
            return 1; // Very weak influence (within full thickness)
        } else {
            return 0; // Stable continental interior
        }
    }

    /**
     * Check if location is near plate boundary (volatility 3+)
     */
    public boolean isPlateNearBoundary(int worldX, int worldZ) {
        return getVolatilityAt(worldX, worldZ) >= 3;
    }

    /**
     * Clear volatility cache (for memory management)
     */
    public void clearCache() {
        volatilityCache.clear();
        Terradyne.LOGGER.debug("Volatility cache cleared for {}", config.getPlanetName());
    }

    /**
     * Get statistics for debugging
     */
    public String getStatistics() {
        return String.format("RidgeVolatility{primaryScale=%.6f, secondaryScale=%.6f, thickness=%.0f blocks, cached=%d}",
                primaryFaultScale, secondaryFaultScale, volatilityThickness, volatilityCache.size());
    }

    /**
     * Get equivalent plate count for compatibility (estimated from fault density)
     */
    public int getPlateCount() {
        // Estimate plate count based on fault frequency
        double faultFrequency = primaryFaultScale * config.getCircumference();
        return (int) Math.max(4, faultFrequency * faultFrequency * 0.8);
    }

    /**
     * Get average "plate" size for compatibility (estimated from fault spacing)
     */
    public double getAveragePlateSize() {
        // Estimate from primary fault frequency
        return 1.0 / (primaryFaultScale * 1.5);
    }

    /**
     * Get volatility thickness for debugging
     */
    public double getVolatilityThickness() {
        return volatilityThickness;
    }
}