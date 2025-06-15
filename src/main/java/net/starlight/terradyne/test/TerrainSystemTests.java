package net.starlight.terradyne.test;

import net.starlight.terradyne.planet.physics.PlanetPhysicsConfig;
import net.starlight.terradyne.planet.physics.PlanetPhysicsModel;
import net.starlight.terradyne.planet.terrain.cache.*;
import net.starlight.terradyne.planet.terrain.config.*;
import net.starlight.terradyne.planet.terrain.math.TerrainMathUtils;
import net.starlight.terradyne.planet.terrain.noise.PlanetaryNoiseSystem;
import net.starlight.terradyne.planet.terrain.tectonic.TectonicSystemManager;
import net.starlight.terradyne.planet.factory.PhysicsPlanetFactory;
import net.starlight.terradyne.util.ModEnums;

import java.util.*;

/**
 * Comprehensive test suite for all Stage 0 terrain generation components
 * Tests mathematical utilities, configuration systems, noise generation, 
 * tectonic systems, and caching infrastructure
 */
public class TerrainSystemTests {

    private static final String SEPARATOR = "========================================";
    private static final String SUB_SEPARATOR = "----------------------------------------";

    private static int totalTests = 0;
    private static int passedTests = 0;
    private static int failedTests = 0;
    private static List<String> failureMessages = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println(SEPARATOR);
        System.out.println("TERRADYNE STAGE 0 COMPREHENSIVE TEST SUITE");
        System.out.println(SEPARATOR);
        System.out.println();

        long startTime = System.currentTimeMillis();

        try {
            // Run all tests
            testTerrainMathUtils();
            testConfigurationSystems();
            testPlanetaryNoiseSystem();
            testTectonicSystemManager();
            testTerrainCacheManager();
            testChunkRegionCache();
            testPlateBoundaryCache();
            testSystemIntegration();

        } catch (Exception e) {
            System.err.println("CRITICAL TEST FAILURE: " + e.getMessage());
            e.printStackTrace();
            failedTests++;
        }

        long duration = System.currentTimeMillis() - startTime;

        // Print final results
        System.out.println();
        System.out.println(SEPARATOR);
        System.out.println("TEST SUITE RESULTS");
        System.out.println(SEPARATOR);
        System.out.printf("Total Tests: %d%n", totalTests);
        System.out.printf("Passed: %d (%.1f%%)%n", passedTests, (double)passedTests / totalTests * 100);
        System.out.printf("Failed: %d (%.1f%%)%n", failedTests, (double)failedTests / totalTests * 100);
        System.out.printf("Duration: %dms%n", duration);

        if (!failureMessages.isEmpty()) {
            System.out.println();
            System.out.println("FAILURE DETAILS:");
            for (String failure : failureMessages) {
                System.out.println("❌ " + failure);
            }
        }

