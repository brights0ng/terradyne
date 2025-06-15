package net.starlight.terradyne.planet.terrain.cache;

import net.starlight.terradyne.planet.terrain.config.UnifiedTerrainConfig;
import net.starlight.terradyne.planet.terrain.math.TerrainMathUtils;
import net.starlight.terradyne.Terradyne;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Master coordinator for all terrain generation caching strategies
 * Manages memory usage, cache lifecycles, and performance optimization
 * Provides unified interface for cache access across all terrain systems
 */
public class TerrainCacheManager {
    
    // Cache type registry
    private final Map<CacheType, TerrainCache<?>> registeredCaches;
    private final UnifiedTerrainConfig terrainConfig;
    
    // Memory management
    private final long maxTotalMemoryBytes;
    private final AtomicLong currentMemoryUsage;
    private final boolean enableMemoryManagement;
    
    // Performance monitoring
    private final Map<CacheType, CacheMetrics> cacheMetrics;
    private final AtomicLong totalCacheAccesses;
    private final AtomicLong totalCacheHits;
    private final AtomicLong totalCacheMisses;
    
    // Automatic optimization
    private final ScheduledExecutorService optimizationScheduler;
    private final boolean enableAutoOptimization;
    private final long optimizationIntervalMs;
    
    // Cache invalidation tracking
    private final Map<String, Long> lastModificationTimes;
    private final long cacheValidityDurationMs;
    
