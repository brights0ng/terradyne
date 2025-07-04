package net.starlight.terradyne.planet.mapping;

import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.biome.Biome;
import net.starlight.terradyne.Terradyne;
import net.starlight.terradyne.planet.biome.BiomeClassificationSystem;
import net.starlight.terradyne.planet.physics.PlanetModel;
import net.starlight.terradyne.planet.terrain.UniversalChunkGenerator;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Exports climate and terrain maps as PNG images for visualization
 * Each pixel represents one chunk (16x16 blocks) sampled at chunk center
 * UPDATED: Now includes physics-based biome map export
 */
public class ClimateMapExporter {

    // Image specifications
    private static final int IMAGE_SIZE = 512; // 512x512 pixels
    private static final int CHUNKS_RADIUS = IMAGE_SIZE / 2; // 256 chunks in each direction from origin

    /**
     * Export all climate maps for a planet
     */
    public static void exportAllMaps(MinecraftServer server, String planetName) {
        try {
            Terradyne.LOGGER.info("=== EXPORTING CLIMATE MAPS FOR {} ===", planetName.toUpperCase());

            // Load planet model - we'll need to get this from the server context
            PlanetModel planetModel = loadPlanetModel(server, planetName);
            if (planetModel == null) {
                Terradyne.LOGGER.error("Cannot export maps: Planet '{}' not found or not loaded", planetName);
                return;
            }

            // Create export directory
            Path exportDir = getExportDirectory(server);
            Files.createDirectories(exportDir);

            // Export each map type
            exportTerrainMap(planetModel, exportDir, planetName);
            exportTectonicMap(planetModel, exportDir, planetName);
            exportTemperatureMap(planetModel, exportDir, planetName);
            exportWindSpeedMap(planetModel, exportDir, planetName);
            exportHumidityMap(planetModel, exportDir, planetName);
            exportVolatilityMap(planetModel, exportDir, planetName); // NEW
            exportBiomeMap(planetModel, exportDir, planetName);      // NEW

            Terradyne.LOGGER.info("✅ Climate maps exported to: {}", exportDir);

        } catch (Exception e) {
            Terradyne.LOGGER.error("Failed to export climate maps for '{}': {}", planetName, e.getMessage(), e);
        }
    }

    /**
     * Export terrain height map
     */
    private static void exportTerrainMap(PlanetModel planetModel, Path exportDir, String planetName) throws IOException {
        Terradyne.LOGGER.info("Exporting terrain map...");

        BufferedImage image = new BufferedImage(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.TYPE_INT_RGB);

        // Find terrain height range for color mapping
        double minHeight = Double.MAX_VALUE;
        double maxHeight = Double.MIN_VALUE;
        double seaLevel = planetModel.getPlanetData().getSeaLevel();

        // First pass: find height range
        for (int pixelX = 0; pixelX < IMAGE_SIZE; pixelX++) {
            for (int pixelZ = 0; pixelZ < IMAGE_SIZE; pixelZ++) {
                int[] worldCoords = pixelToWorldCoords(pixelX, pixelZ);
                double height = planetModel.getTerrainHeight(worldCoords[0], worldCoords[1]);
                minHeight = Math.min(minHeight, height);
                maxHeight = Math.max(maxHeight, height);
            }
        }

        // Second pass: generate image
        for (int pixelX = 0; pixelX < IMAGE_SIZE; pixelX++) {
            for (int pixelZ = 0; pixelZ < IMAGE_SIZE; pixelZ++) {
                int[] worldCoords = pixelToWorldCoords(pixelX, pixelZ);
                double height = planetModel.getTerrainHeight(worldCoords[0], worldCoords[1]);

                Color color = getTerrainColor(height, seaLevel, minHeight, maxHeight);
                image.setRGB(pixelX, pixelZ, color.getRGB());
            }
        }

        Path outputPath = exportDir.resolve(planetName + "_terrain_0_0_" + IMAGE_SIZE + ".png");
        ImageIO.write(image, "PNG", outputPath.toFile());
        Terradyne.LOGGER.info("  ✓ Terrain map: {} (range: {:.1f} to {:.1f})", outputPath.getFileName(), minHeight, maxHeight);
    }

