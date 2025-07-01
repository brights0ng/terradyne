package net.starlight.terradyne.planet.world;

import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;
import net.starlight.terradyne.Terradyne;
import net.starlight.terradyne.planet.chunk.UniversalChunkGenerator;
import net.starlight.terradyne.planet.config.ExistingPlanetRegistry;
import net.starlight.terradyne.planet.config.PlanetConfigLoader;
import net.starlight.terradyne.planet.dimension.DimensionTypeFactory;
import net.starlight.terradyne.planet.dimension.ModDimensionTypes;
import net.starlight.terradyne.planet.dimension.PlanetDimensionManager;
import net.starlight.terradyne.planet.physics.PlanetConfig;
import net.starlight.terradyne.planet.physics.PlanetModel;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Handles dimension injection during server startup via Mixin
 * This is called from DimensionRegistryMixin to add dimensions early enough
 * that they get saved to level.dat
 */
public class DimensionInjector {

    /**
     * Inject Terradyne dimensions into the server's dimension registry
     * Called from mixin during server creation
     */
    public static void injectDimensions(MinecraftServer server) {
        try {
            Terradyne.LOGGER.info("Starting dimension injection for new world...");

            // Only inject for new worlds
            if (!isNewWorld(server)) {
                Terradyne.LOGGER.info("Existing world detected - skipping dimension injection");
                return;
            }

            // Load planet configurations
            Map<String, PlanetConfig> planetConfigs = PlanetConfigLoader.loadAllPlanetConfigs(server);

            if (planetConfigs.isEmpty()) {
                Terradyne.LOGGER.info("No planet configurations found - skipping dimension injection");
                return;
            }

            Terradyne.LOGGER.info("Injecting {} planet dimensions...", planetConfigs.size());

            // Get dimension registry through reflection
            Map<RegistryKey<DimensionOptions>, DimensionOptions> dimensionRegistry = getDimensionRegistry(server);

            if (dimensionRegistry == null) {
                throw new RuntimeException("Could not access dimension registry");
            }

            // Create registry for tracking
            ExistingPlanetRegistry registry = new ExistingPlanetRegistry(server);

            // Inject each planet dimension
            for (Map.Entry<String, PlanetConfig> entry : planetConfigs.entrySet()) {
                try {
                    injectSingleDimension(server, dimensionRegistry, entry.getValue(), registry);
                } catch (Exception e) {
                    Terradyne.LOGGER.error("Failed to inject dimension for planet: {}", entry.getKey(), e);
                }
            }

            Terradyne.LOGGER.info("✅ Dimension injection completed - {} dimensions added", planetConfigs.size());

        } catch (Exception e) {
            Terradyne.LOGGER.error("Critical failure in dimension injection", e);
        }
    }

