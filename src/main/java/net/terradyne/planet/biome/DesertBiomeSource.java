// DesertBiomeSource.java - FIXED to use server-based biome registration

package net.terradyne.planet.biome;

import com.mojang.serialization.Codec;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.noise.SimplexNoiseSampler;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.terradyne.planet.model.DesertModel;
import net.terradyne.Terradyne;

import java.util.List;
import java.util.stream.Stream;

public class DesertBiomeSource extends BiomeSource {
    public static final Codec<DesertBiomeSource> CODEC = Codec.unit(DesertBiomeSource::new);

    private DesertModel desertModel;
    private SimplexNoiseSampler biomeNoise;

    // Custom biome entries (will be set during initialization)
    private RegistryEntry<Biome> duneSeaBiome;
    private RegistryEntry<Biome> graniteMesasBiome;
    private RegistryEntry<Biome> limestoneCanyonsBiome;
    private RegistryEntry<Biome> saltFlatsBiome;

    private List<RegistryEntry<Biome>> biomeList;
    private boolean registryInitialized = false;

    // Codec constructor (required)
    public DesertBiomeSource() {}

    // Actual constructor
    public DesertBiomeSource(DesertModel desertModel) {
        this.desertModel = desertModel;
        this.biomeNoise = new SimplexNoiseSampler(Random.create(desertModel.getConfig().getSeed()));

        Terradyne.LOGGER.info("DesertBiomeSource created - will register and use custom biomes");
    }

    /**
     * Initialize with server registry - register custom biomes and cache entries
     */
    public void init(MinecraftServer server) {
        if (registryInitialized) {
            return; // Already initialized
        }

        try {
            Terradyne.LOGGER.info("=== INITIALIZING CUSTOM BIOME MAPPINGS ===");

            // First, ensure custom biomes are registered with the server
            ModBiomes.init(server);

            // Now get the registered biome entries
            duneSeaBiome = ModBiomes.getBiomeEntry(ModBiomes.DUNE_SEA);
            graniteMesasBiome = ModBiomes.getBiomeEntry(ModBiomes.GRANITE_MESAS);
            limestoneCanyonsBiome = ModBiomes.getBiomeEntry(ModBiomes.LIMESTONE_CANYONS);
            saltFlatsBiome = ModBiomes.getBiomeEntry(ModBiomes.SALT_FLATS);

            // Create ordered list for selection
            biomeList = List.of(duneSeaBiome, graniteMesasBiome, limestoneCanyonsBiome, saltFlatsBiome);

            this.registryInitialized = true;

            Terradyne.LOGGER.info("✅ Custom biome mappings initialized:");
            Terradyne.LOGGER.info("  [0] Dune Sea → {} (rolling sand dunes)", ModBiomes.DUNE_SEA.getValue());
            Terradyne.LOGGER.info("  [1] Granite Mesas → {} (flat-topped plateaus)", ModBiomes.GRANITE_MESAS.getValue());
            Terradyne.LOGGER.info("  [2] Limestone Canyons → {} (deep valleys)", ModBiomes.LIMESTONE_CANYONS.getValue());
            Terradyne.LOGGER.info("  [3] Salt Flats → {} (crystalline plains)", ModBiomes.SALT_FLATS.getValue());

        } catch (Exception e) {
            Terradyne.LOGGER.error("❌ Failed to initialize custom biome mappings: {}", e.getMessage());
            e.printStackTrace();

            // EMERGENCY FALLBACK: Use vanilla desert if custom biomes fail
            Terradyne.LOGGER.warn("⚠️  Using vanilla desert biome as emergency fallback");
            Registry<Biome> biomeRegistry = server.getRegistryManager().get(RegistryKeys.BIOME);
            duneSeaBiome = biomeRegistry.getEntry(BiomeKeys.DESERT).orElseThrow();
            biomeList = List.of(duneSeaBiome);
            this.registryInitialized = true;
        }
    }

    @Override
    protected Codec<? extends BiomeSource> getCodec() {
        return CODEC;
    }

    @Override
    protected Stream<RegistryEntry<Biome>> biomeStream() {
        if (!registryInitialized || biomeList == null) {
            return Stream.empty();
        }
        return biomeList.stream();
    }

    @Override
    public RegistryEntry<Biome> getBiome(int x, int y, int z, MultiNoiseUtil.MultiNoiseSampler sampler) {
        // If registry not initialized, this is an error
        if (!registryInitialized || biomeList == null || biomeList.isEmpty()) {
            Terradyne.LOGGER.error("Biome source not properly initialized!");
            throw new IllegalStateException("Biome source not properly initialized - call initializeWithServer first");
        }

        try {
            // Use noise to select one of the 4 custom biomes (0-3)
            double noiseValue = (biomeNoise.sample(x * 0.0008, 0, z * 0.0008) + 1.0) * 0.5; // 0-1
            int biomeIndex = (int) (noiseValue * 4); // 0, 1, 2, or 3
            biomeIndex = Math.max(0, Math.min(biomeIndex, 3)); // Clamp to 0-3

            return biomeList.get(biomeIndex);

        } catch (Exception e) {
            Terradyne.LOGGER.warn("Error in getBiome() at {},{},{}: {}, using fallback",
                    x, y, z, e.getMessage());
            return duneSeaBiome; // Fallback to dune sea
        }
    }

    public IBiomeType getBiomeTypeAt(int worldX, int worldZ) {
        try {
            // Use same logic as getBiome() to get the biome type
            double noiseValue = (biomeNoise.sample(worldX * 0.0008, 0, worldZ * 0.0008) + 1.0) * 0.5;
            int biomeIndex = (int) (noiseValue * 4);
            biomeIndex = Math.max(0, Math.min(biomeIndex, 3));

            return switch (biomeIndex) {
                case 0 -> DesertBiomeType.DUNE_SEA;
                case 1 -> DesertBiomeType.GRANITE_MESAS;
                case 2 -> DesertBiomeType.LIMESTONE_CANYONS;
                case 3 -> DesertBiomeType.SALT_FLATS;
                default -> DesertBiomeType.DUNE_SEA;
            };

        } catch (Exception e) {
            Terradyne.LOGGER.warn("Error selecting biome type at {},{}: {}, using DUNE_SEA",
                    worldX, worldZ, e.getMessage());
            return DesertBiomeType.DUNE_SEA;
        }
    }

    public DesertModel getDesertModel() {
        return desertModel;
    }

    // Updated method for getting biome percentages
    public String getBiomeDistribution() {
        return "Custom Dune Sea (25%), Custom Granite Mesas (25%), Custom Limestone Canyons (25%), Custom Salt Flats (25%)";
    }
}