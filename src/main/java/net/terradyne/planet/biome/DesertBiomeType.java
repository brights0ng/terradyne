package net.terradyne.planet.biome;

import net.terradyne.planet.PlanetType;

import java.util.List;

public enum DesertBiomeType implements IBiomeType {
    DUNE_SEA("dune_sea") {
        @Override
        public List<OctaveType> getRequestedOctaveTypes() {
            return List.of(
                    OctaveType.BASE_FOUNDATION,    // Large-scale terrain base
                    OctaveType.ADDITIVE_FEATURES   // Sand dune formations
            );
        }
    },

    GRANITE_MESAS("granite_mesas") {
        @Override
        public List<OctaveType> getRequestedOctaveTypes() {
            return List.of(
                    OctaveType.BASE_FOUNDATION,       // Large-scale terrain base
                    OctaveType.SUBTRACTIVE_CARVING    // Canyon erosion features
            );
        }
    },

    LIMESTONE_CANYONS("limestone_canyons") {
        @Override
        public List<OctaveType> getRequestedOctaveTypes() {
            return List.of(
                    OctaveType.BASE_FOUNDATION,       // Large-scale terrain base
                    OctaveType.SUBTRACTIVE_CARVING    // Deep canyon carving
            );
        }
    },

    SCRUBLAND("scrubland") {
        @Override
        public List<OctaveType> getRequestedOctaveTypes() {
            return List.of(
                    OctaveType.BASE_FOUNDATION,       // Large-scale terrain base
                    OctaveType.ADDITIVE_FEATURES,     // Small sand deposits
                    OctaveType.FINE_DETAILS,          // Surface texture variations
                    OctaveType.SUBTRACTIVE_CARVING    // Minor erosion channels
            );
        }
    },

    SCORCHING_WASTE("scorching_waste") {
        @Override
        public List<OctaveType> getRequestedOctaveTypes() {
            return List.of(
                    OctaveType.BASE_FOUNDATION,    // Large-scale terrain base
                    OctaveType.ADDITIVE_FEATURES   // Heat-distorted dune patterns
            );
        }
    },

    DUST_BOWL("dust_bowl") {
        @Override
        public List<OctaveType> getRequestedOctaveTypes() {
            return List.of(
                    OctaveType.BASE_FOUNDATION,    // Large-scale terrain base
                    OctaveType.FINE_DETAILS        // Wind-blown surface texture only
            );
        }
    },

    SALT_FLATS("salt_flats") {
        @Override
        public List<OctaveType> getRequestedOctaveTypes() {
            return List.of(
                    OctaveType.BASE_FOUNDATION,       // Large-scale terrain base
                    OctaveType.SUBTRACTIVE_CARVING    // Drainage channels
            );
        }
    },

    VOLCANIC_WASTELAND("volcanic_wasteland") {
        @Override
        public List<OctaveType> getRequestedOctaveTypes() {
            return List.of(
                    OctaveType.BASE_FOUNDATION,       // Large-scale terrain base
                    OctaveType.SUBTRACTIVE_CARVING    // Lava flow channels
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

    // Abstract method implemented by each enum value
    public abstract List<OctaveType> getRequestedOctaveTypes();
}