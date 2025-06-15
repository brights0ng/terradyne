package net.starlight.terradyne.planet.terrain.cache;

import net.starlight.terradyne.planet.terrain.cache.TerrainCacheManager.TerrainCache;
import net.starlight.terradyne.planet.terrain.config.UnifiedTerrainConfig;
import net.starlight.terradyne.planet.terrain.math.TerrainMathUtils;
import net.starlight.terradyne.planet.terrain.tectonic.TectonicSystemManager;
import net.starlight.terradyne.planet.terrain.tectonic.TectonicSystemManager.EnhancedBoundaryInfo;
import net.starlight.terradyne.planet.terrain.tectonic.TectonicSystemManager.EnhancedTectonicPlate;
import net.starlight.terradyne.planet.physics.TectonicPlateGenerator.BoundaryType;
import net.starlight.terradyne.Terradyne;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches expensive volatility and boundary distance calculations
 * Pre-calculates gradient falloff maps around plate boundaries
 * Provides smooth volatility transitions and boundary type lookups
 * Operates at higher resolution than ChunkRegionCache for precise boundary effects
 */
public class PlateBoundaryCache implements TerrainCache<PlateBoundaryCache.BoundaryRegionData> {
    
    private final Map<String, BoundaryRegionData> cache;
    private final int maxSize;
    private final UnifiedTerrainConfig terrainConfig;
    
    // Dependencies
    private TectonicSystemManager tectonicSystem;
    
    // Cache configuration
    private final int boundaryRegionSize; // Size in blocks (64x64 blocks per region)
    private final int boundaryResolution; // Resolution for boundary sampling (8 blocks)
    private final double maxBoundaryDistance; // Distance beyond which no caching occurs
    private final boolean enableGradientPrecomputation;
    
    // Performance tracking
    private long cacheGenerations = 0;
    private long totalGenerationTime = 0;
    private long boundaryCalculations = 0;
    private long gradientCalculations = 0;
    private long cacheHits = 0;
    private long cacheMisses = 0;
    
    /**
     * Initialize plate boundary cache
     */
    public PlateBoundaryCache(UnifiedTerrainConfig config) {
        this.terrainConfig = config;
        this.maxSize = config.getPlateBoundaryCacheSize();
        this.cache = new ConcurrentHashMap<>(maxSize);
        
        // Cache configuration - focus on boundary areas
        this.boundaryRegionSize = 64; // 64x64 blocks per cached region
        this.boundaryResolution = 8; // Sample every 8 blocks for high precision
        this.maxBoundaryDistance = config.getVolatilityFalloffRadius();
        this.enableGradientPrecomputation = true;
        
        Terradyne.LOGGER.info("Plate Boundary Cache initialized (max size: {}, region: {}x{} blocks, resolution: {}, max distance: {})", 
                maxSize, boundaryRegionSize, boundaryRegionSize, boundaryResolution, maxBoundaryDistance);
    }
    
    /**
     * Set dependencies after initialization
     */
    public void setDependencies(TectonicSystemManager tectonicSystem) {
        this.tectonicSystem = tectonicSystem;
        Terradyne.LOGGER.debug("PlateBoundaryCache dependencies set");
    }
    
    // === BOUNDARY DATA GENERATION ===
    
    /**
     * Get cached boundary data for a world position, generating if necessary
     */
    public BoundaryRegionData getBoundaryRegionData(int worldX, int worldZ) {
        String regionKey = generateBoundaryRegionKey(worldX, worldZ);
        
        BoundaryRegionData cached = cache.get(regionKey);
        if (cached != null) {
            cacheHits++;
            return cached;
        }
        
        cacheMisses++;
        return generateBoundaryRegionData(worldX, worldZ);
    }
    
