package net.starlight.terradyne.planet.terrain.pass;

import net.starlight.terradyne.Terradyne;
import net.starlight.terradyne.planet.biome.IBiomeType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for generation passes - manages pass instances and provides them to biomes
 */
public class PassRegistry {
    private static final Map<Class<? extends IGenerationPass>, IGenerationPass> passInstances = new ConcurrentHashMap<>();
    private static boolean initialized = false;

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }

        Terradyne.LOGGER.info("=== Initializing Pass-Based Generation System ===");

        // === FOUNDATION PASSES (Priority 0-9) ===
        registerPass(new TerrainFoundationPass());

        // === OVERRIDE PASSES (Priority 10-19) ===
        registerPass(new MesaOverridePass());
        registerPass(new DuneOverridePass());        // NEW
        registerPass(new GraniteCapPass());
        registerPass(new LimestoneLayeringPass());

        // === OVERRIDE PASSES (Priority 10-19) ===
        registerPass(new FlatSurfacePass());


        // === CARVING PASSES (Priority 20-29) ===
        registerPass(new CanyonCarvingPass());
        registerPass(new GeometricSaltPass());         // ← Make sure this line exists!
        registerPass(new SaltSurfacePass());         // NEW

        // === DETAIL PASSES (Priority 30-39) ===
        registerPass(new SurfaceDetailPass());

        initialized = true;
        Terradyne.LOGGER.info("✅ Registered {} generation passes", passInstances.size());

        logRegisteredPasses();
    }

    /**
     * Register a new pass instance
     */
    public static void registerPass(IGenerationPass pass) {
        if (pass == null) {
            throw new IllegalArgumentException("Cannot register null pass");
        }

        passInstances.put(pass.getClass(), pass);
        Terradyne.LOGGER.debug("Registered pass: {}", pass.getPassName());
    }

    /**
     * Get configured passes for a biome, ready to use
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

        // Sort by priority (foundation first, details last)
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
     * Get all registered passes
     */
    public static Collection<IGenerationPass> getAllPasses() {
        return new ArrayList<>(passInstances.values());
    }

    /**
     * Clear registry (for testing)
     */
    public static synchronized void clear() {
        passInstances.clear();
        initialized = false;
        Terradyne.LOGGER.info("Cleared pass registry");
    }

    /**
     * Log registered passes for debugging
     */
    private static void logRegisteredPasses() {
        Terradyne.LOGGER.info("=== PASS-BASED GENERATION SYSTEM ===");
        List<IGenerationPass> sortedPasses = passInstances.values().stream()
                .sorted(Comparator.comparingInt(IGenerationPass::getPassPriority))
                .toList();

        for (IGenerationPass pass : sortedPasses) {
            Terradyne.LOGGER.info("  [{}] {} - {}",
                    pass.getPassPriority(),
                    pass.getPassName(),
                    pass.getClass().getSimpleName());
        }
    }

    /**
     * Container for a pass instance with its configuration
     */
    public static class ConfiguredPass {
        public final IGenerationPass pass;
        public final PassConfiguration config;

        public ConfiguredPass(IGenerationPass pass, PassConfiguration config) {
            this.pass = pass;
            this.config = config;
        }

        @Override
        public String toString() {
            return pass.getPassName() + " " + config.getAllParameters();
        }
    }
}