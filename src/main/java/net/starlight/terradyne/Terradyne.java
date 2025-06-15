package net.starlight.terradyne;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.starlight.terradyne.util.CommandRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main mod class for Terradyne
 * Physics-based planetary generation
 */
public class Terradyne implements ModInitializer {
    
    public static final String MOD_ID = "terradyne";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    @Override
    public void onInitialize() {
        LOGGER.info("🚀 Initializing Terradyne Physics Engine...");
        
        // Initialize core systems
        initializeTerrainSystem();
        registerCommands();
        
        LOGGER.info("✅ Terradyne initialized successfully!");
        logSystemStatus();
    }
    
    private void initializeTerrainSystem() {
        try {
            // Initialize registries
            LOGGER.info("✓ Generation systems initialized");
            
        } catch (Exception e) {
            LOGGER.error("❌ Failed to initialize terrain system!", e);
            throw new RuntimeException("Critical terrain system failure", e);
        }
    }
    
    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            CommandRegistry.init(dispatcher);
        });
        LOGGER.info("✓ Commands registered");
    }
    
    private void logSystemStatus() {
        LOGGER.info("=== PHYSICS ENGINE STATUS ===");
        LOGGER.info("• Generation: Physics-based planetary simulation");
        LOGGER.info("• Tectonics: Voronoi plate system");
        LOGGER.info("• Terrain: Pass-based generation");
        LOGGER.info("• Biomes: Emergent from physical conditions");
        LOGGER.info("");
        LOGGER.info("🎮 Use '/terradyne create <n> <preset>' to create planets");
        LOGGER.info("🔬 Available presets: test, earth, mars, venus");
    }
}