package net.starlight.terradyne.planet.terrain.pass;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.starlight.terradyne.planet.biome.IBiomeType;
import net.starlight.terradyne.planet.terrain.OctaveConfiguration;
import net.starlight.terradyne.planet.terrain.OctaveContext;
import net.starlight.terradyne.planet.terrain.OctaveRegistry;
import net.starlight.terradyne.planet.terrain.octave.FoundationOctave;
import net.starlight.terradyne.planet.terrain.octave.RollingTerrainOctave;

/**
 * Limestone Layering Pass - Creates stratified limestone layers above base terrain
 * This creates the "mesa-like" layered geology that canyons will carve through
 */
public class LimestoneLayeringPass implements IGenerationPass {

    @Override
    public void applyPass(Chunk chunk, IBiomeType biome, OctaveContext context, PassConfiguration config) {
        BlockState limestone = config.getBlockState("blockType", Blocks.STONE.getDefaultState());
        int floorLevel = config.getInt("floorLevel", 25);
        int blocksUnderSurface = config.getInt("blocksUnderSurface", 3);

        // Get the same octaves that TerrainFoundationPass uses
        FoundationOctave foundationOctave = (FoundationOctave) OctaveRegistry.getOctave(FoundationOctave.class);
        RollingTerrainOctave rollingOctave = (RollingTerrainOctave) OctaveRegistry.getOctave(RollingTerrainOctave.class);

        if (foundationOctave == null || rollingOctave == null) {
            throw new RuntimeException("Required octaves not found for UndergroundLimestonePass");
        }

        // Use the SAME configuration as TerrainFoundationPass
        int baseSeaLevel = 75;

        OctaveConfiguration foundationConfig = new OctaveConfiguration(FoundationOctave.class)
                .withParameter("amplitude", 3.0)
                .withParameter("frequency", 0.0003);

        OctaveConfiguration rollingConfig = new OctaveConfiguration(RollingTerrainOctave.class)
                .withParameter("hillHeight", 4.0)
                .withParameter("hillFrequency", 0.008)
                .withParameter("rockOutcropIntensity", 0.1)
                .withParameter("washDepth", 1.0)
                .withParameter("undulationStrength", 0.6);

        // REPLACE existing terrain with limestone
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunk.getPos().getStartX() + x;
                int worldZ = chunk.getPos().getStartZ() + z;

                // Calculate terrain height using the SAME logic as TerrainFoundationPass
                double foundationHeight = foundationOctave.generateHeightContribution(worldX, worldZ, context, foundationConfig);
                double rollingHeight = rollingOctave.generateHeightContribution(worldX, worldZ, context, rollingConfig);
                double gentleDuneHeight = createGentleDunePattern(foundationHeight, rollingHeight, worldX, worldZ, context);

                int terrainHeight = baseSeaLevel + (int) gentleDuneHeight;
                int limestoneTop = terrainHeight - blocksUnderSurface;

                // REPLACE sand/sandstone with limestone from floor to calculated top
                for (int y = floorLevel; y <= limestoneTop && y < 256; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState existingBlock = chunk.getBlockState(pos);

                    // Replace sand or sandstone with limestone (don't replace air)
                    if (!existingBlock.isAir()) {
                        chunk.setBlockState(pos, limestone, false);
                    }
                }
            }
        }
    }

    /**
     * COPY of the gentle dune pattern from TerrainFoundationPass
     * This ensures we calculate the exact same terrain height
     */
    private double createGentleDunePattern(double foundation, double rolling, int worldX, int worldZ, OctaveContext context) {
        // Multiple scales of gentle dune patterns (copied from TerrainFoundationPass)
        double largeDunes = context.getNoiseProvider().sampleAt(worldX * 0.0008, 0, worldZ * 0.0008) * 4.0;
        double mediumDunes = context.getNoiseProvider().sampleAt(worldX * 0.002, 0, worldZ * 0.002) * 2.5;
        double smallDunes = context.getNoiseProvider().sampleAt(worldX * 0.008, 0, worldZ * 0.008) * 1.0;
        double microRipples = context.getNoiseProvider().sampleAt(worldX * 0.02, 0, worldZ * 0.02) * 0.5;

        // Combine foundation and rolling with dune patterns (same as TerrainFoundationPass)
        double combinedHeight = foundation + (rolling * 0.6) +
                largeDunes + mediumDunes + smallDunes + microRipples;

        // Apply gentle smoothing (same as TerrainFoundationPass)
        return Math.tanh(combinedHeight / 12.0) * 12.0;
    }

    /**
     * Find the actual sand surface height by looking for the highest sand/solid block
     */
    private int findSandSurfaceHeight(Chunk chunk, int x, int z) {
        // Search from top down to find the sand surface
        for (int y = 90; y >= 60; y--) { // Search in expected sand range
            BlockPos pos = new BlockPos(x, y, z);
            if (!chunk.getBlockState(pos).isAir()) {
                return y; // Found the sand surface
            }
        }

        // Fallback: if no surface found, assume default height
        return 75;
    }

    @Override
    public int getPassPriority() { return 10; }

    @Override
    public String getPassName() { return "NoiseBasedLimestone"; }

    @Override
    public String getParameterDocumentation() {
        return """
            Noise-Based Limestone Pass Parameters:
            - blockType (BlockState, default STONE): Limestone block type
            - floorLevel (int, default 25): Bottom level for limestone placement
            - blocksUnderSurface (int, default 3): Blocks below terrain surface to stop limestone
            
            Uses the same noise calculations as TerrainFoundationPass to ensure
            limestone is always exactly the specified distance below the terrain surface.
            """;
    }
}