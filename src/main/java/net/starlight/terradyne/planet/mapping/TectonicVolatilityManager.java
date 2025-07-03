package net.starlight.terradyne.planet.mapping;

import net.minecraft.util.math.noise.SimplexNoiseSampler;
import net.minecraft.util.math.random.Random;
import net.starlight.terradyne.Terradyne;
import net.starlight.terradyne.planet.physics.PlanetConfig;
import net.starlight.terradyne.planet.physics.PlanetData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages tectonic plate boundaries and geological volatility using Voronoi diagrams
 * Generates plate centers based on planet physics, calculates volatility from boundary distance
 */
public class TectonicVolatilityManager {

    private final PlanetConfig config;
    private final PlanetData planetData;
    private final SimplexNoiseSampler masterNoise;
    
    // Plate center cache - generated once and reused
    private final List<PlateCenter> plateCenters;
    private final double averagePlateSize;
    private final double volatilityGradientRange;
    
    // Volatility cache for performance
    private final ConcurrentHashMap<Long, Integer> volatilityCache = new ConcurrentHashMap<>();
    
    /**
     * Represents a tectonic plate center point
     */
    private static class PlateCenter {
        public final double x;
        public final double z; 
        
        public PlateCenter(double x, double z) {
            this.x = x;
            this.z = z;
        }
        
        public double distanceTo(double worldX, double worldZ) {
            double dx = x - worldX;
            double dz = z - worldZ;
            return Math.sqrt(dx * dx + dz * dz);
        }
    }
    
    /**
     * Create tectonic volatility manager
     */
    public TectonicVolatilityManager(PlanetConfig config, PlanetData planetData, SimplexNoiseSampler masterNoise) {
        this.config = config;
        this.planetData = planetData;
        this.masterNoise = masterNoise;
        
        // Calculate plate characteristics based on planet physics
        this.averagePlateSize = calculateAveragePlateSize();
        this.volatilityGradientRange = averagePlateSize * 0.15; // 15% of plate size for gradient
        
        // Generate plate centers
        this.plateCenters = generatePlateCenters();
        
        Terradyne.LOGGER.info("TectonicVolatilityManager initialized for {}: {} plates, avg size {:.0f} blocks", 
                            config.getPlanetName(), plateCenters.size(), averagePlateSize);
        Terradyne.LOGGER.debug("Volatility gradient range: {:.0f} blocks", volatilityGradientRange);
    }
    
    /**
     * Get volatility level (0-5) at world coordinates
     * 0 = stable continental interior, 5 = active plate boundary
     */
    public int getChunkVolatility(int chunkX, int chunkZ) {
        // Convert chunk coords to world coords (chunk center)
        int worldX = (chunkX << 4) + 8;
        int worldZ = (chunkZ << 4) + 8;
        
        return getVolatilityAt(worldX, worldZ);
    }
    
    /**
     * Get volatility level at world coordinates with caching
     */
    public int getVolatilityAt(int worldX, int worldZ) {
        // Create cache key (pack coordinates into long)
        long cacheKey = ((long) worldX << 32) | (worldZ & 0xFFFFFFFFL);
        
        // Check cache first
        Integer cached = volatilityCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        // Calculate volatility
        int volatility = calculateVolatilityAt(worldX, worldZ);
        
        // Cache result (but limit cache size to prevent memory issues)
        if (volatilityCache.size() < 10000) {
            volatilityCache.put(cacheKey, volatility);
        }
        
        return volatility;
    }
    
    /**
     * Check if location is near plate boundary (volatility 3+)
     */
    public boolean isPlateNearBoundary(int worldX, int worldZ) {
        return getVolatilityAt(worldX, worldZ) >= 3;
    }
    
    /**
     * Calculate volatility at world coordinates (core algorithm)
     */
    private int calculateVolatilityAt(int worldX, int worldZ) {
        if (plateCenters.isEmpty()) {
            return 0; // No plates = no volatility
        }
        
        // Find distance to nearest plate boundary using Voronoi approach
        double nearestDistance = Double.MAX_VALUE;
        double secondNearestDistance = Double.MAX_VALUE;
        
        for (PlateCenter center : plateCenters) {
            double distance = center.distanceTo(worldX, worldZ);
            
            if (distance < nearestDistance) {
                secondNearestDistance = nearestDistance;
                nearestDistance = distance;
            } else if (distance < secondNearestDistance) {
                secondNearestDistance = distance;
            }
        }
        
        // Distance to plate boundary is halfway between nearest and second-nearest centers
        double boundaryDistance = (secondNearestDistance - nearestDistance) * 0.5;
        
        // Convert boundary distance to volatility (0-5) with gradient scaling
        return calculateVolatilityFromDistance(Math.abs(boundaryDistance));
    }
    
