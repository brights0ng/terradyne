// PlanetFactory.java
package net.starlight.terradyne.planet.physics;

import net.starlight.terradyne.planet.terrain.PlanetaryNoiseSystem;
import net.starlight.terradyne.planet.terrain.config.MasterTerrainConfig;
import net.starlight.terradyne.planet.world.PlanetChunkGenerator;
import net.starlight.terradyne.planet.world.biome.PlanetBiomeSource;

/**
 * Orchestrates the creation of planet systems and their registration with Minecraft.
 * Handles the complex process of setting up all planet components.
 */
public class PlanetFactory {

    private final PlanetModel planetModel;

    public PlanetFactory(PlanetModel planetModel) {
        this.planetModel = planetModel;
    }

    /**
     * Create and register all planet systems.
     * This is called by PlanetModel.create() to set up the planet.
     */
    public void createPlanet() {
        PlanetData planetData = planetModel.getPlanetData();

        System.out.println("Creating planet: " + planetData.getPlanetName());
        System.out.println("Planet summary:");
        System.out.println("  Circumference: " + planetData.getCircumference() + " km");
        System.out.println("  Temperature: " + String.format("%.1f°C", planetData.getAverageSurfaceTemp()));
        System.out.println("  Habitability: " + String.format("%.2f", planetData.getHabitability()));
        System.out.println("  Main Rock: " + planetData.getMainRockType());

        // Step 1: Create terrain configuration
        createTerrainConfig();

        // Step 2: Initialize noise system
        createNoiseSystem();

        // Step 3: Create chunk generator
        createChunkGenerator();

        // Step 4: Register dimension (placeholder for now)
        registerDimension();

        System.out.println("Planet creation completed successfully!");
    }

    /**
     * Create terrain configuration based on planet data
     */
    private void createTerrainConfig() {
        System.out.println("  Creating terrain configuration...");

        MasterTerrainConfig config = MasterTerrainConfig.fromPlanetData(planetModel.getPlanetData());
        planetModel.setTerrainConfig(config);
    }

    /**
     * Initialize the planetary noise system
     */
    private void createNoiseSystem() {
        System.out.println("  Initializing noise system...");

        PlanetaryNoiseSystem noiseSystem = new PlanetaryNoiseSystem(planetModel.getTerrainConfig());
        planetModel.setNoiseSystem(noiseSystem);
    }

    /**
     * Create the chunk generator for this planet
     */
    private void createChunkGenerator() {
        System.out.println("  Creating chunk generator...");

        // Create biome source
        PlanetBiomeSource biomeSource = new PlanetBiomeSource(planetModel.getPlanetData());

        // Create chunk generator
        PlanetChunkGenerator generator = new PlanetChunkGenerator(planetModel, biomeSource);
        planetModel.setChunkGenerator(generator);
    }

    /**
     * Register the planet dimension with Minecraft
     */
    private void registerDimension() {
        // TODO: Implement dimension registration with Fabric
        System.out.println("  Registering dimension...");

        // Generate dimension key
        String dimensionKey = "terradyne:" + planetModel.getPlanetData().getPlanetName().toLowerCase().replace(" ", "_");
        planetModel.setDimensionKey(dimensionKey);

        // This will eventually register the dimension with Minecraft using Fabric APIs
        System.out.println("  Dimension key: " + dimensionKey);
    }

    /**
     * Validate that all planet systems were created successfully
     */
    private void validateCreation() {
        // TODO: Add validation checks once systems are implemented
        System.out.println("  Validating planet systems...");
    }
}