package net.terradyne.planet.terrain;

import net.terradyne.planet.biome.IBiomeType;
import net.terradyne.planet.model.IPlanetModel;

/**
 * Updated context for unified octave generation
 * Now uses MasterNoiseProvider instead of individual noise samplers
 */
public class UnifiedOctaveContext {
    private final IPlanetModel planetModel;
    private final IBiomeType currentBiome;
    private final MasterNoiseProvider noiseProvider;    // THE unified noise source
    private final double baseFoundationHeight;          // Base terrain height

    public UnifiedOctaveContext(IPlanetModel planetModel, IBiomeType biome,
                                MasterNoiseProvider noiseProvider, double baseHeight) {
        this.planetModel = planetModel;
        this.currentBiome = biome;
        this.noiseProvider = noiseProvider;
        this.baseFoundationHeight = baseHeight;
    }

    // Getters
    public IPlanetModel getPlanetModel() { return planetModel; }
    public IBiomeType getCurrentBiome() { return currentBiome; }
    public MasterNoiseProvider getNoiseProvider() { return noiseProvider; }
    public double getBaseFoundationHeight() { return baseFoundationHeight; }

    // Convenience methods that delegate to MasterNoiseProvider
    public double sampleMasterNoise(double x, double y, double z) {
        return noiseProvider.sampleAt(x, y, z);
    }

    public double getWindDirection(double x, double z) {
        return noiseProvider.getWindDirection(x, z);
    }

    public double sampleErosion(double x, double y, double z) {
        return noiseProvider.sampleErosion(x, y, z);
    }

    public double sampleRidge(double x, double y, double z) {
        return noiseProvider.sampleRidge(x, y, z);
    }

    public double sampleWindAligned(double x, double z, double windAngle) {
        return noiseProvider.sampleWindAligned(x, z, windAngle);
    }
}