    /**
     * Convert distance from plate boundary to volatility level
     * Uses gradient range scaled by plate size
     */
    private int calculateVolatilityFromDistance(double boundaryDistance) {
        // Scale distance by tectonic activity - more active = wider volatility zones
        double activityMultiplier = 0.5 + planetData.getActualTectonicActivity() * 1.5;
        double effectiveRange = volatilityGradientRange * activityMultiplier;
        
        // Calculate volatility based on distance thresholds
        if (boundaryDistance <= effectiveRange * 0.1) {
            return 5; // On boundary
        } else if (boundaryDistance <= effectiveRange * 0.3) {
            return 4; // Very close
        } else if (boundaryDistance <= effectiveRange * 0.5) {
            return 3; // Close
        } else if (boundaryDistance <= effectiveRange * 0.7) {
            return 2; // Moderate distance
        } else if (boundaryDistance <= effectiveRange) {
            return 1; // Far but still influenced
        } else {
            return 0; // Stable interior
        }
    }
    
    /**
     * Calculate average plate size based on planet characteristics
     * Target: ~8000 blocks for Earth-like planets
     */
    private double calculateAveragePlateSize() {
        // Base plate size scales with planet size
        double basePlateSize = config.getCircumference() * 0.2; // 20% of circumference
        
        // Tectonic activity affects plate fragmentation
        // Low activity = larger plates, high activity = smaller plates  
        double tectonicFragmentation = 0.4 + planetData.getActualTectonicActivity() * 1.2;
        
        double plateSize = basePlateSize / tectonicFragmentation;
        
        // Clamp to reasonable range
        return Math.max(2000.0, Math.min(20000.0, plateSize));
    }
    
    /**
     * Generate plate centers using seeded noise for consistent placement
     */
    private List<PlateCenter> generatePlateCenters() {
        List<PlateCenter> centers = new ArrayList<>();
        
        // Calculate plate count based on planet size and plate size
        double planetArea = config.getCircumference() * config.getCircumference();
        double plateArea = averagePlateSize * averagePlateSize;
        int targetPlateCount = (int) Math.max(3, planetArea / plateArea);
        
        Terradyne.LOGGER.debug("Generating {} plates for planet area {:.0f} (avg plate area {:.0f})", 
                              targetPlateCount, planetArea, plateArea);
        
        // Generate plate centers using seeded random distribution
        Random plateRandom = Random.create(config.getSeed() + 12345); // Offset seed for plates
        int worldSize = config.getCircumference();
        
        for (int i = 0; i < targetPlateCount; i++) {
            // Use stratified sampling to ensure good distribution
            double gridSize = Math.sqrt(targetPlateCount);
            int gridX = i % (int) gridSize;
            int gridZ = i / (int) gridSize;
            
            double cellSizeX = worldSize / gridSize;
            double cellSizeZ = worldSize / gridSize;
            
            // Random position within grid cell
            double baseX = gridX * cellSizeX;
            double baseZ = gridZ * cellSizeZ;
            
            double offsetX = plateRandom.nextDouble() * cellSizeX;
            double offsetZ = plateRandom.nextDouble() * cellSizeZ;
            
            double plateX = baseX + offsetX - worldSize * 0.5; // Center around 0,0
            double plateZ = baseZ + offsetZ - worldSize * 0.5;
            
            centers.add(new PlateCenter(plateX, plateZ));
        }
        
        return centers;
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
        return String.format("TectonicVolatility{plates=%d, avgSize=%.0f, gradientRange=%.0f, cached=%d}", 
                           plateCenters.size(), averagePlateSize, volatilityGradientRange, volatilityCache.size());
    }
    
    /**
     * Get plate count
     */
    public int getPlateCount() {
        return plateCenters.size();
    }
    
    /**
     * Get average plate size in blocks
     */
    public double getAveragePlateSize() {
        return averagePlateSize;
    }
}