package net.starlight.terradyne.planet.physics;

import net.starlight.terradyne.Terradyne;
import net.starlight.terradyne.planet.terrain.PlanetaryNoiseSystem;
import net.starlight.terradyne.planet.terrain.TerrainHeightMapper;
import net.starlight.terradyne.planet.mapping.TectonicVolatilityManager;

/**
 * Central coordination hub for planet generation
 * UPDATED: Now includes tectonic volatility management
 * Manages all planet systems and provides unified API
 */
public class PlanetModel {

    private final PlanetConfig config;
    private final PlanetData planetData;
    private final BlockPaletteManager.BlockPalette blockPalette;

    // === PHASE C: NOISE SYSTEM ===
    private final PlanetaryNoiseSystem noiseSystem;
    private final TerrainHeightMapper heightMapper;

    // === PHASE 2: VOLATILITY SYSTEM ===
    private final TectonicVolatilityManager volatilityManager;

    // Future system references (to be added in later phases)
    // private final TectonicPlateManager tectonicManager;
    // private final TemperatureCalculator temperatureCalculator;
    // private final MoistureCalculator moistureCalculator;

    /**
     * Create a planet model from configuration
     * Initializes all planet systems and calculates physics
     */
    public PlanetModel(PlanetConfig config) {
        Terradyne.LOGGER.info("=== CREATING PLANET MODEL ===");
        Terradyne.LOGGER.info("Planet: {}", config.getPlanetName());
        Terradyne.LOGGER.info("Input Config: {}", config);

        this.config = config;

        // === PHASE 0A: PHYSICS CALCULATION ===
        Terradyne.LOGGER.info("Calculating planet physics...");
        this.planetData = PhysicsCalculator.calculatePlanetData(config);

        // === PHASE 0A: BLOCK PALETTE SELECTION ===
        this.blockPalette = BlockPaletteManager.getPalette(config.getCrustComposition());
        if (this.blockPalette == null) {
            throw new RuntimeException("No block palette found for crust composition: " +
                    config.getCrustComposition());
        }

        // === PHASE C: NOISE SYSTEM INITIALIZATION ===
        Terradyne.LOGGER.info("Initializing noise system...");
        this.noiseSystem = new PlanetaryNoiseSystem(config, planetData);
        this.heightMapper = new TerrainHeightMapper(this, noiseSystem);

        // === PHASE 2: VOLATILITY SYSTEM INITIALIZATION ===
        Terradyne.LOGGER.info("Initializing tectonic volatility system...");
        this.volatilityManager = new TectonicVolatilityManager(config, planetData, noiseSystem.getMasterNoise());

        // === FUTURE PHASES ===
        // Phase 1: this.tectonicManager = new TectonicPlateManager(config, planetData);
        // Phase 5: this.temperatureCalculator = new TemperatureCalculator(config, planetData);
        // Phase 5: this.moistureCalculator = new MoistureCalculator(config, planetData);

        logPlanetSummary();
    }

    // === CORE ACCESSORS ===

    /**
     * Get the original user configuration
     */
    public PlanetConfig getConfig() {
        return config;
    }

    /**
     * Get calculated planet physics data
     */
    public PlanetData getPlanetData() {
        return planetData;
    }

    /**
     * Get geological block palette for this planet
     */
    public BlockPaletteManager.BlockPalette getBlockPalette() {
        return blockPalette;
    }

    /**
     * Get planetary noise system for advanced terrain queries
     */
    public PlanetaryNoiseSystem getNoiseSystem() {
        return noiseSystem;
    }

    /**
     * Get terrain height mapper for block placement
     */
    public TerrainHeightMapper getHeightMapper() {
        return heightMapper;
    }

    /**
     * NEW: Get tectonic volatility manager for geological activity
     */
    public TectonicVolatilityManager getVolatilityManager() {
        return volatilityManager;
    }

    // === TERRAIN GENERATION API ===

    /**
     * Generate terrain height at world coordinates
     * Uses full physics-based noise system
     */
    public double getTerrainHeight(int worldX, int worldZ) {
        return noiseSystem.sampleTerrainHeight(worldX, worldZ);
    }

    /**
     * Get appropriate block state for terrain at given coordinates and Y level
     * Uses height mapper with environmental conditions
     */
    public net.minecraft.block.BlockState getTerrainBlockState(int worldX, int worldZ, int minecraftY) {
        return heightMapper.getTerrainBlockState(worldX, worldZ, minecraftY);
    }

