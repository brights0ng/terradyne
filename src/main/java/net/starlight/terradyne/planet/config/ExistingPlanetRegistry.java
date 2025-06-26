package net.starlight.terradyne.planet.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.MinecraftServer;
import net.starlight.terradyne.Terradyne;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Tracks which planets have already been generated to prevent modification/deletion
 * Stored as JSON in world save directory for persistence
 */
public class ExistingPlanetRegistry {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final String REGISTRY_FILE = "planet_registry.json";

    /**
     * Information about a generated planet
     */
    public static class PlanetRegistryEntry {
        public String planetName;
        public String dimensionId;
        public String generatedAt;
        public String configHash;  // To detect config changes
        public boolean protectedd = true;  // Always protect existing planets

        public PlanetRegistryEntry() {}

        public PlanetRegistryEntry(String planetName, String dimensionId, String configHash) {
            this.planetName = planetName;
            this.dimensionId = dimensionId;
            this.generatedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            this.configHash = configHash;
            this.protectedd = true;
        }
    }

    /**
     * Registry data structure
     */
    public static class RegistryData {
        public Map<String, PlanetRegistryEntry> planets = new HashMap<>();
        public String lastModified;
        public int totalGenerated = 0;

        public RegistryData() {
            this.lastModified = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
    }

    private final MinecraftServer server;
    private final Path registryPath;
    private RegistryData registryData;

    public ExistingPlanetRegistry(MinecraftServer server) {
        this.server = server;
        this.registryPath = getTerradyneConfigDirectory(server).resolve(REGISTRY_FILE);
        this.registryData = loadRegistry();
    }

    /**
     * Load existing planet registry from file
     */
    private RegistryData loadRegistry() {
        try {
            if (Files.exists(registryPath)) {
                String jsonContent = Files.readString(registryPath);
                RegistryData data = GSON.fromJson(jsonContent, RegistryData.class);

                if (data != null && data.planets != null) {
                    Terradyne.LOGGER.info("Loaded existing planet registry with {} planets", data.planets.size());
                    return data;
                }
            }

            // Create new registry if file doesn't exist or is invalid
            Terradyne.LOGGER.info("Creating new planet registry");
            return new RegistryData();

        } catch (Exception e) {
            Terradyne.LOGGER.error("Failed to load planet registry, creating new one", e);
            return new RegistryData();
        }
    }

    /**
     * Save registry to file
     */
    private void saveRegistry() {
        try {
            // Ensure directory exists
            Files.createDirectories(registryPath.getParent());

            // Update modification time and counter
            registryData.lastModified = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            registryData.totalGenerated = registryData.planets.size();

            // Write to file
            String json = GSON.toJson(registryData);
            Files.writeString(registryPath, json);

            Terradyne.LOGGER.debug("Saved planet registry to {}", registryPath);

        } catch (IOException e) {
            Terradyne.LOGGER.error("Failed to save planet registry", e);
        }
    }

    /**
     * Check if a planet has already been generated
     */
    public boolean isPlanetGenerated(String planetName) {
        String normalizedName = planetName.toLowerCase();
        return registryData.planets.containsKey(normalizedName);
    }

    /**
     * Register a newly generated planet
     */
    public void registerGeneratedPlanet(String planetName, String dimensionId, String configHash) {
        String normalizedName = planetName.toLowerCase();

        if (registryData.planets.containsKey(normalizedName)) {
            Terradyne.LOGGER.warn("Planet '{}' is already registered, updating entry", planetName);
        }

        PlanetRegistryEntry entry = new PlanetRegistryEntry(planetName, dimensionId, configHash);
        registryData.planets.put(normalizedName, entry);

        saveRegistry();

        Terradyne.LOGGER.info("✅ Registered generated planet: {} (dimension: {})", planetName, dimensionId);
    }

    /**
     * Get all generated planet names
     */
    public Set<String> getGeneratedPlanetNames() {
        return Set.copyOf(registryData.planets.keySet());
    }

    /**
     * Get registry entry for a planet
     */
    public PlanetRegistryEntry getPlanetEntry(String planetName) {
        String normalizedName = planetName.toLowerCase();
        return registryData.planets.get(normalizedName);
    }

    /**
     * Check if a planet's config has changed since generation
     */
    public boolean hasConfigChanged(String planetName, String newConfigHash) {
        PlanetRegistryEntry entry = getPlanetEntry(planetName);
        if (entry == null) {
            return true; // Not generated yet
        }

        return !newConfigHash.equals(entry.configHash);
    }

    /**
     * Get generation statistics
     */
    public String getGenerationStats() {
        return String.format("Generated planets: %d, Last modified: %s",
                registryData.planets.size(),
                registryData.lastModified != null ? registryData.lastModified : "Never");
    }

    /**
     * Get detailed registry information for debugging
     */
    public String getRegistryInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== EXISTING PLANET REGISTRY ===\n");
        sb.append("Total planets: ").append(registryData.planets.size()).append("\n");
        sb.append("Last modified: ").append(registryData.lastModified).append("\n");
        sb.append("Registry file: ").append(registryPath).append("\n");

        if (!registryData.planets.isEmpty()) {
            sb.append("\nGenerated planets:\n");
            registryData.planets.values().forEach(entry -> {
                sb.append("  - ").append(entry.planetName)
                        .append(" (").append(entry.dimensionId).append(")")
                        .append(" generated at ").append(entry.generatedAt)
                        .append(entry.protectedd ? " [PROTECTED]" : "")
                  .append("\n");
            });
        }

        return sb.toString();
    }

    /**
     * Check registry integrity and report any issues
     */
    public void validateRegistry() {
        Terradyne.LOGGER.info("Validating planet registry...");

        int validPlanets = 0;
        int issues = 0;

        for (Map.Entry<String, PlanetRegistryEntry> entry : registryData.planets.entrySet()) {
            String planetName = entry.getKey();
            PlanetRegistryEntry planetEntry = entry.getValue();

            // Check for missing data
            if (planetEntry.planetName == null || planetEntry.dimensionId == null) {
                Terradyne.LOGGER.warn("Planet '{}' has missing data in registry", planetName);
                issues++;
                continue;
            }

            // Check for name consistency
            if (!planetName.equals(planetEntry.planetName.toLowerCase())) {
                Terradyne.LOGGER.warn("Planet '{}' has inconsistent naming in registry", planetName);
                issues++;
                continue;
            }

            validPlanets++;
        }

        if (issues == 0) {
            Terradyne.LOGGER.info("✅ Planet registry validation passed: {} planets", validPlanets);
        } else {
            Terradyne.LOGGER.warn("⚠️  Planet registry validation found {} issues out of {} planets", issues, registryData.planets.size());
        }
    }

    /**
     * Generate a simple hash for config comparison
     */
    public static String generateConfigHash(Object config) {
        return String.valueOf(config.toString().hashCode());
    }

    /**
     * Get the Terradyne config directory for the current world
     */
    private static Path getTerradyneConfigDirectory(MinecraftServer server) {
        Path worldDir = server.getSavePath(net.minecraft.util.WorldSavePath.ROOT);
        return worldDir.resolve("terradyne");
    }
}