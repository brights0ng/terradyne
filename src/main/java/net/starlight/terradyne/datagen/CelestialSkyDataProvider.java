package net.starlight.terradyne.datagen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;
import net.minecraft.util.Identifier;
import net.starlight.terradyne.planet.physics.AtmosphereComposition;
import net.starlight.terradyne.planet.physics.PlanetConfig;
import net.starlight.terradyne.starsystem.CelestialObjectRegistry;
import net.starlight.terradyne.starsystem.StarSystemModel;
import net.starlight.terradyne.starsystem.StarSystemRegistry;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Generates Celestial mod skybox configurations using equation-based orbital mechanics.
 * Creates realistic 3D skyboxes where objects move according to Keplerian orbits.
 */
public class CelestialSkyDataProvider implements DataProvider {

    private final FabricDataOutput output;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public CelestialSkyDataProvider(FabricDataOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(DataWriter writer) {
        System.out.println("=== GENERATING CELESTIAL SKYBOX DATA WITH ORBITAL MECHANICS ===");

        List<CompletableFuture<?>> futures = new java.util.ArrayList<>();

        try {
            // Generate dimensions.json
            futures.add(generateDimensionsJson(writer));

            // Generate skyboxes for all star systems
            for (StarSystemModel system : StarSystemRegistry.getAllSystems()) {
                futures.addAll(generateSystemSkyboxes(writer, system));
            }

            System.out.println("Waiting for " + futures.size() + " celestial sky files to be written...");

        } catch (Exception e) {
            System.err.println("Failed to generate celestial sky data: " + e.getMessage());
            return CompletableFuture.failedFuture(e);
        }

        // Wait for all writes to complete
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> System.out.println("=== CELESTIAL SKYBOX GENERATION COMPLETE ==="));
    }

    /**
     * Generate dimensions.json - lists dimension paths (without namespace prefix in path)
     * Format: { "dimensions": ["earth", "mars", "venus", ...] }
     */
    private CompletableFuture<?> generateDimensionsJson(DataWriter writer) throws Exception {
        JsonObject root = new JsonObject();

        // Add documentation notes
        root.addProperty("NOTE", "Dimensions are registered here. Each dimension folder contains sky.json and objects/");

        // Create array of dimension folder names (just the path part, no namespace)
        JsonArray dimensionsArray = new JsonArray();

        for (Identifier objectId : CelestialObjectRegistry.getAllIds()) {
            // For dimension "terradyne:earth", we reference folder "terradyne/earth"
            String dimensionFolder = objectId.getNamespace() + "/" + objectId.getPath();
            dimensionsArray.add(dimensionFolder);
        }

        root.add("dimensions", dimensionsArray);

        // Write to assets/celestial/sky/dimensions.json
        Identifier dimensionsId = new Identifier("celestial", "sky/dimensions");
        Path dimensionsPath = output.getResolver(net.minecraft.data.DataOutput.OutputType.RESOURCE_PACK, "")
                .resolveJson(dimensionsId);

        System.out.println("✅ Queued dimensions.json with " + dimensionsArray.size() + " dimensions");
        return DataProvider.writeToPath(writer, root, dimensionsPath);
    }

    /**
     * Generate skybox configurations for all objects in a star system
     */
    private List<CompletableFuture<?>> generateSystemSkyboxes(DataWriter writer, StarSystemModel system) throws Exception {
        System.out.println("Processing star system: " + system.getName());
        
        List<CompletableFuture<?>> futures = new java.util.ArrayList<>();

        for (StarSystemModel.CelestialObject observer : system.getObjects().values()) {
            futures.addAll(generateSkyboxForObserver(writer, system, observer));
        }
        
        return futures;
    }

