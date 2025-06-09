package net.starlight.terradyne.planet.biome;

import net.minecraft.block.Blocks;
import net.starlight.terradyne.planet.PlanetType;
import net.starlight.terradyne.planet.terrain.OctaveConfiguration;
import net.starlight.terradyne.planet.terrain.pass.*;
import net.starlight.terradyne.planet.terrain.octave.*;
import java.util.List;

/**
 * COMPLETE DesertBiomeType - All 4 biomes with pass-based generation
 */
public enum DesertBiomeType implements IBiomeType {

    DUNE_SEA("dune_sea") {
        @Override
        public List<PassConfiguration> getGenerationPasses() {
            return List.of(
                    // Pass 1: Sandy foundation with dune characteristics
                    new PassConfiguration(TerrainFoundationPass.class, 0)
                            .withParameter("blockType", Blocks.SAND.getDefaultState())
                            .withParameter("minHeight", 70)
                            .withParameter("foundation.amplitude", 2.0)  // Lower foundation variation
                            .withParameter("foundation.frequency", 0.0001)
                            .withParameter("rolling.hillHeight", 8.0)    // Gentler rolling base
                            .withParameter("rolling.hillFrequency", 0.005)
                            .withParameter("rolling.rockOutcropIntensity", 0.1) // Very few rock outcrops
                            .withParameter("rolling.washDepth", 1.0)
                            .withParameter("rolling.undulationStrength", 0.8),

                    // Pass 2: Dune override - creates the actual dune formations
                    new PassConfiguration(DuneOverridePass.class, 10)
                            .withParameter("blockType", Blocks.SAND.getDefaultState())
                            .withParameter("maxHeight", 45.0)
                            .withParameter("minHeight", 10.0)
                            .withParameter("duneSpacing", 0.004)
                            .withParameter("sharpness", 4.0)
                            .withParameter("elevationVariation", 30.0)
                            .withParameter("windInfluence", 0.6),

                    // Pass 3: Fine surface detail
                    new PassConfiguration(SurfaceDetailPass.class, 30)
                            .withParameter("enableSurfaceDetail", true)
                            .withParameter("detail.intensity", 0.08)  // Subtle sand ripples
                            .withParameter("detail.frequency", 0.08)  // Fine sand texture
            );
        }
    },

    GRANITE_MESAS("granite_mesas") {
        @Override
        public List<PassConfiguration> getGenerationPasses() {
            return List.of(
                            // Pass 1: DRAMATIC sand accumulation against mesas
                    new PassConfiguration(TerrainFoundationPass.class, 0)
                            .withParameter("blockType", Blocks.SAND.getDefaultState())
                            .withParameter("minHeight", 75)
                            .withParameter("createMesaMounds", true)
                            // Foundation parameters
                            .withParameter("foundation.amplitude", 8.0)
                            .withParameter("foundation.frequency", 0.0004)
                            // Enhanced rolling terrain for better blending
                            .withParameter("rolling.hillHeight", 25.0)
                            .withParameter("rolling.hillFrequency", 0.003)
                            .withParameter("rolling.rockOutcropIntensity", 0.4)
                            .withParameter("rolling.washDepth", 1.8)
                            .withParameter("rolling.undulationStrength", 1.5)
                            // Mesa parameters (same as mesa pass)
                            .withParameter("mesa.mesaHeight", 80.0)
                            .withParameter("mesa.plateauFrequency", 0.005)
                            .withParameter("mesa.steepness", 12.0)
                            .withParameter("mesa.erosionIntensity", 0.5)
                            .withParameter("mesa.layering", 1.0)
                            // DRAMATIC sand accumulation parameters
                            .withParameter("mesaMounds.threshold", 0.02)    // LOWER for wider influence area
                            .withParameter("mesaMounds.heightScale", 0.8),   // MUCH HIGHER for dramatic sand buildup

                    // Pass 2: Heavily eroded pure stone mesas
                    new PassConfiguration(MesaOverridePass.class, 10)
                            .withParameter("blockType", Blocks.GRANITE.getDefaultState())
                            .withParameter("weatheredBlock", Blocks.TERRACOTTA.getDefaultState())
                            .withParameter("threshold", 0.3)
                            .withParameter("addSurfaceTexture", true)
                            .withParameter("addRockDebris", true)
                            .withParameter("mesa.mesaHeight", 80.0)
                            .withParameter("mesa.plateauFrequency", 0.005)
                            .withParameter("mesa.steepness", 12.0)
                            .withParameter("mesa.erosionIntensity", 1.2)
                            .withParameter("mesa.layering", 1.0),

                    // Pass 3: Granite caps
                    new PassConfiguration(GraniteCapPass.class, 15)
                            .withParameter("blockType", Blocks.GRANITE.getDefaultState())
                            .withParameter("threshold", 0.7)
                            .withParameter("capThickness", 6)
                            .withParameter("maxSlopeVariation", 2)
                            .withParameter("mesa.mesaHeight", 80.0)
                            .withParameter("mesa.plateauFrequency", 0.005)
                            .withParameter("mesa.steepness", 12.0),

                    // Pass 4: Surface details
                    new PassConfiguration(SurfaceDetailPass.class, 30)
                            .withParameter("enableSurfaceDetail", true)
                            .withParameter("detail.intensity", 0.12)
                            .withParameter("detail.frequency", 0.02)
        );
        }
    },

