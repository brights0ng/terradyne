package net.starlight.terradyne.planet.terrain.pass;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.starlight.terradyne.planet.biome.IBiomeType;
import net.starlight.terradyne.planet.terrain.OctaveContext;

/**
 * Simple Geometric Salt Pass - Uses cellular/Voronoi pattern for clear geometric cracks
 */
public class GeometricSaltPass implements IGenerationPass {

    @Override
    public void applyPass(Chunk chunk, IBiomeType biome, OctaveContext context, PassConfiguration config) {
        // Read configuration
        BlockState dioriteBlock = config.getBlockState("baseLayer", Blocks.DIORITE.getDefaultState());
        BlockState calciteBlock = config.getBlockState("saltBlock", Blocks.CALCITE.getDefaultState());

        // Simple parameters
        int cellSize = config.getInt("cellSize", 6);           // Size of each "cell" in blocks
        double crackWidth = config.getDouble("crackWidth", 1.5); // Width of cracks

        System.out.println("=== SimpleGeometricSaltPass ===");
        System.out.println("Cell size: " + cellSize);

        // First pass: Place salt layers everywhere
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int sandTop = findTopBlock(chunk, x, z);
                if (sandTop >= 0) {
                    // Place both layers
                    if (sandTop + 1 < 256) {
                        chunk.setBlockState(new BlockPos(x, sandTop + 1, z), dioriteBlock, false);
                    }
                    if (sandTop + 2 < 256) {
                        chunk.setBlockState(new BlockPos(x, sandTop + 2, z), calciteBlock, false);
                    }
                }
            }
        }

        // Second pass: Create cracks using cellular pattern
        int cracksCreated = 0;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunk.getPos().getStartX() + x;
                int worldZ = chunk.getPos().getStartZ() + z;

                // Calculate distance to nearest cell center
                double minDistance = calculateMinDistanceToCellCenter(worldX, worldZ, cellSize, context);

                // If we're close to a cell edge, it's a crack
                if (minDistance < crackWidth) {
                    int sandTop = findTopBlock(chunk, x, z);
                    if (sandTop >= 0 && sandTop + 2 < 256) {
                        // Remove calcite layer to create crack
                        chunk.setBlockState(new BlockPos(x, sandTop + 2, z), Blocks.AIR.getDefaultState(), false);
                        cracksCreated++;

                        // Sometimes remove diorite too for deeper cracks
                        if (minDistance < crackWidth * 0.5 && sandTop + 1 < 256) {
                            chunk.setBlockState(new BlockPos(x, sandTop + 1, z), Blocks.AIR.getDefaultState(), false);
                        }
                    }
                }
            }
        }

        System.out.println("Cracks created: " + cracksCreated);
    }

    /**
     * Calculate minimum distance to any cell center
     */
    private double calculateMinDistanceToCellCenter(int worldX, int worldZ, int cellSize, OctaveContext context) {
        double minDistance = Double.MAX_VALUE;

        // Check nearby cells (3x3 grid)
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                // Calculate cell coordinates
                int cellX = (worldX / cellSize) + dx;
                int cellZ = (worldZ / cellSize) + dz;

                // Get cell center with noise offset
                double centerX = (cellX + 0.5) * cellSize;
                double centerZ = (cellZ + 0.5) * cellSize;

                // Add noise offset to make it less regular
                double offsetX = context.getNoiseProvider().sampleAt(cellX * 0.1, 0, cellZ * 0.1) * cellSize * 0.3;
                double offsetZ = context.getNoiseProvider().sampleAt(cellX * 0.1, 1, cellZ * 0.1) * cellSize * 0.3;

                centerX += offsetX;
                centerZ += offsetZ;

                // Calculate distance
                double distance = Math.sqrt(
                        (worldX - centerX) * (worldX - centerX) +
                                (worldZ - centerZ) * (worldZ - centerZ)
                );

                minDistance = Math.min(minDistance, distance);
            }
        }

        return minDistance;
    }

    /**
     * Find the top solid block
     */
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
    public int getPassPriority() {
        return 25;
    }

    @Override
    public String getPassName() {
        return "SimpleGeometricSalt";
    }

    @Override
    public String getParameterDocumentation() {
        return """
            Simple Geometric Salt Pass Parameters:
            - baseLayer (BlockState, default DIORITE): First layer above terrain
            - saltBlock (BlockState, default CALCITE): Second layer (salt crust)
            - cellSize (int, default 6): Size of geometric cells in blocks
            - crackWidth (double, default 1.5): Width of cracks between cells
            """;
    }
}