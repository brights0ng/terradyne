package net.starlight.terradyne.planet.terrain.pass;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.starlight.terradyne.planet.biome.IBiomeType;
import net.starlight.terradyne.planet.terrain.OctaveConfiguration;
import net.starlight.terradyne.planet.terrain.OctaveContext;
import net.starlight.terradyne.planet.terrain.OctaveRegistry;
import net.starlight.terradyne.planet.terrain.octave.WaterErosionOctave;

/**
 * PLACEMENT: Erosion Carving Pass
 * Uses WaterErosionOctave physics to carve terrain
 */
public class ErosionCarvingPass implements IGenerationPass {

    @Override
    public void applyPass(Chunk chunk, IBiomeType biome, OctaveContext context, PassConfiguration config) {
        WaterErosionOctave erosionPhysics = (WaterErosionOctave) OctaveRegistry.getOctave(WaterErosionOctave.class);
        if (erosionPhysics == null) {
            throw new RuntimeException("AdvancedWaterErosionOctave not found");
        }

        // Block placement configuration (original parameters)
        int canyonFloor = config.getInt("canyonFloor", 25);
        BlockState floorBlock = config.getBlockState("floorBlock", Blocks.SANDSTONE.getDefaultState());

        // Physics configuration (original canyon parameters)
        OctaveConfiguration erosionConfig = new OctaveConfiguration(WaterErosionOctave.class)
                .withParameter("threshold", config.getDouble("threshold", 0.05))
                .withParameter("canyonWidth", config.getDouble("canyonWidth", 0.8))
                .withParameter("canyonDensity", config.getDouble("canyonDensity", 0.6))
                .withParameter("branchingFactor", config.getDouble("branchingFactor", 0.4))
                .withParameter("sharpness", config.getDouble("sharpness", 2.0))
                .withParameter("noiseScale", config.getDouble("noiseScale", 0.3));

        // Carve terrain using physics (original algorithm)
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunk.getPos().getStartX() + x;
                int worldZ = chunk.getPos().getStartZ() + z;

                int surfaceHeight = findSurfaceHeight(chunk, x, z);

                // Get erosion result from physics (negative value)
                double erosionResult = erosionPhysics.generateHeightContribution(worldX, worldZ, context, erosionConfig);

                if (erosionResult < 0) { // Erosion occurred
                    double canyonStrength = Math.abs(erosionResult) / 40.0; // Normalize
                    int depthVariation = (int)(canyonStrength * 5);
                    int actualFloor = Math.max(canyonFloor, canyonFloor + depthVariation - 2);

                    // Remove blocks to create channel (original logic)
                    for (int y = surfaceHeight; y >= actualFloor; y--) {
                        chunk.setBlockState(new BlockPos(x, y, z), Blocks.AIR.getDefaultState(), false);
                    }

                    // Place channel floor
                    chunk.setBlockState(new BlockPos(x, actualFloor, z), floorBlock, false);

                    // Add rock debris (original)
                    if (canyonStrength > 0.8 && context.getNoiseProvider().sampleAt(worldX * 0.1 * 0.3, 0, worldZ * 0.1 * 0.3) > 0.8) {
                        chunk.setBlockState(new BlockPos(x, actualFloor + 1, z), Blocks.COBBLESTONE.getDefaultState(), false);
                    }
                }
            }
        }
    }

    private int findSurfaceHeight(Chunk chunk, int x, int z) {
        for (int y = 85; y >= 65; y--) {
            if (!chunk.getBlockState(new BlockPos(x, y, z)).isAir()) {
                return y;
            }
        }
        return 75;
    }

    @Override
    public int getPassPriority() { return 20; }
    
    @Override
    public String getPassName() { return "ErosionCarving"; }
}
