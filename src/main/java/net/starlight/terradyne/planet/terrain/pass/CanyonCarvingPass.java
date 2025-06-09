package net.starlight.terradyne.planet.terrain.pass;

import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.starlight.terradyne.planet.biome.IBiomeType;
import net.starlight.terradyne.planet.terrain.OctaveConfiguration;
import net.starlight.terradyne.planet.terrain.OctaveContext;
import net.starlight.terradyne.planet.terrain.OctaveRegistry;
import net.starlight.terradyne.planet.terrain.octave.CanyonOctave;

/**
 * Canyon Carving Pass - removes blocks to create canyon systems
 * Uses the canyon octave to determine where and how deep to carve
 */
public class CanyonCarvingPass implements IGenerationPass {

    @Override
    public void applyPass(Chunk chunk, IBiomeType biome, OctaveContext context, PassConfiguration config) {
        // Get canyon octave
        CanyonOctave canyonOctave = (CanyonOctave) OctaveRegistry.getOctave(CanyonOctave.class);
        if (canyonOctave == null) {
            throw new RuntimeException("CanyonOctave not found for CanyonCarvingPass");
        }

        // Get configuration
        double maxDepth = config.getDouble("maxDepth", 50.0);
        double threshold = config.getDouble("threshold", 0.1); // How strong canyon needs to be to carve

        // Create octave configuration from pass parameters
        OctaveConfiguration canyonConfig = new OctaveConfiguration(CanyonOctave.class)
                .withParameter("maxDepth", maxDepth)
                .withParameter("channelFrequency", config.getDouble("canyon.channelFrequency", 0.003))
                .withParameter("meandering", config.getDouble("canyon.meandering", 2.0))
                .withParameter("wallSteepness", config.getDouble("canyon.wallSteepness", 5.0))
                .withParameter("tributaryDensity", config.getDouble("canyon.tributaryDensity", 1.2));

        // Carve canyons in each column
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunk.getPos().getStartX() + x;
                int worldZ = chunk.getPos().getStartZ() + z;

                // Ask canyon octave: "How much should we carve here?"
                // Note: Canyon octave returns NEGATIVE values for carving
                double canyonDepth = -canyonOctave.generateHeightContribution(worldX, worldZ, context, canyonConfig);

                if (canyonDepth > threshold) {
                    // This location should be carved
                    int surfaceHeight = findSurfaceHeight(chunk, x, z);
                    int carveDepth = (int) Math.min(canyonDepth, maxDepth);

                    // Carve from surface downward
                    for (int y = surfaceHeight; y >= surfaceHeight - carveDepth && y >= 0; y--) {
                        chunk.setBlockState(new BlockPos(x, y, z), Blocks.AIR.getDefaultState(), false);
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
        return 20; // Carving pass - runs after terrain placement
    }

    @Override
    public String getPassName() {
        return "CanyonCarving";
    }

    @Override
    public String getParameterDocumentation() {
        return """
            Canyon Carving Pass Parameters:
            - maxDepth (double, default 50.0): Maximum canyon carving depth
            - threshold (double, default 0.1): Minimum canyon strength to carve
            - canyon.channelFrequency (double, default 0.003): Canyon channel frequency
            - canyon.meandering (double, default 2.0): How much canyons meander
            - canyon.wallSteepness (double, default 5.0): Canyon wall steepness
            - canyon.tributaryDensity (double, default 1.2): Tributary network density
            """;
    }
}