package net.terradyne.util;
// CommandRegistry.java - COMPLETE VERSION with Desert + Oceanic + Rocky support
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import net.terradyne.Terradyne;
import net.terradyne.planet.PlanetType;
import net.terradyne.planet.dimension.PlanetDimensionManager;
import net.terradyne.planet.config.DesertConfig;
import net.terradyne.planet.config.OceanicConfig;
import net.terradyne.planet.config.RockyConfig;
import net.terradyne.planet.factory.DesertPlanetFactory;
import net.terradyne.planet.factory.OceanicPlanetFactory;
import net.terradyne.planet.factory.RockyPlanetFactory;
import net.terradyne.planet.model.DesertModel;
import net.terradyne.planet.model.OceanicModel;
import net.terradyne.planet.model.RockyModel;

public class CommandRegistry {

    public static void init(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("terradyne")
                .then(CommandManager.literal("create")
                        .then(CommandManager.argument("name", StringArgumentType.string())
                                .then(CommandManager.argument("type", StringArgumentType.string())
                                        .then(CommandManager.argument("subtype", StringArgumentType.string())
                                                .suggests((context, builder) -> {
                                                    String type = StringArgumentType.getString(context, "type");
                                                    PlanetType planetType = PlanetType.fromString(type);

                                                    if (planetType == PlanetType.DESERT || planetType == PlanetType.HOTHOUSE) {
                                                        builder.suggest("standard");
                                                        builder.suggest("hot");
                                                        builder.suggest("rocky");
                                                    } else if (planetType == PlanetType.OCEANIC) {
                                                        builder.suggest("earthlike");
                                                        builder.suggest("tropical");
                                                        builder.suggest("archipelago");
                                                        builder.suggest("deepocean");
                                                    } else if (planetType == PlanetType.ROCKY) {
                                                        builder.suggest("moonlike");
                                                        builder.suggest("asteroid");
                                                        builder.suggest("barren");
                                                        builder.suggest("shattered");
                                                    }
                                                    return builder.buildFuture();
                                                })
                                                .executes(CommandRegistry::createPlanetWithSubtype))
                                        .suggests((context, builder) -> {
                                            for (PlanetType type : PlanetType.getImplementedTypes()) {
                                                for (String suggestion : type.getCommandSuggestions()) {
                                                    builder.suggest(suggestion);
                                                }
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(CommandRegistry::createPlanetWithType))
                                .executes(CommandRegistry::createDefaultPlanet)))

                .then(CommandManager.literal("visit")
                        .then(CommandManager.argument("name", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    String planets = PlanetDimensionManager.listPlanets();
                                    if (!planets.equals("No planets created yet")) {
                                        String[] planetNames = planets.split(", ");
                                        for (String planet : planetNames) {
                                            String cleanName = planet.split(" \\(")[0];
                                            builder.suggest(cleanName);
                                        }
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(CommandRegistry::visitPlanet)))

                .then(CommandManager.literal("list")
                        .executes(CommandRegistry::listPlanets))

                .then(CommandManager.literal("info")
                        .then(CommandManager.argument("name", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    String planets = PlanetDimensionManager.listPlanets();
                                    if (!planets.equals("No planets created yet")) {
                                        String[] planetNames = planets.split(", ");
                                        for (String planet : planetNames) {
                                            String cleanName = planet.split(" \\(")[0];
                                            builder.suggest(cleanName);
                                        }
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(CommandRegistry::showPlanetInfo)))

                .then(CommandManager.literal("types")
                        .executes(CommandRegistry::listPlanetTypes))

                .then(CommandManager.literal("help")
                        .executes(CommandRegistry::showHelp)));
    }

    // CREATE PLANET WITH TYPE AND SUBTYPE
    private static int createPlanetWithSubtype(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String planetName = StringArgumentType.getString(context, "name");
        String typeString = StringArgumentType.getString(context, "type");
        String subtypeString = StringArgumentType.getString(context, "subtype");
        ServerCommandSource source = context.getSource();

        PlanetType planetType = PlanetType.fromString(typeString);

        Terradyne.LOGGER.info("=== COMMAND: CREATE PLANET WITH SUBTYPE ===");
        Terradyne.LOGGER.info("Planet: " + planetName);
        Terradyne.LOGGER.info("Type: " + typeString + " -> " + planetType.getDisplayName());
        Terradyne.LOGGER.info("Subtype: " + subtypeString);

        if (PlanetDimensionManager.planetExists(planetName)) {
            source.sendError(Text.literal("Planet '" + planetName + "' already exists!"));
            return 0;
        }

        if (!planetType.isImplemented()) {
            source.sendError(Text.literal("Planet type '" + planetType.getDisplayName() +
                    "' is not implemented yet!"));
            return 0;
        }

        try {
            if (planetType == PlanetType.OCEANIC) {
                return createOceanicPlanetWithSubtype(source, planetName, subtypeString);
            } else if (planetType == PlanetType.ROCKY) {
                return createRockyPlanetWithSubtype(source, planetName, subtypeString);
            } else {
                return createDesertPlanetWithSubtype(source, planetName, planetType, subtypeString);
            }

        } catch (Exception e) {
            source.sendError(Text.literal("Failed to create planet: " + e.getMessage()));
            Terradyne.LOGGER.error("Planet creation failed", e);
            return 0;
        }
    }

    // CREATE PLANET WITH TYPE (NO SUBTYPE)
    private static int createPlanetWithType(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String planetName = StringArgumentType.getString(context, "name");
        String typeString = StringArgumentType.getString(context, "type");
        ServerCommandSource source = context.getSource();

        PlanetType planetType = PlanetType.fromString(typeString);

        if (PlanetDimensionManager.planetExists(planetName)) {
            source.sendError(Text.literal("Planet '" + planetName + "' already exists!"));
            return 0;
        }

        if (!planetType.isImplemented()) {
            source.sendError(Text.literal("Planet type '" + planetType.getDisplayName() +
                    "' is not implemented yet!"));
            return 0;
        }

        try {
            if (planetType == PlanetType.OCEANIC) {
                return createOceanicPlanetWithSubtype(source, planetName, "earthlike");
            } else if (planetType == PlanetType.ROCKY) {
                return createRockyPlanetWithSubtype(source, planetName, "moonlike");
            } else {
                return createDesertPlanetWithSubtype(source, planetName, planetType, "standard");
            }

        } catch (Exception e) {
            source.sendError(Text.literal("Failed to create planet: " + e.getMessage()));
            return 0;
        }
    }

    // CREATE DEFAULT PLANET (NO TYPE SPECIFIED)
    private static int createDefaultPlanet(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String planetName = StringArgumentType.getString(context, "name");
        ServerCommandSource source = context.getSource();

        if (PlanetDimensionManager.planetExists(planetName)) {
            source.sendError(Text.literal("Planet '" + planetName + "' already exists!"));
            return 0;
        }

        try {
            return createOceanicPlanetWithSubtype(source, planetName, "earthlike");
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to create planet: " + e.getMessage()));
            return 0;
        }
    }

    // OCEANIC PLANET CREATION
    private static int createOceanicPlanetWithSubtype(ServerCommandSource source, String planetName, String subtype) {
        OceanicConfig config = switch (subtype.toLowerCase()) {
            case "earthlike", "earth", "standard" -> {
                source.sendFeedback(() -> Text.literal("Creating Earth-like oceanic planet..."), true);
                yield OceanicPlanetFactory.createEarthLikeConfig(planetName);
            }
            case "tropical", "warm" -> {
                source.sendFeedback(() -> Text.literal("Creating tropical oceanic planet..."), true);
                yield OceanicPlanetFactory.createTropicalOceanConfig(planetName);
            }
            case "archipelago", "islands" -> {
                source.sendFeedback(() -> Text.literal("Creating archipelago world..."), true);
                yield OceanicPlanetFactory.createArchipelagoConfig(planetName);
            }
            case "deepocean", "deep", "abyssal" -> {
                source.sendFeedback(() -> Text.literal("Creating deep ocean world..."), true);
                yield OceanicPlanetFactory.createDeepOceanConfig(planetName);
            }
            default -> {
                source.sendFeedback(() -> Text.literal("Unknown oceanic subtype '" + subtype +
                        "', using Earth-like..."), true);
                yield OceanicPlanetFactory.createEarthLikeConfig(planetName);
            }
        };

        OceanicModel model = OceanicPlanetFactory.createOceanicModel(config);
        RegistryKey<World> dimensionKey = PlanetDimensionManager.createOceanicPlanet(source.getServer(), model);

        source.sendFeedback(() -> Text.literal("✅ Created oceanic planet: " + planetName), true);
        source.sendFeedback(() -> Text.literal("Ocean Coverage: " +
                String.format("%.1f", config.getOceanCoverage() * 100) + "%"), false);
        source.sendFeedback(() -> Text.literal("Ocean Type: " + config.getDominantOceanType()), false);
        source.sendFeedback(() -> Text.literal("Use '/terradyne visit " + planetName + "' to explore!"), false);

        return 1;
    }

    // ROCKY PLANET CREATION
    private static int createRockyPlanetWithSubtype(ServerCommandSource source, String planetName, String subtype) {
        RockyConfig config = switch (subtype.toLowerCase()) {
            case "moonlike", "moon", "standard" -> {
                source.sendFeedback(() -> Text.literal("Creating Moon-like rocky planet..."), true);
                yield RockyPlanetFactory.createMoonLikeConfig(planetName);
            }
            case "asteroid", "metal" -> {
                source.sendFeedback(() -> Text.literal("Creating metallic asteroid world..."), true);
                yield RockyPlanetFactory.createAsteroidConfig(planetName);
            }
            case "barren", "dead" -> {
                source.sendFeedback(() -> Text.literal("Creating barren rocky world..."), true);
                yield RockyPlanetFactory.createBarrenConfig(planetName);
            }
            case "shattered", "fractured", "broken" -> {
                source.sendFeedback(() -> Text.literal("Creating shattered rocky world..."), true);
                yield RockyPlanetFactory.createShatteredConfig(planetName);
            }
            default -> {
                source.sendFeedback(() -> Text.literal("Unknown rocky subtype '" + subtype +
                        "', using Moon-like..."), true);
                yield RockyPlanetFactory.createMoonLikeConfig(planetName);
            }
        };

        RockyModel model = RockyPlanetFactory.createRockyModel(config);
        RegistryKey<World> dimensionKey = PlanetDimensionManager.createRockyPlanet(source.getServer(), model);

        source.sendFeedback(() -> Text.literal("✅ Created rocky planet: " + planetName), true);
        source.sendFeedback(() -> Text.literal("Surface Type: " + config.getDominantSurface()), false);
        source.sendFeedback(() -> Text.literal("Crater Density: " + String.format("%.1f", config.getCraterDensity())), false);
        source.sendFeedback(() -> Text.literal("Gravity: " + String.format("%.2f", model.getGravity()) + "g"), false);
        source.sendFeedback(() -> Text.literal("Use '/terradyne visit " + planetName + "' to explore!"), false);

        return 1;
    }

    // DESERT PLANET CREATION
    private static int createDesertPlanetWithSubtype(ServerCommandSource source, String planetName,
                                                     PlanetType planetType, String subtype) {
        DesertConfig config = switch (subtype.toLowerCase()) {
            case "standard", "normal" -> {
                source.sendFeedback(() -> Text.literal("Creating standard desert planet..."), true);
                yield DesertPlanetFactory.createDefaultDesertConfig(planetName);
            }
            case "hot", "hothouse", "extreme" -> {
                source.sendFeedback(() -> Text.literal("Creating hothouse desert planet..."), true);
                yield DesertPlanetFactory.createHotDesertConfig(planetName);
            }
            case "rocky", "stone" -> {
                source.sendFeedback(() -> Text.literal("Creating rocky desert planet..."), true);
                yield DesertPlanetFactory.createRockyDesertConfig(planetName);
            }
            default -> {
                if (planetType == PlanetType.HOTHOUSE) {
                    source.sendFeedback(() -> Text.literal("Creating hothouse desert planet..."), true);
                    yield DesertPlanetFactory.createHotDesertConfig(planetName);
                } else {
                    source.sendFeedback(() -> Text.literal("Creating standard desert planet..."), true);
                    yield DesertPlanetFactory.createDefaultDesertConfig(planetName);
                }
            }
        };

        DesertModel model = DesertPlanetFactory.createDesertModel(config);
        RegistryKey<World> dimensionKey = PlanetDimensionManager.createDesertPlanet(source.getServer(), model);

        source.sendFeedback(() -> Text.literal("✅ Created desert planet: " + planetName), true);
        source.sendFeedback(() -> Text.literal("Temperature: " +
                String.format("%.1f", config.getSurfaceTemperature()) + "°C"), false);
        source.sendFeedback(() -> Text.literal("Sand Coverage: " +
                String.format("%.1f", config.getSandDensity() * 100) + "%"), false);
        source.sendFeedback(() -> Text.literal("Use '/terradyne visit " + planetName + "' to explore!"), false);

        return 1;
    }

    // VISIT PLANET
    private static int visitPlanet(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String planetName = StringArgumentType.getString(context, "name");
        ServerCommandSource source = context.getSource();

        if (!PlanetDimensionManager.planetExists(planetName)) {
            source.sendError(Text.literal("Planet '" + planetName + "' does not exist!"));
            source.sendFeedback(() -> Text.literal("Use '/terradyne list' to see available planets"), false);
            return 0;
        }

        try {
            PlanetDimensionManager.teleportToPlanet(source.getPlayerOrThrow(), planetName);

            PlanetType type = PlanetDimensionManager.getPlanetType(planetName);
            source.sendFeedback(() -> Text.literal("✅ Welcome to " + planetName +
                    " (" + (type != null ? type.getDisplayName() : "Unknown") + ")!"), true);

            return 1;

        } catch (Exception e) {
            source.sendError(Text.literal("Failed to teleport: " + e.getMessage()));
            return 0;
        }
    }

    // LIST ALL PLANETS
    private static int listPlanets(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String planetList = PlanetDimensionManager.listPlanets();

        source.sendFeedback(() -> Text.literal("=== Created Planets ==="), false);
        if (planetList.equals("No planets created yet")) {
            source.sendFeedback(() -> Text.literal("§7No planets created yet"), false);
            source.sendFeedback(() -> Text.literal("§7Use '/terradyne create <name> <type>' to create one!"), false);
        } else {
            source.sendFeedback(() -> Text.literal(planetList), false);
        }

        return 1;
    }

    // SHOW DETAILED PLANET INFO
    private static int showPlanetInfo(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String planetName = StringArgumentType.getString(context, "name");
        ServerCommandSource source = context.getSource();

        if (!PlanetDimensionManager.planetExists(planetName)) {
            source.sendError(Text.literal("Planet '" + planetName + "' does not exist!"));
            return 0;
        }

        PlanetType type = PlanetDimensionManager.getPlanetType(planetName);

        source.sendFeedback(() -> Text.literal("=== Planet Info: " + planetName + " ==="), false);
        source.sendFeedback(() -> Text.literal("Type: " + (type != null ? type.getDisplayName() : "Unknown")), false);

        // Show type-specific information
        if (type == PlanetType.OCEANIC) {
            showOceanicPlanetInfo(source, planetName);
        } else if (type == PlanetType.DESERT || type == PlanetType.HOTHOUSE) {
            showDesertPlanetInfo(source, planetName);
        } else if (type == PlanetType.ROCKY) {
            showRockyPlanetInfo(source, planetName);
        }

        source.sendFeedback(() -> Text.literal(""), false);
        source.sendFeedback(() -> Text.literal("§7Use '/terradyne visit " + planetName + "' to explore"), false);

        return 1;
    }

    // OCEANIC PLANET INFO DISPLAY
    private static void showOceanicPlanetInfo(ServerCommandSource source, String planetName) {
        OceanicModel model = PlanetDimensionManager.getOceanicModel(planetName);
        if (model != null) {
            OceanicConfig config = model.getConfig();
            source.sendFeedback(() -> Text.literal("§9=== Oceanic Characteristics ==="), false);
            source.sendFeedback(() -> Text.literal("Ocean Coverage: " +
                    String.format("%.1f", config.getOceanCoverage() * 100) + "%"), false);
            source.sendFeedback(() -> Text.literal("Ocean Type: " + config.getDominantOceanType()), false);
            source.sendFeedback(() -> Text.literal("Average Depth: " +
                    String.format("%.1f", config.getAverageOceanDepth()) + " blocks"), false);
            source.sendFeedback(() -> Text.literal("Continents: " + config.getContinentCount()), false);
            source.sendFeedback(() -> Text.literal("Atmospheric Humidity: " +
                    String.format("%.1f", config.getAtmosphericHumidity() * 100) + "%"), false);
            source.sendFeedback(() -> Text.literal("Has Ice Caps: " + (config.hasIceCaps() ? "Yes" : "No")), false);
            source.sendFeedback(() -> Text.literal("Gravity: " + String.format("%.2f", model.getGravity()) + "g"), false);
            source.sendFeedback(() -> Text.literal("Biodiversity Index: " +
                    String.format("%.2f", model.getBiodiversityIndex())), false);
            source.sendFeedback(() -> Text.literal("Storm Frequency: " +
                    String.format("%.2f", model.getStormFrequency())), false);
        }
    }

    // DESERT PLANET INFO DISPLAY
    private static void showDesertPlanetInfo(ServerCommandSource source, String planetName) {
        DesertModel model = PlanetDimensionManager.getDesertModel(planetName);
        if (model != null) {
            DesertConfig config = model.getConfig();
            source.sendFeedback(() -> Text.literal("§6=== Desert Characteristics ==="), false);
            source.sendFeedback(() -> Text.literal("Temperature: " +
                    String.format("%.1f", config.getSurfaceTemperature()) + "°C"), false);
            source.sendFeedback(() -> Text.literal("Humidity: " +
                    String.format("%.1f", config.getHumidity() * 100) + "%"), false);
            source.sendFeedback(() -> Text.literal("Sand Density: " +
                    String.format("%.1f", config.getSandDensity() * 100) + "%"), false);
            source.sendFeedback(() -> Text.literal("Has Dunes: " + (config.hasDunes() ? "Yes" : "No")), false);
            source.sendFeedback(() -> Text.literal("Wind Strength: " +
                    String.format("%.1f", config.getWindStrength())), false);
            source.sendFeedback(() -> Text.literal("Rock Type: " + config.getDominantRock().name()), false);
            source.sendFeedback(() -> Text.literal("Gravity: " + String.format("%.2f", model.getGravity()) + "g"), false);
            source.sendFeedback(() -> Text.literal("Erosion Rate: " +
                    String.format("%.2f", model.getErosionRate())), false);
        }
    }

    // ROCKY PLANET INFO DISPLAY
    private static void showRockyPlanetInfo(ServerCommandSource source, String planetName) {
        RockyModel model = PlanetDimensionManager.getRockyModel(planetName);
        if (model != null) {
            RockyConfig config = model.getConfig();
            source.sendFeedback(() -> Text.literal("§8=== Rocky Characteristics ==="), false);
            source.sendFeedback(() -> Text.literal("Surface Type: " + config.getDominantSurface()), false);
            source.sendFeedback(() -> Text.literal("Atmospheric Density: " +
                    String.format("%.3f", config.getAtmosphericDensity())), false);
            source.sendFeedback(() -> Text.literal("Crater Density: " +
                    String.format("%.2f", config.getCraterDensity())), false);
            source.sendFeedback(() -> Text.literal("Regolith Depth: " +
                    String.format("%.1f", config.getRegolithDepth()) + " blocks"), false);
            source.sendFeedback(() -> Text.literal("Exposed Bedrock: " +
                    String.format("%.1f", config.getExposedBedrockRatio() * 100) + "%"), false);
            source.sendFeedback(() -> Text.literal("Geological Activity: " + config.getActivity()), false);
            source.sendFeedback(() -> Text.literal("Mineral Richness: " +
                    String.format("%.2f", config.getMineralRichness())), false);
            source.sendFeedback(() -> Text.literal("Temperature Variation: " +
                    String.format("%.1f", config.getTemperatureVariation()) + "°C"), false);
            source.sendFeedback(() -> Text.literal("Gravity: " + String.format("%.2f", model.getGravity()) + "g"), false);
            source.sendFeedback(() -> Text.literal("Surface Roughness: " +
                    String.format("%.2f", model.getSurfaceRoughness())), false);
            source.sendFeedback(() -> Text.literal("Has Caverns: " + (config.hasSubsurfaceCaverns() ? "Yes" : "No")), false);
        }
    }

    // LIST AVAILABLE PLANET TYPES
    private static int listPlanetTypes(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        source.sendFeedback(() -> Text.literal("=== Planet Classification System ==="), false);

        source.sendFeedback(() -> Text.literal("§a✅ Currently Implemented:"), false);
        for (PlanetType type : PlanetType.getImplementedTypes()) {
            source.sendFeedback(() -> Text.literal("§a• " + type.getDisplayName()), false);
            source.sendFeedback(() -> Text.literal("  §8" + type.getDescription()), false);

            // Show subtypes
            if (type == PlanetType.OCEANIC) {
                source.sendFeedback(() -> Text.literal("  §bSubtypes: earthlike, tropical, archipelago, deepocean"), false);
            } else if (type == PlanetType.DESERT) {
                source.sendFeedback(() -> Text.literal("  §bSubtypes: standard, hot, rocky"), false);
            } else if (type == PlanetType.ROCKY) {
                source.sendFeedback(() -> Text.literal("  §bSubtypes: moonlike, asteroid, barren, shattered"), false);
            }
        }

        source.sendFeedback(() -> Text.literal(""), false);
        source.sendFeedback(() -> Text.literal("§7🔜 Coming Soon:"), false);
        for (PlanetType type : PlanetType.getMainTypes()) {
            if (!type.isImplemented()) {
                source.sendFeedback(() -> Text.literal("§7• " + type.getDisplayName()), false);
            }
        }

        source.sendFeedback(() -> Text.literal(""), false);
        source.sendFeedback(() -> Text.literal("§7Usage Examples:"), false);
        source.sendFeedback(() -> Text.literal("§f/terradyne create Earth oceanic earthlike"), false);
        source.sendFeedback(() -> Text.literal("§f/terradyne create Moon rocky moonlike"), false);
        source.sendFeedback(() -> Text.literal("§f/terradyne create Tatooine desert hot"), false);
        source.sendFeedback(() -> Text.literal("§f/terradyne create Ceres rocky asteroid"), false);

        return 1;
    }

    // SHOW HELP
    private static int showHelp(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        source.sendFeedback(() -> Text.literal("=== Terradyne Commands ==="), false);
        source.sendFeedback(() -> Text.literal("§6/terradyne create <name> [type] [subtype]"), false);
        source.sendFeedback(() -> Text.literal("  §7Create a new planet"), false);
        source.sendFeedback(() -> Text.literal("  §7Types: oceanic, desert, rocky"), false);
        source.sendFeedback(() -> Text.literal(""), false);
        source.sendFeedback(() -> Text.literal("§6/terradyne visit <name>"), false);
        source.sendFeedback(() -> Text.literal("  §7Travel to a planet"), false);
        source.sendFeedback(() -> Text.literal(""), false);
        source.sendFeedback(() -> Text.literal("§6/terradyne list"), false);
        source.sendFeedback(() -> Text.literal("  §7List all created planets"), false);
        source.sendFeedback(() -> Text.literal(""), false);
        source.sendFeedback(() -> Text.literal("§6/terradyne info <name>"), false);
        source.sendFeedback(() -> Text.literal("  §7Show detailed planet information"), false);
        source.sendFeedback(() -> Text.literal(""), false);
        source.sendFeedback(() -> Text.literal("§6/terradyne types"), false);
        source.sendFeedback(() -> Text.literal("  §7List available planet types and subtypes"), false);
        source.sendFeedback(() -> Text.literal(""), false);
        source.sendFeedback(() -> Text.literal("§7Quick Examples:"), false);
        source.sendFeedback(() -> Text.literal("§f/terradyne create Earth oceanic"), false);
        source.sendFeedback(() -> Text.literal("§f/terradyne create Luna rocky moonlike"), false);
        source.sendFeedback(() -> Text.literal("§f/terradyne create Mars desert"), false);

        return 1;
    }
}