    /**
     * Get appropriate block state for terrain using cached climate data (PERFORMANCE OPTIMIZED)
     * Uses height mapper with cached environmental conditions
     */
    public net.minecraft.block.BlockState getTerrainBlockState(int worldX, int worldZ, int minecraftY,
                                                               double temperature, double moisture) {
        return heightMapper.getTerrainBlockState(worldX, worldZ, minecraftY, temperature, moisture);
    }

    /**
     * Get the world seed for unique terrain generation
     * This is challenging in codec context, so we use multiple fallback approaches
     */
    private static long getWorldSeed() {
        try {
            // Method 1: Try to get from current server context if available
            // This is the ideal approach but may not always work in codec context

            // Method 2: Try to get from system properties that Minecraft might set
            String seedProperty = System.getProperty("minecraft.world.seed");
            if (seedProperty != null) {
                return Long.parseLong(seedProperty);
            }

            // Method 3: Try to get from current time as fallback for unique seeds
            // This ensures different worlds get different terrain even if we can't get the actual seed
            long timeSeed = System.currentTimeMillis() / 1000; // Changes every second
            Terradyne.LOGGER.debug("Using time-based seed fallback: {}", timeSeed);
            return timeSeed;

        } catch (Exception e) {
            Terradyne.LOGGER.warn("Could not determine world seed, using time-based fallback");
            return System.currentTimeMillis() / 1000;
        }
    }

    /**
     * Generate a complete terrain column for chunk generation
     * Primary method for integration with UniversalChunkGenerator
     */
    public TerrainHeightMapper.TerrainColumn generateTerrainColumn(int worldX, int worldZ) {
        return heightMapper.generateTerrainColumn(worldX, worldZ);
    }

    /**
     * Get appropriate block for terrain at given conditions (legacy method)
     * Uses block palette based on elevation, erosion, and habitability
     */
    public net.minecraft.block.Block getTerrainBlock(double elevation, double erosion, double habitability) {
        return blockPalette.getBlockForConditions(elevation, erosion, habitability);
    }

    /**
     * Get temperature at world coordinates
     * Uses noise system with latitude and elevation effects
     */
    public double getTemperature(int worldX, int worldZ) {
        return noiseSystem.sampleTemperature(worldX, worldZ);
    }

    /**
     * Get moisture at world coordinates
     * Uses noise system for climate modeling
     */
    public double getMoisture(int worldX, int worldZ) {
        return noiseSystem.sampleMoisture(worldX, worldZ);
    }

    /**
     * Get tectonic activity at world coordinates
     * Uses noise system for geological features
     */
    public double getTectonicActivity(int worldX, int worldZ) {
        return noiseSystem.sampleTectonicActivity(worldX, worldZ);
    }

    // === NEW: VOLATILITY API ===

    /**
     * Get geological volatility level (0-5) at world coordinates
     * 0 = stable continental interior, 5 = active plate boundary
     */
    public int getVolatilityAt(int worldX, int worldZ) {
        return volatilityManager.getVolatilityAt(worldX, worldZ);
    }

    /**
     * Check if location is near plate boundary (volatility 3+)
     * Useful for determining mountain biome placement
     */
    public boolean isPlateNearBoundary(int worldX, int worldZ) {
        return volatilityManager.isPlateNearBoundary(worldX, worldZ);
    }

    /**
     * Get volatility level for a specific chunk
     * Convenience method for chunk-based operations
     */
    public int getChunkVolatility(int chunkX, int chunkZ) {
        return volatilityManager.getChunkVolatility(chunkX, chunkZ);
    }

    // === FUTURE SYSTEM ACCESSORS (to be uncommented in later phases) ===

    /*
    public TectonicPlateManager getTectonicManager() {
        return tectonicManager;
    }

    public TemperatureCalculator getTemperatureCalculator() {
        return temperatureCalculator;
    }

    public MoistureCalculator getMoistureCalculator() {
        return moistureCalculator;
    }
    */

    // === VALIDATION & DIAGNOSTICS ===

    /**
     * Validate that planet model is properly initialized
     */
    public boolean isValid() {
        return config != null &&
                planetData != null &&
                blockPalette != null &&
                noiseSystem != null &&
                heightMapper != null &&
                volatilityManager != null;
    }

