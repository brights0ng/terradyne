package net.starlight.terradyne.starsystem;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.starlight.terradyne.Terradyne;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry for all loaded star systems
 * Loads from data/[namespace]/terradyne/star_systems/*.json
 */
public class StarSystemRegistry {
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<Identifier, StarSystemModel> STAR_SYSTEMS = new HashMap<>();

    /**
     * Load all star systems from file system during data generation
     * Must be called AFTER CelestialObjectRegistry.loadFromFileSystem()
     */
    public static void loadFromFileSystem(Path resourcesRoot) {
        STAR_SYSTEMS.clear();

        Terradyne.LOGGER.info("=== LOADING STAR SYSTEMS FROM FILE SYSTEM ===");

        int loaded = 0;

        try {
            // Walk through all data directories
            try (var paths = Files.walk(resourcesRoot)) {
                paths.filter(path -> {
                            String pathStr = path.toString().replace('\\', '/');
                            return pathStr.contains("terradyne/star_systems") && pathStr.endsWith(".json");
                        })
                        .forEach(path -> {
                            try {
                                Terradyne.LOGGER.info("Found star system file: {}", path);

                                // Read JSON file
                                String json = Files.readString(path, StandardCharsets.UTF_8);
                                StarSystemDefinition definition = GSON.fromJson(json, StarSystemDefinition.class);

                                // Extract namespace and system name from path
                                String pathStr = path.toString().replace('\\', '/');
                                String dataSegment = pathStr.substring(pathStr.indexOf("/data/") + 6);
                                String namespace = dataSegment.substring(0, dataSegment.indexOf('/'));
                                String fileName = path.getFileName().toString().replace(".json", "");

                                Identifier systemId = new Identifier(namespace, fileName);

                                // Build star system model
                                StarSystemModel model = buildStarSystemModel(systemId, definition);
                                STAR_SYSTEMS.put(systemId, model);

                                Terradyne.LOGGER.info("✓ Loaded star system: {} ({} objects)",
                                        systemId, model.getObjects().size());

                            } catch (Exception e) {
                                Terradyne.LOGGER.error("Failed to load star system from {}: {}", path, e.getMessage(), e);
                            }
                        });
            }

            loaded = STAR_SYSTEMS.size();

        } catch (IOException e) {
            Terradyne.LOGGER.error("Failed to scan for star systems", e);
        }

        Terradyne.LOGGER.info("=== LOADED {} STAR SYSTEMS ===", loaded);
    }

    /**
     * Load all star systems from ResourceManager (for runtime)
     * Must be called AFTER CelestialObjectRegistry.loadFromDatapacks()
     */
    public static void loadFromDatapacks(ResourceManager resourceManager) {
        STAR_SYSTEMS.clear();

        Terradyne.LOGGER.info("=== LOADING STAR SYSTEMS FROM DATAPACKS (RUNTIME) ===");

        // Find all star system JSON files
        var resources = resourceManager.findResources("terradyne/star_systems",
                path -> path.getPath().endsWith(".json"));

        int loaded = 0;
        for (var entry : resources.entrySet()) {
            Identifier fileId = entry.getKey();

            try (var reader = new BufferedReader(new InputStreamReader(
                    entry.getValue().getInputStream(), StandardCharsets.UTF_8))) {

                // Parse JSON
                StarSystemDefinition definition = GSON.fromJson(reader, StarSystemDefinition.class);

                // Extract system name from file path
                String path = fileId.getPath();
                String systemName = path.substring(
                        path.lastIndexOf('/') + 1,
                        path.lastIndexOf('.')
                );

                // Create identifier
                Identifier systemId = new Identifier(fileId.getNamespace(), systemName);

                // Build star system model
                StarSystemModel model = buildStarSystemModel(systemId, definition);

                // Register
                STAR_SYSTEMS.put(systemId, model);
                loaded++;

                Terradyne.LOGGER.info("✓ Loaded star system: {} ({} objects)",
                        systemId, model.getObjects().size());

            } catch (Exception e) {
                Terradyne.LOGGER.error("Failed to load star system from {}: {}",
                        fileId, e.getMessage(), e);
            }
        }

        Terradyne.LOGGER.info("=== LOADED {} STAR SYSTEMS (RUNTIME) ===", loaded);
    }
    
    /**
     * Build a complete star system model from definition
     */
    private static StarSystemModel buildStarSystemModel(Identifier systemId,
                                                       StarSystemDefinition definition) {
        // Build hierarchy tree
        StarSystemConfig.HierarchyNode rootNode = buildHierarchyNode(definition.star);
        
        // Collect all objects recursively
        Map<Identifier, StarSystemModel.CelestialObject> objects = new HashMap<>();
        collectObjects(rootNode, objects);
        
        return new StarSystemModel(systemId, definition.name, rootNode, objects);
    }
    
    /**
     * Build hierarchy node from definition
     */
    private static StarSystemConfig.HierarchyNode buildHierarchyNode(
            StarSystemDefinition.OrbitNode orbitNode) {
        
        Identifier objectId = new Identifier(orbitNode.object);
        StarSystemConfig.HierarchyNode node = new StarSystemConfig.HierarchyNode(objectId);
        
        // Recursively build children
        if (orbitNode.orbiting != null) {
            for (StarSystemDefinition.OrbitNode child : orbitNode.orbiting) {
                node.addOrbiting(buildHierarchyNode(child));
            }
        }
        
        return node;
    }
    
    /**
     * Collect all celestial objects from hierarchy
     */
    private static void collectObjects(StarSystemConfig.HierarchyNode node,
                                       Map<Identifier, StarSystemModel.CelestialObject> objects) {
        // Get object from registry
        var entry = CelestialObjectRegistry.get(node.objectId);
        if (entry == null) {
            throw new RuntimeException("Celestial object not found: " + node.objectId);
        }
        
        // Create celestial object
        StarSystemModel.CelestialObject celestialObject = new StarSystemModel.CelestialObject(
            entry.identifier,
            entry.name,
            entry.type,
            entry.planetConfig,
            entry.orbitalData
        );
        
        objects.put(node.objectId, celestialObject);
        
        // Recursively collect children
        for (StarSystemConfig.HierarchyNode child : node.orbiting) {
            collectObjects(child, objects);
        }
    }
    
    /**
     * Get a star system by identifier
     */
    public static StarSystemModel get(Identifier systemId) {
        return STAR_SYSTEMS.get(systemId);
    }
    
    /**
     * Get all loaded star systems
     */
    public static Map<Identifier, StarSystemModel> getAll() {
        return STAR_SYSTEMS;
    }
    
    /**
     * Get all star system models (convenience method for iteration)
     */
    public static java.util.Collection<StarSystemModel> getAllSystems() {
        return STAR_SYSTEMS.values();
    }
    
    /**
     * Check if a star system exists
     */
    public static boolean contains(Identifier systemId) {
        return STAR_SYSTEMS.containsKey(systemId);
    }
    
    /**
     * Get a celestial object by its identifier from any star system
     * This is the PRIMARY lookup method for dimension generation
     */
    public static StarSystemModel.CelestialObject getCelestialObject(Identifier objectId) {
        // Search all star systems for this object
        for (StarSystemModel system : STAR_SYSTEMS.values()) {
            StarSystemModel.CelestialObject object = system.getObject(objectId);
            if (object != null) {
                return object;
            }
        }
        return null;
    }
    
    /**
     * Clear the registry
     */
    public static void clear() {
        STAR_SYSTEMS.clear();
    }
}