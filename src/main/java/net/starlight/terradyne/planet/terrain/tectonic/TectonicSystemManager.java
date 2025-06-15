package net.starlight.terradyne.planet.terrain.tectonic;

import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.random.Random;
import net.starlight.terradyne.planet.terrain.config.UnifiedTerrainConfig;
import net.starlight.terradyne.planet.terrain.math.TerrainMathUtils;
import net.starlight.terradyne.planet.terrain.noise.PlanetaryNoiseSystem;
import net.starlight.terradyne.planet.physics.TectonicPlateGenerator.*;
import net.starlight.terradyne.Terradyne;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enhanced tectonic system manager with unified noise integration
 * Builds on TectonicPlateGenerator with advanced caching and smooth boundary calculations
 * Integrates seamlessly with PlanetaryNoiseSystem for realistic terrain generation
 */
public class TectonicSystemManager {
    
    // Core tectonic data
    private final List<EnhancedTectonicPlate> plates;
    private final long seed;
    private final float tectonicScale;
    private final UnifiedTerrainConfig terrainConfig;
    
    // Integration with unified noise system
    private final PlanetaryNoiseSystem noiseSystem;
    
    // Performance caching
    private final Map<String, CachedPlateInfo> plateCache;
    private final Map<String, CachedBoundaryInfo> boundaryCache;
    private final Map<String, Double> volatilityCache;
    private final boolean enableCaching;
    private final int maxCacheSize;
    
    // Boundary calculation optimization
    private final int worldSize;
    private final double maxBoundaryDistance;
    
    // Statistics
    private long plateLookups = 0;
    private long cacheHits = 0;
    private long cacheMisses = 0;
    
    /**
     * Initialize the enhanced tectonic system
     */
    public TectonicSystemManager(UnifiedTerrainConfig config, PlanetaryNoiseSystem noiseSystem) {
        this.terrainConfig = config;
        this.noiseSystem = noiseSystem;
        this.seed = config.getSeed();
        this.tectonicScale = config.getPlanetConfig().getTectonicScale();
        this.enableCaching = config.isEnableCaching();
        this.maxCacheSize = config.getPlateBoundaryCacheSize();
        this.worldSize = 20000; // World size for plate generation
        this.maxBoundaryDistance = config.getVolatilityFalloffRadius();
        
        // Generate enhanced tectonic plates
        this.plates = generateEnhancedPlates();
        
        // Initialize caches
        this.plateCache = enableCaching ? new ConcurrentHashMap<>() : null;
        this.boundaryCache = enableCaching ? new ConcurrentHashMap<>() : null;
        this.volatilityCache = enableCaching ? new ConcurrentHashMap<>() : null;
        
        Terradyne.LOGGER.info("=== ENHANCED TECTONIC SYSTEM INITIALIZED ===");
        Terradyne.LOGGER.info("Plates Generated: {}", plates.size());
        Terradyne.LOGGER.info("Tectonic Scale: {}", tectonicScale);
        Terradyne.LOGGER.info("Max Boundary Distance: {}", maxBoundaryDistance);
        Terradyne.LOGGER.info("Caching: {}", enableCaching ? "Enabled" : "Disabled");
        Terradyne.LOGGER.info("Noise Integration: Unified system");
    }
    
