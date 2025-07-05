package net.starlight.terradyne.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.starlight.terradyne.planet.biology.BiomeFeatureComponents.TreeType;
import net.starlight.terradyne.planet.biology.VegetationPalette;
import net.starlight.terradyne.planet.features.ModConfiguredFeatures;
import net.starlight.terradyne.planet.features.ModPlacedFeatures;
import net.starlight.terradyne.planet.physics.CrustComposition;

import java.util.concurrent.CompletableFuture;

/**
 * Data generation providers for Terradyne features
 * FIXED: Complete implementation that actually generates JSON files for all tree features
 */
public class FeatureDataProviders {

    /**
     * Data provider for configured features
     * FIXED: Now properly generates configured_feature JSON files for all tree types
     */
    public static class ConfiguredFeatureDataProvider extends FabricDynamicRegistryProvider {

        public ConfiguredFeatureDataProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
            super(output, registriesFuture);
        }

        @Override
        protected void configure(RegistryWrapper.WrapperLookup registries, Entries entries) {
            System.out.println("=== GENERATING CONFIGURED TREE FEATURES ===");

            // Use default vegetation palette for base tree configurations
            // (Individual planets will modify these via their biome component systems)
            VegetationPalette defaultPalette = VegetationPalette.TEMPERATE_DECIDUOUS;

            // Generate configured features for each tree type
            entries.add(ModConfiguredFeatures.LARGE_DECIDUOUS_TREE,
                    ModConfiguredFeatures.createTreeConfig(TreeType.LARGE_DECIDUOUS, defaultPalette));

            entries.add(ModConfiguredFeatures.LARGE_CONIFEROUS_TREE,
                    ModConfiguredFeatures.createTreeConfig(TreeType.LARGE_CONIFEROUS, defaultPalette));

            entries.add(ModConfiguredFeatures.SMALL_DECIDUOUS_TREE,
                    ModConfiguredFeatures.createTreeConfig(TreeType.SMALL_DECIDUOUS, defaultPalette));

            entries.add(ModConfiguredFeatures.SMALL_CONIFEROUS_TREE,
                    ModConfiguredFeatures.createTreeConfig(TreeType.SMALL_CONIFEROUS, defaultPalette));

            entries.add(ModConfiguredFeatures.SPARSE_DECIDUOUS_TREE,
                    ModConfiguredFeatures.createTreeConfig(TreeType.SPARSE_DECIDUOUS, defaultPalette));

            entries.add(ModConfiguredFeatures.SPARSE_CONIFEROUS_TREE,
                    ModConfiguredFeatures.createTreeConfig(TreeType.SPARSE_CONIFEROUS, defaultPalette));

            entries.add(ModConfiguredFeatures.TROPICAL_CANOPY_TREE,
                    ModConfiguredFeatures.createTreeConfig(TreeType.TROPICAL_CANOPY, defaultPalette));

            entries.add(ModConfiguredFeatures.MANGROVE_CLUSTER_TREE,
                    ModConfiguredFeatures.createTreeConfig(TreeType.MANGROVE_CLUSTERS, defaultPalette));

            entries.add(ModConfiguredFeatures.THERMOPHILIC_GROVE_TREE,
                    ModConfiguredFeatures.createTreeConfig(TreeType.THERMOPHILIC_GROVES, defaultPalette));

            entries.add(ModConfiguredFeatures.CARBONACEOUS_STRUCTURE_TREE,
                    ModConfiguredFeatures.createTreeConfig(TreeType.CARBONACEOUS_STRUCTURES, defaultPalette));

            entries.add(ModConfiguredFeatures.CRYSTALLINE_GROWTH_TREE,
                    ModConfiguredFeatures.createTreeConfig(TreeType.CRYSTALLINE_GROWTHS, defaultPalette));

            System.out.println("✅ Generated " + TreeType.values().length + " configured tree features");
        }

