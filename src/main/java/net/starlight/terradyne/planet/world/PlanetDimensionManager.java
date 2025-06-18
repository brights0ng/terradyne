// PlanetDimensionManager.java - Fixed for proper ServerWorldProperties handling
package net.starlight.terradyne.planet.world;

import net.fabricmc.fabric.api.dimension.v1.FabricDimensions;
import net.minecraft.client.render.DimensionEffects;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.ProgressListener;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionOptions;

import net.minecraft.world.dimension.DimensionTypes;
import net.starlight.terradyne.planet.physics.PlanetModel;
import net.starlight.terradyne.planet.physics.PlanetData;
import net.starlight.terradyne.planet.world.biome.PlanetBiomeSource;

import java.awt.*;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.List;

// Additional imports for ServerWorld creation - fixed for 1.20.1
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.level.ServerWorldProperties;  // Fixed: Use ServerWorldProperties
import net.minecraft.util.math.random.RandomSequencesState;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.spawner.Spawner;
import net.minecraft.world.spawner.PhantomSpawner;
import net.minecraft.world.spawner.PatrolSpawner;
import net.minecraft.world.spawner.CatSpawner;
import org.jetbrains.annotations.Nullable;
// Removed problematic spawners that require ServerWorldProperties in constructor

/**
 * Manages planetary dimensions - creation, registration, and player teleportation.
 * Handles the lifecycle of planet dimensions within the Minecraft server.
 *
 * This implementation creates actual ServerWorld instances at runtime using reflection
 * to access Minecraft's internal world management system.
 */
public class PlanetDimensionManager {

    // Track all created planet dimensions
    private static final Map<String, RegistryKey<World>> planetDimensions = new ConcurrentHashMap<>();
    private static final Map<String, PlanetModel> planetModels = new ConcurrentHashMap<>();

