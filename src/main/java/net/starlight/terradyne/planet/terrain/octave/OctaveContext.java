package net.starlight.terradyne.planet.terrain.octave;

import net.starlight.terradyne.planet.biome.IBiomeType;
import net.starlight.terradyne.planet.physics.IPlanetModel;
import net.starlight.terradyne.planet.terrain.MasterNoiseProvider;

/**
 * Context passed to generation passes and octaves
 * Contains shared data for terrain generation
 */
public class OctaveContext {
    
    private final IPlanetModel planetModel;
    private final IBiomeType currentBiome;
    private final MasterNoiseProvider noiseProvider;
    private final double baseFoundationHeight;
    
    public OctaveContext(IPlanetModel planetModel, IBiomeType biome,
                         MasterNoiseProvider noiseProvider, double baseHeight) {
        this.planetModel = planetModel;
        this.currentBiome = biome;
        this.noiseProvider = noiseProvider;
        this.baseFoundationHeight = baseHeight;
    }
    
    // Getters
    public IPlanetModel getPlanetModel() { 
        return planetModel; 
    }
    
    public IBiomeType getCurrentBiome() { 
        return currentBiome; 
    }
    
    public MasterNoiseProvider getNoiseProvider() { 
        return noiseProvider; 
    }
    
    public double getBaseFoundationHeight() { 
        return baseFoundationHeight; 
    }
}