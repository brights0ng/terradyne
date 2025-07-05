package net.starlight.terradyne.planet.features;

import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.intprovider.ConstantIntProvider;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.world.gen.feature.*;
import net.minecraft.world.gen.feature.size.TwoLayersFeatureSize;
import net.minecraft.world.gen.foliage.BlobFoliagePlacer;
import net.minecraft.world.gen.foliage.LargeOakFoliagePlacer;
import net.minecraft.world.gen.foliage.SpruceFoliagePlacer;
import net.minecraft.world.gen.stateprovider.BlockStateProvider;
import net.minecraft.world.gen.trunk.ForkingTrunkPlacer;
import net.minecraft.world.gen.trunk.LargeOakTrunkPlacer;
import net.minecraft.world.gen.trunk.StraightTrunkPlacer;
import net.starlight.terradyne.Terradyne;
import net.starlight.terradyne.planet.biology.BiomeFeatureComponents.TreeType;
import net.starlight.terradyne.planet.biology.VegetationPalette;

import static net.minecraft.block.Blocks.MANGROVE_LEAVES;
import static net.minecraft.block.Blocks.MANGROVE_WOOD;

/**
 * Creates configured tree features for each TreeType
 * Integrates TreeType (size/shape) with VegetationPalette (blocks)
 * FIXED: Complete implementation with all missing methods
 */
public class ModConfiguredFeatures {

    // === TREE CONFIGURED FEATURES ===
    // Each TreeType gets its own configured feature

    public static final RegistryKey<ConfiguredFeature<?, ?>> LARGE_DECIDUOUS_TREE = registerKey("large_deciduous_tree");
    public static final RegistryKey<ConfiguredFeature<?, ?>> LARGE_CONIFEROUS_TREE = registerKey("large_coniferous_tree");
    public static final RegistryKey<ConfiguredFeature<?, ?>> SMALL_DECIDUOUS_TREE = registerKey("small_deciduous_tree");
    public static final RegistryKey<ConfiguredFeature<?, ?>> SMALL_CONIFEROUS_TREE = registerKey("small_coniferous_tree");
    public static final RegistryKey<ConfiguredFeature<?, ?>> SPARSE_DECIDUOUS_TREE = registerKey("sparse_deciduous_tree");
    public static final RegistryKey<ConfiguredFeature<?, ?>> SPARSE_CONIFEROUS_TREE = registerKey("sparse_coniferous_tree");
    public static final RegistryKey<ConfiguredFeature<?, ?>> TROPICAL_CANOPY_TREE = registerKey("tropical_canopy_tree");
    public static final RegistryKey<ConfiguredFeature<?, ?>> MANGROVE_CLUSTER_TREE = registerKey("mangrove_cluster_tree");
    public static final RegistryKey<ConfiguredFeature<?, ?>> THERMOPHILIC_GROVE_TREE = registerKey("thermophilic_grove_tree");
    public static final RegistryKey<ConfiguredFeature<?, ?>> CARBONACEOUS_STRUCTURE_TREE = registerKey("carbonaceous_structure_tree");
    public static final RegistryKey<ConfiguredFeature<?, ?>> CRYSTALLINE_GROWTH_TREE = registerKey("crystalline_growth_tree");

