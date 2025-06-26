package net.starlight.terradyne.planet.terrain;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.starlight.terradyne.planet.physics.BlockPaletteManager;
import net.starlight.terradyne.planet.physics.PlanetData;
import net.starlight.terradyne.planet.physics.PlanetModel;

/**
 * Translates mathematical noise values into Minecraft blocks and heights
 * Updated for 0-256 height range (pre-1.18 style)
 */
public class TerrainHeightMapper {

    private final PlanetModel planetModel;
    private final PlanetData planetData;
    private final BlockPaletteManager.BlockPalette blockPalette;
    private final PlanetaryNoiseSystem noiseSystem;

    // Updated Minecraft height constraints for 0-256 range
    private static final int MIN_WORLD_Y = 0;
    private static final int MAX_WORLD_Y = 255;
    private static final int WORLD_HEIGHT = 256;

    /**
     * Create terrain height mapper from planet model
     */
    public TerrainHeightMapper(PlanetModel planetModel, PlanetaryNoiseSystem noiseSystem) {
        this.planetModel = planetModel;
        this.planetData = planetModel.getPlanetData();
        this.blockPalette = planetModel.getBlockPalette();
        this.noiseSystem = noiseSystem;
    }

    /**
     * Convert noise-based terrain height to Minecraft Y coordinate (0-255 range)
     */
    public int getMinecraftHeight(double noiseHeight) {
        // Clamp to new height limits
        int height = (int) Math.round(noiseHeight);
        return Math.max(MIN_WORLD_Y, Math.min(MAX_WORLD_Y, height));
    }

    /**
     * Get appropriate block state for given terrain conditions
     */
    public BlockState getTerrainBlockState(int worldX, int worldZ, int minecraftY) {
        // Sample terrain and environmental conditions
        double terrainHeight = noiseSystem.sampleTerrainHeight(worldX, worldZ);
        double tectonicActivity = noiseSystem.sampleTectonicActivity(worldX, worldZ);
        double temperature = noiseSystem.sampleTemperature(worldX, worldZ);
        double moisture = noiseSystem.sampleMoisture(worldX, worldZ);

        // Calculate terrain factors for block selection
        double elevation = calculateElevationFactor(minecraftY, terrainHeight);
        double erosion = calculateErosionFactor(tectonicActivity, temperature, moisture);
        double habitability = calculateHabitabilityFactor(temperature, moisture, elevation);

        // Use block palette to select appropriate block
        Block selectedBlock = blockPalette.getBlockForConditions(elevation, erosion, habitability);

        // Apply special conditions
        return applySpecialConditions(selectedBlock, worldX, worldZ, minecraftY,
                terrainHeight, temperature, moisture);
    }

    /**
     * Generate a complete terrain column for chunk generation (0-256 range)
     */
    public TerrainColumn generateTerrainColumn(int worldX, int worldZ) {
        double terrainHeight = noiseSystem.sampleTerrainHeight(worldX, worldZ);
        int surfaceY = getMinecraftHeight(terrainHeight);

        TerrainColumn column = new TerrainColumn(worldX, worldZ, surfaceY);

        // Fill column from bottom to surface
        for (int y = MIN_WORLD_Y; y <= MAX_WORLD_Y; y++) {
            if (y <= surfaceY) {
                BlockState blockState = getTerrainBlockState(worldX, worldZ, y);
                column.setBlock(y, blockState);
            } else if (y <= planetData.getSeaLevel() && planetData.hasLiquidWater()) {
                // Fill with water up to sea level
                column.setBlock(y, Blocks.WATER.getDefaultState());
            } else {
                // Air above surface/water
                column.setBlock(y, Blocks.AIR.getDefaultState());
            }
        }

        return column;
    }

    // === TERRAIN FACTOR CALCULATIONS ===

    /**
     * Calculate elevation factor (0.0 = sea level, 1.0 = max height)
     * Updated for 0-256 height range
     */
    private double calculateElevationFactor(int minecraftY, double terrainHeight) {
        double seaLevel = Math.max(MIN_WORLD_Y, Math.min(MAX_WORLD_Y, planetData.getSeaLevel()));
        double maxHeight = seaLevel + (planetData.getMountainScale() * 60.0);
        
        // Clamp max height to our world limits
        maxHeight = Math.min(MAX_WORLD_Y, maxHeight);

        if (minecraftY <= seaLevel) {
            return 0.0;
        }

        return Math.max(0.0, Math.min(1.0, (minecraftY - seaLevel) / (maxHeight - seaLevel)));
    }

    /**
     * Calculate erosion factor based on environmental conditions
     */
    private double calculateErosionFactor(double tectonicActivity, double temperature, double moisture) {
        double waterErosion = planetData.getWaterErosion() * moisture;
        double windErosion = planetData.getWindErosion() * (1.0 - moisture);
        double thermalErosion = Math.abs(temperature - 15.0) / 50.0; // Thermal stress

        // Low tectonic activity = more time for erosion
        double timeErosion = (1.0 - tectonicActivity) * 0.3;

        return Math.max(0.0, Math.min(1.0, waterErosion + windErosion + thermalErosion + timeErosion));
    }