    /**
     * Export tectonic activity map
     */
    private static void exportTectonicMap(PlanetModel planetModel, Path exportDir, String planetName) throws IOException {
        Terradyne.LOGGER.info("Exporting tectonic map...");

        BufferedImage image = new BufferedImage(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.TYPE_INT_RGB);

        for (int pixelX = 0; pixelX < IMAGE_SIZE; pixelX++) {
            for (int pixelZ = 0; pixelZ < IMAGE_SIZE; pixelZ++) {
                int[] worldCoords = pixelToWorldCoords(pixelX, pixelZ);
                double tectonicActivity = planetModel.getTectonicActivity(worldCoords[0], worldCoords[1]);

                Color color = getTectonicColor(tectonicActivity);
                image.setRGB(pixelX, pixelZ, color.getRGB());
            }
        }

        Path outputPath = exportDir.resolve(planetName + "_tectonic_0_0_" + IMAGE_SIZE + ".png");
        ImageIO.write(image, "PNG", outputPath.toFile());
        Terradyne.LOGGER.info("  ✓ Tectonic map: {}", outputPath.getFileName());
    }

    /**
     * Export temperature map
     */
    private static void exportTemperatureMap(PlanetModel planetModel, Path exportDir, String planetName) throws IOException {
        Terradyne.LOGGER.info("Exporting temperature map...");

        BufferedImage image = new BufferedImage(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.TYPE_INT_RGB);

        // Find temperature range for color mapping
        double minTemp = Double.MAX_VALUE;
        double maxTemp = Double.MIN_VALUE;

        // First pass: find temperature range
        for (int pixelX = 0; pixelX < IMAGE_SIZE; pixelX++) {
            for (int pixelZ = 0; pixelZ < IMAGE_SIZE; pixelZ++) {
                int[] worldCoords = pixelToWorldCoords(pixelX, pixelZ);
                double temperature = planetModel.getTemperature(worldCoords[0], worldCoords[1]);
                minTemp = Math.min(minTemp, temperature);
                maxTemp = Math.max(maxTemp, temperature);
            }
        }

        // Second pass: generate image
        for (int pixelX = 0; pixelX < IMAGE_SIZE; pixelX++) {
            for (int pixelZ = 0; pixelZ < IMAGE_SIZE; pixelZ++) {
                int[] worldCoords = pixelToWorldCoords(pixelX, pixelZ);
                double temperature = planetModel.getTemperature(worldCoords[0], worldCoords[1]);

                Color color = getTemperatureColor(temperature, minTemp, maxTemp);
                image.setRGB(pixelX, pixelZ, color.getRGB());
            }
        }

        Path outputPath = exportDir.resolve(planetName + "_temperature_0_0_" + IMAGE_SIZE + ".png");
        ImageIO.write(image, "PNG", outputPath.toFile());
        Terradyne.LOGGER.info("  ✓ Temperature map: {} (range: {:.1f}°C to {:.1f}°C)", outputPath.getFileName(), minTemp, maxTemp);
    }

    /**
     * Export wind speed map
     */
    private static void exportWindSpeedMap(PlanetModel planetModel, Path exportDir, String planetName) throws IOException {
        Terradyne.LOGGER.info("Exporting wind speed map...");

        BufferedImage image = new BufferedImage(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.TYPE_INT_RGB);

        for (int pixelX = 0; pixelX < IMAGE_SIZE; pixelX++) {
            for (int pixelZ = 0; pixelZ < IMAGE_SIZE; pixelZ++) {
                int[] worldCoords = pixelToWorldCoords(pixelX, pixelZ);
                double windSpeed = planetModel.getNoiseSystem().sampleWindSpeed(worldCoords[0], worldCoords[1]);

                Color color = getWindSpeedColor(windSpeed);
                image.setRGB(pixelX, pixelZ, color.getRGB());
            }
        }

        Path outputPath = exportDir.resolve(planetName + "_windspeed_0_0_" + IMAGE_SIZE + ".png");
        ImageIO.write(image, "PNG", outputPath.toFile());
        Terradyne.LOGGER.info("  ✓ Wind speed map: {}", outputPath.getFileName());
    }

