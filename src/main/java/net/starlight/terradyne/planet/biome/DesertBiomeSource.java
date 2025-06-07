package net.starlight.terradyne.planet.biome;

import com.mojang.serialization.Codec;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.noise.SimplexNoiseSampler;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.starlight.terradyne.Terradyne;
import net.starlight.terradyne.planet.config.DesertConfig;
import net.starlight.terradyne.planet.model.DesertModel;

import java.util.*;
import java.util.stream.Stream;

/**
 * Biome source for desert planets
 * Loads custom biomes from data generation, falls back to vanilla biomes if needed
 */
public class DesertBiomeSource extends BiomeSource {
    public static final Codec<DesertBiomeSource> CODEC = Codec.unit(DesertBiomeSource::new);

    private final DesertModel model;
    private final BiomeWeights biomeWeights;
    private final SimplexNoiseSampler biomeNoise;

    // Biome registry entries (loaded from dynamic registry or fallbacks)
    private final RegistryEntry<Biome> duneSeaBiome;
    private final RegistryEntry<Biome> graniteMesasBiome;
    private final RegistryEntry<Biome> limestoneCanyonsBiome;
    private final RegistryEntry<Biome> saltFlatsBiome;

    private final boolean usingCustomBiomes;

    // Default constructor for codec
    public DesertBiomeSource() {
        this.model = null;
        this.biomeWeights = null;
        this.biomeNoise = null;
        this.duneSeaBiome = null;
        this.graniteMesasBiome = null;
        this.limestoneCanyonsBiome = null;
        this.saltFlatsBiome = null;
        this.usingCustomBiomes = false;
    }

    public DesertBiomeSource(DesertModel model, MinecraftServer server) {
        this.model = model;
        this.biomeWeights = BiomeWeights.calculate(model);
        this.biomeNoise = new SimplexNoiseSampler(Random.create(model.getConfig().getSeed()));

        Registry<Biome> biomeRegistry = server.getRegistryManager().get(RegistryKeys.BIOME);

        // Try to load custom biomes first
        BiomeSet customBiomes = loadCustomBiomes(biomeRegistry);
        if (customBiomes.isComplete()) {
            this.duneSeaBiome = customBiomes.duneSeaBiome;
            this.graniteMesasBiome = customBiomes.graniteMesasBiome;
            this.limestoneCanyonsBiome = customBiomes.limestoneCanyonsBiome;
            this.saltFlatsBiome = customBiomes.saltFlatsBiome;
            this.usingCustomBiomes = true;

            Terradyne.LOGGER.info("✅ Loaded custom desert biomes from data generation");
        } else {
            // Fall back to vanilla biomes
            BiomeSet fallbackBiomes = loadFallbackBiomes(biomeRegistry);
            this.duneSeaBiome = fallbackBiomes.duneSeaBiome;
            this.graniteMesasBiome = fallbackBiomes.graniteMesasBiome;
            this.limestoneCanyonsBiome = fallbackBiomes.limestoneCanyonsBiome;
            this.saltFlatsBiome = fallbackBiomes.saltFlatsBiome;
            this.usingCustomBiomes = false;

            Terradyne.LOGGER.info("⚠️ Using vanilla biomes with custom terrain (run 'gradlew runDatagen' for custom biomes)");
        }
    }

    @Override
    protected Codec<? extends BiomeSource> getCodec() {
        return CODEC;
    }

    @Override
    protected Stream<RegistryEntry<Biome>> biomeStream() {
        if (duneSeaBiome == null) return Stream.empty();
        return Stream.of(duneSeaBiome, graniteMesasBiome, limestoneCanyonsBiome, saltFlatsBiome);
    }

    @Override
    public RegistryEntry<Biome> getBiome(int x, int y, int z, MultiNoiseUtil.MultiNoiseSampler sampler) {
        if (biomeWeights == null) return duneSeaBiome;

        DesertBiomeType terrainType = getTerrainTypeAt(x << 2, z << 2);

        return switch (terrainType) {
            case DUNE_SEA -> duneSeaBiome;
            case GRANITE_MESAS -> graniteMesasBiome;
            case LIMESTONE_CANYONS -> limestoneCanyonsBiome;
            case SALT_FLATS -> saltFlatsBiome;
        };
    }