    /**
     * Create TreeFeatureConfig based on TreeType and VegetationPalette
     * FIXED: Complete implementation that was missing from original file
     */
    public static ConfiguredFeature<TreeFeatureConfig, ?> createTreeConfig(TreeType treeType, VegetationPalette palette) {
        TreeFeatureConfig config = switch (treeType) {
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

        return new ConfiguredFeature<>(Feature.TREE, config);
    }

    // === TREE CONFIGURATION METHODS ===

    private static TreeFeatureConfig createLargeDeciduousConfig(VegetationPalette palette) {
        return new TreeFeatureConfig.Builder(
                BlockStateProvider.of(palette.getDeciduousWood()),
                new LargeOakTrunkPlacer(4, 8, 0),
                BlockStateProvider.of(palette.getDeciduousLeaves()),
                new LargeOakFoliagePlacer(ConstantIntProvider.create(2), ConstantIntProvider.create(4), 4),
                new TwoLayersFeatureSize(1, 0, 2))
                .build();
    }

    private static TreeFeatureConfig createLargeConiferousConfig(VegetationPalette palette) {
        return new TreeFeatureConfig.Builder(
                BlockStateProvider.of(palette.getConiferousWood()),
                new StraightTrunkPlacer(5, 6, 3),
                BlockStateProvider.of(palette.getConiferousLeaves()),
                new SpruceFoliagePlacer(UniformIntProvider.create(2, 3), UniformIntProvider.create(0, 2), UniformIntProvider.create(1, 2)),
                new TwoLayersFeatureSize(2, 0, 2))
                .build();
    }

    private static TreeFeatureConfig createSmallDeciduousConfig(VegetationPalette palette) {
        return new TreeFeatureConfig.Builder(
                BlockStateProvider.of(palette.getDeciduousWood()),
                new StraightTrunkPlacer(4, 2, 0),
                BlockStateProvider.of(palette.getDeciduousLeaves()),
                new BlobFoliagePlacer(ConstantIntProvider.create(2), ConstantIntProvider.create(0), 3),
                new TwoLayersFeatureSize(1, 0, 1))
                .build();
    }

    private static TreeFeatureConfig createSmallConiferousConfig(VegetationPalette palette) {
        return new TreeFeatureConfig.Builder(
                BlockStateProvider.of(palette.getConiferousWood()),
                new StraightTrunkPlacer(4, 4, 0),
                BlockStateProvider.of(palette.getConiferousLeaves()),
                new SpruceFoliagePlacer(UniformIntProvider.create(1, 2), UniformIntProvider.create(0, 2), UniformIntProvider.create(1, 1)),
                new TwoLayersFeatureSize(1, 0, 1))
                .build();
    }

    private static TreeFeatureConfig createSparseDeciduousConfig(VegetationPalette palette) {
        return new TreeFeatureConfig.Builder(
                BlockStateProvider.of(palette.getDeciduousWood()),
                new StraightTrunkPlacer(3, 3, 0),
                BlockStateProvider.of(palette.getDeciduousLeaves()),
                new BlobFoliagePlacer(ConstantIntProvider.create(1), ConstantIntProvider.create(0), 2),
                new TwoLayersFeatureSize(1, 0, 1))
                .build();
    }

    private static TreeFeatureConfig createSparseConiferousConfig(VegetationPalette palette) {
        return new TreeFeatureConfig.Builder(
                BlockStateProvider.of(palette.getConiferousWood()),
                new StraightTrunkPlacer(3, 4, 0),
                BlockStateProvider.of(palette.getConiferousLeaves()),
                new SpruceFoliagePlacer(UniformIntProvider.create(1, 1), UniformIntProvider.create(0, 1), UniformIntProvider.create(1, 1)),
                new TwoLayersFeatureSize(1, 0, 1))
                .build();
    }

    private static TreeFeatureConfig createTropicalCanopyConfig(VegetationPalette palette) {
        return new TreeFeatureConfig.Builder(
                BlockStateProvider.of(palette.getDeciduousWood()),
                new LargeOakTrunkPlacer(6, 12, 0),
                BlockStateProvider.of(palette.getDeciduousLeaves()),
                new LargeOakFoliagePlacer(ConstantIntProvider.create(3), ConstantIntProvider.create(5), 5),
                new TwoLayersFeatureSize(1, 0, 2))
                .build();
    }

    private static TreeFeatureConfig createMangroveClusterConfig(VegetationPalette palette) {
        return new TreeFeatureConfig.Builder(
                BlockStateProvider.of(MANGROVE_WOOD),
                new ForkingTrunkPlacer(2, 2, 2),
                BlockStateProvider.of(MANGROVE_LEAVES),
                new BlobFoliagePlacer(ConstantIntProvider.create(2), ConstantIntProvider.create(0), 3),
                new TwoLayersFeatureSize(1, 0, 1))
                .build();
    }

    private static TreeFeatureConfig createThermophilicGroveConfig(VegetationPalette palette) {
        return new TreeFeatureConfig.Builder(
                BlockStateProvider.of(palette.getDeciduousWood()),
                new StraightTrunkPlacer(4, 3, 0),
                BlockStateProvider.of(palette.getDeciduousLeaves()),
                new BlobFoliagePlacer(ConstantIntProvider.create(2), ConstantIntProvider.create(0), 3),
                new TwoLayersFeatureSize(1, 0, 1))
                .build();
    }

    private static TreeFeatureConfig createCarbonaceousStructureConfig(VegetationPalette palette) {
        return new TreeFeatureConfig.Builder(
                BlockStateProvider.of(palette.getConiferousWood()),
                new StraightTrunkPlacer(5, 4, 0),
                BlockStateProvider.of(palette.getConiferousLeaves()),
                new BlobFoliagePlacer(ConstantIntProvider.create(1), ConstantIntProvider.create(0), 2),
                new TwoLayersFeatureSize(1, 0, 1))
                .build();
    }

    private static TreeFeatureConfig createCrystallineGrowthConfig(VegetationPalette palette) {
        return new TreeFeatureConfig.Builder(
                BlockStateProvider.of(palette.getConiferousWood()),
                new StraightTrunkPlacer(3, 2, 0),
                BlockStateProvider.of(palette.getConiferousLeaves()),
                new BlobFoliagePlacer(ConstantIntProvider.create(1), ConstantIntProvider.create(0), 1),
                new TwoLayersFeatureSize(1, 0, 1))
                .build();
    }

    // === UTILITY METHODS ===

    /**
     * Create registry key for configured feature
     */
    private static RegistryKey<ConfiguredFeature<?, ?>> registerKey(String name) {
        return RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, new Identifier(Terradyne.MOD_ID, name));
    }

