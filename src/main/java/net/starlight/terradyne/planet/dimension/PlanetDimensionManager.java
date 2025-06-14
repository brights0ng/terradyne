package net.starlight.terradyne.planet.dimension;

import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.RandomSequencesState;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.dimension.DimensionTypes;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.server.WorldGenerationProgressListener;
import net.starlight.terradyne.Terradyne;
import net.starlight.terradyne.planet.biome.PhysicsBiomeSource;
import net.starlight.terradyne.planet.chunk.UniversalChunkGenerator;
import net.starlight.terradyne.planet.physics.IPlanetModel;
import net.starlight.terradyne.planet.physics.PlanetPhysicsModel;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * Manages planet dimensions and world creation
 * Simplified for physics-based generation
 */
public class PlanetDimensionManager {
    
    // Storage for created planets
    private static final Map<String, RegistryKey<World>> PLANET_DIMENSIONS = new ConcurrentHashMap<>();
    private static final Map<String, ServerWorld> PLANET_WORLDS = new ConcurrentHashMap<>();
    private static final Map<String, IPlanetModel> PLANET_MODELS = new ConcurrentHashMap<>();
    
    /**
     * Create a physics-based planet
     */
    public static RegistryKey<World> createPhysicsPlanet(MinecraftServer server, PlanetPhysicsModel model) {
        String planetName = model.getConfig().getPlanetName().toLowerCase().replace(" ", "_");
        
        Terradyne.LOGGER.info("=== CREATING PHYSICS PLANET ===");
        Terradyne.LOGGER.info("Planet: {}", planetName);
        Terradyne.LOGGER.info("Tectonic Scale: {}", model.getConfig().getTectonicScale());
        Terradyne.LOGGER.info("Type: {} (emergent)", model.getType().getDisplayName());
        
        if (PLANET_DIMENSIONS.containsKey(planetName)) {
            Terradyne.LOGGER.warn("Planet {} already exists", planetName);
            return PLANET_DIMENSIONS.get(planetName);
        }
        
        try {
            // Create dimension identifier
            Identifier dimensionId = new Identifier(Terradyne.MOD_ID, planetName);
            RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, dimensionId);

            Terradyne.LOGGER.info("Attempting to create dimension type...");

            // Get or create dimension type
            RegistryEntry<DimensionType> dimensionType = getOrCreateDimensionType(server, model);
            Terradyne.LOGGER.info("Attempting to create biome source...");

            // Create biome source
            PhysicsBiomeSource biomeSource = new PhysicsBiomeSource(model, server);
            Terradyne.LOGGER.info("Attempting to create chunk generator...");

            // Create chunk generator
            UniversalChunkGenerator chunkGenerator = new UniversalChunkGenerator(model, biomeSource);
            Terradyne.LOGGER.info("Attempting to create dimension options...");

            // Create dimension options
            DimensionOptions dimensionOptions = new DimensionOptions(dimensionType, chunkGenerator);
            Terradyne.LOGGER.info("Attempting to create server world...");

            // Create the server world
            ServerWorld serverWorld = createServerWorld(server, worldKey, dimensionOptions);
            Terradyne.LOGGER.info("Attempting to store references...");

            // Store references
            PLANET_DIMENSIONS.put(planetName, worldKey);
            PLANET_WORLDS.put(planetName, serverWorld);
            PLANET_MODELS.put(planetName, model);
            
            Terradyne.LOGGER.info("✅ Physics planet '{}' created successfully", planetName);
            
            return worldKey;
            
        } catch (Exception e) {
            Terradyne.LOGGER.error("Failed to create physics planet: {}", planetName, e);
            throw new RuntimeException("Planet creation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get or create appropriate dimension type for the planet
     */
    private static RegistryEntry<DimensionType> getOrCreateDimensionType(MinecraftServer server, PlanetPhysicsModel model) {
        Registry<DimensionType> registry = server.getRegistryManager().get(RegistryKeys.DIMENSION_TYPE);
        
        // For now, use overworld dimension type
        // Later we can create custom dimension types based on physics
        return registry.getEntry(DimensionTypes.OVERWORLD).orElseThrow();
    }
    
    /**
     * Create the server world
     */
    private static ServerWorld createServerWorld(MinecraftServer server, RegistryKey<World> worldKey,
                                                 DimensionOptions dimensionOptions) {
        try {
            ServerWorld overworld = server.getWorld(World.OVERWORLD);
            if (overworld == null) {
                throw new RuntimeException("Overworld not found");
            }
            
            Terradyne.LOGGER.info("Creating ServerWorld for dimension {}", worldKey.getValue());


            // Create the world NOTE: THIS LINE IN CREATING THE SERVERWORLD IS WHERE IT FREEZES; THE LOG AFTERWARDS NEVER POPS UP
            ServerWorld world = new ServerWorld(
                    server,
                    Runnable::run,
                    getSession(server),
                    server.getSaveProperties().getMainWorldProperties(),
                    worldKey,
                    dimensionOptions,
                    createProgressListener(),
                    false,  // isDebugWorld
                    overworld.getSeed(),
                    List.of(), // spawners
                    false, // shouldTickTime
                    new RandomSequencesState(overworld.getSeed())
            );

            // Register with server
            registerWorldWithServer(server, worldKey, world);

            // Initialize spawn chunks
            initializeWorld(world);

            return world;
            
        } catch (Exception e) {
            Terradyne.LOGGER.error("Failed to create ServerWorld", e);
            throw new RuntimeException("ServerWorld creation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Teleport a player to a planet
     */
    public static void teleportToPlanet(ServerPlayerEntity player, String planetName) {
        String normalizedName = planetName.toLowerCase().replace(" ", "_");
        ServerWorld targetWorld = PLANET_WORLDS.get(normalizedName);
        
        if (targetWorld == null) {
            throw new RuntimeException("Planet not found: " + planetName);
        }
        
        player.getServer().execute(() -> {
            try {
                Vec3d spawnPos = new Vec3d(0.5, 100, 0.5); // Spawn high to avoid suffocation
                
                // Force load spawn chunks
                int chunkX = 0;
                int chunkZ = 0;
                for (int x = -2; x <= 2; x++) {
                    for (int z = -2; z <= 2; z++) {
                        targetWorld.setChunkForced(chunkX + x, chunkZ + z, true);
                        targetWorld.getChunk(chunkX + x, chunkZ + z);
                    }
                }
                
                // Teleport player
                player.teleport(targetWorld, spawnPos.x, spawnPos.y, spawnPos.z, 0, 0);
                
                Terradyne.LOGGER.info("✅ Teleported {} to planet {}", player.getName().getString(), planetName);
                
            } catch (Exception e) {
                Terradyne.LOGGER.error("Teleportation failed", e);
            }
        });
    }
    
    /**
     * Check if a planet exists
     */
    public static boolean planetExists(String planetName) {
        String normalizedName = planetName.toLowerCase().replace(" ", "_");
        return PLANET_DIMENSIONS.containsKey(normalizedName);
    }
    
    /**
     * Get planet model
     */
    public static IPlanetModel getPlanetModel(String planetName) {
        String normalizedName = planetName.toLowerCase().replace(" ", "_");
        return PLANET_MODELS.get(normalizedName);
    }
    
    /**
     * List all created planets
     */
    public static String listPlanets() {
        if (PLANET_DIMENSIONS.isEmpty()) {
            return "No planets created yet";
        }
        
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, IPlanetModel> entry : PLANET_MODELS.entrySet()) {
            String name = entry.getKey();
            IPlanetModel model = entry.getValue();
            sb.append(name).append(" (").append(model.getType().getDisplayName()).append("), ");
        }
        
        if (sb.length() > 2) {
            sb.setLength(sb.length() - 2);
        }
        
        return sb.toString();
    }
    
    // === PRIVATE HELPER METHODS ===
    
    private static Executor getExecutor(MinecraftServer server) {

        return task -> server.execute(task);
    }
    
    private static LevelStorage.Session getSession(MinecraftServer server) {
        Terradyne.LOGGER.info("Getting session...");
        try {
            // Try common field names
            String[] fieldNames = {"session", "levelStorage", "saveHandler"};
            
            for (String fieldName : fieldNames) {
                try {
                    java.lang.reflect.Field field = MinecraftServer.class.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    Object value = field.get(server);
                    
                    if (value instanceof LevelStorage.Session) {
                        return (LevelStorage.Session) value;
                    }
                } catch (NoSuchFieldException ignored) {}
            }
            
            // Search by type
            java.lang.reflect.Field[] fields = MinecraftServer.class.getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                if (LevelStorage.Session.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    return (LevelStorage.Session) field.get(server);
                }
            }
            
            throw new RuntimeException("Could not find LevelStorage.Session field");
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to get session: " + e.getMessage(), e);
        }
    }
    
    @SuppressWarnings("unchecked")
    private static void registerWorldWithServer(MinecraftServer server, RegistryKey<World> worldKey, ServerWorld world) {
        try {
            // Find the worlds map in the server
            java.lang.reflect.Field[] fields = MinecraftServer.class.getDeclaredFields();
            
            for (java.lang.reflect.Field field : fields) {
                if (field.getType() == Map.class) {
                    field.setAccessible(true);
                    Object fieldValue = field.get(server);
                    
                    if (fieldValue instanceof Map<?, ?> map && !map.isEmpty()) {
                        Object firstKey = map.keySet().iterator().next();
                        
                        if (firstKey instanceof RegistryKey<?>) {
                            Map<RegistryKey<World>, ServerWorld> worldsMap = (Map<RegistryKey<World>, ServerWorld>) map;
                            worldsMap.put(worldKey, world);
                            Terradyne.LOGGER.info("✅ Registered world with server");
                            return;
                        }
                    }
                }
            }
            
            Terradyne.LOGGER.warn("Could not find worlds map in server");
            
        } catch (Exception e) {
            Terradyne.LOGGER.error("Failed to register world with server", e);
        }
    }
    
    private static void initializeWorld(ServerWorld world) {
        try {
            // Pre-generate spawn chunks
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    world.getChunk(x, z);
                }
            }
            
            Terradyne.LOGGER.info("Initialized spawn area for world");
            
        } catch (Exception e) {
            Terradyne.LOGGER.warn("Failed to initialize world: {}", e.getMessage());
        }
    }
    
    private static WorldGenerationProgressListener createProgressListener() {
        Terradyne.LOGGER.info("Creating PL...");
        return new WorldGenerationProgressListener() {
            @Override public void start(net.minecraft.util.math.ChunkPos spawnPos) {}
            @Override public void setChunkStatus(net.minecraft.util.math.ChunkPos pos, net.minecraft.world.chunk.ChunkStatus status) {}
            @Override public void start() {}
            @Override public void stop() {}
        };
    }
}