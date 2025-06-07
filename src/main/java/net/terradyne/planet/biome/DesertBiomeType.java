package net.terradyne.planet.biome;

import net.terradyne.planet.PlanetType;
import net.terradyne.planet.terrain.*;
import net.terradyne.planet.terrain.octave.*;

import java.util.List;

/**
 * UPDATED DesertBiomeType - Now with only 4 biomes and rolling terrain under mesas
 */
public enum DesertBiomeType implements IBiomeType {
    DUNE_SEA("dune_sea") {
        @Override
        public List<OctaveConfiguration> getOctaveConfigurations() {
            // KEEPING the working Dune Sea configuration as-is
            return List.of(
                    new OctaveConfiguration(FoundationOctave.class)
                            .withParameter("amplitude", 2.0)
                            .withParameter("frequency", 0.0001),

                    new OctaveConfiguration(DuneOctave.class)
                            .withParameter("maxHeight", 45.0)
                            .withParameter("minHeight", 10.0)
                            .withParameter("duneSpacing", 0.004)
                            .withParameter("sharpness", 4)
                            .withParameter("elevationVariation", 30.0),

                    new OctaveConfiguration(DetailOctave.class)
                            .withParameter("intensity", 0.01)
                            .withParameter("frequency", 0.08)
            );
        }
    },

    GRANITE_MESAS("granite_mesas") {
        @Override
        public List<OctaveConfiguration> getOctaveConfigurations() {
            // ENHANCED with rolling terrain base underneath the dramatic mesas
            return List.of(
                    new OctaveConfiguration(FoundationOctave.class)
                            .withParameter("amplitude", 6.0)      // Moderate base for mesa contrast
                            .withParameter("frequency", 0.0004),

                    new OctaveConfiguration(RollingTerrainOctave.class)  // NEW! Gentle terrain base
                            .withParameter("hillHeight", 12.0)    // Rolling hills at mesa base
                            .withParameter("hillFrequency", 0.015) // Gentle undulation
                            .withParameter("rockOutcropIntensity", 0.4)  // Scattered rocks
                            .withParameter("washDepth", 3.0)      // Shallow drainage
                            .withParameter("undulationStrength", 0.8),   // Moderate rolling

                    new OctaveConfiguration(MesaOctave.class)     // Dramatic mesas rise from rolling base
                            .withParameter("mesaHeight", 70.0)    // Tall dramatic mesas
                            .withParameter("plateauFrequency", 0.006)  // Larger mesa formations
                            .withParameter("steepness", 6.0)      // Very steep mesa sides
                            .withParameter("erosionIntensity", 0.8)    // Heavy erosion for realism
                            .withParameter("layering", 1.2),      // Visible rock layers

                    new OctaveConfiguration(CanyonOctave.class)   // Canyons between mesas
                            .withParameter("maxDepth", 35.0)      // Deep canyons between mesas
                            .withParameter("channelFrequency", 0.004)
                            .withParameter("meandering", 1.5)     // Winding canyon systems
                            .withParameter("wallSteepness", 4.0)  // Steep canyon walls
                            .withParameter("tributaryDensity", 0.6),  // Moderate tributary network

                    new OctaveConfiguration(DetailOctave.class)
                            .withParameter("intensity", 0.12)     // Rocky surface detail
                            .withParameter("frequency", 0.02)
            );
        }
    },

    LIMESTONE_CANYONS("limestone_canyons") {
        @Override
        public List<OctaveConfiguration> getOctaveConfigurations() {
            // LIMESTONE-FOCUSED canyon landscape
            return List.of(
                    new OctaveConfiguration(FoundationOctave.class)
                            .withParameter("amplitude", 12.0)     // Moderate base terrain
                            .withParameter("frequency", 0.0008),

                    new OctaveConfiguration(CanyonOctave.class)   // PRIMARY feature - deep canyons
                            .withParameter("maxDepth", 50.0)      // VERY deep limestone canyons
                            .withParameter("channelFrequency", 0.003)
                            .withParameter("meandering", 2.0)     // Highly meandering like real limestone canyons
                            .withParameter("wallSteepness", 5.0)  // Very steep limestone walls
                            .withParameter("tributaryDensity", 1.2),  // Dense tributary network

                    new OctaveConfiguration(CanyonOctave.class)   // SECONDARY canyon system at different scale
                            .withParameter("maxDepth", 25.0)      // Secondary canyon depth
                            .withParameter("channelFrequency", 0.008)  // Smaller scale canyons
                            .withParameter("meandering", 1.0)
                            .withParameter("wallSteepness", 3.0)
                            .withParameter("tributaryDensity", 0.8),

                    new OctaveConfiguration(DetailOctave.class)
                            .withParameter("intensity", 0.15)     // Limestone texture
                            .withParameter("frequency", 0.015)
            );
        }
    },

    SALT_FLATS("salt_flats") {
        @Override
        public List<OctaveConfiguration> getOctaveConfigurations() {
            // ULTRA-flat with subtle drainage patterns
            return List.of(
                    new OctaveConfiguration(FoundationOctave.class)
                            .withParameter("amplitude", 2.0)      // Extremely flat base
                            .withParameter("frequency", 0.0002),

                    new OctaveConfiguration(CanyonOctave.class)   // Very shallow drainage only
                            .withParameter("maxDepth", 4.0)       // Very shallow channels
                            .withParameter("channelFrequency", 0.006)
                            .withParameter("meandering", 0.3)     // Straighter drainage
                            .withParameter("wallSteepness", 1.2)  // Very gentle slopes
                            .withParameter("tributaryDensity", 0.2),  // Minimal tributaries

                    new OctaveConfiguration(DetailOctave.class)
                            .withParameter("intensity", 0.05)     // Very fine surface detail
                            .withParameter("frequency", 0.035)
                            .withParameter("saltPatterns", true)  // Special salt crystal patterns
            );
        }
    };

    private final String name;

    DesertBiomeType(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public PlanetType getPlanetType() {
        return PlanetType.DESERT;
    }
}