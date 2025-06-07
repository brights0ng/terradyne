package net.terradyne.planet.terrain.octave;

import net.terradyne.planet.PlanetType;
import net.terradyne.planet.terrain.MasterNoiseProvider;
import net.terradyne.planet.terrain.OctaveConfiguration;
import net.terradyne.planet.terrain.OctaveContext;

import java.util.Set;

/**
 * COMPLETELY REWRITTEN Mesa Octave - Creates dramatic flat-topped rock formations
 *
 * This octave creates the iconic flat-topped mesas with steep sides that you'd
 * see in places like Monument Valley. Uses multiple noise layers to create
 * realistic geological stratification.
 */
public class MesaOctave implements IUnifiedOctave {

    @Override
    public double generateHeightContribution(int x, int z, OctaveContext context,
                                             OctaveConfiguration config) {
        MasterNoiseProvider noise = context.getNoiseProvider();

        // Configuration parameters
        double mesaHeight = config.getDouble("mesaHeight", 60.0);
        double plateauFrequency = config.getDouble("plateauFrequency", 0.008);
        double steepness = config.getDouble("steepness", 4.0);
        double erosionIntensity = config.getDouble("erosionIntensity", 0.6);
        double layering = config.getDouble("layering", 0.8);

        // === STEP 1: CREATE MESA PLATEAU PATTERN ===
        // Large-scale mesa placement - determines where mesas exist
        double mesaPlacement = noise.sampleAt(x * plateauFrequency * 0.6, 0, z * plateauFrequency * 0.4);

        // Secondary mesa pattern for more variety
        double secondaryMesas = noise.sampleAt(x * plateauFrequency * 1.2, 0, z * plateauFrequency * 0.8) * 0.7;

        // Combine patterns - creates irregular mesa shapes
        double combinedMesaPattern = mesaPlacement + secondaryMesas * 0.6;

        // === STEP 2: CREATE SHARP PLATEAU EDGES ===
        // Convert smooth noise to sharp plateau edges using power function
        double plateauMask = Math.pow(Math.max(0.0, combinedMesaPattern + 0.4), steepness);
        plateauMask = Math.min(1.0, plateauMask);

        // === STEP 3: ADD GEOLOGICAL LAYERING ===
        // Simulate sedimentary rock layers - mesas often show distinct strata
        double layerNoise = noise.sampleAt(x * plateauFrequency * 8.0, 0, z * plateauFrequency * 8.0);
        double layers = Math.sin(layerNoise * 12.0) * layering;

        // Apply layering only to mesa sides (not flat tops)
        double layerStrength = plateauMask * (1.0 - plateauMask) * 4.0; // Peaks at mesa edges
        double layeredHeight = mesaHeight * plateauMask + layers * layerStrength;

        // === STEP 4: ADD MESA TOP VARIATION ===
        // Flat mesa tops should have some subtle variation
        double topVariation = 0.0;
        if (plateauMask > 0.8) {
            topVariation = noise.sampleAt(x * plateauFrequency * 4.0, 0, z * plateauFrequency * 4.0) * 2.0;
        }

        // === STEP 5: EROSION AND WEATHERING ===
        // Mesas erode over time, creating talus slopes and carved features
        double erosionPattern = noise.sampleRidge(x * plateauFrequency * 3.0, 0, z * plateauFrequency * 3.0);

        // Apply erosion more heavily to mesa edges
        double erosionAmount = 0.0;
        if (plateauMask > 0.1 && plateauMask < 0.9) {
            // Most erosion happens on the steep sides
            double sideErosionFactor = Math.sin(plateauMask * Math.PI); // Peaks at 0.5 (steepest part)
            erosionAmount = erosionPattern * erosionIntensity * mesaHeight * 0.3 * sideErosionFactor;
        }

        // === STEP 6: CREATE MESA PEDESTALS ===
        // Small rock formations at the base of mesas
        double pedestalNoise = noise.sampleAt(x * plateauFrequency * 6.0, 0, z * plateauFrequency * 6.0);
        double pedestals = 0.0;

        if (plateauMask > 0.05 && plateauMask < 0.3) {
            pedestals = Math.max(0.0, pedestalNoise) * mesaHeight * 0.15;
        }

        // === STEP 7: COMBINE ALL ELEMENTS ===
        double finalHeight = layeredHeight + topVariation - erosionAmount + pedestals;

        // === STEP 8: ADD FINE SURFACE DETAIL TO MESA TOPS ===
        if (plateauMask > 0.7) {
            // Mesa tops should have some rocky texture but remain relatively flat
            double rockDetail = noise.sampleAt(x * 0.04, 0, z * 0.04) * 0.8;
            finalHeight += rockDetail;
        }

        return Math.max(0.0, finalHeight);
    }

    @Override
    public double getPrimaryFrequency() { return 0.008; }

    @Override
    public Set<PlanetType> getSupportedPlanetTypes() {
        return Set.of(PlanetType.DESERT, PlanetType.HOTHOUSE, PlanetType.ROCKY);
    }

    @Override
    public String getOctaveName() { return "DramaticMesa"; }

    @Override
    public String getParameterDocumentation() {
        return """
            Dramatic Mesa Octave Parameters:
            - mesaHeight (double, default 60.0): Maximum height of mesa formations
            - plateauFrequency (double, default 0.008): Size/frequency of mesa plateaus
            - steepness (double, default 4.0): How steep mesa sides are (higher = more dramatic)
            - erosionIntensity (double, default 0.6): Amount of erosion on mesa sides
            - layering (double, default 0.8): Intensity of geological layer visibility
            """;
    }
}