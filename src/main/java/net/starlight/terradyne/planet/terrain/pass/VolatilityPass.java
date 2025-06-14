package net.starlight.terradyne.planet.terrain.pass;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.starlight.terradyne.planet.biome.IBiomeType;
import net.starlight.terradyne.planet.physics.PlanetPhysicsModel;
import net.starlight.terradyne.planet.physics.TectonicPlateGenerator.PlateBoundaryInfo;
import net.starlight.terradyne.planet.physics.TectonicPlateGenerator.BoundaryType;
import net.starlight.terradyne.planet.terrain.MasterNoiseProvider;
import net.starlight.terradyne.planet.terrain.octave.OctaveContext;

/**
 * Volatility Pass - Creates mountains at positive volatility and valleys at negative volatility
 * Based on tectonic plate boundaries
 */
public class VolatilityPass implements IGenerationPass {
    
    @Override
    public void applyPass(Chunk chunk, IBiomeType biome, OctaveContext context, PassConfiguration config) {
        // Check if we have a physics model
        if (!(context.getPlanetModel() instanceof PlanetPhysicsModel)) {
            return; // No volatility without physics
        }
        
        PlanetPhysicsModel physicsModel = (PlanetPhysicsModel) context.getPlanetModel();
        MasterNoiseProvider noise = context.getNoiseProvider();
        
        // Configuration
        double mountainScale = config.getDouble("mountainScale", 40.0);
        double valleyDepth = config.getDouble("valleyDepth", 20.0);
        boolean addNoise = config.getBoolean("addNoise", true);
        boolean visualizeBoundaries = config.getBoolean("visualizeBoundaries", false);
        
        // Process each column
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunk.getPos().getStartX() + x;
                int worldZ = chunk.getPos().getStartZ() + z;
                
                // Get volatility from plate boundaries
                PlateBoundaryInfo boundaryInfo = physicsModel.getPlateBoundaryInfoAt(worldX, worldZ);
                double volatility = boundaryInfo.volatility;
                
                if (Math.abs(volatility) < 0.01) {
                    continue; // No significant volatility, skip
                }
                
                // Find current surface height
                int surfaceY = findSurfaceHeight(chunk, x, z);
                if (surfaceY < 0) continue;
                
                // Calculate height change based on volatility
                double heightChange = 0.0;
                
                if (volatility > 0) {
                    // Positive volatility = Mountains
                    heightChange = volatility * mountainScale;
                    
                    if (addNoise) {
                        // Add mountain ridge patterns
                        double ridgeNoise = noise.sampleRidge(worldX * 0.005, 0, worldZ * 0.005);
                        double detailNoise = noise.sampleTurbulence(worldX * 0.02, 0, worldZ * 0.02, 2);
                        
                        // Shape the mountain based on boundary type
                        if (boundaryInfo.boundaryType == BoundaryType.CONVERGENT) {
                            // Convergent = Sharp, tall mountains
                            heightChange *= (1.0 + ridgeNoise * 0.8 + detailNoise * 0.3);
                        } else {
                            // Divergent = Gentler rifts with some peaks
                            heightChange *= (1.0 + ridgeNoise * 0.4 + detailNoise * 0.2);
                        }
                    }
                    
                    // Add height to terrain
                    int addedHeight = (int) heightChange;
                    BlockState mountainBlock = config.getBlockState("mountainBlock", Blocks.STONE.getDefaultState());
                    
                    for (int dy = 1; dy <= addedHeight && surfaceY + dy < 256; dy++) {
                        BlockPos pos = new BlockPos(x, surfaceY + dy, z);
                        
                        // Vary block type based on height
                        if (dy > addedHeight - 3) {
                            // Peak material
                            chunk.setBlockState(pos, mountainBlock, false);
                        } else {
                            // Core material
                            chunk.setBlockState(pos, Blocks.DEEPSLATE.getDefaultState(), false);
                        }
                    }
                    
                } else {
                    // Negative volatility = Valleys
                    heightChange = volatility * valleyDepth; // This will be negative
                    
                    if (addNoise) {
                        // Add valley erosion patterns
                        double erosionNoise = noise.sampleErosion(worldX * 0.01, 0, worldZ * 0.01);
                        double channelNoise = noise.sampleAt(worldX * 0.03, 0, worldZ * 0.03);
                        
                        // Transform boundaries get channel-like valleys
                        if (boundaryInfo.boundaryType == BoundaryType.TRANSFORM) {
                            heightChange *= (1.0 + Math.abs(erosionNoise) * 0.5 + channelNoise * 0.2);
                        }
                    }
                    
                    // Carve out valley
                    int carveDepth = Math.abs((int) heightChange);
                    
                    for (int dy = 0; dy < carveDepth; dy++) {
                        int targetY = surfaceY - dy;
                        if (targetY < chunk.getBottomY() + 5) break; // Don't carve too deep
                        
                        BlockPos pos = new BlockPos(x, targetY, z);
                        chunk.setBlockState(pos, Blocks.AIR.getDefaultState(), false);
                    }
                }
                
                // Visualization for debugging
                if (visualizeBoundaries && Math.abs(volatility) > 0.5) {
                    BlockState markerBlock = volatility > 0 ? 
                        Blocks.RED_WOOL.getDefaultState() : 
                        Blocks.BLUE_WOOL.getDefaultState();
                    
                    // Place a marker at the surface
                    int markerY = volatility > 0 ? 
                        surfaceY + (int)(heightChange) + 1 : 
                        surfaceY - Math.abs((int)heightChange) - 1;
                        
                    if (markerY > 0 && markerY < 256) {
                        chunk.setBlockState(new BlockPos(x, markerY, z), markerBlock, false);
                    }
                }
            }
        }
    }
    
    /**
     * Find the current surface height
     */
    private int findSurfaceHeight(Chunk chunk, int x, int z) {
        for (int y = 255; y >= chunk.getBottomY(); y--) {
            BlockState state = chunk.getBlockState(new BlockPos(x, y, z));
            if (!state.isAir() && state.getBlock() != Blocks.BEDROCK) {
                return y;
            }
        }
        return -1;
    }
    
    @Override
    public int getPassPriority() {
        return 100; // Run after foundation (0) but before erosion/decoration
    }
    
    @Override
    public String getPassName() {
        return "Volatility";
    }
    
    @Override
    public String getParameterDocumentation() {
        return """
            Volatility Pass Parameters:
            - mountainScale (double, default 40.0): Maximum mountain height
            - valleyDepth (double, default 20.0): Maximum valley depth
            - mountainBlock (BlockState, default STONE): Block type for mountain peaks
            - addNoise (boolean, default true): Add noise variation to features
            - visualizeBoundaries (boolean, default false): Show boundaries with colored wool
            
            Creates terrain features based on tectonic volatility:
            - Convergent boundaries: Tall, sharp mountains
            - Divergent boundaries: Rift valleys with some peaks
            - Transform boundaries: Deep channel valleys
            """;
    }
}