package net.starlight.terradyne.planet.biome;

import net.starlight.terradyne.planet.PlanetType;
import java.util.List;

/**
 * Interface for biome types
 * Biomes define which generation passes to use
 */
public interface IBiomeType {
    
    /**
     * Get the name of this biome
     */
    String getName();
    
    /**
     * Get the planet type this biome belongs to
     * For physics planets, this emerges from conditions
     */
    PlanetType getPlanetType();
    
    /**
     * Get the generation passes this biome uses
     * Passes are applied in priority order
     */
    
    /**
     * Legacy: Get octave configurations
     * Will be removed once we fully transition to passes
     */
}