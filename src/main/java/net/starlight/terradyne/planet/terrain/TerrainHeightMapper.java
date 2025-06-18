// TerrainHeightMapper.java
package net.starlight.terradyne.planet.terrain;

import net.starlight.terradyne.planet.physics.PlanetData;
import net.starlight.terradyne.planet.terrain.config.MasterTerrainConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * Converts unified noise values to actual Minecraft world heights and block types.
 * Handles the translation from mathematical terrain generation to physical world representation,
 * including geological layering, erosion effects, and planet-specific rock compositions.
 */
public class TerrainHeightMapper {
    
    private final MasterTerrainConfig terrainConfig;
    private final PlanetData planetData;
    private final BlockPalette blockPalette;
    
    public TerrainHeightMapper(MasterTerrainConfig terrainConfig) {
        this.terrainConfig = terrainConfig;
        this.planetData = terrainConfig.getPlanetData();
        this.blockPalette = new BlockPalette(planetData);
        
        System.out.println("Initialized TerrainHeightMapper for " + planetData.getPlanetName());
        System.out.println("Height range: " + terrainConfig.getHeightSettings().minHeight + 
                          " to " + terrainConfig.getHeightSettings().maxHeight);
        System.out.println("Sea level: " + terrainConfig.getHeightSettings().seaLevel);
        System.out.println("Main rock type: " + planetData.getMainRockType());
    }
    
    /**
     * Convert noise value to world height
     */
    public int noiseToHeight(double noiseValue) {
        return terrainConfig.getHeightSettings().noiseToWorldHeight(noiseValue);
    }
    
    /**
     * Get appropriate block type for given height and environmental conditions
     */
    public String getBlockAtHeight(int height, double erosionFactor, boolean isUnderwater) {
        // Determine geological layer based on height
        GeologicalLayer layer = determineGeologicalLayer(height);
        
        // Get base block from palette
        String baseBlock = blockPalette.getBlockForLayer(layer);
        
        // Apply environmental modifications
        baseBlock = applyEnvironmentalEffects(baseBlock, layer, erosionFactor, isUnderwater);
        
        return baseBlock;
    }
    
    /**
     * Get complete terrain profile for a column from bedrock to surface
     */
    public TerrainColumn generateTerrainColumn(double noiseValue, double erosionFactor) {
        int surfaceHeight = noiseToHeight(noiseValue);
        boolean isUnderwater = terrainConfig.getHeightSettings().isBelowSeaLevel(surfaceHeight);
        
        TerrainColumn column = new TerrainColumn(surfaceHeight, isUnderwater);
        
        // Fill column from bottom to top
        for (int y = terrainConfig.getHeightSettings().minHeight; y <= surfaceHeight; y++) {
            String blockType = getBlockAtHeight(y, erosionFactor, isUnderwater && y >= surfaceHeight);
            column.setBlock(y, blockType);
        }
        
        // Handle underwater areas
        if (isUnderwater) {
            int seaLevel = terrainConfig.getHeightSettings().seaLevel;
            for (int y = surfaceHeight + 1; y <= seaLevel; y++) {
                // Determine water type based on planet conditions
                String fluidBlock = getFluidBlock();
                column.setBlock(y, fluidBlock);
            }
        }
        
        return column;
    }
    
    /**
     * Determine geological layer based on height
     */
    private GeologicalLayer determineGeologicalLayer(int height) {
        MasterTerrainConfig.TerrainHeightSettings heightSettings = terrainConfig.getHeightSettings();
        
        int totalRange = heightSettings.getHeightRange();
        int relativeHeight = height - heightSettings.minHeight;
        double heightRatio = (double) relativeHeight / totalRange;
        
        // Define layer boundaries (as ratios of total height)
        if (heightRatio < 0.1) {
            return GeologicalLayer.BEDROCK;
        } else if (heightRatio < 0.3) {
            return GeologicalLayer.DEEP_ROCK;
        } else if (heightRatio < 0.7) {
            return GeologicalLayer.MIDDLE_ROCK;
        } else if (heightRatio < 0.9) {
            return GeologicalLayer.UPPER_ROCK;
        } else {
            return GeologicalLayer.SURFACE;
        }
    }
    
