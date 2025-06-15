package net.starlight.terradyne.planet.terrain.cache;

import net.starlight.terradyne.planet.terrain.cache.TerrainCacheManager.TerrainCache;
import net.starlight.terradyne.planet.terrain.config.UnifiedTerrainConfig;
import net.starlight.terradyne.planet.terrain.math.TerrainMathUtils;
import net.starlight.terradyne.planet.terrain.noise.PlanetaryNoiseSystem;
import net.starlight.terradyne.planet.terrain.tectonic.TectonicSystemManager;
import net.starlight.terradyne.planet.terrain.tectonic.TectonicSystemManager.EnhancedTectonicPlate;
import net.starlight.terradyne.Terradyne;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches plate assignments and continental noise per 16x16 chunk regions
 * Reduces expensive plate lookups and noise calculations during terrain generation
 * Stores pre-calculated continental noise values at 4x4 block resolution
 */
public class ChunkRegionCache implements TerrainCache<ChunkRegionCache.ChunkRegionData> {
    
    private final Map<String, ChunkRegionData> cache;
    private final int maxSize;
    private final UnifiedTerrainConfig terrainConfig;
    
    // Dependencies for data generation
    private TectonicSystemManager tectonicSystem;
    private PlanetaryNoiseSystem noiseSystem;
    
    // Cache configuration
    private final int regionSize; // Size of cached region in chunks (default: 4x4 chunks)
    private final int noiseResolution; // Resolution for cached noise (4x4 blocks per cache entry)
    private final boolean enableBatchGeneration;
    
    // Performance tracking
    private long cacheGenerations = 0;
    private long totalGenerationTime = 0;
    private long batchHits = 0;
    private long individualHits = 0;
    
    /**
     * Initialize chunk region cache
     */
    public ChunkRegionCache(UnifiedTerrainConfig config) {
        this.terrainConfig = config;
        this.maxSize = config.getChunkRegionCacheSize();
        this.cache = new ConcurrentHashMap<>(maxSize);
        
        // Cache configuration
        this.regionSize = 4; // 4x4 chunks per region (64x64 blocks)
        this.noiseResolution = 4; // Sample noise every 4 blocks
        this.enableBatchGeneration = true;
        
        Terradyne.LOGGER.info("Chunk Region Cache initialized (max size: {}, region: {}x{} chunks, noise resolution: {})", 
                maxSize, regionSize, regionSize, noiseResolution);
    }
    
    /**
     * Set dependencies after initialization
     */
    public void setDependencies(TectonicSystemManager tectonicSystem, PlanetaryNoiseSystem noiseSystem) {
        this.tectonicSystem = tectonicSystem;
        this.noiseSystem = noiseSystem;
        Terradyne.LOGGER.debug("ChunkRegionCache dependencies set");
    }
    
    // === CHUNK REGION DATA GENERATION ===
    
    /**
     * Get cached data for a chunk region, generating if necessary
     */
    public ChunkRegionData getChunkRegionData(int chunkX, int chunkZ) {
        String regionKey = generateRegionKey(chunkX, chunkZ);
        
        ChunkRegionData cached = cache.get(regionKey);
        if (cached != null) {
            individualHits++;
            return cached;
        }
        
        // Generate new region data
        return generateChunkRegionData(chunkX, chunkZ);
    }
    
    /**
     * Generate comprehensive data for a chunk region
     */
    private ChunkRegionData generateChunkRegionData(int centerChunkX, int centerChunkZ) {
        if (tectonicSystem == null || noiseSystem == null) {
            throw new IllegalStateException("Dependencies not set for ChunkRegionCache");
        }
        
        long startTime = System.currentTimeMillis();
        cacheGenerations++;
        
        // Calculate region bounds
        int regionStartChunkX = (centerChunkX / regionSize) * regionSize;
        int regionStartChunkZ = (centerChunkZ / regionSize) * regionSize;
        int regionEndChunkX = regionStartChunkX + regionSize;
        int regionEndChunkZ = regionStartChunkZ + regionSize;
        
        // Calculate world coordinates
        int regionStartWorldX = regionStartChunkX * 16;
        int regionStartWorldZ = regionStartChunkZ * 16;
        int regionEndWorldX = regionEndChunkX * 16;
        int regionEndWorldZ = regionEndChunkZ * 16;
        
        // Generate plate assignment data
        PlateRegionData plateData = generatePlateRegionData(
            regionStartWorldX, regionStartWorldZ, regionEndWorldX, regionEndWorldZ
        );
        
        // Generate continental noise data
        ContinentalNoiseData continentalData = generateContinentalNoiseData(
            regionStartWorldX, regionStartWorldZ, regionEndWorldX, regionEndWorldZ
        );
        
        // Generate boundary proximity data
        BoundaryProximityData boundaryData = generateBoundaryProximityData(
            regionStartWorldX, regionStartWorldZ, regionEndWorldX, regionEndWorldZ
        );
        
        // Create region data
        ChunkRegionData regionData = new ChunkRegionData(
            regionStartChunkX, regionStartChunkZ, regionSize,
            plateData, continentalData, boundaryData,
            System.currentTimeMillis()
        );
        
        // Cache the data
        String regionKey = generateRegionKey(centerChunkX, centerChunkZ);
        put(regionKey, regionData);
        
        long generationTime = System.currentTimeMillis() - startTime;
        totalGenerationTime += generationTime;
        
        if (generationTime > 50) { // Log slow generations
            Terradyne.LOGGER.debug("Generated chunk region data in {}ms (region: {},{} size: {}x{})", 
                    generationTime, regionStartChunkX, regionStartChunkZ, regionSize, regionSize);
        }
        
        return regionData;
    }
    
