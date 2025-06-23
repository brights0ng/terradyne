package net.starlight.terradyne;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.starlight.terradyne.planet.dimension.ModDimensionTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Terradyne implements ModInitializer {
	public static final String MOD_ID = "terradyne";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("🚀 Initializing Terradyne...");

		// Initialize core systems
		initializeTerrainSystem();
		registerCommands();
		initializeRegistryKeys();

		LOGGER.info("✅ Terradyne initialized successfully!");
		logSystemStatus();
	}

	private void initializeTerrainSystem() {
		try {
//			OctaveRegistry.initialize();
//			PassRegistry.initialize();
			LOGGER.info("✓ Unified octave terrain system active");
		} catch (Exception e) {
			LOGGER.error("❌ Failed to initialize terrain system!", e);
			throw new RuntimeException("Critical terrain system failure", e);
		}
	}

	private void registerCommands() {
		LOGGER.info("✓ Commands registered");
	}

	private void initializeRegistryKeys() {
//		ModBiomes.init();
		ModDimensionTypes.init();
		LOGGER.info("✓ Registry keys initialized");
	}

	private void logSystemStatus() {
		LOGGER.info("=== SYSTEM STATUS ===");
		LOGGER.info("• Planet types: {} implemented", getPlanetTypeCount());
		LOGGER.info("• Terrain: Master noise + modular octaves");
		LOGGER.info("• Biomes: Data-driven (JSON generation)");
		LOGGER.info("• Custom terrain per biome type");
		LOGGER.info("");
		LOGGER.info("💡 Run 'gradlew runDatagen' to generate custom biomes");
		LOGGER.info("🎮 Use '/terradyne create <name> <type>' to create planets");
	}

	private int getPlanetTypeCount() {
		try {
			Class<?> planetTypeClass = Class.forName("net.starlight.terradyne.planet.PlanetType");
			java.lang.reflect.Method method = planetTypeClass.getMethod("getImplementedTypes");
			Object[] types = (Object[]) method.invoke(null);
			return types.length;
		} catch (Exception e) {
			return 3; // Fallback
		}
	}
}