    /**
     * Apply environmental effects to block selection
     */
    private String applyEnvironmentalEffects(String baseBlock, GeologicalLayer layer, 
                                           double erosionFactor, boolean isUnderwater) {
        // Water erosion effects
        if (planetData.getWaterErosion() > 0.3 && erosionFactor > 0.5) {
            baseBlock = applyWaterErosion(baseBlock, layer);
        }
        
        // Wind erosion effects
        if (planetData.getWindErosion() > 0.4 && layer == GeologicalLayer.SURFACE) {
            baseBlock = applyWindErosion(baseBlock);
        }
        
        // Age effects
        if (planetData.getPlanetAge() == PlanetData.PlanetAge.OLD || 
            planetData.getPlanetAge() == PlanetData.PlanetAge.DEAD) {
            baseBlock = applyAgingEffects(baseBlock, layer);
        }
        
        // Underwater modifications
        if (isUnderwater) {
            baseBlock = applyUnderwaterEffects(baseBlock, layer);
        }
        
        return baseBlock;
    }
    
    /**
     * Apply water erosion effects
     */
    private String applyWaterErosion(String baseBlock, GeologicalLayer layer) {
        // Water erosion tends to expose different rock types and create sediments
        switch (layer) {
            case SURFACE:
                // Surface becomes more sedimentary
                if (baseBlock.contains("stone")) {
                    return "minecraft:gravel";
                } else if (baseBlock.contains("basalt")) {
                    return "minecraft:cobblestone";
                }
                break;
            case UPPER_ROCK:
                // Upper rock becomes more weathered
                if (baseBlock.contains("iron")) {
                    return "minecraft:cobblestone";
                }
                break;
        }
        return baseBlock;
    }
    
    /**
     * Apply wind erosion effects
     */
    private String applyWindErosion(String baseBlock) {
        // Wind erosion creates sand and dust
        if (baseBlock.contains("gravel")) {
            return "minecraft:sand";
        } else if (baseBlock.contains("stone")) {
            return "minecraft:coarse_dirt";
        }
        return baseBlock;
    }
    
    /**
     * Apply aging effects to blocks
     */
    private String applyAgingEffects(String baseBlock, GeologicalLayer layer) {
        // Old planets have more weathered surfaces
        if (layer == GeologicalLayer.SURFACE) {
            if (baseBlock.contains("magma")) {
                return "minecraft:basalt"; // Cooled magma
            } else if (baseBlock.contains("iron_block")) {
                return "minecraft:iron_ore"; // Oxidized metal
            }
        }
        return baseBlock;
    }
    
    /**
     * Apply underwater effects
     */
    private String applyUnderwaterEffects(String baseBlock, GeologicalLayer layer) {
        // Underwater areas get different treatments
        if (layer == GeologicalLayer.SURFACE) {
            // Underwater surfaces become sedimentary
            return "minecraft:clay";
        }
        return baseBlock;
    }
    
    /**
     * Determine fluid block type based on planet conditions
     */
    private String getFluidBlock() {
        // Check planet temperature and composition for fluid type
        double temperature = planetData.getAverageSurfaceTemp();
        
        if (temperature < -50) {
            return "minecraft:ice"; // Frozen worlds
        } else if (temperature > 100) {
            return "minecraft:air"; // Steam/vapor (no liquid)
        } else if (planetData.getAtmosphereComposition() == PlanetData.AtmosphereComposition.METHANE) {
            return "minecraft:water"; // Methane lakes (represented as water)
        } else {
            return "minecraft:water"; // Standard water
        }
    }
    
