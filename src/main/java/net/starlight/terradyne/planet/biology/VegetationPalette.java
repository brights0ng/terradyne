package net.starlight.terradyne.planet.biology;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.starlight.terradyne.planet.physics.AtmosphereComposition;
import net.starlight.terradyne.planet.biology.BiomeFeatureComponents.TreeType;

/**
 * Defines vegetation block palettes based on planetary atmosphere composition
 * Each palette represents plant life adapted to specific atmospheric conditions
 * UPDATED: Now uses atmospheric composition instead of crust composition
 */
public enum VegetationPalette {

    // === OXYGEN-RICH ATMOSPHERE (Earth-like) ===
    OXYGEN_DECIDUOUS("Oxygen Deciduous", "Standard photosynthetic broad-leaved trees",
            Blocks.OAK_LOG, Blocks.OAK_LEAVES, Blocks.BIRCH_LOG, Blocks.BIRCH_LEAVES),

    OXYGEN_CONIFEROUS("Oxygen Coniferous", "Standard photosynthetic needle-leaved trees",
            Blocks.SPRUCE_LOG, Blocks.SPRUCE_LEAVES, Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_LEAVES),

    // === CARBON DIOXIDE ATMOSPHERE (Greenhouse world) ===
    GREENHOUSE_DECIDUOUS("Greenhouse Deciduous", "Heat-adapted C4 photosynthesis trees",
            Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_LEAVES, Blocks.ACACIA_LOG, Blocks.ACACIA_LEAVES),

    GREENHOUSE_CONIFEROUS("Greenhouse Coniferous", "Heat-resistant waxy needle trees",
            Blocks.ACACIA_LOG, Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_LEAVES),

    // === NITROGEN-RICH ATMOSPHERE (Nitrogen fixation specialists) ===
    NITROGEN_DECIDUOUS("Nitrogen Deciduous", "Nitrogen-cycling efficient broad-leaved trees",
            Blocks.BIRCH_LOG, Blocks.AZALEA_LEAVES, Blocks.OAK_LOG, Blocks.AZALEA_LEAVES),

    NITROGEN_CONIFEROUS("Nitrogen Coniferous", "Blue-green nitrogen-fixing needle trees",
            Blocks.SPRUCE_LOG, Blocks.AZALEA_LEAVES, Blocks.BIRCH_LOG, Blocks.AZALEA_LEAVES),

    // === METHANE ATMOSPHERE (Alien methanotrophic organisms) ===
    METHANE_DECIDUOUS("Methane Deciduous", "Alien methanotrophic broad structures",
            Blocks.WARPED_STEM, Blocks.WARPED_WART_BLOCK, Blocks.CRIMSON_STEM, Blocks.NETHER_WART_BLOCK),

    METHANE_CONIFEROUS("Methane Coniferous", "Alien methanotrophic spire structures",
            Blocks.WARPED_STEM, Blocks.WARPED_WART_BLOCK, Blocks.WARPED_STEM, Blocks.NETHER_WART_BLOCK),

    // === WATER VAPOR ATMOSPHERE (Steam jungle specialists) ===
    STEAM_DECIDUOUS("Steam Deciduous", "Hyper-humid steam-adapted broad-leaved trees",
            Blocks.JUNGLE_LOG, Blocks.JUNGLE_LEAVES, Blocks.ACACIA_LOG, Blocks.ACACIA_LEAVES),

    STEAM_CONIFEROUS("Steam Coniferous", "Moisture-loving steam-adapted needle trees",
            Blocks.DARK_OAK_LOG, Blocks.JUNGLE_LEAVES, Blocks.JUNGLE_LOG, Blocks.JUNGLE_LEAVES),

    // === HYDROGEN SULFIDE ATMOSPHERE (Chemosynthetic vegetation) ===
    CHEMOSYNTHETIC_DECIDUOUS("Chemosynthetic Deciduous", "Sulfur-utilizing chemosynthetic structures",
            Blocks.ACACIA_LOG, Blocks.YELLOW_STAINED_GLASS, Blocks.STRIPPED_ACACIA_LOG, Blocks.BROWN_STAINED_GLASS),

    CHEMOSYNTHETIC_CONIFEROUS("Chemosynthetic Coniferous", "Sulfur-processing spire organisms",
            Blocks.STRIPPED_OAK_LOG, Blocks.YELLOW_STAINED_GLASS, Blocks.ACACIA_LOG, Blocks.ORANGE_STAINED_GLASS),

    // === NOBLE GAS ATMOSPHERE (Crystalline mineral growths) ===
    CRYSTALLINE_DECIDUOUS("Crystalline Deciduous", "Mineral-based crystalline broad formations",
            Blocks.QUARTZ_PILLAR, Blocks.WHITE_STAINED_GLASS, Blocks.CALCITE, Blocks.LIGHT_GRAY_STAINED_GLASS),

