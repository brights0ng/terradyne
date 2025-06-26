// UPDATE TerradyneDataGenerator.java - Add dimension type provider

package net.starlight.terradyne;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.registry.RegistryBuilder;
import net.minecraft.registry.RegistryKeys;
import net.starlight.terradyne.datagen.BiomeDataProvider;
import net.starlight.terradyne.datagen.DimensionTypeDataProvider;

/**
 * Main data generator entry point for Terradyne mod
 * UPDATED: Now includes dimension type generation
 */
public class TerradyneDataGenerator implements DataGeneratorEntrypoint {

	@Override
	public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
		FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();

		// Register biome data provider
		pack.addProvider(BiomeDataProvider::new);

		// NEW: Register dimension type data provider
		pack.addProvider(DimensionTypeDataProvider::new);

		// Future data providers can be added here:
		// pack.addProvider(RecipeDataProvider::new);
		// pack.addProvider(LootTableDataProvider::new);

		Terradyne.LOGGER.info("✅ Data generation providers registered (including dimension types)");
	}

	@Override
	public void buildRegistry(RegistryBuilder registryBuilder) {
		// Register biomes for data generation
		registryBuilder.addRegistry(RegistryKeys.BIOME, bootstrap -> {
			// Biome registration happens in BiomeDataProvider
		});

		// NEW: Register dimension types for data generation
		registryBuilder.addRegistry(RegistryKeys.DIMENSION_TYPE, bootstrap -> {
			// Dimension type registration happens in DimensionTypeDataProvider
		});
	}
}