// DesertBiomeCalculator.java - UPDATED for new 4 biomes

package net.terradyne.planet.biome;

import net.minecraft.util.math.noise.SimplexNoiseSampler;
import net.terradyne.planet.config.DesertConfig;
import net.terradyne.planet.model.DesertModel;
import net.terradyne.Terradyne;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DesertBiomeCalculator {

    public static class BiomeWeights {
        private final Map<DesertBiomeType, Float> weights = new HashMap<>();
        private final List<DesertBiomeType> biomeList;

        public BiomeWeights(Map<DesertBiomeType, Float> weights) {
            this.weights.putAll(weights);
            this.biomeList = List.copyOf(weights.keySet());
        }

        public DesertBiomeType selectBiomeAt(int x, int z, SimplexNoiseSampler biomeNoise) {
            if (biomeList.isEmpty()) {
                return DesertBiomeType.DUNE_SEA; // Emergency fallback
            }

            // Use noise to select one of the 4 biomes (0-3)
            double noiseValue = (biomeNoise.sample(x * 0.0008, 0, z * 0.0008) + 1.0) * 0.5; // 0-1
            int biomeIndex = (int) (noiseValue * biomeList.size());
            biomeIndex = Math.max(0, Math.min(biomeIndex, biomeList.size() - 1)); // Clamp

            return biomeList.get(biomeIndex);
        }

        // Debug method
        public Map<DesertBiomeType, Float> getPercentages() {
            Map<DesertBiomeType, Float> percentages = new HashMap<>();
            float equalPercent = 100.0f / weights.size();
            for (DesertBiomeType biome : weights.keySet()) {
                percentages.put(biome, equalPercent);
            }
            return percentages;
        }
    }

    /**
     * FIXED 4 BIOMES for all desert planets - updated set
     */
    public static BiomeWeights calculateBiomeWeights(DesertModel model) {
        DesertConfig config = model.getConfig();

        Terradyne.LOGGER.info("=== CREATING DESERT PLANET ===");
        Terradyne.LOGGER.info("Planet: {}", config.getPlanetName());
        Terradyne.LOGGER.info("Temperature: {}°C", config.getSurfaceTemperature());
        Terradyne.LOGGER.info("Humidity: {}", config.getHumidity());
        Terradyne.LOGGER.info("Rock Type: {}", config.getDominantRock());

        // UPDATED FIXED 4 biomes for every desert planet - equal weights (25% each)
        Map<DesertBiomeType, Float> weights = new HashMap<>();
        weights.put(DesertBiomeType.DUNE_SEA, 1.0f);
        weights.put(DesertBiomeType.GRANITE_MESAS, 1.0f);        // Updated name
        weights.put(DesertBiomeType.LIMESTONE_CANYONS, 1.0f);
        weights.put(DesertBiomeType.SALT_FLATS, 1.0f);          // Replaces SCRUBLAND

        Terradyne.LOGGER.info("=== FIXED BIOMES (25% each) ===");
        Terradyne.LOGGER.info("✅ DUNE_SEA - Rolling sand dunes");
        Terradyne.LOGGER.info("✅ GRANITE_MESAS - Flat-topped plateaus");
        Terradyne.LOGGER.info("✅ LIMESTONE_CANYONS - Deep carved valleys");
        Terradyne.LOGGER.info("✅ SALT_FLATS - Crystalline plains");

        return new BiomeWeights(weights);
    }
}