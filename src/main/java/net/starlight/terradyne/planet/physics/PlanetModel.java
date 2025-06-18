// PlanetModel.java
package net.starlight.terradyne.planet.physics;

import net.starlight.terradyne.planet.terrain.config.MasterTerrainConfig;
import net.starlight.terradyne.planet.terrain.PlanetaryNoiseSystem;
import net.starlight.terradyne.planet.world.PlanetChunkGenerator;

/**
 * Central coordination hub for all planet systems.
 * Manages the lifecycle and integration of all planet components.
 */
public class PlanetModel {

    private final PlanetData planetData;
    private final PlanetFactory planetFactory;
    private MasterTerrainConfig terrainConfig; // Removed final
    private PlanetaryNoiseSystem noiseSystem; // Removed final
    private PlanetChunkGenerator chunkGenerator; // Removed final

    // State tracking
    private boolean isInitialized = false;
    private String dimensionKey;

    private PlanetModel(PlanetData planetData) {
        this.planetData = planetData;
        this.planetFactory = new PlanetFactory(this);

        // These will be initialized during create() process
        this.terrainConfig = null;
        this.noiseSystem = null;
        this.chunkGenerator = null;
    }

    /**
     * Creates a new PlanetModel from a PlanetConfig.
     * This only creates the model - call create() to initialize all systems.
     */
    public static PlanetModel fromConfig(PlanetConfig config) {
        // First pass: create PlanetData with input parameters
        PlanetData initialData = PlanetData.fromConfig(config);

        // Calculate derived parameters using PhysicsCalculator
        PhysicsCalculator calculator = new PhysicsCalculator();
        PlanetData calculatedData = calculator.calculateDerivedParameters(initialData);

        return new PlanetModel(calculatedData);
    }

    /**
     * Initialize all planet systems and register with Minecraft.
     * This is where the heavy lifting happens.
     */
    public void create() {
        if (isInitialized) {
            throw new IllegalStateException("Planet model is already initialized");
        }

        try {
            // Let the factory handle the creation process
            planetFactory.createPlanet();
            this.isInitialized = true;

            System.out.println("Successfully created planet: " + planetData.getPlanetName());
        } catch (Exception e) {
            System.err.println("Failed to create planet: " + planetData.getPlanetName());
            throw new RuntimeException("Planet creation failed", e);
        }
    }

    /**
     * Cleanup planet resources and unregister from Minecraft.
     */
    public void destroy() {
        if (!isInitialized) {
            return;
        }

        // TODO: Implement cleanup logic
        // - Unregister dimension
        // - Clear noise system caches
        // - Remove chunk generator

        this.isInitialized = false;
        System.out.println("Destroyed planet: " + planetData.getPlanetName());
    }

    // Getters for all major components
    public PlanetData getPlanetData() {
        return planetData;
    }

    public PlanetFactory getPlanetFactory() {
        return planetFactory;
    }

    public MasterTerrainConfig getTerrainConfig() {
        return terrainConfig;
    }

    public PlanetaryNoiseSystem getNoiseSystem() {
        return noiseSystem;
    }

    public PlanetChunkGenerator getChunkGenerator() {
        return chunkGenerator;
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public String getDimensionKey() {
        return dimensionKey;
    }

    // Package-private setters for PlanetFactory to use during creation
    void setDimensionKey(String dimensionKey) {
        this.dimensionKey = dimensionKey;
    }

    // These will be set by PlanetFactory during initialization
    void setTerrainConfig(MasterTerrainConfig terrainConfig) {
        if (this.terrainConfig != null) {
            throw new IllegalStateException("Terrain config already set");
        }
        this.terrainConfig = terrainConfig;
    }

    void setNoiseSystem(PlanetaryNoiseSystem noiseSystem) {
        if (this.noiseSystem != null) {
            throw new IllegalStateException("Noise system already set");
        }
        this.noiseSystem = noiseSystem;
    }

    void setChunkGenerator(PlanetChunkGenerator chunkGenerator) {
        if (this.chunkGenerator != null) {
            throw new IllegalStateException("Chunk generator already set");
        }
        this.chunkGenerator = chunkGenerator;
    }

    /**
     * Get summary information about this planet for debugging/logging
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Planet: ").append(planetData.getPlanetName()).append("\n");
        sb.append("Circumference: ").append(planetData.getCircumference()).append(" km\n");
        sb.append("Crust: ").append(planetData.getCrustComposition()).append("\n");
        sb.append("Atmosphere: ").append(planetData.getAtmosphereComposition()).append("\n");
        sb.append("Temperature: ").append(String.format("%.1f", planetData.getAverageSurfaceTemp())).append("°C\n");
        sb.append("Habitability: ").append(String.format("%.2f", planetData.getHabitability())).append("\n");
        sb.append("Initialized: ").append(isInitialized);
        return sb.toString();
    }
}