package net.starlight.terradyne.planet.terrain.pass;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.starlight.terradyne.planet.biome.IBiomeType;
import net.starlight.terradyne.planet.terrain.OctaveConfiguration;
import net.starlight.terradyne.planet.terrain.OctaveContext;
import net.starlight.terradyne.planet.terrain.OctaveRegistry;
import net.starlight.terradyne.planet.terrain.octave.DuneOctave;

/**
 * Dune Override Pass - creates flowing sand dune formations
 * Uses the dune octave to create realistic wind-formed dune patterns
 */
public class DuneOverridePass implements IGenerationPass {

    @Override
    public void applyPass(Chunk chunk, IBiomeType biome, OctaveContext context, PassConfiguration config) {
        // Get dune octave
        DuneOctave duneOctave = (DuneOctave) OctaveRegistry.getOctave(DuneOctave.class);
        if (duneOctave == null) {
            throw new RuntimeException("DuneOctave not found for DuneOverridePass");
        }

        // Get configuration
        BlockState duneBlock = config.getBlockState("blockType", Blocks.SAND.getDefaultState());
        double windInfluence = config.getDouble("windInfluence", 0.6);
        int baseHeight = config.getInt("baseHeight", 70);

        // Create octave configuration from pass parameters
        OctaveConfiguration duneConfig = new OctaveConfiguration(DuneOctave.class)
                .withParameter("maxHeight", config.getDouble("maxHeight", 45.0))
                .withParameter("minHeight", config.getDouble("minHeight", 10.0))
                .withParameter("duneSpacing", config.getDouble("duneSpacing", 0.004))
                .withParameter("sharpness", config.getDouble("sharpness", 4.0))
                .withParameter("elevationVariation", config.getDouble("elevationVariation", 30.0));

        // Generate dunes for each column
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunk.getPos().getStartX() + x;
                int worldZ = chunk.getPos().getStartZ() + z;

                // Get existing terrain height
                int existingHeight = findSurfaceHeight(chunk, x, z);
                
                // Calculate dune height contribution
                double duneHeight = duneOctave.generateHeightContribution(worldX, worldZ, context, duneConfig);
                
                if (duneHeight > 5.0) { // Only place dunes where there's significant height
                    int newHeight = baseHeight + (int) duneHeight;
                    
                    // Extend existing terrain upward with dune sand
                    for (int y = existingHeight + 1; y <= newHeight && y < 256; y++) {
                        chunk.setBlockState(new BlockPos(x, y, z), duneBlock, false);
                    }
                }
            }
        }
    }

    /**
     * Find the highest solid block in a column
     */
    private int findSurfaceHeight(Chunk chunk, int x, int z) {
        for (int y = 255; y >= 0; y--) {
            if (!chunk.getBlockState(new BlockPos(x, y, z)).isAir()) {
                return y;
            }
        }
        return 0;
    }

    @Override
    public int getPassPriority() {
        return 10; // Override pass - after foundation
    }

    @Override
    public String getPassName() {
        return "DuneOverride";
    }

    @Override
    public String getParameterDocumentation() {
        return """
            Dune Override Pass Parameters:
            - blockType (BlockState, default SAND): Block type for dune formations
            - windInfluence (double, default 0.6): How much wind affects dune formation
            - baseHeight (int, default 70): Base height for dune calculations
            - maxHeight (double, default 45.0): Maximum dune height
            - minHeight (double, default 10.0): Minimum dune height
            - duneSpacing (double, default 0.004): Dune formation frequency
            - sharpness (double, default 4.0): Dune peak sharpness
            - elevationVariation (double, default 30.0): Dune field elevation variation
            """;
    }
}