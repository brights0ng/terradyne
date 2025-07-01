package net.starlight.terradyne.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.starlight.terradyne.planet.config.PlanetConfigLoader;
import net.starlight.terradyne.planet.dimension.PlanetDimensionManager;
import net.starlight.terradyne.planet.physics.PlanetConfig;
import net.starlight.terradyne.planet.world.WorldPlanetManager;

/**
 * Central command registry for all Terradyne commands
 * Moved from main Terradyne class for better organization
 */
public class CommandRegistry {

    /**
     * Register all Terradyne commands
     */
    public static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(CommandManager.literal("terradyne")
                .then(CommandManager.literal("list")
                        .executes(CommandRegistry::listPlanetsCommand)
                )
                .then(CommandManager.literal("teleport")
                        .then(CommandManager.argument("planet", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    // Suggest available planets
                                    getAvailablePlanets(context.getSource().getServer())
                                            .forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(CommandRegistry::teleportToPlanetCommand)
                        )
                )
                .then(CommandManager.literal("info")
                        .executes(CommandRegistry::planetInfoCommand)
                )
                .then(CommandManager.literal("debug")
                        .then(CommandManager.literal("registry")
                                .executes(CommandRegistry::debugRegistryCommand)
                        )
                        .then(CommandManager.literal("dimensions")
                                .executes(CommandRegistry::debugDimensionsCommand)
                        )
                )
                .then(CommandManager.literal("reload")
                        .requires(source -> source.hasPermissionLevel(3)) // OP only
                        .executes(CommandRegistry::reloadConfigCommand)
                )
        );
    }

    /**
     * List all available planets - simplified for datapack approach
     */
    private static int listPlanetsCommand(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        // Get planets from server's loaded dimensions
        var terradynePlanets = new java.util.ArrayList<String>();
        for (var world : source.getServer().getWorlds()) {
            String dimensionId = world.getRegistryKey().getValue().toString();
            if (dimensionId.startsWith("terradyne:")) {
                String planetName = dimensionId.substring("terradyne:".length());
                terradynePlanets.add(planetName);
            }
        }

        if (terradynePlanets.isEmpty()) {
            source.sendFeedback(() -> Text.literal("No planets available")
                    .formatted(Formatting.YELLOW), false);
            source.sendFeedback(() -> Text.literal("Add planet configs to ")
                    .append(Text.literal("saves/[world]/terradyne/planets/").formatted(Formatting.AQUA))
                    .append(" and restart server")
                    .formatted(Formatting.GRAY), false);
        } else {
            source.sendFeedback(() -> Text.literal("Available Planets (")
                    .append(Text.literal(String.valueOf(terradynePlanets.size())).formatted(Formatting.GREEN))
                    .append("):")
                    .formatted(Formatting.GOLD), false);

            for (String planetName : terradynePlanets) {
                source.sendFeedback(() -> Text.literal("  - ")
                        .append(Text.literal(planetName).formatted(Formatting.AQUA))
                        .formatted(Formatting.WHITE), false);
            }
        }

        return 1;
    }

