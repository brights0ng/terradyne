package net.terradyne.planet.terrain.octave;

import net.terradyne.planet.PlanetType;
import net.terradyne.planet.terrain.MasterNoiseProvider;
import net.terradyne.planet.terrain.OctaveConfiguration;
import net.terradyne.planet.terrain.UnifiedOctaveContext;

import java.util.Set;

/**
 * Mesa octave - creates flat-topped elevated terrain features
 */
public class MesaOctave implements IUnifiedOctave {

    @Override
    public double generateHeightContribution(int x, int z, UnifiedOctaveContext context,
                                           OctaveConfiguration config) {
        MasterNoiseProvider noise = context.getNoiseProvider();
        
        // Get configuration parameters
        double mesaHeight = config.getDouble("mesaHeight", 35.0);
        double plateauSize = config.getDouble("plateauSize", 0.015);
        double steepness = config.getDouble("steepness", 2.0);
        
        // Sample noise for mesa formation
        double mesaNoise = noise.sampleAt(x * plateauSize, 0, z * plateauSize);
        
        // Create plateau effect - sharp transitions between high and low
        double plateauMask = Math.pow(Math.max(0, mesaNoise + 0.3), steepness);
        
        // Add some variation to mesa tops
        double topVariation = noise.sampleAt(x * plateauSize * 4.0, 0, z * plateauSize * 4.0) * 0.1;
        
        // Mesa sides have erosion channels
        double erosion = 0.0;
        if (plateauMask > 0.1 && plateauMask < 0.9) {
            erosion = noise.sampleRidge(x * 0.008, 0, z * 0.008) * mesaHeight * 0.2;
        }
        
        return (plateauMask * mesaHeight) + (topVariation * mesaHeight) - erosion;
    }

    @Override
    public double getPrimaryFrequency() { return 0.015; }

    @Override
    public Set<PlanetType> getSupportedPlanetTypes() {
        return Set.of(PlanetType.DESERT, PlanetType.HOTHOUSE, PlanetType.ROCKY);
    }

    @Override
    public String getOctaveName() { return "Mesa"; }
    
    @Override
    public String getParameterDocumentation() {
        return """
            Mesa Octave Parameters:
            - mesaHeight (double, default 35.0): Height of mesa formations
            - plateauSize (double, default 0.015): Size/frequency of mesa plateaus
            - steepness (double, default 2.0): Steepness of mesa sides
            """;
    }
}