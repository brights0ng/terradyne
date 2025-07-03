package net.starlight.terradyne.planet.mapping;

import net.minecraft.server.MinecraftServer;
import net.starlight.terradyne.Terradyne;
import net.starlight.terradyne.planet.physics.PlanetModel;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Exports climate and terrain maps as PNG images for visualization
 * Each pixel represents one chunk (16x16 blocks) sampled at chunk center
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
            if (!(chunkGenerator instanceof net.starlight.terradyne.planet.chunk.UniversalChunkGenerator)) {
                Terradyne.LOGGER.error("World '{}' does not use UniversalChunkGenerator", planetName);
                return null;
            }

            // Cast and get the planet model
            net.starlight.terradyne.planet.chunk.UniversalChunkGenerator universalGenerator =
                    (net.starlight.terradyne.planet.chunk.UniversalChunkGenerator) chunkGenerator;

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