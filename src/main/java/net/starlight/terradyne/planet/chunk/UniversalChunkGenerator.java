package net.starlight.terradyne.planet.chunk;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.starlight.terradyne.Terradyne;
import net.starlight.terradyne.planet.biome.IBiomeType;
import net.starlight.terradyne.planet.biome.PhysicsBiomeSource;
import net.starlight.terradyne.planet.physics.IPlanetModel;
import net.starlight.terradyne.planet.terrain.MasterNoiseProvider;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Universal chunk generator for all planet types
 * Uses pass-based generation system
 */
public class UniversalChunkGenerator extends ChunkGenerator {
    
    public static final Codec<UniversalChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource)
            ).apply(instance, (biomeSource) -> new UniversalChunkGenerator(null, biomeSource))
    );
    
    private final IPlanetModel planetModel;
    private final MasterNoiseProvider masterNoiseProvider;
    
    public UniversalChunkGenerator(IPlanetModel planetModel, BiomeSource biomeSource) {
        super(biomeSource);
        this.planetModel = planetModel;
        
        if (planetModel != null) {
            // Create the master noise provider
            this.masterNoiseProvider = new MasterNoiseProvider(planetModel.getConfig().getSeed());
            
            // Initialize registries

            Terradyne.LOGGER.info("=== UNIVERSAL CHUNK GENERATOR INITIALIZED ===");
            Terradyne.LOGGER.info("Planet: {}", planetModel.getConfig().getPlanetName());
            Terradyne.LOGGER.info("Type: {}", planetModel.getType().getDisplayName());
            Terradyne.LOGGER.info("Generation: Pass-based");
            
        } else {
            // Codec constructor
            this.masterNoiseProvider = null;
        }
    }
    
    @Override
    protected Codec<? extends ChunkGenerator> getCodec() {
        return CODEC;
    }
    
    @Override
    public CompletableFuture<Chunk> populateNoise(Executor executor, Blender blender,
                                                  NoiseConfig noiseConfig, StructureAccessor structureAccessor,
                                                  Chunk chunk) {
        return CompletableFuture.supplyAsync(() -> {
            generateTerrain(chunk);
            return chunk;
        }, executor);
    }
    
    /**
     * Main terrain generation method
     */
    private void generateTerrain(Chunk chunk) {
        if (planetModel == null || masterNoiseProvider == null) {
            generateFallbackTerrain(chunk);
            return;
        }
        
        try {
            // Get biome for this chunk
            ChunkPos chunkPos = chunk.getPos();
            int centerX = chunkPos.getStartX() + 8;
            int centerZ = chunkPos.getStartZ() + 8;
//            IBiomeType biomeType = getBiomeTypeAt(centerX, centerZ);
            
//            // Create context
//            OctaveContext context = new OctaveContext(
//                    planetModel,
//                    biomeType,
//                    masterNoiseProvider,
//                    64.0 // Base height
//            );
//
//            // Get configured passes for this biome
//            List<PassRegistry.ConfiguredPass> passes = PassRegistry.getConfiguredPassesForBiome(biomeType);
//
//            if (passes.isEmpty()) {
//                Terradyne.LOGGER.warn("No passes configured for biome {} - using fallback", biomeType.getName());
//                generateFallbackTerrain(chunk);
//                return;
//            }
//
//            // Apply each pass in order
//            for (PassRegistry.ConfiguredPass configuredPass : passes) {
//                try {
//                    configuredPass.pass.applyPass(chunk, biomeType, context, configuredPass.config);
//
//                } catch (Exception e) {
//                    Terradyne.LOGGER.error("Error applying pass {} to chunk {}: {}",
//                            configuredPass.pass.getPassName(),
//                            chunkPos,
//                            e.getMessage());
//                    e.printStackTrace();
//                }
//            }
            
        } catch (Exception e) {
            Terradyne.LOGGER.error("Critical error in terrain generation for chunk {}: {}",
                    chunk.getPos(), e.getMessage());
            e.printStackTrace();
            generateFallbackTerrain(chunk);
        }
    }
    
    /**
     * Get biome type at world coordinates
     */
