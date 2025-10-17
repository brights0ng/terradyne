package net.starlight.terradyne.starsystem;

import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.starlight.terradyne.Terradyne;

/**
 * Resource reload listener that loads Terradyne datapacks at the correct time
 *
 * CRITICAL TIMING:
 * This fires DURING resource loading (SaveLoading.load() on world creation)
 * which is BEFORE dimension deserialization but AFTER datapacks are loaded.
 *
 * This ensures StarSystemRegistry and CelestialObjectRegistry are populated
 * before UniversalChunkGenerator.fromCodec() tries to look up celestial objects.
 */
public class TerradyneResourceReloadListener implements SimpleSynchronousResourceReloadListener {

    @Override
    public Identifier getFabricId() {
        return new Identifier("terradyne", "datapack_loader");
    }

    @Override
    public void reload(ResourceManager manager) {
        Terradyne.LOGGER.info("==========================================================");
        Terradyne.LOGGER.info("🔄 TERRADYNE RESOURCE RELOAD TRIGGERED");
        Terradyne.LOGGER.info("==========================================================");
        Terradyne.LOGGER.info("Thread: {}", Thread.currentThread().getName());
        Terradyne.LOGGER.info("Context: Resource reload (world creation or /reload)");
        Terradyne.LOGGER.info("Timing: BEFORE dimension deserialization");

        try {
            // Load celestial objects and star systems from datapacks
            DatapackLoader.loadFromDatapacks(manager);

            // Verify loading
            int objectCount = CelestialObjectRegistry.getAll().size();
            int systemCount = StarSystemRegistry.getAll().size();

            Terradyne.LOGGER.info("==========================================================");
            Terradyne.LOGGER.info("✅ TERRADYNE DATAPACKS LOADED SUCCESSFULLY");
            Terradyne.LOGGER.info("==========================================================");
            Terradyne.LOGGER.info("Celestial Objects: {}", objectCount);
            Terradyne.LOGGER.info("Star Systems: {}", systemCount);

            if (objectCount > 0) {
                Terradyne.LOGGER.info("Available celestial objects:");
                CelestialObjectRegistry.getAll().forEach((id, entry) ->
                        Terradyne.LOGGER.info("  - {} ({})", id, entry.type)
                );
            } else {
                Terradyne.LOGGER.warn("⚠️  No celestial objects found in datapacks!");
                Terradyne.LOGGER.warn("Expected location: data/[namespace]/terradyne/celestial_objects/*.json");
            }

            if (systemCount > 0) {
                Terradyne.LOGGER.info("Available star systems:");
                StarSystemRegistry.getAll().forEach((id, system) ->
                        Terradyne.LOGGER.info("  - {} ({} objects)", id, system.getObjects().size())
                );
            }

            Terradyne.LOGGER.info("==========================================================");
            Terradyne.LOGGER.info("Dimension deserialization can now proceed safely!");
            Terradyne.LOGGER.info("==========================================================");

        } catch (Exception e) {
            Terradyne.LOGGER.error("==========================================================");
            Terradyne.LOGGER.error("❌ CRITICAL: Failed to load Terradyne datapacks!");
            Terradyne.LOGGER.error("==========================================================");
            Terradyne.LOGGER.error("Error: {}", e.getMessage(), e);
            Terradyne.LOGGER.error("World creation/reload will likely fail!");
            Terradyne.LOGGER.error("==========================================================");

            // Don't throw - let Minecraft continue, but dimension generation will fail
            // This prevents total crash and allows debugging
        }
    }
}