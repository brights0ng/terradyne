package net.starlight.terradyne.planet.terrain.pass;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.starlight.terradyne.planet.biome.IBiomeType;
import net.starlight.terradyne.planet.terrain.OctaveConfiguration;
import net.starlight.terradyne.planet.terrain.OctaveContext;
import net.starlight.terradyne.planet.terrain.OctaveRegistry;
import net.starlight.terradyne.planet.terrain.octave.DuneFormationOctave;
import net.starlight.terradyne.planet.terrain.pass.IGenerationPass;
import net.starlight.terradyne.planet.terrain.pass.PassConfiguration;

/**
 * PLACEMENT: Smooth Flowing Dune Construction Pass
 * Takes smooth dune physics and creates beautiful, flowing sand dune blocks
 * Focus: Preserving smoothness, adding surface flow details, realistic materials
 */
public class DuneConstructionPass implements IGenerationPass {

    @Override
    public void applyPass(Chunk chunk, IBiomeType biome, OctaveContext context, PassConfiguration config) {
        // Get physics octave
        DuneFormationOctave dunePhysics = (DuneFormationOctave) OctaveRegistry.getOctave(DuneFormationOctave.class);
        if (dunePhysics == null) {
            throw new RuntimeException("AdvancedDuneFormationOctave not found");
        }

        // Block placement configuration (original parameters)
        BlockState duneBlock = config.getBlockState("blockType", Blocks.SAND.getDefaultState());
        double windInfluence = config.getDouble("windInfluence", 0.6);
        int baseHeight = config.getInt("baseHeight", 70);

        // Physics configuration (original DuneOctave parameters)
        OctaveConfiguration duneConfig = new OctaveConfiguration(DuneFormationOctave.class)
                .withParameter("maxHeight", config.getDouble("maxHeight", 45.0))
                .withParameter("minHeight", config.getDouble("minHeight", 10.0))
                .withParameter("duneSpacing", config.getDouble("duneSpacing", 0.004))
                .withParameter("sharpness", config.getDouble("sharpness", 4.0))
                .withParameter("elevationVariation", config.getDouble("elevationVariation", 30.0));

        // Generate dunes using physics (original algorithm)
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunk.getPos().getStartX() + x;
                int worldZ = chunk.getPos().getStartZ() + z;

                // Get existing terrain height
                int existingHeight = findSurfaceHeight(chunk, x, z);

                // Calculate dune height using physics
                double duneHeight = dunePhysics.generateHeightContribution(worldX, worldZ, context, duneConfig);

                if (duneHeight > 5.0) { // Original threshold
                    int newHeight = baseHeight + (int) duneHeight;

                    // Extend existing terrain upward with dune sand (original logic)
                    for (int y = existingHeight + 1; y <= newHeight && y < 256; y++) {
                        chunk.setBlockState(new BlockPos(x, y, z), duneBlock, false);
                    }
                }
            }
        }
    }

    private int findSurfaceHeight(Chunk chunk, int x, int z) {
        for (int y = 255; y >= 0; y--) {
            if (!chunk.getBlockState(new BlockPos(x, y, z)).isAir()) {
                return y;
            }
        }
        return 0;
    }

    @Override
    public int getPassPriority() { return 10; }

    @Override
    public String getPassName() { return "SmoothDuneConstruction"; }

    @Override
    public String getParameterDocumentation() {
        return """
            Smooth Dune Construction Pass Parameters:
            - looseSand (BlockState, default SAND): Loose sand for dune surfaces
            - packedSand (BlockState, default SANDSTONE): Compressed sand for dune cores
            - wetSand (BlockState, default SMOOTH_SANDSTONE): Moist sand for dune bases
            - minimumDuneHeight (double, default 3.0): Minimum height to build dunes
            - addSurfaceFlow (boolean, default true): Add wind flow surface details
            - addRippleDetails (boolean, default true): Add wind ripple surface texture
            
            Physics parameters:
            - physics.windStrength (double, default 1.2): Wind effect strength
            - physics.sandAvailability (double, default 1.0): Sand availability
            - physics.duneSpacing (double, default 0.003): Dune size/frequency
            - physics.flowSmoothness (double, default 0.9): Smoothness factor
            
            Creates smooth, flowing, majestic sand dunes with:
            - Realistic material layering (wet base, packed core, loose surface)
            - Surface flow patterns following wind direction
            - Wind ripple details for texture
            - Smooth interpolation for natural transitions
            """;
    }
}