package net.starlight.terradyne.planet.terrain.config;

import net.starlight.terradyne.planet.physics.PlanetPhysicsConfig;

/**
 * Master configuration for the unified terrain generation system
 * Integrates with existing PlanetPhysicsConfig and provides stage-specific parameters
 * Controls all aspects of the physics-based terrain generation pipeline
 */
public class UnifiedTerrainConfig {
    
    // Reference to the planet's physics configuration
    private final PlanetPhysicsConfig planetConfig;
    
    // === PERFORMANCE SETTINGS ===
    private final boolean enableCaching;
    private final int chunkRegionCacheSize;
    private final int plateBoundaryCacheSize;
    private final boolean useFastMath;
    private final int maxCacheMemoryMB;
    
    // === CONTINENTAL STAGE SETTINGS ===
    private final double continentalScale;
    private final double oceanDepth;
    private final double continentalHeight;
    private final double continentalRoughness;
    private final boolean enableContinentalShelves;
    
    // === TECTONIC STAGE SETTINGS ===
    private final double plateInfluenceRadius;
    private final double plateBlendSmoothness;
    private final double volatilityFalloffRadius;
    private final double maxVolatilityEffect;
    private final boolean enableVolatilityGradients;
    
    // === ZONE STAGE SETTINGS ===
    private final double crumpleZoneIntensity;
    private final double hotspotZoneIntensity;
    private final double windsweptZoneErosion;
    private final double zoneTransitionRadius;
    
    // === DETAIL STAGE SETTINGS ===
    private final double localTerrainScale;
    private final double surfaceRoughness;
    private final double microTerrainIntensity;
    private final int maxDetailOctaves;
    
    // === WATER STAGE SETTINGS ===
    private final boolean enableImprovedWaterLevel;
    private final double waterLevelVariance;
    private final double coastalShelfWidth;
    private final double underwaterTerrainScale;
    
    // === EROSION STAGE SETTINGS ===
    private final double windErosionStrength;
    private final double glacialErosionStrength;
    private final double erosionSmoothingRadius;
    private final boolean enableProgressiveErosion;
    
    // === GENERATION PIPELINE SETTINGS ===
    private final boolean enableStageDebugOutput;
    private final boolean enablePerformanceProfiling;
    private final int generationThreads;
    private final boolean enableProgressiveDetail;
    
    private UnifiedTerrainConfig(Builder builder) {
        this.planetConfig = builder.planetConfig;
        
        // Performance settings
        this.enableCaching = builder.enableCaching;
        this.chunkRegionCacheSize = builder.chunkRegionCacheSize;
        this.plateBoundaryCacheSize = builder.plateBoundaryCacheSize;
        this.useFastMath = builder.useFastMath;
        this.maxCacheMemoryMB = builder.maxCacheMemoryMB;
        
        // Continental settings
        this.continentalScale = builder.continentalScale;
        this.oceanDepth = builder.oceanDepth;
        this.continentalHeight = builder.continentalHeight;
        this.continentalRoughness = builder.continentalRoughness;
        this.enableContinentalShelves = builder.enableContinentalShelves;
        
        // Tectonic settings
        this.plateInfluenceRadius = builder.plateInfluenceRadius;
        this.plateBlendSmoothness = builder.plateBlendSmoothness;
        this.volatilityFalloffRadius = builder.volatilityFalloffRadius;
        this.maxVolatilityEffect = builder.maxVolatilityEffect;
        this.enableVolatilityGradients = builder.enableVolatilityGradients;
        
        // Zone settings
        this.crumpleZoneIntensity = builder.crumpleZoneIntensity;
        this.hotspotZoneIntensity = builder.hotspotZoneIntensity;
        this.windsweptZoneErosion = builder.windsweptZoneErosion;
        this.zoneTransitionRadius = builder.zoneTransitionRadius;
        
        // Detail settings
        this.localTerrainScale = builder.localTerrainScale;
        this.surfaceRoughness = builder.surfaceRoughness;
        this.microTerrainIntensity = builder.microTerrainIntensity;
        this.maxDetailOctaves = builder.maxDetailOctaves;
        
        // Water settings
        this.enableImprovedWaterLevel = builder.enableImprovedWaterLevel;
        this.waterLevelVariance = builder.waterLevelVariance;
        this.coastalShelfWidth = builder.coastalShelfWidth;
        this.underwaterTerrainScale = builder.underwaterTerrainScale;
        
        // Erosion settings
        this.windErosionStrength = builder.windErosionStrength;
        this.glacialErosionStrength = builder.glacialErosionStrength;
        this.erosionSmoothingRadius = builder.erosionSmoothingRadius;
        this.enableProgressiveErosion = builder.enableProgressiveErosion;
        
        // Pipeline settings
        this.enableStageDebugOutput = builder.enableStageDebugOutput;
        this.enablePerformanceProfiling = builder.enablePerformanceProfiling;
        this.generationThreads = builder.generationThreads;
        this.enableProgressiveDetail = builder.enableProgressiveDetail;
    }
    
