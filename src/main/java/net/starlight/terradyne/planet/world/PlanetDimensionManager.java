// PlanetDimensionManager.java
package net.starlight.terradyne.planet.world;

import net.fabricmc.fabric.api.dimension.v1.FabricDimensions;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionOptions;

import net.starlight.terradyne.planet.physics.PlanetModel;
import net.starlight.terradyne.planet.physics.PlanetData;
import net.starlight.terradyne.planet.world.biome.PlanetBiomeSource;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages planetary dimensions - creation, registration, and player teleportation.
 * Handles the lifecycle of planet dimensions within the Minecraft server.
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
     * Register dimension with the server (Fabric-specific implementation)
     */
    private static void registerDimensionWithServer(MinecraftServer server, RegistryKey<World> dimensionKey, 
                                                   DimensionOptions dimensionOptions) {
        // This is where we'd integrate with Fabric's dimension APIs
        // For now, this is a placeholder - the exact implementation depends on Fabric's current API
        // TODO: Implement proper Fabric dimension registration
        
        System.out.println("Registering dimension with server: " + dimensionKey.getValue());
        
        // The actual dimension registration will likely involve:
        // 1. Adding to the server's dimension registry
        // 2. Creating the ServerWorld instance
        // 3. Adding to the server's world map
        
        // This is a simplified approach - in practice, this might require
        // hooking into Fabric's dimension creation events or APIs
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
     * Get debug information about all dimensions
     */
    public static String getDebugInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== PLANET DIMENSION MANAGER DEBUG ===\n");
        sb.append("Active planet dimensions: ").append(planetDimensions.size()).append("\n");
        
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
                
                // Sky color info
                int skyColor = PlanetDimensionType.calculateSkyColor(data);
                sb.append("  Sky Color: #").append(String.format("%06X", skyColor)).append("\n");
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
}