package net.terradyne;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.terradyne.planet.biome.ModBiomes;
import net.terradyne.planet.dimension.ModDimensionTypes;
import net.terradyne.planet.terrain.OctaveRegistry;
import net.terradyne.util.CommandRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Terradyne implements ModInitializer {
	public static final String MOD_ID = "terradyne";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("🚀 Starting Terradyne initialization...");

		// Initialize the unified octave system FIRST
		initializeUnifiedTerrainSystem();

		// Register commands
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			CommandRegistry.init(dispatcher);
		});

		// Initialize biomes
		ModBiomes.init();

		// Initialize custom dimension types
		ModDimensionTypes.init();

		LOGGER.info("✅ Terradyne fully initialized!");
		LOGGER.info("   - Unified octave terrain system active");
		LOGGER.info("   - {} planet types available", getPlanetTypeCount());
		LOGGER.info("   - Master noise approach eliminates terrain conflicts");
		LOGGER.info("   - Biomes control octave selection for natural terrain");
	}

	/**
	 * Initialize the unified terrain generation system
	 * This is the core of the new architecture
	 */
	private void initializeUnifiedTerrainSystem() {
		LOGGER.info("=== INITIALIZING UNIFIED TERRAIN SYSTEM ===");

		try {
			// Initialize octave registry with all available octaves
			OctaveRegistry.initialize();

		} catch (Exception e) {
			LOGGER.error("❌ Failed to initialize unified terrain system!", e);
			throw new RuntimeException("Critical terrain system failure", e);
		}
	}

	/**
	 * Get count of implemented planet types for logging
	 */
	private int getPlanetTypeCount() {
		try {
			// Use reflection to access PlanetType.getImplementedTypes()
			Class<?> planetTypeClass = Class.forName("net.terradyne.planet.PlanetType");
			java.lang.reflect.Method method = planetTypeClass.getMethod("getImplementedTypes");
			Object[] types = (Object[]) method.invoke(null);
			return types.length;
		} catch (Exception e) {
			return 3; // Fallback: Desert, Oceanic, Rocky
		}
	}
}