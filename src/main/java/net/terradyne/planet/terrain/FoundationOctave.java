package net.terradyne.planet.terrain;

import net.terradyne.planet.PlanetType;
import net.terradyne.planet.biome.IBiomeType;
import java.util.Set;

/**
 * Foundation octave - provides large-scale base terrain
 * Samples master noise at very low frequency for continental-scale features
 */
public class FoundationOctave implements IUnifiedOctave {
    
    @Override
    public double generateHeightContribution(int x, int z, UnifiedOctaveContext context) {
        // Large-scale continental features using master noise
        double largeTerrain = context.getMasterNoise().sample(x * 0.0008, 0, z * 0.0008) * 12;
        double mediumTerrain = context.getMasterNoise().sample(x * 0.002, 0, z * 0.002) * 6;
        
        return largeTerrain + mediumTerrain;
    }
    
    @Override
    public double getPrimaryFrequency() { return 0.001; } // Low frequency = large features
    
    @Override
    public UnifiedOctaveType getOctaveType() { return UnifiedOctaveType.FOUNDATION; }
    
    @Override
    public Set<PlanetType> getSupportedPlanetTypes() {
        return Set.of(PlanetType.values()); // All planets need foundation
    }
    
    @Override
    public boolean appliesToBiome(IBiomeType biome) {
        return true; // All biomes need foundation terrain
    }
    
    @Override
    public String getOctaveName() { return "Foundation"; }
}