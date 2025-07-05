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
     * Bootstrap placed features for data generation
     */
    public static void bootstrap(Registerable<PlacedFeature> context) {
        var configuredFeatureRegistryEntryLookup = context.getRegistryLookup(RegistryKeys.CONFIGURED_FEATURE);

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
     * Create placement modifiers for specific tree type
     * This defines base placement rules - actual density will be calculated dynamically
     */
    private static List<PlacementModifier> createTreePlacement(TreeType treeType) {
        return switch (treeType) {
            case LARGE_DECIDUOUS -> createLargeTreePlacement(8); // Base 8 attempts per chunk
            case LARGE_CONIFEROUS -> createLargeTreePlacement(6); // Base 6 attempts per chunk
            case SMALL_DECIDUOUS -> createSmallTreePlacement(12); // Base 12 attempts per chunk
            case SMALL_CONIFEROUS -> createSmallTreePlacement(10); // Base 10 attempts per chunk
            case SPARSE_DECIDUOUS -> createSparseTreePlacement(2); // Base 2 attempts per chunk
            case SPARSE_CONIFEROUS -> createSparseTreePlacement(2); // Base 2 attempts per chunk
            case TROPICAL_CANOPY -> createCanopyTreePlacement(4); // Base 4 attempts per chunk (large trees)
            case MANGROVE_CLUSTERS -> createWetlandTreePlacement(6); // Base 6 attempts per chunk
            case THERMOPHILIC_GROVES -> createSpecialTreePlacement(4); // Base 4 attempts per chunk
            case CARBONACEOUS_STRUCTURES -> createSpecialTreePlacement(3); // Base 3 attempts per chunk
            case CRYSTALLINE_GROWTHS -> createSpecialTreePlacement(2); // Base 2 attempts per chunk
        };
    }

    /**
     * Large tree placement: Fewer attempts, needs more space
     */
    private static List<PlacementModifier> createLargeTreePlacement(int baseAttempts) {
        return List.of(
                CountPlacementModifier.of(baseAttempts),
                SquarePlacementModifier.of(),
                SurfaceWaterDepthFilterPlacementModifier.of(0), // Not in water
                PlacedFeatures.OCEAN_FLOOR_HEIGHTMAP, // Place on terrain surface
                BiomePlacementModifier.of()
        );
    }

    /**
     * Small tree placement: More attempts, can be closer together
     */
    private static List<PlacementModifier> createSmallTreePlacement(int baseAttempts) {
        return List.of(
                CountPlacementModifier.of(baseAttempts),
                SquarePlacementModifier.of(),
                SurfaceWaterDepthFilterPlacementModifier.of(0), // Not in water
                PlacedFeatures.OCEAN_FLOOR_HEIGHTMAP, // Place on terrain surface
                BiomePlacementModifier.of()
        );
    }

    /**
     * Sparse tree placement: Very few attempts, widely spaced
     */
    private static List<PlacementModifier> createSparseTreePlacement(int baseAttempts) {
        return List.of(
                CountPlacementModifier.of(baseAttempts),
                SquarePlacementModifier.of(),
                SurfaceWaterDepthFilterPlacementModifier.of(0), // Not in water
                PlacedFeatures.OCEAN_FLOOR_HEIGHTMAP, // Place on terrain surface
                BiomePlacementModifier.of()
        );
    }

    /**
     * Canopy tree placement: Dense forest coverage for tropical biomes
     */
    private static List<PlacementModifier> createCanopyTreePlacement(int baseAttempts) {
        return List.of(
                CountPlacementModifier.of(baseAttempts),
                SquarePlacementModifier.of(),
                SurfaceWaterDepthFilterPlacementModifier.of(0), // Not in water
                PlacedFeatures.OCEAN_FLOOR_HEIGHTMAP, // Place on terrain surface
                BiomePlacementModifier.of()
        );
    }

    /**
     * Wetland tree placement: Can tolerate shallow water
     */
    private static List<PlacementModifier> createWetlandTreePlacement(int baseAttempts) {
        return List.of(
                CountPlacementModifier.of(baseAttempts),
                SquarePlacementModifier.of(),
                SurfaceWaterDepthFilterPlacementModifier.of(3), // Can be in shallow water
                PlacedFeatures.OCEAN_FLOOR_HEIGHTMAP, // Place on terrain surface or water bottom
                BiomePlacementModifier.of()
        );
    }

    /**
     * Special tree placement: For unusual tree types (carbonaceous, crystalline, etc.)
     */
    private static List<PlacementModifier> createSpecialTreePlacement(int baseAttempts) {
        return List.of(
                CountPlacementModifier.of(baseAttempts),
                SquarePlacementModifier.of(),
                SurfaceWaterDepthFilterPlacementModifier.of(0), // Not in water
                PlacedFeatures.OCEAN_FLOOR_HEIGHTMAP, // Place on terrain surface
                BiomePlacementModifier.of()
        );
    }

    /**
     * Get placed feature key for TreeType
     */
    public static RegistryKey<PlacedFeature> getPlacedFeatureKey(TreeType treeType) {
        return switch (treeType) {
            case LARGE_DECIDUOUS -> LARGE_DECIDUOUS_TREE_PLACED;
            case LARGE_CONIFEROUS -> LARGE_CONIFEROUS_TREE_PLACED;
            case SMALL_DECIDUOUS -> SMALL_DECIDUOUS_TREE_PLACED;
            case SMALL_CONIFEROUS -> SMALL_CONIFEROUS_TREE_PLACED;
            case SPARSE_DECIDUOUS -> SPARSE_DECIDUOUS_TREE_PLACED;
            case SPARSE_CONIFEROUS -> SPARSE_CONIFEROUS_TREE_PLACED;
            case TROPICAL_CANOPY -> TROPICAL_CANOPY_TREE_PLACED;
            case MANGROVE_CLUSTERS -> MANGROVE_CLUSTER_TREE_PLACED;
            case THERMOPHILIC_GROVES -> THERMOPHILIC_GROVE_TREE_PLACED;
            case CARBONACEOUS_STRUCTURES -> CARBONACEOUS_STRUCTURE_TREE_PLACED;
            case CRYSTALLINE_GROWTHS -> CRYSTALLINE_GROWTH_TREE_PLACED;
        };
    }

    /**
     * Create dynamic tree placement with physics-based density
     * This method will be used at runtime to adjust tree density based on planet conditions
     */
    public static List<PlacementModifier> createDynamicTreePlacement(TreeType treeType, double densityMultiplier) {
        // Get base placement
        List<PlacementModifier> basePlacement = createTreePlacement(treeType);
        
        // Calculate adjusted attempt count based on density
        int baseAttempts = getBaseAttempts(treeType);
        int adjustedAttempts = Math.max(1, (int) Math.round(baseAttempts * densityMultiplier));
        
        // Replace the CountPlacementModifier with the adjusted count
        return List.of(
                CountPlacementModifier.of(adjustedAttempts),
                SquarePlacementModifier.of(),
                SurfaceWaterDepthFilterPlacementModifier.of(treeType == TreeType.MANGROVE_CLUSTERS ? 3 : 0),
                PlacedFeatures.OCEAN_FLOOR_HEIGHTMAP,
                BiomePlacementModifier.of()
        );
    }

    /**
     * Get base attempt count for TreeType
     */
    private static int getBaseAttempts(TreeType treeType) {
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

    // === UTILITY METHODS ===

    private static RegistryKey<PlacedFeature> registerKey(String name) {
        return RegistryKey.of(RegistryKeys.PLACED_FEATURE, new Identifier(Terradyne.MOD_ID, name));
    }

    private static void register(Registerable<PlacedFeature> context,
                               RegistryKey<PlacedFeature> key,
                               RegistryEntry<ConfiguredFeature<?, ?>> configuration,
                               List<PlacementModifier> modifiers) {
        context.register(key, new PlacedFeature(configuration, List.copyOf(modifiers)));
    }
}