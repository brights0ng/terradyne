package net.starlight.terradyne.planet.world;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;
import net.starlight.terradyne.Terradyne;
import net.starlight.terradyne.planet.chunk.UniversalChunkGenerator;
import net.starlight.terradyne.planet.config.ExistingPlanetRegistry;
import net.starlight.terradyne.planet.config.PlanetConfigLoader;
import net.starlight.terradyne.planet.dimension.DimensionTypeFactory;
import net.starlight.terradyne.planet.dimension.ModDimensionTypes;
import net.starlight.terradyne.planet.dimension.PlanetDimensionManager;
import net.starlight.terradyne.planet.physics.PlanetConfig;
import net.starlight.terradyne.planet.physics.PlanetModel;
import net.minecraft.server.world.ServerWorld;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages planet worlds per server instance
 * Handles new world registration and existing world validation
 */
public class WorldPlanetManager {

    // Per-server planet tracking
    private static final Map<MinecraftServer, ExistingPlanetRegistry> SERVER_REGISTRIES = new ConcurrentHashMap<>();
    private static final Map<MinecraftServer, Map<String, PlanetModel>> SERVER_PLANET_MODELS = new ConcurrentHashMap<>();

    /**
     * Attempt to modify level.dat before Minecraft loads dimensions
     * Called from world loading event (earlier than SERVER_STARTING)
     */
    public static void attemptEarlyLevelDatModification(MinecraftServer server) {
        try {
            Terradyne.LOGGER.info("Attempting early level.dat modification...");

            // Check if this is a new world by checking if we've already processed it
            Path worldDir = server.getSavePath(net.minecraft.util.WorldSavePath.ROOT);
            Path terradyneMarker = worldDir.resolve("terradyne").resolve(".dimensions_added");

            if (Files.exists(terradyneMarker)) {
                Terradyne.LOGGER.info("Terradyne dimensions already added to this world (marker file exists)");
                return;
            }

            Terradyne.LOGGER.info("New world detected - attempting immediate level.dat modification");

            // Load planet configurations
            Map<String, PlanetConfig> planetConfigs = PlanetConfigLoader.loadAllPlanetConfigs(server);

            if (planetConfigs.isEmpty()) {
                Terradyne.LOGGER.info("No planet configurations found");
                return;
            }

            // Try to modify level.dat immediately
            Path levelDatPath = worldDir.resolve("level.dat");

            if (!Files.exists(levelDatPath)) {
                Terradyne.LOGGER.warn("level.dat not found during early modification attempt");
                return;
            }

            // Read, modify, and write level.dat
            NbtCompound levelData = net.minecraft.nbt.NbtIo.readCompressed(levelDatPath.toFile());
            NbtCompound data = levelData.getCompound("Data");

            if (data.contains("WorldGenSettings")) {
                NbtCompound worldGenSettings = data.getCompound("WorldGenSettings");

                if (worldGenSettings.contains("dimensions")) {
                    NbtCompound dimensions = worldGenSettings.getCompound("dimensions");

                    // Add our dimensions
                    addDimensionsToLevelDat(dimensions, planetConfigs);

                    // Write back immediately
                    net.minecraft.nbt.NbtIo.writeCompressed(levelData, levelDatPath.toFile());

                    // Create marker file to prevent re-processing
                    Files.createDirectories(worldDir.resolve("terradyne"));
                    Files.writeString(terradyneMarker, "Dimensions added at: " + java.time.LocalDateTime.now());

                    Terradyne.LOGGER.info("✅ Early level.dat modification completed - {} dimensions added", planetConfigs.size());

                    // Create registry and register planets
                    ExistingPlanetRegistry registry = new ExistingPlanetRegistry(server);
                    for (Map.Entry<String, PlanetConfig> entry : planetConfigs.entrySet()) {
                        String dimensionId = Terradyne.MOD_ID + ":" + entry.getKey().toLowerCase().replace(" ", "_");
                        String configHash = ExistingPlanetRegistry.generateConfigHash(entry.getValue());
                        registry.registerGeneratedPlanet(entry.getValue().getPlanetName(), dimensionId, configHash);
                    }

                } else {
                    Terradyne.LOGGER.warn("WorldGenSettings.dimensions not found during early modification");
                }
            } else {
                Terradyne.LOGGER.warn("WorldGenSettings not found during early modification");
            }

        } catch (Exception e) {
            Terradyne.LOGGER.error("Failed early level.dat modification", e);
        }
    }

