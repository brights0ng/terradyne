package net.starlight.terradyne;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.event.registry.DynamicRegistrySetupCallback;
import net.minecraft.registry.RegistryBuilder;
import net.minecraft.registry.RegistryKeys;
import net.starlight.terradyne.datagen.*;
import net.starlight.terradyne.planet.features.ModConfiguredFeatures;
import net.starlight.terradyne.planet.features.ModPlacedFeatures;
import net.starlight.terradyne.starsystem.CelestialObjectRegistry;
import net.starlight.terradyne.starsystem.DatapackLoader;
import net.starlight.terradyne.starsystem.StarSystemRegistry;

import java.nio.file.Path;

/**
 * Main data generator entry point for Terradyne mod
 * FIXED: Complete implementation with proper registry bootstrapping
 */
public class TerradyneDataGenerator implements DataGeneratorEntrypoint {

    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();

        Terradyne.LOGGER.info("=== INITIALIZING TERRADYNE DATA GENERATION ===");

        // NEW: Load celestial objects and star systems from file system
        try {
            // Get the project root and navigate to src/main/resources
            Path buildPath = fabricDataGenerator.getModContainer().getRootPaths().get(0);
            Terradyne.LOGGER.info("Build path: {}", buildPath);

            // Navigate from build/resources/main back to project root
            Path projectRoot = buildPath.getParent().getParent().getParent();
            Path resourcesPath = projectRoot.resolve("src").resolve("main").resolve("resources");

            Terradyne.LOGGER.info("Project root: {}", projectRoot);
            Terradyne.LOGGER.info("Resources path (should be src/main/resources): {}", resourcesPath);

            if (!java.nio.file.Files.exists(resourcesPath)) {
                throw new RuntimeException("Resources path does not exist: " + resourcesPath);
            }

            DatapackLoader.loadFromFileSystem(resourcesPath);
        } catch (Exception e) {
            Terradyne.LOGGER.error("CRITICAL: Failed to load datapacks", e);
            throw new RuntimeException("Cannot generate data without datapacks", e);
        }

        // Verify we have data to work with
        if (CelestialObjectRegistry.getAll().isEmpty()) {
            throw new RuntimeException("No celestial objects found! Please create datapacks in src/main/resources/data/[namespace]/terradyne/celestial_objects/");
        }

        if (StarSystemRegistry.getAll().isEmpty()) {
            throw new RuntimeException("No star systems found! Please create datapacks in src/main/resources/data/[namespace]/terradyne/star_systems/");
        }

        // === REGISTER DATA PROVIDERS ===
        pack.addProvider(BiomeDataProvider::new);
        pack.addProvider(DimensionTypeDataProvider::new);
        pack.addProvider(FeatureDataProviders.ConfiguredFeatureDataProvider::new);
        pack.addProvider(FeatureDataProviders.PlacedFeatureDataProvider::new);
        pack.addProvider(FeatureDataProviders.FeatureValidationProvider::new);
        pack.addProvider(PlanetDimensionDataProvider::new);
        pack.addProvider(CelestialSkyDataProvider::new);

        Terradyne.LOGGER.info("=== DATA GENERATION SETUP COMPLETE ===");
        Terradyne.LOGGER.info("  - {} celestial objects", CelestialObjectRegistry.getAll().size());
        Terradyne.LOGGER.info("  - {} star systems", StarSystemRegistry.getAll().size());
    }

	@Override
	public void buildRegistry(RegistryBuilder registryBuilder) {
		Terradyne.LOGGER.info("=== BUILDING DYNAMIC REGISTRIES ===");

		// Register biomes for data generation
		registryBuilder.addRegistry(RegistryKeys.BIOME, bootstrap -> {
			// Biome registration happens in BiomeDataProvider
			Terradyne.LOGGER.debug("Biome registry builder initialized");
		});

		// Register dimension types for data generation
		registryBuilder.addRegistry(RegistryKeys.DIMENSION_TYPE, bootstrap -> {
			// Dimension type registration happens in DimensionTypeDataProvider
			Terradyne.LOGGER.debug("Dimension type registry builder initialized");
		});

		// FIXED: Register configured features for data generation
		registryBuilder.addRegistry(RegistryKeys.CONFIGURED_FEATURE, bootstrap -> {
			// This is where the actual configured feature registration happens
			ModConfiguredFeatures.bootstrap(bootstrap);
			Terradyne.LOGGER.debug("Configured feature registry builder initialized with tree features");
		});

		// FIXED: Register placed features for data generation
		registryBuilder.addRegistry(RegistryKeys.PLACED_FEATURE, bootstrap -> {
			// This is where the actual placed feature registration happens
			ModPlacedFeatures.bootstrap(bootstrap);
			Terradyne.LOGGER.debug("Placed feature registry builder initialized with tree placement rules");
		});

		Terradyne.LOGGER.info("✅ Dynamic registry builders initialized");
		Terradyne.LOGGER.info("Registry types: BIOME, DIMENSION_TYPE, CONFIGURED_FEATURE, PLACED_FEATURE");
		Terradyne.LOGGER.info("🌳 Tree features: {} configured + {} placed features will be generated",
				net.starlight.terradyne.planet.biology.BiomeFeatureComponents.TreeType.values().length,
				net.starlight.terradyne.planet.biology.BiomeFeatureComponents.TreeType.values().length);
	}
}