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
import net.starlight.terradyne.planet.physics.CrustComposition;

import java.util.concurrent.CompletableFuture;

/**
 * Generates all Terradyne biome JSON files using physics-based component system
 * FIXED: Now passes registries to BiomeFeatureGenerator for proper feature generation
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
        entries.add(ModBiomes.FROZEN_OCEAN, createOceanBiome(ModBiomes.FROZEN_OCEAN, -15.0f, 0.3f, 0x3F5F8F, 0x062F4F, true, CrustComposition.SILICATE));
        entries.add(ModBiomes.FRIGID_OCEAN, createOceanBiome(ModBiomes.FRIGID_OCEAN, -5.0f, 0.5f, 0x4F6F9F, 0x0C3F5F, false, CrustComposition.SILICATE));
        entries.add(ModBiomes.DEAD_OCEAN, createOceanBiome(ModBiomes.DEAD_OCEAN, 10.0f, 0.1f, 0x2F2F2F, 0x1F1F1F, false, CrustComposition.REGOLITHIC));
        entries.add(ModBiomes.OCEAN, createOceanBiome(ModBiomes.OCEAN, 15.0f, 0.5f, 0x3F76E4, 0x0E4B9A, false, CrustComposition.SILICATE));
        entries.add(ModBiomes.WARM_OCEAN, createOceanBiome(ModBiomes.WARM_OCEAN, 20.0f, 0.7f, 0x45ADF2, 0x1787D4, false, CrustComposition.SILICATE));
        entries.add(ModBiomes.CORAL_OCEAN, createOceanBiome(ModBiomes.CORAL_OCEAN, 25.0f, 0.9f, 0x43D5EE, 0x02B0E6, false, CrustComposition.SILICATE));
        entries.add(ModBiomes.TROPICAL_OCEAN, createOceanBiome(ModBiomes.TROPICAL_OCEAN, 35.0f, 0.8f, 0x00FFFF, 0x00CCCC, false, CrustComposition.SILICATE));
        entries.add(ModBiomes.BOILING_OCEAN, createOceanBiome(ModBiomes.BOILING_OCEAN, 70.0f, 0.2f, 0xFF4444, 0xCC1111, false, CrustComposition.HADEAN));

        // === MOUNTAIN BIOMES ===
        entries.add(ModBiomes.FROZEN_PEAKS, createMountainBiome(ModBiomes.FROZEN_PEAKS, -20.0f, 0.6f, 0xFFFFFF, 0xE0E0E0, CrustComposition.SILICATE));
        entries.add(ModBiomes.MOUNTAIN_FOOTHILLS, createMountainBiome(ModBiomes.MOUNTAIN_FOOTHILLS, 5.0f, 0.4f, 0x8B8680, 0x6F6A60, CrustComposition.SILICATE));
        entries.add(ModBiomes.MOUNTAIN_PEAKS, createMountainBiome(ModBiomes.MOUNTAIN_PEAKS, 10.0f, 0.3f, 0x888888, 0x666666, CrustComposition.SILICATE));
        entries.add(ModBiomes.ALPINE_PEAKS, createMountainBiome(ModBiomes.ALPINE_PEAKS, 0.0f, 0.5f, 0xC0C0C0, 0xA0A0A0, CrustComposition.SILICATE));
        entries.add(ModBiomes.VOLCANIC_WASTELAND, createMountainBiome(ModBiomes.VOLCANIC_WASTELAND, 40.0f, 0.1f, 0x4A3728, 0x2A1708, CrustComposition.BASALTIC));
        entries.add(ModBiomes.VOLCANIC_MOUNTAINS, createMountainBiome(ModBiomes.VOLCANIC_MOUNTAINS, 35.0f, 0.4f, 0x654321, 0x432818, CrustComposition.BASALTIC));

        // === HIGHLAND BIOMES ===
        entries.add(ModBiomes.BARREN_HIGHLANDS, createHighlandBiome(ModBiomes.BARREN_HIGHLANDS, 15.0f, 0.2f, 0x9E8873, 0x7C6B53, CrustComposition.REGOLITHIC));
        entries.add(ModBiomes.WINDSWEPT_HILLS, createHighlandBiome(ModBiomes.WINDSWEPT_HILLS, 10.0f, 0.3f, 0x8FAA7C, 0x6F8A5C, CrustComposition.SILICATE));
        entries.add(ModBiomes.ROLLING_HILLS, createHighlandBiome(ModBiomes.ROLLING_HILLS, 18.0f, 0.6f, 0x79C05A, 0x59A03A, CrustComposition.SILICATE));
        entries.add(ModBiomes.HIGHLAND_TUNDRA, createHighlandBiome(ModBiomes.HIGHLAND_TUNDRA, -5.0f, 0.4f, 0x8FBBAE, 0x6F9B8E, CrustComposition.SILICATE));
        entries.add(ModBiomes.FORESTED_HILLS, createHighlandBiome(ModBiomes.FORESTED_HILLS, 15.0f, 0.8f, 0x68A55F, 0x48853F, CrustComposition.SILICATE));
        entries.add(ModBiomes.TROPICAL_HIGHLANDS, createHighlandBiome(ModBiomes.TROPICAL_HIGHLANDS, 28.0f, 0.7f, 0x55C93F, 0x35A91F, CrustComposition.SILICATE));

        // === HOSTILE CONTINENTAL BIOMES ===
        entries.add(ModBiomes.FROZEN_WASTELAND, createHostileBiome(ModBiomes.FROZEN_WASTELAND, -30.0f, 0.1f, 0xE0E0FF, 0xC0C0DF, CrustComposition.REGOLITHIC));
        entries.add(ModBiomes.ROCKY_DESERT, createHostileBiome(ModBiomes.ROCKY_DESERT, 25.0f, 0.0f, 0xBEA17C, 0x9E815C, CrustComposition.REGOLITHIC));
        entries.add(ModBiomes.SCORCHED_PLAINS, createHostileBiome(ModBiomes.SCORCHED_PLAINS, 50.0f, 0.0f, 0xFF6600, 0xCC4400, CrustComposition.SULFURIC));
        entries.add(ModBiomes.WINDSWEPT_TUNDRA, createHostileBiome(ModBiomes.WINDSWEPT_TUNDRA, -10.0f, 0.2f, 0xAEC6CF, 0x8EA6AF, CrustComposition.SILICATE));
        entries.add(ModBiomes.SANDY_DESERT, createHostileBiome(ModBiomes.SANDY_DESERT, 30.0f, 0.1f, 0xFAD5A5, 0xDAB585, CrustComposition.HALLIDE));
        entries.add(ModBiomes.DESERT_MESA, createHostileBiome(ModBiomes.DESERT_MESA, 35.0f, 0.1f, 0xD2B48C, 0xB2946C, CrustComposition.HALLIDE));
        entries.add(ModBiomes.DUST_BOWL, createHostileBiome(ModBiomes.DUST_BOWL, 45.0f, 0.0f, 0xCC9966, 0xAA7744, CrustComposition.REGOLITHIC));

        // === MARGINAL CONTINENTAL BIOMES ===
        entries.add(ModBiomes.COLD_STEPPES, createMarginalBiome(ModBiomes.COLD_STEPPES, -15.0f, 0.3f, 0xB0C4DE, 0x90A4BE, CrustComposition.SILICATE));
        entries.add(ModBiomes.TUNDRA, createMarginalBiome(ModBiomes.TUNDRA, -5.0f, 0.2f, 0x80B497, 0x609477, CrustComposition.SILICATE));
        entries.add(ModBiomes.BOREAL_PLAINS, createMarginalBiome(ModBiomes.BOREAL_PLAINS, 0.0f, 0.5f, 0x8DB360, 0x6D9340, CrustComposition.SILICATE));
        entries.add(ModBiomes.DRY_STEPPES, createMarginalBiome(ModBiomes.DRY_STEPPES, 12.0f, 0.2f, 0xC6B894, 0xA69874, CrustComposition.SILICATE));
        entries.add(ModBiomes.TEMPERATE_STEPPES, createMarginalBiome(ModBiomes.TEMPERATE_STEPPES, 15.0f, 0.4f, 0x8CAD5C, 0x6C8D3C, CrustComposition.SILICATE));
        entries.add(ModBiomes.MEADOWS, createMarginalBiome(ModBiomes.MEADOWS, 18.0f, 0.7f, 0x79C05A, 0x59A03A, CrustComposition.SILICATE));
        entries.add(ModBiomes.SAVANNA, createMarginalBiome(ModBiomes.SAVANNA, 32.0f, 0.3f, 0xBDB25F, 0x9D923F, CrustComposition.SILICATE));
        entries.add(ModBiomes.TROPICAL_GRASSLAND, createMarginalBiome(ModBiomes.TROPICAL_GRASSLAND, 35.0f, 0.5f, 0x88BB67, 0x689B47, CrustComposition.SILICATE));

        // === THRIVING CONTINENTAL BIOMES ===
        // Cold Zone
        entries.add(ModBiomes.SNOWY_PLAINS, createThrivingBiome(ModBiomes.SNOWY_PLAINS, -2.0f, 0.5f, 0xFFFFFF, 0xE0E0E0, CrustComposition.SILICATE));
        entries.add(ModBiomes.TAIGA, createThrivingBiome(ModBiomes.TAIGA, -5.0f, 0.4f, 0x596651, 0x394631, CrustComposition.SILICATE));
        entries.add(ModBiomes.SNOW_FOREST, createThrivingBiome(ModBiomes.SNOW_FOREST, -8.0f, 0.7f, 0x809860, 0x607840, CrustComposition.SILICATE));
        entries.add(ModBiomes.ALPINE_MEADOWS, createThrivingBiome(ModBiomes.ALPINE_MEADOWS, -3.0f, 0.6f, 0x90B380, 0x709360, CrustComposition.SILICATE));

        // Temperate Zone
        entries.add(ModBiomes.PLAINS, createThrivingBiome(ModBiomes.PLAINS, 15.0f, 0.4f, 0x91BD59, 0x719D39, CrustComposition.SILICATE));
        entries.add(ModBiomes.MIXED_PLAINS, createThrivingBiome(ModBiomes.MIXED_PLAINS, 18.0f, 0.6f, 0x7CBD51, 0x5C9D31, CrustComposition.SILICATE));
        entries.add(ModBiomes.WETLANDS, createThrivingBiome(ModBiomes.WETLANDS, 20.0f, 0.9f, 0x4F7F4F, 0x2F5F2F, CrustComposition.SILICATE));
        entries.add(ModBiomes.OAK_FOREST, createThrivingBiome(ModBiomes.OAK_FOREST, 16.0f, 0.3f, 0x79C05A, 0x59A03A, CrustComposition.SILICATE));
        entries.add(ModBiomes.MIXED_FOREST, createThrivingBiome(ModBiomes.MIXED_FOREST, 18.0f, 0.5f, 0x68A055, 0x488035, CrustComposition.SILICATE));
        entries.add(ModBiomes.DENSE_FOREST, createThrivingBiome(ModBiomes.DENSE_FOREST, 20.0f, 0.8f, 0x507A32, 0x305A12, CrustComposition.SILICATE));
        entries.add(ModBiomes.MOUNTAIN_FOREST, createThrivingBiome(ModBiomes.MOUNTAIN_FOREST, 12.0f, 0.6f, 0x6A7D47, 0x4A5D27, CrustComposition.SILICATE));

        // Warm Zone
        entries.add(ModBiomes.HOT_SHRUBLAND, createThrivingBiome(ModBiomes.HOT_SHRUBLAND, 30.0f, 0.3f, 0xAEA42A, 0x8E840A, CrustComposition.SILICATE));
        entries.add(ModBiomes.WINDY_STEPPES, createThrivingBiome(ModBiomes.WINDY_STEPPES, 32.0f, 0.4f, 0xB5A642, 0x958622, CrustComposition.SILICATE));
        entries.add(ModBiomes.TEMPERATE_RAINFOREST, createThrivingBiome(ModBiomes.TEMPERATE_RAINFOREST, 28.0f, 0.6f, 0x0C7B3B, 0x005B1B, CrustComposition.SILICATE));
        entries.add(ModBiomes.CLOUD_FOREST, createThrivingBiome(ModBiomes.CLOUD_FOREST, 25.0f, 0.7f, 0x2D5D2D, 0x0D3D0D, CrustComposition.SILICATE));
        entries.add(ModBiomes.JUNGLE, createThrivingBiome(ModBiomes.JUNGLE, 30.0f, 0.9f, 0x22B14C, 0x02911C, CrustComposition.SILICATE));
        entries.add(ModBiomes.TROPICAL_RAINFOREST, createThrivingBiome(ModBiomes.TROPICAL_RAINFOREST, 32.0f, 0.8f, 0x1A8F42, 0x006F22, CrustComposition.SILICATE));

        // Hot Zone
        entries.add(ModBiomes.HOT_DESERT, createThrivingBiome(ModBiomes.HOT_DESERT, 45.0f, 0.1f, 0xFA9418, 0xDA7400, CrustComposition.SULFURIC));
        entries.add(ModBiomes.TROPICAL_SWAMP, createThrivingBiome(ModBiomes.TROPICAL_SWAMP, 42.0f, 0.7f, 0x4C6123, 0x2C4103, CrustComposition.SILICATE));

        // === EXTREME BIOMES ===
        entries.add(ModBiomes.EXTREME_FROZEN_WASTELAND, createExtremeBiome(ModBiomes.EXTREME_FROZEN_WASTELAND, -60.0f, 0.0f, 0xF0F8FF, 0xD0D8DF, CrustComposition.REGOLITHIC));
        entries.add(ModBiomes.MOLTEN_WASTELAND, createExtremeBiome(ModBiomes.MOLTEN_WASTELAND, 120.0f, 0.0f, 0xFF4500, 0xCC2200, CrustComposition.HADEAN));

        // === DEBUG BIOME ===
        entries.add(ModBiomes.DEBUG, createDebugBiome(ModBiomes.DEBUG));
    }

    @Override
    public String getName() {
        return "Terradyne Biomes (Component-Based)";
    }

    // === BIOME CREATION HELPERS ===

    /**
     * Create ocean biome with component-based generation
     * FIXED: Now passes registries to component generation
     */
    private Biome createOceanBiome(RegistryKey<Biome> biomeKey, float temperature, float downfall, int waterColor, int waterFogColor, boolean frozen, CrustComposition crustComposition) {
        return new Biome.Builder()
                .temperature(temperature)
                .downfall(downfall)
                .precipitation(frozen || temperature < 0.15f)
                .effects(new BiomeEffects.Builder()
                        .skyColor(getSkyColor(temperature))
                        .fogColor(getFogColor(temperature))
                        .waterColor(waterColor)
                        .waterFogColor(waterFogColor)
                        .grassColor(getGrassColor(temperature, downfall))
                        .foliageColor(getFoliageColor(temperature, downfall))
                        .moodSound(BiomeMoodSound.CAVE)
                        .build())
                .spawnSettings(createOceanSpawns())
                .generationSettings(createComponentBasedGeneration(biomeKey, crustComposition, temperature, downfall, 0.3))
                .build();
    }

    /**
     * Create mountain biome with component-based generation
     * FIXED: Now passes registries to component generation
     */
    private Biome createMountainBiome(RegistryKey<Biome> biomeKey, float temperature, float downfall, int grassColor, int foliageColor, CrustComposition crustComposition) {
        return new Biome.Builder()
                .temperature(temperature)
                .downfall(downfall)
                .precipitation(temperature < 0.15f)
                .effects(new BiomeEffects.Builder()
                        .skyColor(getSkyColor(temperature))
                        .fogColor(getFogColor(temperature))
                        .waterColor(getWaterColor(temperature))
                        .waterFogColor(getWaterFogColor(temperature))
                        .grassColor(grassColor)
                        .foliageColor(foliageColor)
                        .moodSound(BiomeMoodSound.CAVE)
                        .build())
                .spawnSettings(createMountainSpawns())
                .generationSettings(createComponentBasedGeneration(biomeKey, crustComposition, temperature, downfall, 0.4))
                .build();
    }

    /**
     * Create highland biome with component-based generation
     * FIXED: Now passes registries to component generation
     */
    private Biome createHighlandBiome(RegistryKey<Biome> biomeKey, float temperature, float downfall, int grassColor, int foliageColor, CrustComposition crustComposition) {
        return new Biome.Builder()
                .temperature(temperature)
                .downfall(downfall)
                .precipitation(temperature < 0.15f)
                .effects(new BiomeEffects.Builder()
                        .skyColor(getSkyColor(temperature))
                        .fogColor(getFogColor(temperature))
                        .waterColor(getWaterColor(temperature))
                        .waterFogColor(getWaterFogColor(temperature))
                        .grassColor(grassColor)
                        .foliageColor(foliageColor)
                        .moodSound(BiomeMoodSound.CAVE)
                        .build())
                .spawnSettings(createHighlandSpawns())
                .generationSettings(createComponentBasedGeneration(biomeKey, crustComposition, temperature, downfall, 0.5))
                .build();
    }

    /**
     * Create hostile biome with component-based generation
     * FIXED: Now passes registries to component generation
     */
    private Biome createHostileBiome(RegistryKey<Biome> biomeKey, float temperature, float downfall, int grassColor, int foliageColor, CrustComposition crustComposition) {
        return new Biome.Builder()
                .temperature(temperature)
                .downfall(downfall)
                .precipitation(temperature < 0.15f)
                .effects(new BiomeEffects.Builder()
                        .skyColor(getSkyColor(temperature))
                        .fogColor(getFogColor(temperature))
                        .waterColor(getWaterColor(temperature))
                        .waterFogColor(getWaterFogColor(temperature))
                        .grassColor(grassColor)
                        .foliageColor(foliageColor)
                        .moodSound(BiomeMoodSound.CAVE)
                        .build())
                .spawnSettings(createHostileSpawns())
                .generationSettings(createComponentBasedGeneration(biomeKey, crustComposition, temperature, downfall, 0.2))
                .build();
    }

    /**
     * Create marginal biome with component-based generation
     * FIXED: Now passes registries to component generation
     */
    private Biome createMarginalBiome(RegistryKey<Biome> biomeKey, float temperature, float downfall, int grassColor, int foliageColor, CrustComposition crustComposition) {
        return new Biome.Builder()
                .temperature(temperature)
                .downfall(downfall)
                .precipitation(temperature < 0.15f)
                .effects(new BiomeEffects.Builder()
                        .skyColor(getSkyColor(temperature))
                        .fogColor(getFogColor(temperature))
                        .waterColor(getWaterColor(temperature))
                        .waterFogColor(getWaterFogColor(temperature))
                        .grassColor(grassColor)
                        .foliageColor(foliageColor)
                        .moodSound(BiomeMoodSound.CAVE)
                        .build())
                .spawnSettings(createMarginalSpawns())
                .generationSettings(createComponentBasedGeneration(biomeKey, crustComposition, temperature, downfall, 0.6))
                .build();
    }

    /**
     * Create thriving biome with component-based generation
     * FIXED: Now passes registries to component generation
     */
    private Biome createThrivingBiome(RegistryKey<Biome> biomeKey, float temperature, float downfall, int grassColor, int foliageColor, CrustComposition crustComposition) {
        return new Biome.Builder()
                .temperature(temperature)
                .downfall(downfall)
                .precipitation(temperature < 0.15f)
                .effects(new BiomeEffects.Builder()
                        .skyColor(getSkyColor(temperature))
                        .fogColor(getFogColor(temperature))
                        .waterColor(getWaterColor(temperature))
                        .waterFogColor(getWaterFogColor(temperature))
                        .grassColor(grassColor)
                        .foliageColor(foliageColor)
                        .moodSound(BiomeMoodSound.CAVE)
                        .build())
                .spawnSettings(createThrivingSpawns())
                .generationSettings(createComponentBasedGeneration(biomeKey, crustComposition, temperature, downfall, 0.8))
                .build();
    }

    /**
     * Create extreme biome with component-based generation
     * FIXED: Now passes registries to component generation
     */
    private Biome createExtremeBiome(RegistryKey<Biome> biomeKey, float temperature, float downfall, int grassColor, int foliageColor, CrustComposition crustComposition) {
        return new Biome.Builder()
                .temperature(temperature)
                .downfall(downfall)
                .precipitation(false) // Extreme conditions prevent normal precipitation
                .effects(new BiomeEffects.Builder()
                        .skyColor(getSkyColor(temperature))
                        .fogColor(getFogColor(temperature))
                        .waterColor(getWaterColor(temperature))
                        .waterFogColor(getWaterFogColor(temperature))
                        .grassColor(grassColor)
                        .foliageColor(foliageColor)
                        .moodSound(BiomeMoodSound.CAVE)
                        .build())
                .spawnSettings(createNoSpawns())
                .generationSettings(createComponentBasedGeneration(biomeKey, crustComposition, temperature, downfall, 0.0))
                .build();
    }

    /**
     * Create debug biome
     * FIXED: Now passes registries to component generation
     */
    private Biome createDebugBiome(RegistryKey<Biome> biomeKey) {
        return new Biome.Builder()
                .temperature(0.5f)
                .downfall(0.5f)
                .precipitation(false)
                .effects(new BiomeEffects.Builder()
                        .skyColor(0x78A7FF)
                        .fogColor(0xC0D8FF)
                        .waterColor(0x5F7F9F)
                        .waterFogColor(0x4F6F8F)
                        .grassColor(0x888888)  // Gray
                        .foliageColor(0x888888) // Gray
                        .moodSound(BiomeMoodSound.CAVE)
                        .build())
                .spawnSettings(createNoSpawns())
                .generationSettings(createComponentBasedGeneration(biomeKey, CrustComposition.SILICATE, 15.0f, 0.5f, 0.7))
                .build();
    }

    /**
     * FIXED: Create component-based generation settings with registries
     * Now actually passes registries to BiomeFeatureGenerator!
     */
    private GenerationSettings createComponentBasedGeneration(RegistryKey<Biome> biomeKey,
                                                              CrustComposition crustComposition,
                                                              double temperature,
                                                              double humidity,
                                                              double habitability) {
        try {
            // Now we pass the stored registries to BiomeFeatureGenerator
            return BiomeFeatureGenerator.createGenerationSettings(biomeKey, crustComposition, temperature, humidity, habitability, this.registries);
        } catch (Exception e) {
            // Fallback on any error
            return BiomeFeatureGenerator.createMinimalGeneration();
        }
    }

    // === COLOR CALCULATION HELPERS ===

    private int getSkyColor(float temperature) {
        if (temperature < -20) return 0x8FBFFF; // Pale blue for very cold
        if (temperature < 0) return 0x78A7FF;   // Blue for cold
        if (temperature < 20) return 0x78A7FF;  // Normal blue
        if (temperature < 40) return 0x87CEEB;  // Sky blue for warm
        return 0xFFB347;                        // Orange for hot
    }

    private int getFogColor(float temperature) {
        if (temperature < -20) return 0xE0E0FF; // Pale for very cold
        if (temperature < 0) return 0xC0D8FF;   // Light blue for cold
        if (temperature < 20) return 0xC0D8FF;  // Normal
        if (temperature < 40) return 0xF0F8FF;  // Warm white
        return 0xFFE4B5;                        // Warm orange for hot
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

    private int getGrassColor(float temperature, float downfall) {
        // Base grass color calculation
        if (temperature < -10) return 0xC0C0C0; // Grayish for very cold
        if (temperature < 10) return 0x80B497;  // Cold green
        if (downfall > 0.8) return 0x4F7F4F;    // Dark green for high moisture
        if (downfall < 0.2) return 0xBDB25F;    // Yellowish for dry
        return 0x91BD59;                        // Normal green
    }

    private int getFoliageColor(float temperature, float downfall) {
        // Base foliage color calculation
        if (temperature < -10) return 0xA0A0A0; // Gray for very cold
        if (temperature < 10) return 0x609477;  // Cold green
        if (downfall > 0.8) return 0x2F5F2F;    // Dark green for high moisture
        if (downfall < 0.2) return 0x9D923F;    // Yellowish for dry
        return 0x719D39;                        // Normal green
    }

    // === SPAWN SETTINGS HELPERS ===

    private SpawnSettings createOceanSpawns() {
        SpawnSettings.Builder builder = new SpawnSettings.Builder();
        // Add ocean-appropriate spawns here if needed
        return builder.build();
    }

    private SpawnSettings createMountainSpawns() {
        SpawnSettings.Builder builder = new SpawnSettings.Builder();
        // Add mountain-appropriate spawns here if needed
        return builder.build();
    }

    private SpawnSettings createHighlandSpawns() {
        SpawnSettings.Builder builder = new SpawnSettings.Builder();
        // Add highland-appropriate spawns here if needed
        return builder.build();
    }

    private SpawnSettings createHostileSpawns() {
        SpawnSettings.Builder builder = new SpawnSettings.Builder();
        // Add hostile environment spawns here if needed
        return builder.build();
    }

    private SpawnSettings createMarginalSpawns() {
        SpawnSettings.Builder builder = new SpawnSettings.Builder();
        // Add marginal environment spawns here if needed
        return builder.build();
    }

    private SpawnSettings createThrivingSpawns() {
        SpawnSettings.Builder builder = new SpawnSettings.Builder();
        // Add thriving environment spawns here if needed
        return builder.build();
    }

    private SpawnSettings createNoSpawns() {
        return new SpawnSettings.Builder().build();
    }
}