package net.starlight.terradyne.planet.terrain;

import net.minecraft.util.math.noise.SimplexNoiseSampler;
import net.minecraft.util.math.random.Random;
import net.starlight.terradyne.planet.physics.PlanetConfig;
import net.starlight.terradyne.planet.physics.PlanetData;
import net.starlight.terradyne.Terradyne;

/**
 * Unified noise generation system with 5 specialized maps
 * Each map has layers that blend using different techniques
 * Maps influence each other through cross-map effects
 */
public class PlanetaryNoiseSystem {
    
    private final PlanetConfig config;
    private final PlanetData planetData;
    
    // === MASTER NOISE SAMPLER ===
    private final SimplexNoiseSampler masterNoise;
    
    // === SPECIALIZED NOISE MAPS ===
    private final TerrainNoiseMap terrainMap;
    private final TectonicNoiseMap tectonicMap;
    private final TemperatureNoiseMap temperatureMap;    // Phase 5
    private final MoistureNoiseMap moistureMap;          // Phase 5  
    private final BiomeNoiseMap biomeMap;                // Phase 7
    
    /**
     * Create planetary noise system from planet model data
     */
    public PlanetaryNoiseSystem(PlanetConfig config, PlanetData planetData) {
        this.config = config;
        this.planetData = planetData;
        
        Terradyne.LOGGER.info("Initializing Planetary Noise System for {}", config.getPlanetName());
        
        // Create master noise sampler - all terrain uses this base
        this.masterNoise = new SimplexNoiseSampler(Random.create(config.getSeed()));
        
        // === PHASE C: CORE TERRAIN MAPS ===
        this.tectonicMap = new TectonicNoiseMap(config, planetData, masterNoise);
        this.terrainMap = new TerrainNoiseMap(config, planetData, masterNoise, tectonicMap);
        
        // === FUTURE PHASES: CLIMATE & BIOME MAPS ===
        this.temperatureMap = null; // Phase 5
        this.moistureMap = null;    // Phase 5
        this.biomeMap = null;       // Phase 7
        
        Terradyne.LOGGER.info("✅ Planetary Noise System initialized");
        logNoiseConfiguration();
    }
    
    // === PRIMARY TERRAIN API ===
    
    /**
     * Sample terrain height at world coordinates
     * This is the main method called by chunk generators
     */
    public double sampleTerrainHeight(int worldX, int worldZ) {
        return terrainMap.sample(worldX, worldZ);
    }
    
    /**
     * Sample tectonic activity at world coordinates
     * Used for geological features and terrain modification
     */
    public double sampleTectonicActivity(int worldX, int worldZ) {
        return tectonicMap.sample(worldX, worldZ);
    }
    
    // === FUTURE CLIMATE API (Phase 5) ===
    
    /**
     * Sample temperature at world coordinates
     * PLACEHOLDER: Will be implemented in Phase 5
     */
    public double sampleTemperature(int worldX, int worldZ) {
        if (temperatureMap != null) {
            return temperatureMap.sample(worldX, worldZ);
        }
        
        // PLACEHOLDER: Simple latitude-based temperature
        double latitude = Math.abs(worldZ) / (config.getCircumference() * 0.25); // 0-1 from equator to pole
        double baseTemp = planetData.getAverageSurfaceTemp();
        double latitudeEffect = -latitude * 40.0; // -40°C from equator to pole
        
        return baseTemp + latitudeEffect;
    }
    
    /**
     * Sample moisture at world coordinates  
     * PLACEHOLDER: Will be implemented in Phase 5
     */
    public double sampleMoisture(int worldX, int worldZ) {
        if (moistureMap != null) {
            return moistureMap.sample(worldX, worldZ);
        }
        
        // PLACEHOLDER: Return planet water content
        return planetData.getActualWaterContent();
    }
    
