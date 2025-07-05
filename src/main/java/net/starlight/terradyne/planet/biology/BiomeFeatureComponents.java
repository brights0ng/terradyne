package net.starlight.terradyne.planet.biology;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;

/**
 * Component-based biome feature system for scalable planetary biome generation
 * Each biome gets a combination of these components based on its characteristics
 */
public class BiomeFeatureComponents {

    /**
     * Large vegetation types (trees)
     */
    public enum TreeType {
        LARGE_DECIDUOUS("Large Deciduous", "Tall broad-leaved trees forming canopy"),
        LARGE_CONIFEROUS("Large Coniferous", "Tall needle-leaved evergreen trees"),
        SMALL_DECIDUOUS("Small Deciduous", "Shorter broad-leaved trees and shrubs"),
        SMALL_CONIFEROUS("Small Coniferous", "Shorter evergreen trees"),
        SPARSE_DECIDUOUS("Sparse Deciduous", "Widely scattered deciduous trees"),
        SPARSE_CONIFEROUS("Sparse Coniferous", "Widely scattered coniferous trees"),
        TROPICAL_CANOPY("Tropical Canopy", "Dense, tall tropical rainforest trees"),
        MANGROVE_CLUSTERS("Mangrove Clusters", "Water-edge adapted root systems"),
        THERMOPHILIC_GROVES("Thermophilic Groves", "Heat-resistant tree communities"),
        CARBONACEOUS_STRUCTURES("Carbonaceous Structures", "Carbon-based tree-like growths"),
        CRYSTALLINE_GROWTHS("Crystalline Growths", "Mineral-based tree-like formations");

        private final String displayName;
        private final String description;

        TreeType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }

    /**
     * Small vegetation types (bushes, shrubs)
     */
    public enum BushType {
        FLOWERING_BUSHES("Flowering Bushes", "Small flowering shrubs and bushes"),
        BERRY_BUSHES("Berry Bushes", "Fruit-bearing low shrubs"),
        THORNY_SHRUBS("Thorny Shrubs", "Defensive spiny vegetation"),
        SUCCULENT_CLUSTERS("Succulent Clusters", "Water-storing desert plants"),
        FERN_UNDERGROWTH("Fern Undergrowth", "Low-growing forest floor vegetation"),
        MOSS_BEDS("Moss Beds", "Carpet-forming moisture-loving plants"),
        LICHEN_PATCHES("Lichen Patches", "Symbiotic crust-forming organisms"),
        SALT_TOLERANT_SHRUBS("Salt-Tolerant Shrubs", "Halophytic salt-resistant bushes"),
        CHEMOSYNTHETIC_MATS("Chemosynthetic Mats", "Chemical-energy utilizing plant mats"),
        XEROPHYTIC_CACTI("Xerophytic Cacti", "Extreme drought-adapted spine plants"),
        VOLCANIC_SUCCULENTS("Volcanic Succulents", "Heat and mineral-tolerant plants");

        private final String displayName;
        private final String description;

        BushType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }

    /**
     * Wild crop types (naturally occurring food plants)
     */
    public enum CropType {
        WILD_ROOTS("Wild Roots", "Wild potato-like tubers", Blocks.POTATOES),
        WILD_GRAINS("Wild Grains", "Wild wheat-like grasses", Blocks.WHEAT),
        WILD_LEGUMES("Wild Legumes", "Wild bean and pea plants", Blocks.CARROTS),
        WILD_BERRIES("Wild Berries", "Wild berry bushes", Blocks.SWEET_BERRY_BUSH),
        WILD_HERBS("Wild Herbs", "Edible and medicinal wild plants", Blocks.GRASS),
        WILD_FUNGI("Wild Fungi", "Edible mushrooms and fungal growth", Blocks.RED_MUSHROOM),
        EXTREME_ALGAE("Extreme Algae", "Extremophile algae in harsh conditions", Blocks.KELP),
        MINERAL_ACCUMULATORS("Mineral Accumulators", "Plants that concentrate minerals", Blocks.BEETROOTS),
        THERMOPHILIC_CROPS("Thermophilic Crops", "Heat-loving edible plants", Blocks.NETHER_WART);

        private final String displayName;
        private final String description;
        private final Block blockType;

        CropType(String displayName, String description, Block blockType) {
            this.displayName = displayName;
            this.description = description;
            this.blockType = blockType;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public Block getBlockType() { return blockType; }
    }

    /**
     * Terrain feature types (landscape formations)
     */
    public enum TerrainFeatureType {
        SAND_DUNES("Sand Dunes", "Wind-formed sand accumulations"),
        ROCK_FORMATIONS("Rock Formations", "Exposed bedrock outcroppings"),
        MESA_PLATEAUS("Mesa Plateaus", "Flat-topped elevated terrain"),
        VOLCANIC_VENTS("Volcanic Vents", "Geothermal surface expressions"),
        SALT_FLATS("Salt Flats", "Evaporated mineral deposits"),
        BOULDER_FIELDS("Boulder Fields", "Scattered large rock debris"),
        THERMAL_SPRINGS("Thermal Springs", "Geothermally heated water sources"),
        SHALLOW_PONDS("Shallow Ponds", "Small water collection areas"),
        EROSION_CHANNELS("Erosion Channels", "Water or wind-carved channels"),
        CRYSTAL_FORMATIONS("Crystal Formations", "Mineral crystal accumulations"),
        LAVA_TUBES("Lava Tubes", "Cooled volcanic conduits"),
        IMPACT_CRATERS("Impact Craters", "Meteorite impact depressions");

        private final String displayName;
        private final String description;

        TerrainFeatureType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }

    /**
     * Ground cover types (surface material)
     */
    public enum GroundCoverType {
        ORGANIC_SOIL("Organic Soil", "Rich biological soil layer", Blocks.GRASS_BLOCK),
        FOREST_LITTER("Forest Litter", "Decomposing organic debris", Blocks.PODZOL),
        SANDY_SUBSTRATE("Sandy Substrate", "Loose sandy surface", Blocks.SAND),
        ROCKY_DEBRIS("Rocky Debris", "Fragmented rock particles", Blocks.GRAVEL),
        CLAY_DEPOSITS("Clay Deposits", "Fine sedimentary particles", Blocks.CLAY),
        SALT_CRUST("Salt Crust", "Crystallized salt surface", Blocks.WHITE_CONCRETE_POWDER),
        VOLCANIC_ASH("Volcanic Ash", "Fine volcanic particulates", Blocks.GRAY_CONCRETE_POWDER),
        REGOLITH_LAYER("Regolith Layer", "Broken rock and debris", Blocks.COARSE_DIRT),
        MINERAL_CRUST("Mineral Crust", "Hardened mineral surface", Blocks.STONE),
        CARBON_DUST("Carbon Dust", "Fine carbon particulates", Blocks.BLACK_CONCRETE_POWDER),
        CRYSTALLINE_POWDER("Crystalline Powder", "Ground crystal particles", Blocks.QUARTZ_BLOCK),
        PERMAFROST("Permafrost", "Permanently frozen ground", Blocks.PACKED_ICE);

        private final String displayName;
        private final String description;
        private final Block blockType;

        GroundCoverType(String displayName, String description, Block blockType) {
            this.displayName = displayName;
            this.description = description;
            this.blockType = blockType;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public Block getBlockType() { return blockType; }
    }

    // === COMPONENT CONTAINER ===
    private final TreeType largeVegetation;
    private final BushType smallVegetation;
    private final CropType cropType;
    private final TerrainFeatureType terrainFeature;
    private final GroundCoverType groundCover;

    /**
     * Create biome feature components
     */
    public BiomeFeatureComponents(TreeType largeVegetation, BushType smallVegetation, 
                                 CropType cropType, TerrainFeatureType terrainFeature,
                                 GroundCoverType groundCover) {
        this.largeVegetation = largeVegetation;
        this.smallVegetation = smallVegetation;
        this.cropType = cropType;
        this.terrainFeature = terrainFeature;
        this.groundCover = groundCover;
    }

    // === GETTERS ===
    public TreeType getLargeVegetation() { return largeVegetation; }
    public BushType getSmallVegetation() { return smallVegetation; }
    public CropType getCropType() { return cropType; }
    public TerrainFeatureType getTerrainFeature() { return terrainFeature; }
    public GroundCoverType getGroundCover() { return groundCover; }

    /**
     * Check if biome has any vegetation
     */
    public boolean hasVegetation() {
        return largeVegetation != null || smallVegetation != null;
    }

    /**
     * Check if biome has terrain features
     */
    public boolean hasTerrainFeatures() {
        return terrainFeature != null;
    }

    /**
     * Check if biome has ground cover
     */
    public boolean hasGroundCover() {
        return groundCover != null;
    }

    /**
     * Get component density based on environmental conditions
     */
    public static class DensityCalculator {
        
        /**
         * Calculate tree density (0.0-1.0) based on environmental factors
         */
        public static double calculateTreeDensity(double habitability, double humidity, double temperature) {
            // Base density from habitability
            double baseDensity = Math.max(0, habitability * 0.6);
            
            // Humidity bonus (trees need water)
            double humidityBonus = Math.max(0, humidity * 0.3);
            
            // Temperature factor (optimal around 15-25°C)
            double tempFactor = 0.1;
            if (temperature >= 5 && temperature <= 35) {
                // Bell curve around 20°C
                double tempOptimal = 20.0;
                double tempDeviation = Math.abs(temperature - tempOptimal) / 15.0;
                tempFactor = 0.1 * (1.0 - tempDeviation);
            }
            
            return Math.max(0.0, Math.min(1.0, baseDensity + humidityBonus + tempFactor));
        }
        
        /**
         * Calculate bush density (0.0-1.0) based on environmental factors
         */
        public static double calculateBushDensity(double habitability, double humidity, double temperature) {
            // Bushes are hardier than trees
            double baseDensity = Math.max(0, habitability * 0.4);
            double humidityBonus = Math.max(0, humidity * 0.2);
            
            // More temperature tolerant than trees
            double tempFactor = 0.1;
            if (temperature >= -5 && temperature <= 45) {
                tempFactor = 0.15;
            }
            
            return Math.max(0.0, Math.min(1.0, baseDensity + humidityBonus + tempFactor));
        }
        
        /**
         * Calculate crop density (0.0-1.0) based on environmental factors
         */
        public static double calculateCropDensity(double habitability, double humidity, double temperature) {
            // Wild crops need good conditions but not as dense as trees
            double density = calculateTreeDensity(habitability, humidity, temperature) * 0.3;
            return Math.max(0.0, Math.min(0.5, density)); // Cap at 50% density
        }
    }

    @Override
    public String toString() {
        return String.format("BiomeComponents{trees=%s, bushes=%s, crops=%s, terrain=%s, ground=%s}",
                largeVegetation != null ? largeVegetation.displayName : "none",
                smallVegetation != null ? smallVegetation.displayName : "none", 
                cropType != null ? cropType.displayName : "none",
                terrainFeature != null ? terrainFeature.displayName : "none",
                groundCover != null ? groundCover.displayName : "none");
    }
}