    /**
     * Export humidity map
     */
    private static void exportHumidityMap(PlanetModel planetModel, Path exportDir, String planetName) throws IOException {
        Terradyne.LOGGER.info("Exporting humidity map...");

        BufferedImage image = new BufferedImage(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.TYPE_INT_RGB);

        for (int pixelX = 0; pixelX < IMAGE_SIZE; pixelX++) {
            for (int pixelZ = 0; pixelZ < IMAGE_SIZE; pixelZ++) {
                int[] worldCoords = pixelToWorldCoords(pixelX, pixelZ);
                double humidity = planetModel.getMoisture(worldCoords[0], worldCoords[1]);

                Color color = getHumidityColor(humidity);
                image.setRGB(pixelX, pixelZ, color.getRGB());
            }
        }

        Path outputPath = exportDir.resolve(planetName + "_humidity_0_0_" + IMAGE_SIZE + ".png");
        ImageIO.write(image, "PNG", outputPath.toFile());
        Terradyne.LOGGER.info("  ✓ Humidity map: {}", outputPath.getFileName());
    }

    /**
     * NEW: Export volatility map showing tectonic plate boundaries
     */
    private static void exportVolatilityMap(PlanetModel planetModel, Path exportDir, String planetName) throws IOException {
        Terradyne.LOGGER.info("Exporting volatility map...");

        BufferedImage image = new BufferedImage(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.TYPE_INT_RGB);

        for (int pixelX = 0; pixelX < IMAGE_SIZE; pixelX++) {
            for (int pixelZ = 0; pixelZ < IMAGE_SIZE; pixelZ++) {
                int[] worldCoords = pixelToWorldCoords(pixelX, pixelZ);
                int volatility = planetModel.getVolatilityAt(worldCoords[0], worldCoords[1]);

                Color color = getVolatilityColor(volatility);
                image.setRGB(pixelX, pixelZ, color.getRGB());
            }
        }

        Path outputPath = exportDir.resolve(planetName + "_volatility_0_0_" + IMAGE_SIZE + ".png");
        ImageIO.write(image, "PNG", outputPath.toFile());
        Terradyne.LOGGER.info("  ✓ Volatility map: {} (plate boundaries and geological activity)", outputPath.getFileName());
    }

    /**
     * NEW: Export biome map showing physics-based biome classification
     */
    private static void exportBiomeMap(PlanetModel planetModel, Path exportDir, String planetName) throws IOException {
        Terradyne.LOGGER.info("Exporting biome map...");

        BufferedImage image = new BufferedImage(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.TYPE_INT_RGB);
        BiomeClassificationSystem classifier = new BiomeClassificationSystem(planetModel);

        // Track biome usage for statistics
        Map<RegistryKey<Biome>, Integer> biomeCount = new HashMap<>();

        for (int pixelX = 0; pixelX < IMAGE_SIZE; pixelX++) {
            for (int pixelZ = 0; pixelZ < IMAGE_SIZE; pixelZ++) {
                int[] worldCoords = pixelToWorldCoords(pixelX, pixelZ);

                try {
                    // Classify biome using physics system
                    RegistryKey<Biome> biomeKey = classifier.classifyBiome(worldCoords[0], worldCoords[1]);

                    // Count biome usage
                    biomeCount.put(biomeKey, biomeCount.getOrDefault(biomeKey, 0) + 1);

                    // Get appropriate color for this biome
                    Color color = getBiomeColor(biomeKey);
                    image.setRGB(pixelX, pixelZ, color.getRGB());

                } catch (Exception e) {
                    // Fallback to debug color on error
                    image.setRGB(pixelX, pixelZ, Color.MAGENTA.getRGB());
                }
            }
        }

        Path outputPath = exportDir.resolve(planetName + "_biomes_0_0_" + IMAGE_SIZE + ".png");
        ImageIO.write(image, "PNG", outputPath.toFile());

        // Log biome statistics
        Terradyne.LOGGER.info("  ✓ Biome map: {} ({} different biomes found)", outputPath.getFileName(), biomeCount.size());

        // Log top 10 most common biomes
        biomeCount.entrySet().stream()
                .sorted(Map.Entry.<RegistryKey<Biome>, Integer>comparingByValue().reversed())
                .limit(10)
                .forEach(entry -> {
                    String biomeName = entry.getKey().getValue().getPath();
                    double percentage = (entry.getValue() * 100.0) / (IMAGE_SIZE * IMAGE_SIZE);
                    Terradyne.LOGGER.info("    {} - {:.1f}%", biomeName, percentage);
                });
    }

