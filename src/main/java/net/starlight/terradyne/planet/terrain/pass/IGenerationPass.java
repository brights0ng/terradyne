package net.starlight.terradyne.planet.terrain.pass;

import net.minecraft.world.chunk.Chunk;
import net.starlight.terradyne.planet.biome.IBiomeType;
import net.starlight.terradyne.planet.terrain.octave.OctaveContext;

/**
 * Interface for terrain generation passes
 * Each pass modifies the chunk in sequence
 */
public interface IGenerationPass {
    
    /**
     * Apply this generation pass to the chunk
     * @param chunk The chunk being generated
     * @param biome The biome type for this chunk area
     * @param context Shared context with noise providers and planet data
     * @param config Pass-specific configuration
     */
    void applyPass(Chunk chunk, IBiomeType biome, OctaveContext context, PassConfiguration config);
    
    /**
     * Get the priority for this pass (lower = earlier)
     * 0-99: Foundation passes (base terrain)
     * 100-199: Formation passes (features like mountains)
     * 200-299: Carving passes (erosion, canyons)
     * 300-399: Detail passes (surface features)
     * 400+: Decoration passes (vegetation, structures)
     */
    int getPassPriority();
    
    /**
     * Get a descriptive name for this pass
     */
    String getPassName();
    
    /**
     * Get parameter documentation for this pass
     */
    default String getParameterDocumentation() {
        return "No parameters documented";
    }
}