    /**
     * Get height variation info for debugging
     */
    public HeightInfo getHeightInfo(double noiseValue) {
        int height = noiseToHeight(noiseValue);
        boolean underwater = terrainConfig.getHeightSettings().isBelowSeaLevel(height);
        GeologicalLayer layer = determineGeologicalLayer(height);
        String surfaceBlock = getBlockAtHeight(height, 0.5, underwater);
        
        return new HeightInfo(height, underwater, layer, surfaceBlock, noiseValue);
    }
    
    /**
     * Geological layer enumeration
     */
    public enum GeologicalLayer {
        BEDROCK,      // Bottom layer - unbreakable foundation
        DEEP_ROCK,    // Deep primary rock layer
        MIDDLE_ROCK,  // Middle transition layer
        UPPER_ROCK,   // Upper transition layer  
        SURFACE       // Surface layer - most varied
    }
    
    /**
     * Block palette manager for different planet types
     */
    private static class BlockPalette {
        private final Map<GeologicalLayer, String> layerBlocks;
        private final PlanetData planetData;
        
        public BlockPalette(PlanetData planetData) {
            this.planetData = planetData;
            this.layerBlocks = new HashMap<>();
            initializePalette();
        }
        
        /**
         * Initialize block palette based on planet crust composition
         */
        private void initializePalette() {
            String mainRock = planetData.getMainRockType();
            
            // Always use bedrock at the bottom
            layerBlocks.put(GeologicalLayer.BEDROCK, "minecraft:bedrock");
            
            // Configure layers based on crust composition
            switch (planetData.getCrustComposition()) {
                case SILICATE:
                    layerBlocks.put(GeologicalLayer.DEEP_ROCK, "minecraft:deepslate");
                    layerBlocks.put(GeologicalLayer.MIDDLE_ROCK, "minecraft:stone");
                    layerBlocks.put(GeologicalLayer.UPPER_ROCK, "minecraft:stone");
                    layerBlocks.put(GeologicalLayer.SURFACE, "minecraft:stone");
                    break;
                    
                case BASALT:
                    layerBlocks.put(GeologicalLayer.DEEP_ROCK, "minecraft:blackstone");
                    layerBlocks.put(GeologicalLayer.MIDDLE_ROCK, "minecraft:basalt");
                    layerBlocks.put(GeologicalLayer.UPPER_ROCK, "minecraft:basalt");
                    layerBlocks.put(GeologicalLayer.SURFACE, "minecraft:basalt");
                    break;
                    
                case FERROUS:
                    layerBlocks.put(GeologicalLayer.DEEP_ROCK, "minecraft:deepslate");
                    layerBlocks.put(GeologicalLayer.MIDDLE_ROCK, "minecraft:iron_ore");
                    layerBlocks.put(GeologicalLayer.UPPER_ROCK, "minecraft:iron_ore");
                    layerBlocks.put(GeologicalLayer.SURFACE, "minecraft:iron_ore");
                    break;
                    
                case HADEAN:
                    layerBlocks.put(GeologicalLayer.DEEP_ROCK, "minecraft:magma_block");
                    layerBlocks.put(GeologicalLayer.MIDDLE_ROCK, "minecraft:magma_block");
                    layerBlocks.put(GeologicalLayer.UPPER_ROCK, "minecraft:netherrack");
                    layerBlocks.put(GeologicalLayer.SURFACE, "minecraft:magma_block");
                    break;
                    
                case REGOLITH:
                    layerBlocks.put(GeologicalLayer.DEEP_ROCK, "minecraft:stone");
                    layerBlocks.put(GeologicalLayer.MIDDLE_ROCK, "minecraft:cobblestone");
                    layerBlocks.put(GeologicalLayer.UPPER_ROCK, "minecraft:gravel");
                    layerBlocks.put(GeologicalLayer.SURFACE, "minecraft:gravel");
                    break;
                    
                case CARBON:
                    layerBlocks.put(GeologicalLayer.DEEP_ROCK, "minecraft:deepslate");
                    layerBlocks.put(GeologicalLayer.MIDDLE_ROCK, "minecraft:coal_ore");
                    layerBlocks.put(GeologicalLayer.UPPER_ROCK, "minecraft:coal_block");
                    layerBlocks.put(GeologicalLayer.SURFACE, "minecraft:coal_block");
                    break;
                    
                case METAL:
                    layerBlocks.put(GeologicalLayer.DEEP_ROCK, "minecraft:deepslate");
                    layerBlocks.put(GeologicalLayer.MIDDLE_ROCK, "minecraft:iron_block");
                    layerBlocks.put(GeologicalLayer.UPPER_ROCK, "minecraft:iron_block");
                    layerBlocks.put(GeologicalLayer.SURFACE, "minecraft:iron_block");
                    break;
                    
                case SULFUR:
                    layerBlocks.put(GeologicalLayer.DEEP_ROCK, "minecraft:stone");
                    layerBlocks.put(GeologicalLayer.MIDDLE_ROCK, "minecraft:yellow_terracotta");
                    layerBlocks.put(GeologicalLayer.UPPER_ROCK, "minecraft:yellow_terracotta");
                    layerBlocks.put(GeologicalLayer.SURFACE, "minecraft:yellow_terracotta");
                    break;
                    
                case HALIDE:
                    layerBlocks.put(GeologicalLayer.DEEP_ROCK, "minecraft:stone");
                    layerBlocks.put(GeologicalLayer.MIDDLE_ROCK, "minecraft:white_terracotta");
                    layerBlocks.put(GeologicalLayer.UPPER_ROCK, "minecraft:white_terracotta");
                    layerBlocks.put(GeologicalLayer.SURFACE, "minecraft:white_terracotta");
                    break;
                    
                default:
                    // Fallback to stone
                    layerBlocks.put(GeologicalLayer.DEEP_ROCK, "minecraft:deepslate");
                    layerBlocks.put(GeologicalLayer.MIDDLE_ROCK, "minecraft:stone");
                    layerBlocks.put(GeologicalLayer.UPPER_ROCK, "minecraft:stone");
                    layerBlocks.put(GeologicalLayer.SURFACE, "minecraft:stone");
                    break;
            }
        }
        
