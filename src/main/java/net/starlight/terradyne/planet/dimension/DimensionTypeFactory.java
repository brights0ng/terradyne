package net.starlight.terradyne.planet.dimension;

import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.dimension.DimensionTypes;
import net.starlight.terradyne.planet.model.DesertModel;
import net.starlight.terradyne.planet.model.OceanicModel;
import net.starlight.terradyne.planet.model.RockyModel;

import java.util.OptionalLong;

/**
 * Factory class for creating custom dimension types based on planet characteristics
 */
public class DimensionTypeFactory {

    /**
     * Creates a custom dimension type for desert planets
     * Adjusts properties based on temperature and atmospheric conditions
     */
    public static DimensionType createDesertPlanetDimension(DesertModel desertModel) {
        float temperature = desertModel.getConfig().getSurfaceTemperature();
        float humidity = desertModel.getConfig().getHumidity();
        float atmosphericPressure = desertModel.getAtmosphericPressure();

        // Determine environmental characteristics
        boolean isHotDesert = temperature > 50.0f;
        boolean isExtremelyHot = temperature > 70.0f;
        boolean hasThinAtmosphere = atmosphericPressure < 0.5f;

        return new DimensionType(
                OptionalLong.empty(),                    // fixedTime - no fixed time, natural day/night
                true,                                    // hasSkylight - desert planets have visible sky
                false,                                   // hasCeiling - no bedrock ceiling
                isExtremelyHot,                         // ultrawarm - extremely hot deserts get nether-like properties
                true,                                    // natural - allow natural mob spawning (controlled by biomes)
                1.0,                                     // coordinateScale - normal coordinate scaling
                true,                                    // bedWorks - beds work on desert planets
                false,                                   // piglinSafe - piglins don't naturally spawn/survive
                0,                                     // minY - standard world bottom
                384,                                     // height - standard world height
                384,                                     // logicalHeight - same as height
                isExtremelyHot ? BlockTags.INFINIBURN_NETHER : BlockTags.INFINIBURN_OVERWORLD, // infiniburn behavior
                DimensionTypes.OVERWORLD_ID,             // effects - overworld-like sky and fog effects
                isHotDesert ? 0.1f : 0.0f,              // ambientLight - hot deserts have slight glow
                createDesertMonsterSettings(desertModel) // monsterSettings - custom spawning rules
        );
    }

    /**
     * Creates a custom dimension type for rocky/airless planets
     * Adjusts sky and atmospheric properties based on atmospheric density
     */
    public static DimensionType createRockyPlanetDimension(RockyModel rockyModel) {
        float atmosphericDensity = rockyModel.getConfig().getAtmosphericDensity();
        float temperatureVariation = rockyModel.getConfig().getTemperatureVariation();

        // Determine environmental characteristics
        boolean hasAtmosphere = atmosphericDensity > 0.02f;
        boolean hasThickAtmosphere = atmosphericDensity > 0.1f;
        boolean hasExtremeTempSwings = temperatureVariation > 100.0f;

        return new DimensionType(
                OptionalLong.empty(),                    // fixedTime - natural day/night cycle
                hasAtmosphere,                          // hasSkylight - no sky if no atmosphere (like space)
                false,                                   // hasCeiling - no bedrock ceiling
                false,                                   // ultrawarm - rocky planets tend to be cold
                hasThickAtmosphere,                     // natural - limited spawning on airless worlds
                1.0,                                     // coordinateScale - normal scaling
                hasAtmosphere,                          // bedWorks - beds only work with atmosphere
                false,                                   // piglinSafe - piglins don't survive on rocky worlds
                0,                                     // minY - standard bottom
                384,                                     // height - standard height
                384,                                     // logicalHeight - same as height
                BlockTags.INFINIBURN_OVERWORLD,        // infiniburn - normal fire behavior
                hasAtmosphere ? DimensionTypes.OVERWORLD_ID : DimensionTypes.THE_END_ID, // effects - End-like for airless
                hasAtmosphere ? 0.0f : 0.15f,           // ambientLight - starlight glow on airless worlds
                createRockyMonsterSettings(rockyModel)   // monsterSettings - limited spawning on airless worlds
        );
    }