    CRYSTALLINE_CONIFEROUS("Crystalline Coniferous", "Mineral-based crystalline spire formations",
            Blocks.QUARTZ_BLOCK, Blocks.WHITE_STAINED_GLASS, Blocks.QUARTZ_PILLAR, Blocks.GRAY_STAINED_GLASS),

    // === TRACE ATMOSPHERE (Extremophile survivors) ===
    EXTREMOPHILE_SURVIVORS("Extremophile Survivors", "Barely-living hardy organisms in thin atmosphere",
            Blocks.DEAD_BUSH, null, Blocks.DEAD_BUSH, null),

    // === NO VEGETATION POSSIBLE ===
    ATMOSPHERIC_BARREN("Atmospheric Barren", "No vegetation possible in this atmosphere",
            null, null, null, null);

    private final String displayName;
    private final String description;
    private final Block deciduousWood;
    private final Block deciduousLeaves;
    private final Block coniferousWood;
    private final Block coniferousLeaves;

    VegetationPalette(String displayName, String description,
                      Block deciduousWood, Block deciduousLeaves,
                      Block coniferousWood, Block coniferousLeaves) {
        this.displayName = displayName;
        this.description = description;
        this.deciduousWood = deciduousWood;
        this.deciduousLeaves = deciduousLeaves;
        this.coniferousWood = coniferousWood;
        this.coniferousLeaves = coniferousLeaves;
    }

    // === GETTERS ===
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public Block getDeciduousWood() { return deciduousWood; }
    public Block getDeciduousLeaves() { return deciduousLeaves; }
    public Block getConiferousWood() { return coniferousWood; }
    public Block getConiferousLeaves() { return coniferousLeaves; }

    /**
     * Check if this palette supports any vegetation
     */
    public boolean hasVegetation() {
        return deciduousWood != null || coniferousWood != null;
    }

    /**
     * Check if palette supports deciduous vegetation
     */
    public boolean hasDeciduousVegetation() {
        return deciduousWood != null && deciduousLeaves != null;
    }

    /**
     * Check if palette supports coniferous vegetation
     */
    public boolean hasConiferousVegetation() {
        return coniferousWood != null && coniferousLeaves != null;
    }

    /**
     * Map atmosphere composition to appropriate vegetation palette
     * Returns the base deciduous variant - use getAppropriateVariant() for tree-specific selection
     */
    public static VegetationPalette fromAtmosphereComposition(AtmosphereComposition atmosphereComposition) {
        return switch (atmosphereComposition) {
            case OXYGEN_RICH -> OXYGEN_DECIDUOUS;           // Standard Earth-like photosynthesis
            case CARBON_DIOXIDE -> GREENHOUSE_DECIDUOUS;    // Heat-adapted greenhouse plants
            case NITROGEN_RICH -> NITROGEN_DECIDUOUS;       // Nitrogen-fixing specialists
            case METHANE -> METHANE_DECIDUOUS;              // Alien methanotrophic organisms
            case WATER_VAPOR_RICH -> STEAM_DECIDUOUS;       // Hyper-humid steam jungle
            case HYDROGEN_SULFIDE -> CHEMOSYNTHETIC_DECIDUOUS; // Sulfur-utilizing chemosynthesis
            case NOBLE_GAS_MIXTURE -> CRYSTALLINE_DECIDUOUS;   // Mineral-based crystal growths
            case TRACE_ATMOSPHERE -> EXTREMOPHILE_SURVIVORS;   // Barely-living extremophiles
            case VACUUM -> ATMOSPHERIC_BARREN;              // No atmosphere = no life
        };
    }

    /**
     * Get the coniferous variant of an atmospheric palette
     */
    public static VegetationPalette getConiferousVariant(VegetationPalette deciduousPalette) {
        return switch (deciduousPalette) {
            case OXYGEN_DECIDUOUS -> OXYGEN_CONIFEROUS;
            case GREENHOUSE_DECIDUOUS -> GREENHOUSE_CONIFEROUS;
            case NITROGEN_DECIDUOUS -> NITROGEN_CONIFEROUS;
            case METHANE_DECIDUOUS -> METHANE_CONIFEROUS;
            case STEAM_DECIDUOUS -> STEAM_CONIFEROUS;
            case CHEMOSYNTHETIC_DECIDUOUS -> CHEMOSYNTHETIC_CONIFEROUS;
            case CRYSTALLINE_DECIDUOUS -> CRYSTALLINE_CONIFEROUS;
            case EXTREMOPHILE_SURVIVORS -> EXTREMOPHILE_SURVIVORS; // No coniferous variant
            default -> deciduousPalette; // No coniferous variant available
        };
    }

    /**
     * Get appropriate vegetation palette variant based on tree type
     */
    public static VegetationPalette getAppropriateVariant(VegetationPalette basePalette, TreeType treeType) {
        if (treeType == null) return basePalette;

        return switch (treeType) {
            case LARGE_CONIFEROUS, SMALL_CONIFEROUS, SPARSE_CONIFEROUS ->
                    getConiferousVariant(basePalette);
            default -> basePalette; // Use deciduous for all other tree types
        };
    }

