package net.starlight.terradyne.starsystem;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.starlight.terradyne.Terradyne;
import net.starlight.terradyne.planet.physics.AtmosphereComposition;
import net.starlight.terradyne.planet.physics.CrustComposition;
import net.starlight.terradyne.planet.physics.PlanetConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry for all loaded classes of celestial objects
 * Loads from data/[namespace]/terradyne/celestial_objects/*.json
 */
public class CelestialObjectRegistry {
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<Identifier, CelestialObjectEntry> OBJECTS = new HashMap<>();

    /**
     * Load all celestial objects from file system during data generation
     */
    public static void loadFromFileSystem(Path resourcesRoot) {
        OBJECTS.clear();

        Terradyne.LOGGER.info("=== LOADING CELESTIAL OBJECTS FROM FILE SYSTEM ===");

        int loaded = 0;

        try {
            // Walk through all data directories
            try (var paths = Files.walk(resourcesRoot)) {
                paths.filter(path -> {
                    String pathStr = path.toString().replace('\\', '/');
                    return pathStr.contains("terradyne/celestial_objects") && pathStr.endsWith(".json");
                })
                        .forEach(path -> {
                            try {
                                Terradyne.LOGGER.info("Found celestial object file: {}", path);

                                // Read JSON file
                                String json = Files.readString(path, StandardCharsets.UTF_8);
                                CelestialObjectDefinition definition = GSON.fromJson(json, CelestialObjectDefinition.class);

                                // Extract namespace and object name from path
                                String pathStr = path.toString().replace('\\', '/');
                                String dataSegment = pathStr.substring(pathStr.indexOf("/data/") + 6);
                                String namespace = dataSegment.substring(0, dataSegment.indexOf('/'));
                                String fileName = path.getFileName().toString().replace(".json", "");

                                Identifier objectId = new Identifier(namespace, fileName);

                                // Convert and register
                                CelestialObjectEntry entry = convertToEntry(objectId, definition);
                                OBJECTS.put(objectId, entry);

                                Terradyne.LOGGER.info("✓ Loaded celestial object: {} ({})", objectId, entry.type);

                            } catch (Exception e) {
                                Terradyne.LOGGER.error("Failed to load celestial object from {}: {}", path, e.getMessage(), e);
                            }
                        });
            }

            loaded = OBJECTS.size();

        } catch (IOException e) {
            Terradyne.LOGGER.error("Failed to scan for celestial objects", e);
        }

        Terradyne.LOGGER.info("=== LOADED {} CELESTIAL OBJECTS ===", loaded);
    }

    /**
     * Load all celestial objects from ResourceManager (for runtime)
     */
    public static void loadFromDatapacks(ResourceManager resourceManager) {
        OBJECTS.clear();

        Terradyne.LOGGER.info("=== LOADING CELESTIAL OBJECTS FROM DATAPACKS (RUNTIME) ===");
        
        // DEBUG: Log what we're searching for
        Terradyne.LOGGER.info("Searching for resources at path: terradyne/celestial_objects");
        Terradyne.LOGGER.info("Expected pattern: data/[namespace]/terradyne/celestial_objects/*.json");

        // Find all celestial object JSON files
        var resources = resourceManager.findResources("terradyne/celestial_objects",
                path -> path.getPath().endsWith(".json"));

        Terradyne.LOGGER.info("Found {} resource entries", resources.size());
        
        // DEBUG: Log all found resources
        if (resources.isEmpty()) {
            Terradyne.LOGGER.error("❌ No resources found! Debugging info:");
            Terradyne.LOGGER.error("ResourceManager type: {}", resourceManager.getClass().getName());
            
            // Try to list what namespaces are available
            try {
                var allNamespaces = resourceManager.getAllNamespaces();
                Terradyne.LOGGER.error("Available namespaces: {}", allNamespaces);
                
                // Try searching in each namespace explicitly
                for (String namespace : allNamespaces) {
                    Terradyne.LOGGER.error("Checking namespace '{}' for terradyne resources...", namespace);
                }
            } catch (Exception e) {
                Terradyne.LOGGER.error("Failed to get namespaces: {}", e.getMessage());
            }
        }

        int loaded = 0;
        for (var entry : resources.entrySet()) {
            Identifier fileId = entry.getKey();
            
            Terradyne.LOGGER.info("Processing resource: {}", fileId);

            try (var reader = new BufferedReader(new InputStreamReader(
                    entry.getValue().getInputStream(), StandardCharsets.UTF_8))) {

                // Parse JSON
                CelestialObjectDefinition definition = GSON.fromJson(reader, CelestialObjectDefinition.class);

                // Extract object name from file path
                String path = fileId.getPath();
                String objectName = path.substring(
                        path.lastIndexOf('/') + 1,
                        path.lastIndexOf('.')
                );

                // Create identifier
                Identifier objectId = new Identifier(fileId.getNamespace(), objectName);

                // Convert to entry
                CelestialObjectEntry objectEntry = convertToEntry(objectId, definition);

                // Register
                OBJECTS.put(objectId, objectEntry);
                loaded++;

                Terradyne.LOGGER.info("✓ Loaded celestial object: {} ({})",
                        objectId, objectEntry.type);

            } catch (Exception e) {
                Terradyne.LOGGER.error("Failed to load celestial object from {}: {}",
                        fileId, e.getMessage(), e);
            }
        }

        Terradyne.LOGGER.info("=== LOADED {} CELESTIAL OBJECTS (RUNTIME) ===", loaded);
    }
    