    /**
     * Sample biome classification at world coordinates
     * PLACEHOLDER: Will be implemented in Phase 7
     */
    public double sampleBiome(int worldX, int worldZ) {
        if (biomeMap != null) {
            return biomeMap.sample(worldX, worldZ);
        }
        
        // PLACEHOLDER: Simple classification based on temp/moisture
        double temp = sampleTemperature(worldX, worldZ);
        double moisture = sampleMoisture(worldX, worldZ);
        
        // Return simple biome index (will be properly classified later)
        if (temp < 0) return 0.0; // Cold
        if (moisture < 0.3) return 1.0; // Desert
        if (temp > 25) return 2.0; // Tropical
        return 3.0; // Temperate
    }
    
    // === NOISE MAP ACCESS ===
    
    public TerrainNoiseMap getTerrainMap() { return terrainMap; }
    public TectonicNoiseMap getTectonicMap() { return tectonicMap; }
    public SimplexNoiseSampler getMasterNoise() { return masterNoise; }
    
    // === DIAGNOSTICS ===
    
    /**
     * Get noise system status for debugging
     */
    public String getSystemStatus() {
        return String.format("PlanetaryNoiseSystem{planet=%s, terrain=%s, tectonic=%s}", 
                           config.getPlanetName(),
                           terrainMap != null ? "ACTIVE" : "NULL",
                           tectonicMap != null ? "ACTIVE" : "NULL");
    }
    
    /**
     * Sample all maps at coordinates for debugging
     */
    public String sampleAllMaps(int worldX, int worldZ) {
        return String.format("NoiseDebug{x=%d,z=%d: terrain=%.2f, tectonic=%.2f, temp=%.1f°C, moisture=%.2f}", 
                           worldX, worldZ,
                           sampleTerrainHeight(worldX, worldZ),
                           sampleTectonicActivity(worldX, worldZ), 
                           sampleTemperature(worldX, worldZ),
                           sampleMoisture(worldX, worldZ));
    }
    
    private void logNoiseConfiguration() {
        Terradyne.LOGGER.info("Noise Maps Configured:");
        Terradyne.LOGGER.info("  Terrain: {} layers", terrainMap.getLayerCount());
        Terradyne.LOGGER.info("  Tectonic: {} layers", tectonicMap.getLayerCount());
        Terradyne.LOGGER.info("  Temperature: PLACEHOLDER (Phase 5)");
        Terradyne.LOGGER.info("  Moisture: PLACEHOLDER (Phase 5)");
        Terradyne.LOGGER.info("  Biome: PLACEHOLDER (Phase 7)");
    }
}

/**
 * Base class for specialized noise maps
 * Each map handles its own layers and blending
 */
abstract class NoiseMap {
    protected final PlanetConfig config;
    protected final PlanetData planetData;
    protected final SimplexNoiseSampler masterNoise;
    
    public NoiseMap(PlanetConfig config, PlanetData planetData, SimplexNoiseSampler masterNoise) {
        this.config = config;
        this.planetData = planetData;
        this.masterNoise = masterNoise;
    }
    
    /**
     * Sample this noise map at world coordinates
     */
    public abstract double sample(int worldX, int worldZ);
    
    /**
     * Get number of layers in this map
     */
    public abstract int getLayerCount();
}

/**
 * TERRAIN NOISE MAP - Primary height generation
 * Layers: Continental, Mountain, Valley, Erosion, Surface Detail
 * Influenced By: Tectonic (through domain warping)
 * Blending: Domain warping + overlay
 */
class TerrainNoiseMap extends NoiseMap {
    
    private final TectonicNoiseMap tectonicInfluence;
    
    public TerrainNoiseMap(PlanetConfig config, PlanetData planetData, SimplexNoiseSampler masterNoise, 
                          TectonicNoiseMap tectonicMap) {
        super(config, planetData, masterNoise);
        this.tectonicInfluence = tectonicMap;
    }
    