//    private IBiomeType getBiomeTypeAt(int worldX, int worldZ) {
//        try {
//            if (biomeSource instanceof PhysicsBiomeSource physicsBiomeSource) {
//                return physicsBiomeSource.getBiomeTypeAt(worldX, worldZ);
//            }
//
//            // Fallback
////            return net.starlight.terradyne.planet.biome.PhysicsBiomeType.TECTONIC_TEST;
//
//        } catch (Exception e) {
//            Terradyne.LOGGER.warn("Error getting biome type at {},{}: {}", worldX, worldZ, e.getMessage());
////            return net.starlight.terradyne.planet.biome.PhysicsBiomeType.TECTONIC_TEST;
//        }
//    }
    
    /**
     * Fallback terrain generation
     */
    private void generateFallbackTerrain(Chunk chunk) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = chunk.getBottomY(); y <= 64; y++) {
                    chunk.setBlockState(new BlockPos(x, y, z), Blocks.STONE.getDefaultState(), false);
                }
            }
        }
    }
    
    // === REQUIRED CHUNK GENERATOR METHODS ===
    
    @Override
    public void carve(ChunkRegion chunkRegion, long seed, NoiseConfig noiseConfig, BiomeAccess biomeAccess, 
                      StructureAccessor structureAccessor, Chunk chunk, GenerationStep.Carver carverStep) {
        // No carving for now
    }
    
    @Override
    public void buildSurface(ChunkRegion region, StructureAccessor structures, NoiseConfig noiseConfig, Chunk chunk) {
        // Surface building handled by passes
    }
    
    @Override
    public void populateEntities(ChunkRegion region) {
        // No entity population for now
    }
    
    @Override
    public int getWorldHeight() {
        return 384;
    }
    
    @Override
    public int getSeaLevel() {
        return 64;
    }
    
    @Override
    public int getMinimumY() {
        return -64;
    }
    
    @Override
    public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
        // Simple height calculation for now
        if (planetModel == null) return 64;
        
        // Later this will calculate actual terrain height
        return 64;
    }
    
    @Override
    public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
        // Simple column for now
        BlockState[] column = new BlockState[world.getHeight()];
        
        for (int y = 0; y < world.getHeight(); y++) {
            int worldY = world.getBottomY() + y;
            if (worldY <= 64) {
                column[y] = Blocks.STONE.getDefaultState();
            } else {
                column[y] = Blocks.AIR.getDefaultState();
            }
        }
        
        return new VerticalBlockSample(world.getBottomY(), column);
    }
    
    @Override
    public void getDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {
        text.add("=== PHYSICS-BASED GENERATION ===");
        if (planetModel != null) {
            text.add("Planet: " + planetModel.getConfig().getPlanetName());
            text.add("Type: " + planetModel.getType().getDisplayName() + " (emergent)");
            
            if (planetModel instanceof net.starlight.terradyne.planet.physics.PlanetPhysicsModel physicsModel) {
                var plate = physicsModel.getTectonicPlateAt(pos.getX(), pos.getZ());
                text.add("Plate ID: " + plate.getId());
                text.add("Plate Type: " + plate.getPlateType());
                text.add("Plate Elevation: " + plate.getBaseElevation());
                
                // Add continental noise info
                if (masterNoiseProvider != null) {
                    double continental = masterNoiseProvider.sampleOctaves(
                        pos.getX() * 0.00005, 0, pos.getZ() * 0.00005, 4, 0.6, 2.0
                    );
                    text.add("Continental Noise: " + String.format("%.3f", continental));
                    text.add("Terrain Type: " + (continental > 0 ? "Land" : "Ocean"));
                }
            }
        }
    }
}