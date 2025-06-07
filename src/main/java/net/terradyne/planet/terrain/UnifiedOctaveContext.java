package net.terradyne.planet.terrain;

import net.minecraft.util.math.noise.SimplexNoiseSampler;
import net.terradyne.planet.biome.IBiomeType;
import net.terradyne.planet.model.IPlanetModel;

/**
 * Context for unified octave generation
 * Provides access to shared noise samplers and planet information
 */
public class UnifiedOctaveContext {
    private final IPlanetModel planetModel;
    private final IBiomeType currentBiome;
    private final SimplexNoiseSampler masterNoise;           // THE shared noise sampler
    private final SimplexNoiseSampler windDirectionNoise;    // For wind-aligned features
    private final SimplexNoiseSampler erosionNoise;          // For erosion patterns
    private final double baseFoundationHeight;               // Base terrain height
    
    public UnifiedOctaveContext(IPlanetModel planetModel, IBiomeType biome, 
                               SimplexNoiseSampler masterNoise, SimplexNoiseSampler windNoise,
                               SimplexNoiseSampler erosionNoise, double baseHeight) {
        this.planetModel = planetModel;
        this.currentBiome = biome;
        this.masterNoise = masterNoise;
        this.windDirectionNoise = windNoise;
        this.erosionNoise = erosionNoise;
        this.baseFoundationHeight = baseHeight;
    }
    
    // Getters
    public IPlanetModel getPlanetModel() { return planetModel; }
    public IBiomeType getCurrentBiome() { return currentBiome; }
    public SimplexNoiseSampler getMasterNoise() { return masterNoise; }
    public SimplexNoiseSampler getWindDirectionNoise() { return windDirectionNoise; }
    public SimplexNoiseSampler getErosionNoise() { return erosionNoise; }
    public double getBaseFoundationHeight() { return baseFoundationHeight; }
}