    /**
     * Add dimensions to level.dat (copied from LevelDatModifier for early use)
     */
    private static void addDimensionsToLevelDat(NbtCompound dimensions, Map<String, PlanetConfig> planetConfigs) {
        for (Map.Entry<String, PlanetConfig> entry : planetConfigs.entrySet()) {
            String planetName = entry.getKey().toLowerCase().replace(" ", "_");
            String dimensionKey = Terradyne.MOD_ID + ":" + planetName;

            Terradyne.LOGGER.info("Adding dimension to level.dat (early): {}", dimensionKey);

            NbtCompound dimension = new NbtCompound();

            // Use minecraft:overworld dimension type (exactly like overworld)
            dimension.putString("type", "minecraft:overworld");

            // Generator - copy overworld's noise generator structure
            NbtCompound generator = new NbtCompound();
            generator.putString("type", "minecraft:noise");

            // Biome source - simple fixed biome
            NbtCompound biomeSource = new NbtCompound();
            biomeSource.putString("type", "minecraft:fixed");
            biomeSource.putString("biome", "minecraft:plains");
            generator.put("biome_source", biomeSource);

            // Settings - use overworld settings
            generator.putString("settings", "minecraft:overworld");

            // Seed
            generator.putLong("seed", entry.getValue().getSeed());

            dimension.put("generator", generator);

            // Add to dimensions compound
            dimensions.put(dimensionKey, dimension);
        }
    }

    /**
     * Initialize planet management for a server instance
     * Called during SERVER_STARTING - now checks if early modification already happened
     */
    public static void initializeForServer(MinecraftServer server) {
        Terradyne.LOGGER.info("Initializing planet management for server...");

        try {
            // Create registry for this server
            ExistingPlanetRegistry registry = new ExistingPlanetRegistry(server);
            SERVER_REGISTRIES.put(server, registry);
            SERVER_PLANET_MODELS.put(server, new ConcurrentHashMap<>());

            // Check if early modification already happened
            Path worldDir = server.getSavePath(net.minecraft.util.WorldSavePath.ROOT);
            Path terradyneMarker = worldDir.resolve("terradyne").resolve(".dimensions_added");

            if (Files.exists(terradyneMarker)) {
                Terradyne.LOGGER.info("Early level.dat modification already completed");

                // Load existing planet configs to populate our registry
                Map<String, PlanetConfig> planetConfigs = PlanetConfigLoader.loadAllPlanetConfigs(server);
                for (Map.Entry<String, PlanetConfig> entry : planetConfigs.entrySet()) {
                    String dimensionId = Terradyne.MOD_ID + ":" + entry.getKey().toLowerCase().replace(" ", "_");
                    String configHash = ExistingPlanetRegistry.generateConfigHash(entry.getValue());

                    // Only register if not already registered
                    if (!registry.isPlanetGenerated(entry.getKey())) {
                        registry.registerGeneratedPlanet(entry.getValue().getPlanetName(), dimensionId, configHash);
                    }
                }
            } else {
                // Fallback: try the original approach if early modification didn't happen
                Terradyne.LOGGER.info("Early modification didn't happen - trying fallback approach");
                LevelDatModifier.modifyLevelDatIfNeeded(server, registry);
            }

        } catch (Exception e) {
            Terradyne.LOGGER.error("Failed to initialize planet management", e);
        }
    }

