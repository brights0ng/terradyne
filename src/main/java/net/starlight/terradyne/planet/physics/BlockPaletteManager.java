package net.starlight.terradyne.planet.physics;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages geological block palettes for different crust compositions
 * Each composition gets a 5-block palette representing different geological layers
 */
public class BlockPaletteManager {

    /**
     * Geological block palette for a crust composition
     */
    public static class BlockPalette {
        public final Block baseRock;      // Most common throughout crust
        public final Block upperRock;     // Layered, more common at higher elevations  
        public final Block featureRock;   // Sporadic intrusions
        public final Block looseRock;     // Wind/water erosion areas
        public final Block organicRock;   // High habitability surface areas

        public BlockPalette(Block baseRock, Block upperRock, Block featureRock, 
                           Block looseRock, Block organicRock) {
            this.baseRock = baseRock;
            this.upperRock = upperRock;
            this.featureRock = featureRock;
            this.looseRock = looseRock;
            this.organicRock = organicRock;
        }

        /**
         * Get appropriate block for given conditions
         */
        public Block getBlockForConditions(double elevation, double erosion, double habitability) {
            // High habitability areas get organic rock
            if (habitability > 0.6 && elevation > 0.3) {
                return organicRock;
            }
            
            // High erosion areas get loose rock
            if (erosion > 0.5) {
                return looseRock;
            }
            
            // High elevation gets upper rock
            if (elevation > 0.7) {
                return upperRock;
            }
            
            // Feature rock appears sporadically (would use noise in actual generation)
            // For now, this is just the interface
            
            // Default to base rock
            return baseRock;
        }

        @Override
        public String toString() {
            return String.format("BlockPalette{base=%s, upper=%s, feature=%s, loose=%s, organic=%s}",
                    baseRock.getTranslationKey(), upperRock.getTranslationKey(), 
                    featureRock.getTranslationKey(), looseRock.getTranslationKey(), 
                    organicRock.getTranslationKey());
        }
    }

    // Static palette cache
    private static final Map<CrustComposition, BlockPalette> PALETTE_CACHE = new HashMap<>();

    static {
        initializePalettes();
    }

    /**
     * Get block palette for a crust composition
     */
    public static BlockPalette getPalette(CrustComposition composition) {
        return PALETTE_CACHE.get(composition);
    }