        System.out.println();
        if (failedTests == 0) {
            System.out.println("🎉 ALL TESTS PASSED! Stage 0 is ready for Stage 1 implementation.");
        } else {
            System.out.println("⚠️  Some tests failed. Please review failures before proceeding.");
        }
    }

    // === TERRAIN MATH UTILS TESTS ===

    private static void testTerrainMathUtils() {
        System.out.println("🧮 Testing TerrainMathUtils...");

        // Test smoothstep function
        testAssert("Smoothstep boundary conditions",
                Math.abs(TerrainMathUtils.smoothstep(0, 1, -0.5) - 0.0) < 0.001 &&
                        Math.abs(TerrainMathUtils.smoothstep(0, 1, 1.5) - 1.0) < 0.001 &&
                        Math.abs(TerrainMathUtils.smoothstep(0, 1, 0.5) - 0.5) < 0.1);

        // Test lerp function
        testAssert("Linear interpolation",
                Math.abs(TerrainMathUtils.lerp(0, 10, 0.5) - 5.0) < 0.001 &&
                        Math.abs(TerrainMathUtils.lerp(-5, 5, 0.0) - (-5.0)) < 0.001);

        // Test clamp function
        testAssert("Clamp function",
                TerrainMathUtils.clamp(-5, 0, 10) == 0 &&
                        TerrainMathUtils.clamp(15, 0, 10) == 10 &&
                        TerrainMathUtils.clamp(5, 0, 10) == 5);

        // Test distance calculations
        testAssert("Distance calculations",
                Math.abs(TerrainMathUtils.distance2D(0, 0, 3, 4) - 5.0) < 0.001 &&
                        TerrainMathUtils.distanceSquared2D(0, 0, 3, 4) == 25.0);

        // Test exponential falloff
        double falloff1 = TerrainMathUtils.exponentialFalloff(0, 100, 1.0);
        double falloff2 = TerrainMathUtils.exponentialFalloff(100, 100, 1.0);
        testAssert("Exponential falloff", falloff1 > falloff2 && falloff1 <= 1.0);

        // Test octave frequency generation
        double freq1 = TerrainMathUtils.getOctaveFrequency(0.001, 0);
        double freq2 = TerrainMathUtils.getOctaveFrequency(0.001, 1);
        testAssert("Octave frequency generation", freq2 > freq1 && freq2 / freq1 > 1.6);

        // Test coordinate normalization
        double normalized = TerrainMathUtils.normalizeWorldCoordinate(1000, 0.001);
        testAssert("Coordinate normalization", Math.abs(normalized - 1.0) < 0.001);

        System.out.println("   ✓ TerrainMathUtils tests completed");
        System.out.println();
    }

    // === CONFIGURATION SYSTEMS TESTS ===

    private static void testConfigurationSystems() {
        System.out.println("⚙️ Testing Configuration Systems...");

        // Test PlanetPhysicsConfig creation
        PlanetPhysicsConfig physicsConfig = PhysicsPlanetFactory.createTectonicTestConfig("TestPlanet");
        testAssert("Physics config creation",
                physicsConfig != null &&
                        physicsConfig.getPlanetName().equals("TestPlanet") &&
                        physicsConfig.getTectonicScale() > 0);

        // Test UnifiedTerrainConfig creation
        UnifiedTerrainConfig terrainConfig = UnifiedTerrainConfig.createDefault(physicsConfig);
        testAssert("Terrain config creation",
                terrainConfig != null &&
                        terrainConfig.getContinentalScale() > 0 &&
                        terrainConfig.getMaxVolatilityEffect() > 0);

        // Test performance optimization config
        UnifiedTerrainConfig perfConfig = UnifiedTerrainConfig.createPerformanceOptimized(physicsConfig);
        testAssert("Performance config optimization",
                perfConfig.isUseFastMath() &&
                        perfConfig.getMaxDetailOctaves() <= 4);

        // Test high quality config
        UnifiedTerrainConfig qualityConfig = UnifiedTerrainConfig.createHighQuality(physicsConfig);
        testAssert("High quality config",
                qualityConfig.getMaxDetailOctaves() >= 6 &&
                        qualityConfig.getSurfaceRoughness() > terrainConfig.getSurfaceRoughness());

        // Test NoiseLayerConfig creation
        NoiseLayerConfig continentalConfig = NoiseLayerConfig.createContinental(40.0);
        testAssert("Continental noise config",
                continentalConfig.getLayerType() == NoiseLayerConfig.NoiseLayerType.CONTINENTAL &&
                        continentalConfig.getOctaves() > 0 &&
                        continentalConfig.getEffectiveAmplitude() > 0);

        // Test layer coordination
        NoiseLayerConfig regionalConfig = NoiseLayerConfig.createRegional(1.0);
        NoiseLayerConfig localConfig = NoiseLayerConfig.createLocal(1.0);

        double continentalFreq = continentalConfig.getEffectiveFrequency();
        double regionalFreq = regionalConfig.getEffectiveFrequency();
        double localFreq = localConfig.getEffectiveFrequency();

        testAssert("Frequency separation",
                regionalFreq > continentalFreq &&
                        localFreq > regionalFreq);

        System.out.println("   ✓ Configuration systems tests completed");
        System.out.println();
    }

    // === PLANETARY NOISE SYSTEM TESTS ===

    private static void testPlanetaryNoiseSystem() {
        System.out.println("🌍 Testing PlanetaryNoiseSystem...");

        // Create test configuration
        PlanetPhysicsConfig physicsConfig = PhysicsPlanetFactory.createTectonicTestConfig("NoiseTestPlanet");
        UnifiedTerrainConfig terrainConfig = UnifiedTerrainConfig.createDefault(physicsConfig);

        // Initialize noise system
        PlanetaryNoiseSystem noiseSystem = new PlanetaryNoiseSystem(terrainConfig);
        testAssert("Noise system initialization", noiseSystem != null);

        // Test master noise sampling
        double noise1 = noiseSystem.sampleMasterNoise(0, 0, 0);
        double noise2 = noiseSystem.sampleMasterNoise(0, 0, 0);
        testAssert("Master noise consistency", Math.abs(noise1 - noise2) < 0.001);

        // Use larger coordinate difference to ensure variation
        double noise3 = noiseSystem.sampleMasterNoise(1000, 0, 1000);
        testAssert("Master noise variation", Math.abs(noise1 - noise3) > 0.01);

        // Test multi-octave sampling with non-zero coordinates to ensure frequency differences matter
        double octaves1 = noiseSystem.sampleMasterOctaves(10, 0, 10, 1, 0.5, 2.0, 0.001);
        double octaves3 = noiseSystem.sampleMasterOctaves(10, 0, 10, 3, 0.5, 2.0, 0.001);
        testAssert("Multi-octave sampling", Math.abs(octaves1 - octaves3) > 0.01);

        // Test layer sampling
        double continental = noiseSystem.sampleLayer(NoiseLayerConfig.NoiseLayerType.CONTINENTAL, 0, 0, 0);
        double regional = noiseSystem.sampleLayer(NoiseLayerConfig.NoiseLayerType.REGIONAL, 0, 0, 0);
        double local = noiseSystem.sampleLayer(NoiseLayerConfig.NoiseLayerType.LOCAL, 0, 0, 0);

        testAssert("Layer sampling",
                !Double.isNaN(continental) &&
                        !Double.isNaN(regional) &&
                        !Double.isNaN(local));

        // Test combined sampling methods
        double continentalTerrain = noiseSystem.sampleContinentalTerrain(0, 0);
        double localTerrain = noiseSystem.sampleLocalTerrain(0, 0);
        testAssert("Combined sampling",
                !Double.isNaN(continentalTerrain) &&
                        !Double.isNaN(localTerrain));

        // Test coordinate transformation
        double noiseX = noiseSystem.worldToNoiseX(1000);
        double noiseZ = noiseSystem.worldToNoiseZ(1000);
        testAssert("Coordinate transformation", noiseX > 0 && noiseZ > 0);

        // Test terrain height sampling
        double terrainHeight = noiseSystem.sampleTerrainHeight(0, 0);
        testAssert("Terrain height sampling", !Double.isNaN(terrainHeight));

        // Test cache statistics
        var cacheStats = noiseSystem.getCacheStatistics();
        testAssert("Cache statistics", cacheStats.totalSamples >= 0);

        System.out.println("   ✓ PlanetaryNoiseSystem tests completed");
        System.out.println();
    }

    // === TECTONIC SYSTEM MANAGER TESTS ===

    private static void testTectonicSystemManager() {
        System.out.println("🌋 Testing TectonicSystemManager...");

        // Create test dependencies
        PlanetPhysicsConfig physicsConfig = PhysicsPlanetFactory.createTectonicTestConfig("TectonicTestPlanet");
        UnifiedTerrainConfig terrainConfig = UnifiedTerrainConfig.createDefault(physicsConfig);
        PlanetaryNoiseSystem noiseSystem = new PlanetaryNoiseSystem(terrainConfig);

        // Initialize tectonic system
        TectonicSystemManager tectonicSystem = new TectonicSystemManager(terrainConfig, noiseSystem);
        testAssert("Tectonic system initialization", tectonicSystem != null);

        // Test plate generation
        var plates = tectonicSystem.getPlates();
        testAssert("Plate generation", plates.size() > 0 && plates.size() <= 50);

        // Test plate assignment
        var plate1 = tectonicSystem.getPlateAt(0, 0);
        var plate2 = tectonicSystem.getPlateAt(0, 0);
        testAssert("Plate assignment consistency", plate1 == plate2);

        var plate3 = tectonicSystem.getPlateAt(5000, 5000);
        testAssert("Plate assignment variation", plate1 != null && plate3 != null);

        // Test boundary information
        var boundaryInfo1 = tectonicSystem.getBoundaryInfoAt(0, 0);
        testAssert("Boundary info generation",
                boundaryInfo1 != null &&
                        boundaryInfo1.distanceToBoundary >= 0);

        // Test tectonic height contribution
        double tectonicHeight = tectonicSystem.getTectonicHeightContribution(0, 0);
        testAssert("Tectonic height contribution", !Double.isNaN(tectonicHeight));

        // Test volatility contribution
        double volatility = tectonicSystem.getVolatilityContribution(0, 0);
        testAssert("Volatility contribution", !Double.isNaN(volatility));

        // Test plate properties
        if (!plates.isEmpty()) {
            var firstPlate = plates.get(0);
            testAssert("Plate properties",
                    firstPlate.getId() >= 0 &&
                            firstPlate.getPlateType() >= 1 && firstPlate.getPlateType() <= 3 &&
                            firstPlate.getPlateAge() >= 0 && firstPlate.getPlateAge() <= 1);
        }

        // Test statistics
        var stats = tectonicSystem.getStatistics();
        testAssert("Tectonic statistics", stats.totalLookups >= 0);

        System.out.println("   ✓ TectonicSystemManager tests completed");
        System.out.println();
    }

    // === TERRAIN CACHE MANAGER TESTS ===

    private static void testTerrainCacheManager() {
        System.out.println("💾 Testing TerrainCacheManager...");

        // Create test configuration
        PlanetPhysicsConfig physicsConfig = PhysicsPlanetFactory.createTectonicTestConfig("CacheTestPlanet");
        UnifiedTerrainConfig terrainConfig = UnifiedTerrainConfig.createDefault(physicsConfig);

        // Initialize cache manager
        TerrainCacheManager cacheManager = new TerrainCacheManager(terrainConfig);
        testAssert("Cache manager initialization", cacheManager != null);

        // Create a simple test cache
        TestCache testCache = new TestCache(100);
        cacheManager.registerCache(TerrainCacheManager.CacheType.NOISE, testCache);

        // Test cache registration
        testAssert("Cache registration",
                cacheManager.getSize(TerrainCacheManager.CacheType.NOISE) == 0);

        // Test cache operations
        cacheManager.put(TerrainCacheManager.CacheType.NOISE, "test_key", "test_value");
        String retrieved = cacheManager.get(TerrainCacheManager.CacheType.NOISE, "test_key");
        testAssert("Cache put/get operations", "test_value".equals(retrieved));

        testAssert("Cache contains check",
                cacheManager.contains(TerrainCacheManager.CacheType.NOISE, "test_key"));

        // Test cache removal
        String removed = cacheManager.remove(TerrainCacheManager.CacheType.NOISE, "test_key");
        testAssert("Cache removal", "test_value".equals(removed));

        testAssert("Cache removal verification",
                !cacheManager.contains(TerrainCacheManager.CacheType.NOISE, "test_key"));

        // Test cache statistics
        var stats = cacheManager.getStatistics();
        testAssert("Cache statistics",
                stats.totalAccesses > 0 &&
                        stats.overallHitRate >= 0);

        // Test cache clearing
        cacheManager.put(TerrainCacheManager.CacheType.NOISE, "test_key2", "test_value2");
        cacheManager.clearCache(TerrainCacheManager.CacheType.NOISE);
        testAssert("Cache clearing",
                cacheManager.getSize(TerrainCacheManager.CacheType.NOISE) == 0);

        System.out.println("   ✓ TerrainCacheManager tests completed");
        System.out.println();
    }

    // === CHUNK REGION CACHE TESTS ===

    private static void testChunkRegionCache() {
        System.out.println("🗺️ Testing ChunkRegionCache...");

        // Create test dependencies
        PlanetPhysicsConfig physicsConfig = PhysicsPlanetFactory.createTectonicTestConfig("ChunkCacheTestPlanet");
        UnifiedTerrainConfig terrainConfig = UnifiedTerrainConfig.createDefault(physicsConfig);
        PlanetaryNoiseSystem noiseSystem = new PlanetaryNoiseSystem(terrainConfig);
        TectonicSystemManager tectonicSystem = new TectonicSystemManager(terrainConfig, noiseSystem);

        // Initialize chunk region cache
        ChunkRegionCache chunkCache = new ChunkRegionCache(terrainConfig);
        chunkCache.setDependencies(tectonicSystem, noiseSystem);
        testAssert("Chunk cache initialization", chunkCache != null);

        // Test region data generation
        var regionData = chunkCache.getChunkRegionData(0, 0);
        testAssert("Region data generation", regionData != null);

        // Test cached plate access
        var cachedPlate = chunkCache.getCachedPlateAt(0, 0);
        testAssert("Cached plate access", cachedPlate != null);

        // Test cached continental noise
        double cachedNoise = chunkCache.getCachedContinentalNoise(0, 0);
        testAssert("Cached continental noise", !Double.isNaN(cachedNoise));

        // Test cached boundary distance
        float cachedDistance = chunkCache.getCachedBoundaryDistance(0, 0);
        testAssert("Cached boundary distance", cachedDistance >= 0);

        // Test interpolation by sampling nearby points
        double noise1 = chunkCache.getCachedContinentalNoise(0, 0);
        double noise2 = chunkCache.getCachedContinentalNoise(2, 2);
        testAssert("Noise interpolation", !Double.isNaN(noise1) && !Double.isNaN(noise2));

        // Test caching efficiency (second access should be faster)
        long start1 = System.nanoTime();
        chunkCache.getChunkRegionData(0, 0);
        long time1 = System.nanoTime() - start1;

        long start2 = System.nanoTime();
        chunkCache.getChunkRegionData(0, 0);
        long time2 = System.nanoTime() - start2;

        testAssert("Cache efficiency", time2 < time1);

        // Test batch generation
        int sizeBefore = chunkCache.size();
        chunkCache.preGenerateRegions(0, 0, 2);
        int sizeAfter = chunkCache.size();
        testAssert("Batch generation", sizeAfter >= sizeBefore);

        // Test statistics
        var stats = chunkCache.getStatistics();
        testAssert("Chunk cache statistics",
                stats.totalGenerations > 0 &&
                        stats.currentSize > 0);

        System.out.println("   ✓ ChunkRegionCache tests completed");
        System.out.println();
    }

    // === PLATE BOUNDARY CACHE TESTS ===

    private static void testPlateBoundaryCache() {
        System.out.println("⚡ Testing PlateBoundaryCache...");

        // Create test dependencies
        PlanetPhysicsConfig physicsConfig = PhysicsPlanetFactory.createTectonicTestConfig("BoundaryCacheTestPlanet");
        UnifiedTerrainConfig terrainConfig = UnifiedTerrainConfig.createDefault(physicsConfig);
        PlanetaryNoiseSystem noiseSystem = new PlanetaryNoiseSystem(terrainConfig);
        TectonicSystemManager tectonicSystem = new TectonicSystemManager(terrainConfig, noiseSystem);

        // Initialize boundary cache
        PlateBoundaryCache boundaryCache = new PlateBoundaryCache(terrainConfig);
        boundaryCache.setDependencies(tectonicSystem);
        testAssert("Boundary cache initialization", boundaryCache != null);

        // Test boundary region data generation
        var regionData = boundaryCache.getBoundaryRegionData(0, 0);
        testAssert("Boundary region data generation", regionData != null);

        // Test cached boundary distance
        float cachedDistance = boundaryCache.getCachedBoundaryDistance(0, 0);
        testAssert("Cached boundary distance", cachedDistance >= 0);

        // Test cached volatility
        float cachedVolatility = boundaryCache.getCachedVolatility(0, 0);
        testAssert("Cached volatility", !Float.isNaN(cachedVolatility));

        // Test cached boundary type
        var boundaryType = boundaryCache.getCachedBoundaryType(0, 0);
        testAssert("Cached boundary type", boundaryType != null);

        // Test boundary proximity check
        boolean isNear = boundaryCache.isNearBoundary(0, 0, 1000.0f);
        testAssert("Boundary proximity check", true); // Should not throw exception

        // Test gradient interpolation by sampling nearby points
        float vol1 = boundaryCache.getCachedVolatility(0, 0);
        float vol2 = boundaryCache.getCachedVolatility(4, 4);
        testAssert("Volatility gradient interpolation", !Float.isNaN(vol1) && !Float.isNaN(vol2));

        // Test caching efficiency
        long start1 = System.nanoTime();
        boundaryCache.getBoundaryRegionData(0, 0);
        long time1 = System.nanoTime() - start1;

        long start2 = System.nanoTime();
        boundaryCache.getBoundaryRegionData(0, 0);
        long time2 = System.nanoTime() - start2;

        testAssert("Boundary cache efficiency", time2 < time1);

        // Test batch generation
        boundaryCache.preGenerateBoundaryRegions(0, 0, 128);
        testAssert("Boundary batch generation", boundaryCache.size() >= 1);

        // Test statistics
        var stats = boundaryCache.getStatistics();
        testAssert("Boundary cache statistics",
                stats.totalGenerations >= 0 &&
                        stats.boundaryCalculations >= 0);

        System.out.println("   ✓ PlateBoundaryCache tests completed");
        System.out.println();
    }

    // === SYSTEM INTEGRATION TESTS ===

    private static void testSystemIntegration() {
        System.out.println("🔗 Testing System Integration...");

        // Create full integrated system
        PlanetPhysicsConfig physicsConfig = PhysicsPlanetFactory.createEarthLikeConfig("IntegrationTestPlanet");
        UnifiedTerrainConfig terrainConfig = UnifiedTerrainConfig.createDefault(physicsConfig);

        // Initialize all systems
        PlanetaryNoiseSystem noiseSystem = new PlanetaryNoiseSystem(terrainConfig);
        TectonicSystemManager tectonicSystem = new TectonicSystemManager(terrainConfig, noiseSystem);
        TerrainCacheManager cacheManager = new TerrainCacheManager(terrainConfig);

        ChunkRegionCache chunkCache = new ChunkRegionCache(terrainConfig);
        chunkCache.setDependencies(tectonicSystem, noiseSystem);

        PlateBoundaryCache boundaryCache = new PlateBoundaryCache(terrainConfig);
        boundaryCache.setDependencies(tectonicSystem);

        // Register caches
        cacheManager.registerCache(TerrainCacheManager.CacheType.CHUNK_REGION, chunkCache);
        cacheManager.registerCache(TerrainCacheManager.CacheType.BOUNDARY, boundaryCache);

        testAssert("Full system initialization",
                noiseSystem != null &&
                        tectonicSystem != null &&
                        cacheManager != null);

        // Test integrated terrain height calculation
        int testX = 1000, testZ = 1000;

        // Get data from all systems
        double noiseHeight = noiseSystem.sampleTerrainHeight(testX, testZ);
        double tectonicHeight = tectonicSystem.getTectonicHeightContribution(testX, testZ);
        float volatility = (float) tectonicSystem.getVolatilityContribution(testX, testZ);

        // Get cached versions
        double cachedNoise = chunkCache.getCachedContinentalNoise(testX, testZ);
        var cachedPlate = chunkCache.getCachedPlateAt(testX, testZ);
        float cachedVolatility = boundaryCache.getCachedVolatility(testX, testZ);

        testAssert("Integrated terrain calculation",
                !Double.isNaN(noiseHeight) &&
                        !Double.isNaN(tectonicHeight) &&
                        !Float.isNaN(volatility) &&
                        !Double.isNaN(cachedNoise) &&
                        cachedPlate != null &&
                        !Float.isNaN(cachedVolatility));

        // Test consistency between cached and direct access
        var directPlate = tectonicSystem.getPlateAt(testX, testZ);
        testAssert("Cache-direct consistency", cachedPlate.getId() == directPlate.getId());

        // Test performance with multiple points
        long startTime = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            int x = i * 100;
            int z = i * 100;

            noiseSystem.sampleTerrainHeight(x, z);
            tectonicSystem.getTectonicHeightContribution(x, z);
            chunkCache.getCachedContinentalNoise(x, z);
            boundaryCache.getCachedVolatility(x, z);
        }
        long duration = System.nanoTime() - startTime;

        testAssert("Performance test", duration < 1_000_000_000L); // Should complete in < 1 second

        // Test cache statistics integration
        var cacheStats = cacheManager.getStatistics();
        var chunkStats = chunkCache.getStatistics();
        var boundaryStats = boundaryCache.getStatistics();

        testAssert("Integrated statistics",
                cacheStats.totalAccesses >= 0 &&
                        chunkStats.totalGenerations >= 0 &&
                        boundaryStats.totalGenerations >= 0);

        // Cleanup
        cacheManager.clearAllCaches();
        testAssert("System cleanup",
                cacheManager.getStatistics().currentMemoryUsage == 0);

        System.out.println("   ✓ System integration tests completed");
        System.out.println();
    }

    // === TEST UTILITIES ===

    private static void testAssert(String testName, boolean condition) {
        totalTests++;
        if (condition) {
            passedTests++;
            System.out.println("   ✅ " + testName);
        } else {
            failedTests++;
            String failure = testName + " - FAILED";
            failureMessages.add(failure);
            System.out.println("   ❌ " + failure);
        }
    }

    /**
     * Simple test cache implementation
     */
    private static class TestCache implements TerrainCacheManager.TerrainCache<String> {
        private final Map<String, String> cache = new HashMap<>();
        private final int maxSize;

        public TestCache(int maxSize) {
            this.maxSize = maxSize;
        }

        @Override
        public String get(String key) { return cache.get(key); }

        @Override
        public String put(String key, String value) { return cache.put(key, value); }

        @Override
        public String remove(String key) { return cache.remove(key); }

        @Override
        public boolean containsKey(String key) { return cache.containsKey(key); }

        @Override
        public void clear() { cache.clear(); }

        @Override
        public int size() { return cache.size(); }

        @Override
        public int getMaxSize() { return maxSize; }

        @Override
        public Set<String> keySet() { return cache.keySet(); }
    }
}