    /**
     * Generate plate assignment data for the region
     */
    private PlateRegionData generatePlateRegionData(int startWorldX, int startWorldZ, int endWorldX, int endWorldZ) {
        Map<String, EnhancedTectonicPlate> plateAssignments = new HashMap<>();
        Map<String, Float> plateDistances = new HashMap<>();
        Set<EnhancedTectonicPlate> regionPlates = new HashSet<>();
        
        // Sample plates at regular intervals within the region
        int sampleInterval = 32; // Sample every 32 blocks
        
        for (int worldX = startWorldX; worldX < endWorldX; worldX += sampleInterval) {
            for (int worldZ = startWorldZ; worldZ < endWorldZ; worldZ += sampleInterval) {
                
                EnhancedTectonicPlate plate = tectonicSystem.getPlateAt(worldX, worldZ);
                if (plate != null) {
                    String key = generatePlateKey(worldX, worldZ, sampleInterval);
                    plateAssignments.put(key, plate);
                    
                    // Calculate distance to plate center for caching
                    float distance = TerrainMathUtils.distance2D(worldX, worldZ, plate.getCenterX(), plate.getCenterZ());
                    plateDistances.put(key, distance);
                    
                    regionPlates.add(plate);
                }
            }
        }
        
        // Determine dominant plate (most common in region)
        EnhancedTectonicPlate dominantPlate = findDominantPlate(plateAssignments.values());
        
        return new PlateRegionData(plateAssignments, plateDistances, regionPlates, dominantPlate);
    }
    
    /**
     * Generate continental noise data for the region
     */
    private ContinentalNoiseData generateContinentalNoiseData(int startWorldX, int startWorldZ, int endWorldX, int endWorldZ) {
        Map<String, Double> continentalNoise = new HashMap<>();
        Map<String, Double> regionalNoise = new HashMap<>();
        double minNoise = Double.MAX_VALUE;
        double maxNoise = Double.MIN_VALUE;
        double avgNoise = 0.0;
        int sampleCount = 0;
        
        // Sample continental noise at configured resolution
        for (int worldX = startWorldX; worldX < endWorldX; worldX += noiseResolution) {
            for (int worldZ = startWorldZ; worldZ < endWorldZ; worldZ += noiseResolution) {
                
                double noiseX = noiseSystem.worldToNoiseX(worldX);
                double noiseZ = noiseSystem.worldToNoiseZ(worldZ);
                
                // Sample continental terrain (continental + regional)
                double continental = noiseSystem.sampleContinentalTerrain(noiseX, noiseZ);
                
                String key = generateNoiseKey(worldX, worldZ, noiseResolution);
                continentalNoise.put(key, continental);
                
                // Track statistics
                minNoise = Math.min(minNoise, continental);
                maxNoise = Math.max(maxNoise, continental);
                avgNoise += continental;
                sampleCount++;
            }
        }
        
        if (sampleCount > 0) {
            avgNoise /= sampleCount;
        }
        
        return new ContinentalNoiseData(continentalNoise, minNoise, maxNoise, avgNoise);
    }
    