    /**
     * Validate planets after server startup
     * Called during SERVER_STARTED
     */
    public static void validateAfterStartup(MinecraftServer server) {
        Terradyne.LOGGER.info("Validating planet accessibility...");

        ExistingPlanetRegistry registry = SERVER_REGISTRIES.get(server);
        if (registry == null) {
            Terradyne.LOGGER.warn("No registry found for server during validation");
            return;
        }

        // Validate that registered planets are actually accessible
        validatePlanetAccessibility(server, registry);

        // Load planet models for accessible planets
        loadPlanetModels(server, registry);

        // IMPORTANT: Replace chunk generators for our dimensions
        replaceChunkGenerators(server, registry);

        Terradyne.LOGGER.info("✅ Planet validation completed");
    }

    /**
     * Check if this is a new world that needs dimension registration
     */
    private static boolean isNewWorld(MinecraftServer server) {
        // Check if our planet registry file exists
        Path registryPath = getTerradyneConfigDirectory(server).resolve("planet_registry.json");
        boolean hasExistingRegistry = Files.exists(registryPath);

        Terradyne.LOGGER.info("World status: hasExistingRegistry={}", hasExistingRegistry);
        return !hasExistingRegistry;
    }

    /**
     * Create biome source for planet
     */
    private static BiomeSource createPlanetBiomeSource(MinecraftServer server, PlanetModel planetModel) {
        // Use the same biome source creation logic from the old PlanetDimensionManager
        return net.starlight.terradyne.planet.dimension.PlanetDimensionManager.createPlanetBiomeSource(server, planetModel);
    }

