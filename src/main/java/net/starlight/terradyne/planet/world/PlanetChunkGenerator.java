// PlanetChunkGenerator.java
package net.starlight.terradyne.planet.world;

import com.mojang.serialization.Codec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
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

import net.starlight.terradyne.planet.physics.PlanetModel;
import net.starlight.terradyne.planet.physics.PlanetData;
import net.starlight.terradyne.planet.terrain.PlanetaryNoiseSystem;
import net.starlight.terradyne.planet.terrain.TerrainHeightMapper;
import net.starlight.terradyne.planet.terrain.config.MasterTerrainConfig;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Custom chunk generator for planetary terrain.
 * Integrates our unified noise system with Minecraft's chunk generation pipeline.
 */
public class PlanetChunkGenerator extends ChunkGenerator {

    private final PlanetModel planetModel;
    private final PlanetaryNoiseSystem noiseSystem;
    private final TerrainHeightMapper heightMapper;
    private final MasterTerrainConfig terrainConfig;
    private final PlanetData planetData;

    // Cached block states for performance
    private final BlockState airBlock;
    private final BlockState bedrockBlock;

    public PlanetChunkGenerator(PlanetModel planetModel, BiomeSource biomeSource) {
        super(biomeSource);

        this.planetModel = planetModel;
        this.planetData = planetModel.getPlanetData();
        this.terrainConfig = planetModel.getTerrainConfig();
        this.noiseSystem = planetModel.getNoiseSystem();
        this.heightMapper = new TerrainHeightMapper(terrainConfig);

        // Cache commonly used block states
        this.airBlock = Blocks.AIR.getDefaultState();
        this.bedrockBlock = Blocks.BEDROCK.getDefaultState();

        System.out.println("Initialized PlanetChunkGenerator for " + planetData.getPlanetName());
        System.out.println("Height range: " + terrainConfig.getHeightSettings().minHeight +
                " to " + terrainConfig.getHeightSettings().maxHeight);
    }

    @Override
    protected Codec<? extends ChunkGenerator> getCodec() {
        // TODO: This will need proper codec implementation for save/load
        // For now, returning a placeholder - this is needed for Minecraft's serialization
        return null;
    }

    @Override
    public void carve(ChunkRegion chunkRegion, long seed, NoiseConfig noiseConfig,
                      BiomeAccess biomeAccess, StructureAccessor structureAccessor,
                      Chunk chunk, GenerationStep.Carver carverStep) {
        // We don't want any carvers (caves, ravines) for now
        // Leave empty to skip all carving
    }