    /**
     * Generate complete skybox for a single observer object
     */
    private List<CompletableFuture<?>> generateSkyboxForObserver(DataWriter writer, StarSystemModel system,
                                           StarSystemModel.CelestialObject observer) throws Exception {
        Identifier observerId = observer.getIdentifier();
        System.out.println("  → Generating skybox for: " + observer.getName());
        
        List<CompletableFuture<?>> futures = new java.util.ArrayList<>();

        // Generate sky.json
        JsonObject skyJson = createSkyJson(observer, system);

        Identifier skyId = new Identifier("celestial",
                "sky/" + observerId.getNamespace() + "/" + observerId.getPath() + "/sky");
        Path skyPath = output.getResolver(net.minecraft.data.DataOutput.OutputType.RESOURCE_PACK, "")
                .resolveJson(skyId);

        futures.add(DataProvider.writeToPath(writer, skyJson, skyPath));

        // Generate basic objects (sun, moon, stars, twilight)
        futures.addAll(generateBasicObjects(writer, system, observer));

        // Generate planet objects using orbital mechanics
        futures.addAll(generatePlanetObjects(writer, system, observer));

        System.out.println("  ✅ Queued skybox files for: " + observer.getName());
        
        return futures;
    }

    /**
     * Create sky.json configuration
     */
    private JsonObject createSkyJson(StarSystemModel.CelestialObject observer, StarSystemModel system) {
        JsonObject sky = new JsonObject();
        PlanetConfig config = observer.getPlanetConfig();

        // Sky objects list - these reference files in the objects/ folder
        JsonArray skyObjects = new JsonArray();
        skyObjects.add("sun");
        skyObjects.add("moon");
        skyObjects.add("stars");
        skyObjects.add("twilight");

                // Add all planets visible from this observer (using StarSystemModel)
        List<StarSystemModel.CelestialObject> visiblePlanets = system.getVisibleObjects(observer.getIdentifier());
        for (StarSystemModel.CelestialObject planetObject : visiblePlanets) {
            if (planetObject.getType() != StarSystemModel.ObjectType.SOLAR)
            skyObjects.add("planet_" + planetObject.getName().toLowerCase()); // e.g., "planet_mars", "planet_venus"
        }

        sky.add("sky_objects", skyObjects);

        // Environment settings
        if (config != null) {
            JsonObject environment = new JsonObject();
            AtmosphereComposition atmosphere = config.getAtmosphereComposition();

            JsonObject fogColor = new JsonObject();
            fogColor.addProperty("base_color", getAtmosphericFogColor(atmosphere));
            environment.add("fog_color", fogColor);

            JsonObject skyColor = new JsonObject();
            skyColor.addProperty("base_color", getAtmosphericSkyColor(atmosphere));
            environment.add("sky_color", skyColor);

            environment.addProperty("twilight_alpha", "0");

            sky.add("environment", environment);
        }

        return sky;
    }

    /**
     * Generate basic celestial objects (sun, moon, stars, twilight)
     */
    private List<CompletableFuture<?>> generateBasicObjects(DataWriter writer, StarSystemModel system,
                                      StarSystemModel.CelestialObject observer) throws Exception {
        Identifier observerId = observer.getIdentifier();
        String basePath = observerId.getNamespace() + "/" + observerId.getPath();
        
        List<CompletableFuture<?>> futures = new java.util.ArrayList<>();

        // Generate sun
        JsonObject sun = createSunObject(system, observer);
        futures.add(writeObject(writer, sun, basePath, "sun"));

        // Generate moon
        JsonObject moon = createMoonObject(system, observer);
        futures.add(writeObject(writer, moon, basePath, "moon"));

        // Generate stars
        JsonObject stars = createStarsObject(system, observer);
        futures.add(writeObject(writer, stars, basePath, "stars"));

        // Generate twilight
        JsonObject twilight = createTwilightObject(system, observer);
        futures.add(writeObject(writer, twilight, basePath, "twilight"));
        
        return futures;
    }

