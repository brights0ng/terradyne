package net.starlight.terradyne.datagen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;
import net.minecraft.util.Identifier;
import net.starlight.terradyne.starsystem.CelestialObjectRegistry;
import net.starlight.terradyne.starsystem.StarSystemModel;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Generates planet dimension JSON files from loaded celestial objects
 * UPDATED: Now uses datapack-loaded celestial objects instead of hardcoded planets
 */
public class PlanetDimensionDataProvider implements DataProvider {

    private final FabricDataOutput output;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public PlanetDimensionDataProvider(FabricDataOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(DataWriter writer) {
        return generatePlanetDimensions(writer);
    }

    /**
     * Generate dimension JSON files for all loaded celestial objects
     * Uses datapack-loaded celestial objects from CelestialObjectRegistry
     * 
     * FIXED: Now properly collects and returns all CompletableFutures to ensure
     * all files are written before data generation completes
     */
    private CompletableFuture<?> generatePlanetDimensions(DataWriter writer) {
        System.out.println("=== STARTING PLANET DIMENSION GENERATION (DATAPACK) ===");

        var celestialObjects = CelestialObjectRegistry.getAll();
        System.out.println("Found " + celestialObjects.size() + " celestial objects from datapacks");

        if (celestialObjects.isEmpty()) {
            System.err.println("ERROR: No celestial objects found in registry!");
            return CompletableFuture.completedFuture(null);
        }

        int totalCount = celestialObjects.size();

        // Collect all futures so we wait for ALL files to be written
        var futures = new java.util.ArrayList<CompletableFuture<?>>();

        for (var entry : celestialObjects.entrySet()) {
            Identifier objectId = entry.getKey();
            CelestialObjectRegistry.CelestialObjectEntry objectEntry = entry.getValue();

            System.out.println("Processing celestial object: " + objectId +
                    " (type: " + objectEntry.type + ", name: " + objectEntry.name + ")");

            try {
                // Create dimension JSON based on object type
                JsonObject dimensionJson = createDimensionJson(objectId, objectEntry);
                System.out.println("Created JSON for " + objectId + " with type " + objectEntry.type);

                // Get the path for dimension files
                // Dimensions go in data/[namespace]/dimension/[name].json
                Path dimensionPath = output.getPath()
                        .resolve("data")
                        .resolve(objectId.getNamespace())
                        .resolve("dimension")
                        .resolve(objectId.getPath() + ".json");

                // Write the JSON file and collect the future
                CompletableFuture<?> writeFuture = DataProvider.writeToPath(writer, dimensionJson, dimensionPath);
                futures.add(writeFuture);

                System.out.println("✅ Queued dimension file for writing: " + objectId + ".json");

            } catch (Exception e) {
                System.err.println("❌ Failed to generate dimension for object: " + objectId);
                e.printStackTrace();
                futures.add(CompletableFuture.failedFuture(e));
            }
        }

        System.out.println("=== PLANET DIMENSION GENERATION QUEUED ===");
        System.out.println("Waiting for " + totalCount + " dimensions to be written to disk...");

        // Wait for ALL futures to complete before returning
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> {
                    System.out.println("=== PLANET DIMENSION GENERATION COMPLETE ===");
                    System.out.println("Successfully wrote " + totalCount + " dimensions to disk");
                });
    }

    /**
     * Create dimension JSON structure with EMBEDDED PlanetConfig
     * No external registries needed - everything is self-contained!
     */
    private JsonObject createDimensionJson(Identifier objectId,
                                           CelestialObjectRegistry.CelestialObjectEntry objectEntry) {
        try {
            JsonObject dimension = new JsonObject();

            // Use overworld dimension type (can be customized later per object type)
            dimension.addProperty("type", "minecraft:overworld");

            // Create generator section based on object type
            JsonObject generator = new JsonObject();

            switch (objectEntry.type) {
                case TERRESTRIAL -> {
                    // Rocky planets/moons - use universal chunk generator with physics-based biomes
                    generator.addProperty("type", "terradyne:universal");
                    
                    // EMBED THE FULL PLANET CONFIG!
                    JsonObject planetConfigJson = createPlanetConfigJson(objectEntry);
                    generator.add("planet_config", planetConfigJson);

                    // Biome source (no config needed, chunk generator will set planet model)
                    JsonObject biomeSource = new JsonObject();
                    biomeSource.addProperty("type", "terradyne:physics_based");
                    generator.add("biome_source", biomeSource);
                }
                case SOLAR -> {
                    // Stars - use lava chunk generator (TODO: implement custom generator)
                    // For now, use universal generator with solar config
                    generator.addProperty("type", "terradyne:universal");
                    generator.addProperty("is_solar", true);
                    
                    JsonObject planetConfigJson = createPlanetConfigJson(objectEntry);
                    generator.add("planet_config", planetConfigJson);

                    // Use fixed basalt delta biome for now
                    JsonObject biomeSource = new JsonObject();
                    biomeSource.addProperty("type", "minecraft:fixed");
                    biomeSource.addProperty("biome", "minecraft:basalt_deltas");
                    generator.add("biome_source", biomeSource);
                }
                case GASEOUS -> {
                    // Gas giants - skybox only (TODO: implement custom generator)
                    generator.addProperty("type", "terradyne:universal");
                    generator.addProperty("is_gaseous", true);
                    
                    JsonObject planetConfigJson = createPlanetConfigJson(objectEntry);
                    generator.add("planet_config", planetConfigJson);

                    // Use void biome for now
                    JsonObject biomeSource = new JsonObject();
                    biomeSource.addProperty("type", "minecraft:fixed");
                    biomeSource.addProperty("biome", "minecraft:the_void");
                    generator.add("biome_source", biomeSource);
                }
            }

            dimension.add("generator", generator);

            return dimension;

        } catch (Exception e) {
            System.err.println("Error creating dimension JSON for " + objectId + ": " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Convert PlanetConfig to JSON object for embedding
     */
    private JsonObject createPlanetConfigJson(CelestialObjectRegistry.CelestialObjectEntry objectEntry) {
        var config = objectEntry.planetConfig;
        
        JsonObject json = new JsonObject();
        json.addProperty("planet_name", config.getPlanetName());
        json.addProperty("seed", config.getSeed());
        json.addProperty("circumference", config.getCircumference());
        json.addProperty("distance_from_star", config.getDistanceFromStar());
        json.addProperty("crust_composition", config.getCrustComposition().name());
        json.addProperty("atmosphere_composition", config.getAtmosphereComposition().name());
        json.addProperty("tectonic_activity", config.getTectonicActivity());
        json.addProperty("water_content", config.getWaterContent());
        json.addProperty("crustal_thickness", config.getCrustalThickness());
        json.addProperty("atmospheric_density", config.getAtmosphericDensity());
        json.addProperty("rotation_period", config.getRotationPeriod());
        json.addProperty("noise_scale", config.getNoiseScale());
        
        return json;
    }

    @Override
    public String getName() {
        return "Terradyne Planet Dimensions (Datapack)";
    }
}