    /**
     * Generate boundary proximity data for the region
     */
    private BoundaryProximityData generateBoundaryProximityData(int startWorldX, int startWorldZ, int endWorldX, int endWorldZ) {
        Map<String, Float> boundaryDistances = new HashMap<>();
        Map<String, Float> volatilityValues = new HashMap<>();
        boolean hasBoundaries = false;
        float minBoundaryDistance = Float.MAX_VALUE;
        float maxVolatility = 0.0f;
        
        // Sample boundary data at medium resolution
        int boundaryResolution = 16; // Sample every 16 blocks for boundaries
        
        for (int worldX = startWorldX; worldX < endWorldX; worldX += boundaryResolution) {
            for (int worldZ = startWorldZ; worldZ < endWorldZ; worldZ += boundaryResolution) {
                
                var boundaryInfo = tectonicSystem.getBoundaryInfoAt(worldX, worldZ);
                
                String key = generateBoundaryKey(worldX, worldZ, boundaryResolution);
                boundaryDistances.put(key, boundaryInfo.distanceToBoundary);
                volatilityValues.put(key, boundaryInfo.volatility);
                
                // Track if this region has significant boundaries
                if (boundaryInfo.distanceToBoundary < terrainConfig.getVolatilityFalloffRadius()) {
                    hasBoundaries = true;
                }
                
                minBoundaryDistance = Math.min(minBoundaryDistance, boundaryInfo.distanceToBoundary);
                maxVolatility = Math.max(maxVolatility, Math.abs(boundaryInfo.volatility));
            }
        }
        
        return new BoundaryProximityData(boundaryDistances, volatilityValues, hasBoundaries, minBoundaryDistance, maxVolatility);
    }
    
    // === CACHED DATA ACCESS METHODS ===
    
    /**
     * Get plate at world coordinates using cached data
     */
    public EnhancedTectonicPlate getCachedPlateAt(int worldX, int worldZ) {
        ChunkRegionData regionData = getChunkRegionData(worldX / 16, worldZ / 16);
        return regionData.getPlateAt(worldX, worldZ);
    }
    
    /**
     * Get interpolated continental noise using cached data
     */
    public double getCachedContinentalNoise(int worldX, int worldZ) {
        ChunkRegionData regionData = getChunkRegionData(worldX / 16, worldZ / 16);
        return regionData.getInterpolatedContinentalNoise(worldX, worldZ);
    }
    
    /**
     * Get boundary distance using cached data
     */
    public float getCachedBoundaryDistance(int worldX, int worldZ) {
        ChunkRegionData regionData = getChunkRegionData(worldX / 16, worldZ / 16);
        return regionData.getBoundaryDistance(worldX, worldZ);
    }
    
    /**
     * Get volatility using cached data
     */
    public float getCachedVolatility(int worldX, int worldZ) {
        ChunkRegionData regionData = getChunkRegionData(worldX / 16, worldZ / 16);
        return regionData.getVolatility(worldX, worldZ);
    }
    
    // === BATCH OPERATIONS ===
    
    /**
     * Pre-generate cache data for multiple chunk regions
     */
    public void preGenerateRegions(int centerChunkX, int centerChunkZ, int radius) {
        if (!enableBatchGeneration) return;
        
        long startTime = System.currentTimeMillis();
        int generated = 0;
        
        for (int chunkX = centerChunkX - radius; chunkX <= centerChunkX + radius; chunkX += regionSize) {
            for (int chunkZ = centerChunkZ - radius; chunkZ <= centerChunkZ + radius; chunkZ += regionSize) {
                
                String regionKey = generateRegionKey(chunkX, chunkZ);
                if (!cache.containsKey(regionKey)) {
                    generateChunkRegionData(chunkX, chunkZ);
                    generated++;
                }
            }
        }
        
        if (generated > 0) {
            long duration = System.currentTimeMillis() - startTime;
            batchHits++;
            Terradyne.LOGGER.debug("Pre-generated {} chunk regions in {}ms (radius: {})", generated, duration, radius);
        }
    }
    
    // === KEY GENERATION ===
    
    private String generateRegionKey(int chunkX, int chunkZ) {
        int regionX = (chunkX / regionSize) * regionSize;
        int regionZ = (chunkZ / regionSize) * regionSize;
        return "region:" + regionX + ":" + regionZ;
    }
    
    private String generatePlateKey(int worldX, int worldZ, int resolution) {
        int gridX = (worldX / resolution) * resolution;
        int gridZ = (worldZ / resolution) * resolution;
        return gridX + ":" + gridZ;
    }
    
    private String generateNoiseKey(int worldX, int worldZ, int resolution) {
        int gridX = (worldX / resolution) * resolution;
        int gridZ = (worldZ / resolution) * resolution;
        return gridX + ":" + gridZ;
    }
    
    private String generateBoundaryKey(int worldX, int worldZ, int resolution) {
        int gridX = (worldX / resolution) * resolution;
        int gridZ = (worldZ / resolution) * resolution;
        return gridX + ":" + gridZ;
    }
    