    /**
     * Get terrain type at world coordinates (used by chunk generator)
     */
    public DesertBiomeType getTerrainTypeAt(int worldX, int worldZ) {
        if (biomeWeights == null) return DesertBiomeType.DUNE_SEA;

        IBiomeType biomeType = biomeWeights.selectBiomeAt(worldX, worldZ, biomeNoise);
        return (biomeType instanceof DesertBiomeType) ? (DesertBiomeType) biomeType : DesertBiomeType.DUNE_SEA;
    }

    // === PRIVATE HELPER METHODS ===

    private BiomeSet loadCustomBiomes(Registry<Biome> registry) {
        Optional<RegistryEntry.Reference<Biome>> duneSeaOpt = registry.getEntry(ModBiomes.DUNE_SEA);
        Optional<RegistryEntry.Reference<Biome>> graniteMesasOpt = registry.getEntry(ModBiomes.GRANITE_MESAS);
        Optional<RegistryEntry.Reference<Biome>> limestoneCanyonsOpt = registry.getEntry(ModBiomes.LIMESTONE_CANYONS);
        Optional<RegistryEntry.Reference<Biome>> saltFlatsOpt = registry.getEntry(ModBiomes.SALT_FLATS);

        return new BiomeSet(
                duneSeaOpt.orElse(null),
                graniteMesasOpt.orElse(null),
                limestoneCanyonsOpt.orElse(null),
                saltFlatsOpt.orElse(null)
        );
    }

    private BiomeSet loadFallbackBiomes(Registry<Biome> registry) {
        return new BiomeSet(
                registry.getEntry(BiomeKeys.DESERT).orElseThrow(),
                registry.getEntry(BiomeKeys.BADLANDS).orElseThrow(),
                registry.getEntry(BiomeKeys.SAVANNA).orElseThrow(),
                registry.getEntry(BiomeKeys.BEACH).orElseThrow()
        );
    }

    // === GETTERS ===

    public DesertModel getModel() { return model; }
    public boolean isUsingCustomBiomes() { return usingCustomBiomes; }
    public List<DesertBiomeType> getAvailableTerrainTypes() {
        return List.of(DesertBiomeType.DUNE_SEA, DesertBiomeType.GRANITE_MESAS,
                DesertBiomeType.LIMESTONE_CANYONS, DesertBiomeType.SALT_FLATS);
    }

    /**
     * Get the biome type at specific world coordinates
     * ADD THIS METHOD to your DesertBiomeSource.java class
     */
    public IBiomeType getBiomeTypeAt(int worldX, int worldZ) {
        if (biomeWeights == null) {
            return DesertBiomeType.DUNE_SEA;
        }
        return biomeWeights.selectBiomeAt(worldX, worldZ, biomeNoise);
    }

    // === HELPER CLASSES ===

    private static class BiomeSet {
        final RegistryEntry<Biome> duneSeaBiome;
        final RegistryEntry<Biome> graniteMesasBiome;
        final RegistryEntry<Biome> limestoneCanyonsBiome;
        final RegistryEntry<Biome> saltFlatsBiome;

        BiomeSet(RegistryEntry<Biome> duneSeaBiome, RegistryEntry<Biome> graniteMesasBiome,
                 RegistryEntry<Biome> limestoneCanyonsBiome, RegistryEntry<Biome> saltFlatsBiome) {
            this.duneSeaBiome = duneSeaBiome;
            this.graniteMesasBiome = graniteMesasBiome;
            this.limestoneCanyonsBiome = limestoneCanyonsBiome;
            this.saltFlatsBiome = saltFlatsBiome;
        }

        boolean isComplete() {
            return duneSeaBiome != null && graniteMesasBiome != null &&
                    limestoneCanyonsBiome != null && saltFlatsBiome != null;
        }
    }

