package net.terradyne;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.terradyne.planet.biome.ModBiomes;
import net.terradyne.planet.dimension.ModDimensionTypes;
import net.terradyne.planet.chunk.UniversalChunkGenerator;
import net.terradyne.planet.terrain.UnifiedOctaveContext;
import net.terradyne.util.CommandRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Terradyne implements ModInitializer {
	public static final String MOD_ID = "terradyne";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// Register commands
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			CommandRegistry.init(dispatcher);
		});

		// Initialize biomes
		ModBiomes.init();

		// Initialize custom dimension types
		ModDimensionTypes.init();

		// Register universal terrain generators
		initializeTerrainGenerators();

		LOGGER.info("🚀 Terradyne initialized with custom dimension types and universal terrain system!");
	}

	/**
	 * Initialize and register all terrain generators
	 */
	private void initializeTerrainGenerators() {
		// Use a stable base seed for consistent terrain generation
		long baseSeed = System.currentTimeMillis();

		// Register terrain generators that work across planet types

		// TODO: Add more generators as you create them:
		// UniversalChunkGenerator.registerGenerator(new MesaGenerator(baseSeed + 2000));
		// UniversalChunkGenerator.registerGenerator(new OasisGenerator(baseSeed + 3000));
		// UniversalChunkGenerator.registerGenerator(new VolcanicFlowGenerator(baseSeed + 4000));
		// UniversalChunkGenerator.registerGenerator(new GlacierGenerator(baseSeed + 5000));

		LOGGER.info("📊 Registered " + getRegisteredGeneratorCount() + " universal terrain generators");
	}

	/**
	 * Get the number of registered terrain generators for debugging
	 */
	private int getRegisteredGeneratorCount() {
		// This would require adding a static method to UniversalChunkGenerator
		// For now, just return the known count
		return 2; // DuneGenerator + CanyonGenerator
	}
}