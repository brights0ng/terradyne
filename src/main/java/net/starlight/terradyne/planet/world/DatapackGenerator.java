package net.starlight.terradyne.planet.world;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import net.starlight.terradyne.Terradyne;
import net.starlight.terradyne.planet.config.ExistingPlanetRegistry;
import net.starlight.terradyne.planet.config.PlanetConfigLoader;
import net.starlight.terradyne.planet.physics.PlanetConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Generates Terradyne datapack with planet dimensions
 * Clean replacement for level.dat modification and mixin injection
 */
public class DatapackGenerator {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    /**
     * Generate Terradyne datapack for a world if needed
     * Called during server startup before dimension loading
     * SIMPLIFIED: No registry dependency
     */
    public static void generateDatapackIfNeeded(MinecraftServer server) {
        try {
            Path worldDir = server.getSavePath(net.minecraft.util.WorldSavePath.ROOT);
            Path datapackDir = worldDir.resolve("datapacks").resolve("terradyne");

            // Check if datapack already exists
            if (Files.exists(datapackDir) && Files.exists(datapackDir.resolve("pack.mcmeta"))) {
                Terradyne.LOGGER.info("Terradyne datapack already exists - checking if update needed");

                // TODO: In future, check if planet configs changed and regenerate if needed
                // For now, assume existing datapack is fine
                return;
            }

            // Load planet configurations directly
            Map<String, PlanetConfig> planetConfigs = PlanetConfigLoader.loadAllPlanetConfigs(server);

            if (planetConfigs.isEmpty()) {
                Terradyne.LOGGER.info("No planet configurations found - skipping datapack generation");
                return;
            }

            Terradyne.LOGGER.info("Generating Terradyne datapack with {} planets...", planetConfigs.size());
            Terradyne.LOGGER.info("Using self-contained chunk generators - no registry required");

            // Create datapack structure
            createDatapackStructure(datapackDir);
            generatePackMcmeta(datapackDir);
            generateDimensionFiles(datapackDir, planetConfigs);

            // Register planets in our tracking system
            registerGeneratedPlanets(server, planetConfigs);

            Terradyne.LOGGER.info("✅ Terradyne datapack generated successfully!");

        } catch (Exception e) {
            Terradyne.LOGGER.error("Failed to generate Terradyne datapack", e);
        }
    }

    /**
     * Create the basic datapack directory structure
     */
    private static void createDatapackStructure(Path datapackDir) throws IOException {
        Files.createDirectories(datapackDir);
        Files.createDirectories(datapackDir.resolve("data").resolve("terradyne").resolve("dimension"));

        Terradyne.LOGGER.debug("Created datapack structure: {}", datapackDir);
    }

    /**
     * Generate pack.mcmeta file
     */
    private static void generatePackMcmeta(Path datapackDir) throws IOException {
        JsonObject packMeta = new JsonObject();
        JsonObject pack = new JsonObject();

        pack.addProperty("pack_format", 15); // Minecraft 1.20.1 datapack format
        pack.addProperty("description", "Terradyne Planetary Dimensions");

        packMeta.add("pack", pack);

        Path packMetaFile = datapackDir.resolve("pack.mcmeta");
        Files.writeString(packMetaFile, GSON.toJson(packMeta));

        Terradyne.LOGGER.debug("Generated pack.mcmeta");
    }

    /**
     * Generate dimension JSON files for each planet
     */
    private static void generateDimensionFiles(Path datapackDir, Map<String, PlanetConfig> planetConfigs) throws IOException {
        Path dimensionDir = datapackDir.resolve("data").resolve("terradyne").resolve("dimension");

        for (Map.Entry<String, PlanetConfig> entry : planetConfigs.entrySet()) {
            String planetName = entry.getKey().toLowerCase().replace(" ", "_");
            PlanetConfig config = entry.getValue();

            // Create dimension JSON
            JsonObject dimension = createDimensionJson(config);

            // Write to file
            Path dimensionFile = dimensionDir.resolve(planetName + ".json");
            Files.writeString(dimensionFile, GSON.toJson(dimension));

            Terradyne.LOGGER.info("Generated dimension file: {}", planetName + ".json");
        }
    }

    /**
     * Create dimension JSON structure using our registered chunk generator
     */
    private static JsonObject createDimensionJson(PlanetConfig config) {
        JsonObject dimension = new JsonObject();

        // Use overworld dimension type (you can customize this later)
        dimension.addProperty("type", "minecraft:overworld");

        // Create generator section with our registered generator
        JsonObject generator = new JsonObject();
        generator.addProperty("type", "terradyne:universal");
        generator.addProperty("planet_name", config.getPlanetName());

        // Biome source - fixed plains for now
        JsonObject biomeSource = new JsonObject();
        biomeSource.addProperty("type", "minecraft:fixed");
        biomeSource.addProperty("biome", "minecraft:plains");
        generator.add("biome_source", biomeSource);

        dimension.add("generator", generator);

        return dimension;
    }

    /**
     * Register generated planets in our tracking system
     */
    private static void registerGeneratedPlanets(MinecraftServer server, Map<String, PlanetConfig> planetConfigs) {
        try {
            ExistingPlanetRegistry registry = new ExistingPlanetRegistry(server);

            for (Map.Entry<String, PlanetConfig> entry : planetConfigs.entrySet()) {
                String planetName = entry.getValue().getPlanetName();
                String dimensionId = "terradyne:" + entry.getKey().toLowerCase().replace(" ", "_");
                String configHash = ExistingPlanetRegistry.generateConfigHash(entry.getValue());

                registry.registerGeneratedPlanet(planetName, dimensionId, configHash);
            }

            Terradyne.LOGGER.debug("Registered {} planets in tracking system", planetConfigs.size());

        } catch (Exception e) {
            Terradyne.LOGGER.error("Failed to register planets in tracking system", e);
        }
    }

    /**
     * Check if datapack needs to be regenerated (for future use)
     */
    public static boolean needsRegeneration(MinecraftServer server) {
        // TODO: Compare existing datapack with current planet configs
        // For now, always return false (don't regenerate existing datapacks)
        return false;
    }

    /**
     * Remove inactive planet dimensions from datapack (for future use)
     */
    public static void cleanupInactivePlanets(MinecraftServer server) {
        // TODO: Implement cleanup for removed planet configs
        // Mark as inactive rather than delete (as requested)
    }
}