package net.starlight.terradyne.planet.terrain.octave;

import net.starlight.terradyne.planet.PlanetType;
import net.starlight.terradyne.planet.terrain.MasterNoiseProvider;
import net.starlight.terradyne.planet.terrain.OctaveConfiguration;
import net.starlight.terradyne.planet.terrain.OctaveContext;

import java.util.Set;

/**
 * PHYSICS: Salt Deposition Octave
 * Simulates evaporation and salt crystal formation
 * Returns values indicating salt formation potential
 */
public class SaltDepositionOctave implements IUnifiedOctave {

    @Override
    public double generateHeightContribution(int x, int z, OctaveContext context, OctaveConfiguration config) {
        MasterNoiseProvider noise = context.getNoiseProvider();

        // Configuration matching original GeometricSaltPass
        int cellSize = config.getInt("cellSize", 6);
        double crackWidth = config.getDouble("crackWidth", 1.5);

        // Calculate distance to nearest cell center (original algorithm)
        double minDistance = calculateMinDistanceToCellCenter(x, z, cellSize, context);

        // Return inverse distance - closer to cell edge = higher salt formation
        // This will be used by placement pass to determine where to place salt
        if (minDistance < crackWidth) {
            return 1.0 - (minDistance / crackWidth); // 0-1 scale, 1 = crack center
        }

        return 0.0; // No salt formation
    }

    /**
     * Original cellular pattern calculation
     */
    private double calculateMinDistanceToCellCenter(int worldX, int worldZ, int cellSize, OctaveContext context) {
        double minDistance = Double.MAX_VALUE;

        // Check nearby cells (3x3 grid) - original algorithm
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int cellX = (worldX / cellSize) + dx;
                int cellZ = (worldZ / cellSize) + dz;

                double centerX = (cellX + 0.5) * cellSize;
                double centerZ = (cellZ + 0.5) * cellSize;

                // Add noise offset (original)
                double offsetX = context.getNoiseProvider().sampleAt(cellX * 0.1, 0, cellZ * 0.1) * cellSize * 0.3;
                double offsetZ = context.getNoiseProvider().sampleAt(cellX * 0.1, 1, cellZ * 0.1) * cellSize * 0.3;

                centerX += offsetX;
                centerZ += offsetZ;

                double distance = Math.sqrt(
                        (worldX - centerX) * (worldX - centerX) +
                                (worldZ - centerZ) * (worldZ - centerZ)
                );

                minDistance = Math.min(minDistance, distance);
            }
        }

        return minDistance;
    }

    @Override
    public double getPrimaryFrequency() { return 0.02; }

    @Override
    public Set<PlanetType> getSupportedPlanetTypes() {
        return Set.of(PlanetType.DESERT, PlanetType.HOTHOUSE);
    }
    
    @Override
    public String getOctaveName() { return "SaltDepositionPhysics"; }
}