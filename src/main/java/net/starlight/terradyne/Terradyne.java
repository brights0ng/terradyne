// TerradyneMod.java
package net.starlight.terradyne;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

import net.starlight.terradyne.planet.world.PlanetDimensionManager;
import net.starlight.terradyne.commands.PlanetCommands;

/**
 * Main mod initialization class for Terradyne.
 * Handles registration of commands and system initialization.
 * Dimension types are registered through data generation.
 */
public class Terradyne implements ModInitializer {

    public static final String MOD_ID = "terradyne";

    @Override
    public void onInitialize() {
        System.out.println("Initializing Terradyne - Realistic Planet Generation");

        // Initialize dimension management
        PlanetDimensionManager.initialize();

        // Register commands
        registerCommands();

        // Register server lifecycle events
        registerServerEvents();

        System.out.println("Terradyne initialization complete!");
        System.out.println("Note: Dimension types must be registered through data generation");
    }

    /**
     * Register planet management commands
     */
    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            PlanetCommands.register(dispatcher);
        });

        System.out.println("Registered Terradyne commands");
    }

    /**
     * Register server lifecycle events
     */
    private void registerServerEvents() {
        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);
    }

    /**
     * Called when server starts - good place for additional initialization
     */
    private void onServerStarted(MinecraftServer server) {
        System.out.println("Terradyne: Server started, planet system ready");

        // Verify our dimension type was registered through data generation
        boolean dimensionTypeExists = server.getRegistryManager()
                .get(net.minecraft.registry.RegistryKeys.DIMENSION_TYPE)
                .containsId(new net.minecraft.util.Identifier("terradyne", "planet"));

        if (dimensionTypeExists) {
            System.out.println("✓ Planet dimension type successfully registered");
        } else {
            System.err.println("✗ Planet dimension type not found - check data generation");
        }
    }

    /**
     * Called when server stops - cleanup
     */
    private void onServerStopping(MinecraftServer server) {
        System.out.println("Terradyne: Server stopping, cleaning up planet systems");
    }
}