    /**
     * Initialize all hardcoded block palettes
     */
    private static void initializePalettes() {
        
        // SILICATE_RICH - Standard rocky planets
        PALETTE_CACHE.put(CrustComposition.SILICATE, new BlockPalette(
                Blocks.STONE,           // baseRock: Common stone
                Blocks.ANDESITE,        // upperRock: Layered volcanic
                Blocks.GRANITE,         // featureRock: Intrusive igneous
                Blocks.GRAVEL,          // looseRock: Weathered fragments
                Blocks.GRASS_BLOCK            // organicRock: Soil formation
        ));

        // IRON_RICH - Metallic worlds
        PALETTE_CACHE.put(CrustComposition.FERROUS, new BlockPalette(
                Blocks.GRANITE,      // baseRock: Metallic iron
                Blocks.RED_SANDSTONE,       // upperRock: Dense dark rock
                Blocks.RAW_IRON_BLOCK,  // featureRock: Ore veins
                Blocks.RED_SAND,      // looseRock: Dark fragments
                Blocks.ROOTED_DIRT     // organicRock: Hardy soil
        ));

        // CARBON_RICH - Diamond/graphite worlds
        PALETTE_CACHE.put(CrustComposition.CARBONACEOUS, new BlockPalette(
                Blocks.COAL_BLOCK,      // baseRock: Carbon deposits
                Blocks.BLACKSTONE,      // upperRock: Carbonized rock
                Blocks.DIAMOND_BLOCK,   // featureRock: Diamond formations
                Blocks.BLACK_CONCRETE_POWDER, // looseRock: Carbon dust
                Blocks.PODZOL          // organicRock: Carbon-rich soil
        ));

        // ICE_RICH - Frozen worlds
        PALETTE_CACHE.put(CrustComposition.REGOLITHIC, new BlockPalette(
                Blocks.COBBLESTONE,             // baseRock: Rocky rubble
                Blocks.COBBLED_DEEPSLATE,      // upperRock: Rocky rubble
                Blocks.ANDESITE,        // featureRock: Rocky rubble
                Blocks.GRAVEL,      // looseRock: Rocky rubble
                Blocks.AIR            // organicRock: No possible organic life
        ));

        // SULFUR_RICH - Volcanic sulfur worlds
        PALETTE_CACHE.put(CrustComposition.SULFURIC, new BlockPalette(
                Blocks.YELLOW_TERRACOTTA,    // baseRock: Sulfur deposits
                Blocks.ORANGE_TERRACOTTA,    // upperRock: Oxidized sulfur
                Blocks.RAW_GOLD_BLOCK,           // featureRock: Sulfur crystals (gold approximation)
                Blocks.YELLOW_CONCRETE_POWDER, // looseRock: Sulfur dust
                Blocks.COARSE_DIRT          // organicRock: Sulfur-enriched soil
        ));

        // SALT_RICH - Evaporite worlds
        PALETTE_CACHE.put(CrustComposition.HALLIDE, new BlockPalette(
                Blocks.WHITE_TERRACOTTA,     // baseRock: Salt deposits
                Blocks.CALCITE,              // upperRock: Crystalline layers
                Blocks.QUARTZ_BLOCK,         // featureRock: Salt crystals
                Blocks.WHITE_CONCRETE_POWDER, // looseRock: Salt dust
                Blocks.SAND                 // organicRock: Sandy soil
        ));

        // BASALTIC - Volcanic worlds
        PALETTE_CACHE.put(CrustComposition.BASALTIC, new BlockPalette(
                Blocks.BASALT,          // baseRock: Volcanic basalt
                Blocks.SMOOTH_BASALT,   // upperRock: Cooled lava flows
                Blocks.MAGMA_BLOCK,     // featureRock: Active volcanism
                Blocks.GRAY_CONCRETE_POWDER, // looseRock: Volcanic ash
                Blocks.ROOTED_DIRT     // organicRock: Volcanic soil
        ));

        // GRANITE - Continental crust worlds
        PALETTE_CACHE.put(CrustComposition.METALLIC, new BlockPalette(
                Blocks.IRON_BLOCK,         // baseRock: Granite bedrock
                Blocks.RAW_COPPER_BLOCK, // upperRock: Weathered granite
                Blocks.RAW_IRON_BLOCK,    // featureRock: Quartz veins
                Blocks.LIGHT_GRAY_CONCRETE_POWDER,     // looseRock: Granite fragments
                Blocks.GRASS_BLOCK     // organicRock: Rich grassland
        ));

        // SANDSTONE - Sedimentary worlds
        PALETTE_CACHE.put(CrustComposition.HADEAN, new BlockPalette(
                Blocks.LAVA,       // baseRock: Sedimentary layers
                Blocks.MAGMA_BLOCK, // upperRock: Compressed sediment
                Blocks.BLACKSTONE, // featureRock: Weathered formations
                Blocks.NETHERRACK,            // looseRock: Loose sediment
                Blocks.AIR            // organicRock: No possible organic rock
        ));
    }

    /**
     * Get all available palettes (for debugging/testing)
     */
    public static Map<CrustComposition, BlockPalette> getAllPalettes() {
        return Map.copyOf(PALETTE_CACHE);
    }

    /**
     * Validate that all crust compositions have palettes
     */
    public static boolean validatePalettes() {
        for (CrustComposition composition : CrustComposition.values()) {
            if (!PALETTE_CACHE.containsKey(composition)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get palette summary for debugging
     */
    public static String getPaletteSummary(CrustComposition composition) {
        BlockPalette palette = getPalette(composition);
        if (palette == null) {
            return "No palette found for " + composition.getDisplayName();
        }
        
        return String.format("%s: %s (base) → %s (upper) → %s (organic)",
                composition.getDisplayName(),
                palette.baseRock.getTranslationKey(),
                palette.upperRock.getTranslationKey(),
                palette.organicRock.getTranslationKey());
    }
}