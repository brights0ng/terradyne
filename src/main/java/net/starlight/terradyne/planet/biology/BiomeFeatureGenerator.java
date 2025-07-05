package net.starlight.terradyne.planet.biology;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.GenerationSettings;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.starlight.terradyne.Terradyne;
import net.starlight.terradyne.planet.biology.BiomeFeatureComponents.*;
import net.starlight.terradyne.planet.features.ModPlacedFeatures;
import net.starlight.terradyne.planet.physics.CrustComposition;

/**
 * Converts BiomeFeatureComponents into Minecraft GenerationSettings
 * FIXED: Now actually adds features to generation settings using data-generated placed features
 */
public class BiomeFeatureGenerator {

    /**
     * Create GenerationSettings from BiomeFeatureComponents and planet conditions
     * FIXED: Now accepts registries parameter to access placed features
     */
    public static GenerationSettings createGenerationSettings(
            RegistryKey<Biome> biomeKey,
            CrustComposition crustComposition,
            double temperature,
            double humidity,
            double habitability,
            RegistryWrapper.WrapperLookup registries) {

        GenerationSettings.Builder builder = new GenerationSettings.Builder();

        // Get components for this biome
        BiomeFeatureComponents components = BiomeComponentRegistry.getComponents(biomeKey);
        if (components == null) {
            Terradyne.LOGGER.warn("No components found for biome: {}, using minimal generation", biomeKey.getValue());
            return builder.build();
        }

        // Get vegetation palette from crust composition
        VegetationPalette palette = VegetationPalette.fromCrustComposition(crustComposition);

        // Apply climate variation to palette
        palette = palette.getClimateVariation(temperature, humidity);

        // Add tree features if vegetation is possible
        if (palette.hasVegetation() && components.getLargeVegetation() != null) {
            addTreeFeatures(builder, components.getLargeVegetation(), palette, temperature, humidity, habitability, registries);
        }

        // TODO: Add other component types (bushes, crops, terrain features, ground cover)
        // These will be implemented in future iterations

        Terradyne.LOGGER.debug("Generated features for biome {}: trees={}, palette={}",
                biomeKey.getValue().getPath(),
                components.getLargeVegetation() != null ?
                        components.getLargeVegetation().getDisplayName() : "none",
                palette.getDisplayName());

        return builder.build();
    }

