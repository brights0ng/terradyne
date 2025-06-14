package net.starlight.terradyne.planet.biome;

import net.minecraft.block.Blocks;
import net.starlight.terradyne.planet.PlanetType;
import net.starlight.terradyne.planet.terrain.octave.OctaveConfiguration;
import net.starlight.terradyne.planet.terrain.octave.TectonicFoundationOctave;
import net.starlight.terradyne.planet.terrain.pass.*;

import java.util.List;

/**
 * Generic biome type for physics-based planets
 * For testing tectonic plate system
 */
public enum PhysicsBiomeType implements IBiomeType {
    
    TECTONIC_TEST("tectonic_test") {
        @Override
        public List<PassConfiguration> getGenerationPasses() {
            return List.of(
                // Foundation pass that combines tectonic plates with continental noise
                new PassConfiguration(PhysicsFoundationPass.class, 0)
                    .withParameter("blockType", Blocks.GRASS_BLOCK.getDefaultState())
                    .withParameter("bedrockBlock", Blocks.BEDROCK.getDefaultState())
                    .withParameter("seaLevel", 64)
                    .withParameter("useContinentalNoise", true)
                    .withParameter("visualizePlates", false) // Set to true to see plate boundaries
                    // Continental noise parameters
                    .withParameter("continentalScale", 40.0)
                    .withParameter("oceanicDepth", -30.0)
                    .withParameter("continentalHeight", 20.0),

                new PassConfiguration(VolatilityPass.class,5)
            );
        }
    },
    
    DEBUG_PLATES("debug_plates") {
        @Override
        public List<PassConfiguration> getGenerationPasses() {
            return List.of(
                // Debug pass that shows plate boundaries clearly
                new PassConfiguration(PhysicsFoundationPass.class, 0)
                    .withParameter("blockType", Blocks.GRASS_BLOCK.getDefaultState())
                    .withParameter("bedrockBlock", Blocks.BEDROCK.getDefaultState())
                    .withParameter("seaLevel", 64)
                    .withParameter("useContinentalNoise", true)
                    .withParameter("visualizePlates", true) // Shows plate types with different blocks
                    // Reduced continental scale to see plates better
                    .withParameter("continentalScale", 20.0)
                    .withParameter("oceanicDepth", -15.0)
                    .withParameter("continentalHeight", 10.0),

                    new PassConfiguration(VolatilityPass.class,5)
            );
        }
    };
    
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
    
    @Override
    public List<OctaveConfiguration> getOctaveConfigurations() {
        // We use passes now, but need to provide this for compatibility
        return List.of(
            new OctaveConfiguration(TectonicFoundationOctave.class)
        );
    }
}