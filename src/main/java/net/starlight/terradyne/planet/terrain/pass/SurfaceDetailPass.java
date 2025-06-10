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
 * Enhanced Surface Detail Pass with smooth transitions option
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
        boolean smoothTransitions = config.getBoolean("smoothTransitions", false);
        
        if (!enableSurfaceDetail) {
            return;
        }

        // Create octave configuration from pass parameters
        OctaveConfiguration detailConfig = new OctaveConfiguration(DetailOctave.class)
                .withParameter("intensity", config.getDouble("detail.intensity", 0.1))
                .withParameter("frequency", config.getDouble("detail.frequency", 0.02))
                .withParameter("saltPatterns", config.getBoolean("detail.saltPatterns", false))
                .withParameter("volcanic", config.getBoolean("detail.volcanic", false));

        // Pre-calculate smooth detail heights if smooth transitions enabled
        double[][] detailHeights = null;
        if (smoothTransitions) {
            detailHeights = precalculateDetailHeights(chunk, context, detailOctave, detailConfig);
        }

        // Add surface details
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunk.getPos().getStartX() + x;
                int worldZ = chunk.getPos().getStartZ() + z;

                // Find surface
                int surfaceY = findSurfaceHeight(chunk, x, z);
                if (surfaceY <= 0) continue;

                // Get detail contribution
                double detailHeight;
                if (smoothTransitions && detailHeights != null) {
                    detailHeight = getSmoothDetailHeight(detailHeights, x, z);
                } else {
                    detailHeight = detailOctave.generateHeightContribution(worldX, worldZ, context, detailConfig);
                }

                // Apply VERY subtle height adjustments for smooth terrain
                if (smoothTransitions) {
                    // For smooth biomes, only apply very subtle changes
                    if (Math.abs(detailHeight) > 0.3) {
                        int heightAdjustment = (int) Math.signum(detailHeight);
                        
                        if (heightAdjustment > 0 && surfaceY + 1 < 256) {
                            BlockState surfaceBlock = chunk.getBlockState(new BlockPos(x, surfaceY, z));
                            chunk.setBlockState(new BlockPos(x, surfaceY + 1, z), surfaceBlock, false);
                        } else if (heightAdjustment < 0 && surfaceY > 0) {
                            chunk.setBlockState(new BlockPos(x, surfaceY, z), Blocks.AIR.getDefaultState(), false);
                        }
                    }
                } else {
                    // Normal detail application for other biomes
                    int heightAdjustment = (int) Math.round(detailHeight);
                    
                    if (heightAdjustment > 0) {
                        BlockState surfaceBlock = chunk.getBlockState(new BlockPos(x, surfaceY, z));
                        for (int i = 1; i <= heightAdjustment && surfaceY + i < 256; i++) {
                            chunk.setBlockState(new BlockPos(x, surfaceY + i, z), surfaceBlock, false);
                        }
                    } else if (heightAdjustment < 0) {
                        for (int i = 0; i > heightAdjustment && surfaceY + i >= 0; i--) {
                            chunk.setBlockState(new BlockPos(x, surfaceY + i, z), Blocks.AIR.getDefaultState(), false);
                        }
                    }
                }
            }
        }
    }
    
    private double[][] precalculateDetailHeights(Chunk chunk, OctaveContext context, 
                                                 DetailOctave detailOctave, OctaveConfiguration config) {
        double[][] heights = new double[18][18];
        
        for (int x = -1; x <= 16; x++) {
            for (int z = -1; z <= 16; z++) {
                int worldX = chunk.getPos().getStartX() + x;
                int worldZ = chunk.getPos().getStartZ() + z;
                
                double height = detailOctave.generateHeightContribution(worldX, worldZ, context, config);
                heights[x + 1][z + 1] = height;
            }
        }
        
        return heights;
    }
    
    private double getSmoothDetailHeight(double[][] heights, int x, int z) {
        double baseHeight = heights[x + 1][z + 1];
        double northHeight = heights[x + 1][z];
        double southHeight = heights[x + 1][z + 2];
        double eastHeight = heights[x + 2][z + 1];
        double westHeight = heights[x][z + 1];
        
        // Heavy smoothing for detail pass
        return baseHeight * 0.4 + (northHeight + southHeight + eastHeight + westHeight) * 0.15;
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
    public int getPassPriority() {
        return 30; // Detail pass - runs last
    }

    @Override
    public String getPassName() {
        return "EnhancedSurfaceDetail";
    }

    @Override
    public String getParameterDocumentation() {
        return """
            Enhanced Surface Detail Pass Parameters:
            - enableSurfaceDetail (boolean, default true): Enable surface detail generation
            - smoothTransitions (boolean, default false): Enable extra smoothing for flowing terrain
            - detail.intensity (double, default 0.1): Detail feature intensity
            - detail.frequency (double, default 0.02): Detail feature frequency
            - detail.saltPatterns (boolean, default false): Enable salt crystal patterns
            - detail.volcanic (boolean, default false): Enable volcanic surface texture
            
            When smoothTransitions is enabled:
            - Applies much more subtle height changes
            - Uses neighbor averaging for smoother results
            - Perfect for flowing dune terrain
            """;
    }
}