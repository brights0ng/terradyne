package net.starlight.terradyne.planet.terrain.octave;

import net.starlight.terradyne.planet.PlanetType;
import net.starlight.terradyne.planet.terrain.MasterNoiseProvider;
import net.starlight.terradyne.planet.terrain.OctaveConfiguration;
import net.starlight.terradyne.planet.terrain.OctaveContext;

import java.util.Set;

/**
 * Creates rare oasis depressions in desert terrain
 */
public class OasisOctave implements IUnifiedOctave {

    @Override
    public double generateHeightContribution(int x, int z, OctaveContext context,
                                             OctaveConfiguration config) {
        MasterNoiseProvider noise = context.getNoiseProvider();

        // Configuration
        double rarity = config.getDouble("rarity", 0.02);      // How rare oases are
        double basinDepth = config.getDouble("basinDepth", 8.0); // How deep the basin
        double basinSize = config.getDouble("basinSize", 0.015);  // Size of oasis areas

        // === STEP 1: Determine if oasis should exist here ===
        // Use very large-scale noise to make oases rare and scattered
        double oasisChance = noise.sampleAt(x * 0.0003, 0, z * 0.0003);
        
        // Only proceed if we're in a potential oasis zone
        if (oasisChance < 1.0 - rarity) {
            return 0.0; // No oasis here
        }

        // === STEP 2: Check if we're in a low area (valley between dunes) ===
        // Sample the dune pattern to see if this is already a low spot
        double duneHeight = noise.sampleAt(x * 0.004, 0, z * 0.004 * 0.6);
        
        // Only create oasis in naturally low areas
        if (duneHeight > -0.3) {
            return 0.0; // This area is too high for an oasis
        }

        // === STEP 3: Create the oasis basin ===
        // Circular/oval depression
        double basinNoise = noise.sampleAt(x * basinSize, 0, z * basinSize);
        double secondaryBasin = noise.sampleAt(x * basinSize * 2.0, 0, z * basinSize * 1.5) * 0.5;
        
        double basinShape = basinNoise + secondaryBasin;
        
        // Create smooth circular depression
        basinShape = smoothstep(-0.5, 0.3, basinShape);
        
        // Invert so it creates a depression
        double depression = -basinShape * basinDepth;
        
        // === STEP 4: Add gentle slopes around the depression ===
        double slopeNoise = noise.sampleAt(x * basinSize * 0.7, 0, z * basinSize * 0.7);
        double gentleSlope = smoothstep(0.2, 0.8, basinShape) * slopeNoise * 2.0;
        
        return depression + gentleSlope;
    }

    private double smoothstep(double edge0, double edge1, double x) {
        double t = Math.max(0, Math.min(1, (x - edge0) / (edge1 - edge0)));
        return t * t * (3.0 - 2.0 * t);
    }

    @Override
    public double getPrimaryFrequency() { return 0.015; }

    @Override
    public Set<PlanetType> getSupportedPlanetTypes() {
        return Set.of(PlanetType.DESERT);
    }

    @Override
    public String getOctaveName() { return "OasisBasins"; }

    @Override
    public String getParameterDocumentation() {
        return """
            Oasis Basin Octave Parameters:
            - rarity (double, default 0.02): How rare oases are (0.01 = very rare, 0.1 = common)
            - basinDepth (double, default 8.0): Depth of oasis depressions
            - basinSize (double, default 0.015): Size scale of oasis basins
            """;
    }
}