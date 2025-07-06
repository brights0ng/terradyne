package net.starlight.terradyne;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.registry.RegistryBuilder;
import net.minecraft.registry.RegistryKeys;
import net.starlight.terradyne.datagen.*;
import net.starlight.terradyne.planet.features.ModConfiguredFeatures;
import net.starlight.terradyne.planet.features.ModPlacedFeatures;

/**
 * Main data generator entry point for Terradyne mod
 * FIXED: Complete implementation with proper registry bootstrapping
 */
public class TerradyneDataGenerator implements DataGeneratorEntrypoint {

	@Override
	public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
		FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();

		Terradyne.LOGGER.info("=== INITIALIZING TERRADYNE DATA GENERATION ===");

		// === CORE REGISTRY DATA ===
		// Register biome data provider (now uses physics-based features)
		pack.addProvider(BiomeDataProvider::new);
		Terradyne.LOGGER.info("✓ BiomeDataProvider registered (physics-based features enabled)");

		// Register dimension type data provider
		pack.addProvider(DimensionTypeDataProvider::new);
		Terradyne.LOGGER.info("✓ DimensionTypeDataProvider registered");

		// === FEATURE SYSTEM DATA ===
		// Register configured feature data provider (tree configurations)
		pack.addProvider(FeatureDataProviders.ConfiguredFeatureDataProvider::new);
		Terradyne.LOGGER.info("✓ ConfiguredFeatureDataProvider registered (tree features)");

		// Register placed feature data provider (tree placement rules)
		pack.addProvider(FeatureDataProviders.PlacedFeatureDataProvider::new);
		Terradyne.LOGGER.info("✓ PlacedFeatureDataProvider registered (tree placement)");

		// Register feature validation provider (system validation)
		pack.addProvider(FeatureDataProviders.FeatureValidationProvider::new);
		Terradyne.LOGGER.info("✓ FeatureValidationProvider registered");

		// === PLANET DIMENSION DATA ===
		// Register planet dimension data provider (hardcoded planets)
		pack.addProvider(PlanetDimensionDataProvider::new);
		Terradyne.LOGGER.info("✓ PlanetDimensionDataProvider registered");

		// === NEW: CELESTIAL SKY DATA ===
		pack.addProvider(CelestialSkyDataProvider::new);
		Terradyne.LOGGER.info("✓ CelestialSkyDataProvider registered (atmospheric sky rendering)");

		Terradyne.LOGGER.info("=== DATA GENERATION SETUP COMPLETE ===");
		Terradyne.LOGGER.info("Components registered:");
		Terradyne.LOGGER.info("  - {} hardcoded planets", HardcodedPlanets.getAllPlanetNames().size());
		Terradyne.LOGGER.info("  - {} biome types with physics-based features", 45);
		Terradyne.LOGGER.info("  - {} tree types with full feature integration",
				net.starlight.terradyne.planet.biology.BiomeFeatureComponents.TreeType.values().length);
		Terradyne.LOGGER.info("  - {} vegetation palettes for different crust compositions",
				net.starlight.terradyne.planet.biology.VegetationPalette.values().length);
		Terradyne.LOGGER.info("");
		Terradyne.LOGGER.info("🚀 Run 'gradle runDatagen' to generate all files");
		Terradyne.LOGGER.info("🌳 Tree generation will be fully functional after data generation");
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