    /**
     * Calculates biome distribution weights based on planet characteristics
     */
    public static class BiomeWeights {
        private final Map<DesertBiomeType, Float> weights = new HashMap<>();
        private final float totalWeight;
        private final List<WeightedBiome> weightedList;

        private BiomeWeights(Map<DesertBiomeType, Float> weights) {
            this.weights.putAll(weights);
            this.totalWeight = weights.values().stream().reduce(0f, Float::sum);
            this.weightedList = createWeightedList();
        }

        public static BiomeWeights calculate(DesertModel model) {
            DesertConfig config = model.getConfig();
            Map<DesertBiomeType, Float> weights = new HashMap<>();

            float tempPercent = Math.max(0, (config.getSurfaceTemperature() - 35f) / 65f);
            float graniteBonus = config.getDominantRock() == DesertConfig.RockType.GRANITE ? 1f : 0f;
            float limestoneBonus = config.getDominantRock() == DesertConfig.RockType.LIMESTONE ? 1f : 0f;
            float sandstoneBonus = config.getDominantRock() == DesertConfig.RockType.SANDSTONE ? 1f : 0f;

            // Dune Sea (always available)
            float duneSeaWeight = config.getSandDensity() * 2f + config.getWindStrength() +
                    sandstoneBonus * 4f - model.getRockExposure() * 2f;
            weights.put(DesertBiomeType.DUNE_SEA, Math.max(1.0f, duneSeaWeight));

            // Granite Mesas (requires granite rock OR low sand density)
            if (config.getDominantRock() == DesertConfig.RockType.GRANITE || config.getSandDensity() < 0.4f) {
                float mesaWeight = model.getRockExposure() * 3f + graniteBonus * 5f - config.getWindStrength() * 0.5f;
                weights.put(DesertBiomeType.GRANITE_MESAS, Math.max(0.5f, mesaWeight));
            }

            // Limestone Canyons (requires limestone OR high humidity OR low sand)
            if (config.getDominantRock() == DesertConfig.RockType.LIMESTONE ||
                    config.getHumidity() > 0.2f || config.getSandDensity() < 0.3f) {
                float canyonWeight = model.getRockExposure() * 2.5f + limestoneBonus * 4f +
                        config.getHumidity() * 3f - config.getWindStrength() - tempPercent * 2f;
                weights.put(DesertBiomeType.LIMESTONE_CANYONS, Math.max(0.3f, canyonWeight));
            }

            // Salt Flats (requires very low humidity AND high rock exposure)
            if (config.getHumidity() < 0.15f && model.getRockExposure() > 0.3f) {
                float saltWeight = model.getRockExposure() * 2f + (0.15f - config.getHumidity()) * 10f -
                        config.getWindStrength() - config.getSandDensity() * 2f;
                weights.put(DesertBiomeType.SALT_FLATS, Math.max(0.2f, saltWeight));
            }

            return new BiomeWeights(weights);
        }

        public IBiomeType selectBiomeAt(int x, int z, SimplexNoiseSampler biomeNoise) {
            if (totalWeight <= 0 || weightedList.isEmpty()) return DesertBiomeType.DUNE_SEA;

            double noiseValue = (biomeNoise.sample(x * 0.0002, 0, z * 0.0002) + 1.0) * 0.5;
            float targetWeight = (float) (noiseValue * totalWeight);

            for (WeightedBiome weighted : weightedList) {
                if (targetWeight <= weighted.cumulativeWeight) {
                    return weighted.biome;
                }
            }

            return weightedList.get(weightedList.size() - 1).biome;
        }

        private List<WeightedBiome> createWeightedList() {
            List<WeightedBiome> list = new ArrayList<>();
            float cumulative = 0f;
            for (Map.Entry<DesertBiomeType, Float> entry : weights.entrySet()) {
                cumulative += entry.getValue();
                list.add(new WeightedBiome(entry.getKey(), cumulative));
            }
            return list;
        }

        private static class WeightedBiome {
            final DesertBiomeType biome;
            final float cumulativeWeight;

            WeightedBiome(DesertBiomeType biome, float weight) {
                this.biome = biome;
                this.cumulativeWeight = weight;
            }
        }
    }
}