    /**
     * Inject a single planet dimension
     */
    private static void injectSingleDimension(MinecraftServer server,
                                              Map<RegistryKey<DimensionOptions>, DimensionOptions> dimensionRegistry,
                                              PlanetConfig config,
                                              ExistingPlanetRegistry registry) {
        String planetName = config.getPlanetName().toLowerCase().replace(" ", "_");

        Terradyne.LOGGER.info("Injecting dimension for planet: {}", config.getPlanetName());

        try {
            // Create planet model
            PlanetModel planetModel = new PlanetModel(config);

            // Create dimension identifier
            Identifier dimensionId = new Identifier(Terradyne.MOD_ID, planetName);
            RegistryKey<DimensionOptions> dimensionOptionsKey = RegistryKey.of(RegistryKeys.DIMENSION, dimensionId);

            // Select appropriate dimension type
            ModDimensionTypes.TerradyneDimensionType selectedType = DimensionTypeFactory.selectDimensionType(planetModel);
            RegistryKey<DimensionType> dimensionTypeKey = ModDimensionTypes.getRegistryKey(selectedType);

            // Get dimension type from registry
            Registry<DimensionType> dimensionTypeRegistry = server.getRegistryManager().get(RegistryKeys.DIMENSION_TYPE);
            var dimensionTypeEntry = dimensionTypeRegistry.getEntry(dimensionTypeKey)
                    .orElseThrow(() -> new RuntimeException("Dimension type not found: " + dimensionTypeKey.getValue()));

            // Create biome source
            BiomeSource biomeSource = PlanetDimensionManager.createPlanetBiomeSource(server, planetModel);

            // Create chunk generator
            UniversalChunkGenerator chunkGenerator = new UniversalChunkGenerator(planetModel, biomeSource);

            // Create dimension options
            DimensionOptions dimensionOptions = new DimensionOptions(dimensionTypeEntry, chunkGenerator);

            // THIS IS THE KEY: Add to the dimension registry directly
            dimensionRegistry.put(dimensionOptionsKey, dimensionOptions);

            // Register in our tracking registry
            String configHash = ExistingPlanetRegistry.generateConfigHash(config);
            registry.registerGeneratedPlanet(config.getPlanetName(), dimensionId.toString(), configHash);

            Terradyne.LOGGER.info("✅ Injected dimension: {} (type: {})",
                    dimensionId, selectedType.getName());

        } catch (Exception e) {
            Terradyne.LOGGER.error("Failed to inject planet dimension: {}", planetName, e);
            throw e;
        }
    }

