package net.starlight.terradyne.planet.config;

import net.minecraft.server.MinecraftServer;
import net.starlight.terradyne.Terradyne;
import net.starlight.terradyne.planet.dimension.PlanetDimensionManager;
import net.starlight.terradyne.planet.physics.PlanetConfig;
import net.starlight.terradyne.planet.physics.PlanetModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Handles startup-only planet generation
 * Only generates planets that don't already exist to preserve existing worlds
 */
public class StartupPlanetGenerator {

    private final MinecraftServer server;
    private final ExistingPlanetRegistry planetRegistry;

    public StartupPlanetGenerator(MinecraftServer server) {
        this.server = server;
        this.planetRegistry = new ExistingPlanetRegistry(server);
    }

    /**
     * Main startup generation method
     * Called during server startup to generate all configured planets
     */
    public void generateStartupPlanets() {
        Terradyne.LOGGER.info("=== STARTUP PLANET GENERATION ===");

        try {
            // Step 1: Validate existing planet registry
            planetRegistry.validateRegistry();

            // Step 2: Load planet configurations from JSON files
            Map<String, PlanetConfig> planetConfigs = PlanetConfigLoader.loadAllPlanetConfigs(server);

            if (planetConfigs.isEmpty()) {
                Terradyne.LOGGER.info("No planet configurations found - default configs should have been generated");
                return;
            }

            // Step 3: Determine which planets need to be generated
            List<PlanetConfig> planetsToGenerate = determinePlanetsToGenerate(planetConfigs);

            if (planetsToGenerate.isEmpty()) {
                Terradyne.LOGGER.info("All configured planets already exist - no generation needed");
                logExistingPlanets(planetConfigs);
                return;
            }

            // Step 4: Generate new planets
            generateNewPlanets(planetsToGenerate);

            // Step 5: Report final status
            reportGenerationResults(planetConfigs.size(), planetsToGenerate.size());

        } catch (Exception e) {
            Terradyne.LOGGER.error("Startup planet generation failed", e);
        }
    }

    /**
     * Determine which planets need to be generated
     */
    private List<PlanetConfig> determinePlanetsToGenerate(Map<String, PlanetConfig> planetConfigs) {
        List<PlanetConfig> toGenerate = new ArrayList<>();

        Terradyne.LOGGER.info("Checking {} configured planets against existing registry...", planetConfigs.size());

        for (Map.Entry<String, PlanetConfig> entry : planetConfigs.entrySet()) {
            String planetName = entry.getKey();
            PlanetConfig config = entry.getValue();

            if (planetRegistry.isPlanetGenerated(planetName)) {
                // Check if config has changed
                String configHash = ExistingPlanetRegistry.generateConfigHash(config);
                if (planetRegistry.hasConfigChanged(planetName, configHash)) {
                    Terradyne.LOGGER.info("🔒 Planet '{}' exists but config changed - keeping existing (protection enabled)", config.getPlanetName());
                } else {
                    Terradyne.LOGGER.info("✅ Planet '{}' already exists and config unchanged", config.getPlanetName());
                }
            } else {
                Terradyne.LOGGER.info("🆕 Planet '{}' will be generated", config.getPlanetName());
                toGenerate.add(config);
            }
        }

        return toGenerate;
    }

    /**
     * Generate new planets that don't exist yet
     */
    private void generateNewPlanets(List<PlanetConfig> planetsToGenerate) {
        int successCount = 0;
        int failCount = 0;

        Terradyne.LOGGER.info("Generating {} new planets...", planetsToGenerate.size());

        for (PlanetConfig config : planetsToGenerate) {
            try {
                Terradyne.LOGGER.info("Generating planet: {}", config.getPlanetName());

                // Create planet model
                PlanetModel planetModel = new PlanetModel(config);

                // Generate dimension
                var worldKey = PlanetDimensionManager.createPlanet(server, planetModel);

                // Register in existing planets registry
                String dimensionId = worldKey.getValue().toString();
                String configHash = ExistingPlanetRegistry.generateConfigHash(config);
                planetRegistry.registerGeneratedPlanet(config.getPlanetName(), dimensionId, configHash);

                successCount++;
                Terradyne.LOGGER.info("✅ Successfully generated planet: {}", config.getPlanetName());

            } catch (Exception e) {
                failCount++;
                Terradyne.LOGGER.error("❌ Failed to generate planet '{}': {}", config.getPlanetName(), e.getMessage(), e);
            }
        }

        Terradyne.LOGGER.info("Planet generation completed: {} succeeded, {} failed", successCount, failCount);
    }

