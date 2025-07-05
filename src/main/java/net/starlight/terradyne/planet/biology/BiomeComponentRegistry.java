package net.starlight.terradyne.planet.biology;

import net.minecraft.registry.RegistryKey;
import net.minecraft.world.biome.Biome;
import net.starlight.terradyne.planet.biome.ModBiomes;
import net.starlight.terradyne.planet.biology.BiomeFeatureComponents.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry mapping each Terradyne biome to its feature components
 * FIXED: Corrected tree assignments - no trees in oceans, volcanic areas, or extreme biomes
 */
public class BiomeComponentRegistry {

    private static final Map<RegistryKey<Biome>, BiomeFeatureComponents> BIOME_COMPONENTS = new HashMap<>();

    static {
        initializeBiomeComponents();
    }

    /**
     * Get feature components for a biome
     */
    public static BiomeFeatureComponents getComponents(RegistryKey<Biome> biomeKey) {
        return BIOME_COMPONENTS.get(biomeKey);
    }

    /**
     * Check if biome has registered components
     */
    public static boolean hasComponents(RegistryKey<Biome> biomeKey) {
        return BIOME_COMPONENTS.containsKey(biomeKey);
    }

    /**
     * Initialize all biome component mappings
     * FIXED: Corrected tree assignments for realistic biome behavior
     */
    private static void initializeBiomeComponents() {

        // === WATER BIOMES ===
        // FIXED: NO TREES IN ANY OCEAN BIOME - trees don't grow underwater!

        register(ModBiomes.FROZEN_OCEAN, null, null, null, null, GroundCoverType.PERMAFROST);
        register(ModBiomes.FRIGID_OCEAN, null, null, null, null, GroundCoverType.ROCKY_DEBRIS);
        register(ModBiomes.DEAD_OCEAN, null, null, null, null, GroundCoverType.MINERAL_CRUST);
        register(ModBiomes.OCEAN, null, null, CropType.EXTREME_ALGAE, null, GroundCoverType.SANDY_SUBSTRATE);
        register(ModBiomes.WARM_OCEAN, null, null, CropType.EXTREME_ALGAE, TerrainFeatureType.SHALLOW_PONDS, GroundCoverType.ORGANIC_SOIL);
        register(ModBiomes.CORAL_OCEAN, null, BushType.SUCCULENT_CLUSTERS, CropType.WILD_BERRIES, TerrainFeatureType.SHALLOW_PONDS, GroundCoverType.ORGANIC_SOIL);
        register(ModBiomes.TROPICAL_OCEAN, null, BushType.FLOWERING_BUSHES, CropType.WILD_BERRIES, TerrainFeatureType.SHALLOW_PONDS, GroundCoverType.ORGANIC_SOIL); // FIXED: Removed mangrove clusters
        register(ModBiomes.BOILING_OCEAN, null, null, null, TerrainFeatureType.THERMAL_SPRINGS, GroundCoverType.VOLCANIC_ASH);

        // === MOUNTAIN BIOMES ===
        // FIXED: Only some mountain biomes should have trees based on elevation and conditions

        register(ModBiomes.FROZEN_PEAKS, null, null, null, TerrainFeatureType.ROCK_FORMATIONS, GroundCoverType.PERMAFROST); // FIXED: No trees on frozen peaks
        register(ModBiomes.MOUNTAIN_FOOTHILLS, TreeType.SPARSE_CONIFEROUS, BushType.LICHEN_PATCHES, null, TerrainFeatureType.BOULDER_FIELDS, GroundCoverType.ROCKY_DEBRIS);
        register(ModBiomes.MOUNTAIN_PEAKS, null, BushType.LICHEN_PATCHES, null, TerrainFeatureType.ROCK_FORMATIONS, GroundCoverType.ROCKY_DEBRIS); // FIXED: No trees on high peaks
        register(ModBiomes.ALPINE_PEAKS, TreeType.SMALL_CONIFEROUS, BushType.MOSS_BEDS, CropType.WILD_HERBS, TerrainFeatureType.BOULDER_FIELDS, GroundCoverType.FOREST_LITTER);
        register(ModBiomes.VOLCANIC_WASTELAND, null, null, null, TerrainFeatureType.VOLCANIC_VENTS, GroundCoverType.VOLCANIC_ASH); // FIXED: No trees in wasteland
        register(ModBiomes.VOLCANIC_MOUNTAINS, null, BushType.VOLCANIC_SUCCULENTS, null, TerrainFeatureType.VOLCANIC_VENTS, GroundCoverType.VOLCANIC_ASH); // FIXED: No trees, only hardy succulents

        // === HIGHLAND BIOMES ===
        // Moderate elevation, moderate vegetation

        register(ModBiomes.BARREN_HIGHLANDS, null, BushType.LICHEN_PATCHES, null, TerrainFeatureType.EROSION_CHANNELS, GroundCoverType.ROCKY_DEBRIS); // FIXED: Barren = no trees
        register(ModBiomes.WINDSWEPT_HILLS, TreeType.SPARSE_DECIDUOUS, BushType.THORNY_SHRUBS, null, TerrainFeatureType.EROSION_CHANNELS, GroundCoverType.REGOLITH_LAYER);
        register(ModBiomes.ROLLING_HILLS, TreeType.SMALL_DECIDUOUS, BushType.FLOWERING_BUSHES, CropType.WILD_GRAINS, null, GroundCoverType.ORGANIC_SOIL);
        register(ModBiomes.HIGHLAND_TUNDRA, TreeType.SPARSE_CONIFEROUS, BushType.MOSS_BEDS, CropType.WILD_HERBS, null, GroundCoverType.PERMAFROST);
        register(ModBiomes.FORESTED_HILLS, TreeType.LARGE_DECIDUOUS, BushType.FERN_UNDERGROWTH, CropType.WILD_BERRIES, null, GroundCoverType.FOREST_LITTER);
        register(ModBiomes.TROPICAL_HIGHLANDS, TreeType.LARGE_DECIDUOUS, BushType.FLOWERING_BUSHES, CropType.WILD_BERRIES, TerrainFeatureType.SHALLOW_PONDS, GroundCoverType.ORGANIC_SOIL);

        // === HOSTILE CONTINENTAL BIOMES ===
        // FIXED: Most hostile biomes should have NO trees due to extreme conditions

        register(ModBiomes.FROZEN_WASTELAND, null, null, null, TerrainFeatureType.IMPACT_CRATERS, GroundCoverType.PERMAFROST); // FIXED: No trees in wasteland
        register(ModBiomes.ROCKY_DESERT, null, BushType.LICHEN_PATCHES, null, TerrainFeatureType.BOULDER_FIELDS, GroundCoverType.ROCKY_DEBRIS); // FIXED: No trees in rocky desert
        register(ModBiomes.SCORCHED_PLAINS, null, null, null, TerrainFeatureType.SALT_FLATS, GroundCoverType.MINERAL_CRUST); // FIXED: No trees in scorched plains
        register(ModBiomes.WINDSWEPT_TUNDRA, null, BushType.LICHEN_PATCHES, CropType.EXTREME_ALGAE, TerrainFeatureType.EROSION_CHANNELS, GroundCoverType.PERMAFROST); // FIXED: No trees in tundra
        register(ModBiomes.SANDY_DESERT, null, BushType.XEROPHYTIC_CACTI, null, TerrainFeatureType.SAND_DUNES, GroundCoverType.SANDY_SUBSTRATE); // FIXED: No trees in desert
        register(ModBiomes.DESERT_MESA, TreeType.SPARSE_DECIDUOUS, BushType.XEROPHYTIC_CACTI, null, TerrainFeatureType.MESA_PLATEAUS, GroundCoverType.CLAY_DEPOSITS); // Keep sparse trees - mesa can support some
        register(ModBiomes.DUST_BOWL, null, BushType.THORNY_SHRUBS, null, TerrainFeatureType.EROSION_CHANNELS, GroundCoverType.REGOLITH_LAYER); // FIXED: No trees in dust bowl

        // === MARGINAL CONTINENTAL BIOMES ===
        // Limited but present vegetation, moderate conditions

        register(ModBiomes.COLD_STEPPES, TreeType.SPARSE_CONIFEROUS, BushType.THORNY_SHRUBS, CropType.WILD_GRAINS, null, GroundCoverType.REGOLITH_LAYER);
        register(ModBiomes.TUNDRA, TreeType.SPARSE_CONIFEROUS, BushType.MOSS_BEDS, CropType.WILD_HERBS, null, GroundCoverType.PERMAFROST);
        register(ModBiomes.BOREAL_PLAINS, TreeType.SMALL_CONIFEROUS, BushType.BERRY_BUSHES, CropType.WILD_BERRIES, null, GroundCoverType.FOREST_LITTER);
        register(ModBiomes.DRY_STEPPES, TreeType.SPARSE_DECIDUOUS, BushType.THORNY_SHRUBS, CropType.WILD_GRAINS, null, GroundCoverType.SANDY_SUBSTRATE);
        register(ModBiomes.TEMPERATE_STEPPES, TreeType.SMALL_DECIDUOUS, BushType.FLOWERING_BUSHES, CropType.WILD_GRAINS, null, GroundCoverType.ORGANIC_SOIL);
        register(ModBiomes.MEADOWS, TreeType.SMALL_DECIDUOUS, BushType.FLOWERING_BUSHES, CropType.WILD_HERBS, TerrainFeatureType.SHALLOW_PONDS, GroundCoverType.ORGANIC_SOIL);
        register(ModBiomes.SAVANNA, TreeType.SPARSE_DECIDUOUS, BushType.THORNY_SHRUBS, CropType.WILD_GRAINS, null, GroundCoverType.SANDY_SUBSTRATE);
        register(ModBiomes.TROPICAL_GRASSLAND, TreeType.SMALL_DECIDUOUS, BushType.FLOWERING_BUSHES, CropType.WILD_LEGUMES, null, GroundCoverType.ORGANIC_SOIL);

        // === THRIVING CONTINENTAL BIOMES ===
        // Rich vegetation, diverse features based on climate zones

        // Cold Zone
        register(ModBiomes.SNOWY_PLAINS, TreeType.SPARSE_CONIFEROUS, BushType.BERRY_BUSHES, CropType.WILD_ROOTS, null, GroundCoverType.PERMAFROST);
        register(ModBiomes.TAIGA, TreeType.LARGE_CONIFEROUS, BushType.BERRY_BUSHES, CropType.WILD_BERRIES, null, GroundCoverType.FOREST_LITTER);
        register(ModBiomes.SNOW_FOREST, TreeType.LARGE_CONIFEROUS, BushType.FERN_UNDERGROWTH, CropType.WILD_FUNGI, null, GroundCoverType.FOREST_LITTER);
        register(ModBiomes.ALPINE_MEADOWS, TreeType.SMALL_CONIFEROUS, BushType.FLOWERING_BUSHES, CropType.WILD_HERBS, TerrainFeatureType.SHALLOW_PONDS, GroundCoverType.ORGANIC_SOIL);

        // Temperate Zone
        register(ModBiomes.PLAINS, TreeType.SPARSE_DECIDUOUS, BushType.FLOWERING_BUSHES, CropType.WILD_GRAINS, null, GroundCoverType.ORGANIC_SOIL);
        register(ModBiomes.MIXED_PLAINS, TreeType.SMALL_DECIDUOUS, BushType.BERRY_BUSHES, CropType.WILD_GRAINS, TerrainFeatureType.SHALLOW_PONDS, GroundCoverType.ORGANIC_SOIL);
        register(ModBiomes.WETLANDS, TreeType.MANGROVE_CLUSTERS, BushType.MOSS_BEDS, CropType.WILD_HERBS, TerrainFeatureType.SHALLOW_PONDS, GroundCoverType.ORGANIC_SOIL);
        register(ModBiomes.OAK_FOREST, TreeType.LARGE_DECIDUOUS, BushType.FERN_UNDERGROWTH, CropType.WILD_BERRIES, null, GroundCoverType.FOREST_LITTER);
        register(ModBiomes.MIXED_FOREST, TreeType.LARGE_DECIDUOUS, BushType.FERN_UNDERGROWTH, CropType.WILD_FUNGI, null, GroundCoverType.FOREST_LITTER);
        register(ModBiomes.DENSE_FOREST, TreeType.LARGE_DECIDUOUS, BushType.FERN_UNDERGROWTH, CropType.WILD_FUNGI, TerrainFeatureType.SHALLOW_PONDS, GroundCoverType.FOREST_LITTER);
        register(ModBiomes.MOUNTAIN_FOREST, TreeType.LARGE_CONIFEROUS, BushType.MOSS_BEDS, CropType.WILD_BERRIES, TerrainFeatureType.BOULDER_FIELDS, GroundCoverType.FOREST_LITTER);

        // Warm Zone
        register(ModBiomes.HOT_SHRUBLAND, TreeType.SPARSE_DECIDUOUS, BushType.SUCCULENT_CLUSTERS, CropType.WILD_HERBS, null, GroundCoverType.SANDY_SUBSTRATE);
        register(ModBiomes.WINDY_STEPPES, TreeType.SPARSE_DECIDUOUS, BushType.THORNY_SHRUBS, CropType.WILD_GRAINS, TerrainFeatureType.EROSION_CHANNELS, GroundCoverType.REGOLITH_LAYER);
        register(ModBiomes.TEMPERATE_RAINFOREST, TreeType.LARGE_DECIDUOUS, BushType.FERN_UNDERGROWTH, CropType.WILD_FUNGI, TerrainFeatureType.SHALLOW_PONDS, GroundCoverType.FOREST_LITTER);
        register(ModBiomes.CLOUD_FOREST, TreeType.LARGE_DECIDUOUS, BushType.MOSS_BEDS, CropType.WILD_FUNGI, TerrainFeatureType.SHALLOW_PONDS, GroundCoverType.FOREST_LITTER);
        register(ModBiomes.JUNGLE, TreeType.TROPICAL_CANOPY, BushType.FLOWERING_BUSHES, CropType.WILD_BERRIES, TerrainFeatureType.SHALLOW_PONDS, GroundCoverType.ORGANIC_SOIL);
        register(ModBiomes.TROPICAL_RAINFOREST, TreeType.TROPICAL_CANOPY, BushType.FLOWERING_BUSHES, CropType.WILD_LEGUMES, TerrainFeatureType.SHALLOW_PONDS, GroundCoverType.ORGANIC_SOIL);

        // Hot Zone
        register(ModBiomes.HOT_DESERT, null, BushType.XEROPHYTIC_CACTI, null, TerrainFeatureType.SAND_DUNES, GroundCoverType.SANDY_SUBSTRATE); // FIXED: No trees in hot desert
        register(ModBiomes.TROPICAL_SWAMP, TreeType.MANGROVE_CLUSTERS, BushType.MOSS_BEDS, CropType.WILD_ROOTS, TerrainFeatureType.SHALLOW_PONDS, GroundCoverType.ORGANIC_SOIL);

        // === EXTREME BIOMES ===
        // FIXED: No trees in extreme biomes
        register(ModBiomes.EXTREME_FROZEN_WASTELAND, null, null, null, TerrainFeatureType.IMPACT_CRATERS, GroundCoverType.PERMAFROST);
        register(ModBiomes.MOLTEN_WASTELAND, null, null, null, TerrainFeatureType.LAVA_TUBES, GroundCoverType.VOLCANIC_ASH);

        // === DEBUG BIOME ===
        register(ModBiomes.DEBUG, TreeType.LARGE_DECIDUOUS, BushType.FLOWERING_BUSHES, CropType.WILD_GRAINS, TerrainFeatureType.SHALLOW_PONDS, GroundCoverType.ORGANIC_SOIL);
    }

