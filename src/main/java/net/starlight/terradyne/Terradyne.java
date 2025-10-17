package net.starlight.terradyne;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.registry.Registry;
import net.minecraft.registry.Registries;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.starlight.terradyne.blocks.ModBlocks;
import net.starlight.terradyne.commands.CommandRegistry;
import net.starlight.terradyne.planet.biome.ModBiomes;
import net.starlight.terradyne.planet.biome.PhysicsBasedBiomeSource;
import net.starlight.terradyne.planet.features.ModFeatures;
import net.starlight.terradyne.planet.physics.PlanetModel;
import net.starlight.terradyne.planet.physics.PlanetModelRegistry;
import net.starlight.terradyne.planet.terrain.UniversalChunkGenerator;
import net.starlight.terradyne.planet.dimension.ModDimensionTypes;
import net.starlight.terradyne.starsystem.DatapackLoader;
import net.starlight.terradyne.starsystem.TerradyneResourceReloadListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main mod class - FIXED for data generation approach
 * Now properly initializes all required systems for tree generation
 */
public class Terradyne implements ModInitializer {
	public static final String MOD_ID = "terradyne";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static MinecraftServer server = null;

	@Override
	public void onInitialize() {
		LOGGER.info("🚀 Initializing Terradyne...");

		// Initialize core systems in correct order
		registerBlocks();
		registerFeatures(); // FIXED: Now properly initializes features
		registerChunkGenerators();
		registerBiomeSources();
		initializeTerrainSystem();
		registerCommands();
		initializeRegistryKeys();
		registerResourceReloadListener(); // NEW: Load datapacks on resource reload
		registerServerEvents();

		LOGGER.info("✅ Terradyne initialized successfully!");
		logSystemStatus();
	}
	
	/**
	 * FIXED: Register blocks
	 */
	private void registerBlocks() {
		try {
			ModBlocks.initialize();
			LOGGER.info("✓ Blocks registered");
		} catch (Exception e) {
			LOGGER.error("❌ Failed to register blocks!", e);
			throw new RuntimeException("Critical block registration failure", e);
		}
	}

	/**
	 * FIXED: Register features - THIS WAS THE MISSING PIECE!
	 * Without this, the PHYSICS_TREE feature isn't registered and tree generation fails
	 */
	private void registerFeatures() {
		try {
			ModFeatures.initialize();
			LOGGER.info("✓ Features registered (including PHYSICS_TREE for tree generation)");
		} catch (Exception e) {
			LOGGER.error("❌ Failed to register features!", e);
			throw new RuntimeException("Critical feature registration failure", e);
		}
	}

	/**
	 * Register chunk generators
	 */
	private void registerChunkGenerators() {
		try {
			Registry.register(
					Registries.CHUNK_GENERATOR,
					new Identifier(MOD_ID, "universal"),
					UniversalChunkGenerator.CODEC
			);
			LOGGER.info("✓ Chunk generator 'terradyne:universal' registered");
		} catch (Exception e) {
			LOGGER.error("❌ Failed to register chunk generator!", e);
			throw new RuntimeException("Critical chunk generator registration failure", e);
		}
	}

	/**
	 * Register biome sources
	 */
	private void registerBiomeSources() {
		try {
			Registry.register(
					Registries.BIOME_SOURCE,
					new Identifier(MOD_ID, "physics_based"),
					PhysicsBasedBiomeSource.CODEC
			);
			LOGGER.info("✓ Biome source 'terradyne:physics_based' registered");
		} catch (Exception e) {
			LOGGER.error("❌ Failed to register biome source!", e);
			throw new RuntimeException("Critical biome source registration failure", e);
		}
	}

	/**
	 * Initialize terrain system
	 */
	private void initializeTerrainSystem() {
		try {
			// Physics system is initialized on-demand
			LOGGER.info("✓ Physics-based terrain system ready");
		} catch (Exception e) {
			LOGGER.error("❌ Failed to initialize terrain system!", e);
			throw new RuntimeException("Critical terrain system failure", e);
		}
	}

