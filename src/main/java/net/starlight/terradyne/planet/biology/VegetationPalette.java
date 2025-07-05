package net.starlight.terradyne.planet.biology;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.starlight.terradyne.planet.physics.CrustComposition;

/**
 * Defines vegetation block palettes based on planetary crust composition
 * Each palette represents the adapted plant life for specific geological environments
 */
public enum VegetationPalette {
    
    // === EARTH-LIKE SILICATE WORLDS ===
    TEMPERATE_DECIDUOUS("Temperate Deciduous", "Broad-leaved trees adapted to seasonal climates",
            Blocks.OAK_LOG, Blocks.OAK_LEAVES, Blocks.BIRCH_LOG, Blocks.BIRCH_LEAVES),
    
    BOREAL_CONIFEROUS("Boreal Coniferous", "Needle-leaved trees adapted to cold climates", 
            Blocks.SPRUCE_LOG, Blocks.SPRUCE_LEAVES, Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_LEAVES),
    
    TROPICAL_BROADLEAF("Tropical Broadleaf", "Dense, fast-growing tropical vegetation",
            Blocks.JUNGLE_LOG, Blocks.JUNGLE_LEAVES, Blocks.ACACIA_LOG, Blocks.ACACIA_LEAVES),
    
    // === IRON-RICH FERROUS WORLDS ===
    METALLIC_ADAPTED("Metallic-Adapted", "Vegetation adapted to iron-rich, oxidized soils",
            Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_LEAVES, Blocks.SPRUCE_LOG, Blocks.SPRUCE_LEAVES),
    
    // === CARBON-RICH CARBONACEOUS WORLDS ===
    GRAPHITIC_FLORA("Graphitic Flora", "Carbon-based vegetation with graphite-reinforced structures",
            Blocks.COAL_BLOCK, Blocks.DARK_OAK_LEAVES, Blocks.COAL_BLOCK, Blocks.DARK_OAK_LEAVES),
    
    CARBONIZED_GROWTH("Carbonized Growth", "Plant life utilizing abundant carbon for structural support",
            Blocks.BLACK_CONCRETE, Blocks.GRAY_STAINED_GLASS, Blocks.COAL_BLOCK, Blocks.BLACK_STAINED_GLASS),
    
    // === VOLCANIC BASALTIC WORLDS ===
    VOLCANIC_TOLERANT("Volcanic-Tolerant", "Hardy vegetation thriving on mineral-rich volcanic soils",
            Blocks.ACACIA_LOG, Blocks.ACACIA_LEAVES, Blocks.JUNGLE_LOG, Blocks.JUNGLE_LEAVES),
    
    THERMOPHILIC_TREES("Thermophilic Trees", "Heat-resistant trees adapted to recent volcanic activity",
            Blocks.MANGROVE_LOG, Blocks.MANGROVE_LEAVES, Blocks.ACACIA_LOG, Blocks.ACACIA_LEAVES),
    
    // === SULFUR-RICH WORLDS ===
    CHEMOSYNTHETIC_FLORA("Chemosynthetic Flora", "Vegetation utilizing sulfur compounds for energy",
            Blocks.STRIPPED_ACACIA_LOG, Blocks.BROWN_STAINED_GLASS, Blocks.DEAD_BUSH, null),
    
    SULFUR_ADAPTED("Sulfur-Adapted", "Acid-resistant plant life in sulfurous environments", 
            Blocks.STRIPPED_OAK_LOG, Blocks.YELLOW_STAINED_GLASS, Blocks.DEAD_BUSH, null),
    
    // === SALT-RICH HALIDE WORLDS ===
    HALOPHYTIC_VEGETATION("Halophytic Vegetation", "Salt-tolerant plants adapted to evaporite environments",
            Blocks.BIRCH_LOG, Blocks.BIRCH_LEAVES, Blocks.DEAD_BUSH, Blocks.BROWN_STAINED_GLASS),
    
    // === ARID/DESERT ADAPTED ===
    XEROPHYTIC_FLORA("Xerophytic Flora", "Drought-resistant vegetation with water-conserving adaptations",
            Blocks.ACACIA_LOG, Blocks.ACACIA_LEAVES, Blocks.DEAD_BUSH, null),
    
    // === IMPOSSIBLE VEGETATION ZONES ===
    REGOLITHIC_BARREN("Regolithic Barren", "No vegetation possible on unstable rubble surfaces",
            null, null, null, null),
    
    METALLIC_BARREN("Metallic Barren", "No organic life on pure metallic surfaces", 
            null, null, null, null),
    
    HADEAN_BARREN("Hadean Barren", "No vegetation possible in active volcanic hellscape",
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
     * Map crust composition to appropriate vegetation palette
     */
    public static VegetationPalette fromCrustComposition(CrustComposition crustComposition) {
        return switch (crustComposition) {
            case SILICATE -> TEMPERATE_DECIDUOUS;       // Standard Earth-like
            case FERROUS -> METALLIC_ADAPTED;           // Iron-rich, oxidized soils
            case CARBONACEOUS -> GRAPHITIC_FLORA;       // Carbon-based life forms
            case REGOLITHIC -> REGOLITHIC_BARREN;       // Unstable rubble - no vegetation
            case SULFURIC -> CHEMOSYNTHETIC_FLORA;      // Sulfur-utilizing plants
            case HALLIDE -> HALOPHYTIC_VEGETATION;      // Salt-tolerant plants
            case BASALTIC -> VOLCANIC_TOLERANT;         // Thrives on volcanic minerals
            case METALLIC -> METALLIC_BARREN;           // Pure metal - no organic life
            case HADEAN -> HADEAN_BARREN;               // Active volcanism - too extreme
        };
    }

    /**
     * Get climate-appropriate variation of this palette
     */
    public VegetationPalette getClimateVariation(double temperature, double humidity) {
        if (!hasVegetation()) {
            return this; // Barren worlds don't change
        }

        // Hot, dry conditions -> more xerophytic
        if (temperature > 35 && humidity < 0.3) {
            return XEROPHYTIC_FLORA;
        }

        // Cold conditions -> more coniferous-focused
        if (temperature < 5) {
            return switch (this) {
                case TEMPERATE_DECIDUOUS, TROPICAL_BROADLEAF -> BOREAL_CONIFEROUS;
                case VOLCANIC_TOLERANT -> THERMOPHILIC_TREES; // Cold volcanic world
                default -> this;
            };
        }

        // Hot, humid conditions -> more tropical
        if (temperature > 25 && humidity > 0.6) {
            return switch (this) {
                case TEMPERATE_DECIDUOUS, BOREAL_CONIFEROUS -> TROPICAL_BROADLEAF;
                case VOLCANIC_TOLERANT -> THERMOPHILIC_TREES; // Hot volcanic world
                default -> this;
            };
        }

        return this; // No climate modification needed
    }

    @Override
    public String toString() {
        return String.format("VegetationPalette{%s: vegetation=%s}", 
                displayName, hasVegetation() ? "YES" : "NONE");
    }
}