    /**
     * Generate enhanced tectonic plates with better distribution and properties
     */
    private List<EnhancedTectonicPlate> generateEnhancedPlates() {
        Random random = Random.create(seed);
        
        // Calculate plate count based on tectonic scale and planet size
        double planetSizeModifier = terrainConfig.getPlanetConfig().getPlanetCircumference() / 4.0;
        int baseCount = (int)(tectonicScale * 12 * planetSizeModifier);
        int plateCount = Math.max(1, Math.min(baseCount, 50)); // Reasonable limits
        
        List<EnhancedTectonicPlate> generatedPlates = new ArrayList<>();
        
        // Generate plate centers with improved Poisson disk sampling
        List<Vec2f> plateCenters = generateOptimalPlateCenters(random, plateCount);
        
        // Create enhanced plates with additional properties
        for (int i = 0; i < plateCenters.size(); i++) {
            Vec2f center = plateCenters.get(i);
            
            // Assign plate type (1, 2, or 3)
            int plateType = random.nextInt(3) + 1;
            
            // Enhanced elevation calculation using continental noise
            double noiseX = noiseSystem.worldToNoiseX((int)center.x);
            double noiseZ = noiseSystem.worldToNoiseZ((int)center.y);
            double continentalNoise = noiseSystem.sampleContinentalTerrain(noiseX, noiseZ);
            
            // Base elevation influenced by continental noise and plate type
            float baseElevation = calculatePlateElevation(plateType, continentalNoise, random);
            
            // Additional enhanced properties
            float plateAge = random.nextFloat(); // 0.0 = new, 1.0 = ancient
            float plateThickness = 5.0f + random.nextFloat() * 10.0f; // 5-15 km
            float plateVelocity = random.nextFloat() * tectonicScale; // Movement speed
            
            EnhancedTectonicPlate plate = new EnhancedTectonicPlate(
                i, center.x, center.y, plateType, baseElevation,
                plateAge, plateThickness, plateVelocity
            );
            
            generatedPlates.add(plate);
        }
        
        Terradyne.LOGGER.info("Generated {} enhanced tectonic plates", generatedPlates.size());
        logPlateStatistics(generatedPlates);
        
        return generatedPlates;
    }
    
    /**
     * Generate optimal plate centers using enhanced Poisson disk sampling
     */
    private List<Vec2f> generateOptimalPlateCenters(Random random, int targetCount) {
        List<Vec2f> centers = new ArrayList<>();
        
        // Calculate minimum distance based on world size and target count
        double area = worldSize * worldSize;
        double averageArea = area / targetCount;
        float minDistance = (float)(Math.sqrt(averageArea) * 0.8); // 80% of ideal spacing
        
        // Enhanced multi-pass generation for better distribution
        int maxAttempts = targetCount * 200;
        int attempts = 0;
        
        // Start with a center point
        centers.add(new Vec2f(0, 0));
        
        // Generate additional points with distance constraints
        while (centers.size() < targetCount && attempts < maxAttempts) {
            float x = (random.nextFloat() - 0.5f) * worldSize;
            float z = (random.nextFloat() - 0.5f) * worldSize;
            Vec2f candidate = new Vec2f(x, z);
            
            boolean validPosition = true;
            
            // Check distance to all existing centers
            for (Vec2f existing : centers) {
                float distance = TerrainMathUtils.distance2D(existing.x, existing.y, candidate.x, candidate.y);
                if (distance < minDistance) {
                    validPosition = false;
                    break;
                }
            }
            
            if (validPosition) {
                centers.add(candidate);
            }
            
            attempts++;
        }
        
        Terradyne.LOGGER.debug("Generated {} plate centers in {} attempts", centers.size(), attempts);
        return centers;
    }
    
    /**
     * Calculate enhanced plate elevation using continental noise and plate type
     */
    private float calculatePlateElevation(int plateType, double continentalNoise, Random random) {
        // Base elevation influenced by continental noise
        float continentalInfluence = (float)(continentalNoise * 20.0); // ±20 blocks
        
        // Plate type modifiers
        float plateTypeModifier = switch (plateType) {
            case 1 -> 20.0f + random.nextFloat() * 40.0f;   // Hotspot zones: +20 to +60
            case 2 -> -10.0f + random.nextFloat() * 20.0f;  // Windswept zones: -10 to +10
            case 3 -> 30.0f + random.nextFloat() * 30.0f;   // Crumple zones: +30 to +60
            default -> 0.0f;
        };
        
        // Combine influences
        return continentalInfluence + plateTypeModifier + (random.nextFloat() - 0.5f) * 10.0f;
    }
    
    // === CORE PLATE LOOKUP METHODS ===
    