	/**
	 * Register commands
	 */
	private void registerCommands() {
		try {
			CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
				CommandRegistry.init(dispatcher);
			});
			LOGGER.info("✓ Commands registered");
		} catch (Exception e) {
			LOGGER.error("❌ Failed to register commands!", e);
			throw new RuntimeException("Critical command registration failure", e);
		}
	}

	/**
	 * Initialize registry keys
	 */
	private void initializeRegistryKeys() {
		try {
			ModBiomes.init();
			ModDimensionTypes.init();
			LOGGER.info("✓ Registry keys initialized");
		} catch (Exception e) {
			LOGGER.error("❌ Failed to initialize registry keys!", e);
			throw new RuntimeException("Critical registry key initialization failure", e);
		}
	}

	/**
	 * Register resource reload listener for datapack loading
	 * CRITICAL: This ensures datapacks load BEFORE dimension deserialization
	 */
	private void registerResourceReloadListener() {
		try {
			// Register for BOTH client and server resource reloads
			// Client reload happens during world creation
			ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES)
				.registerReloadListener(new TerradyneResourceReloadListener());
			
			ResourceManagerHelper.get(ResourceType.SERVER_DATA)
				.registerReloadListener(new TerradyneResourceReloadListener());
			
			LOGGER.info("✓ Resource reload listener registered (CLIENT + SERVER)");
			LOGGER.info("  → Datapacks will load BEFORE dimension deserialization");
			LOGGER.info("  → This fixes the timing issue with chunk generator codec");
		} catch (Exception e) {
			LOGGER.error("❌ Failed to register resource reload listener!", e);
			throw new RuntimeException("Critical resource reload listener registration failure", e);
		}
	}

    /**
     * Register server events
     */
    private void registerServerEvents() {
        try {
            // CRITICAL: Load datapacks BEFORE dimensions deserialize
            ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, success) -> {
                if (success) {
                    LOGGER.info("Loading Terradyne datapacks (BEFORE dimension deserialization)...");
                    try {
                        net.starlight.terradyne.starsystem.DatapackLoader.loadFromDatapacks(player.server.getResourceManager());
                        LOGGER.info("✅ Datapacks loaded successfully - dimensions can now deserialize");
                    } catch (Exception e) {
                        LOGGER.error("❌ CRITICAL: Failed to load datapacks - dimensions will fail!", e);
                        throw new RuntimeException("Failed to load Terradyne datapacks", e);
                    }
                }
            });

            ServerLifecycleEvents.SERVER_STARTING.register(server -> {
                Terradyne.server = server;
                LOGGER.info("Server starting - Terradyne is ready for planet generation");
            });

            // Populate registry after worlds are loaded
            ServerLifecycleEvents.SERVER_STARTED.register(server -> {
                populatePlanetModelRegistry(server);
            });

            ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
                Terradyne.server = null;
                PlanetModelRegistry.clear();
                net.starlight.terradyne.starsystem.DatapackLoader.clear();
                LOGGER.info("Server stopped - Terradyne cleaned up");
            });

            LOGGER.info("✓ Server lifecycle events registered");
        } catch (Exception e) {
            LOGGER.error("❌ Failed to register server events!", e);
            throw new RuntimeException("Critical server event registration failure", e);
        }
    }

	/**
	 * Populate PlanetModelRegistry by scanning all loaded dimensions
	 */
	private void populatePlanetModelRegistry(net.minecraft.server.MinecraftServer server) {
		try {
			LOGGER.info("Populating PlanetModelRegistry from loaded dimensions...");
			int registered = 0;

			// Iterate through all loaded worlds
			for (net.minecraft.server.world.ServerWorld world : server.getWorlds()) {
				var dimensionKey = world.getRegistryKey();

				// Check if this is a Terradyne dimension
				if (dimensionKey.getValue().getNamespace().equals("terradyne")) {
					// Get the chunk generator
					var chunkGenerator = world.getChunkManager().getChunkGenerator();

					if (chunkGenerator instanceof net.starlight.terradyne.planet.terrain.UniversalChunkGenerator universalGenerator) {
						PlanetModel planetModel = universalGenerator.getPlanetModel();

						if (planetModel != null) {
							PlanetModelRegistry.register(dimensionKey.getValue(), planetModel);
							registered++;
							LOGGER.info("✓ Registered PlanetModel for dimension: {} ({})",
									dimensionKey.getValue(), planetModel.getConfig().getPlanetName());
						} else {
							LOGGER.warn("UniversalChunkGenerator has null PlanetModel for: {}", dimensionKey.getValue());
						}
					} else {
						LOGGER.debug("Dimension {} uses non-Universal chunk generator: {}",
								dimensionKey.getValue(), chunkGenerator.getClass().getSimpleName());
					}
				}
			}

			LOGGER.info("✅ PlanetModelRegistry populated with {} planet models", registered);

			if (registered == 0) {
				LOGGER.warn("⚠️  No Terradyne dimensions found during registry population. " +
						"Colors will be populated when chunks are first generated.");
			}

		} catch (Exception e) {
			LOGGER.error("❌ Failed to populate PlanetModelRegistry: {}", e.getMessage(), e);
		}
	}

	/**
	 * Log system status
	 */
	private void logSystemStatus() {
		LOGGER.info("=== TERRADYNE SYSTEM STATUS ===");
		LOGGER.info("• Planet types: {} implemented", getPlanetTypeCount());
		LOGGER.info("• Terrain: Physics-based with height range Y 0-255");
		LOGGER.info("• Biomes: Data-driven (JSON generation)");
		LOGGER.info("• Features: Component-based tree generation");
		LOGGER.info("• Trees: {} types with proper placement rules", getTreeTypeCount());
		LOGGER.info("• Generation: PHYSICS_TREE feature registered ✓");
		LOGGER.info("");
		LOGGER.info("🔧 Development Steps:");
		LOGGER.info("  1. Run 'gradlew runDatagen' to generate biome/feature JSON files");
		LOGGER.info("  2. Use '/terradyne create <n> <type>' to create planets");
		LOGGER.info("  3. Trees should generate in appropriate biomes based on physics");
		LOGGER.info("");
		LOGGER.info("🌳 Tree generation: ENABLED via data generation approach");
	}

	/**
	 * Get planet type count
	 */
	private int getPlanetTypeCount() {
		try {
			Class<?> planetTypeClass = Class.forName("net.starlight.terradyne.planet.PlanetType");
			java.lang.reflect.Method method = planetTypeClass.getMethod("getImplementedTypes");
			Object[] types = (Object[]) method.invoke(null);
			return types.length;
		} catch (Exception e) {
			return 3; // Fallback
		}
	}

	/**
	 * Get tree type count
	 */
	private int getTreeTypeCount() {
		try {
			Class<?> treeTypeClass = Class.forName("net.starlight.terradyne.planet.biology.BiomeFeatureComponents$TreeType");
			Object[] types = treeTypeClass.getEnumConstants();
			return types.length;
		} catch (Exception e) {
			return 11; // Fallback - we know there are 11 tree types
		}
	}
}