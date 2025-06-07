package net.terradyne.planet.terrain.octave;

import net.terradyne.planet.PlanetType;
import net.terradyne.planet.terrain.MasterNoiseProvider;
import net.terradyne.planet.terrain.OctaveConfiguration;
import net.terradyne.planet.terrain.OctaveContext;

import java.util.Set;

/**
 * RENAMED from ScrublandOctave - Rolling Terrain Octave
 * 
 * Creates gentle rolling base terrain that mesas and other dramatic features
 * can rise from. This provides the realistic "badlands" foundation that you
 * see underneath dramatic rock formations in real deserts.
 */
public class RollingTerrainOctave implements IUnifiedOctave {

    @Override
    public double generateHeightContribution(int x, int z, OctaveContext context,
                                           OctaveConfiguration config) {
        MasterNoiseProvider noise = context.getNoiseProvider();
        
        // Configuration parameters
        double hillHeight = config.getDouble("hillHeight", 8.0);
        double hillFrequency = config.getDouble("hillFrequency", 0.012);
        double rockOutcropIntensity = config.getDouble("rockOutcropIntensity", 0.3);
        double washDepth = config.getDouble("washDepth", 2.0);
        double undulationStrength = config.getDouble("undulationStrength", 1.0);
        
        // === STEP 1: CREATE GENTLE ROLLING BASE ===
        // Large-scale gentle undulation that mesas can rise from
        double largeRolling = noise.sampleAt(x * hillFrequency * 0.4, 0, z * hillFrequency * 0.6) * hillHeight * 0.7;
        
        // Medium-scale terrain variation
        double mediumRolling = noise.sampleAt(x * hillFrequency * 1.0, 0, z * hillFrequency * 0.8) * hillHeight * 0.4;
        
        // Fine-scale gentle variation
        double fineRolling = noise.sampleAt(x * hillFrequency * 2.5, 0, z * hillFrequency * 2.0) * hillHeight * 0.2;
        
        // Combine for smooth rolling terrain
        double baseRolling = (largeRolling + mediumRolling + fineRolling) * undulationStrength;
        
        // === STEP 2: ADD SCATTERED ROCK OUTCROPS ===
        // Small rocky areas scattered across the rolling terrain
        double outcropNoise = noise.sampleAt(x * hillFrequency * 5.0, 0, z * hillFrequency * 4.0);
        double rockOutcrops = 0.0;
        
        if (outcropNoise > 0.6) {
            // Create small rocky mounds
            double outcropHeight = (outcropNoise - 0.6) / 0.4; // 0-1 range
            rockOutcrops = Math.pow(outcropHeight, 1.5) * hillHeight * rockOutcropIntensity;
        }
        
        // === STEP 3: CREATE SUBTLE DRAINAGE PATTERNS ===
        // Very gentle channels - won't compete with major canyons
        double washPattern = noise.sampleRidge(x * hillFrequency * 3.0, 0, z * hillFrequency * 2.5);
        double washes = 0.0;
        
        if (washPattern < 0.3) {
            // Create very shallow wash channels
            double washIntensity = (0.3 - washPattern) / 0.3;
            washes = -washIntensity * washDepth; // Negative = carve down gently
        }
        
        // === STEP 4: ADD ALLUVIAL DEPOSITS ===
        // Sediment accumulation areas that create gentle mounds
        double depositNoise = noise.sampleAt(x * hillFrequency * 1.8, 0, z * hillFrequency * 2.2);
        double deposits = 0.0;
        
        if (depositNoise > 0.2) {
            deposits = Math.max(0.0, depositNoise - 0.2) * hillHeight * 0.3;
        }
        
        // === STEP 5: COMBINE ALL FEATURES ===
        double totalHeight = baseRolling + rockOutcrops + washes + deposits;
        
        // === STEP 6: ADD FINE SURFACE TEXTURE ===
        // Subtle surface variation
        double surfaceTexture = noise.sampleAt(x * 0.04, 0, z * 0.035) * 0.6;
        totalHeight += surfaceTexture;
        
        return totalHeight;
    }

    @Override
    public double getPrimaryFrequency() { return 0.012; }

    @Override
    public Set<PlanetType> getSupportedPlanetTypes() {
        return Set.of(PlanetType.DESERT, PlanetType.HOTHOUSE, PlanetType.ROCKY);
    }

    @Override
    public String getOctaveName() { return "RollingTerrain"; }
    
    @Override
    public String getParameterDocumentation() {
        return """
            Rolling Terrain Octave Parameters:
            - hillHeight (double, default 8.0): Height of gentle rolling hills
            - hillFrequency (double, default 0.012): Frequency/size of rolling features
            - rockOutcropIntensity (double, default 0.3): Intensity of scattered rock outcrops
            - washDepth (double, default 2.0): Depth of gentle drainage channels
            - undulationStrength (double, default 1.0): Overall strength of rolling terrain
            """;
    }
}