    // === UTILITY METHODS ===
    
    private EnhancedTectonicPlate findDominantPlate(Collection<EnhancedTectonicPlate> plates) {
        Map<EnhancedTectonicPlate, Integer> plateCounts = new HashMap<>();
        
        for (EnhancedTectonicPlate plate : plates) {
            plateCounts.merge(plate, 1, Integer::sum);
        }
        
        return plateCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }
    
    /**
     * Get cache performance statistics
     */
    public ChunkRegionCacheStatistics getStatistics() {
        double avgGenerationTime = cacheGenerations > 0 ? (double) totalGenerationTime / cacheGenerations : 0.0;
        
        return new ChunkRegionCacheStatistics(
            cache.size(),
            maxSize,
            cacheGenerations,
            avgGenerationTime,
            batchHits,
            individualHits,
            regionSize,
            noiseResolution
        );
    }
    
    // === TERRAINCACHE INTERFACE IMPLEMENTATION ===
    
    @Override
    public ChunkRegionData get(String key) {
        return cache.get(key);
    }
    
    @Override
    public ChunkRegionData put(String key, ChunkRegionData value) {
        if (cache.size() >= maxSize && !cache.containsKey(key)) {
            // Remove oldest entry (simple LRU approximation)
            String oldestKey = cache.keySet().iterator().next();
            cache.remove(oldestKey);
        }
        return cache.put(key, value);
    }
    
    @Override
    public ChunkRegionData remove(String key) {
        return cache.remove(key);
    }
    
    @Override
    public boolean containsKey(String key) {
        return cache.containsKey(key);
    }
    
