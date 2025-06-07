package net.terradyne.planet.biome;

import net.terradyne.planet.PlanetType;
import net.terradyne.planet.terrain.*;
import net.terradyne.planet.terrain.octave.*;

import java.util.List;

/**
 * Updated DesertBiomeType - Now directly configures octaves with parameters
 */
public enum DesertBiomeType implements IBiomeType {
    DUNE_SEA("dune_sea") {
        @Override
        public List<OctaveConfiguration> getOctaveConfigurations() {
            return List.of(
                    new OctaveConfiguration(FoundationOctave.class)
                            .withParameter("amplitude", 2.0)
                            .withParameter("frequency", 0.0001),

                    new OctaveConfiguration(DuneOctave.class)
                            .withParameter("maxHeight", 45.0)
                            .withParameter("minHeight", 10.0)
                            .withParameter("duneSpacing", 0.004)
                            .withParameter("sharpness", 4)
                            .withParameter("elevationVariation", 30.0), // New parameter!

                    new OctaveConfiguration(DetailOctave.class)
                            .withParameter("intensity", 0.01)
                            .withParameter("frequency", 0.08)
            );
        }
    },

    GRANITE_MESAS("granite_mesas") {
        @Override
        public List<OctaveConfiguration> getOctaveConfigurations() {
            return List.of(
                    new OctaveConfiguration(FoundationOctave.class)
                            .withParameter("amplitude", 15.0)
                            .withParameter("frequency", 0.0008),

                    new OctaveConfiguration(MesaOctave.class)
                            .withParameter("mesaHeight", 40.0)
                            .withParameter("plateauSize", 0.02)
                            .withParameter("steepness", 2.5),

                    new OctaveConfiguration(CanyonOctave.class)
                            .withParameter("depth", 25.0)
                            .withParameter("width", 0.008)
                            .withParameter("complexity", 0.6)
            );
        }
    },

    LIMESTONE_CANYONS("limestone_canyons") {
        @Override
        public List<OctaveConfiguration> getOctaveConfigurations() {
            return List.of(
                    new OctaveConfiguration(FoundationOctave.class)
                            .withParameter("amplitude", 10.0),

                    new OctaveConfiguration(CanyonOctave.class)
                            .withParameter("depth", 35.0)
                            .withParameter("width", 0.006)
                            .withParameter("complexity", 0.8)
                            .withParameter("networkDensity", 1.2),

                    new OctaveConfiguration(DetailOctave.class)
                            .withParameter("intensity", 0.08)
                            .withParameter("frequency", 0.012)
            );
        }
    },

    SCRUBLAND("scrubland") {
        @Override
        public List<OctaveConfiguration> getOctaveConfigurations() {
            return List.of(
                    new OctaveConfiguration(FoundationOctave.class)
                            .withParameter("amplitude", 8.0),

                    new OctaveConfiguration(DuneOctave.class)
                            .withParameter("maxHeight", 15.0)
                            .withParameter("windInfluence", 0.4)
                            .withParameter("rippleIntensity", 0.08)
                            .withParameter("sparsity", 0.7),

                    new OctaveConfiguration(DetailOctave.class)
                            .withParameter("intensity", 0.15)
                            .withParameter("frequency", 0.01)
            );
        }
    },

    SCORCHING_WASTE("scorching_waste") {
        @Override
        public List<OctaveConfiguration> getOctaveConfigurations() {
            return List.of(
                    new OctaveConfiguration(FoundationOctave.class)
                            .withParameter("amplitude", 10.0),

                    new OctaveConfiguration(DuneOctave.class)
                            .withParameter("maxHeight", 28.0)
                            .withParameter("windInfluence", 1.2)
                            .withParameter("rippleIntensity", 0.2)
                            .withParameter("heatDistortion", 0.3),

                    new OctaveConfiguration(DetailOctave.class)
                            .withParameter("intensity", 0.18)
                            .withParameter("frequency", 0.015)
            );
        }
    },

    DUST_BOWL("dust_bowl") {
        @Override
        public List<OctaveConfiguration> getOctaveConfigurations() {
            return List.of(
                    new OctaveConfiguration(FoundationOctave.class)
                            .withParameter("amplitude", 4.0)  // Very flat
                            .withParameter("frequency", 0.0005),

                    new OctaveConfiguration(DetailOctave.class)
                            .withParameter("intensity", 0.06)  // Just fine texture
                            .withParameter("frequency", 0.02)
            );
        }
    },

    SALT_FLATS("salt_flats") {
        @Override
        public List<OctaveConfiguration> getOctaveConfigurations() {
            return List.of(
                    new OctaveConfiguration(FoundationOctave.class)
                            .withParameter("amplitude", 3.0)  // Extremely flat
                            .withParameter("frequency", 0.0003),

                    new OctaveConfiguration(CanyonOctave.class)
                            .withParameter("depth", 8.0)     // Shallow drainage channels
                            .withParameter("width", 0.004)
                            .withParameter("complexity", 0.3),

                    new OctaveConfiguration(DetailOctave.class)
                            .withParameter("intensity", 0.04)
                            .withParameter("frequency", 0.025)
                            .withParameter("saltPatterns", true)
            );
        }
    },

    VOLCANIC_WASTELAND("volcanic_wasteland") {
        @Override
        public List<OctaveConfiguration> getOctaveConfigurations() {
            return List.of(
                    new OctaveConfiguration(FoundationOctave.class)
                            .withParameter("amplitude", 18.0),

                    new OctaveConfiguration(VolcanicFlowOctave.class)
                            .withParameter("flowHeight", 25.0)
                            .withParameter("channelDepth", 15.0)
                            .withParameter("roughness", 0.8),

                    new OctaveConfiguration(DetailOctave.class)
                            .withParameter("intensity", 0.25)
                            .withParameter("frequency", 0.02)
                            .withParameter("volcanic", true)
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