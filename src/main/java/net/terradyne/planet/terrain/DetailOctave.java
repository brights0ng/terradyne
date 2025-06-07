package net.terradyne.planet.terrain;

import net.terradyne.planet.PlanetType;
import net.terradyne.planet.biome.IBiomeType;
import java.util.Set;

/**
 * Detail octave - adds fine surface texture using master noise
 * High-frequency sampling for surface roughness and small features
 */
public class DetailOctave implements IUnifiedOctave {
    
    @Override
    public double generateHeightContribution(int x, int z, UnifiedOctaveContext context) {
        // Fine surface texture using master noise at high frequency
        double fineTexture1 = context.getMasterNoise().sample(x * 0.03, 0, z * 0.03) * 2;
        double fineTexture2 = context.getMasterNoise().sample(x * 0.06, 0, z * 0.06) * 1;
        double microTexture = context.getMasterNoise().sample(x * 0.12, 0, z * 0.12) * 0.5;
        
        return fineTexture1 + fineTexture2 + microTexture;
    }
    
    @Override
    public double getPrimaryFrequency() { return 0.05; }
    
    @Override
    public UnifiedOctaveType getOctaveType() { return UnifiedOctaveType.FINE_DETAILS; }
    
    @Override
    public Set<PlanetType> getSupportedPlanetTypes() {
        return Set.of(PlanetType.values()); // All planets can have surface details
    }
    
    @Override
    public boolean appliesToBiome(IBiomeType biome) {
        return true; // All biomes can have fine surface details
    }
    
    @Override
    public String getOctaveName() { return "Surface Details"; }
}