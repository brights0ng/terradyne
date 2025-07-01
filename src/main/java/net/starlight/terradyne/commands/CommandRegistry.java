package net.starlight.terradyne.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.starlight.terradyne.planet.dimension.PlanetDimensionManager;
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
                                    WorldPlanetManager.getAvailablePlanets(context.getSource().getServer())
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
     * List all available planets
     */
    private static int listPlanetsCommand(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        var availablePlanets = WorldPlanetManager.getAvailablePlanets(source.getServer());

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
    private static int teleportToPlanetCommand(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();

        String planetName = StringArgumentType.getString(context, "planet");

        if (!WorldPlanetManager.isPlanetAvailable(source.getServer(), planetName)) {
            source.sendError(Text.literal("Planet '")
                    .append(Text.literal(planetName).formatted(Formatting.RED))
                    .append("' not found"));

            // Suggest available planets
            var available = WorldPlanetManager.getAvailablePlanets(source.getServer());
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
    private static int planetInfoCommand(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        String info = WorldPlanetManager.getPlanetInfo(source.getServer());
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