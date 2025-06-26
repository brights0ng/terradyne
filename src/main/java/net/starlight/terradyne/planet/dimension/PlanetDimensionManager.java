package net.starlight.terradyne.planet.dimension;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.FixedBiomeSource;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.util.math.random.RandomSequencesState;
import net.minecraft.server.WorldGenerationProgressListener;
import net.starlight.terradyne.Terradyne;
import net.starlight.terradyne.planet.chunk.UniversalChunkGenerator;
import net.starlight.terradyne.planet.physics.PlanetModel;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * Manages planet dimensions - UPDATED for startup-only generation
 * No longer supports runtime planet creation to ensure safety and performance
 */
public class PlanetDimensionManager {

    // Global registries for planet data
    private static final Map<String, RegistryKey<World>> PLANET_DIMENSIONS = new ConcurrentHashMap<>();
    private static final Map<String, ServerWorld> PLANET_WORLDS = new ConcurrentHashMap<>();
    private static final Map<String, PlanetModel> PLANET_MODELS = new ConcurrentHashMap<>();

    /**
     * Create a new planet dimension during STARTUP ONLY
     * This method should only be called during server startup from StartupPlanetGenerator
     */
    public static RegistryKey<World> createPlanet(MinecraftServer server, PlanetModel planetModel) {
        String planetName = planetModel.getConfig().getPlanetName().toLowerCase().replace(" ", "_");

        Terradyne.LOGGER.info("=== CREATING STARTUP PLANET ===");
        Terradyne.LOGGER.info("Planet: {}", planetName);
        Terradyne.LOGGER.info("Classification: {}", planetModel.getPlanetClassification());
        Terradyne.LOGGER.info("Temperature: {:.1f}°C", planetModel.getPlanetData().getAverageSurfaceTemp());
        Terradyne.LOGGER.info("Habitability: {:.2f}", planetModel.getPlanetData().getHabitability());

        // Check if planet already exists (safety check)
        if (PLANET_DIMENSIONS.containsKey(planetName)) {
            Terradyne.LOGGER.warn("Planet '{}' already exists, returning existing dimension", planetName);
            return PLANET_DIMENSIONS.get(planetName);
        }

        try {
            // Create dimension identifier
            Identifier dimensionId = new Identifier(Terradyne.MOD_ID, planetName);
            RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, dimensionId);

            // Select appropriate dimension type from data-generated registry
            RegistryEntry<DimensionType> dimensionType = selectDimensionTypeFromRegistry(server, planetModel);

            // Create biome source (Plains for now)
            BiomeSource biomeSource = createPlanetBiomeSource(server, planetModel);

            // Create chunk generator with physics system
            UniversalChunkGenerator chunkGenerator = new UniversalChunkGenerator(planetModel, biomeSource);

            // Create dimension options
            DimensionOptions dimensionOptions = new DimensionOptions(dimensionType, chunkGenerator);

            // Create the actual ServerWorld
            ServerWorld serverWorld = createServerWorld(server, worldKey, dimensionOptions);

            // Store planet data in registries
            PLANET_DIMENSIONS.put(planetName, worldKey);
            PLANET_WORLDS.put(planetName, serverWorld);
            PLANET_MODELS.put(planetName, planetModel);

            Terradyne.LOGGER.info("✅ Startup planet '{}' created successfully", planetName);
            Terradyne.LOGGER.info("   Dimension: {}", dimensionId);
            Terradyne.LOGGER.info("   Dimension Type: {}", getDimensionTypeInfo(planetModel));

            // Verify final dimension type properties
            DimensionType worldDimensionType = serverWorld.getDimension();
            Terradyne.LOGGER.info("=== FINAL WORLD VERIFICATION ===");
            Terradyne.LOGGER.info("Created world dimension type properties:");
            Terradyne.LOGGER.info("  - Ultrawarm: {}", worldDimensionType.ultrawarm());
            Terradyne.LOGGER.info("  - Has Skylight: {}", worldDimensionType.hasSkyLight());
            Terradyne.LOGGER.info("  - Has Ceiling: {}", worldDimensionType.hasCeiling());
            Terradyne.LOGGER.info("  - Ambient Light: {}", worldDimensionType.ambientLight());
            Terradyne.LOGGER.info("  - Height Range: Y {} to {}", worldDimensionType.minY(), worldDimensionType.minY() + worldDimensionType.height() - 1);

            return worldKey;

        } catch (Exception e) {
            Terradyne.LOGGER.error("Failed to create startup planet: {}", planetName, e);
            throw new RuntimeException("Startup planet creation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Select appropriate dimension type from data-generated registry based on planet characteristics
     */
    private static RegistryEntry<DimensionType> selectDimensionTypeFromRegistry(MinecraftServer server, PlanetModel planetModel) {
        // Select dimension type based on planet characteristics
        ModDimensionTypes.TerradyneDimensionType selectedType = DimensionTypeFactory.selectDimensionType(planetModel);

        Terradyne.LOGGER.info("Selected dimension type: {} - {}",
                selectedType.getName(),
                DimensionTypeFactory.getDimensionTypeDescription(selectedType));

        // Get the registry key for the selected type
        RegistryKey<DimensionType> registryKey = ModDimensionTypes.getRegistryKey(selectedType);

        // Get dimension type from server registry
        Registry<DimensionType> dimensionTypeRegistry = server.getRegistryManager().get(RegistryKeys.DIMENSION_TYPE);

        // Look up the dimension type in the registry
        RegistryEntry<DimensionType> dimensionType = dimensionTypeRegistry.getEntry(registryKey).orElse(null);

        if (dimensionType == null) {
            Terradyne.LOGGER.error("❌ Dimension type {} not found in registry!", registryKey.getValue());
            Terradyne.LOGGER.error("Available dimension types in registry:");
            dimensionTypeRegistry.getKeys().forEach(key ->
                    Terradyne.LOGGER.error("  - {}", key.getValue()));

            Terradyne.LOGGER.error("Make sure to run 'gradlew runDatagen' to generate dimension type JSON files!");
            throw new RuntimeException("Dimension type not found in registry: " + registryKey.getValue() +
                    ". Run 'gradlew runDatagen' to generate the required JSON files.");
        }

        // Verify the dimension type properties
        DimensionType actualType = dimensionType.value();
        Terradyne.LOGGER.info("=== USING DATA-GENERATED DIMENSION TYPE ===");
        Terradyne.LOGGER.info("Registry Key: {}", registryKey.getValue());
        Terradyne.LOGGER.info("Dimension type properties:");
        Terradyne.LOGGER.info("  - Ultrawarm: {}", actualType.ultrawarm());
        Terradyne.LOGGER.info("  - Has Skylight: {}", actualType.hasSkyLight());
        Terradyne.LOGGER.info("  - Has Ceiling: {}", actualType.hasCeiling());
        Terradyne.LOGGER.info("  - Ambient Light: {}", actualType.ambientLight());
        Terradyne.LOGGER.info("  - Height: {} (Y {} to {})", actualType.height(), actualType.minY(), actualType.minY() + actualType.height() - 1);

        // Special verification for ultrawarm planets
        if (selectedType == ModDimensionTypes.TerradyneDimensionType.ULTRAWARM && !actualType.ultrawarm()) {
            Terradyne.LOGGER.error("❌ CRITICAL: Ultrawarm dimension type does not have ultrawarm=true!");
        } else if (selectedType == ModDimensionTypes.TerradyneDimensionType.ULTRAWARM && actualType.ultrawarm()) {
            Terradyne.LOGGER.info("✅ Verified: Ultrawarm dimension type has ultrawarm=true");
        }

        return dimensionType;
    }

    /**
     * Create biome source for planet (currently just Plains)
     */
    private static BiomeSource createPlanetBiomeSource(MinecraftServer server, PlanetModel planetModel) {
        Terradyne.LOGGER.debug("Creating biome source for planet: {}", planetModel.getConfig().getPlanetName());

        // Get Plains biome from registry
        Registry<Biome> biomeRegistry = server.getRegistryManager().get(RegistryKeys.BIOME);
        RegistryEntry<Biome> plainsBiome = biomeRegistry.getEntry(BiomeKeys.PLAINS)
                .orElseThrow(() -> new RuntimeException("Plains biome not found in registry"));

        Terradyne.LOGGER.debug("✅ Using Plains biome for terrain (physics-based terrain with vanilla biome)");

        return new FixedBiomeSource(plainsBiome);
    }

    /**
     * Get dimension type info for logging
     */
    private static String getDimensionTypeInfo(PlanetModel planetModel) {
        ModDimensionTypes.TerradyneDimensionType type = DimensionTypeFactory.selectDimensionType(planetModel);
        return String.format("%s (%s)", type.getName(), DimensionTypeFactory.getDimensionTypeDescription(type));
    }

    /**
     * Teleport player to planet
     */
    public static void teleportToPlanet(ServerPlayerEntity player, String planetName) {
        String normalizedName = planetName.toLowerCase().replace(" ", "_");
        ServerWorld targetWorld = PLANET_WORLDS.get(normalizedName);

        if (targetWorld == null) {
            throw new RuntimeException("Planet not found: " + planetName +
                    ". Make sure the planet exists and was generated during server startup.");
        }

        // Ensure everything runs on the main server thread
        player.getServer().execute(() -> {
            try {
                Terradyne.LOGGER.info("=== TELEPORTING TO PLANET ===");
                Terradyne.LOGGER.info("Planet: {}", planetName);
                Terradyne.LOGGER.info("Player: {}", player.getName().getString());

                // Get spawn position (safe height above terrain) - for 0-256 range
                Vec3d spawnPos = new Vec3d(0.5, 150, 0.5);
                int chunkX = (int) spawnPos.x >> 4;
                int chunkZ = (int) spawnPos.z >> 4;

                // Pre-load chunks around spawn position
                Terradyne.LOGGER.debug("Loading chunks around spawn ({}, {})", chunkX, chunkZ);
                for (int x = chunkX - 2; x <= chunkX + 2; x++) {
                    for (int z = chunkZ - 2; z <= chunkZ + 2; z++) {
                        try {
                            targetWorld.setChunkForced(x, z, true);
                            targetWorld.getChunk(x, z);
                        } catch (Exception e) {
                            Terradyne.LOGGER.warn("Failed to load chunk ({}, {}): {}", x, z, e.getMessage());
                        }
                    }
                }

                // Execute any pending chunk tasks
                targetWorld.getChunkManager().executeQueuedTasks();

                // Perform teleportation
                player.teleport(targetWorld, spawnPos.x, spawnPos.y, spawnPos.z, 0, 0);

                Terradyne.LOGGER.info("✅ Teleportation completed successfully");

            } catch (Exception e) {
                Terradyne.LOGGER.error("Teleportation failed", e);
            }
        });
    }

    /**
     * Check if planet exists
     */
    public static boolean planetExists(String planetName) {
        String normalizedName = planetName.toLowerCase().replace(" ", "_");
        return PLANET_DIMENSIONS.containsKey(normalizedName);
    }

    /**
     * List all created planets
     */
    public static String listPlanets() {
        if (PLANET_DIMENSIONS.isEmpty()) {
            return "No planets generated yet";
        }

        StringBuilder sb = new StringBuilder();
        for (String planetName : PLANET_DIMENSIONS.keySet()) {
            PlanetModel model = PLANET_MODELS.get(planetName);
            String classification = (model != null) ? model.getPlanetClassification() : "Unknown";
            sb.append(planetName).append(" (").append(classification).append("), ");
        }

        if (sb.length() > 2) {
            sb.setLength(sb.length() - 2);
        }

        return sb.toString();
    }

    /**
     * Get planet model for a given planet name
     */
    public static PlanetModel getPlanetModelByName(String planetName) {
        String normalizedName = planetName.toLowerCase().replace(" ", "_");
        return PLANET_MODELS.get(normalizedName);
    }

    /**
     * Get PlanetModel for a dimension (used by UniversalChunkGenerator codec)
     */
    public static PlanetModel getPlanetModel(String planetName) {
        String normalizedName = planetName.toLowerCase().replace(" ", "_");
        return PLANET_MODELS.get(normalizedName);
    }

    /**
     * Get PlanetModel by dimension ID
     */
    public static PlanetModel getPlanetModelByDimension(Identifier dimensionId) {
        if (!Terradyne.MOD_ID.equals(dimensionId.getNamespace())) {
            return null;
        }
        return PLANET_MODELS.get(dimensionId.getPath());
    }

    // ============================================================================
    // CORE WORLD CREATION (preserved from existing implementation)
    // ============================================================================

    /**
     * Create ServerWorld instance
     */
    private static ServerWorld createServerWorld(MinecraftServer server, RegistryKey<World> worldKey,
                                                 DimensionOptions dimensionOptions) {
        try {
            ServerWorld overworld = server.getWorld(World.OVERWORLD);
            if (overworld == null) {
                throw new RuntimeException("Overworld not found");
            }

            Terradyne.LOGGER.debug("Creating ServerWorld for dimension: {}", worldKey.getValue());

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

            Terradyne.LOGGER.debug("ServerWorld created and registered successfully");
            return world;

        } catch (Exception e) {
            Terradyne.LOGGER.error("ServerWorld creation failed", e);
            throw new RuntimeException("Failed to create ServerWorld: " + e.getMessage(), e);
        }
    }

    // ============================================================================
    // REFLECTION HELPERS (preserved from existing implementation)
    // ============================================================================

    private static Executor getExecutorField(MinecraftServer server) {
        try {
            String[] possibleNames = {"workerExecutor", "executor", "backgroundExecutor", "taskExecutor"};

            for (String fieldName : possibleNames) {
                try {
                    java.lang.reflect.Field field = MinecraftServer.class.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    Object value = field.get(server);

                    if (value instanceof Executor) {
                        Terradyne.LOGGER.debug("Found executor field: {}", fieldName);
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
                        Terradyne.LOGGER.debug("Found executor field by type: {}", field.getName());
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
                        Terradyne.LOGGER.debug("Found session field: {}", fieldName);
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

                                Terradyne.LOGGER.debug("✅ Successfully registered world with server via field: {}", field.getName());
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
                        Terradyne.LOGGER.warn("Failed to pre-generate chunk ({}, {}): {}", x, z, e.getMessage());
                    }
                }
            }

            Terradyne.LOGGER.debug("World initialized for client synchronization");

        } catch (Exception e) {
            Terradyne.LOGGER.warn("Failed to fully initialize world for client: {}", e.getMessage());
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