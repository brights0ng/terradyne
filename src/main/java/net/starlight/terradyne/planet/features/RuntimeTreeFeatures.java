package net.starlight.terradyne.planet.features;

import net.minecraft.block.Blocks;
import net.minecraft.util.math.intprovider.ConstantIntProvider;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.TreeFeatureConfig;
import net.minecraft.world.gen.feature.size.TwoLayersFeatureSize;
import net.minecraft.world.gen.foliage.BlobFoliagePlacer;
import net.minecraft.world.gen.foliage.LargeOakFoliagePlacer;
import net.minecraft.world.gen.foliage.SpruceFoliagePlacer;
import net.minecraft.world.gen.stateprovider.BlockStateProvider;
import net.minecraft.world.gen.trunk.ForkingTrunkPlacer;
import net.minecraft.world.gen.trunk.LargeOakTrunkPlacer;
import net.minecraft.world.gen.trunk.StraightTrunkPlacer;
import net.starlight.terradyne.planet.biology.BiomeFeatureComponents.TreeType;
import net.starlight.terradyne.planet.biology.VegetationPalette;

import java.util.HashMap;
import java.util.Map;

/**
 * Runtime tree feature generation for 1.20.1 compatibility
 * Creates tree features dynamically during world generation instead of data generation
 */
public class RuntimeTreeFeatures {

    private static final Map<String, ConfiguredFeature<TreeFeatureConfig, ?>> TREE_CACHE = new HashMap<>();

    /**
     * Get or create a tree feature for specific TreeType and VegetationPalette
     * This caches tree configurations to avoid recreating them
     */
    public static ConfiguredFeature<TreeFeatureConfig, ?> getTreeFeature(TreeType treeType, VegetationPalette palette) {
        String cacheKey = treeType.name() + "_" + palette.name();
        
        return TREE_CACHE.computeIfAbsent(cacheKey, key -> 
            new ConfiguredFeature<>(ModFeatures.PHYSICS_TREE, createTreeConfig(treeType, palette))
        );
    }

    /**
     * Create TreeFeatureConfig based on TreeType and VegetationPalette
     */
    private static TreeFeatureConfig createTreeConfig(TreeType treeType, VegetationPalette palette) {
        return switch (treeType) {
            case LARGE_DECIDUOUS -> createLargeDeciduousConfig(palette);
            case LARGE_CONIFEROUS -> createLargeConiferousConfig(palette);
            case SMALL_DECIDUOUS -> createSmallDeciduousConfig(palette);
            case SMALL_CONIFEROUS -> createSmallConiferousConfig(palette);
            case SPARSE_DECIDUOUS -> createSparseDeciduousConfig(palette);
            case SPARSE_CONIFEROUS -> createSparseConiferousConfig(palette);
            case TROPICAL_CANOPY -> createTropicalCanopyConfig(palette);
            case MANGROVE_CLUSTERS -> createMangroveClusterConfig(palette);
            case THERMOPHILIC_GROVES -> createThermophilicGroveConfig(palette);
            case CARBONACEOUS_STRUCTURES -> createCarbonaceousStructureConfig(palette);
            case CRYSTALLINE_GROWTHS -> createCrystallineGrowthConfig(palette);
        };
    }

    // === TREE CONFIGURATION METHODS ===

    /**
     * Large Deciduous: 8-12 blocks tall, broad canopy
     */
    private static TreeFeatureConfig createLargeDeciduousConfig(VegetationPalette palette) {
        return new TreeFeatureConfig.Builder(
                BlockStateProvider.of(palette.hasDeciduousVegetation() ? palette.getDeciduousWood() : Blocks.OAK_LOG),
                new ForkingTrunkPlacer(8, 3, 3), // Base height 8, random 0-3, fork height 3
                BlockStateProvider.of(palette.hasDeciduousVegetation() ? palette.getDeciduousLeaves() : Blocks.OAK_LEAVES),
                new LargeOakFoliagePlacer(ConstantIntProvider.create(2), ConstantIntProvider.create(4), 4),
                new TwoLayersFeatureSize(1, 0, 2)
        ).build();
    }

    /**
     * Large Coniferous: 10-16 blocks tall, narrow canopy
     */
    private static TreeFeatureConfig createLargeConiferousConfig(VegetationPalette palette) {
        return new TreeFeatureConfig.Builder(
                BlockStateProvider.of(palette.hasConiferousVegetation() ? palette.getConiferousWood() : Blocks.SPRUCE_LOG),
                new StraightTrunkPlacer(10, 4, 2), // Base height 10, random 0-4, no branching
                BlockStateProvider.of(palette.hasConiferousVegetation() ? palette.getConiferousLeaves() : Blocks.SPRUCE_LEAVES),
                new SpruceFoliagePlacer(UniformIntProvider.create(2, 3), UniformIntProvider.create(0, 2), UniformIntProvider.create(1, 2)),
                new TwoLayersFeatureSize(2, 0, 2)
        ).build();
    }

    /**
     * Small Deciduous: 4-6 blocks tall, compact canopy
     */
    private static TreeFeatureConfig createSmallDeciduousConfig(VegetationPalette palette) {
        return new TreeFeatureConfig.Builder(
                BlockStateProvider.of(palette.hasDeciduousVegetation() ? palette.getDeciduousWood() : Blocks.BIRCH_LOG),
                new StraightTrunkPlacer(4, 2, 0), // Base height 4, random 0-2
                BlockStateProvider.of(palette.hasDeciduousVegetation() ? palette.getDeciduousLeaves() : Blocks.BIRCH_LEAVES),
                new BlobFoliagePlacer(ConstantIntProvider.create(2), ConstantIntProvider.create(0), 3),
                new TwoLayersFeatureSize(1, 0, 1)
        ).build();
    }

