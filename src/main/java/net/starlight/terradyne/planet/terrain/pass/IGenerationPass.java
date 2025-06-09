package net.starlight.terradyne.planet.terrain.pass;

import net.minecraft.world.chunk.Chunk;
import net.starlight.terradyne.planet.biome.IBiomeType;
import net.starlight.terradyne.planet.terrain.OctaveContext;

/**
 * Interface for generation passes that place/modify blocks in chunks
 * Each pass can read existing blocks and override them
 */
public interface IGenerationPass {
    /**
     * Apply this generation pass to the chunk
     * @param chunk The chunk being generated
     * @param biome The biome type for this chunk area
     * @param context Shared context with noise providers and planet data
     * @param config Pass-specific configuration from the biome
     */
    void applyPass(Chunk chunk, IBiomeType biome, OctaveContext context, PassConfiguration config);
    
    /**
     * What order should this pass run? (lower numbers = earlier)
     * Foundation passes should be 0-9, override passes 10-19, carving passes 20-29, etc.
     */
    int getPassPriority();
    
    /**
     * Get a descriptive name for this pass (for debugging)
     */
    String getPassName();
    
    /**
     * Get parameter documentation for this pass (for debugging/tooling)
     */
    default String getParameterDocumentation() {
        return "No parameters documented";
    }
}