    // === COLOR MAPPING METHODS ===

    /**
     * Generate terrain color: Blue (water) → Green (low) → Brown (mid) → White (high)
     */
    private static Color getTerrainColor(double height, double seaLevel, double minHeight, double maxHeight) {
        if (height <= seaLevel) {
            // Water: Blue shades
            double waterDepth = (seaLevel - height) / Math.max(1, seaLevel - minHeight);
            waterDepth = Math.max(0, Math.min(1, waterDepth));
            int blue = (int) (150 + waterDepth * 100); // 150-250 blue
            return new Color(0, 50, blue);
        } else {
            // Land: Green → Brown → White
            double elevation = (height - seaLevel) / Math.max(1, maxHeight - seaLevel);
            elevation = Math.max(0, Math.min(1, elevation));

            if (elevation < 0.4) {
                // Low elevation: Green
                int green = (int) (100 + elevation * 200); // Green shades
                return new Color(50, green, 50);
            } else if (elevation < 0.8) {
                // Mid elevation: Brown
                float brownFactor = (float) ((elevation - 0.4) / 0.4);
                return new Color(100 + (int) (brownFactor * 55), 80 + (int) (brownFactor * 40), 40);
            } else {
                // High elevation: White/Gray
                int gray = (int) (180 + (elevation - 0.8) * 300); // 180-240 gray to white
                gray = Math.min(255, gray);
                return new Color(gray, gray, gray);
            }
        }
    }

    /**
     * Generate tectonic color: Black (low) → Red (high)
     */
    private static Color getTectonicColor(double activity) {
        activity = Math.max(0, Math.min(1, activity));
        int red = (int) (activity * 255);
        return new Color(red, 0, 0);
    }

    /**
     * Generate temperature color: Blue (cold) → Green → Yellow → Red (hot)
     */
    private static Color getTemperatureColor(double temperature, double minTemp, double maxTemp) {
        double normalized = (temperature - minTemp) / Math.max(1, maxTemp - minTemp);
        normalized = Math.max(0, Math.min(1, normalized));

        if (normalized < 0.25) {
            // Cold: Blue
            int blue = (int) (255 - normalized * 400); // Blue to cyan
            return new Color(0, (int) (normalized * 400), blue);
        } else if (normalized < 0.5) {
            // Cool: Green
            float factor = (float) ((normalized - 0.25) / 0.25);
            return new Color(0, 255, (int) (255 * (1 - factor)));
        } else if (normalized < 0.75) {
            // Warm: Yellow
            float factor = (float) ((normalized - 0.5) / 0.25);
            return new Color((int) (255 * factor), 255, 0);
        } else {
            // Hot: Red
            float factor = (float) ((normalized - 0.75) / 0.25);
            return new Color(255, (int) (255 * (1 - factor)), 0);
        }
    }

