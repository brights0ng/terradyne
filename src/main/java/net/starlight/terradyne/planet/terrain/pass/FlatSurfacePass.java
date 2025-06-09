package net.starlight.terradyne.planet.terrain.pass;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.starlight.terradyne.planet.biome.IBiomeType;
import net.starlight.terradyne.planet.terrain.OctaveContext;

public class FlatSurfacePass implements IGenerationPass {

    @Override
    public void applyPass(Chunk chunk, IBiomeType biome, OctaveContext context, PassConfiguration config) {
        BlockState surfaceBlock = config.getBlockState("blockType", Blocks.SAND.getDefaultState());
        int surfaceLevel = config.getInt("surfaceLevel", 75);

        // Place flat surface layer
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                chunk.setBlockState(new BlockPos(x, surfaceLevel, z), surfaceBlock, false);
            }
        }
    }

    @Override
    public int getPassPriority() { return 10; }

    @Override
    public String getPassName() { return "FlatSurface"; }

    @Override
    public String getParameterDocumentation() {
        return "FlatSurfacePass - creates flat surface layer";
    }
}