    /**
     * Generate comprehensive boundary data for a region
     */
    private BoundaryRegionData generateBoundaryRegionData(int centerWorldX, int centerWorldZ) {
        if (tectonicSystem == null) {
            throw new IllegalStateException("TectonicSystemManager dependency not set for PlateBoundaryCache");
        }
        
        long startTime = System.currentTimeMillis();
        cacheGenerations++;
        
        // Calculate region bounds
        int regionStartX = (centerWorldX / boundaryRegionSize) * boundaryRegionSize;
        int regionStartZ = (centerWorldZ / boundaryRegionSize) * boundaryRegionSize;
        int regionEndX = regionStartX + boundaryRegionSize;
        int regionEndZ = regionStartZ + boundaryRegionSize;
        
        // Check if this region is near any boundaries
        boolean regionHasBoundaries = checkRegionForBoundaries(regionStartX, regionStartZ, regionEndX, regionEndZ);
        
        BoundaryRegionData regionData;
        
        if (regionHasBoundaries) {
            // Generate detailed boundary data
            regionData = generateDetailedBoundaryData(regionStartX, regionStartZ, regionEndX, regionEndZ);
        } else {
            // Generate sparse data for regions far from boundaries
            regionData = generateSparseBoundaryData(regionStartX, regionStartZ, regionEndX, regionEndZ);
        }
        
        // Cache the data
        String regionKey = generateBoundaryRegionKey(centerWorldX, centerWorldZ);
        put(regionKey, regionData);
        
        long generationTime = System.currentTimeMillis() - startTime;
        totalGenerationTime += generationTime;
        
        if (generationTime > 30) { // Log slow generations
            Terradyne.LOGGER.debug("Generated boundary region data in {}ms (region: {},{} detailed: {})", 
                    generationTime, regionStartX, regionStartZ, regionHasBoundaries);
        }
        
        return regionData;
    }
    
    /**
     * Check if a region contains significant plate boundaries
     */
    private boolean checkRegionForBoundaries(int startX, int startZ, int endX, int endZ) {
        // Sample at corners and center to detect boundaries
        int[][] samplePoints = {
            {startX, startZ}, {endX - 1, startZ}, {startX, endZ - 1}, {endX - 1, endZ - 1},
            {(startX + endX) / 2, (startZ + endZ) / 2}
        };
        
        for (int[] point : samplePoints) {
            EnhancedBoundaryInfo boundaryInfo = tectonicSystem.getBoundaryInfoAt(point[0], point[1]);
            if (boundaryInfo.distanceToBoundary < maxBoundaryDistance * 0.8) {
                return true; // Close enough to boundary to warrant detailed caching
            }
        }
        
        return false;
    }
    
    /**
     * Generate detailed boundary data for regions near plate boundaries
     */
    private BoundaryRegionData generateDetailedBoundaryData(int startX, int startZ, int endX, int endZ) {
        Map<String, BoundaryPointData> boundaryPoints = new HashMap<>();
        Map<String, VolatilityGradient> volatilityGradients = new HashMap<>();
        Set<BoundaryType> regionBoundaryTypes = new HashSet<>();
        
        float minDistance = Float.MAX_VALUE;
        float maxVolatility = 0.0f;
        float avgVolatility = 0.0f;
        int volatilitySamples = 0;
        
        // High-resolution sampling for detailed boundary regions
        for (int worldX = startX; worldX < endX; worldX += boundaryResolution) {
            for (int worldZ = startZ; worldZ < endZ; worldZ += boundaryResolution) {
                
                // Get boundary information
                EnhancedBoundaryInfo boundaryInfo = tectonicSystem.getBoundaryInfoAt(worldX, worldZ);
                boundaryCalculations++;
                
                String pointKey = generatePointKey(worldX, worldZ, boundaryResolution);
                
                // Store boundary point data
                BoundaryPointData pointData = new BoundaryPointData(
                    boundaryInfo.distanceToBoundary,
                    boundaryInfo.boundaryType,
                    boundaryInfo.volatility,
                    boundaryInfo.boundaryStrength
                );
                boundaryPoints.put(pointKey, pointData);
                
                // Generate volatility gradient if near boundary
                if (boundaryInfo.distanceToBoundary < maxBoundaryDistance) {
                    VolatilityGradient gradient = generateVolatilityGradient(worldX, worldZ, boundaryInfo);
                    volatilityGradients.put(pointKey, gradient);
                    gradientCalculations++;
                }
                
                // Track region statistics
                regionBoundaryTypes.add(boundaryInfo.boundaryType);
                minDistance = Math.min(minDistance, boundaryInfo.distanceToBoundary);
                maxVolatility = Math.max(maxVolatility, Math.abs(boundaryInfo.volatility));
                
                if (Math.abs(boundaryInfo.volatility) > 0.01) {
                    avgVolatility += Math.abs(boundaryInfo.volatility);
                    volatilitySamples++;
                }
            }
        }
        
        if (volatilitySamples > 0) {
            avgVolatility /= volatilitySamples;
        }
        
        // Create detailed region data
        BoundaryRegionMetadata metadata = new BoundaryRegionMetadata(
            true, regionBoundaryTypes, minDistance, maxVolatility, avgVolatility,
            boundaryPoints.size(), volatilityGradients.size()
        );
        
        return new BoundaryRegionData(
            startX, startZ, boundaryRegionSize, boundaryResolution,
            boundaryPoints, volatilityGradients, metadata,
            System.currentTimeMillis()
        );
    }
    