    /**
     * Convert definition to entry with PlanetConfig
     */
    static CelestialObjectEntry convertToEntry(Identifier objectId,
                                               CelestialObjectDefinition def) {
        // Parse object type
        StarSystemModel.ObjectType type = StarSystemModel.ObjectType.valueOf(
            def.type.toUpperCase()
        );
        
        // Create PlanetConfig from physical properties
        var props = def.physicalProperties;
        PlanetConfig planetConfig = new PlanetConfig(def.name, objectId.hashCode())
            .setCircumference(props.circumference)
            .setDistanceFromStar((long) def.orbitalProperties.distanceFromStar)
            .setCrustComposition(CrustComposition.valueOf(props.crustComposition))
            .setAtmosphereComposition(AtmosphereComposition.valueOf(props.atmosphereComposition))
            .setTectonicActivity(props.tectonicActivity)
            .setWaterContent(props.waterContent)
            .setCrustalThickness(props.crustalThickness)
            .setAtmosphericDensity(props.atmosphericDensity)
            .setRotationPeriod(props.rotationPeriod)
            .setNoiseScale(props.noiseScale);
        
        // Create orbital data
        var orbital = def.orbitalProperties;
        StarSystemModel.OrbitalData orbitalData = new StarSystemModel.OrbitalData(
            orbital.distanceFromParent,
            orbital.distanceFromStar,
            orbital.orbitalPeriod,
            orbital.eccentricity
        );
        
        return new CelestialObjectEntry(objectId, def.name, type, planetConfig, orbitalData);
    }
    
    /**
     * Get a celestial object by identifier
     */
    public static CelestialObjectEntry get(Identifier objectId) {
        return OBJECTS.get(objectId);
    }
    
    /**
     * Get all loaded celestial objects
     */
    public static Map<Identifier, CelestialObjectEntry> getAll() {
        return OBJECTS;
    }
    
    /**
     * Get all celestial object identifiers (convenience method)
     */
    public static java.util.Set<Identifier> getAllIds() {
        return OBJECTS.keySet();
    }
    
    /**
     * Check if an object exists
     */
    public static boolean contains(Identifier objectId) {
        return OBJECTS.containsKey(objectId);
    }
    
    /**
     * Clear the registry
     */
    public static void clear() {
        OBJECTS.clear();
    }
    
    /**
     * Entry in the celestial object registry
     */
    public static class CelestialObjectEntry {
        public final Identifier identifier;
        public final String name;
        public final StarSystemModel.ObjectType type;
        public final PlanetConfig planetConfig;
        public final StarSystemModel.OrbitalData orbitalData;
        
        public CelestialObjectEntry(Identifier identifier, String name,
                                   StarSystemModel.ObjectType type,
                                   PlanetConfig planetConfig,
                                   StarSystemModel.OrbitalData orbitalData) {
            this.identifier = identifier;
            this.name = name;
            this.type = type;
            this.planetConfig = planetConfig;
            this.orbitalData = orbitalData;
        }
    }
}