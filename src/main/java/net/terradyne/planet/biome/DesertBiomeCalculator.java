package net.terradyne.planet.biome;

import net.minecraft.util.math.noise.SimplexNoiseSampler;
import net.terradyne.planet.config.DesertConfig;
import net.terradyne.planet.model.DesertModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Updated DesertBiomeCalculator - Only works with the 4 registered biomes
 */
public class DesertBiomeCalculator {

    public static class BiomeWeights {
        private final Map<DesertBiomeType, Float> weights = new HashMap<>();
        private final float totalWeight;
        private final List<WeightedBiome> weightedList; // For faster selection

        private static class WeightedBiome {
            final DesertBiomeType biome;
            final float cumulativeWeight;

            WeightedBiome(DesertBiomeType biome, float weight) {
                this.biome = biome;
                this.cumulativeWeight = weight;
            }
        }

        public BiomeWeights(Map<DesertBiomeType, Float> weights) {
            this.weights.putAll(weights);
            this.totalWeight = weights.values().stream().reduce(0f, Float::sum);
            this.weightedList = createWeightedList();
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

        public float getWeight(DesertBiomeType biome) {
            return weights.getOrDefault(biome, 0f);
        }

        public float getTotalWeight() {
            return totalWeight;
        }

        public DesertBiomeType selectBiomeAt(int x, int z, SimplexNoiseSampler biomeNoise) {
            if (totalWeight <= 0 || weightedList.isEmpty()) {
                return DesertBiomeType.DUNE_SEA; // Fallback
            }

            // Use noise to select biome based on weights
            double noiseValue = (biomeNoise.sample(x * 0.0002, 0, z * 0.0002) + 1.0) * 0.5; // 0-1
            float targetWeight = (float) (noiseValue * totalWeight);

            // Binary search through weighted list for performance
            for (WeightedBiome weighted : weightedList) {
                if (targetWeight <= weighted.cumulativeWeight) {
                    return weighted.biome;
                }
            }

            // Should never reach here, but fallback
            return weightedList.get(weightedList.size() - 1).biome;
        }

        // Debug method
        public Map<DesertBiomeType, Float> getPercentages() {
            Map<DesertBiomeType, Float> percentages = new HashMap<>();
            for (Map.Entry<DesertBiomeType, Float> entry : weights.entrySet()) {
                percentages.put(entry.getKey(), (entry.getValue() / totalWeight) * 100f);
            }
            return percentages;
        }
    }

    /**
     * Calculate biome weights for the 4 registered desert biomes only
     */
    public static BiomeWeights calculateBiomeWeights(DesertModel model) {
        DesertConfig config = model.getConfig();
        Map<DesertBiomeType, Float> weights = new HashMap<>();

        // Calculate temperature percentage (0 = 35°C, 1 = 100°C)
        float tempPercent = Math.max(0, (config.getSurfaceTemperature() - 35f) / 65f);

        // Rock type multipliers
        float graniteBonus = config.getDominantRock() == DesertConfig.RockType.GRANITE ? 1f : 0f;
        float limestoneBonus = config.getDominantRock() == DesertConfig.RockType.LIMESTONE ? 1f : 0f;
        float sandstoneBonus = config.getDominantRock() == DesertConfig.RockType.SANDSTONE ? 1f : 0f;

        // === DUNE SEA (always available) ===
        float duneSeaWeight = config.getSandDensity() * 2f +
                config.getWindStrength() +
                sandstoneBonus * 4f -
                model.getRockExposure() * 2f;
        weights.put(DesertBiomeType.DUNE_SEA, Math.max(1.0f, duneSeaWeight)); // Minimum 1.0

        // === GRANITE MESAS (requires granite rock OR low sand density) ===
        if (config.getDominantRock() == DesertConfig.RockType.GRANITE || config.getSandDensity() < 0.4f) {
            float mesaWeight = model.getRockExposure() * 3f +
                    graniteBonus * 5f -
                    config.getWindStrength() * 0.5f;
            weights.put(DesertBiomeType.GRANITE_MESAS, Math.max(0.5f, mesaWeight));
        }

        // === LIMESTONE CANYONS (requires limestone OR high humidity OR low sand) ===
        if (config.getDominantRock() == DesertConfig.RockType.LIMESTONE ||
                config.getHumidity() > 0.2f ||
                config.getSandDensity() < 0.3f) {

            float canyonWeight = model.getRockExposure() * 2.5f +
                    limestoneBonus * 4f +
                    config.getHumidity() * 3f -
                    config.getWindStrength() -
                    tempPercent * 2f;
            weights.put(DesertBiomeType.LIMESTONE_CANYONS, Math.max(0.3f, canyonWeight));
        }

        // === SALT FLATS (requires very low humidity AND high rock exposure) ===
        if (config.getHumidity() < 0.15f && model.getRockExposure() > 0.3f) {
            float saltWeight = model.getRockExposure() * 2f +
                    (0.15f - config.getHumidity()) * 10f -  // Lower humidity = more salt
                    config.getWindStrength() -
                    config.getSandDensity() * 2f;
            weights.put(DesertBiomeType.SALT_FLATS, Math.max(0.2f, saltWeight));
        }

        // Ensure at least Dune Sea is available
        if (weights.isEmpty()) {
            weights.put(DesertBiomeType.DUNE_SEA, 1.0f);
        }

        // Log biome distribution for debugging
        BiomeWeights result = new BiomeWeights(weights);
        logBiomeDistribution(config, result);

        return result;
    }

    /**
     * Log the calculated biome distribution for debugging
     */
    private static void logBiomeDistribution(DesertConfig config, BiomeWeights weights) {
        Map<DesertBiomeType, Float> percentages = weights.getPercentages();

        StringBuilder sb = new StringBuilder();
        sb.append("Desert biome distribution for ").append(config.getPlanetName()).append(":\n");
        sb.append("  Planet conditions: temp=").append(config.getSurfaceTemperature())
                .append("°C, humidity=").append(String.format("%.1f", config.getHumidity() * 100))
                .append("%, sand=").append(String.format("%.1f", config.getSandDensity() * 100))
                .append("%, rock=").append(config.getDominantRock()).append("\n");

        for (Map.Entry<DesertBiomeType, Float> entry : percentages.entrySet()) {
            sb.append("  ").append(entry.getKey().getName())
                    .append(": ").append(String.format("%.1f", entry.getValue())).append("%\n");
        }

        System.out.println(sb.toString()); // Using System.out to ensure it's visible
    }
}