    /**
     * Create and register a planet dimension
     */
    public static RegistryKey<World> createPlanetDimension(MinecraftServer server, PlanetModel planetModel) {
        String planetName = planetModel.getPlanetData().getPlanetName();

        // Check if dimension already exists
        if (planetDimensions.containsKey(planetName)) {
            System.out.println("Planet dimension already exists: " + planetName);
            return planetDimensions.get(planetName);
        }

        try {
            // Create dimension key
            Identifier dimensionId = new Identifier("terradyne", "planet_" + planetName.toLowerCase().replace(" ", "_"));
            RegistryKey<World> dimensionKey = RegistryKey.of(RegistryKeys.WORLD, dimensionId);

            // Create biome source for this planet
            PlanetBiomeSource biomeSource = new PlanetBiomeSource(planetModel.getPlanetData());

            // Create chunk generator
            PlanetChunkGenerator chunkGenerator = new PlanetChunkGenerator(planetModel, biomeSource);

            // Create dimension options
            DimensionOptions dimensionOptions = createDimensionOptions(server, chunkGenerator);

            // Register the dimension with the server
            registerDimensionWithServer(server, dimensionKey, dimensionOptions);

            // Store references
            planetDimensions.put(planetName, dimensionKey);
            planetModels.put(planetName, planetModel);

            System.out.println("Successfully created planet dimension: " + planetName + " (" + dimensionId + ")");
            return dimensionKey;

        } catch (Exception e) {
            System.err.println("Failed to create planet dimension for " + planetName + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Create dimension options for the planet
     */
    private static DimensionOptions createDimensionOptions(MinecraftServer server, PlanetChunkGenerator chunkGenerator) {
        return new DimensionOptions(
                server.getRegistryManager().get(RegistryKeys.DIMENSION_TYPE).entryOf(PlanetDimensionType.PLANET_DIMENSION_TYPE),
                chunkGenerator
        );
    }

    /**
     * Create and register a ServerWorld instance at runtime
     * This is the main implementation that creates actual server worlds
     */
    private static void registerDimensionWithServer(MinecraftServer server, RegistryKey<World> dimensionKey,
                                                    DimensionOptions dimensionOptions) {
        try {
            System.out.println("Creating ServerWorld for dimension: " + dimensionKey.getValue());

            // Step 1: Gather all required parameters
            ServerWorldParameters params = gatherServerWorldParameters(server, dimensionKey, dimensionOptions);

            // Step 2: Create the ServerWorld instance
            ServerWorld serverWorld = createServerWorld(params);

            // Step 3: Add to server's worlds map
            addWorldToServer(server, dimensionKey, serverWorld);

            // Step 4: Initialize the world
            initializeServerWorld(serverWorld);

            System.out.println("Successfully registered ServerWorld: " + dimensionKey.getValue());

        } catch (Exception e) {
            System.err.println("Failed to register dimension with server: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("ServerWorld creation failed", e);
        }
    }

    /**
     * Gather all parameters needed for ServerWorld constructor
     */
    private static ServerWorldParameters gatherServerWorldParameters(MinecraftServer server,
                                                                     RegistryKey<World> dimensionKey,
                                                                     DimensionOptions dimensionOptions) throws Exception {

        // MinecraftServer IS an Executor in 1.20.1 (extends ReentrantThreadExecutor)
        Executor workerExecutor = server;
        boolean debugWorld = false;
        boolean shouldTickTime = true;

        // Get seed - we'll use the world seed for now, individual planets could have different seeds
        long seed = server.getSaveProperties().getGeneratorOptions().getSeed();

        // Get level storage session
        LevelStorage.Session session = getLevelStorageSession(server);

        // Create world properties - FIXED: Get actual ServerWorldProperties
        ServerWorldProperties properties = getServerWorldProperties(server, dimensionKey, seed);

        // Create world generation progress listener - FIXED: Use null to avoid hangs
        // If this still hangs, try: createNoOpProgressListener() or createMinimalProgressListener()
        WorldGenerationProgressListener progressListener = createProgressListener();

        // Get default spawners list - FIXED: Simplified to avoid constructor issues
        List<Spawner> spawners = getDefaultSpawners();

        // RandomSequencesState can be null initially
        RandomSequencesState randomSequencesState = null;

        return new ServerWorldParameters(
                server, workerExecutor, session, properties, dimensionKey,
                dimensionOptions, progressListener, debugWorld, seed,
                spawners, shouldTickTime, randomSequencesState
        );
    }

    /**
     * Get the level storage session from the server
     */
    private static LevelStorage.Session getLevelStorageSession(MinecraftServer server) throws Exception {
        // Try to access the session from an existing world first
        ServerWorld overworld = server.getOverworld();
        if (overworld != null) {
            // Use reflection to get the session from an existing world
            try {
                Field sessionField = ServerWorld.class.getDeclaredField("session");
                sessionField.setAccessible(true);
                LevelStorage.Session session = (LevelStorage.Session) sessionField.get(overworld);

                if (session != null) {
                    System.out.println("✓ Retrieved session from overworld");
                    return session;
                }
            } catch (Exception e) {
                System.out.println("Could not get session via reflection: " + e.getMessage());
            }
        }

        // Fallback: try to get session through other means
        try {
            // Try to find the session in the server's fields
            Field[] fields = MinecraftServer.class.getDeclaredFields();
            for (Field field : fields) {
                if (field.getType().equals(LevelStorage.Session.class)) {
                    field.setAccessible(true);
                    LevelStorage.Session session = (LevelStorage.Session) field.get(server);
                    if (session != null) {
                        System.out.println("✓ Retrieved session from server field: " + field.getName());
                        return session;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Could not find session in server fields: " + e.getMessage());
        }

        throw new RuntimeException("Could not obtain LevelStorage.Session - this is required for ServerWorld creation");
    }

    /**
     * Get actual ServerWorldProperties from the overworld using reflection
     * FIXED: This was the main issue - we need ServerWorldProperties, not WorldProperties
     */
    private static ServerWorldProperties getServerWorldProperties(MinecraftServer server,
                                                                  RegistryKey<World> dimensionKey,
                                                                  long seed) throws Exception {

        ServerWorld overworld = server.getOverworld();
        if (overworld != null) {
            try {
                // Try to get the properties field directly from ServerWorld
                Field propertiesField = ServerWorld.class.getDeclaredField("properties");
                propertiesField.setAccessible(true);
                Object properties = propertiesField.get(overworld);

                if (properties instanceof ServerWorldProperties) {
                    System.out.println("✓ Retrieved ServerWorldProperties via reflection");
                    return (ServerWorldProperties) properties;
                }
            } catch (Exception e) {
                System.out.println("Could not get properties field directly: " + e.getMessage());
            }

            // Fallback: Search all fields for ServerWorldProperties
            try {
                Field[] fields = ServerWorld.class.getDeclaredFields();
                for (Field field : fields) {
                    if (ServerWorldProperties.class.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        Object properties = field.get(overworld);
                        if (properties != null) {
                            System.out.println("✓ Retrieved ServerWorldProperties from field: " + field.getName());
                            return (ServerWorldProperties) properties;
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Could not find ServerWorldProperties in fields: " + e.getMessage());
            }
        }

        throw new RuntimeException("Could not obtain ServerWorldProperties - this is required for ServerWorld creation");
    }

    /**
     * Create a progress listener for world generation
     * FIXED: Use null to avoid hanging - most Minecraft code handles this gracefully
     */
    private static WorldGenerationProgressListener createProgressListener() {
        // SAFEST APPROACH: Return null
        // Most Minecraft systems handle null progress listeners gracefully
        System.out.println("Using null progress listener to avoid hangs");
        return null;
    }

    /**
     * Alternative: Create a minimal no-op progress listener if null doesn't work
     */
    private static WorldGenerationProgressListener createNoOpProgressListener() {
        return new WorldGenerationProgressListener() {
            @Override
            public void start(ChunkPos spawnPos) {
                // Completely empty - no operations that could cause hangs
            }

            @Override
            public void setChunkStatus(ChunkPos pos, ChunkStatus status) {
                // Completely empty - no operations that could cause hangs
            }

            @Override
            public void start() {
                // Completely empty - no operations that could cause hangs
            }

            @Override
            public void stop() {
                // Completely empty - no operations that could cause hangs
            }
        };
    }

    /**
     * Another alternative: Try to use a simple logging progress listener
     */
    private static WorldGenerationProgressListener createMinimalProgressListener() {
        return new WorldGenerationProgressListener() {
            @Override
            public void start(ChunkPos spawnPos) {
                // Just log, don't do any complex operations
                try {
                    System.out.println("World generation started at: " + spawnPos);
                } catch (Exception e) {
                    // Ignore any exceptions to prevent hangs
                }
            }

            @Override
            public void setChunkStatus(ChunkPos pos, ChunkStatus status) {
                // Do nothing - this method might be called frequently and cause issues
            }

            @Override
            public void start() {
                try {
                    System.out.println("World generation started");
                } catch (Exception e) {
                    // Ignore any exceptions
                }
            }

            @Override
            public void stop() {
                try {
                    System.out.println("World generation completed");
                } catch (Exception e) {
                    // Ignore any exceptions
                }
            }
        };
    }

    /**
     * Get default mob spawners list
     * FIXED: Simplified to avoid constructor issues with spawners that need ServerWorldProperties
     */
    private static List<Spawner> getDefaultSpawners() {
        // Only use spawners that have simple constructors
        return List.of(
                new PhantomSpawner(),
                new PatrolSpawner(),
                new CatSpawner()
                // Removed ZombieSiegeManager and WanderingTraderManager as they require ServerWorldProperties
                // These can be added later once we have a proper way to construct them
        );
    }

    /**
     * Create the actual ServerWorld instance
     */
    private static ServerWorld createServerWorld(ServerWorldParameters params) {
        System.out.println("Creating ServerWorld instance...");

        ServerWorld serverWorld = new ServerWorld(
                params.server,
                params.workerExecutor,
                params.session,
                params.properties,
                params.dimensionKey,
                params.dimensionOptions,
                new WorldGenerationProgressListener() {
                    @Override
                    public void start(ChunkPos spawnPos) {

                    }

                    @Override
                    public void setChunkStatus(ChunkPos pos, @Nullable ChunkStatus status) {

                    }

                    @Override
                    public void start() {

                    }

                    @Override
                    public void stop() {

                    }
                },
                params.debugWorld,
                params.seed,
                params.spawners,
                params.shouldTickTime,
                params.randomSequencesState
        );

        System.out.println("✓ ServerWorld instance created successfully");
        return serverWorld;
    }

    /**
     * Add the ServerWorld to the server's internal worlds map
     */
    private static void addWorldToServer(MinecraftServer server, RegistryKey<World> dimensionKey,
                                         ServerWorld serverWorld) throws Exception {

        System.out.println("Adding ServerWorld to server registry...");

        // Try to find the worlds field using reflection
        Field worldsField = null;
        Class<?> currentClass = MinecraftServer.class;

        while (currentClass != null && worldsField == null) {
            try {
                Field[] fields = currentClass.getDeclaredFields();
                for (Field field : fields) {
                    // Look for a Map field that could contain worlds
                    if (Map.class.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        Object fieldValue = field.get(server);

                        if (fieldValue instanceof Map) {
                            Map<?, ?> map = (Map<?, ?>) fieldValue;

                            // Check if this map contains RegistryKey<World> keys
                            if (!map.isEmpty()) {
                                Object firstKey = map.keySet().iterator().next();
                                if (firstKey instanceof RegistryKey<?>) {
                                    // This is likely our worlds map
                                    worldsField = field;
                                    System.out.println("✓ Found worlds map field: " + field.getName());
                                    break;
                                }
                            } else {
                                // Empty map, might still be the right one
                                // Check field name or type for clues
                                if (field.getName().toLowerCase().contains("world")) {
                                    worldsField = field;
                                    System.out.println("✓ Found potential worlds map field: " + field.getName());
                                    break;
                                }
                            }
                        }
                    }
                }
                currentClass = currentClass.getSuperclass();
            } catch (Exception e) {
                System.out.println("Error searching for worlds field in " + currentClass.getName() + ": " + e.getMessage());
                currentClass = currentClass.getSuperclass();
            }
        }

        if (worldsField == null) {
            throw new RuntimeException("Could not find server worlds Map field via reflection");
        }

        // Add our world to the map
        try {
            @SuppressWarnings("unchecked")
            Map<RegistryKey<World>, ServerWorld> worlds = (Map<RegistryKey<World>, ServerWorld>) worldsField.get(server);
            worlds.put(dimensionKey, serverWorld);
            System.out.println("✓ Added ServerWorld to server worlds map");

            // Verify it was added
            if (worlds.containsKey(dimensionKey)) {
                System.out.println("✓ Verified ServerWorld is in map with key: " + dimensionKey.getValue());
            } else {
                throw new RuntimeException("ServerWorld was not properly added to worlds map");
            }

        } catch (ClassCastException e) {
            throw new RuntimeException("Server worlds field is not the expected Map type", e);
        }
    }

    /**
     * Initialize the newly created ServerWorld
     */
    private static void initializeServerWorld(ServerWorld serverWorld) {
        System.out.println("Initializing ServerWorld...");

        try {
            // Basic initialization
            serverWorld.calculateAmbientDarkness();

            // Initialize world border (copy from overworld settings)
            serverWorld.getWorldBorder().setMaxRadius(30000000); // Default max radius

            System.out.println("✓ ServerWorld initialization completed");

        } catch (Exception e) {
            System.err.println("Warning: Some ServerWorld initialization failed: " + e.getMessage());
            // Don't throw here - world might still be usable
        }
    }

    /**
     * Teleport player to a planet dimension
     */
    public static boolean teleportPlayerToPlanet(ServerPlayerEntity player, String planetName) {
        RegistryKey<World> dimensionKey = planetDimensions.get(planetName);
        if (dimensionKey == null) {
            System.err.println("Planet dimension not found: " + planetName);
            return false;
        }

        MinecraftServer server = player.getServer();
        if (server == null) {
            System.err.println("Server is null");
            return false;
        }

        ServerWorld targetWorld = server.getWorld(dimensionKey);
        if (targetWorld == null) {
            System.err.println("Target world is null for dimension: " + dimensionKey);
            return false;
        }

        try {
            // Calculate safe spawn position
            Vec3d spawnPos = calculateSafeSpawnPosition(targetWorld, planetName);

            // Create teleport target
            TeleportTarget teleportTarget = new TeleportTarget(spawnPos, Vec3d.ZERO, player.getYaw(), player.getPitch());

            // Teleport using Fabric's dimension API
            FabricDimensions.teleport(player, targetWorld, teleportTarget);

            System.out.println("Teleported " + player.getGameProfile().getName() + " to planet " + planetName);
            return true;

        } catch (Exception e) {
            System.err.println("Failed to teleport player to planet " + planetName + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Calculate a safe spawn position on the planet
     */
    private static Vec3d calculateSafeSpawnPosition(ServerWorld world, String planetName) {
        PlanetModel planetModel = planetModels.get(planetName);
        if (planetModel == null) {
            // Default spawn position
            return new Vec3d(0, 100, 0);
        }

        // Get the chunk generator to calculate proper spawn height
        if (world.getChunkManager().getChunkGenerator() instanceof PlanetChunkGenerator planetGenerator) {
            int spawnHeight = planetGenerator.getSpawnHeight(world);
            return new Vec3d(0, spawnHeight, 0);
        }

        // Fallback
        int seaLevel = planetModel.getTerrainConfig().getHeightSettings().seaLevel;
        return new Vec3d(0, seaLevel + 10, 0);
    }

    /**
     * Get planet model by name
     */
    public static PlanetModel getPlanetModel(String planetName) {
        return planetModels.get(planetName);
    }

    /**
     * Get dimension key by planet name
     */
    public static RegistryKey<World> getDimensionKey(String planetName) {
        return planetDimensions.get(planetName);
    }

    /**
     * Check if a planet dimension exists
     */
    public static boolean planetDimensionExists(String planetName) {
        return planetDimensions.containsKey(planetName);
    }

    /**
     * Get all created planet names
     */
    public static String[] getAllPlanetNames() {
        return planetDimensions.keySet().toArray(new String[0]);
    }

    /**
     * Remove a planet dimension (for cleanup)
     */
    public static boolean removePlanetDimension(String planetName) {
        // TODO: Implement proper dimension cleanup
        RegistryKey<World> removed = planetDimensions.remove(planetName);
        PlanetModel model = planetModels.remove(planetName);

        if (removed != null && model != null) {
            System.out.println("Removed planet dimension: " + planetName);
            return true;
        }

        return false;
    }

    /**
     * Test method to verify our ServerWorld creation works
     * Call this from your planet creation command for testing
     */
    public static boolean testServerWorldCreation(MinecraftServer server) {
        try {
            System.out.println("=== TESTING SERVERWORLD CREATION ===");

            // Create a test dimension key
            Identifier testId = new Identifier("terradyne", "test_dimension");
            RegistryKey<World> testKey = RegistryKey.of(RegistryKeys.WORLD, testId);

            // Create basic dimension options (using overworld generator for testing)
            ServerWorld overworld = server.getOverworld();
            DimensionOptions testOptions = new DimensionOptions(
                    server.getRegistryManager().get(RegistryKeys.DIMENSION_TYPE).entryOf(PlanetDimensionType.PLANET_DIMENSION_TYPE),
                    overworld.getChunkManager().getChunkGenerator()
            );

            // Test the creation process
            registerDimensionWithServer(server, testKey, testOptions);

            // Verify the world was created
            ServerWorld testWorld = server.getWorld(testKey);
            if (testWorld != null) {
                System.out.println("✓ Test ServerWorld creation SUCCESSFUL!");
                System.out.println("✓ World accessible via server.getWorld()");
                System.out.println("✓ Dimension: " + testWorld.getRegistryKey().getValue());
                return true;
            } else {
                System.err.println("✗ Test FAILED: ServerWorld not accessible via server.getWorld()");
                return false;
            }

        } catch (Exception e) {
            System.err.println("✗ Test FAILED with exception: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Debug method to inspect server's current worlds
     */
    public static void debugServerWorlds(MinecraftServer server) {
        try {
            System.out.println("=== SERVER WORLDS DEBUG ===");

            // Try to access the worlds map via reflection
            Field[] fields = MinecraftServer.class.getDeclaredFields();

            for (Field field : fields) {
                if (Map.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    Object fieldValue = field.get(server);

                    if (fieldValue instanceof Map) {
                        Map<?, ?> map = (Map<?, ?>) fieldValue;

                        if (!map.isEmpty()) {
                            Object firstKey = map.keySet().iterator().next();
                            if (firstKey instanceof RegistryKey<?>) {
                                System.out.println("Found worlds map in field: " + field.getName());
                                System.out.println("Map size: " + map.size());

                                for (Object key : map.keySet()) {
                                    if (key instanceof RegistryKey<?>) {
                                        RegistryKey<?> regKey = (RegistryKey<?>) key;
                                        Object world = map.get(key);
                                        System.out.println("  - " + regKey.getValue() + " -> " + world.getClass().getSimpleName());
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Also test the standard getWorld method
            System.out.println("\nTesting server.getWorld() for known dimensions:");
            testGetWorld(server, World.OVERWORLD, "Overworld");
            testGetWorld(server, World.NETHER, "Nether");
            testGetWorld(server, World.END, "End");

        } catch (Exception e) {
            System.err.println("Debug failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testGetWorld(MinecraftServer server, RegistryKey<World> worldKey, String name) {
        ServerWorld world = server.getWorld(worldKey);
        System.out.println("  " + name + " (" + worldKey.getValue() + "): " +
                (world != null ? "✓ Available" : "✗ Not found"));
    }

    /**
     * Get debug information about all dimensions
     */
    public static String getDebugInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== PLANET DIMENSION MANAGER DEBUG ===\n");
        sb.append("Active planet dimensions: ").append(planetDimensions.size()).append("\n");

        if (planetDimensions.isEmpty()) {
            sb.append("No planet dimensions created yet.\n");
            sb.append("Use '/terradyne create <planet_type>' to create a planet.\n");
            return sb.toString();
        }

        for (Map.Entry<String, RegistryKey<World>> entry : planetDimensions.entrySet()) {
            String planetName = entry.getKey();
            RegistryKey<World> dimensionKey = entry.getValue();
            PlanetModel model = planetModels.get(planetName);

            sb.append("\nPlanet: ").append(planetName).append("\n");
            sb.append("  Dimension ID: ").append(dimensionKey.getValue()).append("\n");

            if (model != null) {
                PlanetData data = model.getPlanetData();
                sb.append("  Crust: ").append(data.getCrustComposition()).append("\n");
                sb.append("  Atmosphere: ").append(data.getAtmosphereComposition()).append("\n");
                sb.append("  Temperature: ").append(String.format("%.1f°C", data.getAverageSurfaceTemp())).append("\n");
                sb.append("  Habitability: ").append(String.format("%.2f", data.getHabitability())).append("\n");

                // Sky color info
                int skyColor = PlanetDimensionType.calculateSkyColor(data);
                sb.append("  Sky Color: #").append(String.format("%06X", skyColor)).append("\n");
            } else {
                sb.append("  Model: ✗ Not available\n");
            }
        }

        return sb.toString();
    }

    /**
     * Initialize the dimension manager (called during mod initialization)
     */
    public static void initialize() {
        System.out.println("Initialized PlanetDimensionManager");

        // TODO: Register any necessary event listeners
        // TODO: Set up dimension persistence/loading
    }

    /**
     * Helper class to hold all ServerWorld constructor parameters
     */
    private static class ServerWorldParameters {
        final MinecraftServer server;
        final Executor workerExecutor;
        final LevelStorage.Session session;
        final ServerWorldProperties properties;  // Fixed: Correct type
        final RegistryKey<World> dimensionKey;
        final DimensionOptions dimensionOptions;
        final WorldGenerationProgressListener progressListener;
        final boolean debugWorld;
        final long seed;
        final List<Spawner> spawners;
        final boolean shouldTickTime;
        final RandomSequencesState randomSequencesState;

        ServerWorldParameters(MinecraftServer server, Executor workerExecutor, LevelStorage.Session session,
                              ServerWorldProperties properties, RegistryKey<World> dimensionKey,
                              DimensionOptions dimensionOptions, WorldGenerationProgressListener progressListener,
                              boolean debugWorld, long seed, List<Spawner> spawners, boolean shouldTickTime,
                              RandomSequencesState randomSequencesState) {
            this.server = server;
            this.workerExecutor = workerExecutor;
            this.session = session;
            this.properties = properties;
            this.dimensionKey = dimensionKey;
            this.dimensionOptions = dimensionOptions;
            this.progressListener = progressListener;
            this.debugWorld = debugWorld;
            this.seed = seed;
            this.spawners = spawners;
            this.shouldTickTime = shouldTickTime;
            this.randomSequencesState = randomSequencesState;
        }
    }
}