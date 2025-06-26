package net.starlight.terradyne;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.starlight.terradyne.blocks.ModBlocks;
import net.starlight.terradyne.planet.config.StartupPlanetGenerator;
import net.starlight.terradyne.planet.dimension.ModDimensionTypes;
import net.starlight.terradyne.planet.dimension.PlanetDimensionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main mod class - now uses config-driven startup generation
 * Removed all runtime planet generation in favor of startup-only approach
 */
public class Terradyne implements ModInitializer {
	public static final String MOD_ID = "terradyne";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static StartupPlanetGenerator planetGenerator;

	@Override
	public void onInitialize() {
		LOGGER.info("🚀 Initializing Terradyne...");

		// Initialize core systems
		registerBlocks();            // ← ADD THIS LINE
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
			registerTerradyneCommands(dispatcher, registryAccess);
		});
		LOGGER.info("✓ Commands registered (startup generation mode)");
	}

	/**
	 * Register server lifecycle events for startup planet generation
	 */
	private void registerServerEvents() {
		ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
		LOGGER.info("✓ Server lifecycle events registered");
	}

	/**
	 * Handle server startup - generate planets from config files
	 * Called after server is fully started and overworld is available
	 */
	private void onServerStarted(MinecraftServer server) {
		LOGGER.info("=== SERVER STARTED - STARTUP PLANET GENERATION ===");

		try {
			// Create startup planet generator
			planetGenerator = new StartupPlanetGenerator(server);

			// Generate all configured planets that don't already exist
			planetGenerator.generateStartupPlanets();

			LOGGER.info("✅ Startup planet generation completed");

		} catch (Exception e) {
			LOGGER.error("❌ Startup planet generation failed", e);
		}
	}

	/**
	 * Register Terradyne commands - UPDATED: No runtime generation
	 */
	private void registerTerradyneCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
		dispatcher.register(CommandManager.literal("terradyne")
				.then(CommandManager.literal("list")
						.executes(this::listPlanetsCommand)
				)
				.then(CommandManager.literal("teleport")
						.then(CommandManager.argument("planet", StringArgumentType.string())
								.suggests((context, builder) -> {
									// Suggest available planets
									if (planetGenerator != null) {
										planetGenerator.getAvailablePlanets().forEach(builder::suggest);
									}
									return builder.buildFuture();
								})
								.executes(this::teleportToPlanetCommand)
						)
				)
				.then(CommandManager.literal("info")
						.executes(this::planetInfoCommand)
				)
				.then(CommandManager.literal("debug")
						.then(CommandManager.literal("registry")
								.executes(this::debugRegistryCommand)
						)
				)
		);
	}

	/**
	 * List all available planets
	 */
	private int listPlanetsCommand(CommandContext<ServerCommandSource> context) {
		ServerCommandSource source = context.getSource();

		if (planetGenerator == null) {
			source.sendError(Text.literal("Planet generator not initialized"));
			return 0;
		}

		var availablePlanets = planetGenerator.getAvailablePlanets();

		if (availablePlanets.isEmpty()) {
			source.sendFeedback(() -> Text.literal("No planets available")
					.formatted(Formatting.YELLOW), false);
			source.sendFeedback(() -> Text.literal("Add planet configs to ")
					.append(Text.literal("saves/[world]/terradyne/planets/").formatted(Formatting.AQUA))
					.append(" and restart server")
					.formatted(Formatting.GRAY), false);
		} else {
			source.sendFeedback(() -> Text.literal("Available Planets (")
					.append(Text.literal(String.valueOf(availablePlanets.size())).formatted(Formatting.GREEN))
					.append("):")
					.formatted(Formatting.GOLD), false);

			for (String planetName : availablePlanets) {
				source.sendFeedback(() -> Text.literal("  - ")
						.append(Text.literal(planetName).formatted(Formatting.AQUA))
						.formatted(Formatting.WHITE), false);
			}
		}

		return 1;
	}

	/**
	 * Teleport to a planet
	 */
	private int teleportToPlanetCommand(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerCommandSource source = context.getSource();
		ServerPlayerEntity player = source.getPlayerOrThrow();

		String planetName = StringArgumentType.getString(context, "planet");

		if (planetGenerator == null) {
			source.sendError(Text.literal("Planet generator not initialized"));
			return 0;
		}

		if (!planetGenerator.isPlanetAvailable(planetName)) {
			source.sendError(Text.literal("Planet '")
					.append(Text.literal(planetName).formatted(Formatting.RED))
					.append("' not found"));

			// Suggest available planets
			var available = planetGenerator.getAvailablePlanets();
			if (!available.isEmpty()) {
				source.sendFeedback(() -> Text.literal("Available planets: ")
						.append(Text.literal(String.join(", ", available)).formatted(Formatting.AQUA))
						.formatted(Formatting.GRAY), false);
			}
			return 0;
		}

		try {
			PlanetDimensionManager.teleportToPlanet(player, planetName);
			source.sendFeedback(() -> Text.literal("🚀 Teleporting to ")
					.append(Text.literal(planetName).formatted(Formatting.GREEN))
					.append("...")
					.formatted(Formatting.WHITE), false);
			return 1;
		} catch (Exception e) {
			source.sendError(Text.literal("❌ Failed to teleport: " + e.getMessage()));
			return 0;
		}
	}

	/**
	 * Show detailed planet information
	 */
	private int planetInfoCommand(CommandContext<ServerCommandSource> context) {
		ServerCommandSource source = context.getSource();

		if (planetGenerator == null) {
			source.sendError(Text.literal("Planet generator not initialized"));
			return 0;
		}

		String info = planetGenerator.getPlanetInfo();
		String[] lines = info.split("\n");

		for (String line : lines) {
			if (line.trim().isEmpty()) continue;

			MutableText text;
			if (line.startsWith("===")) {
				text = Text.literal(line).formatted(Formatting.GOLD, Formatting.BOLD);
			} else if (line.contains("✅")) {
				text = Text.literal(line).formatted(Formatting.GREEN);
			} else if (line.contains("❌")) {
				text = Text.literal(line).formatted(Formatting.RED);
			} else if (line.startsWith("  ")) {
				text = Text.literal(line).formatted(Formatting.GRAY);
			} else {
				text = Text.literal(line).formatted(Formatting.WHITE);
			}

			source.sendFeedback(() -> text, false);
		}

		return 1;
	}

	/**
	 * Debug registry information
	 */
	private int debugRegistryCommand(CommandContext<ServerCommandSource> context) {
		ServerCommandSource source = context.getSource();

		if (planetGenerator == null) {
			source.sendError(Text.literal("Planet generator not initialized"));
			return 0;
		}

		String registryInfo = planetGenerator.getPlanetRegistry().getRegistryInfo();
		String[] lines = registryInfo.split("\n");

		for (String line : lines) {
			if (line.trim().isEmpty()) continue;

			MutableText text;
			if (line.startsWith("===")) {
				text = Text.literal(line).formatted(Formatting.YELLOW, Formatting.BOLD);
			} else if (line.contains("[PROTECTED]")) {
				text = Text.literal(line).formatted(Formatting.GREEN);
			} else if (line.startsWith("  ")) {
				text = Text.literal(line).formatted(Formatting.GRAY);
			} else {
				text = Text.literal(line).formatted(Formatting.WHITE);
			}

			source.sendFeedback(() -> text, false);
		}

		return 1;
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
		LOGGER.info("• Generation Mode: CONFIG-DRIVEN STARTUP");
		LOGGER.info("• Physics System: ACTIVE");
		LOGGER.info("• Planet Safety: EXISTING PLANETS PROTECTED");
		LOGGER.info("• Config Location: saves/[world]/terradyne/planets/*.json");
		LOGGER.info("• Dimension Types: Data-Generated JSON");
		LOGGER.info("");
		LOGGER.info("🌍 Add planet configs to your world's terradyne/planets/ folder");
		LOGGER.info("🔄 Restart server to generate new planets");
		LOGGER.info("🚀 Use '/terradyne list' to see available planets");
		LOGGER.info("🎮 Use '/terradyne teleport <planet>' to explore");
		LOGGER.info("📊 Use '/terradyne info' for detailed planet information");
	}
}