package net.terradyne.planet.terrain;

import net.minecraft.util.math.noise.SimplexNoiseSampler;
import net.minecraft.util.math.random.Random;

import java.util.HashMap;
import java.util.Map;

public class NoiseConfig {
    private final long seed;
    private final Map<String, SimplexNoiseSampler> noiseSamplers;

    public NoiseConfig(long seed) {
        this.seed = seed;
        this.noiseSamplers = new HashMap<>();
        initializeNoiseSamplers();
    }

    private void initializeNoiseSamplers() {
        Random random = Random.create(seed);

        // Create noise samplers for common uses
        noiseSamplers.put("base", new SimplexNoiseSampler(random));
        noiseSamplers.put("detail", new SimplexNoiseSampler(random));
        noiseSamplers.put("mesa", new SimplexNoiseSampler(random));
        noiseSamplers.put("canyon", new SimplexNoiseSampler(random));
        noiseSamplers.put("erosion", new SimplexNoiseSampler(random));
        noiseSamplers.put("wind", new SimplexNoiseSampler(random));
    }

    public SimplexNoiseSampler getNoiseSampler(String name) {
        return noiseSamplers.get(name);
    }

    public long getSeed() { return seed; }
}