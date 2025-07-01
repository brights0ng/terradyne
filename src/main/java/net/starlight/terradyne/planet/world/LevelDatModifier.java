package net.starlight.terradyne.planet.world;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.starlight.terradyne.Terradyne;
import net.starlight.terradyne.planet.config.ExistingPlanetRegistry;
import net.starlight.terradyne.planet.config.PlanetConfigLoader;
import net.starlight.terradyne.planet.physics.PlanetConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Alternative approach: Directly modify level.dat to include our dimensions
 * This is more reliable than mixin injection and easier to debug
 */
public class LevelDatModifier {

    /**
     * Check if we need to modify level.dat for new worlds
     * Called before server startup in WorldPlanetManager
     * Updated to use the provided registry instead of creating a new one
     */
    public static void modifyLevelDatIfNeeded(MinecraftServer server, ExistingPlanetRegistry registry) {
        try {
            if (!isNewWorld(server)) {
                Terradyne.LOGGER.info("Existing world detected - level.dat should already contain dimensions");
                return;
            }

            Terradyne.LOGGER.info("New world detected - checking if level.dat needs dimension injection");

            // Load planet configurations
            Map<String, PlanetConfig> planetConfigs = PlanetConfigLoader.loadAllPlanetConfigs(server);

            if (planetConfigs.isEmpty()) {
                Terradyne.LOGGER.info("No planet configurations found - skipping level.dat modification");
                return;
            }

            // Read current level.dat
            Path levelDatPath = server.getSavePath(net.minecraft.util.WorldSavePath.ROOT).resolve("level.dat");

            if (!Files.exists(levelDatPath)) {
                Terradyne.LOGGER.warn("level.dat not found - cannot inject dimensions");
                return;
            }

            NbtCompound levelData = NbtIo.readCompressed(levelDatPath.toFile());
            NbtCompound data = levelData.getCompound("Data");

            // Check if dimensions are already present
            if (data.contains("WorldGenSettings")) {
                NbtCompound worldGenSettings = data.getCompound("WorldGenSettings");

                if (worldGenSettings.contains("dimensions")) {
                    NbtCompound dimensions = worldGenSettings.getCompound("dimensions");

                    // Check if any of our dimensions are already present
                    boolean hasOurDimensions = false;
                    for (String planetName : planetConfigs.keySet()) {
                        String dimensionKey = Terradyne.MOD_ID + ":" + planetName.toLowerCase().replace(" ", "_");
                        if (dimensions.contains(dimensionKey)) {
                            hasOurDimensions = true;
                            break;
                        }
                    }

                    if (hasOurDimensions) {
                        Terradyne.LOGGER.info("Dimensions already present in level.dat - skipping injection");
                        return;
                    }

                    // Add our dimensions to the NBT structure
                    addDimensionsToLevelDat(dimensions, planetConfigs);

                    // Write back to file
                    NbtIo.writeCompressed(levelData, levelDatPath.toFile());

                    // Verify the write by reading it back
                    try {
                        NbtCompound verifyData = NbtIo.readCompressed(levelDatPath.toFile());
                        NbtCompound verifyWorldGenSettings = verifyData.getCompound("Data").getCompound("WorldGenSettings");
                        NbtCompound verifyDimensions = verifyWorldGenSettings.getCompound("dimensions");

                        Terradyne.LOGGER.info("=== LEVEL.DAT VERIFICATION ===");
                        for (String planetName : planetConfigs.keySet()) {
                            String dimensionKey = Terradyne.MOD_ID + ":" + planetName.toLowerCase().replace(" ", "_");
                            if (verifyDimensions.contains(dimensionKey)) {
                                Terradyne.LOGGER.info("✅ Verified dimension in level.dat: {}", dimensionKey);
                            } else {
                                Terradyne.LOGGER.warn("❌ Dimension missing from level.dat: {}", dimensionKey);
                            }
                        }

                        // Log all dimensions currently in level.dat
                        Terradyne.LOGGER.info("All dimensions in level.dat:");
                        for (String key : verifyDimensions.getKeys()) {
                            Terradyne.LOGGER.info("  - {}", key);
                        }

                    } catch (Exception e) {
                        Terradyne.LOGGER.error("Failed to verify level.dat write: {}", e.getMessage());
                    }

                    // Use the provided registry instance instead of creating a new one
                    for (Map.Entry<String, PlanetConfig> entry : planetConfigs.entrySet()) {
                        String dimensionId = Terradyne.MOD_ID + ":" + entry.getKey().toLowerCase().replace(" ", "_");
                        String configHash = ExistingPlanetRegistry.generateConfigHash(entry.getValue());
                        registry.registerGeneratedPlanet(entry.getValue().getPlanetName(), dimensionId, configHash);
                    }

                    Terradyne.LOGGER.info("✅ Successfully injected {} dimensions into level.dat", planetConfigs.size());

                } else {
                    Terradyne.LOGGER.warn("WorldGenSettings.dimensions not found in level.dat");
                }
            } else {
                Terradyne.LOGGER.warn("WorldGenSettings not found in level.dat");
            }

        } catch (Exception e) {
            Terradyne.LOGGER.error("Failed to modify level.dat", e);
        }
    }

    /**
     * Add our dimension definitions to the level.dat NBT structure
     * Simplified to mirror the overworld structure exactly
     */
    private static void addDimensionsToLevelDat(NbtCompound dimensions, Map<String, PlanetConfig> planetConfigs) {
        for (Map.Entry<String, PlanetConfig> entry : planetConfigs.entrySet()) {
            String planetName = entry.getKey().toLowerCase().replace(" ", "_");
            String dimensionKey = Terradyne.MOD_ID + ":" + planetName;

            Terradyne.LOGGER.info("Adding dimension to level.dat: {}", dimensionKey);

            // Create dimension NBT structure by copying overworld structure
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

            Terradyne.LOGGER.debug("Added dimension NBT: {}", dimension.toString());
        }
    }

    /**
     * Check if this is a new world
     */
    private static boolean isNewWorld(MinecraftServer server) {
        Path registryPath = getTerradyneConfigDirectory(server).resolve("planet_registry.json");
        boolean hasExistingRegistry = Files.exists(registryPath);

        Terradyne.LOGGER.debug("New world check: hasExistingRegistry={}", hasExistingRegistry);
        return !hasExistingRegistry;
    }

    /**
     * Get Terradyne config directory
     */
    private static Path getTerradyneConfigDirectory(MinecraftServer server) {
        return server.getSavePath(net.minecraft.util.WorldSavePath.ROOT).resolve("terradyne");
    }
}