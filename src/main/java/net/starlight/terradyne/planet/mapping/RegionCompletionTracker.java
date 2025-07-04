package net.starlight.terradyne.planet.mapping;

import net.starlight.terradyne.Terradyne;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which regions have been fully generated and can be removed from cache
 * UPDATED: Now includes volatility caching for tectonic plate boundaries
 * Manages lifecycle of regional noise cache based on chunk generation completion
 */
public class RegionCompletionTracker {

    /**
     * Region identifier for 32×32 chunk Minecraft regions
     */
    public static class RegionKey {
        public final int regionX;
        public final int regionZ;
        public final String planetName;

        public RegionKey(String planetName, int regionX, int regionZ) {
            this.planetName = planetName;
            this.regionX = regionX;
            this.regionZ = regionZ;
        }

        /**
         * Create region key from world coordinates
         */
        public static RegionKey fromWorldCoords(String planetName, int worldX, int worldZ) {
            return new RegionKey(planetName, worldX >> 9, worldZ >> 9); // Divide by 512 (32*16)
        }

        /**
         * Create region key from chunk coordinates
         */
        public static RegionKey fromChunkCoords(String planetName, int chunkX, int chunkZ) {
            return new RegionKey(planetName, chunkX >> 5, chunkZ >> 5); // Divide by 32
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof RegionKey other)) return false;
            return regionX == other.regionX &&
                    regionZ == other.regionZ &&
                    planetName.equals(other.planetName);
        }

        @Override
        public int hashCode() {
            return planetName.hashCode() ^ (regionX << 16) ^ regionZ;
        }

        @Override
        public String toString() {
            return String.format("RegionKey{%s:r.%d.%d}", planetName, regionX, regionZ);
        }
    }

    // Track completed regions per planet
    private final Set<RegionKey> completedRegions = ConcurrentHashMap.newKeySet();

    // Track regions currently being generated
    private final Set<RegionKey> activeRegions = ConcurrentHashMap.newKeySet();

    // Track chunk completion within regions
    private final ConcurrentHashMap<RegionKey, Set<ChunkKey>> regionChunkStatus = new ConcurrentHashMap<>();

    // NEW: Track volatility data per chunk within regions
    private final ConcurrentHashMap<RegionKey, ConcurrentHashMap<ChunkKey, Integer>> regionVolatilityData = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<RegionKey, ConcurrentHashMap<ChunkKey, Double>> regionTemperatureCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<RegionKey, ConcurrentHashMap<ChunkKey, Double>> regionWindSpeedCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<RegionKey, ConcurrentHashMap<ChunkKey, Double>> regionMoistureCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<RegionKey, ConcurrentHashMap<ChunkKey, Double>> regionHabitabilityCache = new ConcurrentHashMap<>();


    /**
     * Chunk identifier within a region
     */
    private static class ChunkKey {
        public final int chunkX;
        public final int chunkZ;

        public ChunkKey(int chunkX, int chunkZ) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof ChunkKey other)) return false;
            return chunkX == other.chunkX && chunkZ == other.chunkZ;
        }

        @Override
        public int hashCode() {
            return (chunkX << 16) ^ chunkZ;
        }
    }

    /**
     * Mark a region as active (being worked on)
     */
    public void markRegionActive(RegionKey regionKey) {
        if (completedRegions.contains(regionKey)) {
            return; // Already completed, no need to track
        }

        activeRegions.add(regionKey);
        regionChunkStatus.computeIfAbsent(regionKey, k -> ConcurrentHashMap.newKeySet());
        regionVolatilityData.computeIfAbsent(regionKey, k -> new ConcurrentHashMap<>());

        Terradyne.LOGGER.debug("Region {} marked as active", regionKey);
    }


    /**
     * NEW: Set cached temperature for a chunk within a region
     */
    public void setCachedTemperature(RegionKey regionKey, int chunkX, int chunkZ, double temperature) {
        ConcurrentHashMap<ChunkKey, Double> temperatureMap = regionTemperatureCache.computeIfAbsent(regionKey, k -> new ConcurrentHashMap<>());
        temperatureMap.put(new ChunkKey(chunkX, chunkZ), temperature);
    }

    /**
     * NEW: Get cached temperature for a chunk within a region
     */
    public Double getCachedTemperature(RegionKey regionKey, int chunkX, int chunkZ) {
        ConcurrentHashMap<ChunkKey, Double> temperatureMap = regionTemperatureCache.get(regionKey);
        return temperatureMap != null ? temperatureMap.get(new ChunkKey(chunkX, chunkZ)) : null;
    }

    /**
     * NEW: Set cached wind speed for a chunk within a region
     */
    public void setCachedWindSpeed(RegionKey regionKey, int chunkX, int chunkZ, double windSpeed) {
        ConcurrentHashMap<ChunkKey, Double> windSpeedMap = regionWindSpeedCache.computeIfAbsent(regionKey, k -> new ConcurrentHashMap<>());
        windSpeedMap.put(new ChunkKey(chunkX, chunkZ), windSpeed);
    }

    /**
     * NEW: Get cached wind speed for a chunk within a region
     */
    public Double getCachedWindSpeed(RegionKey regionKey, int chunkX, int chunkZ) {
        ConcurrentHashMap<ChunkKey, Double> windSpeedMap = regionWindSpeedCache.get(regionKey);
        return windSpeedMap != null ? windSpeedMap.get(new ChunkKey(chunkX, chunkZ)) : null;
    }

    /**
     * NEW: Set cached moisture for a chunk within a region
     */
    public void setCachedMoisture(RegionKey regionKey, int chunkX, int chunkZ, double moisture) {
        ConcurrentHashMap<ChunkKey, Double> moistureMap = regionMoistureCache.computeIfAbsent(regionKey, k -> new ConcurrentHashMap<>());
        moistureMap.put(new ChunkKey(chunkX, chunkZ), moisture);
    }

    /**
     * NEW: Get cached moisture for a chunk within a region
     */
    public Double getCachedMoisture(RegionKey regionKey, int chunkX, int chunkZ) {
        ConcurrentHashMap<ChunkKey, Double> moistureMap = regionMoistureCache.get(regionKey);
        return moistureMap != null ? moistureMap.get(new ChunkKey(chunkX, chunkZ)) : null;
    }

    /**
     * NEW: Set cached habitability for a chunk within a region
     */
    public void setCachedHabitability(RegionKey regionKey, int chunkX, int chunkZ, double habitability) {
        ConcurrentHashMap<ChunkKey, Double> habitabilityMap = regionHabitabilityCache.computeIfAbsent(regionKey, k -> new ConcurrentHashMap<>());
        habitabilityMap.put(new ChunkKey(chunkX, chunkZ), habitability);
    }

    /**
     * NEW: Get cached habitability for a chunk within a region
     */
    public Double getCachedHabitability(RegionKey regionKey, int chunkX, int chunkZ) {
        ConcurrentHashMap<ChunkKey, Double> habitabilityMap = regionHabitabilityCache.get(regionKey);
        return habitabilityMap != null ? habitabilityMap.get(new ChunkKey(chunkX, chunkZ)) : null;
    }

    // UPDATE the markRegionCompleted() method to clean up noise caches:
    public void markRegionCompleted(RegionKey regionKey) {
        completedRegions.add(regionKey);
        activeRegions.remove(regionKey);
        regionChunkStatus.remove(regionKey);
        regionVolatilityData.remove(regionKey);

        // NEW: Clean up noise caches too
        regionTemperatureCache.remove(regionKey);
        regionWindSpeedCache.remove(regionKey);
        regionMoistureCache.remove(regionKey);
        regionHabitabilityCache.remove(regionKey);

        Terradyne.LOGGER.info("Region {} marked as completed and removed from tracking (including noise caches)", regionKey);
    }

    // UPDATE the clearPlanet() method to clean up noise caches:
    public void clearPlanet(String planetName) {
        completedRegions.removeIf(key -> key.planetName.equals(planetName));
        activeRegions.removeIf(key -> key.planetName.equals(planetName));
        regionChunkStatus.entrySet().removeIf(entry -> entry.getKey().planetName.equals(planetName));
        regionVolatilityData.entrySet().removeIf(entry -> entry.getKey().planetName.equals(planetName));

        // NEW: Clean up noise caches too
        regionTemperatureCache.entrySet().removeIf(entry -> entry.getKey().planetName.equals(planetName));
        regionWindSpeedCache.entrySet().removeIf(entry -> entry.getKey().planetName.equals(planetName));
        regionMoistureCache.entrySet().removeIf(entry -> entry.getKey().planetName.equals(planetName));
        regionHabitabilityCache.entrySet().removeIf(entry -> entry.getKey().planetName.equals(planetName));

        Terradyne.LOGGER.info("Cleared all region tracking data and noise caches for planet: {}", planetName);
    }

    // UPDATE the getStatistics() method to include noise cache counts:
    public String getStatistics() {
        return String.format("RegionTracker{completed=%d, active=%d, tracking=%d, volatility=%d, temp=%d, wind=%d, moisture=%d, habitat=%d}",
                completedRegions.size(),
                activeRegions.size(),
                regionChunkStatus.size(),
                regionVolatilityData.size(),
                regionTemperatureCache.size(),
                regionWindSpeedCache.size(),
                regionMoistureCache.size(),
                regionHabitabilityCache.size());
    }

    /**
     * NEW: Get total cached noise entries for debugging
     */
    public int getTotalNoiseCacheEntries() {
        return regionTemperatureCache.values().stream().mapToInt(map -> map.size()).sum() +
                regionWindSpeedCache.values().stream().mapToInt(map -> map.size()).sum() +
                regionMoistureCache.values().stream().mapToInt(map -> map.size()).sum() +
                regionHabitabilityCache.values().stream().mapToInt(map -> map.size()).sum();
    }

    /**
     * Mark a chunk as generated within a region
     */
    public void markChunkGenerated(RegionKey regionKey, int chunkX, int chunkZ) {
        if (completedRegions.contains(regionKey)) {
            return; // Region already completed
        }

        Set<ChunkKey> chunks = regionChunkStatus.get(regionKey);
        if (chunks != null) {
            chunks.add(new ChunkKey(chunkX, chunkZ));

            // Check if region is now complete
            if (chunks.size() >= 1024) { // 32×32 = 1024 chunks per region
                markRegionCompleted(regionKey);
            }
        }
    }

    /**
     * NEW: Set volatility data for a chunk within a region
     */
    public void setChunkVolatility(RegionKey regionKey, int chunkX, int chunkZ, int volatility) {
        ConcurrentHashMap<ChunkKey, Integer> volatilityMap = regionVolatilityData.get(regionKey);
        if (volatilityMap != null) {
            volatilityMap.put(new ChunkKey(chunkX, chunkZ), volatility);
            Terradyne.LOGGER.debug("Set volatility {} for chunk ({}, {}) in region {}",
                    volatility, chunkX, chunkZ, regionKey);
        }
    }

    /**
     * NEW: Get volatility data for a chunk within a region
     */
    public Integer getChunkVolatility(RegionKey regionKey, int chunkX, int chunkZ) {
        ConcurrentHashMap<ChunkKey, Integer> volatilityMap = regionVolatilityData.get(regionKey);
        if (volatilityMap != null) {
            return volatilityMap.get(new ChunkKey(chunkX, chunkZ));
        }
        return null; // Not cached
    }

    /**
     * NEW: Check if volatility data exists for a chunk
     */
    public boolean hasVolatilityData(RegionKey regionKey, int chunkX, int chunkZ) {
        return getChunkVolatility(regionKey, chunkX, chunkZ) != null;
    }

    /**
     * Check if a region is fully generated
     */
    public boolean isRegionCompleted(RegionKey regionKey) {
        return completedRegions.contains(regionKey);
    }

    /**
     * Check if a region is currently being worked on
     */
    public boolean isRegionActive(RegionKey regionKey) {
        return activeRegions.contains(regionKey);
    }

    /**
     * Get completion percentage for a region (0.0 to 1.0)
     */
    public double getRegionCompletionPercentage(RegionKey regionKey) {
        if (completedRegions.contains(regionKey)) {
            return 1.0;
        }

        Set<ChunkKey> chunks = regionChunkStatus.get(regionKey);
        if (chunks == null) {
            return 0.0;
        }

        return Math.min(1.0, chunks.size() / 1024.0);
    }

    /**
     * Mark a region as no longer needed (player left area)
     * If not completed, cache can be unloaded to storage
     */
    public void markRegionInactive(RegionKey regionKey) {
        if (completedRegions.contains(regionKey)) {
            return; // Completed regions are already cleaned up
        }

        activeRegions.remove(regionKey);
        // Keep chunk tracking data and volatility data in case player returns

        Terradyne.LOGGER.debug("Region {} marked as inactive (can be cached to storage)", regionKey);
    }

    /**
     * Get all completed regions for a planet
     */
    public Set<RegionKey> getCompletedRegions(String planetName) {
        return completedRegions.stream()
                .filter(key -> key.planetName.equals(planetName))
                .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * Get all active regions for a planet
     */
    public Set<RegionKey> getActiveRegions(String planetName) {
        return activeRegions.stream()
                .filter(key -> key.planetName.equals(planetName))
                .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * Get detailed statistics for a planet
     */
    public String getPlanetStatistics(String planetName) {
        long completed = completedRegions.stream().filter(k -> k.planetName.equals(planetName)).count();
        long active = activeRegions.stream().filter(k -> k.planetName.equals(planetName)).count();
        long tracking = regionChunkStatus.keySet().stream().filter(k -> k.planetName.equals(planetName)).count();
        long volatilityCached = regionVolatilityData.keySet().stream().filter(k -> k.planetName.equals(planetName)).count();

        return String.format("Planet %s: completed=%d, active=%d, tracking=%d, volatility=%d",
                planetName, completed, active, tracking, volatilityCached);
    }

    /**
     * NEW: Get total cached volatility entries for debugging
     */
    public int getTotalVolatilityEntries() {
        return regionVolatilityData.values().stream()
                .mapToInt(map -> map.size())
                .sum();
    }

    /**
     * Utility class for chunk-level caching operations
     */
    public static class NoiseCache {

        /**
         * Get cached value or compute and cache if not found
         */
        public static double getCachedValue(String planetName, int worldX, int worldZ, RegionCompletionTracker cacheTracker,
                                            java.util.function.Supplier<Double> computeFunction,
                                            java.util.function.BiConsumer<RegionKey, ChunkKey> cacheFunction) {

            // Convert world coordinates to chunk coordinates
            int chunkX = worldX >> 4; // Divide by 16
            int chunkZ = worldZ >> 4;

            // Get region key for this chunk
            RegionKey regionKey = RegionKey.fromChunkCoords(planetName, chunkX, chunkZ);

            // Check cache first
            // Note: This is a generic method, specific cache gets handled by the cacheFunction

            // If not cached, compute value at chunk center
            int chunkCenterX = (chunkX << 4) + 8; // chunkX * 16 + 8
            int chunkCenterZ = (chunkZ << 4) + 8; // chunkZ * 16 + 8

            double computedValue = computeFunction.get();

            // Cache the result
            cacheFunction.accept(regionKey, new ChunkKey(chunkX, chunkZ));

            return computedValue;
        }

        /**
         * Convert world coordinates to chunk coordinates
         */
        public static int[] worldToChunk(int worldX, int worldZ) {
            return new int[]{worldX >> 4, worldZ >> 4};
        }

        /**
         * Convert chunk coordinates to chunk center world coordinates
         */
        public static int[] chunkToWorldCenter(int chunkX, int chunkZ) {
            return new int[]{(chunkX << 4) + 8, (chunkZ << 4) + 8};
        }
    }
}