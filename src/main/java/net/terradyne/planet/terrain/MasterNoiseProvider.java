package net.terradyne.planet.terrain;

import net.minecraft.util.math.noise.SimplexNoiseSampler;
import net.minecraft.util.math.random.Random;

/**
 * Master Noise Provider - THE single source of noise for all terrain octaves
 * This ensures smooth, natural terrain without visible noise layer conflicts
 *
 * All octaves sample from the same master noise at different frequencies,
 * preventing the "layered noise" artifacts that occur when each generator
 * has its own independent noise source.
 */
public class MasterNoiseProvider {
    private final SimplexNoiseSampler masterNoise;           // THE shared noise sampler
    private final SimplexNoiseSampler windDirectionNoise;    // For wind-aligned features
    private final SimplexNoiseSampler erosionNoise;          // For erosion patterns
    private final SimplexNoiseSampler temperatureNoise;      // For temperature variations
    private final long seed;

    public MasterNoiseProvider(long seed) {
        this.seed = seed;
        Random random = Random.create(seed);

        // Create all noise samplers from the same base seed but with offsets
        this.masterNoise = new SimplexNoiseSampler(random);
        this.windDirectionNoise = new SimplexNoiseSampler(Random.create(seed + 1000));
        this.erosionNoise = new SimplexNoiseSampler(Random.create(seed + 2000));
        this.temperatureNoise = new SimplexNoiseSampler(Random.create(seed + 3000));
    }

    /**
     * Sample the master noise at given coordinates
     * ALL octaves should use this for consistent terrain
     */
    public double sampleAt(double x, double y, double z) {
        return masterNoise.sample(x, y, z);
    }

    /**
     * Sample master noise along wind-aligned coordinates
     * Useful for wind-directed features like dunes
     */
    public double sampleWindAligned(double x, double z, double windAngle) {
        double windX = x * Math.cos(windAngle) - z * Math.sin(windAngle);
        double windZ = x * Math.sin(windAngle) + z * Math.cos(windAngle);
        return masterNoise.sample(windX, 0, windZ);
    }

    /**
     * Get wind direction at a location (0 to 2π radians)
     */
    public double getWindDirection(double x, double z) {
        return (windDirectionNoise.sample(x * 0.0001, 0, z * 0.0001) + 1.0) * Math.PI;
    }

    /**
     * Sample erosion patterns - useful for canyon/channel generation
     */
    public double sampleErosion(double x, double y, double z) {
        return erosionNoise.sample(x, y, z);
    }

    /**
     * Sample temperature variations across the planet
     */
    public double sampleTemperature(double x, double z) {
        return temperatureNoise.sample(x * 0.0001, 0, z * 0.0001);
    }

    /**
     * Create ridge noise (absolute value) for channel/canyon patterns
     */
    public double sampleRidge(double x, double y, double z) {
        return Math.abs(masterNoise.sample(x, y, z));
    }

    /**
     * Sample with turbulence (multiple octaves of master noise)
     */
    public double sampleTurbulent(double x, double z, int octaves, double persistence) {
        double total = 0.0;
        double amplitude = 1.0;
        double frequency = 1.0;
        double maxValue = 0.0;

        for (int i = 0; i < octaves; i++) {
            total += masterNoise.sample(x * frequency, 0, z * frequency) * amplitude;
            maxValue += amplitude;
            amplitude *= persistence;
            frequency *= 2.0;
        }

        return total / maxValue;
    }

    // Direct access to noise samplers for advanced use cases
    public SimplexNoiseSampler getMasterNoise() { return masterNoise; }
    public SimplexNoiseSampler getWindDirectionNoise() { return windDirectionNoise; }
    public SimplexNoiseSampler getErosionNoise() { return erosionNoise; }
    public SimplexNoiseSampler getTemperatureNoise() { return temperatureNoise; }

    public long getSeed() { return seed; }
}