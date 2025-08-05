package net.starlight.terradyne.datagen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;
import net.minecraft.util.Identifier;
import net.starlight.terradyne.datagen.HardcodedPlanets;
import net.starlight.terradyne.planet.physics.PlanetConfig;
import net.starlight.terradyne.planet.physics.AtmosphereComposition;
import net.starlight.terradyne.starsystem.PlanetaryBody;
import net.starlight.terradyne.starsystem.StarSystemModel;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Generates Celestial sky definition files for each Terradyne planet
 * FIXED: Follows exact Celestial structure with proper twilight object format
 */
public class CelestialSkyDataProvider implements DataProvider {

    private final FabricDataOutput output;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public CelestialSkyDataProvider(FabricDataOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(DataWriter writer) {
        try {
            generateDimensionsJson(writer);
            generatePlanetSkyFiles(writer);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Generate the main dimension.json file with proper Celestial format
     */
    private void generateDimensionsJson(DataWriter writer) {
        System.out.println("=== GENERATING CELESTIAL DIMENSIONS.JSON ===");

        try {
            JsonObject dimensionsFile = new JsonObject();

            // Add notes (documentation)
            dimensionsFile.addProperty("NOTE", "Dimensions are registered here. Make sure each dimension has its own folder!");
            dimensionsFile.addProperty("NOTE", "Learn more about dimension registration here: https://github.com/fishcute/Celestial/wiki/JSON-Files#dimensionsjson");

            // Create dimensions array
            JsonArray dimensionsArray = new JsonArray();
            var planets = HardcodedPlanets.getAllPlanets();
            for (String planetKey : planets.keySet()) {
                dimensionsArray.add(planetKey); // Just the planet name, no "terradyne:" prefix
            }
            dimensionsFile.add("dimensions", dimensionsArray);

            // FIXED: Write to assets/celestial/sky/dimension.json
            Identifier dimensionsId = new Identifier("celestial", "sky/dimensions");
            Path dimensionsPath = output.getResolver(net.minecraft.data.DataOutput.OutputType.RESOURCE_PACK, "")
                    .resolveJson(dimensionsId);

            DataProvider.writeToPath(writer, dimensionsFile, dimensionsPath);

            System.out.println("✅ Generated dimension.json with " + planets.size() + " dimensions: " + planets.keySet());

        } catch (Exception e) {
            System.err.println("❌ Failed to generate dimension.json");
            e.printStackTrace();
        }
    }

    private void generatePlanetSkyFiles(DataWriter writer) {
        System.out.println("=== GENERATING PLANET SKY FILES WITH OFFSET-BASED ORBITAL MECHANICS ===");

        var planets = HardcodedPlanets.getAllPlanets();
        StarSystemModel starSystem = StarSystemModel.getInstance();

        System.out.println("Generating sky files for " + planets.size() + " planets with offset-based orbital mechanics");
        System.out.println("StarSystem status: " + starSystem.getSystemStatus());
        System.out.println("Approach: Geocentric rotation + Heliocentric position offsets");

        int successCount = 0;
        int totalCount = planets.size();

        for (var entry : planets.entrySet()) {
            String planetKey = entry.getKey();
            PlanetConfig config = entry.getValue();

            System.out.println("Processing sky for planet: " + planetKey + " (" + config.getPlanetName() + ")");

            try {
                // Create sky.json with offset-based orbital mechanics
                JsonObject skyJson = createPlanetSkyJson(config, planetKey, starSystem);

                // Write sky.json
                Identifier skyId = new Identifier("celestial", "sky/" + planetKey + "/sky");
                Path skyPath = output.getResolver(net.minecraft.data.DataOutput.OutputType.RESOURCE_PACK, "")
                        .resolveJson(skyId);
                DataProvider.writeToPath(writer, skyJson, skyPath);

                // Generate all celestial objects (basic + offset-based orbital planets)
                generatePlanetObjects(writer, planetKey, config);

                successCount++;
                System.out.println("✅ Successfully generated offset-based orbital sky for: " + planetKey + " (" + successCount + "/" + totalCount + ")");

            } catch (Exception e) {
                System.err.println("❌ Failed to generate offset-based orbital sky for planet: " + planetKey);
                e.printStackTrace();
            }
        }

        System.out.println("=== OFFSET-BASED ORBITAL MECHANICS SKY GENERATION COMPLETE ===");
        System.out.println("Successfully generated " + successCount + " out of " + totalCount + " planet skies");
        System.out.println("Each planet now has realistic orbital mechanics using:");
        System.out.println("  - Geocentric rotation for basic orbital motion");
        System.out.println("  - Heliocentric position offsets for correct solar system positioning");
        System.out.println("  - Only basic Celestial functions (no inverse trig)");
    }

    private JsonObject createPlanetSkyJson(PlanetConfig config, String observerPlanetKey, StarSystemModel starSystem) {
        JsonObject sky = new JsonObject();

        // Sky objects array - now includes all planets with orbital mechanics!
        JsonArray skyObjects = new JsonArray();
        skyObjects.add("twilight");
        skyObjects.add("moon");
        skyObjects.add("sun");
        skyObjects.add("stars");

        // Add all planets visible from this observer (using StarSystemModel)
        Map<String, StarSystemModel.CelestialExpressions> visiblePlanets = starSystem.generateCompleteSkybox(observerPlanetKey);
        for (String planetObjectName : visiblePlanets.keySet()) {
            skyObjects.add(planetObjectName); // e.g., "planet_mars", "planet_venus"
        }

        sky.add("sky_objects", skyObjects);

        // Rest of environment settings...
        JsonObject environment = new JsonObject();
        JsonObject fogColor = new JsonObject();
        fogColor.addProperty("base_color", getAtmosphericFogColor(config.getAtmosphereComposition()));
        environment.add("fog_color", fogColor);

        JsonObject skyColor = new JsonObject();
        skyColor.addProperty("base_color", getAtmosphericSkyColor(config.getAtmosphereComposition()));
        environment.add("sky_color", skyColor);

        JsonObject clouds = new JsonObject();
        clouds.addProperty("height", "128");
        clouds.addProperty("color", getAtmosphericCloudColor(config.getAtmosphereComposition()));
        environment.add("clouds", clouds);

        JsonObject fog = new JsonObject();
        fog.addProperty("fog_start", String.valueOf(calculateFogStart(config.getAtmosphericDensity())));
        fog.addProperty("fog_end", String.valueOf(calculateFogEnd(config.getAtmosphericDensity())));
        environment.add("fog", fog);

        environment.addProperty("NOTE", "Do not change this value!");
        environment.addProperty("twilight_alpha", "0");

        sky.add("environment", environment);

        return sky;
    }

    /**
     * Generate planet objects using StarSystemModel orbital mechanics
     */
    private void generatePlanetObjects(DataWriter writer, String planetKey, PlanetConfig config) {
        System.out.println("  → Generating objects for " + planetKey + "...");

        // Get StarSystemModel instance
        StarSystemModel starSystem = StarSystemModel.getInstance();

        StarSystemModel.CelestialExpressions sunExpressions = starSystem.generateSunExpressions(planetKey);

        // Create all basic objects (sun, moon, stars, twilight)
        JsonObject sun = createSunObject(sunExpressions, config, planetKey);
        JsonObject moon = createMoonObject(config);
        JsonObject stars = createStarsObject(config);
        JsonObject twilight = createTwilightObject(config);

        // Write basic objects
        writeObjectWithDelay(writer, sun, planetKey, "sun");
        writeObjectWithDelay(writer, moon, planetKey, "moon");
        writeObjectWithDelay(writer, stars, planetKey, "stars");
        writeObjectWithDelay(writer, twilight, planetKey, "twilight");

        // NEW: Generate realistic orbital planet objects
        generatePlanetObjects(writer, planetKey, starSystem);
    }

    /**
     * Generate planet objects using StarSystemModel orbital mechanics
     * UPDATED: Now uses offset-based heliocentric positioning approach
     */
    private void generatePlanetObjects(DataWriter writer, String observerPlanetKey, StarSystemModel starSystem) {
        System.out.println("  → Generating offset-based orbital planet objects for " + observerPlanetKey + "...");

        // Get complete skybox data from StarSystemModel (now with position offsets)
        Map<String, StarSystemModel.CelestialExpressions> skyboxData = starSystem.generateCompleteSkybox(observerPlanetKey);

        System.out.println("    → Found " + skyboxData.size() + " planets visible from " + observerPlanetKey);
        System.out.println("    → Using offset-based heliocentric positioning approach");

        for (var entry : skyboxData.entrySet()) {
            String objectName = entry.getKey(); // e.g., "planet_mars"
            StarSystemModel.CelestialExpressions expressions = entry.getValue();

            try {
                // Create planet object with offset-based orbital mechanics
                JsonObject planetObject = createRealisticPlanetObject(expressions, observerPlanetKey);

                // Write planet object file
                Identifier objectId = new Identifier("celestial", "sky/" + observerPlanetKey + "/objects/" + objectName);
                Path objectPath = output.getResolver(net.minecraft.data.DataOutput.OutputType.RESOURCE_PACK, "")
                        .resolveJson(objectId);

                DataProvider.writeToPath(writer, planetObject, objectPath);

                System.out.println("    → Generated " + objectName + ".json with offset-based mechanics for " + observerPlanetKey);
                Thread.sleep(10); // Prevent race conditions

            } catch (Exception e) {
                System.err.println("    ❌ Failed to generate offset-based planet object: " + expressions.planet.getName());
                e.printStackTrace();
            }
        }
    }

    /**
     * Create a planet object with realistic orbital mechanics using offset-based positioning
     * UPDATED: Now uses heliocentric position offsets for correct planetary motion
     */
    private JsonObject createRealisticPlanetObject(StarSystemModel.CelestialExpressions expressions, String observerPlanetKey) {
        JsonObject planetObj = new JsonObject();

        PlanetaryBody planet = expressions.planet;

        // Use planet-specific texture
        planetObj.addProperty("texture", "minecraft:textures/environment/moon_phases.png");

        // Display settings with heliocentric position offsets
        JsonObject display = new JsonObject();
        display.addProperty("scale", expressions.scale);           // Dynamic scale based on distance
        display.addProperty("pos_x", expressions.posX);            // NEW: Heliocentric X offset
        display.addProperty("pos_y", expressions.posY);            // NEW: Heliocentric Y offset
        display.addProperty("pos_z", expressions.posZ);            // NEW: Z offset (always "0" for now)
        display.addProperty("distance", expressions.distance);     // Dynamic distance
        planetObj.add("display", display);

        // UPDATED: Simplified rotation (geocentric base + heliocentric offset via position)
        JsonObject rotation = new JsonObject();
        rotation.addProperty("degrees_x", expressions.rotationX);  // Geocentric rotation
        rotation.addProperty("degrees_y", expressions.rotationY);  // Always "0"
        rotation.addProperty("degrees_z", expressions.rotationZ);  // Always "0" (no inclinations yet)
        rotation.addProperty("base_degrees_x", "-90");
        rotation.addProperty("base_degrees_z", "-90");
        planetObj.add("rotation", rotation);

        // Properties with realistic visibility
        JsonObject properties = new JsonObject();
        properties.addProperty("is_solid", false);
        properties.addProperty("blend", true);
        properties.addProperty("alpha", expressions.alpha);       // Distance and daylight-based visibility
        properties.addProperty("has_moon_phases", true);
        properties.addProperty("moon_phase", "moonPhase");


        // Planet color based on real planetary appearance
        JsonObject color = new JsonObject();
        String[] planetRGB = getPlanetRGB(planet);
        color.addProperty("red", planetRGB[0]);
        color.addProperty("green", planetRGB[1]);
        color.addProperty("blue", planetRGB[2]);
        properties.add("color", color);

        planetObj.add("properties", properties);

        // Add debug info for orbital mechanics
        planetObj.addProperty("_debug_planet", planet.getName());
        planetObj.addProperty("_debug_orbital_distance", String.format("%.3f AU", planet.getSemiMajorAxisAU()));
        planetObj.addProperty("_debug_orbital_period", String.format("%.2f MC days", planet.getOrbitalPeriodMCDays()));
        planetObj.addProperty("_debug_approach", "offset_based_heliocentric");
        planetObj.addProperty("_debug_rotation_type", "geocentric_base");
        planetObj.addProperty("_debug_positioning", "heliocentric_offsets");

        return planetObj;
    }

    /**
     * Get planet RGB colors based on real planetary appearance
     */
    private String[] getPlanetRGB(PlanetaryBody planet) {
        return switch (planet.getPlanetKey()) {
            case "mercury" -> new String[]{"0.7", "0.7", "0.7"};     // Gray
            case "venus" -> new String[]{"1.0", "0.9", "0.7"};       // Bright yellow-white
            case "earth" -> new String[]{"0.4", "0.7", "1.0"};       // Blue-white
            case "mars" -> new String[]{"1.0", "0.5", "0.3"};        // Red-orange
            case "pluto" -> new String[]{"0.8", "0.7", "0.6"};       // Brownish
            default -> new String[]{"1.0", "1.0", "1.0"};            // White fallback
        };
    }

    private void writeObjectWithDelay(DataWriter writer, JsonObject object, String planetKey, String objectName) {
        try {
            Identifier objectId = new Identifier("celestial", "sky/" + planetKey + "/objects/" + objectName);
            Path objectPath = output.getResolver(net.minecraft.data.DataOutput.OutputType.RESOURCE_PACK, "")
                    .resolveJson(objectId);

            System.out.println("    → Writing " + objectName + ".json to: " + objectPath);

            // Write the file
            DataProvider.writeToPath(writer, object, objectPath);

            // Small delay to prevent race conditions
            Thread.sleep(10);

            System.out.println("    → Write call completed for " + objectName + ".json");

        } catch (Exception e) {
            System.err.println("    ❌ Exception writing " + objectName + ".json: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * FIXED: Create sun object with correct day/night timing
     */
    private JsonObject createSunObject(StarSystemModel.CelestialExpressions expressions, PlanetConfig config, String observerPlanetKey) {
        JsonObject sun = new JsonObject();

        sun.addProperty("texture", "minecraft:textures/environment/sun.png");

        // Display settings
        JsonObject display = new JsonObject();
        display.addProperty("scale", expressions.scale);
        display.addProperty("pos_x", expressions.posX);
        display.addProperty("pos_y", expressions.posY);
        display.addProperty("pos_z", expressions.posZ);
        display.addProperty("distance", expressions.distance);
        sun.add("display", display);

        // FIXED: Adjust rotation so sun rises in morning, not at night
        JsonObject rotation = new JsonObject();
        rotation.addProperty("degrees_x", "0");
        rotation.addProperty("degrees_y", "0");
        rotation.addProperty("degrees_z", "0");
        rotation.addProperty("base_degrees_x", "-90");
        rotation.addProperty("base_degrees_z", "-90");
        sun.add("rotation", rotation);

        // Properties
        JsonObject properties = new JsonObject();
        properties.addProperty("is_solid", false);
        properties.addProperty("blend", true);

        // FIXED: Sun always visible (no alpha changes)
        properties.addProperty("alpha", "1 - rainAlpha");

        // Sun color affected by atmospheric composition
        JsonObject color = new JsonObject();
        AtmosphereComposition atmosphere = config.getAtmosphereComposition();
        String[] sunRGB = getSunRGB(atmosphere);
        color.addProperty("red", sunRGB[0]);
        color.addProperty("green", sunRGB[1]);
        color.addProperty("blue", sunRGB[2]);
        properties.add("color", color);

        sun.add("properties", properties);

        return sun;
    }

    /**
     * FIXED: Create moon with atmosphere-dependent visibility
     */
    private JsonObject createMoonObject(PlanetConfig config) {
        JsonObject moon = new JsonObject();

        moon.addProperty("texture", "minecraft:textures/environment/moon_phases.png");

        // Display settings
        JsonObject display = new JsonObject();
        display.addProperty("scale", "20");
        display.addProperty("pos_x", "0");
        display.addProperty("pos_y", "0");
        display.addProperty("pos_z", "0");
        display.addProperty("distance", "200");
        moon.add("display", display);

        // Moon rotation with slower orbit and inclination
        JsonObject rotation = new JsonObject();
        rotation.addProperty("degrees_x", "(skyAngle * 2.96) + 180");
        rotation.addProperty("degrees_y", "0");
        rotation.addProperty("degrees_z", "(skyAngle * 0.1)");
        rotation.addProperty("base_degrees_x", "-90");
        rotation.addProperty("base_degrees_z", "-90");
        moon.add("rotation", rotation);

        // Properties
        JsonObject properties = new JsonObject();
        properties.addProperty("has_moon_phases", true);
        properties.addProperty("moon_phase", "0");
        properties.addProperty("is_solid", false);
        properties.addProperty("blend", true);

        // FIXED: Atmosphere-dependent moon visibility
        String moonAlphaFormula = getMoonAlphaFormula(config.getAtmosphereComposition(),
                config.getAtmosphericDensity());
        properties.addProperty("alpha", moonAlphaFormula);

        // Moon color (always white)
        JsonObject color = new JsonObject();
        color.addProperty("red", "1");
        color.addProperty("blue", "1");
        color.addProperty("green", "1");
        properties.add("color", color);

        moon.add("properties", properties);

        return moon;
    }

    /**
     * Calculate moon visibility based on atmospheric conditions
     */
    private String getMoonAlphaFormula(AtmosphereComposition atmosphere, double atmosphericDensity) {
        return switch (atmosphere) {
            case VACUUM ->
                    "1 - rainAlpha"; // Always full visibility - no atmosphere to scatter light

            case TRACE_ATMOSPHERE ->
                    "1 - rainAlpha"; // Always full visibility - atmosphere too thin to matter

            case NOBLE_GAS_MIXTURE -> {
                if (atmosphericDensity < 0.3) {
                    yield "1 - rainAlpha"; // Thin noble gas atmosphere - full visibility
                } else {
                    yield "max(0.5, (1 - dayLight * 0.5)) * (1 - rainAlpha)"; // Half opacity during day
                }
            }

            default ->
                // All other atmospheres: Half opacity during day, full at night
                    "max(0.5, (1 - dayLight * 0.5)) * (1 - rainAlpha)";
        };
    }

    /**
     * FIXED: Create stars object using populate for proper starfield
     */
    private JsonObject createStarsObject(PlanetConfig config) {
        JsonObject stars = new JsonObject();

        // FIXED: Use solid_color instead of texture
        stars.addProperty("solid_color", "#ffffff");

        // Display settings (base object - will be overridden by populate)
        JsonObject display = new JsonObject();
        display.addProperty("scale", "0");  // Base scale 0, populate will override
        display.addProperty("pos_x", "0");
        display.addProperty("pos_y", "0");
        display.addProperty("pos_z", "0");
        display.addProperty("distance", "150");  // Base distance 0, populate will override
        stars.add("display", display);

        // Rotation settings (same as sun/moon for proper positioning)
        JsonObject rotation = new JsonObject();
        rotation.addProperty("degrees_x", "skyAngle + 90");
        rotation.addProperty("degrees_y", "0");
        rotation.addProperty("degrees_z", "0");
        rotation.addProperty("base_degrees_x", "-90");
        rotation.addProperty("base_degrees_z", "-90");
        stars.add("rotation", rotation);

        // Properties
        JsonObject properties = new JsonObject();
        properties.addProperty("has_moon_phases", false);
        properties.addProperty("is_solid", true);
        properties.addProperty("blend", true);

        // FIXED: Use proper dayLight formula instead of starAlpha
        String starAlphaFormula = getStarAlphaFormula(config.getAtmosphereComposition(),
                config.getAtmosphericDensity());
        properties.addProperty("alpha", starAlphaFormula);

        // Color object format
        JsonObject color = new JsonObject();
        color.addProperty("red", "1");
        color.addProperty("green", "1");
        color.addProperty("blue", "1");
        properties.add("color", color);

        stars.add("properties", properties);

        // FIXED: Add populate section for starfield
        JsonObject populate = new JsonObject();
        populate.addProperty("count", getStarCount(config.getAtmosphereComposition()));

        // Populate rotation (random stars across sky)
        JsonObject populateRotation = new JsonObject();
        populateRotation.addProperty("min_degrees_x", 0);
        populateRotation.addProperty("max_degrees_x", 360);
        populateRotation.addProperty("min_degrees_y", 0);
        populateRotation.addProperty("max_degrees_y", 360);
        populateRotation.addProperty("min_degrees_z", 0);
        populateRotation.addProperty("max_degrees_z", 360);
        populate.add("rotation", populateRotation);

        // Populate display (individual star properties)
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
     * Get star count based on atmospheric density
     */
    private int getStarCount(AtmosphereComposition atmosphere) {
        return switch (atmosphere) {
            case VACUUM -> 600;              // Many stars visible
            case TRACE_ATMOSPHERE -> 500;    // Most stars visible
            case NOBLE_GAS_MIXTURE -> 400;   // Standard count
            default -> 300;                  // Fewer stars in thick atmosphere
        };
    }

    /**
     * FIXED: Proper star alpha formula using dayLight
     */
    private String getStarAlphaFormula(AtmosphereComposition atmosphere, double atmosphericDensity) {
        return switch (atmosphere) {
            case VACUUM ->
                    "1"; // Always visible - no atmosphere

            case TRACE_ATMOSPHERE ->
                    "max((1 - dayLight) * (1 - rainAlpha) - 0.3, 0)"; // Mostly visible

            case NOBLE_GAS_MIXTURE -> {
                if (atmosphericDensity < 0.3) {
                    yield "max((1 - dayLight) * (1 - rainAlpha) - 0.4, 0)"; // Moderate visibility
                } else {
                    yield "max((1 - dayLight) * (1 - rainAlpha) - 0.5, 0)"; // Standard formula
                }
            }

            default ->
                    "max((1 - dayLight) * (1 - rainAlpha) - 0.5, 0)"; // Standard atmospheric scattering
        };
    }

    /**
     * FIXED: Create twilight object with proper format matching the sample
     */
    private JsonObject createTwilightObject(PlanetConfig config) {
        JsonObject twilight = new JsonObject();

        twilight.addProperty("type", "twilight");

        // FIXED: Use solid_color and solid_color_transition instead of color object
        String[] twilightColors = getTwilightColors(config.getAtmosphereComposition());
        twilight.addProperty("solid_color", twilightColors[0]);
        twilight.addProperty("solid_color_transition", twilightColors[1]);

        // FIXED: Add display section
        JsonObject display = new JsonObject();
        display.addProperty("scale", "1");
        display.addProperty("pos_x", "0");
        display.addProperty("pos_y", "0");
        display.addProperty("pos_z", "0");
        display.addProperty("distance", "100");
        twilight.add("display", display);

        // FIXED: Proper rotation section with twilight_rotation
        JsonObject rotation = new JsonObject();
        rotation.addProperty("base_degrees_y", "0");
        rotation.addProperty("twilight_rotation", "skyAngle");  // This was the missing key!
        twilight.add("rotation", rotation);

        // FIXED: Properties section matching sample format
        JsonObject properties = new JsonObject();
        properties.addProperty("is_solid", true);
        properties.addProperty("blend", true);

        // FIXED: Atmosphere-dependent twilight intensity
        String twilightAlphaFormula = getTwilightAlphaFormula(config.getAtmosphereComposition(),
                config.getAtmosphericDensity());
        properties.addProperty("alpha", twilightAlphaFormula);

        twilight.add("properties", properties);

        return twilight;
    }

    // === ATMOSPHERIC CALCULATION METHODS ===

    /**
     * FIXED: Get twilight colors as hex strings (solid_color and solid_color_transition)
     */
    private String[] getTwilightColors(AtmosphereComposition atmosphere) {
        return switch (atmosphere) {
            case VACUUM ->
                    new String[]{"#000000", "#000000"}; // No twilight - black

            case TRACE_ATMOSPHERE ->
                    new String[]{"#330000", "#110000"}; // Very faint dark red

            case OXYGEN_RICH ->
                    new String[]{"#ffe533", "#b33333"}; // Standard yellow to red

            case CARBON_DIOXIDE ->
                    new String[]{"#ff6600", "#cc0000"}; // Orange to deep red (Venus-like)

            case NITROGEN_RICH ->
                    new String[]{"#cc99ff", "#660099"}; // Purple to dark purple

            case METHANE ->
                    new String[]{"#ffaa44", "#994400"}; // Orange-brown twilight

            case WATER_VAPOR_RICH ->
                    new String[]{"#ffcccc", "#ff6666"}; // Pink steam twilight

            case HYDROGEN_SULFIDE ->
                    new String[]{"#ffff66", "#cccc00"}; // Yellow sulfurous twilight

            case NOBLE_GAS_MIXTURE ->
                    new String[]{"#ccccff", "#9999cc"}; // Pale blue twilight
        };
    }

    /**
     * Calculate twilight intensity based on atmospheric conditions
     */
    private String getTwilightAlphaFormula(AtmosphereComposition atmosphere, double atmosphericDensity) {
        return switch (atmosphere) {
            case VACUUM ->
                    "0"; // No twilight - no atmosphere to scatter light

            case TRACE_ATMOSPHERE ->
                    "twilightAlpha(skyAngle) * 0.1"; // Very faint twilight

            case NOBLE_GAS_MIXTURE -> {
                if (atmosphericDensity < 0.3) {
                    yield "twilightAlpha(skyAngle) * 0.3"; // Weak twilight
                } else {
                    yield "twilightAlpha(skyAngle)"; // Normal twilight
                }
            }

            default ->
                    "twilightAlpha(skyAngle)"; // Normal atmospheric scattering
        };
    }

    /**
     * Get sky color hex code (without #)
     */
    private String getAtmosphericSkyColor(AtmosphereComposition atmosphere) {
        return switch (atmosphere) {
            case OXYGEN_RICH -> "#78A7FF";           // Earth-like blue
            case CARBON_DIOXIDE -> "#FFA500";        // Orange (Venus-like)
            case NITROGEN_RICH -> "#9370DB";         // Purple-blue
            case METHANE -> "#D2691E";               // Orange-brown (Titan-like)
            case WATER_VAPOR_RICH -> "#F5F5DC";     // Pale white (steamy)
            case HYDROGEN_SULFIDE -> "#B8860B";     // Yellow-brown (sulfurous)
            case NOBLE_GAS_MIXTURE -> "#E6E6FA";    // Pale lavender
            case TRACE_ATMOSPHERE -> "#2F2F2F";     // Dark gray (thin)
            case VACUUM -> "#000000";               // Black (space)
        };
    }

    /**
     * Get fog color (without #)
     */
    private String getAtmosphericFogColor(AtmosphereComposition atmosphere) {
        return switch (atmosphere) {
            case OXYGEN_RICH -> "#C0D8FF";           // Light blue
            case CARBON_DIOXIDE -> "#FFE4B5";        // Light orange
            case NITROGEN_RICH -> "#DDA0DD";         // Light purple
            case METHANE -> "#F4A460";               // Sandy brown
            case WATER_VAPOR_RICH -> "#FFFFFF";     // White (steam)
            case HYDROGEN_SULFIDE -> "#FFFF99";     // Light yellow
            case NOBLE_GAS_MIXTURE -> "#F8F8FF";    // Ghost white
            case TRACE_ATMOSPHERE -> "#696969";     // Dim gray
            case VACUUM -> "#000000";               // Very dark gray
        };
    }

    /**
     * Get cloud color (without #)
     */
    private String getAtmosphericCloudColor(AtmosphereComposition atmosphere) {
        return switch (atmosphere) {
            case OXYGEN_RICH -> "#9CC7FF";           // Medium blue
            case CARBON_DIOXIDE -> "#FFD4AA";        // Medium orange
            case NITROGEN_RICH -> "#B885DD";         // Medium purple
            case METHANE -> "#E3976F";               // Medium brown
            case WATER_VAPOR_RICH -> "#FAFAFA";     // Off-white
            case HYDROGEN_SULFIDE -> "#DDD555";     // Medium yellow
            case NOBLE_GAS_MIXTURE -> "#F2F2FD";    // Very light lavender
            case TRACE_ATMOSPHERE -> "#555555";     // Medium gray
            case VACUUM -> "#000000";               // Very dark
        };
    }

    /**
     * Get sun RGB values as strings with atmospheric filtering
     */
    private String[] getSunRGB(AtmosphereComposition atmosphere) {
        return switch (atmosphere) {
            case VACUUM, TRACE_ATMOSPHERE ->
                    new String[]{"1", "1", "1"};           // Pure white (no atmospheric filtering)

            case NOBLE_GAS_MIXTURE ->
                    new String[]{"0.98", "0.99", "1"};     // Very slight blue tint

            case OXYGEN_RICH ->
                    new String[]{"1", "1", "1"};           // White (normal)

            case CARBON_DIOXIDE ->
                    new String[]{"1", "0.84", "0"};        // Golden (filtered)

            case NITROGEN_RICH ->
                    new String[]{"0.9", "0.9", "0.98"};   // Pale (filtered)

            case METHANE ->
                    new String[]{"1", "0.65", "0"};       // Orange (filtered)

            case WATER_VAPOR_RICH ->
                    new String[]{"1", "1", "0.88"};       // Light yellow (hazy)

            case HYDROGEN_SULFIDE ->
                    new String[]{"1", "1", "0.6"};        // Yellow (sulfurous)
        };
    }

    /**
     * Calculate fog start distance
     */
    private double calculateFogStart(double atmosphericDensity) {
        double baseStart = 80.0;
        double densityFactor = Math.max(0.1, Math.min(2.0, atmosphericDensity));
        return baseStart * (2.0 - densityFactor);
    }

    /**
     * Calculate fog end distance
     */
    private double calculateFogEnd(double atmosphericDensity) {
        double baseEnd = 200.0;
        double densityFactor = Math.max(0.1, Math.min(2.0, atmosphericDensity));
        return baseEnd * (2.0 - densityFactor);
    }

    @Override
    public String getName() {
        return "Terradyne Celestial Sky Data (Fixed Twilight Format)";
    }
}