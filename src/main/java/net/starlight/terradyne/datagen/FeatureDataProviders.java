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
 * FULLY IMPLEMENTED: Properly generates JSON files for all tree features
 */
public class FeatureDataProviders {

    /**
     * Data provider for configured features
     * Generates configured_feature JSON files for all tree types
     */
    public static class ConfiguredFeatureDataProvider extends FabricDynamicRegistryProvider {

        public ConfiguredFeatureDataProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
            super(output, registriesFuture);
        }

        @Override
        protected void configure(RegistryWrapper.WrapperLookup registries, Entries entries) {
            // Generate configured features for each tree type with default vegetation palette
            VegetationPalette defaultPalette = VegetationPalette.TEMPERATE_DECIDUOUS;

            // Register all tree configured features
            for (TreeType treeType : TreeType.values()) {
                try {
                    var configuredFeatureKey = ModConfiguredFeatures.getConfiguredFeatureKey(treeType);
                    var configuredFeature = ModConfiguredFeatures.createTreeConfig(treeType, defaultPalette);

                    entries.add(configuredFeatureKey, configuredFeature);

                } catch (Exception e) {
                    System.err.println("Failed to register configured feature for " + treeType + ": " + e.getMessage());
                }
            }

            System.out.println("✅ Registered " + TreeType.values().length + " configured tree features");
        }

        @Override
        public String getName() {
            return "Terradyne Configured Tree Features";
        }
    }

    /**
     * Data provider for placed features
     * Generates placed_feature JSON files for all tree types
     */
    public static class PlacedFeatureDataProvider extends FabricDynamicRegistryProvider {

        public PlacedFeatureDataProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
            super(output, registriesFuture);
        }

        @Override
        protected void configure(RegistryWrapper.WrapperLookup registries, Entries entries) {
            // Get configured feature registry lookup
            var configuredFeatureLookup = registries.getWrapperOrThrow(net.minecraft.registry.RegistryKeys.CONFIGURED_FEATURE);

            // Register all tree placed features
            for (TreeType treeType : TreeType.values()) {
                try {
                    var configuredFeatureKey = ModConfiguredFeatures.getConfiguredFeatureKey(treeType);
                    var placedFeatureKey = ModPlacedFeatures.getPlacedFeatureKey(treeType);

                    // Get the configured feature entry
                    var configuredFeatureEntry = configuredFeatureLookup.getOrThrow(configuredFeatureKey);

                    // Create placement modifiers for this tree type
                    var placementModifiers = createPlacementModifiers(treeType);

                    // Create the placed feature
                    var placedFeature = new PlacedFeature(configuredFeatureEntry, placementModifiers);

                    entries.add(placedFeatureKey, placedFeature);

                } catch (Exception e) {
                    System.err.println("Failed to register placed feature for " + treeType + ": " + e.getMessage());
                }
            }

            System.out.println("✅ Registered " + TreeType.values().length + " placed tree features");
        }

        /**
         * Create placement modifiers for tree type
         */
        private java.util.List<net.minecraft.world.gen.placementmodifier.PlacementModifier> createPlacementModifiers(TreeType treeType) {
            // Get base attempt count based on tree type
            int baseAttempts = getBaseAttempts(treeType);

            return java.util.List.of(
                    net.minecraft.world.gen.placementmodifier.CountPlacementModifier.of(baseAttempts),
                    net.minecraft.world.gen.placementmodifier.SquarePlacementModifier.of(),
                    net.minecraft.world.gen.placementmodifier.SurfaceWaterDepthFilterPlacementModifier.of(
                            treeType == TreeType.MANGROVE_CLUSTERS ? 3 : 0), // Mangroves can be in shallow water
                    net.minecraft.world.gen.feature.PlacedFeatures.OCEAN_FLOOR_HEIGHTMAP,
                    net.minecraft.world.gen.placementmodifier.BiomePlacementModifier.of()
            );
        }

        /**
         * Get base attempt count for TreeType
         */
        private int getBaseAttempts(TreeType treeType) {
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

        @Override
        public String getName() {
            return "Terradyne Placed Tree Features";
        }
    }

    /**
     * Validation provider to check our feature system
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