    @Override
    public double sample(int worldX, int worldZ) {
        // === TECTONIC DOMAIN WARPING ===
        double tectonicOffset = tectonicInfluence.sample(worldX, worldZ) * planetData.getMountainScale() * 25.0;
        double warpedX = worldX + tectonicOffset * 0.3;
        double warpedZ = worldZ + tectonicOffset * 0.7;
        
        // === CONTINENTAL LAYER (largest scale) ===
        double continentalFreq = config.getNoiseScale() * 0.3;
        double continental = masterNoise.sample(warpedX * continentalFreq, 0, warpedZ * continentalFreq);
        continental *= planetData.getContinentalScale() * 30.0;
        
        // === MOUNTAIN LAYER ===
        double mountainFreq = config.getNoiseScale() * 2.0;
        double mountains = masterNoise.sample(warpedX * mountainFreq, 1, warpedZ * mountainFreq);
        mountains = Math.abs(mountains); // Ridge noise for mountain ranges
        mountains *= planetData.getMountainScale() * 40.0;
        
        // === VALLEY LAYER (erosion) ===
        double valleyFreq = config.getNoiseScale() * 1.5;
        double valleys = masterNoise.sample(warpedX * valleyFreq, 2, warpedZ * valleyFreq);
        valleys = Math.abs(valleys) * -1.0; // Negative for carving
        valleys *= planetData.getErosionScale() * 15.0;
        
        // === SURFACE DETAIL LAYER ===
        double detailFreq = config.getNoiseScale() * 8.0;
        double detail = masterNoise.sample(worldX * detailFreq, 3, worldZ * detailFreq);
        detail *= 3.0;
        
        // === DOMAIN WARPING + OVERLAY BLENDING ===
        double baseHeight = planetData.getSeaLevel();
        double terrainHeight = continental;
        
        // Overlay mountains (where tectonic activity is high)
        double tectonicStrength = Math.max(0, tectonicInfluence.sample(worldX, worldZ));
        terrainHeight += mountains * tectonicStrength;
        
        // Apply valleys with soft blending
        terrainHeight = blendOverlay(terrainHeight, valleys);
        
        // Add surface detail
        terrainHeight += detail;
        
        return baseHeight + terrainHeight;
    }
    
    @Override
    public int getLayerCount() { return 4; }
    
    /**
     * Overlay blending for smooth terrain transitions
     */
    private double blendOverlay(double base, double overlay) {
        if (overlay >= 0) {
            return base + overlay * 0.7; // Additive for positive
        } else {
            return base * (1.0 + overlay * 0.1); // Multiplicative for negative
        }
    }
}

/**
 * TECTONIC NOISE MAP - Geological activity & plate boundaries
 * Layers: Plate boundaries, Volcanism, Seismic zones  
 * Influenced By: Mostly independent (drives other maps)
 * Blending: Hard boundaries (min/max) for plate edges
 */
class TectonicNoiseMap extends NoiseMap {
    
    public TectonicNoiseMap(PlanetConfig config, PlanetData planetData, SimplexNoiseSampler masterNoise) {
        super(config, planetData, masterNoise);
    }
    
    @Override
    public double sample(int worldX, int worldZ) {
        // === PLATE BOUNDARY LAYER ===
        double plateFreq = config.getNoiseScale();
        double plates = masterNoise.sample(worldX * plateFreq, 10, worldZ * plateFreq);
        
        // Create sharp plate boundaries using ridge noise
        double boundaries = Math.abs(plates);
        boundaries = 1.0 - boundaries; // Invert so boundaries are high
        boundaries = Math.max(0, boundaries - 0.7) / 0.3; // Sharp cutoff
        
        // === VOLCANISM LAYER ===
        double volcanismFreq = config.getNoiseScale() * 3.0;
        double volcanism = masterNoise.sample(worldX * volcanismFreq, 11, worldZ * volcanismFreq);
        volcanism = Math.max(0, volcanism) * planetData.getVolcanismLevel();
        
        // === SEISMIC ZONES LAYER ===
        double seismicFreq = config.getNoiseScale() * 0.8;
        double seismic = masterNoise.sample(worldX * seismicFreq, 12, worldZ * seismicFreq);
        seismic = Math.abs(seismic) * planetData.getActualTectonicActivity();
        
        // === HARD BOUNDARY BLENDING ===
        double tectonicActivity = Math.max(boundaries, Math.max(volcanism, seismic));
        
        // Scale by planet's overall tectonic activity
        return tectonicActivity * planetData.getActualTectonicActivity();
    }
    
