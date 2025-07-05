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
import net.starlight.terradyne.planet.mapping.ClimateMapExporter;
import net.starlight.terradyne.planet.physics.PlanetConfig;

/**
 * Central command registry for all Terradyne commands
 * Moved from main Terradyne class for better organization
 */
public class CommandRegistry {

    /**
     * Register all Terradyne commands
     */
    public static void init(CommandDispatcher<ServerCommandSource> dispatcher) {
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
                        .then(CommandManager.literal("components")
                                .executes(CommandRegistry::debugComponentsCommand)
                        )
                        .then(CommandManager.literal("biome")
                                .then(CommandManager.argument("biome_name", StringArgumentType.string())
                                        .executes(CommandRegistry::debugBiomeCommand)
                                )
                        )
                )
                .then(CommandManager.literal("reload")
                        .requires(source -> source.hasPermissionLevel(3)) // OP only
                        .executes(CommandRegistry::reloadConfigCommand)
                )
                .then(CommandManager.literal("export")
                        .then(CommandManager.argument("planet", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    // Suggest available planets
                                    getAvailablePlanets(context.getSource().getServer())
                                            .forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(CommandRegistry::exportClimateCommand)
                        )
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
            source.sendFeedback(() -> Text.literal("✅ Planet configurations reloaded")
                    .formatted(Formatting.GREEN), false);
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("❌ Failed to reload configs: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Export climate maps for a planet
     */
    private static int exportClimateCommand(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String planetName = StringArgumentType.getString(context, "planet");

        // Check if planet exists
        if (!planetExists(source.getServer(), planetName)) {
            source.sendError(Text.literal("Planet '")
                    .append(Text.literal(planetName).formatted(Formatting.RED))
                    .append("' not found"));
            return 0;
        }

        try {
            source.sendFeedback(() -> Text.literal("🗺️ Exporting climate maps for ")
                    .append(Text.literal(planetName).formatted(Formatting.GREEN))
                    .append("...")
                    .formatted(Formatting.WHITE), false);

            // Export all climate maps
            ClimateMapExporter.exportAllMaps(source.getServer(), planetName);

            source.sendFeedback(() -> Text.literal("✅ Climate maps exported successfully! Check ")
                    .append(Text.literal("saves/[world]/terradyne/exports/").formatted(Formatting.AQUA))
                    .formatted(Formatting.GREEN), false);

            return 1;

        } catch (Exception e) {
            source.sendError(Text.literal("❌ Failed to export climate maps: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Debug registry information
     */
    private static int debugRegistryCommand(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        source.sendFeedback(() -> Text.literal("=== COMPONENT REGISTRY DEBUG ===")
                .formatted(Formatting.YELLOW, Formatting.BOLD), false);

        try {
            // Get component registry stats
            String registryStats = net.starlight.terradyne.planet.biology.BiomeComponentRegistry.getRegistryStats();
            String[] lines = registryStats.split("\n");

            for (String line : lines) {
                source.sendFeedback(() -> Text.literal(line).formatted(Formatting.WHITE), false);
            }

            // Get runtime tree feature stats
            String treeStats = net.starlight.terradyne.planet.features.RuntimeTreeFeatures.getCacheStats();
            source.sendFeedback(() -> Text.literal(treeStats).formatted(Formatting.AQUA), false);

        } catch (Exception e) {
            source.sendError(Text.literal("Failed to get registry stats: " + e.getMessage()));
        }

        return 1;
    }

    /**
     * Debug component system
     */
    private static int debugComponentsCommand(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        source.sendFeedback(() -> Text.literal("=== COMPONENT SYSTEM DEBUG ===")
                .formatted(Formatting.GREEN, Formatting.BOLD), false);

        try {
            // Show vegetation palettes
            source.sendFeedback(() -> Text.literal("Vegetation Palettes:")
                    .formatted(Formatting.YELLOW), false);

//            for (var crustType : net.starlight.terradyne.planet.physics.CrustComposition.values()) {
//                var palette = net.starlight.terradyne.planet.biology.VegetationPalette.fromCrustComposition(crustType);
//
//                source.sendFeedback(() -> Text.literal("  " + crustType.getDisplayName() + " → " + palette.getDisplayName())
//                        .formatted(palette.hasVegetation() ? Formatting.GREEN : Formatting.RED), false);
//            }

            // Show sample biome components
            source.sendFeedback(() -> Text.literal("\nSample Biome Components:")
                    .formatted(Formatting.YELLOW), false);

            var sampleBiomes = new net.minecraft.registry.RegistryKey[] {
                    net.starlight.terradyne.planet.biome.ModBiomes.OAK_FOREST,
                    net.starlight.terradyne.planet.biome.ModBiomes.JUNGLE,
                    net.starlight.terradyne.planet.biome.ModBiomes.SANDY_DESERT,
                    net.starlight.terradyne.planet.biome.ModBiomes.VOLCANIC_MOUNTAINS
            };

            for (var biomeKey : sampleBiomes) {
                var components = net.starlight.terradyne.planet.biology.BiomeComponentRegistry.getComponents(biomeKey);
                if (components != null) {
                    source.sendFeedback(() -> Text.literal("  " + biomeKey.getValue().getPath() + ":")
                            .formatted(Formatting.AQUA), false);
                    source.sendFeedback(() -> Text.literal("    " + components.toString())
                            .formatted(Formatting.WHITE), false);
                }
            }

        } catch (Exception e) {
            source.sendError(Text.literal("Failed to show component debug: " + e.getMessage()));
        }

        return 1;
    }

    /**
     * Debug specific biome components
     */
    private static int debugBiomeCommand(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String biomeName = StringArgumentType.getString(context, "biome_name");

        try {
            // Find biome by name
            net.minecraft.util.Identifier biomeId = new net.minecraft.util.Identifier("terradyne", biomeName.toLowerCase());
            net.minecraft.registry.RegistryKey<net.minecraft.world.biome.Biome> biomeKey =
                    net.minecraft.registry.RegistryKey.of(net.minecraft.registry.RegistryKeys.BIOME, biomeId);

            var components = net.starlight.terradyne.planet.biology.BiomeComponentRegistry.getComponents(biomeKey);

            if (components == null) {
                source.sendError(Text.literal("Biome '" + biomeName + "' not found or has no components"));
                return 0;
            }

            source.sendFeedback(() -> Text.literal("=== BIOME: " + biomeName.toUpperCase() + " ===")
                    .formatted(Formatting.GOLD, Formatting.BOLD), false);

            source.sendFeedback(() -> Text.literal("Components: " + components.toString())
                    .formatted(Formatting.WHITE), false);

            // Show feature description for different planet types
            var crustTypes = new net.starlight.terradyne.planet.physics.CrustComposition[]{
                    net.starlight.terradyne.planet.physics.CrustComposition.SILICATE,
                    net.starlight.terradyne.planet.physics.CrustComposition.CARBONACEOUS,
                    net.starlight.terradyne.planet.physics.CrustComposition.BASALTIC
            };

            for (var crustType : crustTypes) {
                source.sendFeedback(() -> Text.literal("\nOn " + crustType.getDisplayName() + " planet:")
                        .formatted(Formatting.YELLOW), false);
//
//                String description = net.starlight.terradyne.planet.biology.BiomeFeatureGenerator.getFeatureDescription(
//                        biomeKey, crustType, 15.0, 0.5, 0.7); // Sample conditions

//                String[] lines = description.split("\n");
//                for (String line : lines) {
//                    source.sendFeedback(() -> Text.literal("  " + line)
//                            .formatted(Formatting.GRAY), false);
//                }
            }

        } catch (Exception e) {
            source.sendError(Text.literal("Failed to debug biome: " + e.getMessage()));
        }

        return 1;

    }

}