        @Override
        public String getName() {
            return "Terradyne Configured Tree Features";
        }
    }

    /**
     * Data provider for placed features
     * FIXED: Now properly generates placed_feature JSON files for all tree types
     */
    public static class PlacedFeatureDataProvider extends FabricDynamicRegistryProvider {

        public PlacedFeatureDataProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
            super(output, registriesFuture);
        }

        @Override
        protected void configure(RegistryWrapper.WrapperLookup registries, Entries entries) {
            System.out.println("=== GENERATING PLACED TREE FEATURES ===");

            // Get configured feature registry lookup
            var configuredFeatureLookup = registries.getWrapperOrThrow(net.minecraft.registry.RegistryKeys.CONFIGURED_FEATURE);

            // Generate placed features for each tree type
            entries.add(ModPlacedFeatures.LARGE_DECIDUOUS_TREE_PLACED,
                    new PlacedFeature(
                            configuredFeatureLookup.getOrThrow(ModConfiguredFeatures.LARGE_DECIDUOUS_TREE),
                            ModPlacedFeatures.createTreePlacement(TreeType.LARGE_DECIDUOUS)
                    ));

            entries.add(ModPlacedFeatures.LARGE_CONIFEROUS_TREE_PLACED,
                    new PlacedFeature(
                            configuredFeatureLookup.getOrThrow(ModConfiguredFeatures.LARGE_CONIFEROUS_TREE),
                            ModPlacedFeatures.createTreePlacement(TreeType.LARGE_CONIFEROUS)
                    ));

            entries.add(ModPlacedFeatures.SMALL_DECIDUOUS_TREE_PLACED,
                    new PlacedFeature(
                            configuredFeatureLookup.getOrThrow(ModConfiguredFeatures.SMALL_DECIDUOUS_TREE),
                            ModPlacedFeatures.createTreePlacement(TreeType.SMALL_DECIDUOUS)
                    ));

            entries.add(ModPlacedFeatures.SMALL_CONIFEROUS_TREE_PLACED,
                    new PlacedFeature(
                            configuredFeatureLookup.getOrThrow(ModConfiguredFeatures.SMALL_CONIFEROUS_TREE),
                            ModPlacedFeatures.createTreePlacement(TreeType.SMALL_CONIFEROUS)
                    ));

            entries.add(ModPlacedFeatures.SPARSE_DECIDUOUS_TREE_PLACED,
                    new PlacedFeature(
                            configuredFeatureLookup.getOrThrow(ModConfiguredFeatures.SPARSE_DECIDUOUS_TREE),
                            ModPlacedFeatures.createTreePlacement(TreeType.SPARSE_DECIDUOUS)
                    ));

            entries.add(ModPlacedFeatures.SPARSE_CONIFEROUS_TREE_PLACED,
                    new PlacedFeature(
                            configuredFeatureLookup.getOrThrow(ModConfiguredFeatures.SPARSE_CONIFEROUS_TREE),
                            ModPlacedFeatures.createTreePlacement(TreeType.SPARSE_CONIFEROUS)
                    ));

            entries.add(ModPlacedFeatures.TROPICAL_CANOPY_TREE_PLACED,
                    new PlacedFeature(
                            configuredFeatureLookup.getOrThrow(ModConfiguredFeatures.TROPICAL_CANOPY_TREE),
                            ModPlacedFeatures.createTreePlacement(TreeType.TROPICAL_CANOPY)
                    ));

            entries.add(ModPlacedFeatures.MANGROVE_CLUSTER_TREE_PLACED,
                    new PlacedFeature(
                            configuredFeatureLookup.getOrThrow(ModConfiguredFeatures.MANGROVE_CLUSTER_TREE),
                            ModPlacedFeatures.createTreePlacement(TreeType.MANGROVE_CLUSTERS)
                    ));

            entries.add(ModPlacedFeatures.THERMOPHILIC_GROVE_TREE_PLACED,
                    new PlacedFeature(
                            configuredFeatureLookup.getOrThrow(ModConfiguredFeatures.THERMOPHILIC_GROVE_TREE),
                            ModPlacedFeatures.createTreePlacement(TreeType.THERMOPHILIC_GROVES)
                    ));

            entries.add(ModPlacedFeatures.CARBONACEOUS_STRUCTURE_TREE_PLACED,
                    new PlacedFeature(
                            configuredFeatureLookup.getOrThrow(ModConfiguredFeatures.CARBONACEOUS_STRUCTURE_TREE),
                            ModPlacedFeatures.createTreePlacement(TreeType.CARBONACEOUS_STRUCTURES)
                    ));

            entries.add(ModPlacedFeatures.CRYSTALLINE_GROWTH_TREE_PLACED,
                    new PlacedFeature(
                            configuredFeatureLookup.getOrThrow(ModConfiguredFeatures.CRYSTALLINE_GROWTH_TREE),
                            ModPlacedFeatures.createTreePlacement(TreeType.CRYSTALLINE_GROWTHS)
                    ));

            System.out.println("✅ Generated " + TreeType.values().length + " placed tree features");
        }

        @Override
        public String getName() {
            return "Terradyne Placed Tree Features";
        }
    }

    /**
     * Validation provider to check our feature system
     * This helps debug what's being generated
     */
    public static class FeatureValidationProvider extends FabricDynamicRegistryProvider {

        public FeatureValidationProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
            super(output, registriesFuture);
        }

        @Override
        protected void configure(RegistryWrapper.WrapperLookup registries, Entries entries) {
            // This provider doesn't add anything, just validates our setup
            System.out.println("=== TERRADYNE FEATURE VALIDATION ===");

            // Validate tree types
            System.out.println("Tree Types: " + TreeType.values().length);
            for (TreeType treeType : TreeType.values()) {
                System.out.println("  - " + treeType.getDisplayName());
            }

            // Validate vegetation palettes
            System.out.println("Vegetation Palettes: " + VegetationPalette.values().length);
            for (VegetationPalette palette : VegetationPalette.values()) {
                System.out.println("  - " + palette.getDisplayName() + " (vegetation: " + palette.hasVegetation() + ")");
            }

            // Validate crust composition mappings
            System.out.println("Crust Composition Mappings:");
            for (CrustComposition crust : CrustComposition.values()) {
                VegetationPalette palette = VegetationPalette.fromCrustComposition(crust);
                System.out.println("  - " + crust.getDisplayName() + " -> " + palette.getDisplayName());
            }

            System.out.println("✅ Feature validation complete");
        }

        @Override
        public String getName() {
            return "Terradyne Feature Validation";
        }
    }
}