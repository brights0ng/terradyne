package net.starlight.terradyne.planet.terrain.octave;

import net.starlight.terradyne.planet.PlanetType;
import net.starlight.terradyne.planet.terrain.MasterNoiseProvider;

import java.util.Set;

/**
 * Continental Foundation Octave
 * Creates broad, natural continental-scale terrain features
 * This provides the base terrain shape that other systems modify
 */
public class ContinentalFoundationOctave implements IUnifiedOctave {
    
    @Override
    public double generateHeightContribution(int x, int z, OctaveContext context,
                                           OctaveConfiguration config) {
        MasterNoiseProvider noise = context.getNoiseProvider();
        
        // Configuration parameters
        double continentalScale = config.getDouble("continentalScale", 40.0);
        double oceanicDepth = config.getDouble("oceanicDepth", -30.0);
        double continentalHeight = config.getDouble("continentalHeight", 20.0);
        
        // === CONTINENTAL SHELF (VERY BROAD) ===
        // This creates continent-sized features using multi-octave noise
        double continental = noise.sampleOctaves(
            x * 0.00005,  // Extremely low frequency for huge features
            0, 
            z * 0.00005,
            4,            // 4 octaves for natural variation
            0.6,          // Persistence (each octave 60% of previous)
            2.0           // Lacunarity (double frequency each octave)
        );
        
        // === REGIONAL VARIATION ===
        // Large-scale height variation within continents
        double regional = noise.sampleOctaves(
            x * 0.0002,
            0,
            z * 0.0002,
            3,
            0.5,
            2.2
        ) * 0.6;
        
        // === SUB-REGIONAL FEATURES ===
        // Medium-scale terrain features with some turbulence
        double subRegional = noise.sampleTurbulence(
            x * 0.0008,
            0,
            z * 0.0008,
            2
        ) * 0.3;
        
        // === LOCAL VARIATION ===
        // Smaller scale height changes
        double local = noise.sampleOctaves(
            x * 0.003,
            0,
            z * 0.003,
            2,
            0.4,
            2.5
        ) * 0.15;
        
        // === DETAIL NOISE ===
        // Fine terrain details
        double detail = noise.sampleDetail(
            x * 0.01,
            0,
            z * 0.01
        ) * 0.05;
        
        // Combine all scales
        double combinedNoise = continental + regional + subRegional + local + detail;
        
        // SMOOTHER TRANSITIONS - Use wider smoothstep range to avoid sharp drops
        // This creates a much wider transition zone between ocean and land
        double shelfEffect = smoothstep(-0.6, 0.6, combinedNoise);
        
        // BLEND heights instead of switching - no more if/else!
        // Ocean depth to continental height is now a smooth gradient
        double baseHeight = lerp(oceanicDepth, continentalHeight, shelfEffect);
        
        // Add terrain variation that scales with elevation
        // This ensures variation doesn't create sharp drops
        double terrainVariation = noise.sampleBillowy(x * 0.001, 0, z * 0.001) * 10.0;
        // Scale variation by how "land-like" we are to keep ocean floor smoother
        baseHeight += terrainVariation * shelfEffect * 0.7;
        
        // SMOOTHER ridge features - gradual transition instead of sharp threshold
        // Use smoothstep for ridge strength instead of hard cutoff
        double ridgeStrength = smoothstep(0.3, 0.8, combinedNoise);
        if (ridgeStrength > 0) {
            double ridges = noise.sampleRidge(x * 0.002, 0, z * 0.002) * 15.0;
            // Multiply by ridge strength for gradual appearance
            baseHeight += ridges * ridgeStrength * 0.6;
        }
        
        // Add some dramatic peaks VERY occasionally
        // This satisfies the "more dramatic terrain SOMETIMES" requirement
        double peakNoise = noise.sampleAt(x * 0.0003, 0, z * 0.0003);
        if (peakNoise > 0.8 && combinedNoise > 0.4) {
            // Rare dramatic peaks
            double peakHeight = (peakNoise - 0.8) * 5.0; // 0 to 1 scaled to 0 to 25
            baseHeight += peakHeight * 25.0 * smoothstep(0.4, 0.7, combinedNoise);
        }
        
        // Ocean floor variation - kept smooth
        if (shelfEffect < 0.5) {
            double oceanVariation = noise.sampleOctaves(x * 0.0005, 0, z * 0.0005, 2, 0.3, 2.0) * 5.0;
            baseHeight += oceanVariation * (1.0 - shelfEffect) * 0.5;
        }
        
        // Apply overall scale
        return baseHeight * (continentalScale / 40.0);
    }
    
    /**
     * Smooth step function for transitions
     */
    private double smoothstep(double edge0, double edge1, double x) {
        double t = Math.max(0, Math.min(1, (x - edge0) / (edge1 - edge0)));
        return t * t * (3.0 - 2.0 * t);
    }
    
    /**
     * Linear interpolation
     */
    private double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
    
    @Override
    public double getPrimaryFrequency() {
        return 0.00005; // Extremely low frequency for continental scale
    }
    
    @Override
    public Set<PlanetType> getSupportedPlanetTypes() {
        return Set.of(PlanetType.values()); // All planets have continental features
    }
    
    @Override
    public String getOctaveName() {
        return "ContinentalFoundation";
    }
    
    @Override
    public String getParameterDocumentation() {
        return """
            Continental Foundation Octave:
            Creates broad continental-scale terrain features.
            
            Parameters:
            - continentalScale (double, default 40.0): Overall terrain scale
            - oceanicDepth (double, default -30.0): Ocean floor depth
            - continentalHeight (double, default 20.0): Continental shelf height
            
            This octave creates:
            - Continent-sized landmasses
            - Ocean basins
            - Regional height variations
            - Natural terrain flow
            """;
    }
}