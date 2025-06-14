package net.starlight.terradyne.planet.terrain.pass;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.starlight.terradyne.planet.biome.IBiomeType;
import net.starlight.terradyne.planet.physics.PlanetPhysicsModel;
import net.starlight.terradyne.planet.physics.TectonicPlateGenerator.TectonicPlate;
import net.starlight.terradyne.planet.terrain.octave.ContinentalFoundationOctave;
import net.starlight.terradyne.planet.terrain.octave.OctaveConfiguration;
import net.starlight.terradyne.planet.terrain.octave.OctaveContext;
import net.starlight.terradyne.planet.terrain.octave.OctaveRegistry;

/**
 * Foundation pass for physics-based terrain
 * Combines tectonic plate elevations with continental noise
 */
public class PhysicsFoundationPass implements IGenerationPass {
    
    @Override
    public void applyPass(Chunk chunk, IBiomeType biome, OctaveContext context, PassConfiguration config) {
        // Get configuration
        BlockState blockType = config.getBlockState("blockType", Blocks.STONE.getDefaultState());
        BlockState bedrockBlock = config.getBlockState("bedrockBlock", Blocks.BEDROCK.getDefaultState());
        int seaLevel = config.getInt("seaLevel", 64);
        boolean useContinentalNoise = config.getBoolean("useContinentalNoise", true);
        boolean visualizePlates = config.getBoolean("visualizePlates", false);
        
        // Check if we have a physics model
        if (!(context.getPlanetModel() instanceof PlanetPhysicsModel)) {
            // Fallback for non-physics planets
            generateFallbackTerrain(chunk, blockType, seaLevel);
            return;
        }
        
        PlanetPhysicsModel physicsModel = (PlanetPhysicsModel) context.getPlanetModel();
        
        // Get continental noise octave if enabled
        ContinentalFoundationOctave continentalOctave = null;
        if (useContinentalNoise) {
            continentalOctave = (ContinentalFoundationOctave) OctaveRegistry.getOctave(ContinentalFoundationOctave.class);
        }
        
        // Generate terrain combining plates and continental noise
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunk.getPos().getStartX() + x;
                int worldZ = chunk.getPos().getStartZ() + z;
                
                // Get the tectonic plate at this position
                TectonicPlate plate = physicsModel.getTectonicPlateAt(worldX, worldZ);
                
                // Start with plate base elevation
                double baseElevation = plate.getBaseElevation();
                
                // Add continental noise if enabled
                double continentalHeight = 0.0;
                if (continentalOctave != null) {
                    OctaveConfiguration continentalConfig = new OctaveConfiguration(ContinentalFoundationOctave.class)
                            .withParameter("continentalScale", config.getDouble("continentalScale", 40.0))
                            .withParameter("oceanicDepth", config.getDouble("oceanicDepth", -30.0))
                            .withParameter("continentalHeight", config.getDouble("continentalHeight", 20.0));
                    
                    continentalHeight = continentalOctave.generateHeightContribution(
                            worldX, worldZ, context, continentalConfig);
                }
                
                // Combine plate elevation with continental noise
                int terrainHeight = seaLevel + (int)(baseElevation + continentalHeight);
                
                // Ensure minimum height
                terrainHeight = Math.max(chunk.getBottomY() + 5, terrainHeight);
                
                // Determine block type
                BlockState terrainBlock = blockType;
                if (visualizePlates) {
                    // Use different blocks for different plate types for visualization
                    terrainBlock = getBlockForPlateType(plate.getPlateType(), blockType);
                }
                
                // Fill terrain column
                fillTerrainColumn(chunk, x, z, terrainHeight, terrainBlock, bedrockBlock);
            }
        }
    }
    
    /**
     * Fill a terrain column with appropriate blocks
     */
    private void fillTerrainColumn(Chunk chunk, int x, int z, int surfaceHeight, 
                                   BlockState terrainBlock, BlockState bedrockBlock) {
        // Place bedrock at bottom
        for (int y = chunk.getBottomY(); y < chunk.getBottomY() + 3; y++) {
            chunk.setBlockState(new BlockPos(x, y, z), bedrockBlock, false);
        }
        
        // Fill with terrain blocks
        for (int y = chunk.getBottomY() + 3; y <= surfaceHeight && y < 256; y++) {
            BlockPos pos = new BlockPos(x, y, z);
            
            // Use different block near surface for visual variety
            if (y > surfaceHeight - 3) {
                chunk.setBlockState(pos, terrainBlock, false);
            } else {
                // Deeper blocks - use stone
                chunk.setBlockState(pos, Blocks.STONE.getDefaultState(), false);
            }
        }
    }
    
    /**
     * Get different block types for different plate types (for visualization)
     */
    private BlockState getBlockForPlateType(int plateType, BlockState defaultBlock) {
        return switch (plateType) {
            case 1 -> Blocks.DIORITE.getDefaultState();        // Light colored
            case 2 -> Blocks.STONE.getDefaultState();          // Medium
            case 3 -> Blocks.DEEPSLATE.getDefaultState();     // Dark colored
            default -> defaultBlock;
        };
    }
    
    /**
     * Fallback terrain generation
     */
    private void generateFallbackTerrain(Chunk chunk, BlockState blockType, int seaLevel) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = chunk.getBottomY(); y <= seaLevel; y++) {
                    chunk.setBlockState(new BlockPos(x, y, z), blockType, false);
                }
            }
        }
    }
    
    @Override
    public int getPassPriority() {
        return 0; // Foundation pass - runs first
    }
    
    @Override
    public String getPassName() {
        return "PhysicsFoundation";
    }
    
    @Override
    public String getParameterDocumentation() {
        return """
            Physics Foundation Pass Parameters:
            - blockType (BlockState, default STONE): Block type for terrain
            - seaLevel (int, default 64): Base sea level
            
            Places blocks based on tectonic plate elevations.
            Different plate types use different block colors for visualization.
            """;
    }
}