    /**
     * Get the configured feature key for a specific tree type
     * FIXED: Method that was referenced but missing
     */
    public static RegistryKey<ConfiguredFeature<?, ?>> getConfiguredFeatureKey(TreeType treeType) {
        return switch (treeType) {
            case LARGE_DECIDUOUS -> LARGE_DECIDUOUS_TREE;
            case LARGE_CONIFEROUS -> LARGE_CONIFEROUS_TREE;
            case SMALL_DECIDUOUS -> SMALL_DECIDUOUS_TREE;
            case SMALL_CONIFEROUS -> SMALL_CONIFEROUS_TREE;
            case SPARSE_DECIDUOUS -> SPARSE_DECIDUOUS_TREE;
            case SPARSE_CONIFEROUS -> SPARSE_CONIFEROUS_TREE;
            case TROPICAL_CANOPY -> TROPICAL_CANOPY_TREE;
            case MANGROVE_CLUSTERS -> MANGROVE_CLUSTER_TREE;
            case THERMOPHILIC_GROVES -> THERMOPHILIC_GROVE_TREE;
            case CARBONACEOUS_STRUCTURES -> CARBONACEOUS_STRUCTURE_TREE;
            case CRYSTALLINE_GROWTHS -> CRYSTALLINE_GROWTH_TREE;
        };
    }

    /**
     * Bootstrap method for data generation
     * FIXED: Method called by TerradyneDataGenerator
     */
    public static void bootstrap(net.minecraft.registry.Registerable<ConfiguredFeature<?, ?>> context) {
        VegetationPalette defaultPalette = VegetationPalette.TEMPERATE_DECIDUOUS;

        // Register all tree configured features
        register(context, LARGE_DECIDUOUS_TREE, createTreeConfig(TreeType.LARGE_DECIDUOUS, defaultPalette));
        register(context, LARGE_CONIFEROUS_TREE, createTreeConfig(TreeType.LARGE_CONIFEROUS, defaultPalette));
        register(context, SMALL_DECIDUOUS_TREE, createTreeConfig(TreeType.SMALL_DECIDUOUS, defaultPalette));
        register(context, SMALL_CONIFEROUS_TREE, createTreeConfig(TreeType.SMALL_CONIFEROUS, defaultPalette));
        register(context, SPARSE_DECIDUOUS_TREE, createTreeConfig(TreeType.SPARSE_DECIDUOUS, defaultPalette));
        register(context, SPARSE_CONIFEROUS_TREE, createTreeConfig(TreeType.SPARSE_CONIFEROUS, defaultPalette));
        register(context, TROPICAL_CANOPY_TREE, createTreeConfig(TreeType.TROPICAL_CANOPY, defaultPalette));
        register(context, MANGROVE_CLUSTER_TREE, createTreeConfig(TreeType.MANGROVE_CLUSTERS, defaultPalette));
        register(context, THERMOPHILIC_GROVE_TREE, createTreeConfig(TreeType.THERMOPHILIC_GROVES, defaultPalette));
        register(context, CARBONACEOUS_STRUCTURE_TREE, createTreeConfig(TreeType.CARBONACEOUS_STRUCTURES, defaultPalette));
        register(context, CRYSTALLINE_GROWTH_TREE, createTreeConfig(TreeType.CRYSTALLINE_GROWTHS, defaultPalette));
    }

    /**
     * Helper method to register configured features
     */
    private static void register(net.minecraft.registry.Registerable<ConfiguredFeature<?, ?>> context,
                                 RegistryKey<ConfiguredFeature<?, ?>> key,
                                 ConfiguredFeature<TreeFeatureConfig, ?> configuredFeature) {
        context.register(key, configuredFeature);
    }
}