    /**
     * Get the tectonic plate at world coordinates with caching
     */
    public EnhancedTectonicPlate getPlateAt(int worldX, int worldZ) {
        plateLookups++;
        
        // Check cache first
        if (enableCaching) {
            String cacheKey = generatePlateCacheKey(worldX, worldZ);
            CachedPlateInfo cached = plateCache.get(cacheKey);
            if (cached != null) {
                cacheHits++;
                return cached.plate;
            }
            cacheMisses++;
        }
        
        // Find nearest plate using Voronoi tessellation
        EnhancedTectonicPlate nearestPlate = null;
        float nearestDistanceSq = Float.MAX_VALUE;
        
        for (EnhancedTectonicPlate plate : plates) {
            float dx = worldX - plate.getCenterX();
            float dz = worldZ - plate.getCenterZ();
            float distanceSq = dx * dx + dz * dz;
            
            if (distanceSq < nearestDistanceSq) {
                nearestDistanceSq = distanceSq;
                nearestPlate = plate;
            }
        }
        
        // Cache result
        if (enableCaching && plateCache.size() < maxCacheSize && nearestPlate != null) {
            String cacheKey = generatePlateCacheKey(worldX, worldZ);
            plateCache.put(cacheKey, new CachedPlateInfo(nearestPlate, Math.sqrt(nearestDistanceSq)));
        }
        
        return nearestPlate;
    }
    
    /**
     * Get enhanced boundary information with smooth gradient calculations
     */
    public EnhancedBoundaryInfo getBoundaryInfoAt(int worldX, int worldZ) {
        // Check cache first
        if (enableCaching) {
            String cacheKey = generateBoundaryCacheKey(worldX, worldZ);
            CachedBoundaryInfo cached = boundaryCache.get(cacheKey);
            if (cached != null) {
                return cached.boundaryInfo;
            }
        }
        
        // Find two nearest plates for boundary calculation
        List<PlateDistance> nearestPlates = findNearestPlates(worldX, worldZ, 3);
        
        if (nearestPlates.size() < 2) {
            return new EnhancedBoundaryInfo(Float.MAX_VALUE, BoundaryType.NONE, 0.0f, 0.0f);
        }
        
        EnhancedTectonicPlate nearest = nearestPlates.get(0).plate;
        EnhancedTectonicPlate secondNearest = nearestPlates.get(1).plate;
        float nearestDist = nearestPlates.get(0).distance;
        float secondNearestDist = nearestPlates.get(1).distance;
        
        // Calculate smooth boundary distance
        float boundaryDistance = calculateSmoothBoundaryDistance(nearestDist, secondNearestDist);
        
        // Determine boundary type
        BoundaryType boundaryType = determineBoundaryType(nearest, secondNearest);
        
        // Calculate enhanced volatility with smooth gradients
        float volatility = calculateEnhancedVolatility(worldX, worldZ, boundaryDistance, boundaryType, nearestPlates);
        
        // Calculate boundary strength (how pronounced the boundary effect is)
        float boundaryStrength = calculateBoundaryStrength(boundaryDistance, boundaryType);
        
        EnhancedBoundaryInfo result = new EnhancedBoundaryInfo(
            boundaryDistance, boundaryType, volatility, boundaryStrength
        );
        
        // Cache result
        if (enableCaching && boundaryCache.size() < maxCacheSize) {
            String cacheKey = generateBoundaryCacheKey(worldX, worldZ);
            boundaryCache.put(cacheKey, new CachedBoundaryInfo(result));
        }
        
        return result;
    }
    
    /**
     * Find N nearest plates to a position
     */
    private List<PlateDistance> findNearestPlates(int worldX, int worldZ, int count) {
        List<PlateDistance> plateDistances = new ArrayList<>();
        
        for (EnhancedTectonicPlate plate : plates) {
            float distance = TerrainMathUtils.distance2D(worldX, worldZ, plate.getCenterX(), plate.getCenterZ());
            plateDistances.add(new PlateDistance(plate, distance));
        }
        
        // Sort by distance and return top N
        plateDistances.sort(Comparator.comparingDouble(pd -> pd.distance));
        return plateDistances.subList(0, Math.min(count, plateDistances.size()));
    }
    
    /**
     * Calculate smooth boundary distance to avoid sharp transitions
     */
    private float calculateSmoothBoundaryDistance(float nearestDist, float secondNearestDist) {
        // Smooth boundary calculation - distance to midpoint
        float rawBoundaryDist = Math.abs(secondNearestDist - nearestDist) / 2.0f;
        
        // Apply smoothing to prevent sharp cutoffs
        float smoothingRadius = (float) terrainConfig.getPlateBlendSmoothness();
        return (float)TerrainMathUtils.smoothstep(0, smoothingRadius, rawBoundaryDist) * rawBoundaryDist;
    }
    