    /**
     * Generate sparse boundary data for regions far from boundaries
     */
    private BoundaryRegionData generateSparseBoundaryData(int startX, int startZ, int endX, int endZ) {
        // Minimal sampling for distant regions
        Map<String, BoundaryPointData> boundaryPoints = new HashMap<>();
        
        // Sample at center only
        int centerX = (startX + endX) / 2;
        int centerZ = (startZ + endZ) / 2;
        
        EnhancedBoundaryInfo boundaryInfo = tectonicSystem.getBoundaryInfoAt(centerX, centerZ);
        
        String pointKey = generatePointKey(centerX, centerZ, boundaryRegionSize);
        BoundaryPointData pointData = new BoundaryPointData(
            boundaryInfo.distanceToBoundary,
            boundaryInfo.boundaryType,
            boundaryInfo.volatility,
            boundaryInfo.boundaryStrength
        );
        boundaryPoints.put(pointKey, pointData);
        
        BoundaryRegionMetadata metadata = new BoundaryRegionMetadata(
            false, Set.of(boundaryInfo.boundaryType), boundaryInfo.distanceToBoundary,
            Math.abs(boundaryInfo.volatility), Math.abs(boundaryInfo.volatility), 1, 0
        );
        
        return new BoundaryRegionData(
            startX, startZ, boundaryRegionSize, boundaryRegionSize, // Use region size as resolution for sparse
            boundaryPoints, new HashMap<>(), metadata,
            System.currentTimeMillis()
        );
    }
    
    /**
     * Generate volatility gradient for smooth transitions
     */
    private VolatilityGradient generateVolatilityGradient(int centerX, int centerZ, EnhancedBoundaryInfo centerInfo) {
        if (!enableGradientPrecomputation) {
            return new VolatilityGradient(centerInfo.volatility, 0.0f, 0.0f, 0.0f);
        }
        
        // Sample surrounding points to calculate gradient
        int offset = boundaryResolution;
        
        float volatilityRight = tectonicSystem.getBoundaryInfoAt(centerX + offset, centerZ).volatility;
        float volatilityLeft = tectonicSystem.getBoundaryInfoAt(centerX - offset, centerZ).volatility;
        float volatilityUp = tectonicSystem.getBoundaryInfoAt(centerX, centerZ + offset).volatility;
        float volatilityDown = tectonicSystem.getBoundaryInfoAt(centerX, centerZ - offset).volatility;
        
        // Calculate gradients
        float gradientX = (volatilityRight - volatilityLeft) / (2.0f * offset);
        float gradientZ = (volatilityUp - volatilityDown) / (2.0f * offset);
        float gradientMagnitude = (float)Math.sqrt(gradientX * gradientX + gradientZ * gradientZ);
        
        return new VolatilityGradient(centerInfo.volatility, gradientX, gradientZ, gradientMagnitude);
    }
    
    // === CACHED DATA ACCESS METHODS ===
    
    /**
     * Get cached boundary distance with interpolation
     */
    public float getCachedBoundaryDistance(int worldX, int worldZ) {
        BoundaryRegionData regionData = getBoundaryRegionData(worldX, worldZ);
        return regionData.getInterpolatedBoundaryDistance(worldX, worldZ);
    }
    
