package net.terradyne.planet.terrain;

import net.terradyne.planet.PlanetType;
import net.terradyne.planet.biome.IBiomeType;

import java.util.Set;

// Base interface for all terrain generators
// Base interface for all terrain generators
public interface ITerrainGenerator {
    TerrainData generateTerrain(int x, int z, TerrainContext context);
    int getPriority(); // Higher priority = applied later (overrides)
    boolean appliesToBiome(IBiomeType biome);
    Set<PlanetType> getSupportedPlanetTypes(); // What planets this works on
}