    /**
     * Calculate enhanced volatility with multiple influencing factors
     */
    private float calculateEnhancedVolatility(int worldX, int worldZ, float boundaryDistance, 
                                            BoundaryType boundaryType, List<PlateDistance> nearestPlates) {
        // Check volatility cache
        if (enableCaching) {
            String cacheKey = generateVolatilityCacheKey(worldX, worldZ);
            Double cached = volatilityCache.get(cacheKey);
            if (cached != null) {
                return cached.floatValue();
            }
        }
        
        // Base volatility from boundary distance
        float maxDistance = (float)maxBoundaryDistance;
        if (boundaryDistance > maxDistance) {
            return 0.0f; // No volatility far from boundaries
        }
        
        // Calculate base volatility strength from boundary type
        float maxVolatility = getMaxVolatilityForBoundaryType(boundaryType);
        
        // Distance-based falloff using smooth exponential
        float distanceFalloff = (float)TerrainMathUtils.exponentialFalloff(
            boundaryDistance, maxDistance * 0.7, 1.0
        );
        
        // Enhanced volatility calculation considering multiple plates
        float enhancedVolatility = maxVolatility * distanceFalloff;
        
        // Modulate with noise for natural variation
        if (nearestPlates.size() >= 2) {
            double noiseX = noiseSystem.worldToNoiseX(worldX);
            double noiseZ = noiseSystem.worldToNoiseZ(worldZ);
            
            // Use ridge noise for volatility variation
            double volatilityNoise = noiseSystem.sampleRidgePattern(noiseX, noiseZ);
            enhancedVolatility *= (1.0f + volatilityNoise * 0.3f); // ±30% variation
        }
        
        // Plate age influence - older plates have less volatility
        if (nearestPlates.size() >= 1) {
            float plateAge = nearestPlates.get(0).plate.getPlateAge();
            float ageModifier = 1.0f - plateAge * 0.4f; // Up to 40% reduction for ancient plates
            enhancedVolatility *= ageModifier;
        }
        
        // Apply tectonic activity scaling
        enhancedVolatility *= terrainConfig.getPlanetConfig().getTectonicActivity();
        
        // Cache result
        if (enableCaching && volatilityCache.size() < maxCacheSize) {
            String cacheKey = generateVolatilityCacheKey(worldX, worldZ);
            volatilityCache.put(cacheKey, (double)enhancedVolatility);
        }
        
        return enhancedVolatility;
    }
    
    /**
     * Get maximum volatility for boundary type
     */
    private float getMaxVolatilityForBoundaryType(BoundaryType boundaryType) {
        float baseMax = (float)terrainConfig.getMaxVolatilityEffect();
        
        return switch (boundaryType) {
            case DIVERGENT -> baseMax * 1.0f;   // Standard positive volatility
            case TRANSFORM -> -baseMax * 0.6f;  // Negative volatility (valleys)
            case CONVERGENT -> baseMax * 1.5f;  // Strong positive volatility (mountains)
            case NONE -> 0.0f;
        };
    }
    
    /**
     * Calculate boundary strength for blending effects
     */
    private float calculateBoundaryStrength(float boundaryDistance, BoundaryType boundaryType) {
        float maxDistance = (float)maxBoundaryDistance;
        if (boundaryDistance > maxDistance) return 0.0f;
        
        float strength = 1.0f - (boundaryDistance / maxDistance);
        
        // Boundary type affects strength
        return switch (boundaryType) {
            case CONVERGENT -> strength * 1.2f;  // Stronger effect
            case DIVERGENT -> strength * 1.0f;   // Standard effect
            case TRANSFORM -> strength * 0.8f;   // Weaker effect
            case NONE -> 0.0f;
        };
    }
    
    /**
     * Determine boundary type between plates (same logic as original)
     */
    private BoundaryType determineBoundaryType(EnhancedTectonicPlate plate1, EnhancedTectonicPlate plate2) {
        if (plate1 == null || plate2 == null) return BoundaryType.NONE;
        
        int type1 = plate1.getPlateType();
        int type2 = plate2.getPlateType();
        
        if (type1 == type2) {
            return BoundaryType.DIVERGENT; // Same number = divergent
        } else if (Math.abs(type1 - type2) == 1) {
            return BoundaryType.TRANSFORM; // Difference of 1 = transform
        } else {
            return BoundaryType.CONVERGENT; // Difference of 2 = convergent
        }
    }
    
