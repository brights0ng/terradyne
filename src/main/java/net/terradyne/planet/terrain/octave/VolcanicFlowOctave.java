package net.terradyne.planet.terrain.octave;

import net.terradyne.planet.PlanetType;
import net.terradyne.planet.terrain.MasterNoiseProvider;
import net.terradyne.planet.terrain.OctaveConfiguration;
import net.terradyne.planet.terrain.UnifiedOctaveContext;

import java.util.Set;

/**
 * Volcanic Flow octave - creates lava flow patterns and volcanic terrain
 */
public class VolcanicFlowOctave implements IUnifiedOctave {

    @Override
    public double generateHeightContribution(int x, int z, UnifiedOctaveContext context,
                                           OctaveConfiguration config) {
        MasterNoiseProvider noise = context.getNoiseProvider();
        
        // Get configuration parameters
        double flowHeight = config.getDouble("flowHeight", 20.0);
        double channelDepth = config.getDouble("channelDepth", 10.0);
        double roughness = config.getDouble("roughness", 0.6);
        
        // Volcanic flow patterns - meandering like lava
        double flowPattern = noise.sampleAt(x * 0.003, 0, z * 0.006);  // Elongated flows
        
        // Flow channels using ridge noise
        double flowChannels = noise.sampleRidge(x * 0.008, 0, z * 0.012);
        
        // Volcanic buildup areas
        double buildup = (noise.sampleAt(x * 0.002, 0, z * 0.002) + 1.0) * 0.5 * flowHeight;
        
        // Rough volcanic surface texture
        double roughSurface = noise.sampleTurbulent(x, z, 3, 0.5) * roughness * flowHeight * 0.3;
        
        // Combine flows - channels carve through buildup
        double flowContribution = buildup + roughSurface;
        
        // Carve channels where flow was strongest
        if (flowChannels < 0.3) {
            flowContribution -= channelDepth * (0.3 - flowChannels) * 3.0;
        }
        
        return flowContribution;
    }

    @Override
    public double getPrimaryFrequency() { return 0.003; }

    @Override
    public Set<PlanetType> getSupportedPlanetTypes() {
        return Set.of(PlanetType.VOLCANIC, PlanetType.HOTHOUSE);
    }

    @Override
    public String getOctaveName() { return "VolcanicFlow"; }
    
    @Override
    public String getParameterDocumentation() {
        return """
            Volcanic Flow Octave Parameters:
            - flowHeight (double, default 20.0): Height of volcanic buildup
            - channelDepth (double, default 10.0): Depth of lava channels
            - roughness (double, default 0.6): Surface roughness intensity
            """;
    }
}