    /**
     * Generate wind speed color: White (calm) → Blue → Purple (strong)
     */
    private static Color getWindSpeedColor(double windSpeed) {
        windSpeed = Math.max(0, Math.min(1, windSpeed));

        if (windSpeed < 0.5) {
            // Calm to moderate: White to Blue
            float factor = (float) (windSpeed / 0.5);
            int color = (int) (255 * (1 - factor));
            return new Color(color, color, 255);
        } else {
            // Strong: Blue to Purple
            float factor = (float) ((windSpeed - 0.5) / 0.5);
            int red = (int) (factor * 128);
            return new Color(red, 0, 255);
        }
    }

    /**
     * Generate humidity color: Brown (dry) → Yellow → Green → Blue (humid)
     */
    private static Color getHumidityColor(double humidity) {
        humidity = Math.max(0, Math.min(1, humidity));

        if (humidity < 0.33) {
            // Dry: Brown to Yellow
            float factor = (float) (humidity / 0.33);
            return new Color(139 + (int) (factor * 116), 69 + (int) (factor * 186), 19);
        } else if (humidity < 0.66) {
            // Moderate: Yellow to Green
            float factor = (float) ((humidity - 0.33) / 0.33);
            return new Color((int) (255 * (1 - factor)), 255, (int) (factor * 100));
        } else {
            // Humid: Green to Blue
            float factor = (float) ((humidity - 0.66) / 0.34);
            return new Color(0, (int) (255 * (1 - factor)), (int) (100 + factor * 155));
        }
    }

    /**
     * Generate volatility color: Black (stable) → Yellow → Red (active boundary)
     */
    private static Color getVolatilityColor(int volatility) {
        switch (volatility) {
            case 0: return new Color(0, 0, 0);         // Black - stable continental
            case 1: return new Color(64, 64, 64);      // Dark gray
            case 2: return new Color(128, 128, 0);     // Dark yellow
            case 3: return new Color(255, 255, 0);     // Yellow
            case 4: return new Color(255, 128, 0);     // Orange
            case 5: return new Color(255, 0, 0);       // Red - active boundary
            default: return new Color(255, 0, 255);    // Magenta - error
        }
    }

