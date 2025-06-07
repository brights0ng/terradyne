package net.terradyne.planet.terrain;

import net.terradyne.planet.PlanetType;
import net.terradyne.planet.biome.IBiomeType;
import net.terradyne.Terradyne;
import net.terradyne.planet.terrain.octave.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Updated registry for configuration-based octave system
 */
public class OctaveRegistry {
    private static final Map<Class<? extends IUnifiedOctave>, IUnifiedOctave> octaveInstances = new ConcurrentHashMap<>();
    private static boolean initialized = false;

    /**
     * Initialize the octave registry with all available octaves
     */
    public static synchronized void initialize() {
        if (initialized) {
            return;
        }

        Terradyne.LOGGER.info("=== Initializing Configuration-Based Octave Registry ===");

        // Register all octave types
        registerOctave(new FoundationOctave());
        registerOctave(new DuneOctave());
        registerOctave(new DetailOctave());
        registerOctave(new CanyonOctave());
        registerOctave(new MesaOctave());
        registerOctave(new VolcanicFlowOctave());

        initialized = true;
        Terradyne.LOGGER.info("✅ Registered {} octave types", octaveInstances.size());

        logRegisteredOctaves();
    }

    /**
     * Register a new octave instance
     */
    public static void registerOctave(IUnifiedOctave octave) {
        if (octave == null) {
            throw new IllegalArgumentException("Cannot register null octave");
        }

        octaveInstances.put(octave.getClass(), octave);
        Terradyne.LOGGER.debug("Registered octave: {}", octave.getOctaveName());
    }

    /**
     * Get configured octaves for a biome, instantiated and ready to use
     */
    public static List<ConfiguredOctave> getConfiguredOctavesForBiome(IBiomeType biome, PlanetType planetType) {
        List<OctaveConfiguration> configurations = biome.getOctaveConfigurations();
        List<ConfiguredOctave> configuredOctaves = new ArrayList<>();

        for (OctaveConfiguration config : configurations) {
            IUnifiedOctave octave = octaveInstances.get(config.getOctaveClass());

            if (octave == null) {
                Terradyne.LOGGER.warn("Octave class {} not registered, skipping",
                        config.getOctaveClass().getSimpleName());
                continue;
            }

            // Check if octave supports this planet type
            if (!octave.getSupportedPlanetTypes().contains(planetType)) {
                Terradyne.LOGGER.debug("Octave {} doesn't support planet type {}, skipping",
                        octave.getOctaveName(), planetType);
                continue;
            }

            configuredOctaves.add(new ConfiguredOctave(octave, config));
        }

        // Sort by frequency (foundation first, details last)
        configuredOctaves.sort(Comparator.comparingDouble(co -> co.octave.getPrimaryFrequency()));

        return configuredOctaves;
    }

    /**
     * Get an octave instance by class
     */
    public static IUnifiedOctave getOctave(Class<? extends IUnifiedOctave> octaveClass) {
        return octaveInstances.get(octaveClass);
    }

    /**
     * Get all registered octaves
     */
    public static Collection<IUnifiedOctave> getAllOctaves() {
        return new ArrayList<>(octaveInstances.values());
    }

    /**
     * Get octaves that support a specific planet type
     */
    public static List<IUnifiedOctave> getOctavesForPlanetType(PlanetType planetType) {
        return octaveInstances.values().stream()
                .filter(octave -> octave.getSupportedPlanetTypes().contains(planetType))
                .sorted(Comparator.comparingDouble(IUnifiedOctave::getPrimaryFrequency))
                .toList();
    }

    /**
     * Clear registry (for testing)
     */
    public static synchronized void clear() {
        octaveInstances.clear();
        initialized = false;
        Terradyne.LOGGER.info("Cleared octave registry");
    }

    /**
     * Log registered octaves for debugging
     */
    private static void logRegisteredOctaves() {
        Terradyne.LOGGER.info("Registered octaves:");
        for (IUnifiedOctave octave : octaveInstances.values()) {
            Terradyne.LOGGER.info("  {} - Supports: {}",
                    octave.getOctaveName(),
                    octave.getSupportedPlanetTypes());
        }
    }

    /**
     * Container for an octave instance with its configuration
     */
    public static class ConfiguredOctave {
        public final IUnifiedOctave octave;
        public final OctaveConfiguration config;

        public ConfiguredOctave(IUnifiedOctave octave, OctaveConfiguration config) {
            this.octave = octave;
            this.config = config;
        }

        @Override
        public String toString() {
            return octave.getOctaveName() + " " + config.getAllParameters();
        }
    }
}