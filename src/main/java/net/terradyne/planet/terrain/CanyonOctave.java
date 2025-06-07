package net.terradyne.planet.terrain;

import net.terradyne.planet.PlanetType;
import net.terradyne.planet.biome.IBiomeType;
import net.terradyne.planet.biome.DesertBiomeType;
import java.util.Set;

/**
 * Canyon octave - carves erosion features using master noise
 * Uses ridge noise patterns to create realistic dendritic canyon networks
 */
public class CanyonOctave implements IUnifiedOctave {
    
    @Override
    public double generateHeightContribution(int x, int z, UnifiedOctaveContext context) {
        // Only carve if sufficient erosion
        if (context.getPlanetModel().getErosionRate() < 0.3f) {
            return 0.0;
        }
        
        // Create dendritic canyon patterns using master noise
        double canyonPattern = generateDendriticPattern(x, z, context);
        
        if (canyonPattern < 0.3) {  // Only carve narrow channels
            double carvingDepth = (0.3 - canyonPattern) / 0.3;  // 0-1 intensity
            return -carvingDepth * context.getPlanetModel().getErosionRate() * 25.0; // Negative = carve down
        }
        
        return 0.0; // No carving here
    }
    
    private double generateDendriticPattern(int x, int z, UnifiedOctaveContext context) {
        // Create branching patterns using master noise at canyon frequency
        
        // Primary flow direction varies across landscape
        double primaryDirection = context.getErosionNoise().sample(x * 0.0002, 0, z * 0.0002) * Math.PI;
        
        // Rotate coordinates to align with flow direction
        double rotatedX = x * Math.cos(primaryDirection) - z * Math.sin(primaryDirection);
        double rotatedZ = x * Math.sin(primaryDirection) + z * Math.cos(primaryDirection);
        
        // Primary ridge (main canyon) using master noise
        double primary = Math.abs(context.getMasterNoise().sample(rotatedX * 0.0012, 0, rotatedZ * 0.0004));
        
        // Secondary ridges (tributaries) at different frequencies
        double secondary1 = Math.abs(context.getMasterNoise().sample(rotatedX * 0.0016, 0, rotatedZ * 0.0008)) * 0.7;
        double secondary2 = Math.abs(context.getMasterNoise().sample(rotatedX * 0.0008, 0, rotatedZ * 0.0012)) * 0.5;
        
        // Create dendritic network
        return Math.min(primary, Math.min(secondary1, secondary2));
    }
    
    @Override
    public double getPrimaryFrequency() { return 0.001; }
    
    @Override
    public UnifiedOctaveType getOctaveType() { return UnifiedOctaveType.CARVING; }
    
    @Override
    public Set<PlanetType> getSupportedPlanetTypes() {
        return Set.of(PlanetType.DESERT, PlanetType.ROCKY, PlanetType.VOLCANIC, PlanetType.OCEANIC);
    }
    
    @Override
    public boolean appliesToBiome(IBiomeType biome) {
        if (biome instanceof DesertBiomeType desertBiome) {
            return switch (desertBiome) {
                case GRANITE_MESAS, LIMESTONE_CANYONS, SALT_FLATS, VOLCANIC_WASTELAND, SCRUBLAND -> true;
                case DUNE_SEA, SCORCHING_WASTE, DUST_BOWL -> false;
            };
        }
        return true; // Other planet types can have canyons
    }
    
    @Override
    public String getOctaveName() { return "Canyons"; }
}