    /**
     * Creates a custom dimension type for oceanic planets
     * Earth-like properties with rich atmospheric conditions
     */
    public static DimensionType createOceanicPlanetDimension(OceanicModel oceanicModel) {
        float atmosphericHumidity = oceanicModel.getConfig().getAtmosphericHumidity();
        float stormFrequency = oceanicModel.getStormFrequency();
        boolean hasIceCaps = oceanicModel.getConfig().hasIceCaps();

        return new DimensionType(
                OptionalLong.empty(),                    // fixedTime - natural day/night
                true,                                    // hasSkylight - oceanic worlds have clear skies
                false,                                   // hasCeiling - no ceiling
                false,                                   // ultrawarm - oceans moderate temperature
                true,                                    // natural - rich ecosystems support diverse spawning
                1.0,                                     // coordinateScale - normal scaling
                true,                                    // bedWorks - beds work normally
                false,                                   // piglinSafe - piglins don't survive in ocean worlds
                0,                                     // minY - standard bottom (deeper for ocean trenches)
                384,                                     // height - standard height
                384,                                     // logicalHeight - same as height
                BlockTags.INFINIBURN_OVERWORLD,        // infiniburn - normal fire behavior
                DimensionTypes.OVERWORLD_ID,             // effects - Earth-like atmosphere and sky
                0.0f,                                    // ambientLight - natural sunlight only
                createOceanicMonsterSettings(oceanicModel) // monsterSettings - rich ecosystem spawning
        );
    }

    /**
     * Creates a custom dimension type for volcanic planets
     * Hot, active worlds with frequent eruptions
     */
    public static DimensionType createVolcanicPlanetDimension(/* VolcanicModel when you create it */) {
        return new DimensionType(
                OptionalLong.empty(),                    // fixedTime
                true,                                    // hasSkylight - volcanic planets have sky (though often obscured)
                false,                                   // hasCeiling
                true,                                    // ultrawarm - volcanic worlds are very hot
                true,                                    // natural - life can exist in volcanic areas
                1.0,                                     // coordinateScale
                true,                                    // bedWorks - beds work but dangerous due to heat
                false,                                   // piglinSafe
                0,                                     // minY
                384,                                     // height
                384,                                     // logicalHeight
                BlockTags.INFINIBURN_NETHER,           // infiniburn - fire spreads like in nether
                DimensionTypes.THE_NETHER_ID,                // effects - nether-like atmosphere (red/smoky)
                0.2f,                                    // ambientLight - glow from lava and volcanic activity
                createVolcanicMonsterSettings()         // monsterSettings - heat-adapted spawning
        );
    }

    /**
     * Creates a custom dimension type for icy planets
     * Cold, frozen worlds with limited atmosphere
     */
    public static DimensionType createIcyPlanetDimension(/* IcyModel when you create it */) {
        return new DimensionType(
                OptionalLong.empty(),                    // fixedTime
                true,                                    // hasSkylight - icy planets have sky
                false,                                   // hasCeiling
                false,                                   // ultrawarm - opposite! Very cold
                false,                                   // natural - limited life in frozen conditions
                1.0,                                     // coordinateScale
                true,                                    // bedWorks - beds work but cold
                false,                                   // piglinSafe
                0,                                     // minY
                384,                                     // height
                384,                                     // logicalHeight
                BlockTags.INFINIBURN_OVERWORLD,        // infiniburn - normal (limited) fire behavior
                DimensionTypes.OVERWORLD_ID,             // effects - overworld-like but could be modified for cold
                0.05f,                                   // ambientLight - slight reflection from ice/snow
                createDefaultMonsterSettings()           // monsterSettings - limited spawning
        );
    }

