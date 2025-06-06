package net.terradyne.planet.chunk;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.terradyne.planet.PlanetType;
import net.terradyne.planet.biome.DesertBiomeSource;
import net.terradyne.planet.biome.DesertBiomeType;
import net.terradyne.planet.biome.IBiomeType;
import net.terradyne.planet.model.IPlanetModel;
import net.terradyne.planet.terrain.ITerrainGenerator;
import net.terradyne.planet.terrain.TerrainContext;
import net.terradyne.planet.terrain.TerrainData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
public class UniversalChunkGenerator extends ChunkGenerator {
    private final IPlanetModel planetModel;
    private final List<ITerrainGenerator> generators;
    private final NoiseConfig noiseConfig;

    // Static registry of all available generators
    private static final List<ITerrainGenerator> ALL_GENERATORS = new ArrayList<>();

    public static void registerGenerator(ITerrainGenerator generator) {
        ALL_GENERATORS.add(generator);
    }

    public UniversalChunkGenerator(IPlanetModel planetModel, BiomeSource biomeSource, NoiseConfig noiseConfig) {
        super(biomeSource);
        this.planetModel = planetModel;
        this.generators = loadGeneratorsForPlanet(planetModel);
        this.noiseConfig = noiseConfig;
    }

    private List<ITerrainGenerator> loadGeneratorsForPlanet(IPlanetModel model) {
        List<ITerrainGenerator> applicable = new ArrayList<>();

        for (ITerrainGenerator generator : ALL_GENERATORS) {
            if (generator.getSupportedPlanetTypes().contains(model.getType())) {
                applicable.add(generator);
            }
        }

        applicable.sort(Comparator.comparingInt(ITerrainGenerator::getPriority));
        return applicable;
    }

    @Override
    public CompletableFuture<Chunk> populateNoise(Executor executor, Blender blender,
                                                  NoiseConfig noiseConfig, StructureAccessor structureAccessor,
                                                  Chunk chunk) {
        return CompletableFuture.supplyAsync(() -> {
            generateUniversalTerrain(chunk, noiseConfig);
            return chunk;
        }, executor);
    }

    private void generateUniversalTerrain(Chunk chunk, NoiseConfig noiseConfig) {
        ChunkPos chunkPos = chunk.getPos();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkPos.getStartX() + x;
                int worldZ = chunkPos.getStartZ() + z;

                // Get biome type directly from our biome source
                IBiomeType biomeType = getBiomeTypeAt(worldX, worldZ);

                // Create context
                TerrainContext context = new TerrainContext(planetModel, chunkPos, biomeType);

                // Apply generators
                TerrainData finalTerrain = null;
                for (ITerrainGenerator generator : generators) {
                    if (generator.appliesToBiome(biomeType)) {
                        TerrainData generatorResult = generator.generateTerrain(worldX, worldZ, context);

                        if (finalTerrain == null) {
                            finalTerrain = new TerrainData(generatorResult.baseHeight,
                                    generatorResult.surfaceHeight, generatorResult.primaryBlock,
                                    generatorResult.surfaceBlock);
                        } else {
                            blendTerrain(finalTerrain, generatorResult, generator.getPriority());
                        }
                    }
                }

                if (finalTerrain != null) {
                    fillTerrainColumn(chunk, x, z, finalTerrain);
                }
            }
        }
    }

    // Simplified biome type getter
    private IBiomeType getBiomeTypeAt(int worldX, int worldZ) {
        if (biomeSource instanceof DesertBiomeSource desertSource) {
            return desertSource.getBiomeTypeAt(worldX, worldZ);
        }

        // Fallback based on planet type
        return switch (planetModel.getType()) {
            case DESERT -> DesertBiomeType.DUNE_SEA;
            // Add other planet types as you implement them
            default -> DesertBiomeType.DUNE_SEA; // Temporary fallback
        };
    }

    private void blendTerrain(TerrainData base, TerrainData overlay, int priority) {
        // Simple priority-based blending
        if (priority > 150) {
            // High priority - mostly override
            base.blendWith(overlay, 0.8f);
        } else if (priority > 100) {
            // Medium priority - balanced blend
            base.blendWith(overlay, 0.5f);
        } else {
            // Low priority - light blend
            base.blendWith(overlay, 0.2f);
        }
    }

    private void fillTerrainColumn(Chunk chunk, int x, int z, TerrainData terrain) {
        // Fill from bottom to top
        for (int y = chunk.getBottomY(); y <= terrain.surfaceHeight + 5; y++) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState blockState;

            if (y <= terrain.baseHeight) {
                // Below base = bedrock/foundation
                blockState = getBedrockBlock();
            } else if (y <= terrain.surfaceHeight) {
                // Between base and surface = primary terrain
                blockState = terrain.primaryBlock;
            } else if (y == terrain.surfaceHeight + 1) {
                // Surface layer
                blockState = terrain.surfaceBlock;
            } else {
                // Above surface = air (or liquid if specified)
                if (terrain.hasLiquid() && y <= terrain.liquidLevel) {
                    blockState = terrain.liquidBlock;
                } else {
                    blockState = Blocks.AIR.getDefaultState();
                }
            }

            if (blockState != Blocks.AIR.getDefaultState()) {
                chunk.setBlockState(pos, blockState, false);
            }
        }
    }

    private BlockState getBedrockBlock() {
        return switch (planetModel.getType()) {
            case VOLCANIC -> Blocks.BASALT.getDefaultState();
            case ICY -> Blocks.PACKED_ICE.getDefaultState();
            case DESERT -> Blocks.SANDSTONE.getDefaultState();
            default -> Blocks.DEEPSLATE.getDefaultState();
        };
    }

    @Override
    protected Codec<? extends ChunkGenerator> getCodec() {
        return null;
    }

    @Override
    public void carve(ChunkRegion chunkRegion, long seed, NoiseConfig noiseConfig, BiomeAccess biomeAccess, StructureAccessor structureAccessor, Chunk chunk, GenerationStep.Carver carverStep) {

    }

    @Override
    public void buildSurface(ChunkRegion region, StructureAccessor structures, NoiseConfig noiseConfig, Chunk chunk) {

    }

    @Override
    public void populateEntities(ChunkRegion region) {

    }

    @Override
    public int getWorldHeight() {
        return 384; // Standard world height
    }

    @Override
    public int getSeaLevel() {
        return switch (planetModel.getType()) {
            case OCEANIC -> 80;
            case DESERT -> 50;
            default -> 63;
        };
    }

    @Override
    public int getMinimumY() {
        return -64; // Standard minimum Y
    }

    @Override
    public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
        return 0;
    }

    @Override
    public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
        return null;
    }

    @Override
    public void getDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {

    }
}