    /**
     * Teleport to a planet - simplified
     */
    private static int teleportToPlanetCommand(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();

        String planetName = StringArgumentType.getString(context, "planet");

        // Check if planet dimension exists
        if (!planetExists(source.getServer(), planetName)) {
            source.sendError(Text.literal("Planet '")
                    .append(Text.literal(planetName).formatted(Formatting.RED))
                    .append("' not found"));

            // Suggest available planets
            var available = getAvailablePlanets(source.getServer());
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
     * Show planet information - simplified
     */
    private static int planetInfoCommand(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        try {
            // Load planet configs
            var planetConfigs = PlanetConfigLoader.loadAllPlanetConfigs(source.getServer());
            var availablePlanets = getAvailablePlanets(source.getServer());

            source.sendFeedback(() -> Text.literal("=== TERRADYNE PLANET INFO ===")
                    .formatted(Formatting.GOLD, Formatting.BOLD), false);

            source.sendFeedback(() -> Text.literal("Generation Mode: ")
                    .append(Text.literal("DATAPACK-BASED").formatted(Formatting.GREEN))
                    .formatted(Formatting.WHITE), false);

            source.sendFeedback(() -> Text.literal("Planet Configs: ")
                    .append(Text.literal(String.valueOf(planetConfigs.size())).formatted(Formatting.AQUA))
                    .formatted(Formatting.WHITE), false);

            source.sendFeedback(() -> Text.literal("Loaded Dimensions: ")
                    .append(Text.literal(String.valueOf(availablePlanets.size())).formatted(Formatting.AQUA))
                    .formatted(Formatting.WHITE), false);

            if (!planetConfigs.isEmpty()) {
                source.sendFeedback(() -> Text.literal("Planet Configurations:")
                        .formatted(Formatting.YELLOW), false);

                for (var entry : planetConfigs.entrySet()) {
                    PlanetConfig config = entry.getValue();
                    boolean isLoaded = availablePlanets.contains(entry.getKey());

                    source.sendFeedback(() -> Text.literal("  - ")
                            .append(Text.literal(config.getPlanetName()).formatted(Formatting.AQUA))
                            .append(" (")
                            .append(Text.literal(isLoaded ? "LOADED" : "NOT LOADED")
                                    .formatted(isLoaded ? Formatting.GREEN : Formatting.RED))
                            .append(")")
                            .formatted(Formatting.WHITE), false);
                }
            }

        } catch (Exception e) {
            source.sendError(Text.literal("Failed to get planet info: " + e.getMessage()));
        }

        return 1;
    }

// ADD these helper methods:

    /**
     * Check if planet exists in loaded dimensions
     */
    private static boolean planetExists(MinecraftServer server, String planetName) {
        String normalizedName = planetName.toLowerCase().replace(" ", "_");
        net.minecraft.util.Identifier dimensionId = new net.minecraft.util.Identifier("terradyne", normalizedName);
        net.minecraft.registry.RegistryKey<net.minecraft.world.World> worldKey =
                net.minecraft.registry.RegistryKey.of(net.minecraft.registry.RegistryKeys.WORLD, dimensionId);
        return server.getWorld(worldKey) != null;
    }

    /**
     * Get list of available planet names
     */
    private static java.util.List<String> getAvailablePlanets(MinecraftServer server) {
        var planets = new java.util.ArrayList<String>();
        for (var world : server.getWorlds()) {
            String dimensionId = world.getRegistryKey().getValue().toString();
            if (dimensionId.startsWith("terradyne:")) {
                planets.add(dimensionId.substring("terradyne:".length()));
            }
        }
        return planets;
    }

    /**
     * Debug registry information
     */
    private static int debugRegistryCommand(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        String registryInfo = WorldPlanetManager.getRegistryInfo(source.getServer());
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

    /**
     * Debug dimension information
     */
    private static int debugDimensionsCommand(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        source.sendFeedback(() -> Text.literal("=== DIMENSION DEBUG ===")
                .formatted(Formatting.YELLOW, Formatting.BOLD), false);

        // List all dimensions known to the server
        source.getServer().getWorlds().forEach(world -> {
            String dimensionId = world.getRegistryKey().getValue().toString();
            boolean isTerradyne = dimensionId.startsWith("terradyne:");
            
            MutableText text = Text.literal("  - " + dimensionId);
            if (isTerradyne) {
                text = text.formatted(Formatting.GREEN);
            } else {
                text = text.formatted(Formatting.GRAY);
            }

            MutableText finalText = text;
            source.sendFeedback(() -> finalText, false);
        });

        return 1;
    }

    /**
     * Reload planet configurations (for development)
     */
    private static int reloadConfigCommand(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        try {
            WorldPlanetManager.reloadConfigs(source.getServer());
            source.sendFeedback(() -> Text.literal("✅ Planet configurations reloaded")
                    .formatted(Formatting.GREEN), false);
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("❌ Failed to reload configs: " + e.getMessage()));
            return 0;
        }
    }
}