    /**
     * Helper method to register biome components
     */
    private static void register(RegistryKey<Biome> biome, TreeType trees, BushType bushes,
                                 CropType crops, TerrainFeatureType terrain, GroundCoverType ground) {
        BiomeFeatureComponents components = new BiomeFeatureComponents(trees, bushes, crops, terrain, ground);
        BIOME_COMPONENTS.put(biome, components);
    }

    /**
     * Get statistics about component usage
     */
    public static String getRegistryStats() {
        int totalBiomes = BIOME_COMPONENTS.size();
        int biomesWithTrees = 0;
        int biomesWithBushes = 0;
        int biomesWithCrops = 0;
        int biomesWithTerrain = 0;
        int biomesWithGround = 0;

        for (BiomeFeatureComponents components : BIOME_COMPONENTS.values()) {
            if (components.getLargeVegetation() != null) biomesWithTrees++;
            if (components.getSmallVegetation() != null) biomesWithBushes++;
            if (components.getCropType() != null) biomesWithCrops++;
            if (components.getTerrainFeature() != null) biomesWithTerrain++;
            if (components.getGroundCover() != null) biomesWithGround++;
        }

        return String.format("BiomeComponentRegistry: %d biomes registered (FIXED VERSION)\n" +
                        "  Trees: %d biomes (%.1f%%) - FIXED: No trees in oceans/volcanic/extreme biomes\n" +
                        "  Bushes: %d biomes (%.1f%%)\n" +
                        "  Crops: %d biomes (%.1f%%)\n" +
                        "  Terrain: %d biomes (%.1f%%)\n" +
                        "  Ground: %d biomes (%.1f%%)",
                totalBiomes,
                biomesWithTrees, (biomesWithTrees * 100.0 / totalBiomes),
                biomesWithBushes, (biomesWithBushes * 100.0 / totalBiomes),
                biomesWithCrops, (biomesWithCrops * 100.0 / totalBiomes),
                biomesWithTerrain, (biomesWithTerrain * 100.0 / totalBiomes),
                biomesWithGround, (biomesWithGround * 100.0 / totalBiomes));
    }

