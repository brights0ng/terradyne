
package net.starlight.terradyne.planet.biome;

import net.minecraft.block.Blocks;
import net.starlight.terradyne.planet.PlanetType;
import net.starlight.terradyne.planet.terrain.pass.*;

import java.util.List;

/**
 * UPDATED Desert Biomes - Mathematically equivalent to original system
 * Physics octaves calculate effects, placement passes place blocks
 */
public enum DesertBiomeType implements IBiomeType {

    /**
     * DUNE SEA - Mathematically equivalent to original
     * Physics: AdvancedDuneFormationOctave with exact original DuneOctave parameters
     * Placement: AdvancedDuneConstructionPass with exact original DuneOverridePass logic
     */
    DUNE_SEA("dune_sea") {
        @Override
        public List<PassConfiguration> getGenerationPasses() {
            return List.of(
                    // FOUNDATION: Exact original TerrainFoundationPass parameters
                    new PassConfiguration(TerrainFoundationPass.class, 0)
                            .withParameter("blockType", Blocks.SAND.getDefaultState())
                            .withParameter("baseSeaLevel", 70)
                            .withParameter("createMesaMounds", false)
                            // Foundation physics (original parameters)
                            .withParameter("foundation.amplitude", 2.0)
                            .withParameter("foundation.frequency", 0.0001)
                            // Rolling terrain physics (original parameters)
                            .withParameter("rolling.hillHeight", 8.0)
                            .withParameter("rolling.hillFrequency", 0.005)
                            .withParameter("rolling.rockOutcropIntensity", 0.1)
                            .withParameter("rolling.washDepth", 1.0)
                            .withParameter("rolling.undulationStrength", 0.8),

                    // DUNE FORMATION: Exact original DuneOverridePass parameters
                    new PassConfiguration(DuneConstructionPass.class, 10)
                            .withParameter("blockType", Blocks.SAND.getDefaultState())
                            .withParameter("windInfluence", 0.6)
                            .withParameter("baseHeight", 70)
                            // Dune physics (exact original DuneOctave parameters)
                            .withParameter("maxHeight", 45.0)
                            .withParameter("minHeight", 10.0)
                            .withParameter("duneSpacing", 0.004)
                            .withParameter("sharpness", 4.0)
                            .withParameter("elevationVariation", 30.0),

                    // SURFACE DETAILS: Exact original SurfaceDetailPass parameters
                    new PassConfiguration(SurfaceDetailPass.class, 30)
                            .withParameter("enableSurfaceDetail", true)
                            .withParameter("detail.intensity", 0.08)
                            .withParameter("detail.frequency", 0.08)
            );
        }
    },

    /**
     * GRANITE MESAS - Mathematically equivalent to original
     * Physics: AdvancedMesaFormationOctave + AdvancedDuneFormationOctave for sand accumulation
     * Placement: AdvancedMesaConstructionPass + AdvancedTerrainFoundationPass with mesa mounds
     */
    GRANITE_MESAS("granite_mesas") {
        @Override
        public List<PassConfiguration> getGenerationPasses() {
            return List.of(
                    // FOUNDATION: Exact original TerrainFoundationPass with mesa mounds
                    new PassConfiguration(TerrainFoundationPass.class, 0)
                            .withParameter("blockType", Blocks.SAND.getDefaultState())
                            .withParameter("baseSeaLevel", 75)
                            .withParameter("createMesaMounds", true)
                            // Foundation physics (original parameters)
                            .withParameter("foundation.amplitude", 8.0)
                            .withParameter("foundation.frequency", 0.0004)
                            // Enhanced rolling terrain physics (original parameters)
                            .withParameter("rolling.hillHeight", 25.0)
                            .withParameter("rolling.hillFrequency", 0.003)
                            .withParameter("rolling.rockOutcropIntensity", 0.4)
                            .withParameter("rolling.washDepth", 1.8)
                            .withParameter("rolling.undulationStrength", 1.5)
                            // Mesa parameters (exact original)
                            .withParameter("mesa.mesaHeight", 80.0)
                            .withParameter("mesa.plateauFrequency", 0.005)
                            .withParameter("mesa.steepness", 12.0)
                            .withParameter("mesa.erosionIntensity", 0.5)
                            .withParameter("mesa.layering", 1.0)
                            // Mesa mounds parameters (exact original)
                            .withParameter("mesaMounds.threshold", 0.02)
                            .withParameter("mesaMounds.heightScale", 0.8),

                    // MESA FORMATION: Exact original MesaOverridePass parameters
                    new PassConfiguration(MesaConstructionPass.class, 10)
                            .withParameter("blockType", Blocks.GRANITE.getDefaultState())
                            .withParameter("weatheredBlock", Blocks.TERRACOTTA.getDefaultState())
                            .withParameter("threshold", 0.3)
                            .withParameter("addSurfaceTexture", true)
                            .withParameter("addRockDebris", true)
                            // Mesa physics (exact original MesaOctave parameters)
                            .withParameter("mesa.mesaHeight", 80.0)
                            .withParameter("mesa.plateauFrequency", 0.005)
                            .withParameter("mesa.steepness", 12.0)
                            .withParameter("mesa.erosionIntensity", 1.2)
                            .withParameter("mesa.layering", 1.0),

                    // GRANITE CAPS: Exact original GraniteCapPass parameters
                    new PassConfiguration(GraniteCapPass.class, 15)
                            .withParameter("blockType", Blocks.GRANITE.getDefaultState())
                            .withParameter("threshold", 0.7)
                            .withParameter("capThickness", 6)
                            .withParameter("maxSlopeVariation", 2)
                            .withParameter("mesa.mesaHeight", 80.0)
                            .withParameter("mesa.plateauFrequency", 0.005)
                            .withParameter("mesa.steepness", 12.0),

                    // SURFACE DETAILS: Exact original parameters
                    new PassConfiguration(SurfaceDetailPass.class, 30)
                            .withParameter("enableSurfaceDetail", true)
                            .withParameter("detail.intensity", 0.12)
                            .withParameter("detail.frequency", 0.02)
            );
        }
    },

