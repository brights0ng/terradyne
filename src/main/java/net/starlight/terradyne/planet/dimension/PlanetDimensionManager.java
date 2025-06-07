package net.starlight.terradyne.planet.dimension;
// PlanetDimensionManager.java - COMPLETE VERSION with Desert + Oceanic + Rocky support
import com.mojang.serialization.Lifecycle;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.dimension.DimensionTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.FixedBiomeSource;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.util.math.random.RandomSequencesState;
import net.minecraft.server.WorldGenerationProgressListener;
import net.starlight.terradyne.Terradyne;
import net.starlight.terradyne.planet.PlanetType;
import net.starlight.terradyne.planet.biome.DesertBiomeSource;
import net.starlight.terradyne.planet.chunk.UniversalChunkGenerator;
import net.starlight.terradyne.planet.model.DesertModel;
import net.starlight.terradyne.planet.model.OceanicModel;
import net.starlight.terradyne.planet.model.RockyModel;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.Map;
import java.util.List;

public class PlanetDimensionManager {
    // Storage maps for all planet types
    private static final Map<String, RegistryKey<World>> PLANET_DIMENSIONS = new ConcurrentHashMap<>();
    private static final Map<String, ServerWorld> PLANET_WORLDS = new ConcurrentHashMap<>();
    private static final Map<String, PlanetType> PLANET_TYPES = new ConcurrentHashMap<>();

    // Type-specific model storage
    private static final Map<String, DesertModel> DESERT_MODELS = new ConcurrentHashMap<>();
    private static final Map<String, OceanicModel> OCEANIC_MODELS = new ConcurrentHashMap<>();
    private static final Map<String, RockyModel> ROCKY_MODELS = new ConcurrentHashMap<>();

    // Updated createDesertPlanet method for PlanetDimensionManager.java
// Replace the existing method with this cleaned-up version:

    public static RegistryKey<World> createDesertPlanet(MinecraftServer server, DesertModel model) {
        String planetName = model.getConfig().getPlanetName().toLowerCase().replace(" ", "_");

        Terradyne.LOGGER.info("=== CREATING DESERT PLANET ===");
        Terradyne.LOGGER.info("Planet: {}", planetName);
        Terradyne.LOGGER.info("Temperature: {}°C", model.getConfig().getSurfaceTemperature());
        Terradyne.LOGGER.info("Sand Density: {}%", model.getConfig().getSandDensity() * 100);

        if (PLANET_DIMENSIONS.containsKey(planetName)) {
            return PLANET_DIMENSIONS.get(planetName);
        }

        try {
            Identifier dimensionId = new Identifier(Terradyne.MOD_ID, planetName);
            RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, dimensionId);

            RegistryEntry<DimensionType> dimensionType = selectDimensionTypeForDesert(server, model);
            BiomeSource biomeSource = createDesertBiomeSource(server, model);
            UniversalChunkGenerator chunkGenerator = new UniversalChunkGenerator(model, biomeSource);

            DimensionOptions dimensionOptions = new DimensionOptions(dimensionType, chunkGenerator);
            ServerWorld serverWorld = createServerWorld(server, worldKey, dimensionOptions);

            // Store planet data
            PLANET_DIMENSIONS.put(planetName, worldKey);
            PLANET_WORLDS.put(planetName, serverWorld);
            PLANET_TYPES.put(planetName, model.getConfig().getType());
            DESERT_MODELS.put(planetName, model);

            Terradyne.LOGGER.info("✅ Desert planet '{}' created successfully", planetName);
            logBiomeSourceStatus(biomeSource);

            return worldKey;

        } catch (Exception e) {
            Terradyne.LOGGER.error("Failed to create desert planet: {}", planetName, e);
            throw new RuntimeException("Desert planet creation failed: " + e.getMessage(), e);
        }
    }

    // NEW METHOD: Create desert biome source (simplified)
    private static BiomeSource createDesertBiomeSource(MinecraftServer server, DesertModel model) {
        Terradyne.LOGGER.info("Creating desert biome source...");

        DesertBiomeSource biomeSource = new DesertBiomeSource(model, server);

        Terradyne.LOGGER.info("✅ Desert biome source created");
        return biomeSource;
    }

    // NEW METHOD: Log biome source status
    private static void logBiomeSourceStatus(BiomeSource biomeSource) {
        if (biomeSource instanceof DesertBiomeSource desertBiomeSource) {
            if (desertBiomeSource.isUsingCustomBiomes()) {
                Terradyne.LOGGER.info("  ✓ Using custom biomes from data generation");
            } else {
                Terradyne.LOGGER.info("  ⚠ Using vanilla biomes with custom terrain");
                Terradyne.LOGGER.info("    Run 'gradlew runDatagen' to generate custom biomes");
            }
            Terradyne.LOGGER.info("  Available terrain types: {}", desertBiomeSource.getAvailableTerrainTypes().size());
        }
    }

    // UPDATED BIOME SOURCE CREATION for other planet types (simplified naming)
    private static BiomeSource createOceanicBiomeSource(MinecraftServer server, OceanicModel model) {
        Terradyne.LOGGER.info("Creating oceanic biome source (vanilla biomes)...");

        Registry<Biome> biomeRegistry = server.getRegistryManager().get(RegistryKeys.BIOME);
        RegistryEntry<Biome> oceanBiome = biomeRegistry.getEntry(BiomeKeys.OCEAN).orElseThrow();

        return new FixedBiomeSource(oceanBiome);
    }

    private static BiomeSource createRockyBiomeSource(MinecraftServer server, RockyModel model) {
        Terradyne.LOGGER.info("Creating rocky biome source (vanilla biomes)...");

        Registry<Biome> biomeRegistry = server.getRegistryManager().get(RegistryKeys.BIOME);
        RegistryKey<Biome> biomeKey = model.getConfig().getAtmosphericDensity() < 0.02f ?
                BiomeKeys.END_BARRENS : BiomeKeys.BADLANDS;

        RegistryEntry<Biome> biome = biomeRegistry.getEntry(biomeKey).orElseThrow();
        return new FixedBiomeSource(biome);
    }

    // TELEPORTATION
    public static void teleportToPlanet(ServerPlayerEntity player, String planetName) {
        String normalizedName = planetName.toLowerCase().replace(" ", "_");
        ServerWorld targetWorld = PLANET_WORLDS.get(normalizedName);

        if (targetWorld != null) {
            // Ensure everything runs on the main server thread
            player.getServer().execute(() -> {
                try {
                    Terradyne.LOGGER.info("=== TELEPORTING TO PLANET (SERVER THREAD) ===");
                    Terradyne.LOGGER.info("Planet: " + planetName);

                    Vec3d spawnPos = new Vec3d(0.5, 70, 0.5);
                    int chunkX = (int) spawnPos.x >> 4;
                    int chunkZ = (int) spawnPos.z >> 4;

                    // Force load chunks around spawn position
                    Terradyne.LOGGER.info("Loading chunks around " + chunkX + "," + chunkZ);
                    for (int x = chunkX - 2; x <= chunkX + 2; x++) {
                        for (int z = chunkZ - 2; z <= chunkZ + 2; z++) {
                            try {
                                targetWorld.setChunkForced(x, z, true);
                                targetWorld.getChunk(x, z);
                            } catch (Exception e) {
                                Terradyne.LOGGER.warn("Failed to load chunk " + x + "," + z + ": " + e.getMessage());
                            }
                        }
                    }

                    // Execute chunk tasks
                    targetWorld.getChunkManager().executeQueuedTasks();

                    // Teleport after chunks are loaded
                    player.teleport(targetWorld, spawnPos.x, spawnPos.y, spawnPos.z, 0, 0);

                    Terradyne.LOGGER.info("✅ Teleportation completed successfully");

                } catch (Exception e) {
                    Terradyne.LOGGER.error("Server thread teleportation failed", e);
                }
            });
        } else {
            throw new RuntimeException("Planet not found: " + planetName);
        }
    }

    // UTILITY METHODS
    public static boolean planetExists(String planetName) {
        String normalizedName = planetName.toLowerCase().replace(" ", "_");
        return PLANET_DIMENSIONS.containsKey(normalizedName);
    }

    public static PlanetType getPlanetType(String planetName) {
        String normalizedName = planetName.toLowerCase().replace(" ", "_");
        return PLANET_TYPES.get(normalizedName);
    }

    public static String listPlanets() {
        if (PLANET_DIMENSIONS.isEmpty()) {
            return "No planets created yet";
        }

        StringBuilder sb = new StringBuilder();
        for (String planetName : PLANET_DIMENSIONS.keySet()) {
            PlanetType type = PLANET_TYPES.get(planetName);
            sb.append(planetName).append(" (").append(type != null ? type.getDisplayName() : "Unknown").append("), ");
        }

        if (sb.length() > 2) {
            sb.setLength(sb.length() - 2);
        }

        return sb.toString();
    }

    // MODEL GETTERS
    public static DesertModel getDesertModel(String planetName) {
        String normalizedName = planetName.toLowerCase().replace(" ", "_");
        return DESERT_MODELS.get(normalizedName);
    }

    public static OceanicModel getOceanicModel(String planetName) {
        String normalizedName = planetName.toLowerCase().replace(" ", "_");
        return OCEANIC_MODELS.get(normalizedName);
    }

    public static RockyModel getRockyModel(String planetName) {
        String normalizedName = planetName.toLowerCase().replace(" ", "_");
        return ROCKY_MODELS.get(normalizedName);
    }


    private static RegistryEntry<DimensionType> selectDimensionTypeForDesert(MinecraftServer server, DesertModel model) {
        Registry<DimensionType> registry = server.getRegistryManager().get(RegistryKeys.DIMENSION_TYPE);

        // Check if our custom dimension type is already registered
        if (registry.contains(ModDimensionTypes.DESERT_PLANET)) {
            return registry.getEntry(ModDimensionTypes.DESERT_PLANET).orElseThrow();
        }

        // If not registered, register it dynamically
        return registerDesertPlanetDimensionType(server, model);
    }

    // ADD this new method for dynamic registration:
    private static RegistryEntry<DimensionType> registerDesertPlanetDimensionType(MinecraftServer server, DesertModel model) {
        try {
            MutableRegistry<DimensionType> mutableRegistry = (MutableRegistry<DimensionType>)
                    server.getRegistryManager().get(RegistryKeys.DIMENSION_TYPE);

            // Create the custom dimension type based on planet characteristics
            DimensionType customDimensionType = DimensionTypeFactory.createDesertPlanetDimension(model);

            // Register it
            RegistryEntry<DimensionType> entry = mutableRegistry.add(ModDimensionTypes.DESERT_PLANET, customDimensionType, Lifecycle.stable());

            Terradyne.LOGGER.info("✅ Registered custom desert planet dimension type");
            Terradyne.LOGGER.info("   Temperature: " + model.getConfig().getSurfaceTemperature() + "°C");
            Terradyne.LOGGER.info("   Ultrawarm: " + customDimensionType.ultrawarm());
            Terradyne.LOGGER.info("   Ambient Light: " + customDimensionType.ambientLight());
            Terradyne.LOGGER.info("   Has Skylight: " + customDimensionType.hasSkyLight());

            return entry;

        } catch (Exception e) {
            Terradyne.LOGGER.error("Failed to register custom desert dimension type, falling back to overworld", e);

            // Fallback to overworld if registration fails
            Registry<DimensionType> registry = server.getRegistryManager().get(RegistryKeys.DIMENSION_TYPE);
            return registry.getEntry(DimensionTypes.OVERWORLD).orElseThrow();
        }
    }

    // UPDATE the selectDimensionTypeForRocky method:
    private static RegistryEntry<DimensionType> selectDimensionTypeForRocky(MinecraftServer server, RockyModel model) {
        Registry<DimensionType> registry = server.getRegistryManager().get(RegistryKeys.DIMENSION_TYPE);

        // Check if our custom dimension type is already registered
        if (registry.contains(ModDimensionTypes.ROCKY_PLANET)) {
            return registry.getEntry(ModDimensionTypes.ROCKY_PLANET).orElseThrow();
        }

        // If not registered, register it dynamically
        return registerRockyPlanetDimensionType(server, model);
    }

    // ADD this method for rocky planet dimension registration:
    private static RegistryEntry<DimensionType> registerRockyPlanetDimensionType(MinecraftServer server, RockyModel model) {
        try {
            MutableRegistry<DimensionType> mutableRegistry = (MutableRegistry<DimensionType>)
                    server.getRegistryManager().get(RegistryKeys.DIMENSION_TYPE);

            DimensionType customDimensionType = DimensionTypeFactory.createRockyPlanetDimension(model);
            RegistryEntry<DimensionType> entry = mutableRegistry.add(ModDimensionTypes.ROCKY_PLANET, customDimensionType, Lifecycle.stable());

            Terradyne.LOGGER.info("✅ Registered custom rocky planet dimension type");
            Terradyne.LOGGER.info("   Atmospheric Density: " + model.getConfig().getAtmosphericDensity());
            Terradyne.LOGGER.info("   Has Skylight: " + customDimensionType.hasSkyLight());
            Terradyne.LOGGER.info("   Bed Works: " + customDimensionType.bedWorks());
            Terradyne.LOGGER.info("   Ambient Light: " + customDimensionType.ambientLight());

            return entry;

        } catch (Exception e) {
            Terradyne.LOGGER.error("Failed to register rocky dimension type, falling back to End", e);
            Registry<DimensionType> registry = server.getRegistryManager().get(RegistryKeys.DIMENSION_TYPE);
            return registry.getEntry(DimensionTypes.THE_END).orElseThrow();
        }
    }

    // UPDATE the selectDimensionTypeForOceanic method:
    private static RegistryEntry<DimensionType> selectDimensionTypeForOceanic(MinecraftServer server, OceanicModel model) {
        Registry<DimensionType> registry = server.getRegistryManager().get(RegistryKeys.DIMENSION_TYPE);

        // Check if our custom dimension type is already registered
        if (registry.contains(ModDimensionTypes.OCEANIC_PLANET)) {
            return registry.getEntry(ModDimensionTypes.OCEANIC_PLANET).orElseThrow();
        }

        // If not registered, register it dynamically
        return registerOceanicPlanetDimensionType(server, model);
    }

    // ADD this method for oceanic planet dimension registration:
    private static RegistryEntry<DimensionType> registerOceanicPlanetDimensionType(MinecraftServer server, OceanicModel model) {
        try {
            MutableRegistry<DimensionType> mutableRegistry = (MutableRegistry<DimensionType>)
                    server.getRegistryManager().get(RegistryKeys.DIMENSION_TYPE);

            DimensionType customDimensionType = DimensionTypeFactory.createOceanicPlanetDimension(model);
            RegistryEntry<DimensionType> entry = mutableRegistry.add(ModDimensionTypes.OCEANIC_PLANET, customDimensionType, Lifecycle.stable());

            Terradyne.LOGGER.info("✅ Registered custom oceanic planet dimension type");
            Terradyne.LOGGER.info("   Ocean Coverage: " + (model.getConfig().getOceanCoverage() * 100) + "%");
            Terradyne.LOGGER.info("   Atmospheric Humidity: " + (model.getConfig().getAtmosphericHumidity() * 100) + "%");

            return entry;

        } catch (Exception e) {
            Terradyne.LOGGER.error("Failed to register oceanic dimension type, falling back to overworld", e);
            Registry<DimensionType> registry = server.getRegistryManager().get(RegistryKeys.DIMENSION_TYPE);
            return registry.getEntry(DimensionTypes.OVERWORLD).orElseThrow();
        }
    }

    // CORE WORLD CREATION
    private static ServerWorld createServerWorld(MinecraftServer server, RegistryKey<World> worldKey,
                                                 DimensionOptions dimensionOptions) {
        try {
            ServerWorld overworld = server.getWorld(World.OVERWORLD);
            if (overworld == null) {
                throw new RuntimeException("Overworld not found");
            }

            Terradyne.LOGGER.info("Creating ServerWorld...");

            ServerWorld world = new ServerWorld(
                    server,
                    getExecutorField(server),
                    getSessionField(server),
                    server.getSaveProperties().getMainWorldProperties(),
                    worldKey,
                    dimensionOptions,
                    createDummyProgressListener(),
                    false,
                    overworld.getSeed(),
                    List.of(),
                    false,
                    new RandomSequencesState(overworld.getSeed())
            );

            // Register the world with the server
            registerWorldWithServer(server, worldKey, world);

            // Initialize the world properly
            initializeWorldForClient(world);

            Terradyne.LOGGER.info("ServerWorld created and registered successfully");
            return world;

        } catch (Exception e) {
            Terradyne.LOGGER.error("ServerWorld creation failed", e);
            throw new RuntimeException("Failed to create ServerWorld: " + e.getMessage(), e);
        }
    }

    // REFLECTION HELPERS
    private static Executor getExecutorField(MinecraftServer server) {
        try {
            String[] possibleNames = {"workerExecutor", "executor", "backgroundExecutor", "taskExecutor"};

            for (String fieldName : possibleNames) {
                try {
                    java.lang.reflect.Field field = MinecraftServer.class.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    Object value = field.get(server);

                    if (value instanceof Executor) {
                        Terradyne.LOGGER.info("Found executor field: " + fieldName);
                        return (Executor) value;
                    }
                } catch (NoSuchFieldException ignored) {}
            }

            java.lang.reflect.Field[] fields = MinecraftServer.class.getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                if (Executor.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    Object value = field.get(server);
                    if (value != null) {
                        Terradyne.LOGGER.info("Found executor field by type: " + field.getName());
                        return (Executor) value;
                    }
                }
            }

            return task -> server.execute(() -> task.run());

        } catch (Exception e) {
            return task -> server.execute(() -> task.run());
        }
    }

    private static LevelStorage.Session getSessionField(MinecraftServer server) {
        try {
            String[] possibleNames = {"session", "levelStorage", "saveHandler"};

            for (String fieldName : possibleNames) {
                try {
                    java.lang.reflect.Field field = MinecraftServer.class.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    Object value = field.get(server);

                    if (value instanceof LevelStorage.Session) {
                        Terradyne.LOGGER.info("Found session field: " + fieldName);
                        return (LevelStorage.Session) value;
                    }
                } catch (NoSuchFieldException ignored) {}
            }

            java.lang.reflect.Field[] fields = MinecraftServer.class.getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                if (LevelStorage.Session.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    Object value = field.get(server);
                    if (value != null) {
                        return (LevelStorage.Session) value;
                    }
                }
            }

            throw new RuntimeException("Could not find LevelStorage.Session field");

        } catch (Exception e) {
            throw new RuntimeException("Failed to get session field: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private static void registerWorldWithServer(MinecraftServer server, RegistryKey<World> worldKey, ServerWorld world) {
        try {
            java.lang.reflect.Field[] fields = MinecraftServer.class.getDeclaredFields();

            for (java.lang.reflect.Field field : fields) {
                if (field.getType() == Map.class) {
                    field.setAccessible(true);
                    Object fieldValue = field.get(server);

                    if (fieldValue instanceof Map<?, ?> map && !map.isEmpty()) {
                        Object firstKey = map.keySet().iterator().next();

                        if (firstKey instanceof RegistryKey<?>) {
                            try {
                                Map<RegistryKey<World>, ServerWorld> worldsMap = (Map<RegistryKey<World>, ServerWorld>) map;
                                worldsMap.put(worldKey, world);

                                Terradyne.LOGGER.info("✅ Successfully registered world with server via field: " + field.getName());
                                return;
                            } catch (ClassCastException ignored) {}
                        }
                    }
                }
            }

            Terradyne.LOGGER.warn("Could not find worlds map in server - chunks may not sync properly");

        } catch (Exception e) {
            Terradyne.LOGGER.error("Failed to register world with server", e);
        }
    }

    private static void initializeWorldForClient(ServerWorld world) {
        try {
            world.getChunkManager().executeQueuedTasks();

            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    try {
                        world.getChunk(x, z);
                    } catch (Exception e) {
                        Terradyne.LOGGER.warn("Failed to pre-generate chunk " + x + "," + z + ": " + e.getMessage());
                    }
                }
            }

            Terradyne.LOGGER.info("World initialized for client synchronization");

        } catch (Exception e) {
            Terradyne.LOGGER.warn("Failed to fully initialize world for client: " + e.getMessage());
        }
    }

    private static WorldGenerationProgressListener createDummyProgressListener() {
        return new WorldGenerationProgressListener() {
            @Override public void start(net.minecraft.util.math.ChunkPos spawnPos) {}
            @Override public void setChunkStatus(net.minecraft.util.math.ChunkPos pos, net.minecraft.world.chunk.ChunkStatus status) {}
            @Override public void start() {}
            @Override public void stop() {}
        };
    }
}