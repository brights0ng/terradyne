package net.starlight.terradyne.planet.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.starlight.terradyne.Terradyne;
import net.starlight.terradyne.planet.physics.PlanetModel;

import java.util.stream.Stream;

import static net.starlight.terradyne.Terradyne.server;

/**
 * Physics-based biome source that uses planetary data instead of noise
 * Implements the complete Terradyne biome classification system
 */
public class PhysicsBasedBiomeSource extends BiomeSource {

    public static final Codec<PhysicsBasedBiomeSource> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("planet_name").forGetter(source -> source.planetName)
            ).apply(instance, PhysicsBasedBiomeSource::new)
    );

    private final String planetName;
    private PlanetModel planetModel; // Set after construction
    private BiomeClassificationSystem classifier;

    /**
     * Constructor for codec deserialization
     */
    public PhysicsBasedBiomeSource(String planetName) {
        this.planetName = planetName;
        // planetModel will be set later via setPlanetModel()
    }

    /**
     * Constructor for direct creation
     */
    public PhysicsBasedBiomeSource(PlanetModel planetModel) {
        this.planetModel = planetModel;
        this.planetName = planetModel.getConfig().getPlanetName();
        this.classifier = new BiomeClassificationSystem(planetModel);
    }

    /**
     * Set planet model after codec construction
     */
    public void setPlanetModel(PlanetModel planetModel) {
        this.planetModel = planetModel;
        this.classifier = new BiomeClassificationSystem(planetModel);
        Terradyne.LOGGER.info("PhysicsBasedBiomeSource initialized for planet: {}", planetName);
    }

    @Override
    protected Codec<? extends BiomeSource> getCodec() {
        return CODEC;
    }

    /**
     * Main biome assignment method - called for every block/chunk
     */
    @Override
    public RegistryEntry<Biome> getBiome(int x, int y, int z, MultiNoiseUtil.MultiNoiseSampler sampler) {
        if (planetModel == null || classifier == null) {
            Terradyne.LOGGER.warn("PhysicsBasedBiomeSource not initialized for {}, using debug biome", planetName);
            return getBiomeEntry(ModBiomes.DEBUG);
        }

        try {
            // Convert coordinates to world space (ignore Y for surface biomes)
            int worldX = x << 2; // Biome coordinates are 1/4 resolution
            int worldZ = z << 2;

            // Use classifier to determine biome
            RegistryKey<Biome> biomeKey = classifier.classifyBiome(worldX, worldZ);

            // Get registry entry
            return getBiomeEntry(biomeKey);

        } catch (Exception e) {
            Terradyne.LOGGER.error("Error classifying biome at ({}, {}, {}): {}", x, y, z, e.getMessage());
            return getBiomeEntry(ModBiomes.DEBUG);
        }
    }

    /**
     * Get all possible biomes this source can generate
     */
    @Override
    protected Stream<RegistryEntry<Biome>> biomeStream() {
        // For 1.20.1, we'll return a stream of our known biomes
        // This is used by Minecraft for validation and caching
        return Stream.of(
                // Water biomes
                ModBiomes.FROZEN_OCEAN, ModBiomes.FRIGID_OCEAN, ModBiomes.DEAD_OCEAN,
                ModBiomes.OCEAN, ModBiomes.WARM_OCEAN, ModBiomes.CORAL_OCEAN,
                ModBiomes.TROPICAL_OCEAN, ModBiomes.BOILING_OCEAN,
                // Mountain biomes
                ModBiomes.FROZEN_PEAKS, ModBiomes.MOUNTAIN_FOOTHILLS, ModBiomes.MOUNTAIN_PEAKS,
                ModBiomes.ALPINE_PEAKS, ModBiomes.VOLCANIC_WASTELAND, ModBiomes.VOLCANIC_MOUNTAINS,
                // Highland biomes
                ModBiomes.BARREN_HIGHLANDS, ModBiomes.WINDSWEPT_HILLS, ModBiomes.ROLLING_HILLS,
                ModBiomes.HIGHLAND_TUNDRA, ModBiomes.FORESTED_HILLS, ModBiomes.TROPICAL_HIGHLANDS,
                // Hostile continental
                ModBiomes.FROZEN_WASTELAND, ModBiomes.ROCKY_DESERT, ModBiomes.SCORCHED_PLAINS,
                ModBiomes.WINDSWEPT_TUNDRA, ModBiomes.SANDY_DESERT, ModBiomes.DESERT_MESA, ModBiomes.DUST_BOWL,
                // Marginal continental
                ModBiomes.COLD_STEPPES, ModBiomes.TUNDRA, ModBiomes.BOREAL_PLAINS,
                ModBiomes.DRY_STEPPES, ModBiomes.TEMPERATE_STEPPES, ModBiomes.MEADOWS,
                ModBiomes.SAVANNA, ModBiomes.TROPICAL_GRASSLAND,
                // Thriving continental
                ModBiomes.SNOWY_PLAINS, ModBiomes.TAIGA, ModBiomes.SNOW_FOREST, ModBiomes.ALPINE_MEADOWS,
                ModBiomes.PLAINS, ModBiomes.MIXED_PLAINS, ModBiomes.WETLANDS,
                ModBiomes.OAK_FOREST, ModBiomes.MIXED_FOREST, ModBiomes.DENSE_FOREST, ModBiomes.MOUNTAIN_FOREST,
                ModBiomes.HOT_SHRUBLAND, ModBiomes.WINDY_STEPPES, ModBiomes.TEMPERATE_RAINFOREST,
                ModBiomes.CLOUD_FOREST, ModBiomes.JUNGLE, ModBiomes.TROPICAL_RAINFOREST,
                ModBiomes.HOT_DESERT, ModBiomes.TROPICAL_SWAMP,
                // Extreme biomes
                ModBiomes.EXTREME_FROZEN_WASTELAND, ModBiomes.MOLTEN_WASTELAND,
                // Debug
                ModBiomes.DEBUG
        ).map(this::getBiomeEntry);
    }

    /**
     * Get biome registry entry - works with server registry manager in 1.20.1
     */
    private RegistryEntry<Biome> getBiomeEntry(RegistryKey<Biome> biomeKey) {
            return server.getRegistryManager().get(net.minecraft.registry.RegistryKeys.BIOME)
                    .getEntry(biomeKey).orElseThrow(() ->
                            new RuntimeException("Biome not found: " + biomeKey.getValue()));


    }

    /**
     * Get planet name for debugging
     */
    public String getPlanetName() {
        return planetName;
    }

    /**
     * Check if biome source is properly initialized
     */
    public boolean isInitialized() {
        return planetModel != null && classifier != null;
    }

    @Override
    public String toString() {
        return String.format("PhysicsBasedBiomeSource{planet=%s, initialized=%s}",
                planetName, isInitialized());
    }
}