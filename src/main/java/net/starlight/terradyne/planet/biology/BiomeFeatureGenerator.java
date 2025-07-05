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
import net.starlight.terradyne.planet.physics.AtmosphereComposition;

/**
 * Converts BiomeFeatureComponents into Minecraft GenerationSettings
 * UPDATED: Now uses atmospheric composition instead of crust composition for vegetation
 */
public class BiomeFeatureGenerator {

    /**
     * Create GenerationSettings from BiomeFeatureComponents and planet conditions
     * UPDATED: Now uses atmospheric composition for vegetation selection
     */
    public static GenerationSettings createGenerationSettings(
            RegistryKey<Biome> biomeKey,
            AtmosphereComposition atmosphereComposition,
            double temperature,
            double humidity,
            double habitability,
            double atmosphericDensity,
            RegistryWrapper.WrapperLookup registries) {

        GenerationSettings.Builder builder = new GenerationSettings.Builder();

        // Get components for this biome
        BiomeFeatureComponents components = BiomeComponentRegistry.getComponents(biomeKey);
        if (components == null) {
            Terradyne.LOGGER.warn("No components found for biome: {}, using minimal generation", biomeKey.getValue());
            return builder.build();
        }

        // Get vegetation palette from atmosphere composition
        VegetationPalette basePalette = VegetationPalette.fromAtmosphereComposition(atmosphereComposition);

        // Choose appropriate variant based on tree type
        VegetationPalette palette = components.getLargeVegetation() != null ?
                VegetationPalette.getAppropriateVariant(basePalette, components.getLargeVegetation()) : basePalette;

        // Add tree features if vegetation is possible
        if (palette.hasVegetation() && components.getLargeVegetation() != null) {
            addTreeFeatures(builder, components.getLargeVegetation(), palette, temperature, humidity,
                    habitability, atmosphericDensity, registries);
        }

        // TODO: Add other component types (bushes, crops, terrain features, ground cover)
        // These will be implemented in future iterations

        Terradyne.LOGGER.debug("Generated features for biome {}: trees={}, atmosphere={}, palette={}",
                biomeKey.getValue().getPath(),
                components.getLargeVegetation() != null ?
                        components.getLargeVegetation().getDisplayName() : "none",
                atmosphereComposition.getDisplayName(),
                palette.getDisplayName());

        return builder.build();
    }

    /**
     * Add tree features to generation settings using data-generated placed features
     * UPDATED: Now includes atmospheric density in calculations
     */
    private static void addTreeFeatures(
            GenerationSettings.Builder builder,
            TreeType treeType,
            VegetationPalette palette,
            double temperature,
            double humidity,
            double habitability,
            double atmosphericDensity,
            RegistryWrapper.WrapperLookup registries) {

        try {
            // Calculate tree density with atmospheric density factor
            double baseDensity = BiomeFeatureComponents.DensityCalculator.calculateTreeDensity(habitability, humidity, temperature);

            // Apply atmospheric density multiplier (more atmosphere = more trees, but capped)
            double atmosphericMultiplier = Math.max(0.1, Math.min(2.0, atmosphericDensity)); // Clamp between 0.1x and 2.0x
            double finalDensity = baseDensity * atmosphericMultiplier;

            // Skip if density is too low
            if (finalDensity < 0.05) {
                Terradyne.LOGGER.debug("Skipping tree generation for {}: final density too low ({:.3f} = {:.3f} base × {:.3f} atmospheric)",
                        treeType.getDisplayName(), finalDensity, baseDensity, atmosphericMultiplier);
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

            // Add the feature to the generation settings
            builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, placedFeatureEntry);

            Terradyne.LOGGER.info("✅ Added tree feature: {} (density: {:.2f} = {:.2f} × {:.2f}) for palette: {} to biome generation",
                    treeType.getDisplayName(), finalDensity, baseDensity, atmosphericMultiplier, palette.getDisplayName());

        } catch (Exception e) {
            Terradyne.LOGGER.error("❌ Failed to add tree features for {}: {}", treeType.getDisplayName(), e.getMessage());
        }
    }

