package net.starlight.terradyne.planet.biome;

import net.minecraft.block.Blocks;
import net.starlight.terradyne.planet.PlanetType;

import java.util.List;

/**
 * Generic biome type for physics-based planets
 * For testing tectonic plate system with smooth transitions
 */
public enum PhysicsBiomeType implements IBiomeType {;
//
//    TECTONIC_TEST("tectonic_test") {
//        @Override
//        public List<PassConfiguration> getGenerationPasses() {
//            return List.of(
//                    // Single foundation pass that combines all octaves
//                    new PassConfiguration(PhysicsFoundationPass.class, 0)
//                            .withParameter("blockType", Blocks.GRASS_BLOCK.getDefaultState())
//                            .withParameter("bedrockBlock", Blocks.BEDROCK.getDefaultState())
//                            .withParameter("seaLevel", 64)
//                            .withParameter("visualizePlates", false) // Set to true to see plate boundaries
//                            // Continental noise parameters
//                            .withParameter("continentalScale", 40.0)
//                            .withParameter("oceanicDepth", -30.0)
//                            .withParameter("continentalHeight", 20.0)
//                            // Plate elevation parameters
//                            .withParameter("plateInfluence", 0.7)
//                            .withParameter("plateBlendDistance", 500.0)
//                            // Volatility parameters
//                            .withParameter("mountainScale", 40.0)
//                            .withParameter("valleyDepth", 20.0)
//                            .withParameter("volatilityNoiseInfluence", 0.6)
//            );
//        }
//    },
//
//    DEBUG_PLATES("debug_plates") {
//        @Override
//        public List<PassConfiguration> getGenerationPasses() {
//            return List.of(
//                    // Debug pass that shows plate boundaries clearly
//                    new PassConfiguration(PhysicsFoundationPass.class, 0)
//                            .withParameter("blockType", Blocks.GRASS_BLOCK.getDefaultState())
//                            .withParameter("bedrockBlock", Blocks.BEDROCK.getDefaultState())
//                            .withParameter("seaLevel", 64)
//                            .withParameter("visualizePlates", true) // Shows plate types with different blocks
//                            // Reduced scales for better plate visibility
//                            .withParameter("continentalScale", 20.0)
//                            .withParameter("oceanicDepth", -15.0)
//                            .withParameter("continentalHeight", 10.0)
//                            // Stronger plate influence for debugging
//                            .withParameter("plateInfluence", 0.9)
//                            .withParameter("plateBlendDistance", 300.0)
//                            // Reduced volatility for clearer plate viewing
//                            .withParameter("mountainScale", 20.0)
//                            .withParameter("valleyDepth", 10.0)
//                            .withParameter("volatilityNoiseInfluence", 0.4)
//            );
//        }
//    };

    private final String name;

    PhysicsBiomeType(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public PlanetType getPlanetType() {
        // Physics biomes work with any planet type
        return PlanetType.ROCKY; // Default
    }

//    @Override
//    public List<OctaveConfiguration> getOctaveConfigurations() {
//        // We use passes now, but need to provide this for compatibility
//        return List.of(
//                new OctaveConfiguration(TectonicFoundationOctave.class)
//        );
//    }
}