    /**
     * Create sun object with dynamic positioning
     */
    private JsonObject createSunObject(StarSystemModel system, StarSystemModel.CelestialObject observer) {
        JsonObject sun = new JsonObject();
        sun.addProperty("texture", "minecraft:textures/environment/sun.png");

        // Get the star (sun) of this system
        StarSystemModel.CelestialObject star = system.getStar();
        String[] position = system.generatePositionEquations(observer.getIdentifier(), star.getIdentifier());
        String scale = system.generateScaleEquation(star.getIdentifier());
        String skyRotation = system.generateSkyRotationEquation(observer.getIdentifier());

        // Display
        JsonObject display = new JsonObject();
        display.addProperty("scale", scale);
        display.addProperty("pos_x", position[0]);
        display.addProperty("pos_y", position[1]);
        display.addProperty("pos_z", position[2]);
        display.addProperty("distance", "100");
        sun.add("display", display);

        // Rotation
        JsonObject rotation = new JsonObject();
        rotation.addProperty("degrees_x", skyRotation);
        rotation.addProperty("degrees_y", "0");
        rotation.addProperty("degrees_z", "0");
        rotation.addProperty("base_degrees_x", "-90");
        rotation.addProperty("base_degrees_z", "-90");
        sun.add("rotation", rotation);

        // Properties
        JsonObject properties = new JsonObject();
        properties.addProperty("is_solid", false);
        properties.addProperty("blend", true);
        properties.addProperty("alpha", "1 - rainAlpha");

        JsonObject color = new JsonObject();
        color.addProperty("red", "1");
        color.addProperty("green", "1");
        color.addProperty("blue", "1");
        properties.add("color", color);

        sun.add("properties", properties);

        return sun;
    }

    /**
     * Create moon object
     */
    private JsonObject createMoonObject(StarSystemModel system, StarSystemModel.CelestialObject observer) {
        JsonObject moon = new JsonObject();
        moon.addProperty("texture", "minecraft:textures/environment/moon_phases.png");

        String skyRotation = system.generateSkyRotationEquation(observer.getIdentifier());

        // Display
        JsonObject display = new JsonObject();
        display.addProperty("scale", "20");
        display.addProperty("pos_x", "0");
        display.addProperty("pos_y", "0");
        display.addProperty("pos_z", "0");
        display.addProperty("distance", "200");
        moon.add("display", display);

        // Rotation (moon orbits slower than day/night cycle)
        JsonObject rotation = new JsonObject();
        rotation.addProperty("degrees_x", String.format("(%s) * 2.96 + 180", skyRotation));
        rotation.addProperty("degrees_y", "0");
        rotation.addProperty("degrees_z", "0");
        rotation.addProperty("base_degrees_x", "-90");
        rotation.addProperty("base_degrees_z", "-90");
        moon.add("rotation", rotation);

        // Properties
        JsonObject properties = new JsonObject();
        properties.addProperty("has_moon_phases", true);
        properties.addProperty("moon_phase", "moonPhase");
        properties.addProperty("is_solid", false);
        properties.addProperty("blend", true);
        properties.addProperty("alpha", "max(0.5, (1 - dayLight * 0.5)) * (1 - rainAlpha)");

        JsonObject color = new JsonObject();
        color.addProperty("red", "1");
        color.addProperty("green", "1");
        color.addProperty("blue", "1");
        properties.add("color", color);

        moon.add("properties", properties);

        return moon;
    }

    /**
     * Create stars object using populate
     */
    private JsonObject createStarsObject(StarSystemModel system, StarSystemModel.CelestialObject observer) {
        JsonObject stars = new JsonObject();
        stars.addProperty("solid_color", "#ffffff");

        String skyRotation = system.generateSkyRotationEquation(observer.getIdentifier());

        // Display
        JsonObject display = new JsonObject();
        display.addProperty("scale", "0");
        display.addProperty("pos_x", "0");
        display.addProperty("pos_y", "0");
        display.addProperty("pos_z", "0");
        display.addProperty("distance", "150");
        stars.add("display", display);

        // Rotation
        JsonObject rotation = new JsonObject();
        rotation.addProperty("degrees_x", String.format("(%s) + 90", skyRotation));
        rotation.addProperty("degrees_y", "0");
        rotation.addProperty("degrees_z", "0");
        rotation.addProperty("base_degrees_x", "-90");
        rotation.addProperty("base_degrees_z", "-90");
        stars.add("rotation", rotation);

        // Properties
        JsonObject properties = new JsonObject();
        properties.addProperty("is_solid", true);
        properties.addProperty("blend", true);
        properties.addProperty("alpha", "max((1 - dayLight) * (1 - rainAlpha) - 0.5, 0)");

        JsonObject color = new JsonObject();
        color.addProperty("red", "1");
        color.addProperty("green", "1");
        color.addProperty("blue", "1");
        properties.add("color", color);

        stars.add("properties", properties);

        // Populate
        JsonObject populate = new JsonObject();
        populate.addProperty("count", 300);

        JsonObject populateRotation = new JsonObject();
        populateRotation.addProperty("min_degrees_x", 0);
        populateRotation.addProperty("max_degrees_x", 360);
        populateRotation.addProperty("min_degrees_y", 0);
        populateRotation.addProperty("max_degrees_y", 360);
        populateRotation.addProperty("min_degrees_z", 0);
        populateRotation.addProperty("max_degrees_z", 360);
        populate.add("rotation", populateRotation);

        JsonObject populateDisplay = new JsonObject();
        populateDisplay.addProperty("min_scale", 0.3);
        populateDisplay.addProperty("max_scale", 0.35);
        populateDisplay.addProperty("min_pos_x", 0);
        populateDisplay.addProperty("max_pos_x", 0);
        populateDisplay.addProperty("min_pos_y", 0);
        populateDisplay.addProperty("max_pos_y", 0);
        populateDisplay.addProperty("min_pos_z", 0);
        populateDisplay.addProperty("max_pos_z", 0);
        populateDisplay.addProperty("min_distance", 0);
        populateDisplay.addProperty("max_distance", 0);
        populate.add("display", populateDisplay);

        stars.add("populate", populate);

        return stars;
    }