    /**
     * Generate biome color based on biome type and category
     * Uses distinctive colors for each biome category
     */
    private static Color getBiomeColor(RegistryKey<Biome> biomeKey) {
        String biomeName = biomeKey.getValue().getPath();

        // === WATER BIOMES (Blues) ===
        if (biomeName.contains("ocean")) {
            if (biomeName.contains("frozen")) return new Color(135, 206, 250);      // Light sky blue
            if (biomeName.contains("frigid")) return new Color(70, 130, 180);       // Steel blue
            if (biomeName.contains("dead")) return new Color(47, 79, 79);           // Dark slate gray
            if (biomeName.contains("warm")) return new Color(0, 191, 255);          // Deep sky blue
            if (biomeName.contains("coral")) return new Color(255, 127, 80);        // Coral
            if (biomeName.contains("tropical")) return new Color(64, 224, 208);     // Turquoise
            if (biomeName.contains("boiling")) return new Color(255, 69, 0);        // Red orange
            return new Color(0, 100, 200); // Default ocean blue
        }

        // === MOUNTAIN BIOMES (Grays/Browns) ===
        if (biomeName.contains("peak") || biomeName.contains("mountain") || biomeName.contains("volcanic")) {
            if (biomeName.contains("frozen")) return new Color(248, 248, 255);      // Ghost white
            if (biomeName.contains("foothills")) return new Color(139, 137, 137);   // Dim gray
            if (biomeName.contains("alpine")) return new Color(192, 192, 192);      // Silver
            if (biomeName.contains("volcanic_wasteland")) return new Color(105, 105, 105); // Dim gray
            if (biomeName.contains("volcanic")) return new Color(160, 82, 45);      // Saddle brown
            return new Color(128, 128, 128); // Default mountain gray
        }

        // === HIGHLAND BIOMES (Green-Browns) ===
        if (biomeName.contains("highland") || biomeName.contains("hills")) {
            if (biomeName.contains("barren")) return new Color(160, 82, 45);        // Saddle brown
            if (biomeName.contains("windswept")) return new Color(189, 183, 107);   // Dark khaki
            if (biomeName.contains("rolling")) return new Color(107, 142, 35);      // Olive drab
            if (biomeName.contains("tundra")) return new Color(176, 196, 222);      // Light steel blue
            if (biomeName.contains("forested")) return new Color(34, 139, 34);      // Forest green
            if (biomeName.contains("tropical")) return new Color(50, 205, 50);      // Lime green
            return new Color(154, 205, 50); // Default highland yellow green
        }

        // === HOSTILE CONTINENTAL BIOMES (Reds/Oranges) ===
        if (biomeName.contains("wasteland") || biomeName.contains("scorched") || biomeName.contains("dust")) {
            if (biomeName.contains("frozen")) return new Color(230, 230, 250);      // Lavender
            if (biomeName.contains("rocky")) return new Color(205, 133, 63);        // Peru
            if (biomeName.contains("scorched")) return new Color(255, 69, 0);       // Orange red
            if (biomeName.contains("sandy")) return new Color(238, 203, 173);       // Navajo white
            if (biomeName.contains("mesa")) return new Color(222, 184, 135);        // Burlywood
            if (biomeName.contains("dust")) return new Color(188, 143, 143);        // Rosy brown
            return new Color(220, 20, 60); // Default hostile crimson
        }

        // === DESERT BIOMES (Yellows/Tans) ===
        if (biomeName.contains("desert")) {
            if (biomeName.contains("hot")) return new Color(255, 140, 0);           // Dark orange
            return new Color(238, 203, 173); // Navajo white
        }

        // === MARGINAL CONTINENTAL BIOMES (Yellows/Light Browns) ===
        if (biomeName.contains("steppes") || biomeName.contains("savanna") || biomeName.contains("meadows")) {
            if (biomeName.contains("cold")) return new Color(175, 238, 238);        // Pale turquoise
            if (biomeName.contains("dry")) return new Color(240, 230, 140);         // Khaki
            if (biomeName.contains("temperate")) return new Color(173, 255, 47);    // Green yellow
            if (biomeName.contains("meadows")) return new Color(152, 251, 152);     // Pale green
            if (biomeName.contains("savanna")) return new Color(255, 228, 181);     // Moccasin
            if (biomeName.contains("tropical_grassland")) return new Color(255, 215, 0); // Gold
            return new Color(189, 183, 107); // Default marginal dark khaki
        }

        // === THRIVING CONTINENTAL BIOMES (Greens) ===
        // Cold Zone
        if (biomeName.contains("snowy") || biomeName.contains("taiga") || biomeName.contains("snow")) {
            if (biomeName.contains("snowy_plains")) return new Color(255, 250, 250); // Snow
            if (biomeName.contains("taiga")) return new Color(25, 25, 112);          // Midnight blue
            if (biomeName.contains("snow_forest")) return new Color(72, 61, 139);    // Dark slate blue
            if (biomeName.contains("alpine_meadows")) return new Color(173, 216, 230); // Light blue
            return new Color(176, 196, 222); // Default cold light steel blue
        }

        // Temperate Zone
        if (biomeName.contains("plains") || biomeName.contains("forest") || biomeName.contains("wetlands")) {
            if (biomeName.contains("plains") && !biomeName.contains("mixed")) return new Color(124, 252, 0); // Lawn green
            if (biomeName.contains("mixed_plains")) return new Color(173, 255, 47);  // Green yellow
            if (biomeName.contains("wetlands")) return new Color(46, 139, 87);       // Sea green
            if (biomeName.contains("oak")) return new Color(34, 139, 34);            // Forest green
            if (biomeName.contains("mixed_forest")) return new Color(0, 100, 0);     // Dark green
            if (biomeName.contains("dense_forest")) return new Color(0, 128, 0);     // Green
            if (biomeName.contains("mountain_forest")) return new Color(85, 107, 47); // Dark olive green
            return new Color(0, 128, 0); // Default temperate green
        }

        // Warm Zone
        if (biomeName.contains("shrubland") || biomeName.contains("rainforest") || biomeName.contains("jungle")) {
            if (biomeName.contains("hot_shrubland")) return new Color(255, 165, 0);  // Orange
            if (biomeName.contains("windy_steppes")) return new Color(218, 165, 32); // Goldenrod
            if (biomeName.contains("temperate_rainforest")) return new Color(0, 100, 0); // Dark green
            if (biomeName.contains("cloud_forest")) return new Color(102, 205, 170); // Medium aquamarine
            if (biomeName.contains("jungle")) return new Color(0, 128, 0);           // Green
            if (biomeName.contains("tropical_rainforest")) return new Color(34, 139, 34); // Forest green
            return new Color(50, 205, 50); // Default warm lime green
        }

        // Hot Zone
        if (biomeName.contains("swamp")) {
            return new Color(107, 142, 35); // Olive drab
        }

        // === SPECIAL BIOMES ===
        if (biomeName.contains("extreme_frozen")) return new Color(248, 248, 255);  // Ghost white
        if (biomeName.contains("molten")) return new Color(139, 0, 0);              // Dark red
        if (biomeName.contains("debug")) return new Color(255, 0, 255);             // Magenta

        // === FALLBACK ===
        return new Color(128, 128, 128); // Gray for unknown biomes
    }

