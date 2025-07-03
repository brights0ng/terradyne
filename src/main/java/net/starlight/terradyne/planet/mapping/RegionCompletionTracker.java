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
     * Mark a region as fully generated
     */
    public void markRegionCompleted(RegionKey regionKey) {
        completedRegions.add(regionKey);
        activeRegions.remove(regionKey);
        regionChunkStatus.remove(regionKey);
        regionVolatilityData.remove(regionKey); // Clean up volatility data too

        Terradyne.LOGGER.info("Region {} marked as completed and removed from tracking", regionKey);
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
     * Clear all tracking data for a planet (when planet is unloaded)
     */
    public void clearPlanet(String planetName) {
        completedRegions.removeIf(key -> key.planetName.equals(planetName));
        activeRegions.removeIf(key -> key.planetName.equals(planetName));
        regionChunkStatus.entrySet().removeIf(entry -> entry.getKey().planetName.equals(planetName));
        regionVolatilityData.entrySet().removeIf(entry -> entry.getKey().planetName.equals(planetName));

        Terradyne.LOGGER.info("Cleared all region tracking data for planet: {}", planetName);
    }

    /**
     * Get statistics for debugging
     */
    public String getStatistics() {
        return String.format("RegionTracker{completed=%d, active=%d, tracking=%d, volatility=%d}",
                completedRegions.size(),
                activeRegions.size(),
                regionChunkStatus.size(),
                regionVolatilityData.size());
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
}