    /**
     * LIMESTONE CANYONS - Mathematically equivalent to original
     * Physics: AdvancedWaterErosionOctave with exact original CanyonCarvingPass parameters
     * Placement: AdvancedErosionCarvingPass + LimestoneLayeringPass
     */
    LIMESTONE_CANYONS("limestone_canyons") {
        @Override
        public List<PassConfiguration> getGenerationPasses() {
            return List.of(
                    // FOUNDATION: Exact original TerrainFoundationPass parameters
                    new PassConfiguration(TerrainFoundationPass.class, 0)
                            .withParameter("blockType", Blocks.SAND.getDefaultState())
                            .withParameter("baseSeaLevel", 75)
                            .withParameter("createMesaMounds", false)
                            // Foundation physics (original parameters)
                            .withParameter("foundation.amplitude", 3.0)
                            .withParameter("foundation.frequency", 0.0003)
                            // Rolling terrain physics (original parameters)
                            .withParameter("rolling.hillHeight", 4.0)
                            .withParameter("rolling.hillFrequency", 0.008)
                            .withParameter("rolling.rockOutcropIntensity", 0.1)
                            .withParameter("rolling.washDepth", 1.0)
                            .withParameter("rolling.undulationStrength", 0.6),

                    // LIMESTONE LAYERING: Exact original LimestoneLayeringPass parameters
                    new PassConfiguration(LimestoneLayeringPass.class, 15)
                            .withParameter("blockType", Blocks.STONE.getDefaultState())
                            .withParameter("floorLevel", 25)
                            .withParameter("blocksUnderSurface", 3),

                    // CANYON CARVING: Exact original CanyonCarvingPass parameters
                    new PassConfiguration(ErosionCarvingPass.class, 20)
                            .withParameter("canyonFloor", 25)
                            .withParameter("floorBlock", Blocks.SANDSTONE.getDefaultState())
                            // Canyon physics (exact original CanyonCarvingPass parameters)
                            .withParameter("threshold", 0.05)
                            .withParameter("canyonWidth", 0.8)
                            .withParameter("canyonDensity", 0.6)
                            .withParameter("branchingFactor", 0.4)
                            .withParameter("sharpness", 2.0)
                            .withParameter("noiseScale", 0.3)
            );
        }
    },

    /**
     * SALT FLATS - Mathematically equivalent to original
     * Physics: AdvancedSaltDepositionOctave with exact original GeometricSaltPass parameters
     * Placement: AdvancedSaltFormationPass with exact original block placement logic
     */
    SALT_FLATS("salt_flats") {
        @Override
        public List<PassConfiguration> getGenerationPasses() {
            return List.of(
                    // FOUNDATION: Exact original TerrainFoundationPass parameters for flat terrain
                    new PassConfiguration(TerrainFoundationPass.class, 0)
                            .withParameter("blockType", Blocks.SAND.getDefaultState())
                            .withParameter("baseSeaLevel", 75)
                            .withParameter("createMesaMounds", false)
                            // Minimal foundation physics (original parameters)
                            .withParameter("foundation.amplitude", 1.5)
                            .withParameter("foundation.frequency", 0.0001)
                            // Minimal rolling terrain (original parameters)
                            .withParameter("rolling.hillHeight", 2.0)
                            .withParameter("rolling.hillFrequency", 0.012)
                            .withParameter("rolling.rockOutcropIntensity", 0.02)
                            .withParameter("rolling.washDepth", 0.3)
                            .withParameter("rolling.undulationStrength", 0.2),

                    // SALT FORMATION: Exact original GeometricSaltPass parameters
                    new PassConfiguration(SaltFormationPass.class, 25)
                            .withParameter("baseLayer", Blocks.DIORITE.getDefaultState())
                            .withParameter("saltBlock", Blocks.CALCITE.getDefaultState())
                            // Salt physics (exact original GeometricSaltPass parameters)
                            .withParameter("cellSize", 6)
                            .withParameter("crackWidth", 1.5),

                    // SURFACE DETAILS: Exact original parameters with salt patterns
                    new PassConfiguration(SurfaceDetailPass.class, 30)
                            .withParameter("enableSurfaceDetail", true)
                            .withParameter("detail.intensity", 0.03)
                            .withParameter("detail.frequency", 0.05)
                            .withParameter("detail.saltPatterns", true)
            );
        }
    };

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
}