    /**
     * Get access to the server's dimension registry using reflection
     * Updated: GeneratorOptions doesn't contain dimensions in 1.20.1
     */
    @SuppressWarnings("unchecked")
    private static Map<RegistryKey<DimensionOptions>, DimensionOptions> getDimensionRegistry(MinecraftServer server) {
        try {
            Terradyne.LOGGER.debug("Searching for dimension registry in server...");

            // Approach 1: Through SaveProperties - look for world gen settings or dimension data
            var saveProperties = server.getSaveProperties();

            // Try to find dimension-related methods in SaveProperties
            Method[] savePropertiesMethods = saveProperties.getClass().getDeclaredMethods();
            for (Method method : savePropertiesMethods) {
                if (method.getParameterCount() == 0) {
                    String methodName = method.getName().toLowerCase();
                    if (methodName.contains("dimension") || methodName.contains("world")) {
                        try {
                            method.setAccessible(true);
                            Object result = method.invoke(saveProperties);

                            if (result instanceof Map<?, ?> map && !map.isEmpty()) {
                                Object firstKey = map.keySet().iterator().next();
                                if (firstKey instanceof RegistryKey<?> registryKey &&
                                        registryKey.getRegistry().equals(RegistryKeys.DIMENSION)) {

                                    Terradyne.LOGGER.debug("Found dimension registry via SaveProperties.{}", method.getName());
                                    return (Map<RegistryKey<DimensionOptions>, DimensionOptions>) map;
                                }
                            }
                        } catch (Exception e) {
                            // Continue searching
                        }
                    }
                }
            }

            // Approach 2: Through reflection on SaveProperties fields
            Field[] savePropertiesFields = saveProperties.getClass().getDeclaredFields();
            for (Field field : savePropertiesFields) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(saveProperties);

                    if (value instanceof Map<?, ?> map && !map.isEmpty()) {
                        Object firstKey = map.keySet().iterator().next();
                        if (firstKey instanceof RegistryKey<?> registryKey &&
                                registryKey.getRegistry().equals(RegistryKeys.DIMENSION)) {

                            Terradyne.LOGGER.debug("Found dimension registry via SaveProperties field: {}", field.getName());
                            return (Map<RegistryKey<DimensionOptions>, DimensionOptions>) map;
                        }
                    }

                    // Check if field contains an object that might have dimension data
                    if (value != null && !value.getClass().isPrimitive()) {
                        Field[] nestedFields = value.getClass().getDeclaredFields();
                        for (Field nestedField : nestedFields) {
                            if (nestedField.getType() == Map.class) {
                                try {
                                    nestedField.setAccessible(true);
                                    Object nestedValue = nestedField.get(value);

                                    if (nestedValue instanceof Map<?, ?> nestedMap && !nestedMap.isEmpty()) {
                                        Object firstKey = nestedMap.keySet().iterator().next();
                                        if (firstKey instanceof RegistryKey<?> registryKey &&
                                                registryKey.getRegistry().equals(RegistryKeys.DIMENSION)) {

                                            Terradyne.LOGGER.debug("Found dimension registry via nested field: {}.{}", field.getName(), nestedField.getName());
                                            return (Map<RegistryKey<DimensionOptions>, DimensionOptions>) nestedMap;
                                        }
                                    }
                                } catch (Exception e) {
                                    // Continue searching
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    // Continue searching
                }
            }

            // Approach 3: Through reflection on MinecraftServer - look for dimension maps
            Field[] serverFields = MinecraftServer.class.getDeclaredFields();
            for (Field field : serverFields) {
                if (field.getType() == Map.class) {
                    try {
                        field.setAccessible(true);
                        Object value = field.get(server);

                        if (value instanceof Map<?, ?> map && !map.isEmpty()) {
                            Object firstKey = map.keySet().iterator().next();
                            if (firstKey instanceof RegistryKey<?> registryKey &&
                                    registryKey.getRegistry().equals(RegistryKeys.DIMENSION)) {

                                Terradyne.LOGGER.debug("Found dimension registry via MinecraftServer field: {}", field.getName());
                                return (Map<RegistryKey<DimensionOptions>, DimensionOptions>) map;
                            }
                        }
                    } catch (Exception e) {
                        // Continue searching
                    }
                }
            }

            // Approach 4: Try to access DynamicRegistryManager and modify it
            try {
                var registryManager = server.getRegistryManager();
                var dimensionRegistry = registryManager.get(RegistryKeys.DIMENSION);

                // Try reflection to get the backing map
                Field[] registryFields = dimensionRegistry.getClass().getDeclaredFields();
                for (Field field : registryFields) {
                    if (field.getType() == Map.class) {
                        try {
                            field.setAccessible(true);
                            Object value = field.get(dimensionRegistry);

                            if (value instanceof Map<?, ?> map) {
                                Terradyne.LOGGER.debug("Found dimension registry via DynamicRegistry field: {}", field.getName());
                                return (Map<RegistryKey<DimensionOptions>, DimensionOptions>) map;
                            }
                        } catch (Exception e) {
                            // Continue searching
                        }
                    }
                }
            } catch (Exception e) {
                Terradyne.LOGGER.debug("DynamicRegistryManager approach failed: {}", e.getMessage());
            }

            // If we get here, log debug information
            Terradyne.LOGGER.error("Could not find dimension registry. Debugging information:");
            Terradyne.LOGGER.error("SaveProperties class: {}", saveProperties.getClass().getName());
            Terradyne.LOGGER.error("Available SaveProperties methods:");
            for (Method method : savePropertiesMethods) {
                if (method.getParameterCount() == 0) {
                    Terradyne.LOGGER.error("  - {} -> {}", method.getName(), method.getReturnType().getSimpleName());
                }
            }

            throw new RuntimeException("Could not find mutable dimension registry using any approach");

        } catch (Exception e) {
            Terradyne.LOGGER.error("Failed to access dimension registry: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Check if this is a new world that needs dimension injection
     */
    private static boolean isNewWorld(MinecraftServer server) {
        Path registryPath = getTerradyneConfigDirectory(server).resolve("planet_registry.json");
        boolean hasExistingRegistry = Files.exists(registryPath);

        Terradyne.LOGGER.debug("New world check: hasExistingRegistry={}", hasExistingRegistry);
        return !hasExistingRegistry;
    }

    /**
     * Get Terradyne config directory
     */
    private static Path getTerradyneConfigDirectory(MinecraftServer server) {
        return server.getSavePath(net.minecraft.util.WorldSavePath.ROOT).resolve("terradyne");
    }
}