package net.terradyne;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.registry.RegistryBuilder;
import net.minecraft.registry.RegistryKeys;
import net.terradyne.datagen.BiomeDataProvider;

/**
 * Main data generator entry point for Terradyne mod
 */
public class TerradyneDataGenerator implements DataGeneratorEntrypoint {

	@Override
	public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
		FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();

		// Register biome data provider
		pack.addProvider(BiomeDataProvider::new);

		// Future data providers can be added here:
		// pack.addProvider(RecipeDataProvider::new);
		// pack.addProvider(LootTableDataProvider::new);

		Terradyne.LOGGER.info("✅ Data generation providers registered");
	}

	@Override
	public void buildRegistry(RegistryBuilder registryBuilder) {
		// Register biomes for data generation
		registryBuilder.addRegistry(RegistryKeys.BIOME, bootstrap -> {
			// Biome registration happens in BiomeDataProvider
		});
	}
}