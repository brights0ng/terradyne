package net.starlight.terradyne.planet.terrain.pass;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.starlight.terradyne.planet.biome.IBiomeType;
import net.starlight.terradyne.planet.terrain.OctaveContext;

import java.util.*;

/**
 * Simple Salt Cracks - Random vertices with nearest-neighbor connections
 */
public class GeometricSaltPass implements IGenerationPass {

    @Override
    public void applyPass(Chunk chunk, IBiomeType biome, OctaveContext context, PassConfiguration config) {

        // STEP 1: Place layers everywhere
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int surfaceY = findSurfaceHeight(chunk, x, z);
                if (surfaceY <= 0) continue;

                chunk.setBlockState(new BlockPos(x, surfaceY + 1, z), Blocks.DIORITE.getDefaultState(), false);
                chunk.setBlockState(new BlockPos(x, surfaceY + 2, z), Blocks.SMOOTH_QUARTZ.getDefaultState(), false);
            }
        }

        // STEP 2: Generate truly random vertices
        List<CrackVertex> vertices = generateRandomVertices(chunk, context);

        // STEP 3: Connect each vertex to its 2-3 closest neighbors
        Set<BlockPos> crackPositions = connectToNearestNeighbors(vertices);

        // STEP 4: Apply cracks
        for (BlockPos crackPos : crackPositions) {
            int x = crackPos.getX();
            int z = crackPos.getZ();

            if (x >= 0 && x < 16 && z >= 0 && z < 16) {
                int surfaceY = findSurfaceHeight(chunk, x, z);
                if (surfaceY > 0) {
                    chunk.setBlockState(new BlockPos(x, surfaceY + 2, z), Blocks.AIR.getDefaultState(), false);
                }
            }
        }
    }

    /**
     * Generate truly random vertices (not on a grid!)
     */
    private List<CrackVertex> generateRandomVertices(Chunk chunk, OctaveContext context) {
        List<CrackVertex> vertices = new ArrayList<>();

        // Sample many potential locations randomly
        for (int attempt = 0; attempt < 200; attempt++) { // Try 200 random locations
            // Generate random coordinates in expanded area around chunk
            int randomX = -15 + (attempt * 7) % 42; // Pseudo-random X from -15 to +27
            int randomZ = -15 + (attempt * 11) % 42; // Pseudo-random Z from -15 to +27

            int worldX = chunk.getPos().getStartX() + randomX;
            int worldZ = chunk.getPos().getStartZ() + randomZ;

            // Use noise to determine if vertex should exist at this random location
            double vertexNoise = context.getNoiseProvider().sampleAt(worldX * 0.015, 0, worldZ * 0.015);
            double secondaryNoise = context.getNoiseProvider().sampleAt(worldX * 0.008, 0, worldZ * 0.012);

            // Combine noise values for more randomness
            double combinedNoise = (vertexNoise + secondaryNoise) / 2.0;

            if (combinedNoise > 0.4) { // Higher threshold = fewer, more spread out vertices
                // Add small random offset using different noise
                double offsetNoise1 = context.getNoiseProvider().sampleAt(worldX * 0.031, 0, worldZ * 0.027);
                double offsetNoise2 = context.getNoiseProvider().sampleAt(worldX * 0.023, 0, worldZ * 0.037);

                int finalX = randomX + (int)(offsetNoise1 * 4.0);
                int finalZ = randomZ + (int)(offsetNoise2 * 4.0);

                // Avoid placing vertices too close to existing ones
                boolean tooClose = false;
                for (CrackVertex existing : vertices) {
                    double distance = Math.sqrt(
                            (existing.x - finalX) * (existing.x - finalX) +
                                    (existing.z - finalZ) * (existing.z - finalZ)
                    );
                    if (distance < 6.0) { // Minimum 6 blocks apart
                        tooClose = true;
                        break;
                    }
                }

                if (!tooClose) {
                    vertices.add(new CrackVertex(finalX, finalZ));
                }
            }
        }

        System.out.println("Generated " + vertices.size() + " random vertices");
        return vertices;
    }

    /**
     * Connect each vertex to its 2-3 closest neighbors
     */
    private Set<BlockPos> connectToNearestNeighbors(List<CrackVertex> vertices) {
        Set<BlockPos> crackPositions = new HashSet<>();

        for (CrackVertex vertex : vertices) {
            // Find distances to all other vertices
            List<VertexDistance> distances = new ArrayList<>();

            for (CrackVertex other : vertices) {
                if (other != vertex) {
                    double distance = Math.sqrt(
                            (vertex.x - other.x) * (vertex.x - other.x) +
                                    (vertex.z - other.z) * (vertex.z - other.z)
                    );
                    distances.add(new VertexDistance(other, distance));
                }
            }

            // Sort by distance (closest first)
            distances.sort(Comparator.comparingDouble(vd -> vd.distance));

            // Connect to 2-3 closest vertices
            int connectionsToMake = Math.min(3, distances.size());
            for (int i = 0; i < connectionsToMake; i++) {
                CrackVertex target = distances.get(i).vertex;
                drawThickLine(vertex.x, vertex.z, target.x, target.z, crackPositions);
            }
        }

        return crackPositions;
    }

    /**
     * Draw 2-block wide line between two points
     */
    private void drawThickLine(int x1, int z1, int x2, int z2, Set<BlockPos> crackPositions) {
        int dx = Math.abs(x2 - x1);
        int dz = Math.abs(z2 - z1);
        int stepX = x1 < x2 ? 1 : -1;
        int stepZ = z1 < z2 ? 1 : -1;
        int err = dx - dz;

        int currentX = x1;
        int currentZ = z1;

        while (true) {
            // Add 2x2 block pattern for thickness
            crackPositions.add(new BlockPos(currentX, 0, currentZ));
            crackPositions.add(new BlockPos(currentX + 1, 0, currentZ));
            crackPositions.add(new BlockPos(currentX, 0, currentZ + 1));
            crackPositions.add(new BlockPos(currentX + 1, 0, currentZ + 1));

            if (currentX == x2 && currentZ == z2) break;

            int e2 = 2 * err;
            if (e2 > -dz) {
                err -= dz;
                currentX += stepX;
            }
            if (e2 < dx) {
                err += dx;
                currentZ += stepZ;
            }
        }
    }

    private int findSurfaceHeight(Chunk chunk, int x, int z) {
        for (int y = chunk.getTopY() - 1; y >= chunk.getBottomY(); y--) {
            BlockState state = chunk.getBlockState(new BlockPos(x, y, z));
            if (!state.isAir() && state.getBlock() != Blocks.WATER) {
                return y;
            }
        }
        return 0;
    }

    @Override
    public int getPassPriority() { return 25; }

    @Override
    public String getPassName() { return "RandomVertexSaltCracks"; }

    /**
     * Vertex class
     */
    private static class CrackVertex {
        final int x, z;

        CrackVertex(int x, int z) {
            this.x = x;
            this.z = z;
        }
    }

    /**
     * Helper class for sorting vertices by distance
     */
    private static class VertexDistance {
        final CrackVertex vertex;
        final double distance;

        VertexDistance(CrackVertex vertex, double distance) {
            this.vertex = vertex;
            this.distance = distance;
        }
    }
}