    LIMESTONE_CANYONS("limestone_canyons") {
        @Override
        public List<PassConfiguration> getGenerationPasses() {
            return List.of(
                    // Pass 1: Sandy foundation
                    new PassConfiguration(TerrainFoundationPass.class, 0)
                            .withParameter("blockType", Blocks.SAND.getDefaultState())
                            .withParameter("minHeight", 60)
                            .withParameter("foundation.amplitude", 12.0)
                            .withParameter("foundation.frequency", 0.0008)
                            .withParameter("rolling.hillHeight", 10.0)  // Gentler rolling under limestone
                            .withParameter("rolling.hillFrequency", 0.006)
                            .withParameter("rolling.rockOutcropIntensity", 0.2)
                            .withParameter("rolling.washDepth", 2.0)
                            .withParameter("rolling.undulationStrength", 1.0),

                    // Pass 2: Limestone layer
                    new PassConfiguration(MesaOverridePass.class, 10)
                            .withParameter("blockType", Blocks.STONE.getDefaultState())
                            .withParameter("threshold", 0.1) // Lower threshold - more limestone coverage
                            .withParameter("baseHeight", 60)
                            .withParameter("mesa.mesaHeight", 40.0)
                            .withParameter("mesa.plateauFrequency", 0.004)
                            .withParameter("mesa.steepness", 2.0), // Much gentler than granite mesas

                    // Pass 3: Primary canyon carving
                    new PassConfiguration(CanyonCarvingPass.class, 20)
                            .withParameter("maxDepth", 50.0)
                            .withParameter("threshold", 0.1)
                            .withParameter("canyon.channelFrequency", 0.003)
                            .withParameter("canyon.meandering", 2.0)
                            .withParameter("canyon.wallSteepness", 5.0)
                            .withParameter("canyon.tributaryDensity", 1.2),

                    // Pass 4: Secondary canyon system (smaller scale)
                    new PassConfiguration(CanyonCarvingPass.class, 22)
                            .withParameter("maxDepth", 25.0)
                            .withParameter("threshold", 0.2)
                            .withParameter("canyon.channelFrequency", 0.008)
                            .withParameter("canyon.meandering", 1.0)
                            .withParameter("canyon.wallSteepness", 3.0)
                            .withParameter("canyon.tributaryDensity", 0.8),

                    // Pass 5: Surface details
                    new PassConfiguration(SurfaceDetailPass.class, 30)
                            .withParameter("detail.intensity", 0.15)
                            .withParameter("detail.frequency", 0.015)
            );
        }
    },

    SALT_FLATS("salt_flats") {
        @Override
        public List<PassConfiguration> getGenerationPasses() {
            return List.of(
                    // Pass 1: Correct height foundation (75, not 64!)
                    new PassConfiguration(TerrainFoundationPass.class, 0)
                            .withParameter("blockType", Blocks.SAND.getDefaultState())
                            .withParameter("baseSeaLevel", 75)  // FIXED: Correct height
                            .withParameter("foundation.amplitude", 1.5)  // Very flat foundation
                            .withParameter("foundation.frequency", 0.0001)
                            .withParameter("rolling.hillHeight", 2.0)    // Minimal rolling
                            .withParameter("rolling.hillFrequency", 0.012)
                            .withParameter("rolling.rockOutcropIntensity", 0.02) // Almost no outcrops
                            .withParameter("rolling.washDepth", 0.3)     // Very shallow drainage
                            .withParameter("rolling.undulationStrength", 0.2),

                    // Pass 2: Very minimal drainage patterns
                    new PassConfiguration(CanyonCarvingPass.class, 20)
                            .withParameter("maxDepth", 2.0)        // Extremely shallow
                            .withParameter("threshold", 0.4)       // Higher threshold - minimal carving
                            .withParameter("canyon.channelFrequency", 0.008)
                            .withParameter("canyon.meandering", 0.2)      // Straighter channels
                            .withParameter("canyon.wallSteepness", 1.0)   // Very gentle slopes
                            .withParameter("canyon.tributaryDensity", 0.1), // Minimal tributaries

                    // Pass 3: GEOMETRIC salt formations
                    new PassConfiguration(GeometricSaltPass.class, 25)  // NEW PASS
                            .withParameter("saltBlock", Blocks.SMOOTH_QUARTZ.getDefaultState()) // FIXED: Smooth quartz
                            .withParameter("coverage", 0.9)        // 90% salt coverage
                            .withParameter("polygonSize", 0.015)   // Size of salt polygons
                            .withParameter("terraceHeight", 3)     // Height of salt terraces
                            .withParameter("crystallineDetail", true)
                            .withParameter("hexagonalPatterns", true),

                    // Pass 4: Minimal surface details
                    new PassConfiguration(SurfaceDetailPass.class, 30)
                            .withParameter("enableSurfaceDetail", true)
                            .withParameter("detail.intensity", 0.03)     // Very subtle
                            .withParameter("detail.frequency", 0.05)
                            .withParameter("detail.saltPatterns", true)
            );
        }
    };;

    private final String name;

    DesertBiomeType(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public PlanetType getPlanetType() {
        return PlanetType.DESERT;
    }

    // Keep legacy method for backward compatibility during transition
    @Override
    @Deprecated
    public List<OctaveConfiguration> getOctaveConfigurations() {
        // Return empty list - we use passes now
        return List.of();
    }
}