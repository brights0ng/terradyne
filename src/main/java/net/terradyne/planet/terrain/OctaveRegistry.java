package net.terradyne.planet.terrain;

import net.terradyne.planet.PlanetType;
import net.terradyne.planet.biome.IBiomeType;
import net.terradyne.Terradyne;
import net.terradyne.planet.terrain.octave.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FINAL OctaveRegistry - Now includes RollingTerrainOctave for mesa base terrain
 */
public class OctaveRegistry {
    private static final Map<Class<? extends IUnifiedOctave>, IUnifiedOctave> octaveInstances = new ConcurrentHashMap<>();
    private static boolean initialized = false;

    /**
     * Initialize the octave registry with all octaves for the 4-biome desert system
     */
    public static synchronized void initialize() {
        if (initialized) {
            return;
        }

        Terradyne.LOGGER.info("=== Initializing 4-Biome Desert Terrain System ===");

        // === FOUNDATIONAL OCTAVES ===
        registerOctave(new FoundationOctave());

        // === ADDITIVE TERRAIN OCTAVES ===
        registerOctave(new DuneOctave());                // Dune Sea
        registerOctave(new MesaOctave());                // Granite Mesas - dramatic formations
        registerOctave(new RollingTerrainOctave());      // RENAMED from Scrubland - gentle base terrain
        registerOctave(new VolcanicFlowOctave());        // For future use

        // === SUBTRACTIVE/CARVING OCTAVES ===
        registerOctave(new CanyonOctave());              // Limestone Canyons + Mesa erosion
        registerOctave(new WindErosionOctave());         // For future extreme environments

        // === DETAIL OCTAVES ===
        registerOctave(new DetailOctave());              // Surface texture - includes salt patterns
        registerOctave(new OasisOctave());               // For future rare oasis features

        initialized = true;
        Terradyne.LOGGER.info("✅ Registered {} octave types for 4-biome desert system", octaveInstances.size());

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
     * Log registered octaves for debugging - now shows 4-biome focus
     */
    private static void logRegisteredOctaves() {
        Terradyne.LOGGER.info("=== 4-BIOME DESERT TERRAIN SYSTEM ===");
        for (IUnifiedOctave octave : octaveInstances.values()) {
            Terradyne.LOGGER.info("  {} - Supports: {} (Freq: {})",
                    octave.getOctaveName(),
                    octave.getSupportedPlanetTypes(),
                    String.format("%.4f", octave.getPrimaryFrequency()));
        }
        Terradyne.LOGGER.info("=== BIOME SPECIALIZATIONS ===");
        Terradyne.LOGGER.info("  🏔️  GRANITE_MESAS: Rolling base + Dramatic mesas + Canyon systems");
        Terradyne.LOGGER.info("  🏜️  LIMESTONE_CANYONS: Deep meandering canyon networks");
        Terradyne.LOGGER.info("  🌊  DUNE_SEA: Smooth flowing dune formations");
        Terradyne.LOGGER.info("  🧂  SALT_FLATS: Ultra-flat with subtle drainage + salt patterns");
        Terradyne.LOGGER.info("=== KEY ENHANCEMENT ===");
        Terradyne.LOGGER.info("  🌾  RollingTerrainOctave: Creates realistic gentle base for dramatic mesas");
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