    /**
     * Calculate habitability factor for organic material formation
     */
    private double calculateHabitabilityFactor(double temperature, double moisture, double elevation) {
        double tempFactor = 0.0;
        if (temperature > 0 && temperature < 30) {
            tempFactor = 1.0 - Math.abs(temperature - 15.0) / 15.0; // Optimal at 15°C
        }

        double moistureFactor = Math.max(0.0, Math.min(1.0, moisture));
        double elevationFactor = Math.max(0.0, 1.0 - elevation); // Less habitable at high elevation

        return (tempFactor * moistureFactor * elevationFactor + planetData.getHabitability()) * 0.5;
    }

    /**
     * Apply special environmental conditions to block selection
     * Updated for 0-256 height range
     */
    private BlockState applySpecialConditions(Block baseBlock, int worldX, int worldZ, int minecraftY,
                                              double terrainHeight, double temperature, double moisture) {

        // === TEMPERATURE-BASED MODIFICATIONS ===

        // Ice formation in cold conditions
        if (temperature < -5 && moisture > 0.3 && minecraftY > planetData.getSeaLevel()) {
            if (baseBlock == Blocks.WATER) {
                return Blocks.ICE.getDefaultState();
            }
            if (minecraftY > terrainHeight - 1 && planetData.getGlacialCoverage() > 0.5) {
                return Blocks.SNOW_BLOCK.getDefaultState();
            }
        }

        // Volcanic activity effects
        if (noiseSystem.sampleTectonicActivity(worldX, worldZ) > 0.8 &&
                planetData.getVolcanismLevel() > 0.6) {
            if (baseBlock == blockPalette.upperRock && Math.random() < 0.1) {
                return Blocks.MAGMA_BLOCK.getDefaultState();
            }
        }

        // === DEPTH-BASED MODIFICATIONS (updated for 0-256 range) ===

        double depthBelowSurface = terrainHeight - minecraftY;

        // Deep underground -> deepslate layer (adjusted for smaller height range)
        if (depthBelowSurface > 30) {
            return Blocks.DEEPSLATE.getDefaultState();
        }

        // Very deep -> actual bedrock (much closer to surface in 0-256 range)
        if (depthBelowSurface > 50 || minecraftY < 5) {
            return Blocks.BEDROCK.getDefaultState();
        }

        return baseBlock.getDefaultState();
    }

    // === UTILITY CLASSES ===

    /**
     * Container for a complete vertical column of terrain blocks (0-256 range)
     */
    public static class TerrainColumn {
        private final int worldX;
        private final int worldZ;
        private final int surfaceY;
        private final BlockState[] blocks;

        public TerrainColumn(int worldX, int worldZ, int surfaceY) {
            this.worldX = worldX;
            this.worldZ = worldZ;
            this.surfaceY = surfaceY;
            this.blocks = new BlockState[WORLD_HEIGHT];

            // Initialize with air
            for (int i = 0; i < blocks.length; i++) {
                blocks[i] = Blocks.AIR.getDefaultState();
            }
        }

        public void setBlock(int minecraftY, BlockState blockState) {
            int arrayIndex = minecraftY - MIN_WORLD_Y;
            if (arrayIndex >= 0 && arrayIndex < blocks.length) {
                blocks[arrayIndex] = blockState;
            }
        }

        public BlockState getBlock(int minecraftY) {
            int arrayIndex = minecraftY - MIN_WORLD_Y;
            if (arrayIndex >= 0 && arrayIndex < blocks.length) {
                return blocks[arrayIndex];
            }
            return Blocks.AIR.getDefaultState();
        }

        public int getWorldX() { return worldX; }
        public int getWorldZ() { return worldZ; }
        public int getSurfaceY() { return surfaceY; }
        public BlockState[] getAllBlocks() { return blocks.clone(); }
    }

    // === DIAGNOSTICS ===

    /**
     * Get terrain analysis at specific coordinates for debugging
     */
    public String analyzeTerrainAt(int worldX, int worldZ) {
        double terrainHeight = noiseSystem.sampleTerrainHeight(worldX, worldZ);
        double tectonicActivity = noiseSystem.sampleTectonicActivity(worldX, worldZ);
        double temperature = noiseSystem.sampleTemperature(worldX, worldZ);
        double moisture = noiseSystem.sampleMoisture(worldX, worldZ);

        int minecraftY = getMinecraftHeight(terrainHeight);
        double elevation = calculateElevationFactor(minecraftY, terrainHeight);
        double erosion = calculateErosionFactor(tectonicActivity, temperature, moisture);
        double habitability = calculateHabitabilityFactor(temperature, moisture, elevation);

        Block selectedBlock = blockPalette.getBlockForConditions(elevation, erosion, habitability);

        return String.format("TerrainAnalysis{x=%d,z=%d: height=%.1f(Y=%d), tectonic=%.2f, " +
                        "temp=%.1f°C, moisture=%.2f, elevation=%.2f, erosion=%.2f, " +
                        "habitability=%.2f, block=%s} [Height Range: %d-%d]",
                worldX, worldZ, terrainHeight, minecraftY, tectonicActivity,
                temperature, moisture, elevation, erosion, habitability,
                selectedBlock.getTranslationKey(), MIN_WORLD_Y, MAX_WORLD_Y);
    }
}