        public String getBlockForLayer(GeologicalLayer layer) {
            return layerBlocks.getOrDefault(layer, "minecraft:stone");
        }
    }
    
    /**
     * Represents a complete vertical column of terrain
     */
    public static class TerrainColumn {
        private final Map<Integer, String> blocks;
        private final int surfaceHeight;
        private final boolean isUnderwater;
        
        public TerrainColumn(int surfaceHeight, boolean isUnderwater) {
            this.blocks = new HashMap<>();
            this.surfaceHeight = surfaceHeight;
            this.isUnderwater = isUnderwater;
        }
        
        public void setBlock(int y, String blockType) {
            blocks.put(y, blockType);
        }
        
        public String getBlock(int y) {
            return blocks.getOrDefault(y, "minecraft:air");
        }
        
        public int getSurfaceHeight() { return surfaceHeight; }
        public boolean isUnderwater() { return isUnderwater; }
        
        /**
         * Get all blocks in height order for debugging
         */
        public Map<Integer, String> getAllBlocks() {
            return new HashMap<>(blocks);
        }
    }
    
    /**
     * Height information for debugging
     */
    public static class HeightInfo {
        public final int height;
        public final boolean underwater;
        public final GeologicalLayer layer;
        public final String surfaceBlock;
        public final double originalNoise;
        
        public HeightInfo(int height, boolean underwater, GeologicalLayer layer, 
                         String surfaceBlock, double originalNoise) {
            this.height = height;
            this.underwater = underwater;
            this.layer = layer;
            this.surfaceBlock = surfaceBlock;
            this.originalNoise = originalNoise;
        }
        
        @Override
        public String toString() {
            return String.format("Height: %d, Layer: %s, Block: %s, Underwater: %s, Noise: %.3f", 
                               height, layer, surfaceBlock, underwater, originalNoise);
        }
    }
}