    /**
     * FIXED: Add tree features to generation settings using data-generated placed features
     * Now actually adds features to the builder instead of just calculating them!
     */
    private static void addTreeFeatures(
            GenerationSettings.Builder builder,
            TreeType treeType,
            VegetationPalette palette,
            double temperature,
            double humidity,
            double habitability,
            RegistryWrapper.WrapperLookup registries) {

        try {
            // Calculate tree density based on environmental factors
            double density = BiomeFeatureComponents.DensityCalculator.calculateTreeDensity(habitability, humidity, temperature);

            // Skip if density is too low
            if (density < 0.05) {
                Terradyne.LOGGER.debug("Skipping tree generation for {}: density too low ({:.3f})",
                        treeType.getDisplayName(), density);
                return;
            }

            // Get the placed feature registry entry for this tree type
            RegistryKey<PlacedFeature> placedFeatureKey = getPlacedFeatureKey(treeType);

            if (placedFeatureKey == null) {
                Terradyne.LOGGER.warn("No placed feature key found for tree type: {}", treeType.getDisplayName());
                return;
            }

            // Get the placed feature registry
            var placedFeatureRegistry = registries.getWrapperOrThrow(RegistryKeys.PLACED_FEATURE);

            // Get the placed feature entry
            var placedFeatureEntry = placedFeatureRegistry.getOrThrow(placedFeatureKey);

            // FINALLY! Actually add the feature to the generation settings
            builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, placedFeatureEntry);

            Terradyne.LOGGER.info("✅ Added tree feature: {} (density: {:.2f}) for palette: {} to biome generation",
                    treeType.getDisplayName(), density, palette.getDisplayName());

        } catch (Exception e) {
            Terradyne.LOGGER.error("❌ Failed to add tree features for {}: {}", treeType.getDisplayName(), e.getMessage());
        }
    }

    /**
     * Map TreeType to corresponding placed feature registry key
     * FIXED: Now returns actual placed feature keys from ModPlacedFeatures
     */
    private static RegistryKey<PlacedFeature> getPlacedFeatureKey(TreeType treeType) {
        return switch (treeType) {
            case LARGE_DECIDUOUS -> ModPlacedFeatures.LARGE_DECIDUOUS_TREE_PLACED;
            case LARGE_CONIFEROUS -> ModPlacedFeatures.LARGE_CONIFEROUS_TREE_PLACED;
            case SMALL_DECIDUOUS -> ModPlacedFeatures.SMALL_DECIDUOUS_TREE_PLACED;
            case SMALL_CONIFEROUS -> ModPlacedFeatures.SMALL_CONIFEROUS_TREE_PLACED;
            case SPARSE_DECIDUOUS -> ModPlacedFeatures.SPARSE_DECIDUOUS_TREE_PLACED;
            case SPARSE_CONIFEROUS -> ModPlacedFeatures.SPARSE_CONIFEROUS_TREE_PLACED;
            case TROPICAL_CANOPY -> ModPlacedFeatures.TROPICAL_CANOPY_TREE_PLACED;
            case MANGROVE_CLUSTERS -> ModPlacedFeatures.MANGROVE_CLUSTER_TREE_PLACED;
            case THERMOPHILIC_GROVES -> ModPlacedFeatures.THERMOPHILIC_GROVE_TREE_PLACED;
            case CARBONACEOUS_STRUCTURES -> ModPlacedFeatures.CARBONACEOUS_STRUCTURE_TREE_PLACED;
            case CRYSTALLINE_GROWTHS -> ModPlacedFeatures.CRYSTALLINE_GROWTH_TREE_PLACED;
        };
    }

    /**
     * Calculate placement attempts based on tree type and density
     * (This is now mainly for informational purposes since placement is handled by placed features)
     */
    private static int calculatePlacementAttempts(TreeType treeType, double density) {
        int baseAttempts = switch (treeType) {
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

        return Math.max(1, (int) Math.round(baseAttempts * density));
    }

    /**
     * Calculate feature density multiplier for environmental conditions
     * This adjusts the base placement attempts based on physics conditions
     */
    public static double calculateFeatureDensityMultiplier(TreeType treeType, double temperature, double humidity, double habitability) {
        // Base density from physics
        double baseDensity = BiomeFeatureComponents.DensityCalculator.calculateTreeDensity(habitability, humidity, temperature);

        // Tree type specific adjustments
        double typeMultiplier = switch (treeType) {
            case SPARSE_DECIDUOUS, SPARSE_CONIFEROUS -> 0.3; // Intentionally sparse
            case LARGE_DECIDUOUS, LARGE_CONIFEROUS -> 1.0; // Standard density
            case SMALL_DECIDUOUS, SMALL_CONIFEROUS -> 1.5; // Can pack closer together
            case TROPICAL_CANOPY -> 0.8; // Large trees need space
            case MANGROVE_CLUSTERS -> 1.2; // Clustered growth pattern
            case THERMOPHILIC_GROVES -> 0.7; // Harsh conditions
            case CARBONACEOUS_STRUCTURES, CRYSTALLINE_GROWTHS -> 0.4; // Unusual formations
        };

        return baseDensity * typeMultiplier;
    }

    /**
     * Check if biome should have any vegetation based on crust composition
     */
    public static boolean canSupportVegetation(CrustComposition crustComposition) {
        VegetationPalette palette = VegetationPalette.fromCrustComposition(crustComposition);
        return palette.hasVegetation();
    }

    /**
     * Get description of what features will be generated for debugging
     */
    public static String getFeatureDescription(
            RegistryKey<Biome> biomeKey,
            CrustComposition crustComposition,
            double temperature,
            double humidity,
            double habitability) {

        BiomeFeatureComponents components = BiomeComponentRegistry.getComponents(biomeKey);
        if (components == null) {
            return "No components defined";
        }

        VegetationPalette palette = VegetationPalette.fromCrustComposition(crustComposition);
        palette = palette.getClimateVariation(temperature, humidity);

        StringBuilder description = new StringBuilder();
        description.append(String.format("Biome: %s\n", biomeKey.getValue().getPath()));
        description.append(String.format("Crust: %s -> Palette: %s\n",
                crustComposition.getDisplayName(), palette.getDisplayName()));
        description.append(String.format("Climate: T=%.1f°C, H=%.2f, Hab=%.2f\n",
                temperature, humidity, habitability));

        if (palette.hasVegetation() && components.getLargeVegetation() != null) {
            double density = calculateFeatureDensityMultiplier(components.getLargeVegetation(), temperature, humidity, habitability);
            description.append(String.format("Trees: %s (density: %.2f)\n",
                    components.getLargeVegetation().getDisplayName(), density));
        } else {
            description.append("Trees: None (no vegetation support)\n");
        }

        description.append(String.format("Components: %s", components.toString()));

        return description.toString();
    }

    /**
     * Create generation settings with planet-aware feature selection
     * FIXED: Now accepts registries parameter
     */
    public static GenerationSettings createPlanetAwareGenerationSettings(
            RegistryKey<Biome> biomeKey,
            CrustComposition crustComposition,
            double averageTemperature,
            double averageHumidity,
            double planetHabitability,
            RegistryWrapper.WrapperLookup registries) {

        return createGenerationSettings(biomeKey, crustComposition, averageTemperature, averageHumidity, planetHabitability, registries);
    }

    /**
     * Overloaded method for backwards compatibility during transition
     * This version falls back to minimal generation since it can't access registries
     */
    public static GenerationSettings createGenerationSettings(
            RegistryKey<Biome> biomeKey,
            CrustComposition crustComposition,
            double temperature,
            double humidity,
            double habitability) {

        Terradyne.LOGGER.warn("BiomeFeatureGenerator called without registries - falling back to minimal generation for biome: {}",
                biomeKey.getValue());
        return createMinimalGeneration();
    }

    /**
     * Simplified generation settings for biomes without components
     * Fallback when component system fails
     */
    public static GenerationSettings createMinimalGeneration() {
        GenerationSettings.Builder builder = new GenerationSettings.Builder();
        return builder.build();
    }
}