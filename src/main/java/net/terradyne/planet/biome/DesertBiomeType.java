// DesertBiomeType.java - UPDATED with better biome configurations

package net.terradyne.planet.biome;

import net.terradyne.planet.PlanetType;
import net.terradyne.planet.terrain.*;
import net.terradyne.planet.terrain.octave.*;

import java.util.List;

/**
 * Updated DesertBiomeType - 4 well-designed biomes
 */
public enum DesertBiomeType implements IBiomeType {

    /**
     * DUNE SEA - Rolling sand dunes with wind patterns
     * Smooth, flowing terrain with large dune formations
     */
    DUNE_SEA("dune_sea") {
        @Override
        public List<OctaveConfiguration> getOctaveConfigurations() {
            return List.of(
                    // Gentle foundation
                    new OctaveConfiguration(FoundationOctave.class)
                            .withParameter("amplitude", 8.0)
                            .withParameter("frequency", 0.0003),

                    // Large flowing dunes
                    new OctaveConfiguration(DuneOctave.class)
                            .withParameter("maxHeight", 45.0)
                            .withParameter("minHeight", 1.0)
                            .withParameter("duneSpacing", 0.003)
                            .withParameter("sharpness", 2.0)
                            .withParameter("elevationVariation", 30.0),

                    // Fine sand ripples
                    new OctaveConfiguration(DetailOctave.class)
                            .withParameter("intensity", 0.8)
                            .withParameter("frequency", 0.025)
            );
        }
    },

    /**
     * GRANITE MESA - Flat-topped elevated plateaus with steep sides
     * Dramatic vertical terrain with mesa formations and erosion
     */
    GRANITE_MESAS("granite_mesas") {
        @Override
        public List<OctaveConfiguration> getOctaveConfigurations() {
            return List.of(
                    // Higher foundation for elevated terrain
                    new OctaveConfiguration(FoundationOctave.class)
                            .withParameter("amplitude", 20.0)
                            .withParameter("frequency", 0.0008),

                    // Dramatic mesa formations
                    new OctaveConfiguration(MesaOctave.class)
                            .withParameter("mesaHeight", 50.0)
                            .withParameter("plateauSize", 0.012)
                            .withParameter("steepness", 3.5),

                    // Mesa erosion and cliff detail
                    new OctaveConfiguration(CanyonOctave.class)
                            .withParameter("depth", 15.0)
                            .withParameter("width", 0.015)
                            .withParameter("complexity", 0.4)
                            .withParameter("steepness", 2.0),

                    // Rocky surface texture
                    new OctaveConfiguration(DetailOctave.class)
                            .withParameter("intensity", 1.2)
                            .withParameter("frequency", 0.02)
            );
        }
    },

    /**
     * LIMESTONE CANYONS - Deep carved canyons and winding valleys
     * Intricate erosion patterns with layered canyon walls
     */
    LIMESTONE_CANYONS("limestone_canyons") {
        @Override
        public List<OctaveConfiguration> getOctaveConfigurations() {
            return List.of(
                    // Moderate foundation
                    new OctaveConfiguration(FoundationOctave.class)
                            .withParameter("amplitude", 12.0)
                            .withParameter("frequency", 0.0006),

                    // Deep primary canyons
                    new OctaveConfiguration(CanyonOctave.class)
                            .withParameter("depth", 40.0)
                            .withParameter("width", 0.004)
                            .withParameter("complexity", 0.9)
                            .withParameter("networkDensity", 1.5)
                            .withParameter("steepness", 1.8),

                    // Secondary canyon network
                    new OctaveConfiguration(CanyonOctave.class)
                            .withParameter("depth", 18.0)
                            .withParameter("width", 0.012)
                            .withParameter("complexity", 0.6)
                            .withParameter("networkDensity", 0.8)
                            .withParameter("steepness", 1.2),

                    // Canyon wall detail and ledges
                    new OctaveConfiguration(DetailOctave.class)
                            .withParameter("intensity", 0.6)
                            .withParameter("frequency", 0.018)
            );
        }
    },

    /**
     * SALT FLATS - Extremely flat crystalline plains with hexagonal patterns
     * Very low terrain with intricate salt crystal formations
     */
    SALT_FLATS("salt_flats") {
        @Override
        public List<OctaveConfiguration> getOctaveConfigurations() {
            return List.of(
                    // Very flat foundation
                    new OctaveConfiguration(FoundationOctave.class)
                            .withParameter("amplitude", 2.0)
                            .withParameter("frequency", 0.0002),

                    // Subtle drainage channels
                    new OctaveConfiguration(CanyonOctave.class)
                            .withParameter("depth", 6.0)
                            .withParameter("width", 0.003)
                            .withParameter("complexity", 0.2)
                            .withParameter("steepness", 0.8),

                    // Salt crystal patterns
                    new OctaveConfiguration(DetailOctave.class)
                            .withParameter("intensity", 0.3)
                            .withParameter("frequency", 0.035)
                            .withParameter("saltPatterns", true),

                    // Larger hexagonal salt formations
                    new OctaveConfiguration(DetailOctave.class)
                            .withParameter("intensity", 0.15)
                            .withParameter("frequency", 0.008)
                            .withParameter("saltPatterns", true)
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