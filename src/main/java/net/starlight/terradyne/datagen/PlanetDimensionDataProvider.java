package net.starlight.terradyne.datagen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;
import net.minecraft.util.Identifier;
import net.starlight.terradyne.datagen.HardcodedPlanets;
import net.starlight.terradyne.planet.physics.PlanetConfig;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Generates planet dimension JSON files from hardcoded planet definitions
 * These will be packaged with the mod and always available
 */
public class PlanetDimensionDataProvider implements DataProvider {

    private final FabricDataOutput output;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public PlanetDimensionDataProvider(FabricDataOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(DataWriter writer) {
        // FIXED: Make synchronous to avoid concurrency issues
        try {
            generatePlanetDimensions(writer);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Generate dimension JSON files for all hardcoded planets
     * FIXED: Better error handling and logging
     */
    private void generatePlanetDimensions(DataWriter writer) {
        System.out.println("=== STARTING PLANET DIMENSION GENERATION ===");

        var planets = HardcodedPlanets.getAllPlanets();
        System.out.println("Found " + planets.size() + " hardcoded planets: " + planets.keySet());

        if (planets.isEmpty()) {
            System.err.println("ERROR: No planets found in HardcodedPlanets.getAllPlanets()!");
            return;
        }

        int successCount = 0;
        int totalCount = planets.size();

        for (var entry : planets.entrySet()) {
            String planetKey = entry.getKey();
            PlanetConfig config = entry.getValue();

            System.out.println("Processing planet: " + planetKey + " (display name: " + config.getPlanetName() + ")");

            try {
                // Create dimension JSON - use the map key as planet_name for consistency
                JsonObject dimensionJson = createDimensionJson(planetKey, config);
                System.out.println("Created JSON for " + planetKey + ": " + dimensionJson.toString());

                // Create identifier for the dimension file using the map key
                Identifier dimensionId = new Identifier("terradyne", planetKey);
                System.out.println("Creating dimension ID: " + dimensionId);

                // Get the path for dimension files
                Path dimensionPath = output.getResolver(net.minecraft.data.DataOutput.OutputType.DATA_PACK, "dimension")
                        .resolveJson(dimensionId);
                System.out.println("Resolved path: " + dimensionPath);

                // Write the JSON file
                DataProvider.writeToPath(writer, dimensionJson, dimensionPath);

                successCount++;
                System.out.println("✅ Successfully generated dimension file: " + planetKey + ".json (" + successCount + "/" + totalCount + ")");

            } catch (Exception e) {
                System.err.println("❌ Failed to generate dimension for planet: " + planetKey);
                e.printStackTrace();
                // Continue with next planet instead of stopping
            }
        }

        System.out.println("=== PLANET DIMENSION GENERATION COMPLETE ===");
        System.out.println("Successfully generated " + successCount + " out of " + totalCount + " planets");

        if (successCount != totalCount) {
            System.err.println("WARNING: Not all planets were generated successfully!");
        }
    }

    /**
     * Create dimension JSON structure using our registered chunk generator
     * FIXED: Use planetKey instead of config name for consistency
     */
    private JsonObject createDimensionJson(String planetKey, PlanetConfig config) {
        try {
            JsonObject dimension = new JsonObject();

            // Use overworld dimension type (can be customized later with your dimension types)
            dimension.addProperty("type", "minecraft:overworld");

            // Create generator section with our registered generator
            JsonObject generator = new JsonObject();
            generator.addProperty("type", "terradyne:universal");
            // Use the planetKey (e.g., "proximacentaurib") instead of config.getPlanetName()
            generator.addProperty("planet_name", planetKey);

            // Biome source - fixed plains for now
            JsonObject biomeSource = new JsonObject();
            biomeSource.addProperty("type", "minecraft:fixed");
            biomeSource.addProperty("biome", "minecraft:plains");
            generator.add("biome_source", biomeSource);

            dimension.add("generator", generator);

            return dimension;
        } catch (Exception e) {
            System.err.println("Error creating dimension JSON for " + planetKey + ": " + e.getMessage());
            throw e;
        }
    }

    @Override
    public String getName() {
        return "Terradyne Planet Dimensions";
    }
}