    /**
     * Small Coniferous: 5-8 blocks tall, narrow canopy
     */
    private static TreeFeatureConfig createSmallConiferousConfig(VegetationPalette palette) {
        return new TreeFeatureConfig.Builder(
                BlockStateProvider.of(palette.hasConiferousVegetation() ? palette.getConiferousWood() : Blocks.SPRUCE_LOG),
                new StraightTrunkPlacer(5, 3, 0), // Base height 5, random 0-3
                BlockStateProvider.of(palette.hasConiferousVegetation() ? palette.getConiferousLeaves() : Blocks.SPRUCE_LEAVES),
                new SpruceFoliagePlacer(UniformIntProvider.create(1, 2), UniformIntProvider.create(0, 1), UniformIntProvider.create(1, 1)),
                new TwoLayersFeatureSize(1, 0, 1)
        ).build();
    }

    /**
     * Sparse Deciduous: Same as large deciduous (sparsity handled by placement)
     */
    private static TreeFeatureConfig createSparseDeciduousConfig(VegetationPalette palette) {
        return createLargeDeciduousConfig(palette); // Same trees, just fewer of them
    }

    /**
     * Sparse Coniferous: Same as large coniferous (sparsity handled by placement)
     */
    private static TreeFeatureConfig createSparseConiferousConfig(VegetationPalette palette) {
        return createLargeConiferousConfig(palette); // Same trees, just fewer of them
    }

    /**
     * Tropical Canopy: Very tall and broad for jungle-like environments
     */
    private static TreeFeatureConfig createTropicalCanopyConfig(VegetationPalette palette) {
        return new TreeFeatureConfig.Builder(
                BlockStateProvider.of(palette.hasDeciduousVegetation() ? palette.getDeciduousWood() : Blocks.JUNGLE_LOG),
                new LargeOakTrunkPlacer(12, 6, 3), // Base height 12, random 0-6, very large
                BlockStateProvider.of(palette.hasDeciduousVegetation() ? palette.getDeciduousLeaves() : Blocks.JUNGLE_LEAVES),
                new LargeOakFoliagePlacer(ConstantIntProvider.create(3), ConstantIntProvider.create(4), 6),
                new TwoLayersFeatureSize(1, 0, 2)
        ).build();
    }

    /**
     * Mangrove Clusters: Medium height, specialized for wetlands
     */
    private static TreeFeatureConfig createMangroveClusterConfig(VegetationPalette palette) {
        return new TreeFeatureConfig.Builder(
                BlockStateProvider.of(Blocks.MANGROVE_LOG), // Always use mangrove wood
                new StraightTrunkPlacer(6, 3, 1), // Base height 6, random 0-3
                BlockStateProvider.of(Blocks.MANGROVE_LEAVES), // Always use mangrove leaves
                new BlobFoliagePlacer(ConstantIntProvider.create(2), ConstantIntProvider.create(1), 3),
                new TwoLayersFeatureSize(1, 0, 1)
        ).build();
    }

    /**
     * Thermophilic Groves: Heat-resistant trees for volcanic environments
     */
    private static TreeFeatureConfig createThermophilicGroveConfig(VegetationPalette palette) {
        return new TreeFeatureConfig.Builder(
                BlockStateProvider.of(palette.hasDeciduousVegetation() ? palette.getDeciduousWood() : Blocks.ACACIA_LOG),
                new ForkingTrunkPlacer(6, 2, 2), // Base height 6, heat-resistant form
                BlockStateProvider.of(palette.hasDeciduousVegetation() ? palette.getDeciduousLeaves() : Blocks.ACACIA_LEAVES),
                new BlobFoliagePlacer(ConstantIntProvider.create(2), ConstantIntProvider.create(0), 3),
                new TwoLayersFeatureSize(1, 0, 1)
        ).build();
    }

    /**
     * Carbonaceous Structures: Carbon-based "trees" using carbon blocks
     */
    private static TreeFeatureConfig createCarbonaceousStructureConfig(VegetationPalette palette) {
        return new TreeFeatureConfig.Builder(
                BlockStateProvider.of(Blocks.COAL_BLOCK), // Carbon structure trunk
                new StraightTrunkPlacer(7, 3, 0), // Base height 7, simple structure
                BlockStateProvider.of(Blocks.GRAY_STAINED_GLASS), // Translucent carbon "leaves"
                new BlobFoliagePlacer(ConstantIntProvider.create(1), ConstantIntProvider.create(1), 2),
                new TwoLayersFeatureSize(1, 0, 1)
        ).build();
    }

    /**
     * Crystalline Growths: Mineral-based tree-like formations
     */
    private static TreeFeatureConfig createCrystallineGrowthConfig(VegetationPalette palette) {
        return new TreeFeatureConfig.Builder(
                BlockStateProvider.of(Blocks.QUARTZ_PILLAR), // Crystal structure trunk
                new StraightTrunkPlacer(5, 2, 0), // Base height 5, rigid crystal growth
                BlockStateProvider.of(Blocks.WHITE_STAINED_GLASS), // Translucent crystal formations
                new BlobFoliagePlacer(ConstantIntProvider.create(1), ConstantIntProvider.create(0), 2),
                new TwoLayersFeatureSize(1, 0, 1)
        ).build();
    }

    /**
     * Clear the tree cache (for memory management)
     */
    public static void clearCache() {
        TREE_CACHE.clear();
    }

    /**
     * Get cache statistics for debugging
     */
    public static String getCacheStats() {
        return String.format("RuntimeTreeFeatures: %d cached configurations", TREE_CACHE.size());
    }
}