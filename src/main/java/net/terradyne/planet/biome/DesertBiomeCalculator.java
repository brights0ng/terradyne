package net.terradyne.planet.biome;


import net.minecraft.util.math.noise.SimplexNoiseSampler;
import net.terradyne.planet.config.DesertConfig;
import net.terradyne.planet.model.DesertModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public static BiomeWeights calculateBiomeWeights(DesertModel model) {
        DesertConfig config = model.getConfig();
        Map<DesertBiomeType, Float> weights = new HashMap<>();

        // Calculate temperature percentage (0 = 35°C, 1 = 100°C)
        float tempPercent = Math.max(0, (config.getSurfaceTemperature() - 35f) / 65f);

        // Rock type multipliers
        float graniteBonus = config.getDominantRock() == DesertConfig.RockType.GRANITE ? 1f : 0f;
        float volcanicBonus = config.getDominantRock() == DesertConfig.RockType.VOLCANIC ? 1f : 0f;
        float limestoneBonus = config.getDominantRock() == DesertConfig.RockType.LIMESTONE ? 1f : 0f;
        float sandstoneBonus = config.getDominantRock() == DesertConfig.RockType.SANDSTONE ? 1f : 0f;

        // Check requirements and calculate weights for each biome

        // DUNE SEA: (temperature: 35-50 OR humidity > 0.1)
        if ((config.getSurfaceTemperature() >= 35 && config.getSurfaceTemperature() <= 50) ||
                config.getHumidity() > 0.1f) {
            float weight = config.getSandDensity() * 2f +
                    config.getWindStrength() +
                    sandstoneBonus * 4f -
                    model.getRockExposure() * 2f;
            weights.put(DesertBiomeType.DUNE_SEA, Math.max(0, weight));
        }

        // SCORCHING WASTE: (temperature > 50, humidity < 0.1)
        if (config.getSurfaceTemperature() > 50 && config.getHumidity() < 0.1f) {
            float weight = config.getSandDensity() +
                    tempPercent * 4f +
                    config.getWindStrength() +
                    volcanicBonus * 4f -
                    config.getHumidity() * 4f -
                    model.getRockExposure() * 2f;
            weights.put(DesertBiomeType.SCORCHING_WASTE, Math.max(0, weight));
        }

        // GRANITE MESAS: (rockType: GRANITE)
        if (config.getDominantRock() == DesertConfig.RockType.GRANITE) {
            float weight = model.getRockExposure() * 2f +
                    graniteBonus * 4f -
                    config.getWindStrength();
            weights.put(DesertBiomeType.GRANITE_MESAS, Math.max(0, weight));
        }

        // VOLCANIC WASTELAND: (rockType: VOLCANIC OR temperature > 50)
        if (config.getDominantRock() == DesertConfig.RockType.VOLCANIC ||
                config.getSurfaceTemperature() > 50) {
            float weight = model.getRockExposure() * 2f +
                    volcanicBonus * 4f +
                    tempPercent * 2f -
                    config.getWindStrength() -
                    config.getHumidity() * 4f;
            weights.put(DesertBiomeType.VOLCANIC_WASTELAND, Math.max(0, weight));
        }

        // LIMESTONE CANYONS: (rockType: LIMESTONE OR humidity > 0.2)
        if (config.getDominantRock() == DesertConfig.RockType.LIMESTONE ||
                config.getHumidity() > 0.2f) {
            float weight = model.getRockExposure() * 2f +
                    limestoneBonus * 4f +
                    config.getHumidity() -
                    config.getWindStrength() -
                    tempPercent;
            weights.put(DesertBiomeType.LIMESTONE_CANYONS, Math.max(0, weight));
        }

        // SALT FLATS: (rockExposure > 0.2, sandDensity < 0.2)
        if (model.getRockExposure() > 0.2f && config.getSandDensity() < 0.2f) {
            float weight = model.getRockExposure() * 2f +
                    limestoneBonus -
                    config.getHumidity() -
                    config.getWindStrength() -
                    config.getSandDensity();
            weights.put(DesertBiomeType.SALT_FLATS, Math.max(0, weight));
        }

        // SCRUBLAND: (humidity > 0.2, temperature < 40)
        if (config.getHumidity() > 0.2f && config.getSurfaceTemperature() < 40f) {
            float weight = config.getHumidity() * 4f +
                    limestoneBonus * 2f -
                    tempPercent * 4f -
                    config.getWindStrength() -
                    config.getSandDensity() -
                    model.getRockExposure();
            weights.put(DesertBiomeType.SCRUBLAND, Math.max(0, weight));
        }

        // DUST BOWL: (humidity < 0.1, windStrength > 1)
        if (config.getHumidity() < 0.1f && config.getWindStrength() > 1f) {
            float weight = config.getWindStrength() * 4f +
                    tempPercent -
                    config.getHumidity() * 4f;
            weights.put(DesertBiomeType.DUST_BOWL, Math.max(0, weight));
        }

        // Ensure at least one biome is available (fallback to Dune Sea)
        if (weights.isEmpty()) {
            weights.put(DesertBiomeType.DUNE_SEA, 1.0f);
        }

        return new BiomeWeights(weights);
    }
}