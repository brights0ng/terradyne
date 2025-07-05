package net.starlight.terradyne.planet.features;

import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.feature.PlacedFeatures;
import net.minecraft.world.gen.placementmodifier.*;
import net.starlight.terradyne.Terradyne;
import net.starlight.terradyne.planet.biology.BiomeFeatureComponents.TreeType;

import java.util.List;

/**
 * Creates placed features for tree generation with physics-based density
 * Controls WHERE and HOW OFTEN trees spawn in each biome
 * FIXED: Complete implementation with all missing methods
 */
public class ModPlacedFeatures {

    // === TREE PLACED FEATURES ===
    // Each TreeType gets placement rules for density and positioning

    public static final RegistryKey<PlacedFeature> LARGE_DECIDUOUS_TREE_PLACED = registerKey("large_deciduous_tree_placed");
    public static final RegistryKey<PlacedFeature> LARGE_CONIFEROUS_TREE_PLACED = registerKey("large_coniferous_tree_placed");
    public static final RegistryKey<PlacedFeature> SMALL_DECIDUOUS_TREE_PLACED = registerKey("small_deciduous_tree_placed");
    public static final RegistryKey<PlacedFeature> SMALL_CONIFEROUS_TREE_PLACED = registerKey("small_coniferous_tree_placed");
    public static final RegistryKey<PlacedFeature> SPARSE_DECIDUOUS_TREE_PLACED = registerKey("sparse_deciduous_tree_placed");
    public static final RegistryKey<PlacedFeature> SPARSE_CONIFEROUS_TREE_PLACED = registerKey("sparse_coniferous_tree_placed");
    public static final RegistryKey<PlacedFeature> TROPICAL_CANOPY_TREE_PLACED = registerKey("tropical_canopy_tree_placed");
    public static final RegistryKey<PlacedFeature> MANGROVE_CLUSTER_TREE_PLACED = registerKey("mangrove_cluster_tree_placed");
    public static final RegistryKey<PlacedFeature> THERMOPHILIC_GROVE_TREE_PLACED = registerKey("thermophilic_grove_tree_placed");
    public static final RegistryKey<PlacedFeature> CARBONACEOUS_STRUCTURE_TREE_PLACED = registerKey("carbonaceous_structure_tree_placed");
    public static final RegistryKey<PlacedFeature> CRYSTALLINE_GROWTH_TREE_PLACED = registerKey("crystalline_growth_tree_placed");

    /**
     * Create placement modifiers for specific tree type
     * FIXED: Method that was referenced but missing
     */
    public static List<PlacementModifier> createTreePlacement(TreeType treeType) {
        return switch (treeType) {
            case LARGE_DECIDUOUS -> createLargeDeciduousPlacement();
            case LARGE_CONIFEROUS -> createLargeConiferousPlacement();
            case SMALL_DECIDUOUS -> createSmallDeciduousPlacement();
            case SMALL_CONIFEROUS -> createSmallConiferousPlacement();
            case SPARSE_DECIDUOUS -> createSparseDeciduousPlacement();
            case SPARSE_CONIFEROUS -> createSparseConiferousPlacement();
            case TROPICAL_CANOPY -> createTropicalCanopyPlacement();
            case MANGROVE_CLUSTERS -> createMangroveClusterPlacement();
            case THERMOPHILIC_GROVES -> createThermophilicGrovePlacement();
            case CARBONACEOUS_STRUCTURES -> createCarbonaceousStructurePlacement();
            case CRYSTALLINE_GROWTHS -> createCrystallineGrowthPlacement();
        };
    }

    // === PLACEMENT CREATION METHODS ===

    private static List<PlacementModifier> createLargeDeciduousPlacement() {
        return List.of(
                CountPlacementModifier.of(8), // 8 attempts per chunk
                SquarePlacementModifier.of(), // Random X/Z in chunk
                SurfaceWaterDepthFilterPlacementModifier.of(0), // Only on land
                PlacedFeatures.OCEAN_FLOOR_HEIGHTMAP, // Use heightmap
                BiomePlacementModifier.of() // Only in appropriate biomes
        );
    }

    private static List<PlacementModifier> createLargeConiferousPlacement() {
        return List.of(
                CountPlacementModifier.of(6), // 6 attempts per chunk
                SquarePlacementModifier.of(),
                SurfaceWaterDepthFilterPlacementModifier.of(0),
                PlacedFeatures.OCEAN_FLOOR_HEIGHTMAP,
                BiomePlacementModifier.of()
        );
    }

