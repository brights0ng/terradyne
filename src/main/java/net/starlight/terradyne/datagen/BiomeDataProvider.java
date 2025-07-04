package net.starlight.terradyne.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.sound.BiomeMoodSound;
import net.minecraft.world.biome.*;
import net.starlight.terradyne.planet.biome.ModBiomes;

import java.util.concurrent.CompletableFuture;

/**
 * Generates all Terradyne biome JSON files using Fabric's data generation system
 * Creates ~45 physics-based biomes with appropriate properties
 */
public class BiomeDataProvider extends FabricDynamicRegistryProvider {

    public BiomeDataProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup registries, Entries entries) {
        // === WATER BIOMES ===
        entries.add(ModBiomes.FROZEN_OCEAN, createOceanBiome(-15.0f, 0.3f, 0x3F5F8F, 0x062F4F, true));
        entries.add(ModBiomes.FRIGID_OCEAN, createOceanBiome(-5.0f, 0.5f, 0x4F6F9F, 0x0C3F5F, false));
        entries.add(ModBiomes.DEAD_OCEAN, createOceanBiome(10.0f, 0.1f, 0x2F2F2F, 0x1F1F1F, false));
        entries.add(ModBiomes.OCEAN, createOceanBiome(15.0f, 0.5f, 0x3F76E4, 0x0E4B9A, false));
        entries.add(ModBiomes.WARM_OCEAN, createOceanBiome(20.0f, 0.7f, 0x45ADF2, 0x1787D4, false));
        entries.add(ModBiomes.CORAL_OCEAN, createOceanBiome(25.0f, 0.9f, 0x43D5EE, 0x02B0E6, false));
        entries.add(ModBiomes.TROPICAL_OCEAN, createOceanBiome(35.0f, 0.8f, 0x00FFFF, 0x00CCCC, false));
        entries.add(ModBiomes.BOILING_OCEAN, createOceanBiome(70.0f, 0.2f, 0xFF4444, 0xCC1111, false));

        // === MOUNTAIN BIOMES ===
        entries.add(ModBiomes.FROZEN_PEAKS, createMountainBiome(-20.0f, 0.6f, 0xFFFFFF, 0xE0E0E0));
        entries.add(ModBiomes.MOUNTAIN_FOOTHILLS, createMountainBiome(5.0f, 0.4f, 0x8B8680, 0x6F6A60));
        entries.add(ModBiomes.MOUNTAIN_PEAKS, createMountainBiome(10.0f, 0.3f, 0x888888, 0x666666));
        entries.add(ModBiomes.ALPINE_PEAKS, createMountainBiome(0.0f, 0.5f, 0xC0C0C0, 0xA0A0A0));
        entries.add(ModBiomes.VOLCANIC_WASTELAND, createMountainBiome(40.0f, 0.1f, 0x4A3728, 0x2A1708));
        entries.add(ModBiomes.VOLCANIC_MOUNTAINS, createMountainBiome(35.0f, 0.4f, 0x654321, 0x432818));

        // === HIGHLAND BIOMES ===
        entries.add(ModBiomes.BARREN_HIGHLANDS, createHighlandBiome(15.0f, 0.2f, 0x9E8873, 0x7C6B53));
        entries.add(ModBiomes.WINDSWEPT_HILLS, createHighlandBiome(10.0f, 0.3f, 0x8FAA7C, 0x6F8A5C));
        entries.add(ModBiomes.ROLLING_HILLS, createHighlandBiome(18.0f, 0.6f, 0x79C05A, 0x59A03A));
        entries.add(ModBiomes.HIGHLAND_TUNDRA, createHighlandBiome(-5.0f, 0.4f, 0x8FBBAE, 0x6F9B8E));
        entries.add(ModBiomes.FORESTED_HILLS, createHighlandBiome(15.0f, 0.8f, 0x68A55F, 0x48853F));
        entries.add(ModBiomes.TROPICAL_HIGHLANDS, createHighlandBiome(28.0f, 0.7f, 0x55C93F, 0x35A91F));

        // === HOSTILE CONTINENTAL BIOMES ===
        entries.add(ModBiomes.FROZEN_WASTELAND, createHostileBiome(-30.0f, 0.1f, 0xE0E0FF, 0xC0C0DF));
        entries.add(ModBiomes.ROCKY_DESERT, createHostileBiome(25.0f, 0.0f, 0xBEA17C, 0x9E815C));
        entries.add(ModBiomes.SCORCHED_PLAINS, createHostileBiome(50.0f, 0.0f, 0xFF6600, 0xCC4400));
        entries.add(ModBiomes.WINDSWEPT_TUNDRA, createHostileBiome(-10.0f, 0.2f, 0xAEC6CF, 0x8EA6AF));
        entries.add(ModBiomes.SANDY_DESERT, createHostileBiome(30.0f, 0.1f, 0xFAD5A5, 0xDAB585));
        entries.add(ModBiomes.DESERT_MESA, createHostileBiome(35.0f, 0.1f, 0xD2B48C, 0xB2946C));
        entries.add(ModBiomes.DUST_BOWL, createHostileBiome(45.0f, 0.0f, 0xCC9966, 0xAA7744));

        // === MARGINAL CONTINENTAL BIOMES ===
        entries.add(ModBiomes.COLD_STEPPES, createMarginalBiome(-15.0f, 0.3f, 0xB0C4DE, 0x90A4BE));
        entries.add(ModBiomes.TUNDRA, createMarginalBiome(-5.0f, 0.2f, 0x80B497, 0x609477));
        entries.add(ModBiomes.BOREAL_PLAINS, createMarginalBiome(0.0f, 0.5f, 0x8DB360, 0x6D9340));
        entries.add(ModBiomes.DRY_STEPPES, createMarginalBiome(12.0f, 0.2f, 0xC6B894, 0xA69874));
        entries.add(ModBiomes.TEMPERATE_STEPPES, createMarginalBiome(15.0f, 0.4f, 0x8CAD5C, 0x6C8D3C));
        entries.add(ModBiomes.MEADOWS, createMarginalBiome(18.0f, 0.7f, 0x79C05A, 0x59A03A));
        entries.add(ModBiomes.SAVANNA, createMarginalBiome(32.0f, 0.3f, 0xBDB25F, 0x9D923F));
        entries.add(ModBiomes.TROPICAL_GRASSLAND, createMarginalBiome(35.0f, 0.5f, 0x88BB67, 0x689B47));

        // === THRIVING CONTINENTAL BIOMES ===
        // Cold Zone
        entries.add(ModBiomes.SNOWY_PLAINS, createThrivingBiome(-2.0f, 0.5f, 0xFFFFFF, 0xE0E0E0));
        entries.add(ModBiomes.TAIGA, createThrivingBiome(-5.0f, 0.4f, 0x596651, 0x394631));
        entries.add(ModBiomes.SNOW_FOREST, createThrivingBiome(-8.0f, 0.7f, 0x809860, 0x607840));
        entries.add(ModBiomes.ALPINE_MEADOWS, createThrivingBiome(-3.0f, 0.6f, 0x90B380, 0x709360));

        // Temperate Zone
        entries.add(ModBiomes.PLAINS, createThrivingBiome(15.0f, 0.4f, 0x91BD59, 0x719D39));
        entries.add(ModBiomes.MIXED_PLAINS, createThrivingBiome(18.0f, 0.6f, 0x7CBD51, 0x5C9D31));
        entries.add(ModBiomes.WETLANDS, createThrivingBiome(20.0f, 0.9f, 0x4F7F4F, 0x2F5F2F));
        entries.add(ModBiomes.OAK_FOREST, createThrivingBiome(16.0f, 0.3f, 0x79C05A, 0x59A03A));
        entries.add(ModBiomes.MIXED_FOREST, createThrivingBiome(18.0f, 0.5f, 0x68A055, 0x488035));
        entries.add(ModBiomes.DENSE_FOREST, createThrivingBiome(20.0f, 0.8f, 0x507A32, 0x305A12));
        entries.add(ModBiomes.MOUNTAIN_FOREST, createThrivingBiome(12.0f, 0.6f, 0x6A7D47, 0x4A5D27));

        // Warm Zone
        entries.add(ModBiomes.HOT_SHRUBLAND, createThrivingBiome(30.0f, 0.3f, 0xAEA42A, 0x8E840A));
        entries.add(ModBiomes.WINDY_STEPPES, createThrivingBiome(32.0f, 0.4f, 0xB5A642, 0x958622));
        entries.add(ModBiomes.TEMPERATE_RAINFOREST, createThrivingBiome(28.0f, 0.6f, 0x0C7B3B, 0x005B1B));
        entries.add(ModBiomes.CLOUD_FOREST, createThrivingBiome(25.0f, 0.7f, 0x2D5D2D, 0x0D3D0D));
        entries.add(ModBiomes.JUNGLE, createThrivingBiome(30.0f, 0.9f, 0x22B14C, 0x02911C));
        entries.add(ModBiomes.TROPICAL_RAINFOREST, createThrivingBiome(32.0f, 0.8f, 0x1A8F42, 0x006F22));

        // Hot Zone
        entries.add(ModBiomes.HOT_DESERT, createThrivingBiome(45.0f, 0.1f, 0xFA9418, 0xDA7400));
        entries.add(ModBiomes.TROPICAL_SWAMP, createThrivingBiome(42.0f, 0.7f, 0x4C6123, 0x2C4103));

        // === EXTREME BIOMES ===
        entries.add(ModBiomes.EXTREME_FROZEN_WASTELAND, createExtremeBiome(-60.0f, 0.0f, 0xF0F8FF, 0xD0D8DF));
        entries.add(ModBiomes.MOLTEN_WASTELAND, createExtremeBiome(120.0f, 0.0f, 0xFF4500, 0xCC2200));

        // === DEBUG BIOME ===
        entries.add(ModBiomes.DEBUG, createDebugBiome());
    }

    @Override
    public String getName() {
        return "Terradyne Biomes";
    }

    // === BIOME CREATION HELPERS ===

    /**
     * Create ocean biome with temperature-appropriate properties
     */
    private Biome createOceanBiome(float temperature, float downfall, int waterColor, int waterFogColor, boolean frozen) {
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
                .generationSettings(createMinimalGeneration())
                .build();
    }

    /**
     * Create mountain biome with appropriate properties
     */
    private Biome createMountainBiome(float temperature, float downfall, int grassColor, int foliageColor) {
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
                .generationSettings(createMinimalGeneration())
                .build();
    }

    /**
     * Create highland biome with moderate properties
     */
    private Biome createHighlandBiome(float temperature, float downfall, int grassColor, int foliageColor) {
        return createMountainBiome(temperature, downfall, grassColor, foliageColor); // Similar to mountains
    }

    /**
     * Create hostile biome with minimal life
     */
    private Biome createHostileBiome(float temperature, float downfall, int grassColor, int foliageColor) {
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
                .generationSettings(createMinimalGeneration())
                .build();
    }

    /**
     * Create marginal biome with limited life
     */
    private Biome createMarginalBiome(float temperature, float downfall, int grassColor, int foliageColor) {
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
                .generationSettings(createMinimalGeneration())
                .build();
    }

    /**
     * Create thriving biome with rich life
     */
    private Biome createThrivingBiome(float temperature, float downfall, int grassColor, int foliageColor) {
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
                .generationSettings(createMinimalGeneration())
                .build();
    }

    /**
     * Create extreme biome for temperature overrides
     */
    private Biome createExtremeBiome(float temperature, float downfall, int grassColor, int foliageColor) {
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
                .generationSettings(createMinimalGeneration())
                .build();
    }

    /**
     * Create debug biome
     */
    private Biome createDebugBiome() {
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
                .generationSettings(createMinimalGeneration())
                .build();
    }

    // === COLOR CALCULATION HELPERS ===

    private int getSkyColor(float temperature) {
        // Sky color based on temperature: blue for normal, reddish for hot, grayish for cold
        if (temperature < -20) return 0x6F8FBF; // Pale blue
        if (temperature < 0) return 0x78A7FF;   // Normal blue
        if (temperature < 40) return 0x78A7FF;  // Normal blue
        if (temperature < 60) return 0xFF9A56;  // Orange-ish
        return 0xFF6B47; // Red-orange
    }

    private int getFogColor(float temperature) {
        if (temperature < -20) return 0xC0D0E0; // Pale
        if (temperature < 0) return 0xC0D8FF;   // Normal
        if (temperature < 40) return 0xC0D8FF;  // Normal
        if (temperature < 60) return 0xFFB380;  // Orange tint
        return 0xFF8066; // Red tint
    }

    private int getWaterColor(float temperature) {
        if (temperature < 0) return 0x3F5F8F;   // Cold blue
        if (temperature < 25) return 0x3F76E4;  // Normal blue
        if (temperature < 60) return 0x45ADF2;  // Warm blue
        return 0xFF4444; // Hot red
    }

    private int getWaterFogColor(float temperature) {
        if (temperature < 0) return 0x2F4F7F;
        if (temperature < 25) return 0x0E4B9A;
        if (temperature < 60) return 0x1787D4;
        return 0xCC2222;
    }

    private int getGrassColor(float temperature, float downfall) {
        // Simple grass color calculation
        return 0x91BD59; // Default green
    }

    private int getFoliageColor(float temperature, float downfall) {
        // Simple foliage color calculation
        return 0x77AB2F; // Default green
    }

    // === SPAWN SETTINGS ===

    private SpawnSettings createOceanSpawns() {
        return new SpawnSettings.Builder().build(); // No land mobs in ocean
    }

    private SpawnSettings createMountainSpawns() {
        return new SpawnSettings.Builder().build(); // Minimal spawns for now
    }

    private SpawnSettings createHostileSpawns() {
        return new SpawnSettings.Builder().build(); // No spawns in hostile
    }

    private SpawnSettings createMarginalSpawns() {
        return new SpawnSettings.Builder().build(); // Few spawns
    }

    private SpawnSettings createThrivingSpawns() {
        return new SpawnSettings.Builder()
                .spawn(SpawnGroup.CREATURE, new SpawnSettings.SpawnEntry(net.minecraft.entity.EntityType.SHEEP, 12, 4, 4))
                .build();
    }

    private SpawnSettings createNoSpawns() {
        return new SpawnSettings.Builder().build();
    }

    // === GENERATION SETTINGS ===

    private GenerationSettings createMinimalGeneration() {
        return new GenerationSettings.Builder().build(); // No features for now
    }
}