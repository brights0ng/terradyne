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
import net.starlight.terradyne.Terradyne;
import net.starlight.terradyne.planet.physics.PlanetModel;
import net.starlight.terradyne.planet.world.WorldPlanetManager;

/**
 * Manages planet dimension access and teleportation
 * NO LONGER creates dimensions - that's handled by WorldPlanetManager
 * Focuses purely on accessing existing dimensions and teleportation
 */
public class PlanetDimensionManager {

    /**
     * Teleport player to planet
     * Planet must already exist (registered during world creation)
     */
    public static void teleportToPlanet(ServerPlayerEntity player, String planetName) {
        String normalizedName = planetName.toLowerCase().replace(" ", "_");
        MinecraftServer server = player.getServer();

        // Get the world from server's existing dimensions
        Identifier dimensionId = new Identifier(Terradyne.MOD_ID, normalizedName);
        RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, dimensionId);
        ServerWorld targetWorld = server.getWorld(worldKey);

        if (targetWorld == null) {
            throw new RuntimeException("Planet not found: " + planetName +
                    ". Make sure the planet was registered during world creation.");
        }

        // Ensure everything runs on the main server thread
        server.execute(() -> {
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
     * Check if planet exists in the server's dimension registry
     */
    public static boolean planetExists(MinecraftServer server, String planetName) {
        String normalizedName = planetName.toLowerCase().replace(" ", "_");
        Identifier dimensionId = new Identifier(Terradyne.MOD_ID, normalizedName);
        RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, dimensionId);
        return server.getWorld(worldKey) != null;
    }

    /**
     * List all Terradyne planets available on the server
     */
    public static String listPlanets(MinecraftServer server) {
        var terradynePlanets = new java.util.ArrayList<String>();

        // server.getWorlds() returns Iterable<ServerWorld>, convert to list first
        for (ServerWorld world : server.getWorlds()) {
            if (world.getRegistryKey().getValue().getNamespace().equals(Terradyne.MOD_ID)) {
                terradynePlanets.add(world.getRegistryKey().getValue().getPath());
            }
        }

        if (terradynePlanets.isEmpty()) {
            return "No Terradyne planets found";
        }

        StringBuilder sb = new StringBuilder();
        for (String planetName : terradynePlanets) {
            PlanetModel model = WorldPlanetManager.getPlanetModel(server, planetName);
            String classification = (model != null) ? model.getPlanetClassification() : "Unknown";
            sb.append(planetName).append(" (").append(classification).append("), ");
        }

        if (sb.length() > 2) {
            sb.setLength(sb.length() - 2);
        }

        return sb.toString();
    }

    /**
     * Get planet model by dimension ID (for UniversalChunkGenerator codec)
     */
    public static PlanetModel getPlanetModelByDimension(MinecraftServer server, Identifier dimensionId) {
        return WorldPlanetManager.getPlanetModelByDimension(server, dimensionId);
    }

    // === BIOME SOURCE CREATION (kept for WorldPlanetManager) ===

    /**
     * Create biome source for planet (used by WorldPlanetManager)
     */
    public static BiomeSource createPlanetBiomeSource(MinecraftServer server, PlanetModel planetModel) {
        Terradyne.LOGGER.debug("Creating biome source for planet: {}", planetModel.getConfig().getPlanetName());

        // Get Plains biome from registry
        Registry<Biome> biomeRegistry = server.getRegistryManager().get(RegistryKeys.BIOME);
        RegistryEntry<Biome> plainsBiome = biomeRegistry.getEntry(BiomeKeys.PLAINS)
                .orElseThrow(() -> new RuntimeException("Plains biome not found in registry"));

        Terradyne.LOGGER.debug("✅ Using Plains biome for terrain (physics-based terrain with vanilla biome)");

        return new FixedBiomeSource(plainsBiome);
    }

    // === LEGACY METHODS (preserved for compatibility) ===

    /**
     * Get planet model by name (delegates to WorldPlanetManager)
     */
    public static PlanetModel getPlanetModelByName(MinecraftServer server, String planetName) {
        return WorldPlanetManager.getPlanetModel(server, planetName);
    }

    /**
     * Get planet model for dimension (delegates to WorldPlanetManager)
     */
    public static PlanetModel getPlanetModel(String planetName) {
        // This method is used by UniversalChunkGenerator codec
        // We need a way to get the current server instance
        // For now, return null and let the codec handle it
        Terradyne.LOGGER.warn("getPlanetModel(String) called without server context - returning null");
        return null;
    }
}