    // === UTILITY METHODS ===

    /**
     * Convert pixel coordinates to world coordinates (chunk center)
     */
    private static int[] pixelToWorldCoords(int pixelX, int pixelZ) {
        // Convert pixel to chunk coordinates (centered around origin)
        int chunkX = pixelX - CHUNKS_RADIUS;
        int chunkZ = pixelZ - CHUNKS_RADIUS;

        // Convert chunk to world coordinates (chunk center)
        int worldX = chunkX * 16 + 8;
        int worldZ = chunkZ * 16 + 8;

        return new int[]{worldX, worldZ};
    }

    /**
     * Load planet model from server context
     */
    private static PlanetModel loadPlanetModel(MinecraftServer server, String planetName) {
        try {
            // Normalize planet name and create dimension identifier
            String normalizedName = planetName.toLowerCase().replace(" ", "_");
            net.minecraft.util.Identifier dimensionId = new net.minecraft.util.Identifier("terradyne", normalizedName);
            net.minecraft.registry.RegistryKey<net.minecraft.world.World> worldKey =
                    net.minecraft.registry.RegistryKey.of(net.minecraft.registry.RegistryKeys.WORLD, dimensionId);

            // Get the world for this planet
            net.minecraft.server.world.ServerWorld world = server.getWorld(worldKey);
            if (world == null) {
                Terradyne.LOGGER.error("World not found for planet: {}", planetName);
                return null;
            }

            // Get the chunk generator from the world
            net.minecraft.world.gen.chunk.ChunkGenerator chunkGenerator = world.getChunkManager().getChunkGenerator();

            // Check if it's our Universal Chunk Generator
            if (!(chunkGenerator instanceof UniversalChunkGenerator)) {
                Terradyne.LOGGER.error("World '{}' does not use UniversalChunkGenerator", planetName);
                return null;
            }

            // Cast and get the planet model
            UniversalChunkGenerator universalGenerator =
                    (UniversalChunkGenerator) chunkGenerator;

            PlanetModel planetModel = universalGenerator.getPlanetModel();
            if (planetModel == null) {
                Terradyne.LOGGER.error("No planet model found in chunk generator for: {}", planetName);
                return null;
            }

            return planetModel;

        } catch (Exception e) {
            Terradyne.LOGGER.error("Failed to load planet model for '{}': {}", planetName, e.getMessage());
            return null;
        }
    }

    /**
     * Get export directory path
     */
    private static Path getExportDirectory(MinecraftServer server) {
        Path worldDir = server.getSavePath(net.minecraft.util.WorldSavePath.ROOT);
        return worldDir.resolve("terradyne").resolve("exports");
    }
}