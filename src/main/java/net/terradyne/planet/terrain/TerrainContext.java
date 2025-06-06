package net.terradyne.planet.terrain;

import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.noise.SimplexNoiseSampler;
import net.minecraft.util.math.random.Random;
import net.terradyne.planet.biome.IBiomeType;
import net.terradyne.planet.model.IPlanetModel;

import java.util.HashMap;
import java.util.Map;

// Generic terrain context
public class TerrainContext {
    private final IPlanetModel planetModel;
    private final ChunkPos chunkPos;
    private final IBiomeType currentBiome;
    private final Map<String, SimplexNoiseSampler> noiseSamplers;

    public TerrainContext(IPlanetModel planetModel, ChunkPos chunkPos, IBiomeType biome) {
        this.planetModel = planetModel;
        this.chunkPos = chunkPos;
        this.currentBiome = biome;
        this.noiseSamplers = new HashMap<>();
        initializeNoiseSamplers();
    }

    private void initializeNoiseSamplers() {
        Random random = Random.create(planetModel.getConfig().getSeed());

        // Create noise samplers for common terrain generation
        noiseSamplers.put("base", new SimplexNoiseSampler(random));
        noiseSamplers.put("detail", new SimplexNoiseSampler(random));
        noiseSamplers.put("mesa", new SimplexNoiseSampler(random));
        noiseSamplers.put("canyon", new SimplexNoiseSampler(random));
        noiseSamplers.put("erosion", new SimplexNoiseSampler(random));
        noiseSamplers.put("wind", new SimplexNoiseSampler(random));
        noiseSamplers.put("large_dune", new SimplexNoiseSampler(random));
        noiseSamplers.put("medium_dune", new SimplexNoiseSampler(random));
        noiseSamplers.put("small_dune", new SimplexNoiseSampler(random));
    }

    public IPlanetModel getPlanetModel() { return planetModel; }
    public ChunkPos getChunkPos() { return chunkPos; }
    public IBiomeType getCurrentBiome() { return currentBiome; }

    public SimplexNoiseSampler getNoiseSampler(String name) {
        return noiseSamplers.get(name);
    }
}