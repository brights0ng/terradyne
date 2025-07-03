package net.starlight.terradyne;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.registry.Registry;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.starlight.terradyne.blocks.ModBlocks;
import net.starlight.terradyne.commands.CommandRegistry;
import net.starlight.terradyne.planet.biome.ModBiomes;
import net.starlight.terradyne.planet.chunk.UniversalChunkGenerator;
import net.starlight.terradyne.planet.dimension.ModDimensionTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main mod class - SIMPLIFIED for data generation approach
 * No more runtime datapack generation - planets are defined via data generation
 */
public class Terradyne implements ModInitializer {
	public static final String MOD_ID = "terradyne";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("🚀 Initializing Terradyne...");

		// Initialize core systems
		registerBlocks();
		registerChunkGenerators();
		initializeTerrainSystem();
		registerCommands();
		initializeRegistryKeys();
		registerServerEvents();

		LOGGER.info("✅ Terradyne initialized successfully!");
		logSystemStatus();
	}

	private void registerChunkGenerators() {
		try {
			Registry.register(
					Registries.CHUNK_GENERATOR,
					new Identifier(MOD_ID, "universal"),
					UniversalChunkGenerator.CODEC
			);
			LOGGER.info("✓ Chunk generator 'terradyne:universal' registered");
		} catch (Exception e) {
			LOGGER.error("❌ Failed to register chunk generator!", e);
			throw new RuntimeException("Critical chunk generator registration failure", e);
		}
	}

	private void initializeTerrainSystem() {
		try {
			// Physics system is initialized on-demand
			LOGGER.info("✓ Physics-based terrain system ready");
		} catch (Exception e) {
			LOGGER.error("❌ Failed to initialize terrain system!", e);
			throw new RuntimeException("Critical terrain system failure", e);
		}
	}

	private void registerCommands() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			CommandRegistry.registerCommands(dispatcher, registryAccess);
		});
		LOGGER.info("✓ Commands registered");
	}

	/**
	 * Register server lifecycle events - SIMPLIFIED (no runtime generation)
	 */
	private void registerServerEvents() {
		// Just validate after server starts - no runtime generation needed
		ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);

		LOGGER.info("✓ Server lifecycle events registered");
	}

	/**
	 * Validate planets after server startup
	 */
	private void onServerStarted(MinecraftServer server) {
		LOGGER.info("=== SERVER STARTED - VALIDATION ===");

		try {
			validateDimensionsLoaded(server);
		} catch (Exception e) {
			LOGGER.error("❌ Post-startup validation failed", e);
		}
	}

	/**
	 * Validate that our dimensions were loaded from data generation
	 */
	private void validateDimensionsLoaded(MinecraftServer server) {
		LOGGER.info("Validating dimensions loaded from data generation...");

		int terradyneDimensions = 0;
		for (var world : server.getWorlds()) {
			String dimensionId = world.getRegistryKey().getValue().toString();
			if (dimensionId.startsWith("terradyne:")) {
				String planetName = dimensionId.substring("terradyne:".length());

				// Check if it's a hardcoded planet
				boolean isHardcoded = net.starlight.terradyne.datagen.HardcodedPlanets.isHardcodedPlanet(planetName);

				LOGGER.info("✅ Planet dimension loaded: {} ({})",
						dimensionId, isHardcoded ? "HARDCODED" : "USER-DEFINED");
				terradyneDimensions++;
			}
		}

		if (terradyneDimensions > 0) {
			LOGGER.info("✅ Successfully loaded {} Terradyne planet dimensions", terradyneDimensions);
			LOGGER.info("Hardcoded planets available: {}",
					net.starlight.terradyne.datagen.HardcodedPlanets.getAllPlanetNames());
		} else {
			LOGGER.warn("⚠️  No Terradyne dimensions found");
			LOGGER.info("Expected hardcoded planets: {}",
					net.starlight.terradyne.datagen.HardcodedPlanets.getAllPlanetNames());
			LOGGER.info("Make sure you ran 'gradle runDatagen' to generate dimension files");
		}
	}

	private void initializeRegistryKeys() {
		ModDimensionTypes.init();
		ModBiomes.init();
		LOGGER.info("✓ Registry keys initialized");
	}

	private void registerBlocks() {
		ModBlocks.initialize();
	}

	private void logSystemStatus() {
		LOGGER.info("=== SYSTEM STATUS ===");
		LOGGER.info("• Generation Mode: DATA-GENERATION-BASED");
		LOGGER.info("• Physics System: ACTIVE");
		LOGGER.info("• Chunk Generator: terradyne:universal REGISTERED");
		LOGGER.info("• Planet Definitions: HARDCODED + USER-CUSTOMIZABLE");
		LOGGER.info("• Hardcoded Planets: {}", net.starlight.terradyne.datagen.HardcodedPlanets.getAllPlanetNames());
		LOGGER.info("• User Config Location: saves/[world]/terradyne/planets/*.json");
		LOGGER.info("• Generated Files: src/generated/resources/data/terradyne/dimension/");
		LOGGER.info("");
		LOGGER.info("🔧 Run 'gradle runDatagen' to generate dimension files");
		LOGGER.info("🚀 Use '/terradyne list' to see available planets");
		LOGGER.info("🎮 Use '/terradyne teleport <planet>' to explore");
		LOGGER.info("📊 Use '/terradyne info' for detailed planet information");
		LOGGER.info("");
		LOGGER.info("🌍 Hardcoded planets are always available!");
		LOGGER.info("📝 Add custom configs to terradyne/planets/ for user planets");
	}
}