    /**
     * Get cached volatility with gradient interpolation
     */
    public float getCachedVolatility(int worldX, int worldZ) {
        BoundaryRegionData regionData = getBoundaryRegionData(worldX, worldZ);
        return regionData.getInterpolatedVolatility(worldX, worldZ);
    }
    
    /**
     * Get cached boundary type at position
     */
    public BoundaryType getCachedBoundaryType(int worldX, int worldZ) {
        BoundaryRegionData regionData = getBoundaryRegionData(worldX, worldZ);
        return regionData.getBoundaryType(worldX, worldZ);
    }
    
    /**
     * Get cached boundary strength
     */
    public float getCachedBoundaryStrength(int worldX, int worldZ) {
        BoundaryRegionData regionData = getBoundaryRegionData(worldX, worldZ);
        return regionData.getBoundaryStrength(worldX, worldZ);
    }
    
    /**
     * Check if a position is near cached boundaries
     */
    public boolean isNearBoundary(int worldX, int worldZ, float threshold) {
        float distance = getCachedBoundaryDistance(worldX, worldZ);
        return distance < threshold;
    }
    
    // === BATCH OPERATIONS ===
    
    /**
     * Pre-generate boundary cache for regions around a point
     */
    public void preGenerateBoundaryRegions(int centerWorldX, int centerWorldZ, int radius) {
        long startTime = System.currentTimeMillis();
        int generated = 0;
        
        int regionRadius = (radius / boundaryRegionSize + 1) * boundaryRegionSize;
        
        for (int worldX = centerWorldX - regionRadius; worldX <= centerWorldX + regionRadius; worldX += boundaryRegionSize) {
            for (int worldZ = centerWorldZ - regionRadius; worldZ <= centerWorldZ + regionRadius; worldZ += boundaryRegionSize) {
                
                String regionKey = generateBoundaryRegionKey(worldX, worldZ);
                if (!cache.containsKey(regionKey)) {
                    // Only generate if likely to have boundaries
                    if (shouldCacheRegion(worldX, worldZ)) {
                        generateBoundaryRegionData(worldX, worldZ);
                        generated++;
                    }
                }
            }
        }
        
        if (generated > 0) {
            long duration = System.currentTimeMillis() - startTime;
            Terradyne.LOGGER.debug("Pre-generated {} boundary regions in {}ms (radius: {})", generated, duration, radius);
        }
    }
    
    /**
     * Check if a region should be cached (has potential boundaries nearby)
     */
    private boolean shouldCacheRegion(int worldX, int worldZ) {
        // Quick check - sample center and see if it's worth caching
        EnhancedBoundaryInfo centerInfo = tectonicSystem.getBoundaryInfoAt(worldX, worldZ);
        return centerInfo.distanceToBoundary < maxBoundaryDistance * 1.5;
    }
    
    // === KEY GENERATION ===
    
    private String generateBoundaryRegionKey(int worldX, int worldZ) {
        int regionX = (worldX / boundaryRegionSize) * boundaryRegionSize;
        int regionZ = (worldZ / boundaryRegionSize) * boundaryRegionSize;
        return "boundary_region:" + regionX + ":" + regionZ;
    }
    
    private String generatePointKey(int worldX, int worldZ, int resolution) {
        int gridX = (worldX / resolution) * resolution;
        int gridZ = (worldZ / resolution) * resolution;
        return gridX + ":" + gridZ;
    }
    
    /**
     * Get cache performance statistics
     */
    public PlateBoundaryCacheStatistics getStatistics() {
        double avgGenerationTime = cacheGenerations > 0 ? (double) totalGenerationTime / cacheGenerations : 0.0;
        double hitRate = (cacheHits + cacheMisses) > 0 ? (double) cacheHits / (cacheHits + cacheMisses) : 0.0;
        
        return new PlateBoundaryCacheStatistics(
            cache.size(),
            maxSize,
            cacheGenerations,
            avgGenerationTime,
            boundaryCalculations,
            gradientCalculations,
            hitRate,
            boundaryRegionSize,
            boundaryResolution
        );
    }
    
    // === TERRAINCACHE INTERFACE IMPLEMENTATION ===
    
    @Override
    public BoundaryRegionData get(String key) {
        return cache.get(key);
    }
    
