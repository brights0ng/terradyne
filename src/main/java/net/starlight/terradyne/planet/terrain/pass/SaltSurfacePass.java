package net.starlight.terradyne.planet.terrain.pass;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.starlight.terradyne.planet.biome.IBiomeType;
import net.starlight.terradyne.planet.terrain.OctaveContext;

/**
 * Salt Surface Pass - places salt deposits on flat surfaces
 * Creates realistic salt flat surfaces with patchy salt coverage
 */
public class SaltSurfacePass implements IGenerationPass {

    @Override
    public void applyPass(Chunk chunk, IBiomeType biome, OctaveContext context, PassConfiguration config) {
        // Get configuration
        BlockState saltBlock = config.getBlockState("saltBlock", Blocks.QUARTZ_BLOCK.getDefaultState());
        double coverage = config.getDouble("coverage", 0.8);  // What % of surface gets salt
        int thickness = config.getInt("thickness", 1);        // How thick the salt layer is
        double patchSize = config.getDouble("patchSize", 0.02); // Size of salt patches

        // Place salt patches
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunk.getPos().getStartX() + x;
                int worldZ = chunk.getPos().getStartZ() + z;

                // Find surface
                int surfaceY = findSurfaceHeight(chunk, x, z);
                if (surfaceY <= 0) continue;

                // Check if this location should have salt
                double saltNoise = context.getNoiseProvider().sampleAt(worldX * patchSize, 0, worldZ * patchSize);
                double saltChance = (saltNoise + 1.0) * 0.5; // Convert to 0-1 range

                if (saltChance < coverage) {
                    // Place salt on surface
                    BlockState surfaceBlock = chunk.getBlockState(new BlockPos(x, surfaceY, z));
                    
                    // Only place salt on solid surfaces (not air/water)
                    if (!surfaceBlock.isAir()) {
                        for (int i = 1; i <= thickness && surfaceY + i < 256; i++) {
                            chunk.setBlockState(new BlockPos(x, surfaceY + i, z), saltBlock, false);
                        }
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
        return 25; // After carving, before final details
    }

    @Override
    public String getPassName() {
        return "SaltSurface";
    }

    @Override
    public String getParameterDocumentation() {
        return """
            Salt Surface Pass Parameters:
            - saltBlock (BlockState, default QUARTZ_BLOCK): Block type for salt deposits
            - coverage (double, default 0.8): Percentage of surface covered with salt (0.0-1.0)
            - thickness (int, default 1): Thickness of salt layer in blocks
            - patchSize (double, default 0.02): Size scale of salt patches
            """;
    }
}