    private static List<PlacementModifier> createSmallDeciduousPlacement() {
        return List.of(
                CountPlacementModifier.of(12), // 12 attempts per chunk (more small trees)
                SquarePlacementModifier.of(),
                SurfaceWaterDepthFilterPlacementModifier.of(0),
                PlacedFeatures.OCEAN_FLOOR_HEIGHTMAP,
                BiomePlacementModifier.of()
        );
    }

    private static List<PlacementModifier> createSmallConiferousPlacement() {
        return List.of(
                CountPlacementModifier.of(10), // 10 attempts per chunk
                SquarePlacementModifier.of(),
                SurfaceWaterDepthFilterPlacementModifier.of(0),
                PlacedFeatures.OCEAN_FLOOR_HEIGHTMAP,
                BiomePlacementModifier.of()
        );
    }

    private static List<PlacementModifier> createSparseDeciduousPlacement() {
        return List.of(
                CountPlacementModifier.of(2), // Only 2 attempts per chunk (sparse!)
                SquarePlacementModifier.of(),
                SurfaceWaterDepthFilterPlacementModifier.of(0),
                PlacedFeatures.OCEAN_FLOOR_HEIGHTMAP,
                BiomePlacementModifier.of()
        );
    }

    private static List<PlacementModifier> createSparseConiferousPlacement() {
        return List.of(
                CountPlacementModifier.of(2), // Only 2 attempts per chunk (sparse!)
                SquarePlacementModifier.of(),
                SurfaceWaterDepthFilterPlacementModifier.of(0),
                PlacedFeatures.OCEAN_FLOOR_HEIGHTMAP,
                BiomePlacementModifier.of()
        );
    }

    private static List<PlacementModifier> createTropicalCanopyPlacement() {
        return List.of(
                CountPlacementModifier.of(4), // 4 attempts per chunk (large trees need space)
                SquarePlacementModifier.of(),
                SurfaceWaterDepthFilterPlacementModifier.of(0),
                PlacedFeatures.OCEAN_FLOOR_HEIGHTMAP,
                BiomePlacementModifier.of()
        );
    }

    private static List<PlacementModifier> createMangroveClusterPlacement() {
        return List.of(
                CountPlacementModifier.of(6), // 6 attempts per chunk
                SquarePlacementModifier.of(),
                SurfaceWaterDepthFilterPlacementModifier.of(3), // Can be in shallow water (up to 3 blocks deep)
                PlacedFeatures.OCEAN_FLOOR_HEIGHTMAP,
                BiomePlacementModifier.of()
        );
    }

    private static List<PlacementModifier> createThermophilicGrovePlacement() {
        return List.of(
                CountPlacementModifier.of(4), // 4 attempts per chunk (harsh conditions)
                SquarePlacementModifier.of(),
                SurfaceWaterDepthFilterPlacementModifier.of(0),
                PlacedFeatures.OCEAN_FLOOR_HEIGHTMAP,
                BiomePlacementModifier.of()
        );
    }

    private static List<PlacementModifier> createCarbonaceousStructurePlacement() {
        return List.of(
                CountPlacementModifier.of(3), // 3 attempts per chunk (unusual formations)
                SquarePlacementModifier.of(),
                SurfaceWaterDepthFilterPlacementModifier.of(0),
                PlacedFeatures.OCEAN_FLOOR_HEIGHTMAP,
                BiomePlacementModifier.of()
        );
    }

    private static List<PlacementModifier> createCrystallineGrowthPlacement() {
        return List.of(
                CountPlacementModifier.of(2), // 2 attempts per chunk (very rare formations)
                SquarePlacementModifier.of(),
                SurfaceWaterDepthFilterPlacementModifier.of(0),
                PlacedFeatures.OCEAN_FLOOR_HEIGHTMAP,
                BiomePlacementModifier.of()
        );
    }

    // === UTILITY METHODS ===

    /**
     * Create registry key for placed feature
     */
    private static RegistryKey<PlacedFeature> registerKey(String name) {
        return RegistryKey.of(RegistryKeys.PLACED_FEATURE, new Identifier(Terradyne.MOD_ID, name));
    }

