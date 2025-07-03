package net.starlight.terradyne.planet.chunk;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.noise.NoiseConfig;

import net.starlight.terradyne.Terradyne;
import net.starlight.terradyne.datagen.HardcodedPlanets;
import net.starlight.terradyne.planet.physics.PlanetConfig;
import net.starlight.terradyne.planet.physics.PlanetModel;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Universal Chunk Generator with Physics-Based Generation
 * UPDATED: Now gets PlanetModel from server context via WorldPlanetManager
 */
public class UniversalChunkGenerator extends ChunkGenerator {

    // Updated height constraints for all Terradyne dimensions
    private static final int MIN_WORLD_Y = 0;
    private static final int MAX_WORLD_Y = 255;
    private static final int WORLD_HEIGHT = 256;

    public static final Codec<UniversalChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource),
                    Codec.STRING.optionalFieldOf("planet_name", "").forGetter(generator -> generator.planetName)
            ).apply(instance, UniversalChunkGenerator::fromCodec)
    );

    private String planetName;
    private PlanetModel planetModel; // Loaded lazily from server context


    /**
     * Constructor for direct creation (used by WorldPlanetManager)
     */
    public UniversalChunkGenerator(PlanetModel planetModel, BiomeSource biomeSource) {
        super(biomeSource);
        this.planetModel = planetModel;
        this.planetName = planetModel != null ? planetModel.getConfig().getPlanetName() : "";

        if (planetModel != null) {
            Terradyne.LOGGER.info("=== UNIVERSAL CHUNK GENERATOR INITIALIZED (DIRECT) ===");
            Terradyne.LOGGER.info("Planet: {}", planetModel.getConfig().getPlanetName());
            Terradyne.LOGGER.info("Classification: {}", planetModel.getPlanetClassification());
            Terradyne.LOGGER.info("Generation: PHYSICS-BASED");
            Terradyne.LOGGER.info("Height Range: Y {} to {}", MIN_WORLD_Y, MAX_WORLD_Y);
            Terradyne.LOGGER.info("Noise System: {}", planetModel.getNoiseSystem().getSystemStatus());
        } else {
            Terradyne.LOGGER.warn("UniversalChunkGenerator created without planet model");
        }
    }


    /**
     * Constructor for codec deserialization - IMPROVED to use hardcoded planets first
     * Falls back to file-based loading for user customization
     */
    private static UniversalChunkGenerator fromCodec(BiomeSource biomeSource, String planetName) {
        Terradyne.LOGGER.info("Creating UniversalChunkGenerator for planet: {}", planetName);

        try {
            PlanetConfig planetConfig = null;

            // Step 1: Try hardcoded planets first
            if (HardcodedPlanets.isHardcodedPlanet(planetName)) {
                planetConfig = HardcodedPlanets.getPlanet(planetName);
                Terradyne.LOGGER.info("✅ Using hardcoded planet definition: {}", planetName);
            }

            // Step 2: Fall back to file-based loading for user customization
            if (planetConfig == null) {
                planetConfig = loadPlanetConfigFromWorld(planetName);
                if (planetConfig != null) {
                    Terradyne.LOGGER.info("✅ Using user-defined planet config: {}", planetName);
                }
            }

            // Step 3: Create planet model if we found a config
            if (planetConfig != null) {
                PlanetModel planetModel = new PlanetModel(planetConfig);

                Terradyne.LOGGER.info("✅ Successfully created planet model: {} ({})",
                        planetName, planetModel.getPlanetClassification());
                return new UniversalChunkGenerator(planetModel, biomeSource);

            } else {
                Terradyne.LOGGER.error("❌ No planet definition found for: {}", planetName);
                Terradyne.LOGGER.error("Available hardcoded planets: {}", HardcodedPlanets.getAllPlanetNames());

                // Create generator without model - will use fallback terrain
                UniversalChunkGenerator generator = new UniversalChunkGenerator(null, biomeSource);
                generator.planetName = planetName;
                return generator;
            }

        } catch (Exception e) {
            Terradyne.LOGGER.error("❌ Failed to create chunk generator for '{}': {}", planetName, e.getMessage());

            // Fallback generator
            UniversalChunkGenerator generator = new UniversalChunkGenerator(null, biomeSource);
            generator.planetName = planetName;
            return generator;
        }
    }

    /**
     * Load planet config directly from world directory
     * Works on both client and server, eliminates timing issues
     */
    private static PlanetConfig loadPlanetConfigFromWorld(String planetName) {
        try {
            // Get the current world directory - this is tricky in codec context
            // We need to find the world save directory
            Path worldDir = getCurrentWorldDirectory();
            if (worldDir == null) {
                Terradyne.LOGGER.warn("Could not determine world directory for planet config loading");
                return null;
            }

            Path planetsDir = worldDir.resolve("terradyne").resolve("planets");

            if (!Files.exists(planetsDir)) {
                Terradyne.LOGGER.warn("Planets config directory not found: {}", planetsDir);
                return null;
            }

            // Normalize planet name for file lookup
            String normalizedName = planetName.toLowerCase().replace(" ", "_");

            // Try to find the config file
            Path configFile = planetsDir.resolve(normalizedName + ".json");
            if (!Files.exists(configFile)) {
                // Try with original name
                configFile = planetsDir.resolve(planetName + ".json");
                if (!Files.exists(configFile)) {
                    Terradyne.LOGGER.warn("Planet config file not found: {} or {}",
                            normalizedName + ".json", planetName + ".json");
                    return null;
                }
            }

            // Load the specific config file
            String jsonContent = Files.readString(configFile);

            // Parse JSON manually (simplified version of PlanetConfigLoader logic)
            PlanetConfig config = parseConfigFromJson(jsonContent, planetName);

            if (config != null) {
                Terradyne.LOGGER.debug("Successfully loaded planet config from: {}", configFile);
            }

            return config;

        } catch (Exception e) {
            Terradyne.LOGGER.error("Failed to load planet config for '{}': {}", planetName, e.getMessage());
            return null;
        }
    }

    /**
     * Get current world directory - works in codec context
     */
    private static Path getCurrentWorldDirectory() {
        try {
            // Try to get world directory from system properties or working directory
            // This is a common approach for Minecraft mods

            // Method 1: Check if we're running in a server context
            String serverDir = System.getProperty("user.dir");
            if (serverDir != null) {
                Path serverPath = Path.of(serverDir);

                // Look for world directories
                Path[] possiblePaths = {
                        serverPath.resolve("world"),  // Default server world
                        serverPath.resolve("saves").resolve("New World"), // Common dev world name
                        serverPath  // Current directory might be the world
                };

                for (Path path : possiblePaths) {
                    if (Files.exists(path.resolve("level.dat"))) {
                        Terradyne.LOGGER.debug("Found world directory: {}", path);
                        return path;
                    }
                }
            }

            // Method 2: Try to find any world with terradyne configs
            Path userDir = Path.of(System.getProperty("user.dir"));
            try (var stream = Files.walk(userDir, 3)) { // Search up to 3 levels deep
                var worldDirs = stream
                        .filter(Files::isDirectory)
                        .filter(path -> Files.exists(path.resolve("level.dat")))
                        .filter(path -> Files.exists(path.resolve("terradyne").resolve("planets")))
                        .findFirst();

                if (worldDirs.isPresent()) {
                    Terradyne.LOGGER.debug("Found world with terradyne configs: {}", worldDirs.get());
                    return worldDirs.get();
                }
            } catch (Exception e) {
                // Continue to fallback
            }

            Terradyne.LOGGER.warn("Could not auto-detect world directory");
            return null;

        } catch (Exception e) {
            Terradyne.LOGGER.error("Error detecting world directory: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Simple JSON parsing for planet config (simplified version)
     */
    private static PlanetConfig parseConfigFromJson(String jsonContent, String planetName) {
        try {
            // Use Gson to parse the JSON
            com.google.gson.Gson gson = new com.google.gson.Gson();

            // Parse to map first for easier handling
            @SuppressWarnings("unchecked")
            Map<String, Object> jsonMap = gson.fromJson(jsonContent, Map.class);

            // Extract basic values with defaults
            String name = getStringValue(jsonMap, "name", planetName);
            long seed = name.hashCode(); // Generate seed from name

            // Create config with Earth-like defaults
            PlanetConfig config = new PlanetConfig(name, seed);

            // Apply values with safe parsing
            if (jsonMap.containsKey("circumference")) {
                config.setCircumference(getIntValue(jsonMap, "circumference", 40000));
            }
            if (jsonMap.containsKey("distanceFromStar")) {
                config.setDistanceFromStar(getLongValue(jsonMap, "distanceFromStar", 150));
            }
            if (jsonMap.containsKey("tectonicActivity")) {
                config.setTectonicActivity(getDoubleValue(jsonMap, "tectonicActivity", 0.6));
            }
            if (jsonMap.containsKey("waterContent")) {
                config.setWaterContent(getDoubleValue(jsonMap, "waterContent", 0.7));
            }

            // Parse enums safely
            if (jsonMap.containsKey("crustComposition")) {
                try {
                    String crustStr = getStringValue(jsonMap, "crustComposition", "SILICATE");
                    config.setCrustComposition(net.starlight.terradyne.planet.physics.CrustComposition.valueOf(crustStr.toUpperCase()));
                } catch (Exception e) {
                    Terradyne.LOGGER.warn("Invalid crust composition, using default: {}", e.getMessage());
                }
            }

            if (jsonMap.containsKey("atmosphereComposition")) {
                try {
                    String atmoStr = getStringValue(jsonMap, "atmosphereComposition", "OXYGEN_RICH");
                    config.setAtmosphereComposition(net.starlight.terradyne.planet.physics.AtmosphereComposition.valueOf(atmoStr.toUpperCase()));
                } catch (Exception e) {
                    Terradyne.LOGGER.warn("Invalid atmosphere composition, using default: {}", e.getMessage());
                }
            }

            Terradyne.LOGGER.debug("Parsed planet config: {}", config);
            return config;

        } catch (Exception e) {
            Terradyne.LOGGER.error("Failed to parse planet config JSON: {}", e.getMessage());
            return null;
        }
    }

    // Helper methods for safe JSON value extraction
    private static String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value instanceof String ? (String) value : defaultValue;
    }

    private static int getIntValue(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    private static long getLongValue(Map<String, Object> map, String key, long defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return defaultValue;
    }

    private static double getDoubleValue(Map<String, Object> map, String key, double defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }

    @Override
    protected Codec<? extends ChunkGenerator> getCodec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<Chunk> populateNoise(Executor executor, Blender blender,
                                                  NoiseConfig noiseConfig, StructureAccessor structureAccessor,
                                                  Chunk chunk) {
        return CompletableFuture.supplyAsync(() -> {
            generateTerrain(chunk, noiseConfig);
            return chunk;
        }, executor);
    }

    /**
     * Main terrain generation method - uses physics system (PERFORMANCE FIXED)
     * NOW SAMPLES CLIMATE ONCE PER CHUNK FOR PERFORMANCE
     */
    private void generateTerrain(Chunk chunk, NoiseConfig noiseConfig) {
        // No more lazy loading - planet model should be loaded from codec
        if (planetModel == null) {
            Terradyne.LOGGER.warn("No planet model available for chunk {} - using fallback terrain", chunk.getPos());
            generateFallbackTerrain(chunk);
            return;
        }

        try {
            ChunkPos chunkPos = chunk.getPos();

            Terradyne.LOGGER.debug("Generating physics-based terrain for chunk {} on planet {}",
                    chunkPos, planetModel.getConfig().getPlanetName());

            // === PERFORMANCE FIX: Sample climate ONCE per chunk at chunk center ===
            int chunkCenterX = chunkPos.getStartX() + 8;
            int chunkCenterZ = chunkPos.getStartZ() + 8;

            double chunkTemperature = planetModel.getTemperature(chunkCenterX, chunkCenterZ);
            double chunkMoisture = planetModel.getMoisture(chunkCenterX, chunkCenterZ);
            double chunkWindSpeed = planetModel.getNoiseSystem().sampleWindSpeed(chunkCenterX, chunkCenterZ);

            Terradyne.LOGGER.debug("Chunk climate: temp={:.1f}°C, moisture={:.2f}, wind={:.2f}",
                    chunkTemperature, chunkMoisture, chunkWindSpeed);

            // Generate terrain using physics system with cached climate data
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    int worldX = chunkPos.getStartX() + x;
                    int worldZ = chunkPos.getStartZ() + z;

                    // Generate complete terrain column using physics - adapted for 0-256 range
                    generateTerrainColumn(chunk, x, z, worldX, worldZ, planetModel,
                            chunkTemperature, chunkMoisture, chunkWindSpeed);
                }
            }

            Terradyne.LOGGER.debug("✅ Physics-based generation completed for chunk {}", chunkPos);

        } catch (Exception e) {
            Terradyne.LOGGER.error("Critical error in physics-based terrain generation for chunk {}: {}",
                    chunk.getPos(), e.getMessage(), e);
            generateFallbackTerrain(chunk);
        }
    }

    /**
     * Generate terrain column adapted for 0-256 height range
     * PERFORMANCE FIX: Now accepts cached climate data instead of sampling per column
     */
    private void generateTerrainColumn(Chunk chunk, int x, int z, int worldX, int worldZ,
                                       PlanetModel planetModel, double temperature, double moisture, double windSpeed) {
        // Sample terrain height (this is still needed per column for height variation)
        double terrainHeight = planetModel.getTerrainHeight(worldX, worldZ);

        // Use cached climate data instead of sampling again
        // double temperature = planetModel.getTemperature(worldX, worldZ);  // REMOVED - now cached!
        // double moisture = planetModel.getMoisture(worldX, worldZ);        // REMOVED - now cached!

        // Clamp terrain height to our 0-256 range
        int surfaceY = Math.max(MIN_WORLD_Y, Math.min(MAX_WORLD_Y, (int) terrainHeight));

        // Adjust sea level for 0-256 range
        int seaLevel = Math.max(MIN_WORLD_Y, Math.min(MAX_WORLD_Y, planetModel.getPlanetData().getSeaLevel()));

        // Generate column from bottom to top
        for (int y = MIN_WORLD_Y; y <= MAX_WORLD_Y; y++) {
            BlockPos pos = new BlockPos(x, y, z);

            if (y <= surfaceY) {
                // Use physics system with cached climate data to determine block type
                BlockState blockState = planetModel.getTerrainBlockState(worldX, worldZ, y, temperature, moisture);
                chunk.setBlockState(pos, blockState, false);
            } else if (y <= seaLevel && planetModel.getPlanetData().hasLiquidWater()) {
                // Fill with water up to sea level
                chunk.setBlockState(pos, Blocks.WATER.getDefaultState(), false);
            } else {
                // Air above surface/water
                chunk.setBlockState(pos, Blocks.AIR.getDefaultState(), false);
            }
        }
    }

// UPDATE these methods to not try lazy loading:

    @Override
    public int getSeaLevel() {
        if (planetModel != null) {
            return Math.max(MIN_WORLD_Y, Math.min(MAX_WORLD_Y, planetModel.getPlanetData().getSeaLevel()));
        }
        return 63; // Minecraft default fallback
    }

    @Override
    public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
        if (planetModel == null) {
            return 120; // Fallback height for 0-256 range
        }

        double terrainHeight = planetModel.getTerrainHeight(x, z);
        return Math.max(MIN_WORLD_Y, Math.min(MAX_WORLD_Y, (int) terrainHeight));
    }

    @Override
    public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
        if (planetModel == null) {
            // Fallback column for 0-256 range
            BlockState[] column = new BlockState[WORLD_HEIGHT];
            for (int y = 0; y < WORLD_HEIGHT; y++) {
                int worldY = MIN_WORLD_Y + y;
                if (worldY <= 120) {
                    column[y] = Blocks.STONE.getDefaultState();
                } else {
                    column[y] = Blocks.AIR.getDefaultState();
                }
            }
            return new VerticalBlockSample(MIN_WORLD_Y, column);
        }

        // Generate physics-based column for 0-256 range
        BlockState[] column = new BlockState[WORLD_HEIGHT];
        double terrainHeight = planetModel.getTerrainHeight(x, z);
        int surfaceY = Math.max(MIN_WORLD_Y, Math.min(MAX_WORLD_Y, (int) terrainHeight));
        int seaLevel = Math.max(MIN_WORLD_Y, Math.min(MAX_WORLD_Y, planetModel.getPlanetData().getSeaLevel()));

        for (int y = 0; y < WORLD_HEIGHT; y++) {
            int worldY = MIN_WORLD_Y + y;

            if (worldY <= surfaceY) {
                column[y] = planetModel.getTerrainBlockState(x, z, worldY);
            } else if (worldY <= seaLevel && planetModel.getPlanetData().hasLiquidWater()) {
                column[y] = Blocks.WATER.getDefaultState();
            } else {
                column[y] = Blocks.AIR.getDefaultState();
            }
        }

        return new VerticalBlockSample(MIN_WORLD_Y, column);
    }

    // UPDATE debug method:
    @Override
    public void getDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {
        text.add("=== TERRADYNE PHYSICS-BASED GENERATION ===");
        text.add("Height Range: Y " + MIN_WORLD_Y + " to " + MAX_WORLD_Y);

        if (planetModel != null) {
            text.add("Planet: " + planetModel.getConfig().getPlanetName());
            text.add("Classification: " + planetModel.getPlanetClassification());
            text.add("Physics Status: " + (planetModel.isValid() ? "ACTIVE" : "ERROR"));

            text.add("");
            text.add("=== TERRAIN ANALYSIS ===");
            try {
                text.add(planetModel.analyzeTerrainAt(pos.getX(), pos.getZ()));
            } catch (Exception e) {
                text.add("Error analyzing terrain: " + e.getMessage());
            }

            // Add more debug info...

        } else {
            text.add("No planet model loaded");
            text.add("Planet Name: " + planetName);
            text.add("Status: FALLBACK GENERATION");
        }
    }

    /**
     * Fallback terrain generation if physics system fails or is unavailable
     */
    private void generateFallbackTerrain(Chunk chunk) {
        Terradyne.LOGGER.warn("Using fallback terrain generation for chunk {} (planet model unavailable)",
                chunk.getPos());

        // Simple flat terrain as emergency fallback - adapted for 0-256 range
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = MIN_WORLD_Y; y <= 150; y++) { // Up to Y=150 for fallback
                    BlockPos pos = new BlockPos(x, y, z);

                    if (y <= 50) {
                        chunk.setBlockState(pos, Blocks.DEEPSLATE.getDefaultState(), false);
                    } else if (y <= 120) {
                        chunk.setBlockState(pos, Blocks.STONE.getDefaultState(), false);
                    } else if (y <= 150) {
                        chunk.setBlockState(pos, Blocks.DIRT.getDefaultState(), false);
                    }
                }
            }
        }
    }

    // ============================================================================
    // REQUIRED CHUNK GENERATOR METHODS - Updated for 0-256 height
    // ============================================================================

    @Override
    public void carve(ChunkRegion chunkRegion, long seed, NoiseConfig noiseConfig, BiomeAccess biomeAccess,
                      StructureAccessor structureAccessor, Chunk chunk, GenerationStep.Carver carverStep) {
        // Carving disabled - physics system handles erosion through noise
        // TODO: Implement cave/cavern generation in future phases
    }

    @Override
    public void buildSurface(ChunkRegion region, StructureAccessor structures, NoiseConfig noiseConfig, Chunk chunk) {
        // Surface building handled by physics system in terrain generation
        // The physics system already places appropriate surface blocks
    }

    @Override
    public void populateEntities(ChunkRegion region) {
        // Default entity population - no custom entity spawning yet
    }

    @Override
    public int getWorldHeight() {
        return WORLD_HEIGHT; // 256 blocks tall
    }

    @Override
    public int getMinimumY() {
        return MIN_WORLD_Y; // Y=0
    }

    // === ACCESSORS ===
    /**
     * Get the planet name
     */
    public String getPlanetName() {
        return planetName;
    }

    /**
     * Check if physics-based generation is available
     */
    public boolean hasPhysicsGeneration() {
        return planetModel != null && planetModel.isValid();
    }

    /**
     * Get the planet model for external access (map exports, etc.)
     */
    public PlanetModel getPlanetModel() {
        return planetModel;
    }
}