package net.starlight.terradyne.planet.terrain.pass;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.starlight.terradyne.planet.biome.IBiomeType;
import net.starlight.terradyne.planet.terrain.OctaveConfiguration;
import net.starlight.terradyne.planet.terrain.OctaveContext;
import net.starlight.terradyne.planet.terrain.OctaveRegistry;
import net.starlight.terradyne.planet.terrain.octave.SaltDepositionOctave;

/**
 * PLACEMENT: Salt Formation Pass
 * Uses SaltDepositionOctave physics to place salt formations
 */
public class SaltFormationPass implements IGenerationPass {

    @Override
    public void applyPass(Chunk chunk, IBiomeType biome, OctaveContext context, PassConfiguration config) {
        SaltDepositionOctave saltPhysics = (SaltDepositionOctave) OctaveRegistry.getOctave(SaltDepositionOctave.class);
        if (saltPhysics == null) {
            throw new RuntimeException("AdvancedSaltDepositionOctave not found");
        }

        // Block placement configuration (original parameters)
        BlockState dioriteBlock = config.getBlockState("baseLayer", Blocks.DIORITE.getDefaultState());
        BlockState calciteBlock = config.getBlockState("saltBlock", Blocks.CALCITE.getDefaultState());

        // Physics configuration (original cellular parameters)
        OctaveConfiguration saltConfig = new OctaveConfiguration(SaltDepositionOctave.class)
                .withParameter("cellSize", config.getInt("cellSize", 6))
                .withParameter("crackWidth", config.getDouble("crackWidth", 1.5));

        // First pass: Place salt layers everywhere (original algorithm)
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int sandTop = findTopBlock(chunk, x, z);
                if (sandTop >= 0) {
                    if (sandTop + 1 < 256) {
                        chunk.setBlockState(new BlockPos(x, sandTop + 1, z), dioriteBlock, false);
                    }
                    if (sandTop + 2 < 256) {
                        chunk.setBlockState(new BlockPos(x, sandTop + 2, z), calciteBlock, false);
                    }
                }
            }
        }

        // Second pass: Create cracks using physics (original algorithm)
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunk.getPos().getStartX() + x;
                int worldZ = chunk.getPos().getStartZ() + z;

                // Get salt formation strength from physics
                double saltFormation = saltPhysics.generateHeightContribution(worldX, worldZ, context, saltConfig);

                if (saltFormation > 0) { // This is a crack area
                    int sandTop = findTopBlock(chunk, x, z);
                    if (sandTop >= 0 && sandTop + 2 < 256) {
                        // Remove calcite layer to create crack
                        chunk.setBlockState(new BlockPos(x, sandTop + 2, z), Blocks.AIR.getDefaultState(), false);

                        // Sometimes remove diorite too for deeper cracks
                        if (saltFormation > 0.5 && sandTop + 1 < 256) {
                            chunk.setBlockState(new BlockPos(x, sandTop + 1, z), Blocks.AIR.getDefaultState(), false);
                        }
                    }
                }
            }
        }
    }

    private int findTopBlock(Chunk chunk, int x, int z) {
        for (int y = 120; y >= 0; y--) {
            BlockState state = chunk.getBlockState(new BlockPos(x, y, z));
            if (!state.isAir() && state.getBlock() != Blocks.WATER) {
                return y;
            }
        }
        return -1;
    }

    @Override
    public int getPassPriority() { return 25; }
    
    @Override
    public String getPassName() { return "SaltFormation"; }
}