    /**
     * Get base attempt count for TreeType (informational)
     */
    public static int getBaseAttempts(TreeType treeType) {
        return switch (treeType) {
            case LARGE_DECIDUOUS -> 8;
            case LARGE_CONIFEROUS -> 6;
            case SMALL_DECIDUOUS -> 12;
            case SMALL_CONIFEROUS -> 10;
            case SPARSE_DECIDUOUS, SPARSE_CONIFEROUS -> 2;
            case TROPICAL_CANOPY, THERMOPHILIC_GROVES -> 4;
            case MANGROVE_CLUSTERS -> 6;
            case CARBONACEOUS_STRUCTURES -> 3;
            case CRYSTALLINE_GROWTHS -> 2;
        };
    }

    /**
     * Bootstrap method for data generation
     * FIXED: Method called by TerradyneDataGenerator
     */
    public static void bootstrap(net.minecraft.registry.Registerable<PlacedFeature> context) {
        var configuredFeatureRegistryEntryLookup = context.getRegistryLookup(net.minecraft.registry.RegistryKeys.CONFIGURED_FEATURE);

        // Register placement rules for each tree type
        register(context, LARGE_DECIDUOUS_TREE_PLACED,
                configuredFeatureRegistryEntryLookup.getOrThrow(ModConfiguredFeatures.LARGE_DECIDUOUS_TREE),
                createTreePlacement(TreeType.LARGE_DECIDUOUS));

        register(context, LARGE_CONIFEROUS_TREE_PLACED,
                configuredFeatureRegistryEntryLookup.getOrThrow(ModConfiguredFeatures.LARGE_CONIFEROUS_TREE),
                createTreePlacement(TreeType.LARGE_CONIFEROUS));

        register(context, SMALL_DECIDUOUS_TREE_PLACED,
                configuredFeatureRegistryEntryLookup.getOrThrow(ModConfiguredFeatures.SMALL_DECIDUOUS_TREE),
                createTreePlacement(TreeType.SMALL_DECIDUOUS));

        register(context, SMALL_CONIFEROUS_TREE_PLACED,
                configuredFeatureRegistryEntryLookup.getOrThrow(ModConfiguredFeatures.SMALL_CONIFEROUS_TREE),
                createTreePlacement(TreeType.SMALL_CONIFEROUS));

        register(context, SPARSE_DECIDUOUS_TREE_PLACED,
                configuredFeatureRegistryEntryLookup.getOrThrow(ModConfiguredFeatures.SPARSE_DECIDUOUS_TREE),
                createTreePlacement(TreeType.SPARSE_DECIDUOUS));

        register(context, SPARSE_CONIFEROUS_TREE_PLACED,
                configuredFeatureRegistryEntryLookup.getOrThrow(ModConfiguredFeatures.SPARSE_CONIFEROUS_TREE),
                createTreePlacement(TreeType.SPARSE_CONIFEROUS));

        register(context, TROPICAL_CANOPY_TREE_PLACED,
                configuredFeatureRegistryEntryLookup.getOrThrow(ModConfiguredFeatures.TROPICAL_CANOPY_TREE),
                createTreePlacement(TreeType.TROPICAL_CANOPY));

        register(context, MANGROVE_CLUSTER_TREE_PLACED,
                configuredFeatureRegistryEntryLookup.getOrThrow(ModConfiguredFeatures.MANGROVE_CLUSTER_TREE),
                createTreePlacement(TreeType.MANGROVE_CLUSTERS));

        register(context, THERMOPHILIC_GROVE_TREE_PLACED,
                configuredFeatureRegistryEntryLookup.getOrThrow(ModConfiguredFeatures.THERMOPHILIC_GROVE_TREE),
                createTreePlacement(TreeType.THERMOPHILIC_GROVES));

        register(context, CARBONACEOUS_STRUCTURE_TREE_PLACED,
                configuredFeatureRegistryEntryLookup.getOrThrow(ModConfiguredFeatures.CARBONACEOUS_STRUCTURE_TREE),
                createTreePlacement(TreeType.CARBONACEOUS_STRUCTURES));

        register(context, CRYSTALLINE_GROWTH_TREE_PLACED,
                configuredFeatureRegistryEntryLookup.getOrThrow(ModConfiguredFeatures.CRYSTALLINE_GROWTH_TREE),
                createTreePlacement(TreeType.CRYSTALLINE_GROWTHS));
    }

    /**
     * Helper method to register placed features
     */
    private static void register(Registerable<PlacedFeature> context,
                                 RegistryKey<PlacedFeature> key,
                                 RegistryEntry.Reference<ConfiguredFeature<?, ?>> configuredFeature,
                                 List<PlacementModifier> placementModifiers) {
        context.register(key, new PlacedFeature(configuredFeature, placementModifiers));
    }
}