    // === MONSTER SETTINGS FACTORY METHODS ===

    /**
     * Creates monster settings for desert planets
     * Reduced spawning in extreme heat, adapted mobs in moderate conditions
     */
    private static DimensionType.MonsterSettings createDesertMonsterSettings(DesertModel desertModel) {
        float temperature = desertModel.getConfig().getSurfaceTemperature();
        boolean isExtremelyHot = temperature > 70.0f;

        if (isExtremelyHot) {
            // Very hot deserts - minimal mob spawning
            return new DimensionType.MonsterSettings(
                    false,    // piglinSafe - piglins can't survive extreme heat
                    true,     // hasRaids - settlements could still have raids
                    UniformIntProvider.create(0, 7),  // monsterSpawnLightLevel - higher threshold due to heat stress
                    7         // monsterSpawnBlockLightLimit - limited spawning
            );
        } else {
            // Normal desert conditions
            return new DimensionType.MonsterSettings(
                    false,    // piglinSafe
                    true,     // hasRaids
                    UniformIntProvider.create(0, 7),  // monsterSpawnLightLevel - normal spawning
                    15        // monsterSpawnBlockLightLimit - normal spawning
            );
        }
    }

    /**
     * Creates monster settings for rocky planets
     * Very limited spawning on airless worlds, normal spawning with atmosphere
     */
    private static DimensionType.MonsterSettings createRockyMonsterSettings(RockyModel rockyModel) {
        boolean hasAtmosphere = rockyModel.getConfig().getAtmosphericDensity() > 0.02f;

        if (hasAtmosphere) {
            // Rocky planet with atmosphere - normal spawning
            return new DimensionType.MonsterSettings(
                    false,    // piglinSafe
                    true,     // hasRaids
                    UniformIntProvider.create(0, 7),  // monsterSpawnLightLevel
                    15        // monsterSpawnBlockLightLimit
            );
        } else {
            // Airless world - very limited spawning (maybe just some hardy creatures)
            return new DimensionType.MonsterSettings(
                    false,    // piglinSafe
                    false,    // hasRaids - no villages on airless worlds
                    UniformIntProvider.create(0, 0),  // monsterSpawnLightLevel - no natural spawning
                    0         // monsterSpawnBlockLightLimit - no spawning
            );
        }
    }

    /**
     * Creates monster settings for oceanic planets
     * Rich ecosystem with diverse spawning opportunities
     */
    private static DimensionType.MonsterSettings createOceanicMonsterSettings(OceanicModel oceanicModel) {
        float biodiversityIndex = oceanicModel.getBiodiversityIndex();
        boolean highBiodiversity = biodiversityIndex > 0.7f;

        return new DimensionType.MonsterSettings(
                false,    // piglinSafe
                true,     // hasRaids - oceanic worlds can support civilizations
                UniformIntProvider.create(0, 7),  // monsterSpawnLightLevel - normal spawning
                highBiodiversity ? 15 : 12        // monsterSpawnBlockLightLimit - enhanced spawning for high biodiversity
        );
    }

    /**
     * Creates default monster settings for basic planet types
     */
    private static DimensionType.MonsterSettings createDefaultMonsterSettings() {
        return new DimensionType.MonsterSettings(
                false,    // piglinSafe
                true,     // hasRaids
                UniformIntProvider.create(0, 7),  // monsterSpawnLightLevel
                15        // monsterSpawnBlockLightLimit
        );
    }

    /**
     * Creates monster settings for volcanic planets
     * Heat-adapted creatures, dangerous environment
     */
    private static DimensionType.MonsterSettings createVolcanicMonsterSettings() {
        return new DimensionType.MonsterSettings(
                true,     // piglinSafe - piglins might survive volcanic heat
                true,     // hasRaids
                UniformIntProvider.create(0, 7),  // monsterSpawnLightLevel
                15        // monsterSpawnBlockLightLimit - normal spawning but different mob types
        );
    }
}