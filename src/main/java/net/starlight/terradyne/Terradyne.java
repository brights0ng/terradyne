package net.starlight.terradyne;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.starlight.terradyne.blocks.ModBlocks;
import net.starlight.terradyne.commands.CommandRegistry;
import net.starlight.terradyne.planet.dimension.ModDimensionTypes;
import net.starlight.terradyne.planet.world.WorldPlanetManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main mod class - simplified to focus on core initialization
 * All planet management moved to WorldPlanetManager
 * All commands moved to CommandRegistry
 */
public class Terradyne implements ModInitializer {
	public static final String MOD_ID = "terradyne";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("🚀 Initializing Terradyne...");

		// Initialize core systems
		registerBlocks();
		initializeTerrainSystem();
		registerCommands();
		initializeRegistryKeys();
		registerServerEvents();

		LOGGER.info("✅ Terradyne initialized successfully!");
		logSystemStatus();
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
	 * Register server lifecycle events for world-based planet management
	 */
	private void registerServerEvents() {
		// Hook into world loading before server starts
		ServerLifecycleEvents.SERVER_STARTING.register(this::onServerStarting);
		ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);

		// NEW: Hook into world loading events (earlier than server starting)
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents.LOAD.register(this::onWorldLoad);

		LOGGER.info("✓ Server lifecycle events registered");
	}

	/**
	 * Handle world loading - modify level.dat before server fully starts
	 * This fires when each world is loaded, including at server startup
	 */
	private void onWorldLoad(MinecraftServer server, net.minecraft.server.world.ServerWorld world) {
		// Only modify level.dat for overworld loading (first world loaded)
		if (world.getRegistryKey().equals(net.minecraft.world.World.OVERWORLD)) {
			LOGGER.info("=== OVERWORLD LOADING - CHECKING LEVEL.DAT ===");

			try {
				// Try to modify level.dat if this is a new world
				// This should happen before other dimensions are loaded
				WorldPlanetManager.attemptEarlyLevelDatModification(server);
			} catch (Exception e) {
				LOGGER.error("❌ Failed to modify level.dat during world loading", e);
			}
		}
	}

	/**
	 * Handle server starting - register dimensions for new worlds
	 * This happens BEFORE Minecraft finalizes level.dat
	 */
	private void onServerStarting(MinecraftServer server) {
		LOGGER.info("=== SERVER STARTING - WORLD INITIALIZATION ===");

		try {
			WorldPlanetManager.initializeForServer(server);
		} catch (Exception e) {
			LOGGER.error("❌ World initialization failed", e);
		}
	}

	/**
	 * Handle server started - validate existing worlds
	 * This happens AFTER all dimensions are loaded
	 */
	private void onServerStarted(MinecraftServer server) {
		LOGGER.info("=== SERVER STARTED - VALIDATION ===");

		try {
			WorldPlanetManager.validateAfterStartup(server);
		} catch (Exception e) {
			LOGGER.error("❌ Post-startup validation failed", e);
		}
	}

	private void initializeRegistryKeys() {
		ModDimensionTypes.init();
		LOGGER.info("✓ Registry keys initialized");
	}

	private void registerBlocks() {
		ModBlocks.initialize();
	}

	private void logSystemStatus() {
		LOGGER.info("=== SYSTEM STATUS ===");
		LOGGER.info("• Generation Mode: WORLD-CREATION-BASED");
		LOGGER.info("• Physics System: ACTIVE");
		LOGGER.info("• Planet Safety: EXISTING PLANETS PROTECTED");
		LOGGER.info("• Config Location: saves/[world]/terradyne/planets/*.json");
		LOGGER.info("• Dimension Types: Data-Generated JSON");
		LOGGER.info("");
		LOGGER.info("🌍 Add planet configs to your world's terradyne/planets/ folder");
		LOGGER.info("🔄 Restart server to register new planets in NEW worlds");
		LOGGER.info("🚀 Use '/terradyne list' to see available planets");
		LOGGER.info("🎮 Use '/terradyne teleport <planet>' to explore");
		LOGGER.info("📊 Use '/terradyne info' for detailed planet information");
	}
}