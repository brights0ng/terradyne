package net.terradyne.planet.biome;

import net.terradyne.planet.PlanetType;

// Desert biomes implement this
public enum DesertBiomeType implements IBiomeType {
    DUNE_SEA("dune_sea"),
    SCORCHING_WASTE("scorching_waste"),
    GRANITE_MESAS("granite_mesas"),
    LIMESTONE_CANYONS("limestone_canyons"),
    SALT_FLATS("salt_flats"),
    SCRUBLAND("scrubland"),
    DUST_BOWL("dust_bowl"),
    VOLCANIC_WASTELAND("volcanic_wasteland");

    private final String name;

    DesertBiomeType(String name) { this.name = name; }

    @Override public String getName() { return name; }
    @Override public PlanetType getPlanetType() { return PlanetType.DESERT; }
}