    @Override
    public BoundaryRegionData put(String key, BoundaryRegionData value) {
        if (cache.size() >= maxSize && !cache.containsKey(key)) {
            // Remove least recently used entry (simple LRU approximation)
            Optional<Map.Entry<String, BoundaryRegionData>> oldestEntry = cache.entrySet().stream()
                    .min(Comparator.comparingLong(entry -> entry.getValue().getCreationTime()));
            
            if (oldestEntry.isPresent()) {
                cache.remove(oldestEntry.get().getKey());
            }
        }
        return cache.put(key, value);
    }
    
    @Override
    public BoundaryRegionData remove(String key) {
        return cache.remove(key);
    }
    
    @Override
    public boolean containsKey(String key) {
        return cache.containsKey(key);
    }
    
    @Override
    public void clear() {
        cache.clear();
        Terradyne.LOGGER.debug("Cleared plate boundary cache");
    }
    
    @Override
    public int size() {
        return cache.size();
    }
    
    @Override
    public int getMaxSize() {
        return maxSize;
    }
    
    @Override
    public Set<String> keySet() {
        return cache.keySet();
    }
    
    // === DATA CLASSES ===
    
    /**
     * Complete boundary data for a region
     */
    public static class BoundaryRegionData {
        private final int regionStartX, regionStartZ;
        private final int regionSize;
        private final int resolution;
        private final Map<String, BoundaryPointData> boundaryPoints;
        private final Map<String, VolatilityGradient> volatilityGradients;
        private final BoundaryRegionMetadata metadata;
        private final long creationTime;
        
        public BoundaryRegionData(int regionStartX, int regionStartZ, int regionSize, int resolution,
                                 Map<String, BoundaryPointData> boundaryPoints,
                                 Map<String, VolatilityGradient> volatilityGradients,
                                 BoundaryRegionMetadata metadata, long creationTime) {
            this.regionStartX = regionStartX;
            this.regionStartZ = regionStartZ;
            this.regionSize = regionSize;
            this.resolution = resolution;
            this.boundaryPoints = boundaryPoints;
            this.volatilityGradients = volatilityGradients;
            this.metadata = metadata;
            this.creationTime = creationTime;
        }
        
        /**
         * Get interpolated boundary distance
         */
        public float getInterpolatedBoundaryDistance(int worldX, int worldZ) {
            if (!metadata.isDetailed) {
                // Sparse region - return center value
                return metadata.minBoundaryDistance;
            }
            
            // Find nearest sample point
            int nearestX = (worldX / resolution) * resolution;
            int nearestZ = (worldZ / resolution) * resolution;
            String key = nearestX + ":" + nearestZ;
            
            BoundaryPointData point = boundaryPoints.get(key);
            return point != null ? point.distanceToBoundary : metadata.minBoundaryDistance;
        }
        
        /**
         * Get interpolated volatility using gradients
         */
        public float getInterpolatedVolatility(int worldX, int worldZ) {
            if (!metadata.isDetailed) {
                return metadata.avgVolatility;
            }
            
            // Find nearest sample point
            int nearestX = (worldX / resolution) * resolution;
            int nearestZ = (worldZ / resolution) * resolution;
            String key = nearestX + ":" + nearestZ;
            
            BoundaryPointData point = boundaryPoints.get(key);
            if (point == null) {
                return 0.0f;
            }
            
            // Use gradient for smooth interpolation if available
            VolatilityGradient gradient = volatilityGradients.get(key);
            if (gradient != null) {
                float dx = worldX - nearestX;
                float dz = worldZ - nearestZ;
                return gradient.baseVolatility + gradient.gradientX * dx + gradient.gradientZ * dz;
            }
            
            return point.volatility;
        }
        
        /**
         * Get boundary type at position
         */
        public BoundaryType getBoundaryType(int worldX, int worldZ) {
            if (!metadata.isDetailed) {
                return metadata.boundaryTypes.iterator().next();
            }
            
            int nearestX = (worldX / resolution) * resolution;
            int nearestZ = (worldZ / resolution) * resolution;
            String key = nearestX + ":" + nearestZ;
            
            BoundaryPointData point = boundaryPoints.get(key);
            return point != null ? point.boundaryType : BoundaryType.NONE;
        }
        
