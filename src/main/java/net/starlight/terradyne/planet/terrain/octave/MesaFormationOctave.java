package net.starlight.terradyne.planet.terrain.octave;

import net.starlight.terradyne.planet.PlanetType;
import net.starlight.terradyne.planet.terrain.MasterNoiseProvider;
import net.starlight.terradyne.planet.terrain.OctaveConfiguration;
import net.starlight.terradyne.planet.terrain.OctaveContext;

import java.util.Set;

/**
 * PHYSICS: Mesa Formation Octave  
 * Simulates geological uplift and mesa formation
 * Returns height contributions where mesas should form
 */
public class MesaFormationOctave implements IUnifiedOctave {

    @Override
    public double generateHeightContribution(int x, int z, OctaveContext context, OctaveConfiguration config) {
        MasterNoiseProvider noise = context.getNoiseProvider();

        // Configuration matching original MesaOctave
        double mesaHeight = config.getDouble("mesaHeight", 60.0);
        double plateauFrequency = config.getDouble("plateauFrequency", 0.008);
        double steepness = config.getDouble("steepness", 4.0);
        double erosionIntensity = config.getDouble("erosionIntensity", 0.6);
        double layering = config.getDouble("layering", 0.8);

        // === STEP 1: CREATE MESA PLATEAU PATTERN (original algorithm) ===
        double mesaPlacement = noise.sampleAt(x * plateauFrequency * 0.6, 0, z * plateauFrequency * 0.4);
        double secondaryMesas = noise.sampleAt(x * plateauFrequency * 1.2, 0, z * plateauFrequency * 0.8) * 0.7;
        double combinedMesaPattern = mesaPlacement + secondaryMesas * 0.6;

        // === STEP 2: CREATE SHARP PLATEAU EDGES (original) ===
        double plateauMask = Math.pow(Math.max(0.0, combinedMesaPattern + 0.4), steepness);
        plateauMask = Math.min(1.0, plateauMask);

        // === STEP 3: ADD GEOLOGICAL LAYERING (original) ===
        double layerNoise = noise.sampleAt(x * plateauFrequency * 8.0, 0, z * plateauFrequency * 8.0);
        double layers = Math.sin(layerNoise * 12.0) * layering;
        double layerStrength = plateauMask * (1.0 - plateauMask) * 4.0;
        double layeredHeight = mesaHeight * plateauMask + layers * layerStrength;

        // === STEP 4: ADD MESA TOP VARIATION (original) ===
        double topVariation = 0.0;
        if (plateauMask > 0.8) {
            topVariation = noise.sampleAt(x * plateauFrequency * 4.0, 0, z * plateauFrequency * 4.0) * 2.0;
        }

        // === STEP 5: EROSION AND WEATHERING (original) ===
        double erosionPattern = noise.sampleRidge(x * plateauFrequency * 3.0, 0, z * plateauFrequency * 3.0);
        double erosionAmount = 0.0;
        if (plateauMask > 0.1 && plateauMask < 0.9) {
            double sideErosionFactor = Math.sin(plateauMask * Math.PI);
            erosionAmount = erosionPattern * erosionIntensity * mesaHeight * 0.3 * sideErosionFactor;
        }

        // === STEP 6: CREATE MESA PEDESTALS (original) ===
        double pedestalNoise = noise.sampleAt(x * plateauFrequency * 6.0, 0, z * plateauFrequency * 6.0);
        double pedestals = 0.0;
        if (plateauMask > 0.05 && plateauMask < 0.3) {
            pedestals = Math.max(0.0, pedestalNoise) * mesaHeight * 0.15;
        }

        // === STEP 7: APPLY MESA MASK (original logic) ===
        if (plateauMask < 0.1) {
            return pedestals; // Only small pedestals, if any
        }

        double mesaContribution = layeredHeight + topVariation - erosionAmount + pedestals;

        // === STEP 8: ADD FINE SURFACE DETAIL TO MESA TOPS (original) ===
        if (plateauMask > 0.7) {
            double rockDetail = noise.sampleAt(x * 0.04, 0, z * 0.04) * 0.8;
            mesaContribution += rockDetail;
        }

        // === STEP 9: SMOOTH TRANSITION AT MESA EDGES (original) ===
        double blendedContribution = mesaContribution * plateauMask;
        return Math.max(0.0, blendedContribution);
    }

    @Override
    public double getPrimaryFrequency() { return 0.008; }

    @Override
    public Set<PlanetType> getSupportedPlanetTypes() {
        return Set.of(PlanetType.DESERT, PlanetType.HOTHOUSE, PlanetType.ROCKY);
    }

    @Override
    public String getOctaveName() { return "MesaFormationPhysics"; }
}