    @Override
    public void buildSurface(ChunkRegion region, StructureAccessor structures,
                             NoiseConfig noiseConfig, Chunk chunk) {
        // This is where we actually place our blocks!
        ChunkPos chunkPos = chunk.getPos();
        int chunkX = chunkPos.x;
        int chunkZ = chunkPos.z;

        // Process each column in the chunk (16x16)
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                // Convert chunk-local coordinates to world coordinates
                int worldX = (chunkX << 4) + localX; // chunkX * 16 + localX
                int worldZ = (chunkZ << 4) + localZ; // chunkZ * 16 + localZ

                generateTerrainColumn(chunk, worldX, worldZ);
            }
        }
    }

    /**
     * Generate terrain for a single column (x, z coordinate)
     */
    private void generateTerrainColumn(Chunk chunk, int worldX, int worldZ) {
        // Sample our unified noise system
        double noiseValue = noiseSystem.sampleTerrain(worldX, worldZ);

        // Calculate erosion factor for this location
        double erosionFactor = calculateErosionFactor(worldX, worldZ);

        // Generate complete terrain column using our height mapper
        TerrainHeightMapper.TerrainColumn terrainColumn =
                heightMapper.generateTerrainColumn(noiseValue, erosionFactor);

        // Place blocks in the chunk
        placeTerrainColumn(chunk, worldX, worldZ, terrainColumn);
    }

    /**
     * Calculate erosion factor for given coordinates
     */
    private double calculateErosionFactor(int worldX, int worldZ) {
        // Simple erosion calculation - can be enhanced later
        // Use a different noise pattern for erosion variation
        double erosionNoise = Math.sin(worldX * 0.01) * Math.cos(worldZ * 0.01);

        // Combine with planet's erosion factors
        double baseErosion = (planetData.getWaterErosion() + planetData.getWindErosion()) * 0.5;

        // Add some spatial variation
        return Math.max(0.0, Math.min(1.0, baseErosion + (erosionNoise * 0.2)));
    }

    /**
     * Place terrain column blocks into the chunk
     */
    private void placeTerrainColumn(Chunk chunk, int worldX, int worldZ,
                                    TerrainHeightMapper.TerrainColumn terrainColumn) {
        int chunkLocalX = worldX & 15; // worldX % 16
        int chunkLocalZ = worldZ & 15; // worldZ % 16

        int minHeight = terrainConfig.getHeightSettings().minHeight;
        int maxHeight = terrainConfig.getHeightSettings().maxHeight;
        int seaLevel = terrainConfig.getHeightSettings().seaLevel;

        // Place all blocks in the column
        for (int y = minHeight; y <= maxHeight; y++) {
            BlockPos pos = new BlockPos(chunkLocalX, y, chunkLocalZ);
            String blockId = terrainColumn.getBlock(y);

            if (!blockId.equals("minecraft:air")) {
                BlockState blockState = getBlockStateFromId(blockId);
                chunk.setBlockState(pos, blockState, false);
            }
        }

        // Handle fluid placement above terrain surface if underwater
        if (terrainColumn.isUnderwater()) {
            for (int y = terrainColumn.getSurfaceHeight() + 1; y <= seaLevel; y++) {
                BlockPos pos = new BlockPos(chunkLocalX, y, chunkLocalZ);
                String fluidId = terrainColumn.getBlock(y);

                if (!fluidId.equals("minecraft:air")) {
                    BlockState fluidState = getBlockStateFromId(fluidId);
                    chunk.setBlockState(pos, fluidState, false);
                }
            }
        }
    }

    /**
     * Convert block identifier string to BlockState
     */
    private BlockState getBlockStateFromId(String blockId) {
        try {
            Identifier identifier = new Identifier(blockId);
            Block block = Registries.BLOCK.get(identifier);
            return block.getDefaultState();
        } catch (Exception e) {
            System.err.println("Failed to get block for ID: " + blockId + ", using stone as fallback");
            return Blocks.STONE.getDefaultState();
        }
    }

    @Override
    public int getHeight(int x, int z, Heightmap.Type heightmapType, HeightLimitView world, NoiseConfig noiseConfig) {
        // Sample noise and convert to height
        double noiseValue = noiseSystem.sampleTerrain(x, z);
        return heightMapper.noiseToHeight(noiseValue);
    }

    @Override
    public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
        int minY = world.getBottomY();
        int height = world.getHeight();
        BlockState[] blocks = new BlockState[height];

        // Sample noise for this column
        double noiseValue = noiseSystem.sampleTerrain(x, z);
        double erosionFactor = calculateErosionFactor(x, z);

        // Generate terrain column
        TerrainHeightMapper.TerrainColumn terrainColumn =
                heightMapper.generateTerrainColumn(noiseValue, erosionFactor);

        // Fill block array
        for (int i = 0; i < height; i++) {
            int y = minY + i;
            String blockId = terrainColumn.getBlock(y);

            if (blockId.equals("minecraft:air")) {
                blocks[i] = airBlock;
            } else {
                blocks[i] = getBlockStateFromId(blockId);
            }
        }

        return new VerticalBlockSample(minY, blocks);
    }

    @Override
    public int getSpawnHeight(HeightLimitView world) {
        // Return a reasonable spawn height - sea level + 10
        return terrainConfig.getHeightSettings().seaLevel + 10;
    }

    @Override
    public void populateEntities(ChunkRegion region) {
        // No special entity population for now
        // We'll handle mob spawning through biomes later
    }

    @Override
    public void getDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {
        // Add our planet information to the F3 debug screen
        text.add("Planet: " + planetData.getPlanetName());
        text.add("Crust: " + planetData.getCrustComposition());
        text.add("Atmosphere: " + planetData.getAtmosphereComposition());
        text.add("Temperature: " + String.format("%.1f°C", planetData.getAverageSurfaceTemp()));
        text.add("Habitability: " + String.format("%.2f", planetData.getHabitability()));
        text.add("Sea Level: " + getSeaLevel());

        // Add terrain-specific debug info
        double noiseValue = noiseSystem.sampleTerrain(pos.getX(), pos.getZ());
        text.add("Noise: " + String.format("%.3f", noiseValue));

        int height = heightMapper.noiseToHeight(noiseValue);
        text.add("Terrain Height: " + height);

        TerrainHeightMapper.HeightInfo heightInfo = heightMapper.getHeightInfo(noiseValue);
        text.add("Geological Layer: " + heightInfo.layer);
        text.add("Surface Block: " + heightInfo.surfaceBlock);
    }

    @Override
    public CompletableFuture<Chunk> populateNoise(Executor executor, Blender blender,
                                                  NoiseConfig noiseConfig, StructureAccessor structureAccessor,
                                                  Chunk chunk) {
        // This is where the main noise generation happens
        // For our system, we do most work in buildSurface, so this can be minimal
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public int getSeaLevel() {
        return terrainConfig.getHeightSettings().seaLevel;
    }

    @Override
    public int getMinimumY() {
        return terrainConfig.getHeightSettings().minHeight;
    }

    @Override
    public int getWorldHeight() {
        return terrainConfig.getHeightSettings().getHeightRange();
    }

    /**
     * Get debug information about terrain generation at specific coordinates
     */
    public String getDebugInfo(int x, int z) {
        double noiseValue = noiseSystem.sampleTerrain(x, z);
        double erosionFactor = calculateErosionFactor(x, z);
        TerrainHeightMapper.HeightInfo heightInfo = heightMapper.getHeightInfo(noiseValue);

        StringBuilder sb = new StringBuilder();
        sb.append("=== TERRAIN DEBUG INFO ===\n");
        sb.append("Planet: ").append(planetData.getPlanetName()).append("\n");
        sb.append("Coordinates: (").append(x).append(", ").append(z).append(")\n");
        sb.append("Noise Value: ").append(String.format("%.3f", noiseValue)).append("\n");
        sb.append("Erosion Factor: ").append(String.format("%.3f", erosionFactor)).append("\n");
        sb.append("Height Info: ").append(heightInfo.toString()).append("\n");
        sb.append("Sea Level: ").append(getSeaLevel()).append("\n");

        return sb.toString();
    }

    /**
     * Get the planet model this generator is using
     */
    public PlanetModel getPlanetModel() {
        return planetModel;
    }

    /**
     * Test if noise system is working correctly
     */
    public boolean testNoiseSystem() {
        try {
            // Sample a few test coordinates
            double test1 = noiseSystem.sampleTerrain(0, 0);
            double test2 = noiseSystem.sampleTerrain(100, 100);
            double test3 = noiseSystem.sampleTerrain(-100, -100);

            // Check that we get reasonable values
            boolean valid = (test1 >= -1.0 && test1 <= 1.0) &&
                    (test2 >= -1.0 && test2 <= 1.0) &&
                    (test3 >= -1.0 && test3 <= 1.0);

            if (valid) {
                System.out.println("✓ Noise system test passed");
                System.out.println("  Sample values: " + test1 + ", " + test2 + ", " + test3);
            } else {
                System.err.println("✗ Noise system test failed - values out of range");
            }

            return valid;
        } catch (Exception e) {
            System.err.println("✗ Noise system test failed with exception: " + e.getMessage());
            return false;
        }
    }
}