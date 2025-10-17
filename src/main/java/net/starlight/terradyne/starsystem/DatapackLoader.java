package net.starlight.terradyne.starsystem;

import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.starlight.terradyne.Terradyne;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * Manages loading of celestial objects and star systems
 * 
 * LOADING FLOW:
 * 1. Data Generation: loadFromFileSystem() loads from src/main/resources during gradlew runDatagen
 * 2. Runtime: loadFromDatapacks() is called by TerradyneResourceReloadListener
 *    - Fires during resource reload (world creation or /reload command)
 *    - Happens BEFORE dimension deserialization but AFTER datapacks are loaded
 *    - This timing is critical for UniversalChunkGenerator.fromCodec() to work
 */
public class DatapackLoader {

    private static boolean loaded = false;

    /**
     * Load all datapack content from file system (for data generation)
     */
    public static void loadFromFileSystem(Path resourcesRoot) {
        if (loaded) {
            Terradyne.LOGGER.debug("Datapack content already loaded, skipping");
            return;
        }

        Terradyne.LOGGER.info("=== LOADING TERRADYNE DATAPACK CONTENT ===");
        Terradyne.LOGGER.info("Scanning path: {}", resourcesRoot);

        try {
            // Step 1: Load all celestial objects
            CelestialObjectRegistry.loadFromFileSystem(resourcesRoot);

            // Step 2: Load all star systems (requires objects to be loaded first)
            StarSystemRegistry.loadFromFileSystem(resourcesRoot);

            loaded = true;

            // Summary
            int totalObjects = CelestialObjectRegistry.getAll().size();
            int totalSystems = StarSystemRegistry.getAll().size();

            Terradyne.LOGGER.info("=== DATAPACK LOADING COMPLETE ===");
            Terradyne.LOGGER.info("✓ {} celestial objects loaded", totalObjects);
            Terradyne.LOGGER.info("✓ {} star systems loaded", totalSystems);

            if (totalSystems == 0) {
                Terradyne.LOGGER.warn("⚠️  No star systems found!");
                Terradyne.LOGGER.warn("Create datapacks in: src/main/resources/data/[namespace]/terradyne/");
            }

        } catch (Exception e) {
            Terradyne.LOGGER.error("Failed to load datapack content", e);
            throw new RuntimeException("Critical datapack loading failure", e);
        }
    }

    /**
     * Load all datapack content from ResourceManager (for runtime)
     */
    public static void loadFromDatapacks(ResourceManager resourceManager) {
        clear(); // Clear any existing data

        Terradyne.LOGGER.info("=== LOADING TERRADYNE DATAPACK CONTENT (RUNTIME) ===");

        try {
            // Step 1: Load all celestial objects
            CelestialObjectRegistry.loadFromDatapacks(resourceManager);

            // Step 2: Load all star systems (requires objects to be loaded first)
            StarSystemRegistry.loadFromDatapacks(resourceManager);

            loaded = true;

            logSummary();

        } catch (Exception e) {
            Terradyne.LOGGER.error("Failed to load datapack content from resource manager", e);
            throw new RuntimeException("Critical datapack loading failure", e);
        }
    }

    /**
     * Log loading summary
     */
    private static void logSummary() {
        int totalObjects = CelestialObjectRegistry.getAll().size();
        int totalSystems = StarSystemRegistry.getAll().size();

        Terradyne.LOGGER.info("=== DATAPACK LOADING COMPLETE ===");
        Terradyne.LOGGER.info("✓ {} celestial objects loaded", totalObjects);
        Terradyne.LOGGER.info("✓ {} star systems loaded", totalSystems);

        if (totalSystems == 0) {
            Terradyne.LOGGER.warn("⚠️  No star systems found!");
            Terradyne.LOGGER.warn("Create datapacks in: data/[namespace]/terradyne/");
        }
    }

    /**
     * Clear all loaded data
     */
    public static void clear() {
        CelestialObjectRegistry.clear();
        StarSystemRegistry.clear();
        loaded = false;
    }

    public static boolean isLoaded() {
        return loaded;
    }
}