        /**
         * Get boundary strength at position
         */
        public float getBoundaryStrength(int worldX, int worldZ) {
            if (!metadata.isDetailed) {
                return 0.0f;
            }
            
            int nearestX = (worldX / resolution) * resolution;
            int nearestZ = (worldZ / resolution) * resolution;
            String key = nearestX + ":" + nearestZ;
            
            BoundaryPointData point = boundaryPoints.get(key);
            return point != null ? point.boundaryStrength : 0.0f;
        }
        
        // Getters
        public BoundaryRegionMetadata getMetadata() { return metadata; }
        public boolean hasDetailedData() { return metadata.isDetailed; }
        public boolean hasBoundaries() { return metadata.minBoundaryDistance < 1000.0f; }
        public long getCreationTime() { return creationTime; }
    }
    
    /**
     * Data for a single boundary sample point
     */
    public static class BoundaryPointData {
        final float distanceToBoundary;
        final BoundaryType boundaryType;
        final float volatility;
        final float boundaryStrength;
        
        public BoundaryPointData(float distanceToBoundary, BoundaryType boundaryType, 
                               float volatility, float boundaryStrength) {
            this.distanceToBoundary = distanceToBoundary;
            this.boundaryType = boundaryType;
            this.volatility = volatility;
            this.boundaryStrength = boundaryStrength;
        }
    }
    
    /**
     * Volatility gradient for smooth interpolation
     */
    public static class VolatilityGradient {
        final float baseVolatility;
        final float gradientX;
        final float gradientZ;
        final float gradientMagnitude;
        
        public VolatilityGradient(float baseVolatility, float gradientX, float gradientZ, float gradientMagnitude) {
            this.baseVolatility = baseVolatility;
            this.gradientX = gradientX;
            this.gradientZ = gradientZ;
            this.gradientMagnitude = gradientMagnitude;
        }
    }
    
    /**
     * Metadata about a boundary region
     */
    public static class BoundaryRegionMetadata {
        final boolean isDetailed;
        final Set<BoundaryType> boundaryTypes;
        final float minBoundaryDistance;
        final float maxVolatility;
        final float avgVolatility;
        final int totalPoints;
        final int gradientPoints;
        
        public BoundaryRegionMetadata(boolean isDetailed, Set<BoundaryType> boundaryTypes,
                                     float minBoundaryDistance, float maxVolatility, float avgVolatility,
                                     int totalPoints, int gradientPoints) {
            this.isDetailed = isDetailed;
            this.boundaryTypes = boundaryTypes;
            this.minBoundaryDistance = minBoundaryDistance;
            this.maxVolatility = maxVolatility;
            this.avgVolatility = avgVolatility;
            this.totalPoints = totalPoints;
            this.gradientPoints = gradientPoints;
        }
    }
    
    /**
     * Performance statistics
     */
    public static class PlateBoundaryCacheStatistics {
        public final int currentSize;
        public final int maxSize;
        public final long totalGenerations;
        public final double avgGenerationTime;
        public final long boundaryCalculations;
        public final long gradientCalculations;
        public final double hitRate;
        public final int regionSize;
        public final int resolution;
        
        public PlateBoundaryCacheStatistics(int currentSize, int maxSize, long totalGenerations,
                                           double avgGenerationTime, long boundaryCalculations,
                                           long gradientCalculations, double hitRate,
                                           int regionSize, int resolution) {
            this.currentSize = currentSize;
            this.maxSize = maxSize;
            this.totalGenerations = totalGenerations;
            this.avgGenerationTime = avgGenerationTime;
            this.boundaryCalculations = boundaryCalculations;
            this.gradientCalculations = gradientCalculations;
            this.hitRate = hitRate;
            this.regionSize = regionSize;
            this.resolution = resolution;
        }
        
        @Override
        public String toString() {
            return String.format("PlateBoundaryCache{size=%d/%d, generations=%d, avgTime=%.1fms, calculations=[boundary:%d,gradient:%d], hitRate=%.2f%%}", 
                    currentSize, maxSize, totalGenerations, avgGenerationTime, 
                    boundaryCalculations, gradientCalculations, hitRate * 100);
        }
    }
}