    // === FACTORY METHODS ===
    
    /**
     * Create default configuration optimized for the given planet physics
     */
    public static UnifiedTerrainConfig createDefault(PlanetPhysicsConfig planetConfig) {
        return new Builder(planetConfig)
                .withOptimalSettingsForPlanet()
                .build();
    }
    
    /**
     * Create performance-optimized configuration
     */
    public static UnifiedTerrainConfig createPerformanceOptimized(PlanetPhysicsConfig planetConfig) {
        return new Builder(planetConfig)
                .withPerformanceOptimizations()
                .build();
    }
    
    /**
     * Create high-quality configuration (slower but more detailed)
     */
    public static UnifiedTerrainConfig createHighQuality(PlanetPhysicsConfig planetConfig) {
        return new Builder(planetConfig)
                .withHighQualitySettings()
                .build();
    }
    
    /**
     * Create debug configuration with visualization enabled
     */
    public static UnifiedTerrainConfig createDebug(PlanetPhysicsConfig planetConfig) {
        return new Builder(planetConfig)
                .withDebugSettings()
                .build();
    }
    
    // === GETTERS ===
    
    // Core configuration
    public PlanetPhysicsConfig getPlanetConfig() { return planetConfig; }
    public long getSeed() { return planetConfig.getSeed(); }
    public String getPlanetName() { return planetConfig.getPlanetName(); }
    
    // Performance settings
    public boolean isEnableCaching() { return enableCaching; }
    public int getChunkRegionCacheSize() { return chunkRegionCacheSize; }
    public int getPlateBoundaryCacheSize() { return plateBoundaryCacheSize; }
    public boolean isUseFastMath() { return useFastMath; }
    public int getMaxCacheMemoryMB() { return maxCacheMemoryMB; }
    
    // Continental stage
    public double getContinentalScale() { return continentalScale; }
    public double getOceanDepth() { return oceanDepth; }
    public double getContinentalHeight() { return continentalHeight; }
    public double getContinentalRoughness() { return continentalRoughness; }
    public boolean isEnableContinentalShelves() { return enableContinentalShelves; }
    
    // Tectonic stage
    public double getPlateInfluenceRadius() { return plateInfluenceRadius; }
    public double getPlateBlendSmoothness() { return plateBlendSmoothness; }
    public double getVolatilityFalloffRadius() { return volatilityFalloffRadius; }
    public double getMaxVolatilityEffect() { return maxVolatilityEffect; }
    public boolean isEnableVolatilityGradients() { return enableVolatilityGradients; }
    
    // Zone stage
    public double getCrumpleZoneIntensity() { return crumpleZoneIntensity; }
    public double getHotspotZoneIntensity() { return hotspotZoneIntensity; }
    public double getWindsweptZoneErosion() { return windsweptZoneErosion; }
    public double getZoneTransitionRadius() { return zoneTransitionRadius; }
    
    // Detail stage
    public double getLocalTerrainScale() { return localTerrainScale; }
    public double getSurfaceRoughness() { return surfaceRoughness; }
    public double getMicroTerrainIntensity() { return microTerrainIntensity; }
    public int getMaxDetailOctaves() { return maxDetailOctaves; }
    
    // Water stage
    public boolean isEnableImprovedWaterLevel() { return enableImprovedWaterLevel; }
    public double getWaterLevelVariance() { return waterLevelVariance; }
    public double getCoastalShelfWidth() { return coastalShelfWidth; }
    public double getUnderwaterTerrainScale() { return underwaterTerrainScale; }
    
    // Erosion stage
    public double getWindErosionStrength() { return windErosionStrength; }
    public double getGlacialErosionStrength() { return glacialErosionStrength; }
    public double getErosionSmoothingRadius() { return erosionSmoothingRadius; }
    public boolean isEnableProgressiveErosion() { return enableProgressiveErosion; }
    
    // Pipeline settings
    public boolean isEnableStageDebugOutput() { return enableStageDebugOutput; }
    public boolean isEnablePerformanceProfiling() { return enablePerformanceProfiling; }
    public int getGenerationThreads() { return generationThreads; }
    public boolean isEnableProgressiveDetail() { return enableProgressiveDetail; }
    
    // === CALCULATED PROPERTIES ===
    
    /**
     * Calculate effective continental scale based on planet size
     */
    public double getEffectiveContinentalScale() {
        double planetSizeModifier = planetConfig.getPlanetCircumference() / 4.0; // Earth = 1.0
        return continentalScale * planetSizeModifier;
    }
    
    /**
     * Calculate effective volatility based on tectonic activity
     */
    public double getEffectiveVolatility() {
        return maxVolatilityEffect * planetConfig.getTectonicActivity();
    }
    
    /**
     * Calculate erosion strength based on atmosphere
     */
    public double getEffectiveErosionStrength() {
        double atmosphereModifier = Math.min(1.0, planetConfig.getAtmosphereDensity());
        return windErosionStrength * atmosphereModifier;
    }
    
