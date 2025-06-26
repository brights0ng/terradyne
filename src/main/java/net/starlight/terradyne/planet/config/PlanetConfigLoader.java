package net.starlight.terradyne.planet.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.minecraft.server.MinecraftServer;
import net.starlight.terradyne.Terradyne;
import net.starlight.terradyne.planet.physics.AtmosphereComposition;
import net.starlight.terradyne.planet.physics.CrustComposition;
import net.starlight.terradyne.planet.physics.PlanetConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads planet configurations from JSON files in world save directory
 */
public class PlanetConfigLoader {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    /**
     * JSON representation of a planet configuration
     */
    public static class PlanetConfigJson {
        public String name;
        public boolean enabled = true;
        public int circumference = 40000;
        public int distanceFromStar = 150;
        public String crustComposition = "SILICATE";
        public String atmosphereComposition = "OXYGEN_RICH";
        public double tectonicActivity = 0.6;
        public double waterContent = 0.7;
        public double crustalThickness = 35.0;
        public double atmosphericDensity = 1.0;
        public double rotationPeriod = 1.0;
        public double noiseScale = 0.002;
    }

    /**
     * Load all planet configurations from world save directory
     */
    public static Map<String, PlanetConfig> loadAllPlanetConfigs(MinecraftServer server) {
        Map<String, PlanetConfig> configs = new HashMap<>();

        try {
            Path planetsDir = getPlanetsConfigDirectory(server);

            // Create directory if it doesn't exist
            if (!Files.exists(planetsDir)) {
                Files.createDirectories(planetsDir);
                Terradyne.LOGGER.info("Created planets config directory: {}", planetsDir);

                // Generate default planet configs
                generateDefaultPlanetConfigs(planetsDir);
            }

            // Load all JSON files in the planets directory
            try (var stream = Files.list(planetsDir)) {
                stream.filter(path -> path.toString().endsWith(".json"))
                        .forEach(configFile -> {
                            try {
                                PlanetConfig config = loadSinglePlanetConfig(configFile);
                                if (config != null) {
                                    configs.put(config.getPlanetName().toLowerCase(), config);
                                    Terradyne.LOGGER.info("✅ Loaded planet config: {}", config.getPlanetName());
                                }
                            } catch (Exception e) {
                                Terradyne.LOGGER.error("❌ Failed to load planet config from {}: {}", configFile, e.getMessage());
                            }
                        });
            }

            Terradyne.LOGGER.info("Loaded {} planet configurations from {}", configs.size(), planetsDir);

        } catch (IOException e) {
            Terradyne.LOGGER.error("Failed to load planet configurations", e);
        }

        return configs;
    }

    /**
     * Load a single planet configuration from JSON file
     */
    private static PlanetConfig loadSinglePlanetConfig(Path configFile) {
        try {
            String jsonContent = Files.readString(configFile);
            PlanetConfigJson jsonConfig = GSON.fromJson(jsonContent, PlanetConfigJson.class);

            // Validate and convert to PlanetConfig
            return convertJsonToPlanetConfig(jsonConfig, configFile);

        } catch (JsonSyntaxException e) {
            Terradyne.LOGGER.error("Invalid JSON syntax in {}: {}", configFile, e.getMessage());
            return null;
        } catch (IOException e) {
            Terradyne.LOGGER.error("Failed to read config file {}: {}", configFile, e.getMessage());
            return null;
        }
    }

    /**
     * Convert JSON config to PlanetConfig with validation and defaults
     */
    private static PlanetConfig convertJsonToPlanetConfig(PlanetConfigJson json, Path configFile) {
        List<String> warnings = new ArrayList<>();

        // Use filename as fallback name
        String planetName = json.name != null ? json.name :
                configFile.getFileName().toString().replace(".json", "");

        // Check if planet is enabled
        if (!json.enabled) {
            Terradyne.LOGGER.info("Planet '{}' is disabled in config, skipping", planetName);
            return null;
        }

        // Generate seed from planet name
        long seed = planetName.hashCode();

        // Create config with defaults
        PlanetConfig config = new PlanetConfig(planetName, seed);

        // Apply values with validation and defaults
        config.setCircumference(validateRange(json.circumference, 1000, 100000, 40000, "circumference", warnings));
        config.setDistanceFromStar(validateRange(json.distanceFromStar, 30, 600, 150, "distanceFromStar", warnings));

        // Validate enums with fallbacks
        config.setCrustComposition(parseEnum(json.crustComposition, CrustComposition.class, CrustComposition.SILICATE, "crustComposition", warnings));
        config.setAtmosphereComposition(parseEnum(json.atmosphereComposition, AtmosphereComposition.class, AtmosphereComposition.OXYGEN_RICH, "atmosphereComposition", warnings));

        // Validate doubles
        config.setTectonicActivity(validateRange(json.tectonicActivity, 0.0, 1.0, 0.6, "tectonicActivity", warnings));
        config.setWaterContent(validateRange(json.waterContent, 0.0, 1.0, 0.7, "waterContent", warnings));
        config.setCrustalThickness(validateRange(json.crustalThickness, 2.0, 70.0, 35.0, "crustalThickness", warnings));
        config.setAtmosphericDensity(validateRange(json.atmosphericDensity, 0.0, 1.0, 1.0, "atmosphericDensity", warnings));
        config.setRotationPeriod(validateRange(json.rotationPeriod, 0.1, 20.0, 1.0, "rotationPeriod", warnings));
        config.setNoiseScale(validateRange(json.noiseScale, 0.0001, 0.01, 0.002, "noiseScale", warnings));

        // Log any warnings
        if (!warnings.isEmpty()) {
            Terradyne.LOGGER.warn("Planet config '{}' had {} validation issues:", planetName, warnings.size());
            warnings.forEach(warning -> Terradyne.LOGGER.warn("  - {}", warning));
        }

        return config;
    }