    /**
     * Create twilight object
     */
    private JsonObject createTwilightObject(StarSystemModel system, StarSystemModel.CelestialObject observer) {
        JsonObject twilight = new JsonObject();
        twilight.addProperty("type", "twilight");
        twilight.addProperty("solid_color", "#ffe533");
        twilight.addProperty("solid_color_transition", "#b33333");

        String skyRotation = system.generateSkyRotationEquation(observer.getIdentifier());

        // Display
        JsonObject display = new JsonObject();
        display.addProperty("scale", "1");
        display.addProperty("pos_x", "0");
        display.addProperty("pos_y", "0");
        display.addProperty("pos_z", "0");
        display.addProperty("distance", "100");
        twilight.add("display", display);

        // Rotation
        JsonObject rotation = new JsonObject();
        rotation.addProperty("base_degrees_y", "0");
        rotation.addProperty("twilight_rotation", String.format("(%s) + 90", skyRotation));
        twilight.add("rotation", rotation);

        // Properties
        JsonObject properties = new JsonObject();
        properties.addProperty("is_solid", true);
        properties.addProperty("blend", true);
        // Calculate twilight alpha from dayLight (visible during sunrise/sunset)
        // Peaks when dayLight is around 0.2-0.3
        properties.addProperty("alpha", "max(0, 1 - abs(dayLight - 0.25) * 4) * (1 - rainAlpha)");

        twilight.add("properties", properties);

        return twilight;
    }

    /**
     * Generate planet objects with orbital mechanics
     */
    private List<CompletableFuture<?>> generatePlanetObjects(DataWriter writer, StarSystemModel system,
                                       StarSystemModel.CelestialObject observer) throws Exception {
        String basePath = observer.getIdentifier().getNamespace() + "/" + observer.getIdentifier().getPath();
        
        List<CompletableFuture<?>> futures = new java.util.ArrayList<>();

        // Get all visible objects (excluding observer itself)
        List<StarSystemModel.CelestialObject> visibleObjects = system.getVisibleObjects(observer.getIdentifier());

        for (StarSystemModel.CelestialObject target : visibleObjects) {
            // Skip the sun (already generated as basic object)
            if (target.getType() == StarSystemModel.ObjectType.SOLAR) {
                continue;
            }

            // Generate planet object with orbital mechanics
            JsonObject planetObj = createPlanetObject(system, observer, target);

            // Write to file
            String objectName = "planet_" + target.getIdentifier().getPath();
            futures.add(writeObject(writer, planetObj, basePath, objectName));
        }
        
        return futures;
    }

