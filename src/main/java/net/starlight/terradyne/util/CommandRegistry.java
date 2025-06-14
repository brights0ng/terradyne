package net.starlight.terradyne.util;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import net.starlight.terradyne.Terradyne;
import net.starlight.terradyne.planet.dimension.PlanetDimensionManager;
import net.starlight.terradyne.planet.factory.PhysicsPlanetFactory;
import net.starlight.terradyne.planet.physics.IPlanetModel;
import net.starlight.terradyne.planet.physics.PlanetPhysicsConfig;
import net.starlight.terradyne.planet.physics.PlanetPhysicsModel;

/**
 * Command registry for Terradyne
 * Simplified for physics-based planets
 */
public class CommandRegistry {
    
    public static void init(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("terradyne")
                
                // Create physics planet
                .then(CommandManager.literal("create")
                        .then(CommandManager.argument("name", StringArgumentType.string())
                                .then(CommandManager.argument("preset", StringArgumentType.string())
                                        .suggests((context, builder) -> {
                                            builder.suggest("test");
                                            builder.suggest("earth");
                                            builder.suggest("mars");
                                            builder.suggest("venus");
                                            builder.suggest("debug");
                                            return builder.buildFuture();
                                        })
                                        .executes(CommandRegistry::createPhysicsPlanet))
                                .executes(context -> createPhysicsPlanet(context, "test"))))
                
                // Visit planet
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
                
                // List planets
                .then(CommandManager.literal("list")
                        .executes(CommandRegistry::listPlanets))
                
                // Planet info
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
                
                // Help
                .then(CommandManager.literal("help")
                        .executes(CommandRegistry::showHelp)));
    }
    
    /**
     * Create a physics-based planet
     */
    private static int createPhysicsPlanet(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String preset = StringArgumentType.getString(context, "preset");
        return createPhysicsPlanet(context, preset);
    }
    
    private static int createPhysicsPlanet(CommandContext<ServerCommandSource> context, String preset) throws CommandSyntaxException {
        String planetName = StringArgumentType.getString(context, "name");
        ServerCommandSource source = context.getSource();
        
        if (PlanetDimensionManager.planetExists(planetName)) {
            source.sendError(Text.literal("Planet '" + planetName + "' already exists!"));
            return 0;
        }
        
        try {
            // Create config based on preset
            PlanetPhysicsConfig config = switch (preset.toLowerCase()) {
                case "test" -> {
                    source.sendFeedback(() -> Text.literal("Creating tectonic test planet..."), true);
                    yield PhysicsPlanetFactory.createTectonicTestConfig(planetName);
                }
                case "earth" -> {
                    source.sendFeedback(() -> Text.literal("Creating Earth-like planet..."), true);
                    yield PhysicsPlanetFactory.createEarthLikeConfig(planetName);
                }
                case "mars" -> {
                    source.sendFeedback(() -> Text.literal("Creating Mars-like planet..."), true);
                    yield PhysicsPlanetFactory.createMarsLikeConfig(planetName);
                }
                case "venus" -> {
                    source.sendFeedback(() -> Text.literal("Creating Venus-like planet..."), true);
                    yield PhysicsPlanetFactory.createVenusLikeConfig(planetName);
                }
                case "debug" -> {
                    source.sendFeedback(() -> Text.literal("Creating debug planet with visible plates..."), true);
                    yield PhysicsPlanetFactory.createDebugPlanetConfig(planetName);
                }
                default -> {
                    source.sendFeedback(() -> Text.literal("Unknown preset '" + preset + "', using test..."), true);
                    yield PhysicsPlanetFactory.createTectonicTestConfig(planetName);
                }
            };
            
            // Create model and planet
            PlanetPhysicsModel model = PhysicsPlanetFactory.createPhysicsModel(config);
            RegistryKey<World> worldKey = PlanetDimensionManager.createPhysicsPlanet(source.getServer(), model);
            
            // Send feedback
            source.sendFeedback(() -> Text.literal("✅ Created physics planet: " + planetName), true);
            source.sendFeedback(() -> Text.literal("Type: " + model.getType().getDisplayName() + " (emergent from physics)"), false);
            source.sendFeedback(() -> Text.literal("Tectonic Plates: " + model.getTectonicGenerator().getPlates().size()), false);
            source.sendFeedback(() -> Text.literal("Continental Noise: Enabled (multi-octave)"), false);
            source.sendFeedback(() -> Text.literal("Gravity: " + String.format("%.2f", config.getGravity()) + "g"), false);
            source.sendFeedback(() -> Text.literal("Temperature: " + 
                    String.format("%.1f", config.getSurfaceTempMin()) + "°C to " +
                    String.format("%.1f", config.getSurfaceTempMax()) + "°C"), false);
            source.sendFeedback(() -> Text.literal("Use '/terradyne visit " + planetName + "' to explore!"), false);
            
            return 1;
            
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to create planet: " + e.getMessage()));
            Terradyne.LOGGER.error("Planet creation failed", e);
            return 0;
        }
    }
    
    /**
     * Visit a planet
     */
    private static int visitPlanet(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String planetName = StringArgumentType.getString(context, "name");
        ServerCommandSource source = context.getSource();
        
        if (!PlanetDimensionManager.planetExists(planetName)) {
            source.sendError(Text.literal("Planet '" + planetName + "' does not exist!"));
            return 0;
        }
        
        try {
            PlanetDimensionManager.teleportToPlanet(source.getPlayerOrThrow(), planetName);
            
            IPlanetModel model = PlanetDimensionManager.getPlanetModel(planetName);
            source.sendFeedback(() -> Text.literal("✅ Welcome to " + planetName + 
                    " (" + (model != null ? model.getType().getDisplayName() : "Unknown") + ")!"), true);
            
            return 1;
            
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to teleport: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * List all planets
     */
    private static int listPlanets(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String planetList = PlanetDimensionManager.listPlanets();
        
        source.sendFeedback(() -> Text.literal("=== Created Planets ==="), false);
        if (planetList.equals("No planets created yet")) {
            source.sendFeedback(() -> Text.literal("§7No planets created yet"), false);
            source.sendFeedback(() -> Text.literal("§7Use '/terradyne create <name> <preset>' to create one!"), false);
        } else {
            source.sendFeedback(() -> Text.literal(planetList), false);
        }
        
        return 1;
    }
    
    /**
     * Show planet info
     */
    private static int showPlanetInfo(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String planetName = StringArgumentType.getString(context, "name");
        ServerCommandSource source = context.getSource();
        
        if (!PlanetDimensionManager.planetExists(planetName)) {
            source.sendError(Text.literal("Planet '" + planetName + "' does not exist!"));
            return 0;
        }
        
        IPlanetModel model = PlanetDimensionManager.getPlanetModel(planetName);
        if (model == null) {
            source.sendError(Text.literal("Could not find model for planet '" + planetName + "'"));
            return 0;
        }
        
        source.sendFeedback(() -> Text.literal("=== Planet Info: " + planetName + " ==="), false);
        source.sendFeedback(() -> Text.literal("Type: " + model.getType().getDisplayName() + " (emergent)"), false);
        
        if (model instanceof PlanetPhysicsModel physicsModel) {
            PlanetPhysicsConfig config = (PlanetPhysicsConfig) physicsModel.getConfig();
            
            source.sendFeedback(() -> Text.literal("§b=== Physical Parameters ==="), false);
            source.sendFeedback(() -> Text.literal("Tectonic Scale: " + config.getTectonicScale()), false);
            source.sendFeedback(() -> Text.literal("Tectonic Activity: " + config.getTectonicActivity()), false);
            source.sendFeedback(() -> Text.literal("Water Coverage: " + (config.getWaterHeight() * 100) + "%"), false);
            source.sendFeedback(() -> Text.literal("Atmosphere: " + config.getAtmosphereDensity() + " Earth atmospheres"), false);
            source.sendFeedback(() -> Text.literal("Distance from Sun: " + config.getDistanceFromSun() + " x100M km"), false);
            source.sendFeedback(() -> Text.literal("Circumference: " + config.getPlanetCircumference() + " x10k km"), false);
            source.sendFeedback(() -> Text.literal("Day Length: " + config.getRotationPeriod() + " Earth days"), false);
            
            source.sendFeedback(() -> Text.literal("§e=== Calculated Properties ==="), false);
            source.sendFeedback(() -> Text.literal("Gravity: " + String.format("%.2f", config.getGravity()) + "g"), false);
            source.sendFeedback(() -> Text.literal("Temperature Range: " + 
                    String.format("%.1f", config.getSurfaceTempMin()) + "°C to " +
                    String.format("%.1f", config.getSurfaceTempMax()) + "°C"), false);
            
            var plates = physicsModel.getTectonicGenerator().getPlates();
            source.sendFeedback(() -> Text.literal("Tectonic Plates: " + plates.size()), false);
        }
        
        return 1;
    }
    
    /**
     * Show help
     */
    private static int showHelp(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        source.sendFeedback(() -> Text.literal("=== Terradyne Physics Commands ==="), false);
        source.sendFeedback(() -> Text.literal(""), false);
        
        source.sendFeedback(() -> Text.literal("§6/terradyne create <name> [preset]"), false);
        source.sendFeedback(() -> Text.literal("  §7Create a physics-based planet"), false);
        source.sendFeedback(() -> Text.literal("  §7Presets: test, earth, mars, venus"), false);
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
        
        source.sendFeedback(() -> Text.literal("§7Example Commands:"), false);
        source.sendFeedback(() -> Text.literal("§f/terradyne create TestWorld test"), false);
        source.sendFeedback(() -> Text.literal("§f/terradyne create Earth earth"), false);
        source.sendFeedback(() -> Text.literal("§f/terradyne create RedPlanet mars"), false);
        
        return 1;
    }
}