    /**
     * Validate numeric range with fallback to default
     */
    private static <T extends Number & Comparable<T>> T validateRange(T value, T min, T max, T defaultValue, String fieldName, List<String> warnings) {
        if (value == null) {
            warnings.add(fieldName + " is null, using default: " + defaultValue);
            return defaultValue;
        }

        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            warnings.add(fieldName + " value " + value + " out of range [" + min + "-" + max + "], using default: " + defaultValue);
            return defaultValue;
        }

        return value;
    }

    /**
     * Parse enum with fallback to default
     */
    private static <E extends Enum<E>> E parseEnum(String value, Class<E> enumClass, E defaultValue, String fieldName, List<String> warnings) {
        if (value == null || value.trim().isEmpty()) {
            warnings.add(fieldName + " is null/empty, using default: " + defaultValue);
            return defaultValue;
        }

        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            warnings.add(fieldName + " value '" + value + "' is invalid, using default: " + defaultValue);
            return defaultValue;
        }
    }

    /**
     * Get the planets config directory for the current world
     */
    private static Path getPlanetsConfigDirectory(MinecraftServer server) {
        Path worldDir = server.getSavePath(net.minecraft.util.WorldSavePath.ROOT);
        return worldDir.resolve("terradyne").resolve("planets");
    }

    /**
     * Generate default planet configuration files
     */
    private static void generateDefaultPlanetConfigs(Path planetsDir) {
        try {
            Terradyne.LOGGER.info("Generating default planet configurations...");

            // Earth configuration
            PlanetConfigJson earth = new PlanetConfigJson();
            earth.name = "Earth";
            earth.enabled = true;
            earth.circumference = 40000;
            earth.distanceFromStar = 150;
            earth.crustComposition = "SILICATE";
            earth.atmosphereComposition = "OXYGEN_RICH";
            earth.tectonicActivity = 0.6;
            earth.waterContent = 0.7;
            earth.crustalThickness = 35.0;
            earth.atmosphericDensity = 1.0;
            earth.rotationPeriod = 1.0;
            earth.noiseScale = 0.002;

            // Venus configuration
            PlanetConfigJson venus = new PlanetConfigJson();
            venus.name = "Venus";
            venus.enabled = true;
            venus.circumference = 38000;
            venus.distanceFromStar = 108;
            venus.crustComposition = "BASALTIC";
            venus.atmosphereComposition = "CARBON_DIOXIDE";
            venus.tectonicActivity = 0.3;
            venus.waterContent = 0.0;
            venus.crustalThickness = 40.0;
            venus.atmosphericDensity = 1.0;
            venus.rotationPeriod = 243.0;
            venus.noiseScale = 0.002;

            // Mars configuration
            PlanetConfigJson mars = new PlanetConfigJson();
            mars.name = "Mars";
            mars.enabled = true;
            mars.circumference = 21000;
            mars.distanceFromStar = 228;
            mars.crustComposition = "FERROUS";
            mars.atmosphereComposition = "TRACE_ATMOSPHERE";
            mars.tectonicActivity = 0.1;
            mars.waterContent = 0.1;
            mars.crustalThickness = 50.0;
            mars.atmosphericDensity = 0.01;
            mars.rotationPeriod = 1.03;
            mars.noiseScale = 0.003;

            // Write configuration files
            writeConfigFile(planetsDir.resolve("earth.json"), earth);
            writeConfigFile(planetsDir.resolve("venus.json"), venus);
            writeConfigFile(planetsDir.resolve("mars.json"), mars);

            Terradyne.LOGGER.info("✅ Generated default planet configurations: Earth, Venus, Mars");

        } catch (IOException e) {
            Terradyne.LOGGER.error("Failed to generate default planet configurations", e);
        }
    }

    /**
     * Write a configuration file
     */
    private static void writeConfigFile(Path configFile, PlanetConfigJson config) throws IOException {
        String json = GSON.toJson(config);
        Files.writeString(configFile, json);
        Terradyne.LOGGER.debug("Created config file: {}", configFile);
    }
}