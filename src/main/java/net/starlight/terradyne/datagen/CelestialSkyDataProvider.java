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

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Generates Celestial sky definition files for each Terradyne planet
 * Follows exact Celestial structure with proper dimensions.json and sky.json format
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
     * Generate the main dimensions.json file with proper Celestial format
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

            // Write to assets/celestial/sky/dimensions.json
            Identifier dimensionsId = new Identifier("celestial", "sky/dimensions");
            Path dimensionsPath = output.getResolver(net.minecraft.data.DataOutput.OutputType.RESOURCE_PACK, "assets")
                    .resolveJson(dimensionsId);

            DataProvider.writeToPath(writer, dimensionsFile, dimensionsPath);

            System.out.println("✅ Generated dimensions.json with " + planets.size() + " dimensions: " + planets.keySet());

        } catch (Exception e) {
            System.err.println("❌ Failed to generate dimensions.json");
            e.printStackTrace();
        }
    }

    /**
     * Generate individual sky files for each planet dimension
     */
    private void generatePlanetSkyFiles(DataWriter writer) {
        System.out.println("=== GENERATING PLANET SKY FILES ===");

        var planets = HardcodedPlanets.getAllPlanets();
        System.out.println("Generating sky files for " + planets.size() + " planets");

        int successCount = 0;
        int totalCount = planets.size();

        for (var entry : planets.entrySet()) {
            String planetKey = entry.getKey();
            PlanetConfig config = entry.getValue();

            System.out.println("Processing sky for planet: " + planetKey);

            try {
                // Create sky.json for this planet
                JsonObject skyJson = createPlanetSkyJson(config);

                // Write to assets/celestial/sky/[planetKey]/sky.json
                Identifier skyId = new Identifier("celestial", "sky/" + planetKey + "/sky");
                Path skyPath = output.getResolver(net.minecraft.data.DataOutput.OutputType.RESOURCE_PACK, "assets")
                        .resolveJson(skyId);

                DataProvider.writeToPath(writer, skyJson, skyPath);

                // Generate celestial objects in objects/ folder
                generatePlanetObjects(writer, planetKey, config);

                successCount++;
                System.out.println("✅ Successfully generated sky for: " + planetKey + " (" + successCount + "/" + totalCount + ")");

            } catch (Exception e) {
                System.err.println("❌ Failed to generate sky for planet: " + planetKey);
                e.printStackTrace();
            }
        }

        System.out.println("=== CELESTIAL SKY GENERATION COMPLETE ===");
        System.out.println("Successfully generated " + successCount + " out of " + totalCount + " planet skies");
    }

    /**
     * Create Celestial sky.json with proper format
     */
    private JsonObject createPlanetSkyJson(PlanetConfig config) {
        JsonObject sky = new JsonObject();

        // Sky objects array
        JsonArray skyObjects = new JsonArray();
        skyObjects.add("twilight");
        skyObjects.add("moon");
        skyObjects.add("sun");
        skyObjects.add("stars");
        sky.add("sky_objects", skyObjects);

        // Environment settings
        JsonObject environment = new JsonObject();

        // Fog color
        JsonObject fogColor = new JsonObject();
        fogColor.addProperty("base_color", getAtmosphericFogColor(config.getAtmosphereComposition()));
        environment.add("fog_color", fogColor);

        // Sky color
        JsonObject skyColor = new JsonObject();
        skyColor.addProperty("base_color", getAtmosphericSkyColor(config.getAtmosphereComposition()));
        environment.add("sky_color", skyColor);

        // Clouds
        JsonObject clouds = new JsonObject();
        clouds.addProperty("height", "128");
        clouds.addProperty("color", getAtmosphericCloudColor(config.getAtmosphereComposition()));
        environment.add("clouds", clouds);

        // Fog settings
        JsonObject fog = new JsonObject();
        fog.addProperty("fog_start", String.valueOf(calculateFogStart(config.getAtmosphericDensity())));
        fog.addProperty("fog_end", String.valueOf(calculateFogEnd(config.getAtmosphericDensity())));
        environment.add("fog", fog);

        // Required twilight alpha (deprecated but required)
        environment.addProperty("NOTE", "Do not change this value!");
        environment.addProperty("twilight_alpha", "0");

        sky.add("environment", environment);

        return sky;
    }

    /**
     * Generate celestial objects in objects/ folder for each planet
     */
    private void generatePlanetObjects(DataWriter writer, String planetKey, PlanetConfig config) {
        try {
            // Generate objects/sun.json
            JsonObject sun = createSunObject(config);
            Identifier sunId = new Identifier("celestial", "sky/" + planetKey + "/objects/sun");
            Path sunPath = output.getResolver(net.minecraft.data.DataOutput.OutputType.RESOURCE_PACK, "assets")
                    .resolveJson(sunId);
            DataProvider.writeToPath(writer, sun, sunPath);

            // Generate objects/moon.json
            JsonObject moon = createMoonObject(config);
            Identifier moonId = new Identifier("celestial", "sky/" + planetKey + "/objects/moon");
            Path moonPath = output.getResolver(net.minecraft.data.DataOutput.OutputType.RESOURCE_PACK, "assets")
                    .resolveJson(moonId);
            DataProvider.writeToPath(writer, moon, moonPath);

            // Generate objects/stars.json
            JsonObject stars = createStarsObject(config);
            Identifier starsId = new Identifier("celestial", "sky/" + planetKey + "/objects/stars");
            Path starsPath = output.getResolver(net.minecraft.data.DataOutput.OutputType.RESOURCE_PACK, "assets")
                    .resolveJson(starsId);
            DataProvider.writeToPath(writer, stars, starsPath);

            // Generate objects/twilight.json
            JsonObject twilight = createTwilightObject(config);
            Identifier twilightId = new Identifier("celestial", "sky/" + planetKey + "/objects/twilight");
            Path twilightPath = output.getResolver(net.minecraft.data.DataOutput.OutputType.RESOURCE_PACK, "assets")
                    .resolveJson(twilightId);
            DataProvider.writeToPath(writer, twilight, twilightPath);

            System.out.println("  ✓ Generated celestial objects for " + planetKey);

        } catch (Exception e) {
            System.err.println("  ❌ Failed to generate objects for " + planetKey + ": " + e.getMessage());
        }
    }

    /**
     * Create sun object with proper Celestial format
     */
    private JsonObject createSunObject(PlanetConfig config) {
        JsonObject sun = new JsonObject();

        sun.addProperty("texture", "minecraft:textures/environment/sun.png");

        // Display settings
        JsonObject display = new JsonObject();
        double starDistance = config.getDistanceFromStar();
        double sunScale = Math.max(10.0, Math.min(40.0, 3000.0 / starDistance)); // Scale based on distance
        display.addProperty("scale", String.valueOf(sunScale));
        display.addProperty("pos_x", "0");
        display.addProperty("pos_y", "0");
        display.addProperty("pos_z", "0");
        display.addProperty("distance", "-100");
        sun.add("display", display);

        // Rotation settings
        JsonObject rotation = new JsonObject();
        rotation.addProperty("degrees_x", "skyAngle + 90");
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
     * Create moon object with exact Celestial format
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
        display.addProperty("distance", "-100");
        moon.add("display", display);

        // Rotation settings
        JsonObject rotation = new JsonObject();
        rotation.addProperty("degrees_x", "skyAngle + 90");
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
        properties.addProperty("alpha", "1 - rainAlpha");

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
     * Create stars object with proper texture reference
     */
    private JsonObject createStarsObject(PlanetConfig config) {
        JsonObject stars = new JsonObject();

        stars.addProperty("texture", "minecraft:textures/environment/end_sky.png");

        // Display settings
        JsonObject display = new JsonObject();
        display.addProperty("scale", "30");
        display.addProperty("pos_x", "0");
        display.addProperty("pos_y", "0");
        display.addProperty("pos_z", "0");
        display.addProperty("distance", "-100");
        stars.add("display", display);

        // Properties
        JsonObject properties = new JsonObject();
        properties.addProperty("is_solid", false);
        properties.addProperty("blend", true);

        // Star visibility affected by atmospheric density
        double starAlpha = Math.max(0.1, 1.0 - (config.getAtmosphericDensity() * 0.7));
        properties.addProperty("alpha", String.valueOf(starAlpha) + " * starAlpha");

        // Star color (always white)
        JsonObject color = new JsonObject();
        color.addProperty("red", "1");
        color.addProperty("blue", "1");
        color.addProperty("green", "1");
        properties.add("color", color);

        stars.add("properties", properties);

        return stars;
    }

    /**
     * Create twilight object (basic implementation)
     */
    private JsonObject createTwilightObject(PlanetConfig config) {
        JsonObject twilight = new JsonObject();

        twilight.addProperty("type", "twilight");

        // Properties
        JsonObject properties = new JsonObject();
        properties.addProperty("is_solid", false);
        properties.addProperty("blend", true);
        properties.addProperty("alpha", "twilightAlpha");

        // Twilight color based on atmospheric composition
        JsonObject color = new JsonObject();
        String[] twilightRGB = getTwilightRGB(config.getAtmosphereComposition());
        color.addProperty("red", twilightRGB[0]);
        color.addProperty("green", twilightRGB[1]);
        color.addProperty("blue", twilightRGB[2]);
        properties.add("color", color);

        twilight.add("properties", properties);

        return twilight;
    }

    // === COLOR CALCULATION METHODS ===

    /**
     * Get sky color hex code (without #)
     */
    private String getAtmosphericSkyColor(AtmosphereComposition atmosphere) {
        return switch (atmosphere) {
            case OXYGEN_RICH -> "78A7FF";           // Earth-like blue
            case CARBON_DIOXIDE -> "FFA500";        // Orange (Venus-like)
            case NITROGEN_RICH -> "9370DB";         // Purple-blue
            case METHANE -> "D2691E";               // Orange-brown (Titan-like)
            case WATER_VAPOR_RICH -> "F5F5DC";     // Pale white (steamy)
            case HYDROGEN_SULFIDE -> "B8860B";     // Yellow-brown (sulfurous)
            case NOBLE_GAS_MIXTURE -> "E6E6FA";    // Pale lavender
            case TRACE_ATMOSPHERE -> "2F2F2F";     // Dark gray (thin)
            case VACUUM -> "000000";               // Black (space)
        };
    }

    /**
     * Get fog color (without #)
     */
    private String getAtmosphericFogColor(AtmosphereComposition atmosphere) {
        return switch (atmosphere) {
            case OXYGEN_RICH -> "C0D8FF";           // Light blue
            case CARBON_DIOXIDE -> "FFE4B5";        // Light orange
            case NITROGEN_RICH -> "DDA0DD";         // Light purple
            case METHANE -> "F4A460";               // Sandy brown
            case WATER_VAPOR_RICH -> "FFFFFF";     // White (steam)
            case HYDROGEN_SULFIDE -> "FFFF99";     // Light yellow
            case NOBLE_GAS_MIXTURE -> "F8F8FF";    // Ghost white
            case TRACE_ATMOSPHERE -> "696969";     // Dim gray
            case VACUUM -> "000000";               // Very dark gray
        };
    }

    /**
     * Get cloud color (without #)
     */
    private String getAtmosphericCloudColor(AtmosphereComposition atmosphere) {
        return switch (atmosphere) {
            case OXYGEN_RICH -> "9CC7FF";           // Medium blue
            case CARBON_DIOXIDE -> "FFD4AA";        // Medium orange
            case NITROGEN_RICH -> "B885DD";         // Medium purple
            case METHANE -> "E3976F";               // Medium brown
            case WATER_VAPOR_RICH -> "FAFAFA";     // Off-white
            case HYDROGEN_SULFIDE -> "DDD555";     // Medium yellow
            case NOBLE_GAS_MIXTURE -> "F2F2FD";    // Very light lavender
            case TRACE_ATMOSPHERE -> "555555";     // Medium gray
            case VACUUM -> "000000";               // Very dark
        };
    }

    /**
     * Get sun RGB values as strings
     */
    private String[] getSunRGB(AtmosphereComposition atmosphere) {
        return switch (atmosphere) {
            case OXYGEN_RICH -> new String[]{"1", "1", "1"};           // White (normal)
            case CARBON_DIOXIDE -> new String[]{"1", "0.84", "0"};     // Golden (filtered)
            case NITROGEN_RICH -> new String[]{"0.9", "0.9", "0.98"};  // Pale (filtered)
            case METHANE -> new String[]{"1", "0.65", "0"};            // Orange (filtered)
            case WATER_VAPOR_RICH -> new String[]{"1", "1", "0.88"};   // Light yellow (hazy)
            case HYDROGEN_SULFIDE -> new String[]{"1", "1", "0.6"};    // Yellow (sulfurous)
            case NOBLE_GAS_MIXTURE -> new String[]{"0.94", "0.97", "1"}; // Alice blue (clear)
            case TRACE_ATMOSPHERE -> new String[]{"1", "1", "1"};      // White (no filtering)
            case VACUUM -> new String[]{"1", "1", "1"};               // White (no atmosphere)
        };
    }

    /**
     * Get twilight RGB values as strings
     */
    private String[] getTwilightRGB(AtmosphereComposition atmosphere) {
        return switch (atmosphere) {
            case OXYGEN_RICH -> new String[]{"1", "0.5", "0.2"};       // Orange twilight
            case CARBON_DIOXIDE -> new String[]{"1", "0.3", "0"};      // Red twilight
            case NITROGEN_RICH -> new String[]{"0.8", "0.4", "0.9"};   // Purple twilight
            case METHANE -> new String[]{"0.9", "0.6", "0.2"};        // Brown twilight
            case WATER_VAPOR_RICH -> new String[]{"1", "0.9", "0.9"}; // Pink twilight
            case HYDROGEN_SULFIDE -> new String[]{"1", "0.8", "0.4"}; // Yellow twilight
            case NOBLE_GAS_MIXTURE -> new String[]{"0.9", "0.9", "1"}; // Pale twilight
            case TRACE_ATMOSPHERE -> new String[]{"0.5", "0.5", "0.5"}; // Gray twilight
            case VACUUM -> new String[]{"0", "0", "0"};               // No twilight
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
        return "Terradyne Celestial Sky Data (Proper Celestial Format)";
    }
}