    @Override
    public void clear() {
        cache.clear();
        Terradyne.LOGGER.debug("Cleared chunk region cache");
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
     * Complete data for a chunk region
     */
    public static class ChunkRegionData {
        private final int regionStartChunkX, regionStartChunkZ;
        private final int regionSize;
        private final PlateRegionData plateData;
        private final ContinentalNoiseData continentalData;
        private final BoundaryProximityData boundaryData;
        private final long creationTime;
        
        public ChunkRegionData(int regionStartChunkX, int regionStartChunkZ, int regionSize,
                              PlateRegionData plateData, ContinentalNoiseData continentalData,
                              BoundaryProximityData boundaryData, long creationTime) {
            this.regionStartChunkX = regionStartChunkX;
            this.regionStartChunkZ = regionStartChunkZ;
            this.regionSize = regionSize;
            this.plateData = plateData;
            this.continentalData = continentalData;
            this.boundaryData = boundaryData;
            this.creationTime = creationTime;
        }
        
        /**
         * Get plate at world coordinates within this region
         */
        public EnhancedTectonicPlate getPlateAt(int worldX, int worldZ) {
            // Find nearest cached plate assignment
            int sampleInterval = 32;
            int nearestX = (worldX / sampleInterval) * sampleInterval;
            int nearestZ = (worldZ / sampleInterval) * sampleInterval;
            
            String key = nearestX + ":" + nearestZ;
            EnhancedTectonicPlate plate = plateData.plateAssignments.get(key);
            
            return plate != null ? plate : plateData.dominantPlate;
        }
        
        /**
         * Get interpolated continental noise at world coordinates
         */
        public double getInterpolatedContinentalNoise(int worldX, int worldZ) {
            int resolution = 4;
            
            // Find surrounding sample points
            int x0 = (worldX / resolution) * resolution;
            int z0 = (worldZ / resolution) * resolution;
            int x1 = x0 + resolution;
            int z1 = z0 + resolution;
            
            // Get noise values at corners
            String key00 = x0 + ":" + z0;
            String key01 = x0 + ":" + z1;
            String key10 = x1 + ":" + z0;
            String key11 = x1 + ":" + z1;
            
            Double n00 = continentalData.continentalNoise.get(key00);
            Double n01 = continentalData.continentalNoise.get(key01);
            Double n10 = continentalData.continentalNoise.get(key10);
            Double n11 = continentalData.continentalNoise.get(key11);
            
            // Use bilinear interpolation if all points available
            if (n00 != null && n01 != null && n10 != null && n11 != null) {
                double tx = (double)(worldX - x0) / resolution;
                double tz = (double)(worldZ - z0) / resolution;
                return TerrainMathUtils.bilerp(n00, n10, n01, n11, tx, tz);
            }
            
            // Fallback to nearest available value or average
            if (n00 != null) return n00;
            if (n01 != null) return n01;
            if (n10 != null) return n10;
            if (n11 != null) return n11;
            
            return continentalData.avgNoise;
        }
        
        /**
         * Get boundary distance at world coordinates
         */
        public float getBoundaryDistance(int worldX, int worldZ) {
            int resolution = 16;
            int nearestX = (worldX / resolution) * resolution;
            int nearestZ = (worldZ / resolution) * resolution;
            
            String key = nearestX + ":" + nearestZ;
            Float distance = boundaryData.boundaryDistances.get(key);
            
            return distance != null ? distance : boundaryData.minBoundaryDistance;
        }
        
        /**
         * Get volatility at world coordinates
         */
        public float getVolatility(int worldX, int worldZ) {
            int resolution = 16;
            int nearestX = (worldX / resolution) * resolution;
            int nearestZ = (worldZ / resolution) * resolution;
            
            String key = nearestX + ":" + nearestZ;
            Float volatility = boundaryData.volatilityValues.get(key);
            
            return volatility != null ? volatility : 0.0f;
        }
        
        // Getters
        public PlateRegionData getPlateData() { return plateData; }
        public ContinentalNoiseData getContinentalData() { return continentalData; }
        public BoundaryProximityData getBoundaryData() { return boundaryData; }
        public boolean hasBoundaries() { return boundaryData.hasBoundaries; }
        public Set<EnhancedTectonicPlate> getRegionPlates() { return plateData.regionPlates; }
        public long getCreationTime() { return creationTime; }
    }
    
    /**
     * Plate data for a region
     */
    public static class PlateRegionData {
        final Map<String, EnhancedTectonicPlate> plateAssignments;
        final Map<String, Float> plateDistances;
        final Set<EnhancedTectonicPlate> regionPlates;
        final EnhancedTectonicPlate dominantPlate;
        
        public PlateRegionData(Map<String, EnhancedTectonicPlate> plateAssignments,
                              Map<String, Float> plateDistances,
                              Set<EnhancedTectonicPlate> regionPlates,
                              EnhancedTectonicPlate dominantPlate) {
            this.plateAssignments = plateAssignments;
            this.plateDistances = plateDistances;
            this.regionPlates = regionPlates;
            this.dominantPlate = dominantPlate;
        }
    }
    
    /**
     * Continental noise data for a region
     */
    public static class ContinentalNoiseData {
        final Map<String, Double> continentalNoise;
        final double minNoise, maxNoise, avgNoise;
        
        public ContinentalNoiseData(Map<String, Double> continentalNoise, 
                                   double minNoise, double maxNoise, double avgNoise) {
            this.continentalNoise = continentalNoise;
            this.minNoise = minNoise;
            this.maxNoise = maxNoise;
            this.avgNoise = avgNoise;
        }
    }
    
    /**
     * Boundary proximity data for a region
     */
    public static class BoundaryProximityData {
        final Map<String, Float> boundaryDistances;
        final Map<String, Float> volatilityValues;
        final boolean hasBoundaries;
        final float minBoundaryDistance;
        final float maxVolatility;
        
        public BoundaryProximityData(Map<String, Float> boundaryDistances,
                                    Map<String, Float> volatilityValues,
                                    boolean hasBoundaries,
                                    float minBoundaryDistance,
                                    float maxVolatility) {
            this.boundaryDistances = boundaryDistances;
            this.volatilityValues = volatilityValues;
            this.hasBoundaries = hasBoundaries;
            this.minBoundaryDistance = minBoundaryDistance;
            this.maxVolatility = maxVolatility;
        }
    }
    
    /**
     * Performance statistics
     */
    public static class ChunkRegionCacheStatistics {
        public final int currentSize;
        public final int maxSize;
        public final long totalGenerations;
        public final double avgGenerationTime;
        public final long batchHits;
        public final long individualHits;
        public final int regionSize;
        public final int noiseResolution;
        
        public ChunkRegionCacheStatistics(int currentSize, int maxSize, long totalGenerations,
                                         double avgGenerationTime, long batchHits, long individualHits,
                                         int regionSize, int noiseResolution) {
            this.currentSize = currentSize;
            this.maxSize = maxSize;
            this.totalGenerations = totalGenerations;
            this.avgGenerationTime = avgGenerationTime;
            this.batchHits = batchHits;
            this.individualHits = individualHits;
            this.regionSize = regionSize;
            this.noiseResolution = noiseResolution;
        }
        
        @Override
        public String toString() {
            return String.format("ChunkRegionCache{size=%d/%d, generations=%d, avgTime=%.1fms, hits=[batch:%d,individual:%d]}", 
                    currentSize, maxSize, totalGenerations, avgGenerationTime, batchHits, individualHits);
        }
    }
}