    /**
     * Map TreeType to corresponding placed feature registry key
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
     * Calculate feature density multiplier for environmental conditions
     * UPDATED: Now includes atmospheric density factor
     */
    public static double calculateFeatureDensityMultiplier(TreeType treeType, double temperature, double humidity,
                                                           double habitability, double atmosphericDensity) {
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

        // Apply atmospheric density factor
        double atmosphericMultiplier = Math.max(0.1, Math.min(2.0, atmosphericDensity));

        return baseDensity * typeMultiplier * atmosphericMultiplier;
    }

    /**
     * Check if biome should have any vegetation based on atmosphere composition
     * UPDATED: Now checks atmosphere instead of crust
     */
    public static boolean canSupportVegetation(AtmosphereComposition atmosphereComposition) {
        VegetationPalette palette = VegetationPalette.fromAtmosphereComposition(atmosphereComposition);
        return palette.hasVegetation();
    }

    /**
     * Get description of what features will be generated for debugging
     * UPDATED: Now uses atmospheric composition
     */
    public static String getFeatureDescription(
            RegistryKey<Biome> biomeKey,
            AtmosphereComposition atmosphereComposition,
            double temperature,
            double humidity,
            double habitability,
            double atmosphericDensity) {

        BiomeFeatureComponents components = BiomeComponentRegistry.getComponents(biomeKey);
        if (components == null) {
            return "No components defined";
        }

        VegetationPalette basePalette = VegetationPalette.fromAtmosphereComposition(atmosphereComposition);
        VegetationPalette palette = components.getLargeVegetation() != null ?
                VegetationPalette.getAppropriateVariant(basePalette, components.getLargeVegetation()) : basePalette;

        StringBuilder description = new StringBuilder();
        description.append(String.format("Biome: %s\n", biomeKey.getValue().getPath()));
        description.append(String.format("Atmosphere: %s -> Palette: %s\n",
                atmosphereComposition.getDisplayName(), palette.getDisplayName()));
        description.append(String.format("Climate: T=%.1f°C, H=%.2f, Hab=%.2f, AtmDens=%.2f\n",
                temperature, humidity, habitability, atmosphericDensity));

        if (palette.hasVegetation() && components.getLargeVegetation() != null) {
            double density = calculateFeatureDensityMultiplier(components.getLargeVegetation(),
                    temperature, humidity, habitability, atmosphericDensity);
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
     * UPDATED: Now uses atmospheric composition
     */
    public static GenerationSettings createPlanetAwareGenerationSettings(
            RegistryKey<Biome> biomeKey,
            AtmosphereComposition atmosphereComposition,
            double averageTemperature,
            double averageHumidity,
            double planetHabitability,
            double atmosphericDensity,
            RegistryWrapper.WrapperLookup registries) {

        return createGenerationSettings(biomeKey, atmosphereComposition, averageTemperature,
                averageHumidity, planetHabitability, atmosphericDensity, registries);
    }

    /**
     * Simplified generation settings for biomes without components
     * Fallback when component system fails
     */
    public static GenerationSettings createMinimalGeneration() {
        GenerationSettings.Builder builder = new GenerationSettings.Builder();
        return builder.build();
    }

    /**
     * Get grass and foliage colors for a biome based on atmospheric vegetation
     * UPDATED: New method for atmospheric color calculation
     */
    public static BiomeColors calculateBiomeColors(
            RegistryKey<Biome> biomeKey,
            AtmosphereComposition atmosphereComposition,
            double temperature,
            double humidity) {

        BiomeFeatureComponents components = BiomeComponentRegistry.getComponents(biomeKey);
        VegetationPalette palette = VegetationPalette.fromAtmosphereComposition(atmosphereComposition);

        TreeType treeType = components != null ? components.getLargeVegetation() : null;
        VegetationPalette finalPalette = treeType != null ?
                VegetationPalette.getAppropriateVariant(palette, treeType) : palette;

        int grassColor = finalPalette.getGrassColor(treeType);
        int foliageColor = finalPalette.getFoliageColor(treeType);

        return new BiomeColors(grassColor, foliageColor);
    }

    /**
     * Container for biome color data
     */
    public static class BiomeColors {
        public final int grassColor;
        public final int foliageColor;

        public BiomeColors(int grassColor, int foliageColor) {
            this.grassColor = grassColor;
            this.foliageColor = foliageColor;
        }
    }
}