    @Override
    public int getLayerCount() { return 3; }
}

// Add these classes to the bottom of PlanetaryNoiseSystem.java file

/**
 * PLACEHOLDER - Temperature Noise Map (Phase 5)
 * TODO: Implement proper temperature distribution with latitude, elevation, and atmospheric effects
 */
class TemperatureNoiseMap extends NoiseMap {

    public TemperatureNoiseMap(PlanetConfig config, PlanetData planetData, SimplexNoiseSampler masterNoise) {
        super(config, planetData, masterNoise);
    }

    @Override
    public double sample(int worldX, int worldZ) {
        // PLACEHOLDER: Simple latitude-based temperature
        double latitude = Math.abs(worldZ) / (config.getCircumference() * 0.25); // 0-1 from equator to pole
        double baseTemp = planetData.getAverageSurfaceTemp();
        double latitudeEffect = -latitude * 40.0; // -40°C from equator to pole

        return baseTemp + latitudeEffect;
    }

    @Override
    public int getLayerCount() {
        return 1; // Will have 3-4 layers in Phase 5: Base + Latitude + Elevation + Greenhouse
    }
}

/**
 * PLACEHOLDER - Moisture Noise Map (Phase 5)
 * TODO: Implement proper moisture distribution with evaporation, wind patterns, and orographic effects
 */
class MoistureNoiseMap extends NoiseMap {

    public MoistureNoiseMap(PlanetConfig config, PlanetData planetData, SimplexNoiseSampler masterNoise) {
        super(config, planetData, masterNoise);
    }

    @Override
    public double sample(int worldX, int worldZ) {
        // PLACEHOLDER: Return planet water content with slight noise variation
        double baseNoise = masterNoise.sample(worldX * 0.001, 4, worldZ * 0.001);
        double variation = baseNoise * 0.2; // ±20% variation
        return Math.max(0.0, Math.min(1.0, planetData.getActualWaterContent() + variation));
    }

    @Override
    public int getLayerCount() {
        return 1; // Will have 4-5 layers in Phase 5: Base + Evaporation + Wind + Orographic + Distance from water
    }
}

/**
 * PLACEHOLDER - Biome Noise Map (Phase 7)
 * TODO: Implement proper biome classification based on temperature, moisture, elevation, and geology
 */
class BiomeNoiseMap extends NoiseMap {

    public BiomeNoiseMap(PlanetConfig config, PlanetData planetData, SimplexNoiseSampler masterNoise) {
        super(config, planetData, masterNoise);
    }

    @Override
    public double sample(int worldX, int worldZ) {
        // PLACEHOLDER: Simple biome classification based on temperature/moisture
        // This will be replaced with proper Whittaker biome classification in Phase 7

        // Get temperature and moisture (will use proper maps in Phase 7)
        double temp = planetData.getAverageSurfaceTemp();
        double moisture = planetData.getActualWaterContent();

        // Simple biome index classification
        if (temp < 0) return 0.0;           // Cold/Tundra
        if (moisture < 0.3) return 1.0;     // Desert
        if (temp > 25) return 2.0;          // Tropical
        return 3.0;                         // Temperate
    }

    @Override
    public int getLayerCount() {
        return 1; // Will have 2-3 layers in Phase 7: Classification + Transition smoothing + Local variations
    }
}