    /**
     * Create a planet object with equation-based orbital mechanics
     */
    private JsonObject createPlanetObject(StarSystemModel system,
                                          StarSystemModel.CelestialObject observer,
                                          StarSystemModel.CelestialObject target) {
        JsonObject planet = new JsonObject();

        // Use texture type
        planet.addProperty("type", "texture");
        planet.addProperty("texture", "minecraft:textures/environment/moon_phases.png");

        // Generate equations for this planet
        String[] position = system.generatePositionEquations(observer.getIdentifier(), target.getIdentifier());
        String scale = system.generateScaleEquation(target.getIdentifier());
        String alpha = system.generateAlphaEquation(target.getIdentifier());
        String skyRotation = system.generateSkyRotationEquation(observer.getIdentifier());

        // Display with equation-based positioning
        JsonObject display = new JsonObject();
        display.addProperty("scale", scale);
        display.addProperty("pos_x", position[0]);
        display.addProperty("pos_y", position[1]);
        display.addProperty("pos_z", position[2]);
        display.addProperty("distance", "100");
        planet.add("display", display);

        // Rotation (skybox rotation only, position equations handle orbital motion)
        JsonObject rotation = new JsonObject();
        rotation.addProperty("degrees_x", skyRotation);
        rotation.addProperty("degrees_y", "0");
        rotation.addProperty("degrees_z", "0");
        rotation.addProperty("base_degrees_x", "-90");
        rotation.addProperty("base_degrees_z", "-90");
        planet.add("rotation", rotation);

        // Properties
        JsonObject properties = new JsonObject();
        properties.addProperty("is_solid", false);
        properties.addProperty("blend", true);
        properties.addProperty("alpha", alpha);
        properties.addProperty("has_moon_phases", true);
        properties.addProperty("moon_phase", "moonPhase");

        // Color
        JsonObject color = new JsonObject();
        String[] rgb = getPlanetColor(target);
        color.addProperty("red", rgb[0]);
        color.addProperty("green", rgb[1]);
        color.addProperty("blue", rgb[2]);
        properties.add("color", color);

        planet.add("properties", properties);

        return planet;
    }

    /**
     * Get planet color based on type
     */
    private String[] getPlanetColor(StarSystemModel.CelestialObject object) {
        return switch (object.getType()) {
            case SOLAR -> new String[]{"1", "1", "0.9"}; // Yellow
            case TERRESTRIAL -> new String[]{"0.7", "0.6", "0.5"}; // Brownish
            case GASEOUS -> new String[]{"0.8", "0.7", "0.6"}; // Tan
        };
    }

    /**
     * Get atmospheric sky color
     */
    private String getAtmosphericSkyColor(AtmosphereComposition atmosphere) {
        return switch (atmosphere) {
            case OXYGEN_RICH -> "#78A7FF";
            case CARBON_DIOXIDE -> "#FFA500";
            case NITROGEN_RICH -> "#9370DB";
            case METHANE -> "#D2691E";
            case WATER_VAPOR_RICH -> "#F5F5DC";
            case HYDROGEN_SULFIDE -> "#B8860B";
            case NOBLE_GAS_MIXTURE -> "#E6E6FA";
            case TRACE_ATMOSPHERE -> "#2F2F2F";
            case VACUUM -> "#000000";
        };
    }

    /**
     * Get atmospheric fog color
     */
    private String getAtmosphericFogColor(AtmosphereComposition atmosphere) {
        return switch (atmosphere) {
            case OXYGEN_RICH -> "#C0D8FF";
            case CARBON_DIOXIDE -> "#FFE4B5";
            case NITROGEN_RICH -> "#DDA0DD";
            case METHANE -> "#F4A460";
            case WATER_VAPOR_RICH -> "#FFFFFF";
            case HYDROGEN_SULFIDE -> "#FFFF99";
            case NOBLE_GAS_MIXTURE -> "#F8F8FF";
            case TRACE_ATMOSPHERE -> "#696969";
            case VACUUM -> "#000000";
        };
    }

    /**
     * Write an object JSON file
     */
    private CompletableFuture<?> writeObject(DataWriter writer, JsonObject object, String basePath, String objectName)
            throws Exception {
        Identifier objectId = new Identifier("celestial", "sky/" + basePath + "/objects/" + objectName);
        Path objectPath = output.getResolver(net.minecraft.data.DataOutput.OutputType.RESOURCE_PACK, "")
                .resolveJson(objectId);

        System.out.println("    → Queued " + objectName + ".json");
        return DataProvider.writeToPath(writer, object, objectPath);
    }

    @Override
    public String getName() {
        return "Terradyne Celestial Sky (Orbital Mechanics)";
    }
}