    /**
     * Get grass color that matches this vegetation type with tree-specific variance
     */
    public int getGrassColor(TreeType treeType) {
        // Base color depends on atmospheric type
        int baseColor = switch (this) {
            case OXYGEN_DECIDUOUS, OXYGEN_CONIFEROUS -> 0x91BD59; // Standard green
            case GREENHOUSE_DECIDUOUS, GREENHOUSE_CONIFEROUS -> 0x6B8E23; // Darker olive green (heat-adapted)
            case NITROGEN_DECIDUOUS, NITROGEN_CONIFEROUS -> 0x4F7F4F; // Blue-green (nitrogen fixation)
            case METHANE_DECIDUOUS, METHANE_CONIFEROUS -> 0x40E0D0; // Cyan/turquoise (alien methane life)
            case STEAM_DECIDUOUS, STEAM_CONIFEROUS -> 0x228B22; // Forest green (steam jungle)
            case CHEMOSYNTHETIC_DECIDUOUS, CHEMOSYNTHETIC_CONIFEROUS -> 0xFFD700; // Yellow-gold (sulfur-based)
            case CRYSTALLINE_DECIDUOUS, CRYSTALLINE_CONIFEROUS -> 0xE6E6FA; // Lavender (mineral crystal)
            case EXTREMOPHILE_SURVIVORS -> 0xA0522D; // Sienna brown (hardy survivors)
            case ATMOSPHERIC_BARREN -> 0xBEA17C; // Desert tan (no vegetation)
        };

        // Apply slight variations based on tree type for diversity
        if (treeType != null) {
            return switch (treeType) {
                case LARGE_DECIDUOUS, SMALL_DECIDUOUS -> baseColor; // Standard color
                case LARGE_CONIFEROUS, SMALL_CONIFEROUS -> darkenColor(baseColor, 0.15f); // Slightly darker
                case SPARSE_DECIDUOUS, SPARSE_CONIFEROUS -> lightenColor(baseColor, 0.1f); // Slightly lighter
                case TROPICAL_CANOPY -> saturateColor(baseColor, 0.2f); // More vibrant
                case MANGROVE_CLUSTERS -> darkenColor(baseColor, 0.2f); // Wetland dark
                case THERMOPHILIC_GROVES -> adjustHue(baseColor, 10); // Slight yellow shift
                case CARBONACEOUS_STRUCTURES -> darkenColor(baseColor, 0.3f); // Much darker for carbon
                case CRYSTALLINE_GROWTHS -> lightenColor(baseColor, 0.3f); // Much lighter for crystals
                default -> baseColor;
            };
        }

        return baseColor;
    }

    /**
     * Get foliage color that matches grass (slightly different for variety)
     */
    public int getFoliageColor(TreeType treeType) {
        int grassColor = getGrassColor(treeType);
        return darkenColor(grassColor, 0.1f); // Foliage slightly darker than grass
    }

    // === COLOR UTILITY METHODS ===

    private static int darkenColor(int color, float factor) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        r = (int) (r * (1.0f - factor));
        g = (int) (g * (1.0f - factor));
        b = (int) (b * (1.0f - factor));

        return (r << 16) | (g << 8) | b;
    }

    private static int lightenColor(int color, float factor) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        r = Math.min(255, (int) (r + (255 - r) * factor));
        g = Math.min(255, (int) (g + (255 - g) * factor));
        b = Math.min(255, (int) (b + (255 - b) * factor));

        return (r << 16) | (g << 8) | b;
    }

    private static int saturateColor(int color, float factor) {
        // Simple saturation increase by making colors more vibrant
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        // Find the dominant color channel and enhance it
        if (g > r && g > b) { // Green dominant
            g = Math.min(255, (int) (g * (1.0f + factor)));
        }

        return (r << 16) | (g << 8) | b;
    }

    private static int adjustHue(int color, int hueShift) {
        // Simple hue adjustment by shifting RGB values
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        // Shift towards yellow (increase red slightly)
        r = Math.min(255, r + hueShift);

        return (r << 16) | (g << 8) | b;
    }

    /**
     * Get climate-appropriate variation based on temperature and humidity
     * (This method is now mainly for extreme climate adjustments)
     */
    public VegetationPalette getClimateVariation(double temperature, double humidity) {
        if (!hasVegetation()) {
            return this; // Barren worlds don't change
        }

        // Extreme conditions can override atmospheric vegetation
        if (temperature < -30 || humidity < 0.05) {
            return ATMOSPHERIC_BARREN; // Too extreme for any vegetation
        }

        return this; // Climate variation now handled by biome selection
    }

    @Override
    public String toString() {
        return String.format("VegetationPalette{%s: vegetation=%s, type=%s}",
                displayName, hasVegetation() ? "YES" : "NONE",
                this.name().contains("CONIFEROUS") ? "CONIFEROUS" :
                        this.name().contains("DECIDUOUS") ? "DECIDUOUS" : "SPECIAL");
    }
}