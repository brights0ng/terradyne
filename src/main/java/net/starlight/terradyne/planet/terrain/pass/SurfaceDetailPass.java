package net.starlight.terradyne.planet.terrain.pass;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.starlight.terradyne.planet.biome.IBiomeType;
import net.starlight.terradyne.planet.terrain.OctaveConfiguration;
import net.starlight.terradyne.planet.terrain.OctaveContext;
import net.starlight.terradyne.planet.terrain.OctaveRegistry;
import net.starlight.terradyne.planet.terrain.octave.DetailOctave;

/**
 * Surface Detail Pass - adds fine surface details and decorations
 * Uses the detail octave to add surface texture and small features
 */
public class SurfaceDetailPass implements IGenerationPass {

    @Override
    public void applyPass(Chunk chunk, IBiomeType biome, OctaveContext context, PassConfiguration config) {
        // Get detail octave
        DetailOctave detailOctave = (DetailOctave) OctaveRegistry.getOctave(DetailOctave.class);
        if (detailOctave == null) {
            return; // Detail is optional
        }

        // Get configuration
        boolean enableSurfaceDetail = config.getBoolean("enableSurfaceDetail", true);
        if (!enableSurfaceDetail) {
            return;
        }

        // Create octave configuration from pass parameters
        OctaveConfiguration detailConfig = new OctaveConfiguration(DetailOctave.class)
                .withParameter("intensity", config.getDouble("detail.intensity", 0.1))
                .withParameter("frequency", config.getDouble("detail.frequency", 0.02))
                .withParameter("saltPatterns", config.getBoolean("detail.saltPatterns", false))
                .withParameter("volcanic", config.getBoolean("detail.volcanic", false));

        // Add surface details
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunk.getPos().getStartX() + x;
                int worldZ = chunk.getPos().getStartZ() + z;

                // Find surface
                int surfaceY = findSurfaceHeight(chunk, x, z);
                if (surfaceY <= 0) continue;

                // Get detail contribution
                double detailHeight = detailOctave.generateHeightContribution(worldX, worldZ, context, detailConfig);

                // Apply small height adjustments
                int heightAdjustment = (int) Math.round(detailHeight);
                
                if (heightAdjustment > 0) {
                    // Add small bumps
                    BlockState surfaceBlock = chunk.getBlockState(new BlockPos(x, surfaceY, z));
                    for (int i = 1; i <= heightAdjustment && surfaceY + i < 256; i++) {
                        chunk.setBlockState(new BlockPos(x, surfaceY + i, z), surfaceBlock, false);
                    }
                } else if (heightAdjustment < 0) {
                    // Create small depressions
                    for (int i = 0; i > heightAdjustment && surfaceY + i >= 0; i--) {
                        chunk.setBlockState(new BlockPos(x, surfaceY + i, z), Blocks.AIR.getDefaultState(), false);
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
        return 30; // Detail pass - runs last
    }

    @Override
    public String getPassName() {
        return "SurfaceDetail";
    }

    @Override
    public String getParameterDocumentation() {
        return """
            Surface Detail Pass Parameters:
            - enableSurfaceDetail (boolean, default true): Enable surface detail generation
            - detail.intensity (double, default 0.1): Detail feature intensity
            - detail.frequency (double, default 0.02): Detail feature frequency
            - detail.saltPatterns (boolean, default false): Enable salt crystal patterns
            - detail.volcanic (boolean, default false): Enable volcanic surface texture
            """;
    }
}