    /**
     * Initialize the terrain cache manager
     */
    public TerrainCacheManager(UnifiedTerrainConfig config) {
        this.terrainConfig = config;
        this.enableMemoryManagement = config.isEnableCaching();
        this.maxTotalMemoryBytes = config.getMaxCacheMemoryMB() * 1024L * 1024L;
        this.currentMemoryUsage = new AtomicLong(0);
        
        // Initialize collections
        this.registeredCaches = new ConcurrentHashMap<>();
        this.cacheMetrics = new ConcurrentHashMap<>();
        this.lastModificationTimes = new ConcurrentHashMap<>();
        
        // Performance counters
        this.totalCacheAccesses = new AtomicLong(0);
        this.totalCacheHits = new AtomicLong(0);
        this.totalCacheMisses = new AtomicLong(0);
        
        // Auto-optimization settings
        this.enableAutoOptimization = config.isEnablePerformanceProfiling();
        this.optimizationIntervalMs = 30000; // 30 seconds
        this.cacheValidityDurationMs = 300000; // 5 minutes
        
        // Initialize optimization scheduler
        if (enableAutoOptimization) {
            this.optimizationScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "TerrainCacheOptimizer");
                t.setDaemon(true);
                return t;
            });
            
            // Schedule periodic optimization
            optimizationScheduler.scheduleAtFixedRate(
                this::performAutoOptimization,
                optimizationIntervalMs,
                optimizationIntervalMs,
                TimeUnit.MILLISECONDS
            );
        } else {
            this.optimizationScheduler = null;
        }
        
        Terradyne.LOGGER.info("=== TERRAIN CACHE MANAGER INITIALIZED ===");
        Terradyne.LOGGER.info("Memory Management: {}", enableMemoryManagement ? "Enabled" : "Disabled");
        Terradyne.LOGGER.info("Max Total Memory: {} MB", config.getMaxCacheMemoryMB());
        Terradyne.LOGGER.info("Auto Optimization: {}", enableAutoOptimization ? "Enabled" : "Disabled");
        Terradyne.LOGGER.info("Cache Validity: {} minutes", cacheValidityDurationMs / 60000);
    }
    
    // === CACHE REGISTRATION ===
    
    /**
     * Register a cache with the manager
     */
    public <T> void registerCache(CacheType type, TerrainCache<T> cache) {
        if (registeredCaches.containsKey(type)) {
            Terradyne.LOGGER.warn("Cache type {} already registered, replacing", type);
        }
        
        registeredCaches.put(type, cache);
        cacheMetrics.put(type, new CacheMetrics());
        
        Terradyne.LOGGER.info("Registered {} cache (max size: {}, estimated memory: {} KB)", 
                type.getDisplayName(), 
                cache.getMaxSize(),
                estimateCacheMemoryUsage(cache) / 1024);
    }
    
    /**
     * Unregister a cache
     */
    public void unregisterCache(CacheType type) {
        TerrainCache<?> cache = registeredCaches.remove(type);
        if (cache != null) {
            long memoryFreed = estimateCacheMemoryUsage(cache);
            currentMemoryUsage.addAndGet(-memoryFreed);
            
            cache.clear();
            cacheMetrics.remove(type);
            
            Terradyne.LOGGER.info("Unregistered {} cache, freed {} KB", 
                    type.getDisplayName(), memoryFreed / 1024);
        }
    }
    
    // === UNIFIED CACHE ACCESS INTERFACE ===
    
    /**
     * Get a value from a specific cache
     */
    @SuppressWarnings("unchecked")
    public <T> T get(CacheType type, String key) {
        totalCacheAccesses.incrementAndGet();
        
        TerrainCache<T> cache = (TerrainCache<T>) registeredCaches.get(type);
        if (cache == null) {
            totalCacheMisses.incrementAndGet();
            return null;
        }
        
        // Check cache validity
        if (!isCacheEntryValid(type, key)) {
            cache.remove(key);
            updateCacheMetrics(type, false);
            totalCacheMisses.incrementAndGet();
            return null;
        }
        
        T value = cache.get(key);
        boolean hit = (value != null);
        
        updateCacheMetrics(type, hit);
        
        if (hit) {
            totalCacheHits.incrementAndGet();
        } else {
            totalCacheMisses.incrementAndGet();
        }
        
        return value;
    }
    
    /**
     * Put a value into a specific cache
     */
    @SuppressWarnings("unchecked")
    public <T> void put(CacheType type, String key, T value) {
        TerrainCache<T> cache = (TerrainCache<T>) registeredCaches.get(type);
        if (cache == null) {
            Terradyne.LOGGER.warn("Attempted to put value in unregistered cache: {}", type);
            return;
        }
        
        // Check memory constraints before adding
        if (enableMemoryManagement && !canAddToCache(cache, key, value)) {
            performEmergencyOptimization();
            
            // Try again after optimization
            if (!canAddToCache(cache, key, value)) {
                Terradyne.LOGGER.debug("Skipping cache put due to memory constraints: {}", type);
                return;
            }
        }
        
        // Calculate memory impact
        long memoryDelta = estimateEntryMemoryUsage(key, value);
        
        // Put the value
        T oldValue = cache.put(key, value);
        
        // Update memory tracking
        if (oldValue == null) {
            currentMemoryUsage.addAndGet(memoryDelta);
        }
        
        // Mark entry as valid
        markCacheEntryValid(type, key);
    }
    
    /**
     * Remove a value from a specific cache
     */
    @SuppressWarnings("unchecked")
    public <T> T remove(CacheType type, String key) {
        TerrainCache<T> cache = (TerrainCache<T>) registeredCaches.get(type);
        if (cache == null) {
            return null;
        }
        
        T removed = cache.remove(key);
        if (removed != null) {
            long memoryFreed = estimateEntryMemoryUsage(key, removed);
            currentMemoryUsage.addAndGet(-memoryFreed);
        }
        
        invalidateCacheEntry(type, key);
        return removed;
    }
    
    /**
     * Check if a cache contains a key
     */
    public boolean contains(CacheType type, String key) {
        TerrainCache<?> cache = registeredCaches.get(type);
        return cache != null && cache.containsKey(key) && isCacheEntryValid(type, key);
    }
    
    /**
     * Get cache size for a specific type
     */
    public int getSize(CacheType type) {
        TerrainCache<?> cache = registeredCaches.get(type);
        return cache != null ? cache.size() : 0;
    }
    
    // === CACHE OPTIMIZATION ===
    
    /**
     * Perform automatic cache optimization
     */
    private void performAutoOptimization() {
        try {
            long startTime = System.currentTimeMillis();
            
            // Check memory usage
            double memoryUsagePercent = (double) currentMemoryUsage.get() / maxTotalMemoryBytes;
            
            if (memoryUsagePercent > 0.8) {
                // High memory usage - aggressive optimization
                performMemoryOptimization(0.3); // Free 30% of memory
            } else if (memoryUsagePercent > 0.6) {
                // Moderate memory usage - light optimization
                performMemoryOptimization(0.1); // Free 10% of memory
            }
            
            // Clean up invalid entries
            cleanupInvalidEntries();
            
            // Update cache efficiency metrics
            updateCacheEfficiencyMetrics();
            
            long duration = System.currentTimeMillis() - startTime;
            if (duration > 100) { // Log if optimization takes more than 100ms
                Terradyne.LOGGER.debug("Cache optimization completed in {}ms", duration);
            }
            
        } catch (Exception e) {
            Terradyne.LOGGER.error("Error during cache optimization", e);
        }
    }
    
    /**
     * Perform emergency optimization when memory is critically low
     */
    private void performEmergencyOptimization() {
        Terradyne.LOGGER.warn("Performing emergency cache optimization - memory usage critical");
        
        // Aggressive cleanup - remove 50% of cached data
        performMemoryOptimization(0.5);
        
        // Force cleanup of invalid entries
        cleanupInvalidEntries();
    }
    
    /**
     * Optimize memory usage by removing least efficient cache entries
     */
    private void performMemoryOptimization(double targetReductionPercent) {
        long targetReduction = (long) (currentMemoryUsage.get() * targetReductionPercent);
        long freedMemory = 0;
        
        // Sort caches by efficiency (hit rate / memory usage)
        List<Map.Entry<CacheType, TerrainCache<?>>> cacheEntries = new ArrayList<>();
        for (Map.Entry<CacheType, TerrainCache<?>> entry : registeredCaches.entrySet()) {
            cacheEntries.add(entry);
        }
        
        // Sort by efficiency (least efficient first)
        cacheEntries.sort((a, b) -> {
            double efficiencyA = calculateCacheEfficiency(a.getKey());
            double efficiencyB = calculateCacheEfficiency(b.getKey());
            return Double.compare(efficiencyA, efficiencyB);
        });
        
        // Remove entries from least efficient caches first
        for (Map.Entry<CacheType, TerrainCache<?>> entry : cacheEntries) {
            if (freedMemory >= targetReduction) break;
            
            CacheType type = entry.getKey();
            TerrainCache<?> cache = entry.getValue();
            
            int entriesToRemove = Math.max(1, cache.size() / 4); // Remove 25% of entries
            freedMemory += optimizeSpecificCache(type, cache, entriesToRemove);
        }
        
        Terradyne.LOGGER.debug("Memory optimization freed {} KB", freedMemory / 1024);
    }
    
    /**
     * Optimize a specific cache by removing least recently used entries
     */
    private long optimizeSpecificCache(CacheType type, TerrainCache<?> cache, int entriesToRemove) {
        // For now, use simple approach - remove oldest entries
        // In a more sophisticated implementation, we'd track LRU
        
        long freedMemory = 0;
        int removed = 0;
        
        Iterator<?> iterator = cache.keySet().iterator();
        while (iterator.hasNext() && removed < entriesToRemove) {
            Object key = iterator.next();
            Object value = cache.get((String) key);
            
            if (value != null) {
                freedMemory += estimateEntryMemoryUsage(key.toString(), value);
                iterator.remove();
                removed++;
            }
        }
        
        currentMemoryUsage.addAndGet(-freedMemory);
        return freedMemory;
    }
    
    /**
     * Clean up entries that are no longer valid
     */
    private void cleanupInvalidEntries() {
        long currentTime = System.currentTimeMillis();
        long freedMemory = 0;
        
        Iterator<Map.Entry<String, Long>> iterator = lastModificationTimes.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            String cacheKey = entry.getKey();
            long modTime = entry.getValue();
            
            if (currentTime - modTime > cacheValidityDurationMs) {
                // Entry is too old, remove it
                String[] parts = cacheKey.split(":", 2);
                if (parts.length == 2) {
                    try {
                        CacheType type = CacheType.valueOf(parts[0]);
                        String key = parts[1];
                        
                        TerrainCache<?> cache = registeredCaches.get(type);
                        if (cache != null) {
                            Object value = cache.remove(key);
                            if (value != null) {
                                freedMemory += estimateEntryMemoryUsage(key, value);
                            }
                        }
                    } catch (IllegalArgumentException e) {
                        // Invalid cache type, ignore
                    }
                }
                
                iterator.remove();
            }
        }
        
        if (freedMemory > 0) {
            currentMemoryUsage.addAndGet(-freedMemory);
            Terradyne.LOGGER.debug("Cleaned up invalid entries, freed {} KB", freedMemory / 1024);
        }
    }
    
    // === MEMORY MANAGEMENT ===
    
    /**
     * Check if we can add an entry to cache without exceeding memory limits
     */
    private <T> boolean canAddToCache(TerrainCache<T> cache, String key, T value) {
        if (!enableMemoryManagement) return true;
        
        long entrySize = estimateEntryMemoryUsage(key, value);
        long projectedUsage = currentMemoryUsage.get() + entrySize;
        
        return projectedUsage <= maxTotalMemoryBytes;
    }
    
    /**
     * Estimate memory usage of a cache entry
     */
    private <T> long estimateEntryMemoryUsage(String key, T value) {
        long keySize = key.length() * 2; // Rough UTF-16 estimate
        long valueSize = estimateObjectSize(value);
        long overhead = 64; // Map entry overhead
        
        return keySize + valueSize + overhead;
    }
    
    /**
     * Estimate memory usage of an entire cache
     */
    private long estimateCacheMemoryUsage(TerrainCache<?> cache) {
        // Rough estimation based on cache size and average entry size
        int size = cache.size();
        if (size == 0) return 0;
        
        // Estimate average entry size (key + value + overhead)
        long averageEntrySize = 256; // Conservative estimate
        long mapOverhead = size * 32; // Hash map overhead
        
        return size * averageEntrySize + mapOverhead;
    }
    
    /**
     * Estimate object memory size (rough approximation)
     */
    private long estimateObjectSize(Object value) {
        if (value == null) return 0;
        
        // Rough estimates for common types
        if (value instanceof String) {
            return ((String) value).length() * 2 + 40;
        } else if (value instanceof Double || value instanceof Float) {
            return 24;
        } else if (value instanceof Integer || value instanceof Boolean) {
            return 16;
        } else if (value instanceof List<?>) {
            return ((List<?>) value).size() * 8 + 40;
        } else if (value instanceof Map<?, ?>) {
            return ((Map<?, ?>) value).size() * 16 + 40;
        } else {
            // Default estimate for complex objects
            return 128;
        }
    }
    
    // === CACHE VALIDITY TRACKING ===
    
    /**
     * Mark a cache entry as valid (recently accessed/updated)
     */
    private void markCacheEntryValid(CacheType type, String key) {
        String cacheKey = type.name() + ":" + key;
        lastModificationTimes.put(cacheKey, System.currentTimeMillis());
    }
    
    /**
     * Check if a cache entry is still valid
     */
    private boolean isCacheEntryValid(CacheType type, String key) {
        String cacheKey = type.name() + ":" + key;
        Long modTime = lastModificationTimes.get(cacheKey);
        
        if (modTime == null) return false;
        
        long currentTime = System.currentTimeMillis();
        return (currentTime - modTime) <= cacheValidityDurationMs;
    }
    
    /**
     * Invalidate a specific cache entry
     */
    private void invalidateCacheEntry(CacheType type, String key) {
        String cacheKey = type.name() + ":" + key;
        lastModificationTimes.remove(cacheKey);
    }
    
    // === PERFORMANCE METRICS ===
    
    /**
     * Update cache metrics for a specific cache type
     */
    private void updateCacheMetrics(CacheType type, boolean hit) {
        CacheMetrics metrics = cacheMetrics.get(type);
        if (metrics != null) {
            metrics.recordAccess(hit);
        }
    }
    
    /**
     * Calculate cache efficiency (hit rate / memory usage ratio)
     */
    private double calculateCacheEfficiency(CacheType type) {
        CacheMetrics metrics = cacheMetrics.get(type);
        TerrainCache<?> cache = registeredCaches.get(type);
        
        if (metrics == null || cache == null) return 0.0;
        
        double hitRate = metrics.getHitRate();
        long memoryUsage = estimateCacheMemoryUsage(cache);
        
        return memoryUsage > 0 ? hitRate / (memoryUsage / 1024.0) : 0.0; // Efficiency per KB
    }
    
    /**
     * Update efficiency metrics for all caches
     */
    private void updateCacheEfficiencyMetrics() {
        for (CacheType type : registeredCaches.keySet()) {
            double efficiency = calculateCacheEfficiency(type);
            CacheMetrics metrics = cacheMetrics.get(type);
            if (metrics != null) {
                metrics.updateEfficiency(efficiency);
            }
        }
    }
    
    // === PUBLIC MANAGEMENT METHODS ===
    
    /**
     * Clear all caches
     */
    public void clearAllCaches() {
        for (TerrainCache<?> cache : registeredCaches.values()) {
            cache.clear();
        }
        
        currentMemoryUsage.set(0);
        lastModificationTimes.clear();
        
        // Reset metrics
        for (CacheMetrics metrics : cacheMetrics.values()) {
            metrics.reset();
        }
        
        Terradyne.LOGGER.info("Cleared all terrain caches");
    }
    
    /**
     * Clear a specific cache type
     */
    public void clearCache(CacheType type) {
        TerrainCache<?> cache = registeredCaches.get(type);
        if (cache != null) {
            long memoryFreed = estimateCacheMemoryUsage(cache);
            cache.clear();
            currentMemoryUsage.addAndGet(-memoryFreed);
            
            // Remove validity tracking for this cache type
            lastModificationTimes.entrySet().removeIf(entry -> entry.getKey().startsWith(type.name() + ":"));
            
            CacheMetrics metrics = cacheMetrics.get(type);
            if (metrics != null) {
                metrics.reset();
            }
            
            Terradyne.LOGGER.info("Cleared {} cache, freed {} KB", type.getDisplayName(), memoryFreed / 1024);
        }
    }
    
    /**
     * Force optimization of all caches
     */
    public void optimizeAllCaches() {
        performAutoOptimization();
    }
    
    /**
     * Get overall cache statistics
     */
    public CacheManagerStatistics getStatistics() {
        Map<CacheType, CacheTypeStatistics> typeStats = new HashMap<>();
        
        for (CacheType type : registeredCaches.keySet()) {
            TerrainCache<?> cache = registeredCaches.get(type);
            CacheMetrics metrics = cacheMetrics.get(type);
            
            if (cache != null && metrics != null) {
                typeStats.put(type, new CacheTypeStatistics(
                    cache.size(),
                    cache.getMaxSize(),
                    estimateCacheMemoryUsage(cache),
                    metrics.getHitRate(),
                    metrics.getEfficiency(),
                    metrics.getTotalAccesses(),
                    metrics.getTotalHits()
                ));
            }
        }
        
        return new CacheManagerStatistics(
            totalCacheAccesses.get(),
            totalCacheHits.get(),
            totalCacheMisses.get(),
            currentMemoryUsage.get(),
            maxTotalMemoryBytes,
            typeStats
        );
    }
    
    // === CLEANUP ===
    
    /**
     * Shutdown the cache manager
     */
    public void shutdown() {
        if (optimizationScheduler != null) {
            optimizationScheduler.shutdown();
            try {
                if (!optimizationScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    optimizationScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                optimizationScheduler.shutdownNow();
            }
        }
        
        clearAllCaches();
        Terradyne.LOGGER.info("Terrain cache manager shut down");
    }
    
    // === INNER CLASSES ===
    
    /**
     * Cache types enumeration
     */
    public enum CacheType {
        NOISE("Noise Cache"),
        PLATE("Plate Cache"),
        BOUNDARY("Boundary Cache"),
        VOLATILITY("Volatility Cache"),
        CHUNK_REGION("Chunk Region Cache"),
        CONTINENTAL("Continental Cache"),
        EROSION("Erosion Cache");
        
        private final String displayName;
        
        CacheType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * Basic cache interface
     */
    public interface TerrainCache<T> {
        T get(String key);
        T put(String key, T value);
        T remove(String key);
        boolean containsKey(String key);
        void clear();
        int size();
        int getMaxSize();
        Set<String> keySet();
    }
    
    /**
     * Cache metrics tracking
     */
    private static class CacheMetrics {
        private final AtomicLong totalAccesses = new AtomicLong(0);
        private final AtomicLong totalHits = new AtomicLong(0);
        private volatile double efficiency = 0.0;
        
        void recordAccess(boolean hit) {
            totalAccesses.incrementAndGet();
            if (hit) {
                totalHits.incrementAndGet();
            }
        }
        
        void updateEfficiency(double efficiency) {
            this.efficiency = efficiency;
        }
        
        void reset() {
            totalAccesses.set(0);
            totalHits.set(0);
            efficiency = 0.0;
        }
        
        double getHitRate() {
            long accesses = totalAccesses.get();
            return accesses > 0 ? (double) totalHits.get() / accesses : 0.0;
        }
        
        double getEfficiency() { return efficiency; }
        long getTotalAccesses() { return totalAccesses.get(); }
        long getTotalHits() { return totalHits.get(); }
    }
    
    /**
     * Statistics for a specific cache type
     */
    public static class CacheTypeStatistics {
        public final int currentSize;
        public final int maxSize;
        public final long memoryUsage;
        public final double hitRate;
        public final double efficiency;
        public final long totalAccesses;
        public final long totalHits;
        
        public CacheTypeStatistics(int currentSize, int maxSize, long memoryUsage, 
                                 double hitRate, double efficiency, long totalAccesses, long totalHits) {
            this.currentSize = currentSize;
            this.maxSize = maxSize;
            this.memoryUsage = memoryUsage;
            this.hitRate = hitRate;
            this.efficiency = efficiency;
            this.totalAccesses = totalAccesses;
            this.totalHits = totalHits;
        }
    }
    
    /**
     * Overall cache manager statistics
     */
    public static class CacheManagerStatistics {
        public final long totalAccesses;
        public final long totalHits;
        public final long totalMisses;
        public final long currentMemoryUsage;
        public final long maxMemoryUsage;
        public final double overallHitRate;
        public final double memoryUsagePercent;
        public final Map<CacheType, CacheTypeStatistics> typeStatistics;
        
        public CacheManagerStatistics(long totalAccesses, long totalHits, long totalMisses,
                                    long currentMemoryUsage, long maxMemoryUsage,
                                    Map<CacheType, CacheTypeStatistics> typeStatistics) {
            this.totalAccesses = totalAccesses;
            this.totalHits = totalHits;
            this.totalMisses = totalMisses;
            this.currentMemoryUsage = currentMemoryUsage;
            this.maxMemoryUsage = maxMemoryUsage;
            this.overallHitRate = totalAccesses > 0 ? (double) totalHits / totalAccesses : 0.0;
            this.memoryUsagePercent = maxMemoryUsage > 0 ? (double) currentMemoryUsage / maxMemoryUsage : 0.0;
            this.typeStatistics = typeStatistics;
        }
        
        @Override
        public String toString() {
            return String.format("CacheManager{accesses=%d, hits=%d, hitRate=%.2f%%, memory=%dKB/%.1f%%}", 
                    totalAccesses, totalHits, overallHitRate * 100, 
                    currentMemoryUsage / 1024, memoryUsagePercent * 100);
        }
    }
}