    /**
     * Get all registered biomes
     */
    public static java.util.Set<RegistryKey<Biome>> getRegisteredBiomes() {
        return BIOME_COMPONENTS.keySet();
    }

    /**
     * Validate biome assignments for realism
     * NEW: Validation method to check for unrealistic tree assignments
     */
    public static String validateBiomeAssignments() {
        StringBuilder report = new StringBuilder();
        report.append("=== BIOME ASSIGNMENT VALIDATION ===\n");

        int validAssignments = 0;
        int issuesFound = 0;

        for (var entry : BIOME_COMPONENTS.entrySet()) {
            String biomeName = entry.getKey().getValue().getPath();
            BiomeFeatureComponents components = entry.getValue();

            // Check for problematic tree assignments
            boolean hasIssue = false;

            // Ocean biomes should not have trees
            if (biomeName.contains("ocean") && components.getLargeVegetation() != null) {
                report.append("❌ ").append(biomeName).append(" (ocean) has trees: ").append(components.getLargeVegetation().getDisplayName()).append("\n");
                hasIssue = true;
            }

            // Volcanic biomes should not have trees
            if (biomeName.contains("volcanic") && biomeName.contains("wasteland") && components.getLargeVegetation() != null) {
                report.append("❌ ").append(biomeName).append(" (volcanic wasteland) has trees: ").append(components.getLargeVegetation().getDisplayName()).append("\n");
                hasIssue = true;
            }

            // Frozen/extreme biomes should not have trees
            if ((biomeName.contains("frozen") || biomeName.contains("extreme")) &&
                    biomeName.contains("wasteland") && components.getLargeVegetation() != null) {
                report.append("❌ ").append(biomeName).append(" (extreme/frozen wasteland) has trees: ").append(components.getLargeVegetation().getDisplayName()).append("\n");
                hasIssue = true;
            }

            // Desert biomes should generally not have trees (except oasis-like ones)
            if (biomeName.contains("desert") && !biomeName.contains("mesa") && components.getLargeVegetation() != null) {
                report.append("⚠️  ").append(biomeName).append(" (desert) has trees: ").append(components.getLargeVegetation().getDisplayName()).append(" (may be unrealistic)\n");
            }

            if (hasIssue) {
                issuesFound++;
            } else {
                validAssignments++;
            }
        }

        report.append("\n=== VALIDATION SUMMARY ===\n");
        report.append("Valid assignments: ").append(validAssignments).append("\n");
        report.append("Issues found: ").append(issuesFound).append("\n");

        if (issuesFound == 0) {
            report.append("✅ All biome assignments appear realistic!\n");
        }

        return report.toString();
    }
}