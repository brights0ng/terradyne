package net.starlight.terradyne.planet.terrain;

import net.minecraft.util.math.noise.SimplexNoiseSampler;
import net.minecraft.util.math.random.Random;

/**
 * Master noise provider for the entire planet
 * All terrain generation uses this single noise source
 * Provides sophisticated multi-octave noise for natural terrain
 */
public class MasterNoiseProvider {
    
    private final SimplexNoiseSampler masterNoise;
    private final SimplexNoiseSampler detailNoise;
    private final SimplexNoiseSampler erosionNoise;
    private final SimplexNoiseSampler temperatureNoise;
    private final SimplexNoiseSampler ridgeNoise;
    private final SimplexNoiseSampler turbulenceNoise;
    private final long seed;
    
    public MasterNoiseProvider(long seed) {
        this.seed = seed;
        Random random = Random.create(seed);
        
        // Create noise samplers with different seeds
        this.masterNoise = new SimplexNoiseSampler(random);
        this.detailNoise = new SimplexNoiseSampler(Random.create(seed + 1000));
        this.erosionNoise = new SimplexNoiseSampler(Random.create(seed + 2000));
        this.temperatureNoise = new SimplexNoiseSampler(Random.create(seed + 3000));
        this.ridgeNoise = new SimplexNoiseSampler(Random.create(seed + 4000));
        this.turbulenceNoise = new SimplexNoiseSampler(Random.create(seed + 5000));
    }
    
    /**
     * Sample the master noise at given coordinates
     * This is the primary noise function all terrain should use
     */
    public double sampleAt(double x, double y, double z) {
        return masterNoise.sample(x, y, z);
    }
    
    /**
     * Sample with multiple octaves for more natural terrain
     * @param octaves Number of noise layers to combine
     * @param persistence How much each octave contributes (0.5 = each octave half the previous)
     * @param lacunarity Frequency multiplier between octaves (2.0 = double frequency each octave)
     */
    public double sampleOctaves(double x, double y, double z, int octaves, double persistence, double lacunarity) {
        double total = 0.0;
        double amplitude = 1.0;
        double frequency = 1.0;
        double maxValue = 0.0;
        
        for (int i = 0; i < octaves; i++) {
            total += sampleAt(x * frequency, y * frequency, z * frequency) * amplitude;
            maxValue += amplitude;
            amplitude *= persistence;
            frequency *= lacunarity;
        }
        
        return total / maxValue; // Normalize to [-1, 1]
    }
    
    /**
     * Sample detail noise for fine features
     */
    public double sampleDetail(double x, double y, double z) {
        return detailNoise.sample(x, y, z);
    }
    
    /**
     * Sample erosion patterns
     */
    public double sampleErosion(double x, double y, double z) {
        return erosionNoise.sample(x, y, z);
    }
    
    /**
     * Sample temperature variations
     */
    public double sampleTemperature(double x, double z) {
        return temperatureNoise.sample(x * 0.0001, 0, z * 0.0001);
    }
    
    /**
     * Create ridge noise (absolute value for ridges/valleys)
     */
    public double sampleRidge(double x, double y, double z) {
        return Math.abs(ridgeNoise.sample(x, y, z));
    }
    
    /**
     * Create turbulence noise (absolute value of noise)
     * Good for rough, chaotic terrain
     */
    public double sampleTurbulence(double x, double y, double z, int octaves) {
        double total = 0.0;
        double amplitude = 1.0;
        double frequency = 1.0;
        
        for (int i = 0; i < octaves; i++) {
            total += Math.abs(turbulenceNoise.sample(x * frequency, y * frequency, z * frequency)) * amplitude;
            amplitude *= 0.5;
            frequency *= 2.0;
        }
        
        return total;
    }
    
    /**
     * Create billowy noise (squared noise for puffy features)
     * Good for clouds, sand dunes, rolling hills
     */
    public double sampleBillowy(double x, double y, double z) {
        double value = sampleAt(x, y, z);
        return value * value * Math.signum(value);
    }
    
    /**
     * Create terraced noise (stepped terrain)
     * Good for mesa-like formations
     */
    public double sampleTerraced(double x, double y, double z, int terraces) {
        double value = sampleAt(x, y, z);
        return Math.round(value * terraces) / (double)terraces;
    }
    
    /**
     * Get seed
     */
    public long getSeed() { 
        return seed; 
    }
}