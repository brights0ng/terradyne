package net.starlight.terradyne.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.sound.BiomeMoodSound;
import net.minecraft.world.biome.*;
import net.starlight.terradyne.planet.biome.ModBiomes;
import net.starlight.terradyne.planet.biology.BiomeFeatureGenerator;
import net.starlight.terradyne.planet.physics.AtmosphereComposition;

import java.util.concurrent.CompletableFuture;

/**
 * Generates all Terradyne biome JSON files using physics-based component system
 * UPDATED: Now uses atmospheric composition for vegetation instead of crust composition
 */
public class BiomeDataProvider extends FabricDynamicRegistryProvider {

    private RegistryWrapper.WrapperLookup registries; // Store registries for use in generation

    public BiomeDataProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup registries, Entries entries) {
        // Store registries for use in biome generation
        this.registries = registries;

        // === WATER BIOMES ===
        // Water biomes have minimal terrestrial features, mostly ground cover

        entries.add(ModBiomes.FROZEN_OCEAN, createOceanBiome(ModBiomes.FROZEN_OCEAN, -15.0f, 0.3f, 0x3F5F8F, 0x062F4F, true, AtmosphereComposition.OXYGEN_RICH));
        entries.add(ModBiomes.FRIGID_OCEAN, createOceanBiome(ModBiomes.FRIGID_OCEAN, -5.0f, 0.5f, 0x4F6F9F, 0x0C3F5F, false, AtmosphereComposition.OXYGEN_RICH));
        entries.add(ModBiomes.DEAD_OCEAN, createOceanBiome(ModBiomes.DEAD_OCEAN, 10.0f, 0.1f, 0x2F2F2F, 0x1F1F1F, false, AtmosphereComposition.TRACE_ATMOSPHERE));
        entries.add(ModBiomes.OCEAN, createOceanBiome(ModBiomes.OCEAN, 15.0f, 0.5f, 0x3F76E4, 0x0E4B9A, false, AtmosphereComposition.OXYGEN_RICH));
        entries.add(ModBiomes.WARM_OCEAN, createOceanBiome(ModBiomes.WARM_OCEAN, 20.0f, 0.7f, 0x45ADF2, 0x1787D4, false, AtmosphereComposition.OXYGEN_RICH));
        entries.add(ModBiomes.CORAL_OCEAN, createOceanBiome(ModBiomes.CORAL_OCEAN, 25.0f, 0.9f, 0x43D5EE, 0x02B0E6, false, AtmosphereComposition.NITROGEN_RICH));
        entries.add(ModBiomes.TROPICAL_OCEAN, createOceanBiome(ModBiomes.TROPICAL_OCEAN, 35.0f, 0.8f, 0x00FFFF, 0x00CCCC, false, AtmosphereComposition.WATER_VAPOR_RICH));
        entries.add(ModBiomes.BOILING_OCEAN, createOceanBiome(ModBiomes.BOILING_OCEAN, 70.0f, 0.2f, 0xFF4444, 0xCC1111, false, AtmosphereComposition.HYDROGEN_SULFIDE));

        // === MOUNTAIN BIOMES ===
        entries.add(ModBiomes.FROZEN_PEAKS, createMountainBiome(ModBiomes.FROZEN_PEAKS, -20.0f, 0.6f, AtmosphereComposition.TRACE_ATMOSPHERE));
        entries.add(ModBiomes.MOUNTAIN_FOOTHILLS, createMountainBiome(ModBiomes.MOUNTAIN_FOOTHILLS, 5.0f, 0.4f, AtmosphereComposition.OXYGEN_RICH));
        entries.add(ModBiomes.MOUNTAIN_PEAKS, createMountainBiome(ModBiomes.MOUNTAIN_PEAKS, 10.0f, 0.3f, AtmosphereComposition.TRACE_ATMOSPHERE));
        entries.add(ModBiomes.ALPINE_PEAKS, createMountainBiome(ModBiomes.ALPINE_PEAKS, 0.0f, 0.5f, AtmosphereComposition.OXYGEN_RICH));
        entries.add(ModBiomes.VOLCANIC_WASTELAND, createMountainBiome(ModBiomes.VOLCANIC_WASTELAND, 40.0f, 0.1f, AtmosphereComposition.HYDROGEN_SULFIDE));
        entries.add(ModBiomes.VOLCANIC_MOUNTAINS, createMountainBiome(ModBiomes.VOLCANIC_MOUNTAINS, 35.0f, 0.4f, AtmosphereComposition.HYDROGEN_SULFIDE));

        // === HIGHLAND BIOMES ===
        entries.add(ModBiomes.BARREN_HIGHLANDS, createHighlandBiome(ModBiomes.BARREN_HIGHLANDS, 15.0f, 0.2f, AtmosphereComposition.TRACE_ATMOSPHERE));
        entries.add(ModBiomes.WINDSWEPT_HILLS, createHighlandBiome(ModBiomes.WINDSWEPT_HILLS, 10.0f, 0.3f, AtmosphereComposition.OXYGEN_RICH));
        entries.add(ModBiomes.ROLLING_HILLS, createHighlandBiome(ModBiomes.ROLLING_HILLS, 18.0f, 0.6f, AtmosphereComposition.OXYGEN_RICH));
        entries.add(ModBiomes.HIGHLAND_TUNDRA, createHighlandBiome(ModBiomes.HIGHLAND_TUNDRA, -5.0f, 0.4f, AtmosphereComposition.NITROGEN_RICH));
        entries.add(ModBiomes.FORESTED_HILLS, createHighlandBiome(ModBiomes.FORESTED_HILLS, 15.0f, 0.8f, AtmosphereComposition.OXYGEN_RICH));
        entries.add(ModBiomes.TROPICAL_HIGHLANDS, createHighlandBiome(ModBiomes.TROPICAL_HIGHLANDS, 28.0f, 0.7f, AtmosphereComposition.WATER_VAPOR_RICH));

        // === HOSTILE CONTINENTAL BIOMES ===
        entries.add(ModBiomes.FROZEN_WASTELAND, createHostileBiome(ModBiomes.FROZEN_WASTELAND, -30.0f, 0.1f, AtmosphereComposition.TRACE_ATMOSPHERE));
        entries.add(ModBiomes.ROCKY_DESERT, createHostileBiome(ModBiomes.ROCKY_DESERT, 25.0f, 0.0f, AtmosphereComposition.TRACE_ATMOSPHERE));
        entries.add(ModBiomes.SCORCHED_PLAINS, createHostileBiome(ModBiomes.SCORCHED_PLAINS, 50.0f, 0.0f, AtmosphereComposition.CARBON_DIOXIDE));
        entries.add(ModBiomes.WINDSWEPT_TUNDRA, createHostileBiome(ModBiomes.WINDSWEPT_TUNDRA, -10.0f, 0.2f, AtmosphereComposition.OXYGEN_RICH));
        entries.add(ModBiomes.SANDY_DESERT, createHostileBiome(ModBiomes.SANDY_DESERT, 30.0f, 0.1f, AtmosphereComposition.OXYGEN_RICH));
        entries.add(ModBiomes.DESERT_MESA, createHostileBiome(ModBiomes.DESERT_MESA, 35.0f, 0.1f, AtmosphereComposition.NOBLE_GAS_MIXTURE));
        entries.add(ModBiomes.DUST_BOWL, createHostileBiome(ModBiomes.DUST_BOWL, 45.0f, 0.0f, AtmosphereComposition.TRACE_ATMOSPHERE));

        // === MARGINAL CONTINENTAL BIOMES ===
        entries.add(ModBiomes.COLD_STEPPES, createMarginalBiome(ModBiomes.COLD_STEPPES, -15.0f, 0.3f, AtmosphereComposition.NITROGEN_RICH));
        entries.add(ModBiomes.TUNDRA, createMarginalBiome(ModBiomes.TUNDRA, -5.0f, 0.2f, AtmosphereComposition.OXYGEN_RICH));
        entries.add(ModBiomes.BOREAL_PLAINS, createMarginalBiome(ModBiomes.BOREAL_PLAINS, 0.0f, 0.5f, AtmosphereComposition.OXYGEN_RICH));
        entries.add(ModBiomes.DRY_STEPPES, createMarginalBiome(ModBiomes.DRY_STEPPES, 12.0f, 0.2f, AtmosphereComposition.OXYGEN_RICH));
        entries.add(ModBiomes.TEMPERATE_STEPPES, createMarginalBiome(ModBiomes.TEMPERATE_STEPPES, 15.0f, 0.4f, AtmosphereComposition.OXYGEN_RICH));
        entries.add(ModBiomes.MEADOWS, createMarginalBiome(ModBiomes.MEADOWS, 18.0f, 0.7f, AtmosphereComposition.OXYGEN_RICH));
        entries.add(ModBiomes.SAVANNA, createMarginalBiome(ModBiomes.SAVANNA, 32.0f, 0.3f, AtmosphereComposition.OXYGEN_RICH));
        entries.add(ModBiomes.TROPICAL_GRASSLAND, createMarginalBiome(ModBiomes.TROPICAL_GRASSLAND, 35.0f, 0.5f, AtmosphereComposition.NITROGEN_RICH));

        // === THRIVING CONTINENTAL BIOMES ===
        // Cold Zone
        entries.add(ModBiomes.SNOWY_PLAINS, createThrivingBiome(ModBiomes.SNOWY_PLAINS, -2.0f, 0.5f, AtmosphereComposition.OXYGEN_RICH));
        entries.add(ModBiomes.TAIGA, createThrivingBiome(ModBiomes.TAIGA, -5.0f, 0.4f, AtmosphereComposition.OXYGEN_RICH));
        entries.add(ModBiomes.SNOW_FOREST, createThrivingBiome(ModBiomes.SNOW_FOREST, -8.0f, 0.7f, AtmosphereComposition.NITROGEN_RICH));
        entries.add(ModBiomes.ALPINE_MEADOWS, createThrivingBiome(ModBiomes.ALPINE_MEADOWS, -3.0f, 0.6f, AtmosphereComposition.OXYGEN_RICH));

        // Temperate Zone
        entries.add(ModBiomes.PLAINS, createThrivingBiome(ModBiomes.PLAINS, 15.0f, 0.4f, AtmosphereComposition.OXYGEN_RICH));
        entries.add(ModBiomes.MIXED_PLAINS, createThrivingBiome(ModBiomes.MIXED_PLAINS, 18.0f, 0.6f, AtmosphereComposition.OXYGEN_RICH));
        entries.add(ModBiomes.WETLANDS, createThrivingBiome(ModBiomes.WETLANDS, 20.0f, 0.9f, AtmosphereComposition.WATER_VAPOR_RICH));
        entries.add(ModBiomes.OAK_FOREST, createThrivingBiome(ModBiomes.OAK_FOREST, 16.0f, 0.3f, AtmosphereComposition.OXYGEN_RICH));
        entries.add(ModBiomes.MIXED_FOREST, createThrivingBiome(ModBiomes.MIXED_FOREST, 18.0f, 0.5f, AtmosphereComposition.OXYGEN_RICH));
        entries.add(ModBiomes.DENSE_FOREST, createThrivingBiome(ModBiomes.DENSE_FOREST, 20.0f, 0.8f, AtmosphereComposition.NITROGEN_RICH));
        entries.add(ModBiomes.MOUNTAIN_FOREST, createThrivingBiome(ModBiomes.MOUNTAIN_FOREST, 12.0f, 0.6f, AtmosphereComposition.OXYGEN_RICH));

        // Warm Zone - More exotic atmospheres for variety
        entries.add(ModBiomes.HOT_SHRUBLAND, createThrivingBiome(ModBiomes.HOT_SHRUBLAND, 30.0f, 0.3f, AtmosphereComposition.CARBON_DIOXIDE));
        entries.add(ModBiomes.WINDY_STEPPES, createThrivingBiome(ModBiomes.WINDY_STEPPES, 32.0f, 0.4f, AtmosphereComposition.OXYGEN_RICH));
        entries.add(ModBiomes.TEMPERATE_RAINFOREST, createThrivingBiome(ModBiomes.TEMPERATE_RAINFOREST, 28.0f, 0.6f, AtmosphereComposition.WATER_VAPOR_RICH));
        entries.add(ModBiomes.CLOUD_FOREST, createThrivingBiome(ModBiomes.CLOUD_FOREST, 25.0f, 0.7f, AtmosphereComposition.WATER_VAPOR_RICH));
        entries.add(ModBiomes.JUNGLE, createThrivingBiome(ModBiomes.JUNGLE, 30.0f, 0.9f, AtmosphereComposition.WATER_VAPOR_RICH));
        entries.add(ModBiomes.TROPICAL_RAINFOREST, createThrivingBiome(ModBiomes.TROPICAL_RAINFOREST, 32.0f, 0.8f, AtmosphereComposition.WATER_VAPOR_RICH));

        // Hot Zone - Exotic atmospheres
        entries.add(ModBiomes.HOT_DESERT, createThrivingBiome(ModBiomes.HOT_DESERT, 45.0f, 0.1f, AtmosphereComposition.NOBLE_GAS_MIXTURE));
        entries.add(ModBiomes.TROPICAL_SWAMP, createThrivingBiome(ModBiomes.TROPICAL_SWAMP, 42.0f, 0.7f, AtmosphereComposition.METHANE));

        // === EXTREME BIOMES ===
        entries.add(ModBiomes.EXTREME_FROZEN_WASTELAND, createExtremeBiome(ModBiomes.EXTREME_FROZEN_WASTELAND, -60.0f, 0.0f, AtmosphereComposition.VACUUM));
        entries.add(ModBiomes.MOLTEN_WASTELAND, createExtremeBiome(ModBiomes.MOLTEN_WASTELAND, 120.0f, 0.0f, AtmosphereComposition.HYDROGEN_SULFIDE));

        // === DEBUG BIOME ===
        entries.add(ModBiomes.DEBUG, createDebugBiome(ModBiomes.DEBUG));
    }

    @Override
    public String getName() {
        return "Terradyne Biomes (Atmospheric-Based)";
    }

    // === BIOME CREATION HELPERS ===

    /**
     * Create ocean biome with atmospheric-based generation
     */
    private Biome createOceanBiome(RegistryKey<Biome> biomeKey, float temperature, float downfall, int waterColor, int waterFogColor, boolean frozen, AtmosphereComposition atmosphereComposition) {
        // Calculate atmospheric colors
        var colors = BiomeFeatureGenerator.calculateBiomeColors(biomeKey, atmosphereComposition, temperature, downfall);

        return new Biome.Builder()
                .temperature(temperature)
                .downfall(downfall)
                .precipitation(frozen || temperature < 0.15f)
                .effects(new BiomeEffects.Builder()
                        .skyColor(getAtmosphericSkyColor(atmosphereComposition, temperature))
                        .fogColor(getAtmosphericFogColor(atmosphereComposition, temperature))
                        .waterColor(waterColor)
                        .waterFogColor(waterFogColor)
                        .grassColor(colors.grassColor)
                        .foliageColor(colors.foliageColor)
                        .moodSound(BiomeMoodSound.CAVE)
                        .build())
                .spawnSettings(createOceanSpawns())
                .generationSettings(createAtmosphericGeneration(biomeKey, atmosphereComposition, temperature, downfall, 0.3, 1.0))
                .build();
    }

    /**
     * Create mountain biome with atmospheric-based generation
     */
    private Biome createMountainBiome(RegistryKey<Biome> biomeKey, float temperature, float downfall, AtmosphereComposition atmosphereComposition) {
        var colors = BiomeFeatureGenerator.calculateBiomeColors(biomeKey, atmosphereComposition, temperature, downfall);

        return new Biome.Builder()
                .temperature(temperature)
                .downfall(downfall)
                .precipitation(temperature < 0.15f)
                .effects(new BiomeEffects.Builder()
                        .skyColor(getAtmosphericSkyColor(atmosphereComposition, temperature))
                        .fogColor(getAtmosphericFogColor(atmosphereComposition, temperature))
                        .waterColor(getWaterColor(temperature))
                        .waterFogColor(getWaterFogColor(temperature))
                        .grassColor(colors.grassColor)
                        .foliageColor(colors.foliageColor)
                        .moodSound(BiomeMoodSound.CAVE)
                        .build())
                .spawnSettings(createMountainSpawns())
                .generationSettings(createAtmosphericGeneration(biomeKey, atmosphereComposition, temperature, downfall, 0.4, 0.8))
                .build();
    }

    /**
     * Create highland biome with atmospheric-based generation
     */
    private Biome createHighlandBiome(RegistryKey<Biome> biomeKey, float temperature, float downfall, AtmosphereComposition atmosphereComposition) {
        var colors = BiomeFeatureGenerator.calculateBiomeColors(biomeKey, atmosphereComposition, temperature, downfall);

        return new Biome.Builder()
                .temperature(temperature)
                .downfall(downfall)
                .precipitation(temperature < 0.15f)
                .effects(new BiomeEffects.Builder()
                        .skyColor(getAtmosphericSkyColor(atmosphereComposition, temperature))
                        .fogColor(getAtmosphericFogColor(atmosphereComposition, temperature))
                        .waterColor(getWaterColor(temperature))
                        .waterFogColor(getWaterFogColor(temperature))
                        .grassColor(colors.grassColor)
                        .foliageColor(colors.foliageColor)
                        .moodSound(BiomeMoodSound.CAVE)
                        .build())
                .spawnSettings(createHighlandSpawns())
                .generationSettings(createAtmosphericGeneration(biomeKey, atmosphereComposition, temperature, downfall, 0.5, 1.0))
                .build();
    }

    /**
     * Create hostile biome with atmospheric-based generation
     */
    private Biome createHostileBiome(RegistryKey<Biome> biomeKey, float temperature, float downfall, AtmosphereComposition atmosphereComposition) {
        var colors = BiomeFeatureGenerator.calculateBiomeColors(biomeKey, atmosphereComposition, temperature, downfall);

        return new Biome.Builder()
                .temperature(temperature)
                .downfall(downfall)
                .precipitation(temperature < 0.15f)
                .effects(new BiomeEffects.Builder()
                        .skyColor(getAtmosphericSkyColor(atmosphereComposition, temperature))
                        .fogColor(getAtmosphericFogColor(atmosphereComposition, temperature))
                        .waterColor(getWaterColor(temperature))
                        .waterFogColor(getWaterFogColor(temperature))
                        .grassColor(colors.grassColor)
                        .foliageColor(colors.foliageColor)
                        .moodSound(BiomeMoodSound.CAVE)
                        .build())
                .spawnSettings(createHostileSpawns())
                .generationSettings(createAtmosphericGeneration(biomeKey, atmosphereComposition, temperature, downfall, 0.2, 0.5))
                .build();
    }

    /**
     * Create marginal biome with atmospheric-based generation
     */
    private Biome createMarginalBiome(RegistryKey<Biome> biomeKey, float temperature, float downfall, AtmosphereComposition atmosphereComposition) {
        var colors = BiomeFeatureGenerator.calculateBiomeColors(biomeKey, atmosphereComposition, temperature, downfall);

        return new Biome.Builder()
                .temperature(temperature)
                .downfall(downfall)
                .precipitation(temperature < 0.15f)
                .effects(new BiomeEffects.Builder()
                        .skyColor(getAtmosphericSkyColor(atmosphereComposition, temperature))
                        .fogColor(getAtmosphericFogColor(atmosphereComposition, temperature))
                        .waterColor(getWaterColor(temperature))
                        .waterFogColor(getWaterFogColor(temperature))
                        .grassColor(colors.grassColor)
                        .foliageColor(colors.foliageColor)
                        .moodSound(BiomeMoodSound.CAVE)
                        .build())
                .spawnSettings(createMarginalSpawns())
                .generationSettings(createAtmosphericGeneration(biomeKey, atmosphereComposition, temperature, downfall, 0.6, 1.0))
                .build();
    }

    /**
     * Create thriving biome with atmospheric-based generation
     */
    private Biome createThrivingBiome(RegistryKey<Biome> biomeKey, float temperature, float downfall, AtmosphereComposition atmosphereComposition) {
        var colors = BiomeFeatureGenerator.calculateBiomeColors(biomeKey, atmosphereComposition, temperature, downfall);

        return new Biome.Builder()
                .temperature(temperature)
                .downfall(downfall)
                .precipitation(temperature < 0.15f)
                .effects(new BiomeEffects.Builder()
                        .skyColor(getAtmosphericSkyColor(atmosphereComposition, temperature))
                        .fogColor(getAtmosphericFogColor(atmosphereComposition, temperature))
                        .waterColor(getWaterColor(temperature))
                        .waterFogColor(getWaterFogColor(temperature))
                        .grassColor(colors.grassColor)
                        .foliageColor(colors.foliageColor)
                        .moodSound(BiomeMoodSound.CAVE)
                        .build())
                .spawnSettings(createThrivingSpawns())
                .generationSettings(createAtmosphericGeneration(biomeKey, atmosphereComposition, temperature, downfall, 0.8, 1.2))
                .build();
    }

    /**
     * Create extreme biome with atmospheric-based generation
     */
    private Biome createExtremeBiome(RegistryKey<Biome> biomeKey, float temperature, float downfall, AtmosphereComposition atmosphereComposition) {
        var colors = BiomeFeatureGenerator.calculateBiomeColors(biomeKey, atmosphereComposition, temperature, downfall);

        return new Biome.Builder()
                .temperature(temperature)
                .downfall(downfall)
                .precipitation(false) // Extreme conditions prevent normal precipitation
                .effects(new BiomeEffects.Builder()
                        .skyColor(getAtmosphericSkyColor(atmosphereComposition, temperature))
                        .fogColor(getAtmosphericFogColor(atmosphereComposition, temperature))
                        .waterColor(getWaterColor(temperature))
                        .waterFogColor(getWaterFogColor(temperature))
                        .grassColor(colors.grassColor)
                        .foliageColor(colors.foliageColor)
                        .moodSound(BiomeMoodSound.CAVE)
                        .build())
                .spawnSettings(createNoSpawns())
                .generationSettings(createAtmosphericGeneration(biomeKey, atmosphereComposition, temperature, downfall, 0.0, 0.1))
                .build();
    }

    /**
     * Create debug biome
     */
    private Biome createDebugBiome(RegistryKey<Biome> biomeKey) {
        return new Biome.Builder()
                .temperature(0.5f)
                .downfall(0.5f)
                .precipitation(false)
                .effects(new BiomeEffects.Builder()
                        .skyColor(getAtmosphericSkyColor(AtmosphereComposition.OXYGEN_RICH, 15.0f))
                        .fogColor(getAtmosphericFogColor(AtmosphereComposition.OXYGEN_RICH, 15.0f))
                        .waterColor(0x5F7F9F)
                        .waterFogColor(0x4F6F8F)
                        .grassColor(0x888888)  // Gray
                        .foliageColor(0x888888) // Gray
                        .moodSound(BiomeMoodSound.CAVE)
                        .build())
                .spawnSettings(createNoSpawns())
                .generationSettings(createAtmosphericGeneration(biomeKey, AtmosphereComposition.OXYGEN_RICH, 15.0f, 0.5f, 0.7, 1.0))
                .build();
    }

    /**
     * Create atmospheric-based generation settings
     * UPDATED: Now uses atmospheric composition instead of crust composition
     */
    private GenerationSettings createAtmosphericGeneration(RegistryKey<Biome> biomeKey,
                                                           AtmosphereComposition atmosphereComposition,
                                                           double temperature,
                                                           double humidity,
                                                           double habitability,
                                                           double atmosphericDensity) {
        try {
            // Pass atmospheric composition to BiomeFeatureGenerator
            return BiomeFeatureGenerator.createGenerationSettings(biomeKey, atmosphereComposition, temperature,
                    humidity, habitability, atmosphericDensity, this.registries);
        } catch (Exception e) {
            // Fallback on any error
            return BiomeFeatureGenerator.createMinimalGeneration();
        }
    }

    // === ATMOSPHERIC COLOR CALCULATION HELPERS ===

    /**
     * Get sky color based on atmospheric composition with temperature variation
     * UPDATED: Now uses atmospheric composition as primary factor
     */
    private int getAtmosphericSkyColor(AtmosphereComposition atmosphereComposition, float temperature) {
        // Base color from atmospheric composition
        int baseColor = switch (atmosphereComposition) {
            case OXYGEN_RICH -> 0x78A7FF;           // Earth-like blue
            case CARBON_DIOXIDE -> 0xFFA500;        // Orange (greenhouse/Mars-like)
            case NITROGEN_RICH -> 0x9370DB;         // Purple-blue (nitrogen dominance)
            case METHANE -> 0xD2691E;               // Orange-brown (Titan-like)
            case WATER_VAPOR_RICH -> 0xF5F5DC;     // Pale/white (very cloudy steam)
            case HYDROGEN_SULFIDE -> 0xB8860B;     // Yellow-brown (sulfurous)
            case NOBLE_GAS_MIXTURE -> 0xE6E6FA;    // Very pale lavender (inert/clear)
            case TRACE_ATMOSPHERE -> 0x2F2F2F;     // Dark gray (thin atmosphere)
            case VACUUM -> 0x000000;               // Black (space)
        };

        // Apply temperature variation (dust, particles, thermal effects)
        if (temperature > 40) {
            // Hot temperatures can add orange/red tint (dust, heat shimmer)
            return tintColor(baseColor, 0xFFB347, 0.2f);
        } else if (temperature < -20) {
            // Very cold can add blue tint (ice crystals)
            return tintColor(baseColor, 0x87CEEB, 0.15f);
        }

        return baseColor;
    }

    /**
     * Get fog color based on atmospheric composition with temperature variation
     * UPDATED: Now uses atmospheric composition as primary factor
     */
    private int getAtmosphericFogColor(AtmosphereComposition atmosphereComposition, float temperature) {
        // Base color from atmospheric composition (usually lighter than sky)
        int baseColor = switch (atmosphereComposition) {
            case OXYGEN_RICH -> 0xC0D8FF;           // Light blue
            case CARBON_DIOXIDE -> 0xFFE4B5;        // Light orange
            case NITROGEN_RICH -> 0xDDA0DD;         // Light purple
            case METHANE -> 0xF4A460;               // Sandy brown
            case WATER_VAPOR_RICH -> 0xFFFFFF;     // White (steam fog)
            case HYDROGEN_SULFIDE -> 0xFFFF99;     // Light yellow
            case NOBLE_GAS_MIXTURE -> 0xF8F8FF;    // Ghost white
            case TRACE_ATMOSPHERE -> 0x696969;     // Dim gray
            case VACUUM -> 0x000000;               // Black
        };

        // Apply temperature variation
        if (temperature > 40) {
            return tintColor(baseColor, 0xFFA500, 0.15f);
        } else if (temperature < -20) {
            return tintColor(baseColor, 0xB0E0E6, 0.1f);
        }

        return baseColor;
    }

    /**
     * Blend two colors by a factor
     */
    private int tintColor(int baseColor, int tintColor, float factor) {
        int baseR = (baseColor >> 16) & 0xFF;
        int baseG = (baseColor >> 8) & 0xFF;
        int baseB = baseColor & 0xFF;

        int tintR = (tintColor >> 16) & 0xFF;
        int tintG = (tintColor >> 8) & 0xFF;
        int tintB = tintColor & 0xFF;

        int resultR = (int) (baseR * (1 - factor) + tintR * factor);
        int resultG = (int) (baseG * (1 - factor) + tintG * factor);
        int resultB = (int) (baseB * (1 - factor) + tintB * factor);

        return (resultR << 16) | (resultG << 8) | resultB;
    }

    private int getWaterColor(float temperature) {
        if (temperature < -10) return 0x3938C9; // Deep blue for cold
        if (temperature < 20) return 0x3F76E4;  // Normal blue
        if (temperature < 40) return 0x45ADF2;  // Light blue for warm
        return 0x00BFFF;                        // Bright blue for hot
    }

    private int getWaterFogColor(float temperature) {
        if (temperature < -10) return 0x050533; // Dark blue for cold
        if (temperature < 20) return 0x0E4B9A;  // Normal dark blue
        if (temperature < 40) return 0x1787D4;  // Lighter for warm
        return 0x0080FF;                        // Bright for hot
    }

    // === SPAWN SETTINGS HELPERS (unchanged) ===

    private SpawnSettings createOceanSpawns() {
        SpawnSettings.Builder builder = new SpawnSettings.Builder();
        return builder.build();
    }

    private SpawnSettings createMountainSpawns() {
        SpawnSettings.Builder builder = new SpawnSettings.Builder();
        return builder.build();
    }

    private SpawnSettings createHighlandSpawns() {
        SpawnSettings.Builder builder = new SpawnSettings.Builder();
        return builder.build();
    }

    private SpawnSettings createHostileSpawns() {
        SpawnSettings.Builder builder = new SpawnSettings.Builder();
        return builder.build();
    }

    private SpawnSettings createMarginalSpawns() {
        SpawnSettings.Builder builder = new SpawnSettings.Builder();
        return builder.build();
    }

    private SpawnSettings createThrivingSpawns() {
        SpawnSettings.Builder builder = new SpawnSettings.Builder();
        return builder.build();
    }

    private SpawnSettings createNoSpawns() {
        return new SpawnSettings.Builder().build();
    }
}