    // === INTEGRATION WITH UNIFIED NOISE SYSTEM ===
    
    /**
     * Get terrain height contribution from tectonic plates
     * Integrates smoothly with PlanetaryNoiseSystem
     */
    public double getTectonicHeightContribution(int worldX, int worldZ) {
        EnhancedTectonicPlate plate = getPlateAt(worldX, worldZ);
        if (plate == null) return 0.0;
        
        // Base plate elevation
        double plateElevation = plate.getBaseElevation();
        
        // Smooth blending with continental noise
        double noiseX = noiseSystem.worldToNoiseX(worldX);
        double noiseZ = noiseSystem.worldToNoiseZ(worldZ);
        double continentalNoise = noiseSystem.sampleContinentalTerrain(noiseX, noiseZ);
        
        // Blend plate elevation with continental noise based on distance from plate center
        float distanceFromCenter = TerrainMathUtils.distance2D(worldX, worldZ, plate.getCenterX(), plate.getCenterZ());
        float plateInfluenceRadius = (float)terrainConfig.getPlateInfluenceRadius();
        
        double blendWeight = TerrainMathUtils.calculateBlendWeight(
            distanceFromCenter, plateInfluenceRadius * 0.3, plateInfluenceRadius
        );
        
        return plateElevation * blendWeight + continentalNoise * (1.0 - blendWeight);
    }
    
    /**
     * Get volatility contribution for terrain generation
     */
    public double getVolatilityContribution(int worldX, int worldZ) {
        EnhancedBoundaryInfo boundaryInfo = getBoundaryInfoAt(worldX, worldZ);
        return boundaryInfo.volatility;
    }
    
    // === CACHING UTILITIES ===
    
    private String generatePlateCacheKey(int x, int z) {
        // Reduce precision for caching efficiency
        int gridX = x / 64; // 64-block grid
        int gridZ = z / 64;
        return "plate:" + gridX + ":" + gridZ;
    }
    
    private String generateBoundaryCacheKey(int x, int z) {
        int gridX = x / 32; // 32-block grid for finer boundary resolution
        int gridZ = z / 32;
        return "boundary:" + gridX + ":" + gridZ;
    }
    
    private String generateVolatilityCacheKey(int x, int z) {
        int gridX = x / 16; // 16-block grid for finest volatility resolution
        int gridZ = z / 16;
        return "volatility:" + gridX + ":" + gridZ;
    }
    
    /**
     * Clear all caches
     */
    public void clearCaches() {
        if (plateCache != null) plateCache.clear();
        if (boundaryCache != null) boundaryCache.clear();
        if (volatilityCache != null) volatilityCache.clear();
        Terradyne.LOGGER.debug("Cleared tectonic system caches");
    }
    
    /**
     * Get performance statistics
     */
    public TectonicStatistics getStatistics() {
        return new TectonicStatistics(
            plateLookups, cacheHits, cacheMisses,
            plateCache != null ? plateCache.size() : 0,
            boundaryCache != null ? boundaryCache.size() : 0,
            volatilityCache != null ? volatilityCache.size() : 0
        );
    }
    
    // === UTILITY METHODS ===
    
    private void logPlateStatistics(List<EnhancedTectonicPlate> plates) {
        Map<Integer, Integer> typeCounts = new HashMap<>();
        double avgElevation = 0.0;
        
        for (EnhancedTectonicPlate plate : plates) {
            typeCounts.merge(plate.getPlateType(), 1, Integer::sum);
            avgElevation += plate.getBaseElevation();
        }
        
        avgElevation /= plates.size();
        
        Terradyne.LOGGER.info("Plate Distribution - Type 1: {}, Type 2: {}, Type 3: {}", 
                typeCounts.getOrDefault(1, 0),
                typeCounts.getOrDefault(2, 0), 
                typeCounts.getOrDefault(3, 0));
        Terradyne.LOGGER.info("Average Plate Elevation: {:.1f}", avgElevation);
    }
    
    // === GETTERS ===
    