    /**
     * Validate that registered planets are actually accessible
     */
    private static void validatePlanetAccessibility(MinecraftServer server, ExistingPlanetRegistry registry) {
        var generatedPlanetNames = registry.getGeneratedPlanetNames();
        Terradyne.LOGGER.info("Validating accessibility for {} registered planets", generatedPlanetNames.size());

        if (generatedPlanetNames.isEmpty()) {
            Terradyne.LOGGER.warn("No planets found in registry during validation");
            return;
        }

        // Debug: List all dimensions that Minecraft actually loaded
        Terradyne.LOGGER.info("=== MINECRAFT LOADED DIMENSIONS ===");
        for (ServerWorld world : server.getWorlds()) {
            Identifier dimensionId = world.getRegistryKey().getValue();
            Terradyne.LOGGER.info("  - {} ({})", dimensionId, world.getClass().getSimpleName());
        }
        Terradyne.LOGGER.info("Total dimensions loaded by Minecraft: {}", server.getWorlds().toString());

        for (String planetName : generatedPlanetNames) {
            var entry = registry.getPlanetEntry(planetName);
            if (entry != null) {
                try {
                    Identifier dimensionId = new Identifier(entry.dimensionId);
                    RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, dimensionId);

                    if (server.getWorld(worldKey) != null) {
                        Terradyne.LOGGER.info("✅ Planet '{}' is accessible", planetName);
                    } else {
                        Terradyne.LOGGER.warn("❌ Planet '{}' registered but not accessible (dimension: {})", planetName, entry.dimensionId);
                        Terradyne.LOGGER.warn("    Expected registry key: {}", worldKey.getValue());
                    }
                } catch (Exception e) {
                    Terradyne.LOGGER.warn("❌ Failed to validate planet '{}': {}", planetName, e.getMessage());
                }
            } else {
                Terradyne.LOGGER.warn("❌ Planet '{}' has no registry entry", planetName);
            }
        }
    }

    /**
     * Load planet models for accessible planets
     */
    private static void loadPlanetModels(MinecraftServer server, ExistingPlanetRegistry registry) {
        // Load planet configs
        Map<String, PlanetConfig> planetConfigs = PlanetConfigLoader.loadAllPlanetConfigs(server);
        Map<String, PlanetModel> planetModels = SERVER_PLANET_MODELS.get(server);

        for (Map.Entry<String, PlanetConfig> entry : planetConfigs.entrySet()) {
            String planetName = entry.getKey();

            if (registry.isPlanetGenerated(planetName)) {
                try {
                    PlanetModel planetModel = new PlanetModel(entry.getValue());
                    planetModels.put(planetName, planetModel);
                    Terradyne.LOGGER.debug("Loaded planet model: {}", planetName);
                } catch (Exception e) {
                    Terradyne.LOGGER.error("Failed to load planet model for '{}': {}", planetName, e.getMessage());
                }
            }
        }
    }

    /**
     * Replace chunk generators for our dimensions with physics-based generators
     * This happens after Minecraft has loaded the dimensions with placeholder generators
     */
    private static void replaceChunkGenerators(MinecraftServer server, ExistingPlanetRegistry registry) {
        Terradyne.LOGGER.info("Replacing chunk generators with physics-based generators...");

        Map<String, PlanetModel> planetModels = SERVER_PLANET_MODELS.get(server);
        if (planetModels == null) {
            Terradyne.LOGGER.warn("No planet models available for chunk generator replacement");
            return;
        }

        for (String planetName : registry.getGeneratedPlanetNames()) {
            try {
                var entry = registry.getPlanetEntry(planetName);
                if (entry == null) continue;

                // Get the dimension
                Identifier dimensionId = new Identifier(entry.dimensionId);
                RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, dimensionId);
                ServerWorld world = server.getWorld(worldKey);

                if (world == null) {
                    Terradyne.LOGGER.warn("Cannot replace chunk generator for '{}' - world not found", planetName);
                    continue;
                }

                // Get the planet model
                PlanetModel planetModel = planetModels.get(planetName.toLowerCase().replace(" ", "_"));
                if (planetModel == null) {
                    Terradyne.LOGGER.warn("Cannot replace chunk generator for '{}' - no planet model", planetName);
                    continue;
                }

                // Replace the chunk generator using reflection
                replaceWorldChunkGenerator(world, planetModel);

                Terradyne.LOGGER.info("✅ Replaced chunk generator for planet: {}", planetName);

            } catch (Exception e) {
                Terradyne.LOGGER.error("Failed to replace chunk generator for planet '{}': {}", planetName, e.getMessage());
            }
        }
    }

    /**
     * Replace a world's chunk generator using reflection
     */
    private static void replaceWorldChunkGenerator(ServerWorld world, PlanetModel planetModel) {
        try {
            // Create our physics-based chunk generator
            BiomeSource biomeSource = createPlanetBiomeSource(world.getServer(), planetModel);
            UniversalChunkGenerator newGenerator = new UniversalChunkGenerator(planetModel, biomeSource);

            // Use reflection to replace the chunk generator
            // This is necessary because chunk generators are typically immutable after creation
            java.lang.reflect.Field chunkManagerField = findChunkManagerField(world);
            if (chunkManagerField != null) {
                chunkManagerField.setAccessible(true);
                Object chunkManager = chunkManagerField.get(world);

                // Find the chunk generator field in the chunk manager
                java.lang.reflect.Field generatorField = findChunkGeneratorField(chunkManager);
                if (generatorField != null) {
                    generatorField.setAccessible(true);
                    generatorField.set(chunkManager, newGenerator);

                    Terradyne.LOGGER.debug("Successfully replaced chunk generator via reflection");
                } else {
                    Terradyne.LOGGER.warn("Could not find chunk generator field in chunk manager");
                }
            } else {
                Terradyne.LOGGER.warn("Could not find chunk manager field in ServerWorld");
            }

        } catch (Exception e) {
            Terradyne.LOGGER.error("Failed to replace chunk generator via reflection", e);
        }
    }

    /**
     * Find the chunk manager field in ServerWorld
     */
    private static java.lang.reflect.Field findChunkManagerField(ServerWorld world) {
        java.lang.reflect.Field[] fields = world.getClass().getDeclaredFields();
        for (java.lang.reflect.Field field : fields) {
            // Look for fields that might be the chunk manager
            if (field.getName().toLowerCase().contains("chunk") ||
                    field.getType().getSimpleName().toLowerCase().contains("chunk")) {
                return field;
            }
        }
        return null;
    }

    /**
     * Find the chunk generator field in the chunk manager
     */
    private static java.lang.reflect.Field findChunkGeneratorField(Object chunkManager) {
        java.lang.reflect.Field[] fields = chunkManager.getClass().getDeclaredFields();
        for (java.lang.reflect.Field field : fields) {
            // Look for ChunkGenerator type
            if (field.getType().getSimpleName().equals("ChunkGenerator")) {
                return field;
            }
        }
        return null;
    }

    // === PUBLIC API FOR COMMANDS ===

    /**
     * Get list of available planets for a server
     */
    public static List<String> getAvailablePlanets(MinecraftServer server) {
        ExistingPlanetRegistry registry = SERVER_REGISTRIES.get(server);
        if (registry == null) {
            Terradyne.LOGGER.warn("No registry found for server in getAvailablePlanets()");
            return List.of();
        }

        var planetNames = registry.getGeneratedPlanetNames();
        Terradyne.LOGGER.debug("getAvailablePlanets() returning {} planets: {}", planetNames.size(), planetNames);
        return List.copyOf(planetNames);
    }

    /**
     * Check if a planet is available on a server
     */
    public static boolean isPlanetAvailable(MinecraftServer server, String planetName) {
        ExistingPlanetRegistry registry = SERVER_REGISTRIES.get(server);
        if (registry == null) {
            return false;
        }
        return registry.isPlanetGenerated(planetName.toLowerCase());
    }

    /**
     * Get planet info for a server
     */
    public static String getPlanetInfo(MinecraftServer server) {
        ExistingPlanetRegistry registry = SERVER_REGISTRIES.get(server);
        if (registry == null) {
            return "No planet registry found for this server";
        }

        // Use existing logic from StartupPlanetGenerator
        // TODO: Implement similar to the old getPlanetInfo method
        return "Planet info implementation needed";
    }

    /**
     * Get registry info for debugging
     */
    public static String getRegistryInfo(MinecraftServer server) {
        ExistingPlanetRegistry registry = SERVER_REGISTRIES.get(server);
        if (registry == null) {
            return "No planet registry found for this server";
        }
        return registry.getRegistryInfo();
    }

    /**
     * Reload configurations (for development)
     */
    public static void reloadConfigs(MinecraftServer server) {
        // TODO: Implement config reloading
        Terradyne.LOGGER.info("Config reloading not yet implemented");
    }

    /**
     * Get planet model by name
     */
    public static PlanetModel getPlanetModel(MinecraftServer server, String planetName) {
        Map<String, PlanetModel> planetModels = SERVER_PLANET_MODELS.get(server);
        if (planetModels == null) {
            return null;
        }
        return planetModels.get(planetName.toLowerCase().replace(" ", "_"));
    }

    /**
     * Get planet model by dimension ID (for UniversalChunkGenerator codec)
     */
    public static PlanetModel getPlanetModelByDimension(MinecraftServer server, Identifier dimensionId) {
        if (!Terradyne.MOD_ID.equals(dimensionId.getNamespace())) {
            return null;
        }
        return getPlanetModel(server, dimensionId.getPath());
    }

    /**
     * Clean up when server shuts down
     */
    public static void cleanupForServer(MinecraftServer server) {
        SERVER_REGISTRIES.remove(server);
        SERVER_PLANET_MODELS.remove(server);
    }

    // === UTILITY METHODS ===

    private static Path getTerradyneConfigDirectory(MinecraftServer server) {
        return server.getSavePath(net.minecraft.util.WorldSavePath.ROOT).resolve("terradyne");
    }
}