    /**
     * Get water level calculation method based on configuration
     */
    public double calculateWaterLevel(double minElevation, double maxElevation, double averageElevation) {
        if (enableImprovedWaterLevel) {
            // Use average plate elevation method to avoid mountain flooding
            double waterPercentage = planetConfig.getWaterHeight();
            double elevationRange = maxElevation - minElevation;
            return averageElevation + (waterPercentage - 0.5) * elevationRange * 0.8;
        } else {
            // Legacy method using min/max range
            return minElevation + (maxElevation - minElevation) * planetConfig.getWaterHeight();
        }
    }
    
    // === BUILDER PATTERN ===
    
    public static class Builder {
        private final PlanetPhysicsConfig planetConfig;
        
        // Default values (will be modified by preset methods)
        private boolean enableCaching = true;
        private int chunkRegionCacheSize = 1000;
        private int plateBoundaryCacheSize = 5000;
        private boolean useFastMath = false;
        private int maxCacheMemoryMB = 256;
        
        private double continentalScale = 40.0;
        private double oceanDepth = -40.0;
        private double continentalHeight = 30.0;
        private double continentalRoughness = 1.0;
        private boolean enableContinentalShelves = true;
        
        private double plateInfluenceRadius = 2000.0;
        private double plateBlendSmoothness = 500.0;
        private double volatilityFalloffRadius = 1000.0;
        private double maxVolatilityEffect = 60.0;
        private boolean enableVolatilityGradients = true;
        
        private double crumpleZoneIntensity = 1.5;
        private double hotspotZoneIntensity = 1.2;
        private double windsweptZoneErosion = 0.8;
        private double zoneTransitionRadius = 800.0;
        
        private double localTerrainScale = 15.0;
        private double surfaceRoughness = 5.0;
        private double microTerrainIntensity = 2.0;
        private int maxDetailOctaves = 4;
        
        private boolean enableImprovedWaterLevel = true;
        private double waterLevelVariance = 0.1;
        private double coastalShelfWidth = 200.0;
        private double underwaterTerrainScale = 0.5;
        
        private double windErosionStrength = 1.0;
        private double glacialErosionStrength = 0.8;
        private double erosionSmoothingRadius = 100.0;
        private boolean enableProgressiveErosion = true;
        
        private boolean enableStageDebugOutput = false;
        private boolean enablePerformanceProfiling = false;
        private int generationThreads = 1;
        private boolean enableProgressiveDetail = true;
        
        public Builder(PlanetPhysicsConfig planetConfig) {
            this.planetConfig = planetConfig;
        }
        
        /**
         * Configure optimal settings based on planet characteristics
         */
        public Builder withOptimalSettingsForPlanet() {
            // Scale settings based on planet size
            double planetSize = planetConfig.getPlanetCircumference() / 4.0;
            this.continentalScale *= planetSize;
            this.plateInfluenceRadius *= planetSize;
            this.volatilityFalloffRadius *= planetSize;
            
            // Adjust effects based on tectonic activity
            double tectonicActivity = planetConfig.getTectonicActivity();
            this.maxVolatilityEffect *= (0.5 + tectonicActivity);
            this.crumpleZoneIntensity *= (0.8 + tectonicActivity * 0.4);
            
            // Adjust erosion based on atmosphere
            double atmosphereDensity = Math.min(1.0, planetConfig.getAtmosphereDensity());
            this.windErosionStrength *= atmosphereDensity;
            
            return this;
        }
        
        /**
         * Configure for maximum performance
         */
        public Builder withPerformanceOptimizations() {
            this.useFastMath = true;
            this.maxDetailOctaves = 3;
            this.enableProgressiveDetail = true;
            this.chunkRegionCacheSize = 2000;
            this.maxCacheMemoryMB = 512;
            return this;
        }
        
        /**
         * Configure for maximum quality
         */
        public Builder withHighQualitySettings() {
            this.maxDetailOctaves = 6;
            this.surfaceRoughness = 8.0;
            this.microTerrainIntensity = 3.0;
            this.enableVolatilityGradients = true;
            this.enableProgressiveErosion = true;
            this.continentalRoughness = 1.5;
            return this;
        }
        
        /**
         * Configure for debugging and visualization
         */
        public Builder withDebugSettings() {
            this.enableStageDebugOutput = true;
            this.enablePerformanceProfiling = true;
            this.useFastMath = false;
            return this;
        }
        
        // Individual setting methods (for custom configurations)
        public Builder withContinentalScale(double scale) { this.continentalScale = scale; return this; }
        public Builder withMaxVolatilityEffect(double effect) { this.maxVolatilityEffect = effect; return this; }
        public Builder withCacheSize(int size) { this.chunkRegionCacheSize = size; return this; }
        public Builder withFastMath(boolean enabled) { this.useFastMath = enabled; return this; }
        public Builder withDebug(boolean enabled) { this.enableStageDebugOutput = enabled; return this; }
        
        public UnifiedTerrainConfig build() {
            return new UnifiedTerrainConfig(this);
        }
    }
}