    public List<EnhancedTectonicPlate> getPlates() { return Collections.unmodifiableList(plates); }
    public long getSeed() { return seed; }
    public float getTectonicScale() { return tectonicScale; }
    public double getMaxBoundaryDistance() { return maxBoundaryDistance; }
    
    // === DATA CLASSES ===
    
    /**
     * Enhanced tectonic plate with additional properties
     */
    public static class EnhancedTectonicPlate {
        private final int id;
        private final float centerX, centerZ;
        private final int plateType;
        private final float baseElevation;
        private final float plateAge;
        private final float plateThickness;
        private final float plateVelocity;
        
        public EnhancedTectonicPlate(int id, float centerX, float centerZ, int plateType, 
                                   float baseElevation, float plateAge, float plateThickness, float plateVelocity) {
            this.id = id;
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.plateType = plateType;
            this.baseElevation = baseElevation;
            this.plateAge = plateAge;
            this.plateThickness = plateThickness;
            this.plateVelocity = plateVelocity;
        }
        
        // Getters
        public int getId() { return id; }
        public float getCenterX() { return centerX; }
        public float getCenterZ() { return centerZ; }
        public int getPlateType() { return plateType; }
        public float getBaseElevation() { return baseElevation; }
        public float getPlateAge() { return plateAge; }
        public float getPlateThickness() { return plateThickness; }
        public float getPlateVelocity() { return plateVelocity; }
    }
    
    /**
     * Enhanced boundary information
     */
    public static class EnhancedBoundaryInfo {
        public final float distanceToBoundary;
        public final BoundaryType boundaryType;
        public final float volatility;
        public final float boundaryStrength;
        
        public EnhancedBoundaryInfo(float distanceToBoundary, BoundaryType boundaryType, 
                                  float volatility, float boundaryStrength) {
            this.distanceToBoundary = distanceToBoundary;
            this.boundaryType = boundaryType;
            this.volatility = volatility;
            this.boundaryStrength = boundaryStrength;
        }
    }
    
    // Cache data classes
    private static class CachedPlateInfo {
        final EnhancedTectonicPlate plate;
        final double distanceToCenter;
        
        CachedPlateInfo(EnhancedTectonicPlate plate, double distance) {
            this.plate = plate;
            this.distanceToCenter = distance;
        }
    }
    
    private static class CachedBoundaryInfo {
        final EnhancedBoundaryInfo boundaryInfo;
        
        CachedBoundaryInfo(EnhancedBoundaryInfo boundaryInfo) {
            this.boundaryInfo = boundaryInfo;
        }
    }
    
    private static class PlateDistance {
        final EnhancedTectonicPlate plate;
        final float distance;
        
        PlateDistance(EnhancedTectonicPlate plate, float distance) {
            this.plate = plate;
            this.distance = distance;
        }
    }
    
    /**
     * Performance statistics
     */
    public static class TectonicStatistics {
        public final long totalLookups;
        public final long cacheHits;
        public final long cacheMisses;
        public final double hitRate;
        public final int plateCacheSize;
        public final int boundaryCacheSize;
        public final int volatilityCacheSize;
        
        public TectonicStatistics(long totalLookups, long cacheHits, long cacheMisses,
                                int plateCacheSize, int boundaryCacheSize, int volatilityCacheSize) {
            this.totalLookups = totalLookups;
            this.cacheHits = cacheHits;
            this.cacheMisses = cacheMisses;
            this.hitRate = totalLookups > 0 ? (double)cacheHits / totalLookups : 0.0;
            this.plateCacheSize = plateCacheSize;
            this.boundaryCacheSize = boundaryCacheSize;
            this.volatilityCacheSize = volatilityCacheSize;
        }
        
        @Override
        public String toString() {
            return String.format("TectonicStats{lookups=%d, hits=%d, misses=%d, hitRate=%.2f%%, caches=[%d,%d,%d]}", 
                    totalLookups, cacheHits, cacheMisses, hitRate * 100, 
                    plateCacheSize, boundaryCacheSize, volatilityCacheSize);
        }
    }
    
    /**
     * Cleanup for mod unloading
     */
    public void cleanup() {
        clearCaches();
        Terradyne.LOGGER.info("Tectonic system manager cleaned up");
    }
}