package net.terradyne.planet.terrain;

import net.terradyne.planet.PlanetType;
import net.terradyne.planet.biome.IBiomeType;
import net.terradyne.planet.biome.DesertBiomeType;
import net.terradyne.planet.config.DesertConfig;
import net.terradyne.planet.model.DesertModel;
import java.util.Set;

/**
 * Dune octave - generates sand dune formations using master noise
 * Samples at multiple frequencies to create realistic dune patterns
 */
public class DuneOctave implements IUnifiedOctave {
    
    @Override
    public double generateHeightContribution(int x, int z, UnifiedOctaveContext context) {
        // Only generate for desert planets
        if (context.getPlanetModel().getType() != PlanetType.DESERT && 
            context.getPlanetModel().getType() != PlanetType.HOTHOUSE) {
            return 0.0;
        }
        
        // Only generate if planet has loose material formations
        if (!context.getPlanetModel().hasLooseMaterialFormations()) {
            return 0.0;
        }
        
        DesertModel desertModel = (DesertModel) context.getPlanetModel();
        DesertConfig config = desertModel.getConfig();
        
        if (!config.hasDunes()) {
            return 0.0;
        }
        
        double maxDuneHeight = Math.max(desertModel.getDuneHeight(), 60.0);
        
        // Use master noise at multiple frequencies for smooth dune patterns
        // Large dune formations (elongated in X direction like in DesertChunkGenerator)
        double largeDunes = (context.getMasterNoise().sample(x * 0.00016, 0, z * 0.0008) + 1.0) * 0.5 *
                maxDuneHeight * 0.4;

        // Medium dune ridges
        double mediumDunes1 = (context.getMasterNoise().sample(x * 0.00028, 0, z * 0.0014) + 1.0) * 0.5 *
                maxDuneHeight * 0.27;
        double mediumDunes2 = (context.getMasterNoise().sample(x * 0.00048, 0, z * 0.0024) + 1.0) * 0.5 *
                maxDuneHeight * 0.2;

        // Wind-aligned patterns using wind direction noise
        double windStrength = config.getWindStrength();
        double windAngle = context.getWindDirectionNoise().sample(x * 0.00006, 0, z * 0.0003) * Math.PI;
        double windAlignedX = x * Math.cos(windAngle) - z * Math.sin(windAngle);
        double windAlignedZ = x * Math.sin(windAngle) + z * Math.cos(windAngle);

        double windRidges = (context.getErosionNoise().sample(windAlignedX * 0.00012, 0, windAlignedZ * 0.0006) + 1.0) * 0.5 *
                windStrength * maxDuneHeight * 0.23;

        // Fine sand ripples using master noise at high frequency
        double ripples = (context.getMasterNoise().sample(x * 0.0032, 0, z * 0.016) + 1.0) * 0.5 *
                maxDuneHeight * 0.1;

        // Medium-scale variations
        double mediumSand = (context.getMasterNoise().sample(x * 0.0012, 0, z * 0.006) + 1.0) * 0.5 *
                config.getSandDensity() * maxDuneHeight * 0.17;

        // Base undulation (lowest frequency for this octave)
        double baseUndulation = (context.getMasterNoise().sample(x * 0.001, 0, z * 0.005) + 1.0) * 0.5 *
                maxDuneHeight * 0.15;

        // Combine all dune features smoothly
        double totalDuneHeight = largeDunes +
                Math.max(mediumDunes1, mediumDunes2) +
                windRidges +
                ripples +
                mediumSand +
                baseUndulation;

        // Ensure minimum sand coverage
        double minSandBase = config.getSandDensity() * 12;

        return Math.max(minSandBase, totalDuneHeight);
    }
    
    @Override
    public double getPrimaryFrequency() { return 0.002; }
    
    @Override
    public UnifiedOctaveType getOctaveType() { return UnifiedOctaveType.MAJOR_FEATURES; }
    
    @Override
    public Set<PlanetType> getSupportedPlanetTypes() {
        return Set.of(PlanetType.DESERT, PlanetType.HOTHOUSE);
    }
    
    @Override
    public boolean appliesToBiome(IBiomeType biome) {
        if (biome instanceof DesertBiomeType desertBiome) {
            return switch (desertBiome) {
                case DUNE_SEA, SCORCHING_WASTE, DUST_BOWL, SCRUBLAND -> true;
                case GRANITE_MESAS, LIMESTONE_CANYONS, SALT_FLATS, VOLCANIC_WASTELAND -> false;
            };
        }
        return false;
    }
    
    @Override
    public String getOctaveName() { return "Dunes"; }
}