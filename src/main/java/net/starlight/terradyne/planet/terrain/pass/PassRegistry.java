package net.starlight.terradyne.planet.terrain.pass;

import net.starlight.terradyne.Terradyne;
import net.starlight.terradyne.planet.biome.IBiomeType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for generation passes
 */
public class PassRegistry {
    
    private static final Map<Class<? extends IGenerationPass>, IGenerationPass> passInstances = new ConcurrentHashMap<>();
    private static boolean initialized = false;
    
    /**
     * Initialize the pass registry
     */
    public static synchronized void initialize() {
        if (initialized) return;
        
        Terradyne.LOGGER.info("=== Initializing Generation Pass Registry ===");
        
        // Register passes
        registerPass(new PhysicsFoundationPass());
        registerPass(new VolatilityPass());
        
        // Later we'll add:
        // registerPass(new VolatilityPass());         // Mountains and valleys
        // registerPass(new WaterPlacementPass());     // Oceans and lakes
        // registerPass(new TemperaturePass());        // Ice and snow
        // registerPass(new WindErosionPass());        // Erosion effects
        // registerPass(new BiomeDecorationPass());    // Trees, grass, etc.
        
        initialized = true;
        Terradyne.LOGGER.info("✅ Pass Registry: {} passes registered", passInstances.size());
    }
    
    /**
     * Register a generation pass
     */
    public static void registerPass(IGenerationPass pass) {
        if (pass == null) {
            throw new IllegalArgumentException("Cannot register null pass");
        }
        
        passInstances.put(pass.getClass(), pass);
        Terradyne.LOGGER.debug("Registered pass: {} (priority {})", 
                pass.getPassName(), pass.getPassPriority());
    }
    
    /**
     * Get configured passes for a biome
     */
    public static List<ConfiguredPass> getConfiguredPassesForBiome(IBiomeType biome) {
        List<PassConfiguration> configurations = biome.getGenerationPasses();
        List<ConfiguredPass> configuredPasses = new ArrayList<>();
        
        for (PassConfiguration config : configurations) {
            IGenerationPass pass = passInstances.get(config.getPassClass());
            
            if (pass == null) {
                Terradyne.LOGGER.warn("Pass class {} not registered, skipping",
                        config.getPassClass().getSimpleName());
                continue;
            }
            
            configuredPasses.add(new ConfiguredPass(pass, config));
        }
        
        // Sort by priority
        configuredPasses.sort(Comparator.comparingInt(cp -> cp.config.getPriority()));
        
        return configuredPasses;
    }
    
    /**
     * Get a pass instance by class
     */
    public static IGenerationPass getPass(Class<? extends IGenerationPass> passClass) {
        return passInstances.get(passClass);
    }
    
    /**
     * Clear registry (for testing)
     */
    public static synchronized void clear() {
        passInstances.clear();
        initialized = false;
    }
    
    /**
     * Container for a configured pass
     */
    public static class ConfiguredPass {
        public final IGenerationPass pass;
        public final PassConfiguration config;
        
        public ConfiguredPass(IGenerationPass pass, PassConfiguration config) {
            this.pass = pass;
            this.config = config;
        }
    }
}