package net.starlight.terradyne.planet.terrain.pass;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.starlight.terradyne.planet.biome.IBiomeType;
import net.starlight.terradyne.planet.terrain.OctaveConfiguration;
import net.starlight.terradyne.planet.terrain.OctaveContext;
import net.starlight.terradyne.planet.terrain.OctaveRegistry;
import net.starlight.terradyne.planet.terrain.octave.MesaFormationOctave;

/**
 * FIXED Granite Cap Pass - Only places granite on FLAT mesa tops, not slopes
 */
public class GraniteCapPass implements IGenerationPass {

    @Override
    public void applyPass(Chunk chunk, IBiomeType biome, OctaveContext context, PassConfiguration config) {
        MesaFormationOctave mesaOctave = (MesaFormationOctave) OctaveRegistry.getOctave(MesaFormationOctave.class);
        if (mesaOctave == null) return;

        // FIXED: Read from configuration instead of hardcoding
        BlockState capBlock = config.getBlockState("blockType", Blocks.GRANITE.getDefaultState()); // Will now use grass!
        double threshold = config.getDouble("threshold", 0.7);
        int capThickness = config.getInt("capThickness", 6);
        int maxSlopeVariation = config.getInt("maxSlopeVariation", 2);

        OctaveConfiguration mesaConfig = new OctaveConfiguration(MesaFormationOctave.class)
                .withParameter("mesaHeight", config.getDouble("mesa.mesaHeight", 80.0))
                .withParameter("plateauFrequency", config.getDouble("mesa.plateauFrequency", 0.005))
                .withParameter("steepness", config.getDouble("mesa.steepness", 12.0));

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunk.getPos().getStartX() + x;
                int worldZ = chunk.getPos().getStartZ() + z;

                double mesaStrength = mesaOctave.generateHeightContribution(worldX, worldZ, context, mesaConfig);

                if (mesaStrength > threshold) {
                    int surfaceY = findSurfaceHeight(chunk, x, z);

                    if (surfaceY > 120) { // Only on very tall mesas
                        if (isFlateauTop(chunk, x, z, surfaceY, maxSlopeVariation)) {
                            // Replace top layers with configured block type (now grass!)
                            for (int i = 0; i < capThickness && surfaceY - i >= 0; i++) {
                                BlockPos pos = new BlockPos(x, surfaceY - i, z);
                                BlockState existing = chunk.getBlockState(pos);

                                // Replace granite blocks with configured block type
                                if (existing.getBlock() == Blocks.GRANITE) {
                                    chunk.setBlockState(pos, capBlock, false); // This will now place grass!
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * NEW: Check if this location is on a flat plateau top (not a slope)
     */
    private boolean isFlateauTop(Chunk chunk, int x, int z, int surfaceY, int maxVariation) {
        // Check heights of neighboring blocks
        int[] neighborHeights = new int[8];
        int validNeighbors = 0;

        // Check 8 surrounding positions
        int[][] offsets = {{-1,-1}, {-1,0}, {-1,1}, {0,-1}, {0,1}, {1,-1}, {1,0}, {1,1}};

        for (int i = 0; i < offsets.length; i++) {
            int nx = x + offsets[i][0];
            int nz = z + offsets[i][1];

            // Skip if outside chunk
            if (nx < 0 || nx >= 16 || nz < 0 || nz >= 16) continue;

            int neighborHeight = findSurfaceHeight(chunk, nx, nz);
            neighborHeights[validNeighbors] = neighborHeight;
            validNeighbors++;
        }

        if (validNeighbors < 4) return false; // Not enough neighbors to judge

        // Check if all neighbors are within acceptable height variation
        for (int i = 0; i < validNeighbors; i++) {
            if (Math.abs(neighborHeights[i] - surfaceY) > maxVariation) {
                return false; // Too steep, this is a slope
            }
        }

        return true; // Flat enough to be a plateau top
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
    public int getPassPriority() { return 15; }

    @Override
    public String getPassName() { return "GraniteCap"; }

    @Override
    public String getParameterDocumentation() {
        return """
            Granite Cap Pass Parameters (FIXED):
            - blockType (BlockState, default GRANITE): Block type for granite caps
            - threshold (double, default 0.7): Minimum mesa strength for granite caps
            - capThickness (int, default 6): Thickness of granite cap layer
            - maxSlopeVariation (int, default 2): Max height difference for flat plateau detection
            """;
    }
}