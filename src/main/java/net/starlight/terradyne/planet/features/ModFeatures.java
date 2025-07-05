package net.starlight.terradyne.planet.features;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.TreeFeature;
import net.minecraft.world.gen.feature.TreeFeatureConfig;
import net.starlight.terradyne.Terradyne;

/**
 * Registry for custom Terradyne features
 * Starting with tree features, will expand to other component types
 */
public class ModFeatures {

    /**
     * Physics-based tree feature that adapts to planetary conditions
     * Uses vanilla TreeFeature but with dynamic configuration based on planet physics
     */
    public static final Feature<TreeFeatureConfig> PHYSICS_TREE = Registry.register(
            Registries.FEATURE,
            new Identifier(Terradyne.MOD_ID, "physics_tree"),
            new TreeFeature(TreeFeatureConfig.CODEC)
    );

    /**
     * Initialize all mod features (called from main mod class)
     */
    public static void initialize() {
        Terradyne.LOGGER.info("✓ Registering Terradyne features");
        Terradyne.LOGGER.info("  - physics_tree: Planetary tree generation with crust-based vegetation");
        
        // Future features will be registered here:
        // PHYSICS_BUSH, PHYSICS_CROP, TERRAIN_FORMATIONS, etc.
    }
}