    /**
     * Log information about existing planets
     */
    private void logExistingPlanets(Map<String, PlanetConfig> planetConfigs) {
        Terradyne.LOGGER.info("Existing planets in registry:");

        for (String planetName : planetConfigs.keySet()) {
            var registryEntry = planetRegistry.getPlanetEntry(planetName);
            if (registryEntry != null) {
                Terradyne.LOGGER.info("  - {} (dimension: {}, generated: {})",
                        registryEntry.planetName,
                        registryEntry.dimensionId,
                        registryEntry.generatedAt);
            }
        }
    }

    /**
     * Report final generation results
     */
    private void reportGenerationResults(int totalConfigured, int newlyGenerated) {
        Terradyne.LOGGER.info("=== STARTUP GENERATION COMPLETE ===");
        Terradyne.LOGGER.info("Configured planets: {}", totalConfigured);
        Terradyne.LOGGER.info("Newly generated: {}", newlyGenerated);
        Terradyne.LOGGER.info("Total existing: {}", planetRegistry.getGeneratedPlanetNames().size());
        Terradyne.LOGGER.info("Registry stats: {}", planetRegistry.getGenerationStats());

        if (newlyGenerated > 0) {
            Terradyne.LOGGER.info("🌍 {} new planets are now available for exploration!", newlyGenerated);
        }
    }

    /**
     * Get list of all available planets (both from config and registry)
     */
    public List<String> getAvailablePlanets() {
        // Get planets from registry (these definitely exist)
        return new ArrayList<>(planetRegistry.getGeneratedPlanetNames());
    }

    /**
     * Check if a specific planet is available
     */
    public boolean isPlanetAvailable(String planetName) {
        return planetRegistry.isPlanetGenerated(planetName);
    }

    /**
     * Get detailed information about all planets
     */
    public String getPlanetInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== TERRADYNE PLANETS ===\n");

        // Load current configs
        Map<String, PlanetConfig> configs = PlanetConfigLoader.loadAllPlanetConfigs(server);

        if (configs.isEmpty()) {
            sb.append("No planet configurations found.\n");
            sb.append("Check ").append(server.getSavePath(net.minecraft.util.WorldSavePath.ROOT)
                    .resolve("terradyne").resolve("planets")).append("\n");
        } else {
            sb.append("Configured planets: ").append(configs.size()).append("\n");

            for (Map.Entry<String, PlanetConfig> entry : configs.entrySet()) {
                String planetName = entry.getKey();
                PlanetConfig config = entry.getValue();

                sb.append("\n").append(config.getPlanetName()).append(":\n");

                if (planetRegistry.isPlanetGenerated(planetName)) {
                    var registryEntry = planetRegistry.getPlanetEntry(planetName);
                    sb.append("  Status: GENERATED ✅\n");
                    sb.append("  Generated: ").append(registryEntry.generatedAt).append("\n");
                    sb.append("  Dimension: ").append(registryEntry.dimensionId).append("\n");
                } else {
                    sb.append("  Status: NOT GENERATED ❌\n");
                    sb.append("  Will be generated on next server restart\n");
                }

                sb.append("  Distance: ").append(config.getDistanceFromStar()).append(" Mkm\n");
                sb.append("  Crust: ").append(config.getCrustComposition().getDisplayName()).append("\n");
                sb.append("  Atmosphere: ").append(config.getAtmosphereComposition().getDisplayName()).append("\n");
            }
        }

        sb.append("\n").append(planetRegistry.getGenerationStats());

        return sb.toString();
    }

    /**
     * Get the planet registry for external access
     */
    public ExistingPlanetRegistry getPlanetRegistry() {
        return planetRegistry;
    }
}