    /**
     * Get detailed planet information for debugging
     */
    public String getDetailedInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== PLANET MODEL DETAILS ===\n");
        sb.append("Name: ").append(config.getPlanetName()).append("\n");
        sb.append("Config: ").append(config).append("\n");
        sb.append("Physics: ").append(planetData).append("\n");
        sb.append("Summary: ").append(planetData.getSummary()).append("\n");
        sb.append("Block Palette: ").append(blockPalette).append("\n");
        sb.append("Noise System: ").append(noiseSystem.getSystemStatus()).append("\n");
        sb.append("Volatility: ").append(volatilityManager.getStatistics()).append("\n");
        sb.append("Valid: ").append(isValid()).append("\n");

        // Future systems will add their info here

        return sb.toString();
    }

    /**
     * Analyze terrain at specific coordinates for debugging
     */
    public String analyzeTerrainAt(int worldX, int worldZ) {
        StringBuilder analysis = new StringBuilder();
        analysis.append(heightMapper.analyzeTerrainAt(worldX, worldZ));
        analysis.append("\n");
        analysis.append("Volatility: ").append(getVolatilityAt(worldX, worldZ));
        analysis.append(" (Plate boundary: ").append(isPlateNearBoundary(worldX, worldZ) ? "YES" : "NO").append(")");
        return analysis.toString();
    }

    /**
     * Sample all noise maps at coordinates for debugging
     */
    public String sampleAllNoiseAt(int worldX, int worldZ) {
        return noiseSystem.sampleAllMaps(worldX, worldZ);
    }

    /**
     * Get planet classification for display
     * FIXED: More accurate temperature classifications
     */
    public String getPlanetClassification() {
        StringBuilder classification = new StringBuilder();

        // Age classification
        classification.append(planetData.getPlanetAge().getDisplayName());

        // FIXED: More accurate thermal classification
        double temp = planetData.getAverageSurfaceTemp();
        if (temp < -50) {
            classification.append(" Frozen");
        } else if (temp < -10) {
            classification.append(" Very Cold");
        } else if (temp < 10) {
            classification.append(" Cold");
        } else if (temp < 30) {
            classification.append(" Temperate");
        } else if (temp < 60) {
            classification.append(" Warm");
        } else if (temp < 100) {
            classification.append(" Hot");
        } else if (temp < 200) {
            classification.append(" Very Hot");
        } else {
            classification.append(" Scorching");
        }

        // Composition classification
        classification.append(" ").append(config.getCrustComposition().getDisplayName());

        // FIXED: More accurate habitability classification
        if (planetData.getHabitability() > 0.7) {
            classification.append(" (Highly Habitable)");
        } else if (planetData.getHabitability() > 0.4) {
            classification.append(" (Marginally Habitable)");
        } else if (planetData.getHabitability() > 0.1) {
            classification.append(" (Barely Habitable)");
        } else {
            classification.append(" (Hostile)");
        }

        return classification.toString();
    }

    // === PRIVATE HELPERS ===

    /**
     * Log planet creation summary
     */
    private void logPlanetSummary() {
        Terradyne.LOGGER.info("✅ Planet model created successfully");
        Terradyne.LOGGER.info("Classification: {}", getPlanetClassification());
        Terradyne.LOGGER.info("Physics: {}", planetData);
        Terradyne.LOGGER.info("Block Palette: {}",
                BlockPaletteManager.getPaletteSummary(config.getCrustComposition()));
        Terradyne.LOGGER.info("Noise System: {}", noiseSystem.getSystemStatus());
        Terradyne.LOGGER.info("Volatility: {}", volatilityManager.getStatistics());

        // Physics validation warnings
        if (planetData.getHabitability() < 0.1) {
            Terradyne.LOGGER.warn("⚠️  Planet appears hostile to life");
        }

        if (!planetData.hasAtmosphere()) {
            Terradyne.LOGGER.warn("⚠️  Planet has no significant atmosphere");
        }

        if (!planetData.hasLiquidWater()) {
            Terradyne.LOGGER.warn("⚠️  Planet has no liquid water");
        }

        Terradyne.LOGGER.info("=== END PLANET MODEL CREATION ===");
    }

    @Override
    public String toString() {
        return String.format("PlanetModel{%s, %s}",
                config.getPlanetName(),
                getPlanetClassification());
    }
}