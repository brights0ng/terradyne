package net.starlight.terradyne.planet.dimension;

import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.dimension.DimensionTypes;
import net.starlight.terradyne.planet.physics.AtmosphereComposition;
import net.starlight.terradyne.planet.physics.CrustComposition;
import net.starlight.terradyne.planet.physics.PlanetModel;

import java.util.OptionalLong;

/**
 * Factory class for creating custom dimension types based on planet characteristics
 */
public class DimensionTypeFactory {

    // Base properties for all Terradyne dimensions
    private static final boolean NATURAL = true;
    private static final boolean BED_WORKS = true;
    private static final boolean PIGLIN_SAFE = true;
    private static final boolean RESPAWN_ANCHOR_WORKS = false;
    private static final boolean HAS_RAIDS = true;
    private static final int MIN_Y = 0;
    private static final int HEIGHT = 256;
    private static final int LOGICAL_HEIGHT = 256;

    /**
     * Create TERRADYNE_HABITABLE dimension type
     * For oxygen-rich, habitable planets with good living conditions
     */
    public static DimensionType createHabitableDimension() {
        return new DimensionType(
                OptionalLong.empty(),           // fixed_time - normal day/night cycle
                true,                           // has_skylight - clear sky
                false,                          // has_ceiling - no ceiling
                false,                          // ultrawarm - normal temperature
                NATURAL,
                1.0,                           // coordinate_scale - normal
                BED_WORKS,
                RESPAWN_ANCHOR_WORKS,
                MIN_Y,
                HEIGHT,
                LOGICAL_HEIGHT,
                BlockTags.INFINIBURN_OVERWORLD, // infiniburn
                DimensionTypes.OVERWORLD_ID,     // effects - overworld-like
                0.0f,                          // ambient_light - normal lighting
                new DimensionType.MonsterSettings(
                        PIGLIN_SAFE,
                        HAS_RAIDS,
                        UniformIntProvider.create(0, 7), // monster_spawn_light_level
                        0                                // monster_spawn_block_light_limit
                )
        );
    }

    /**
     * Create TERRADYNE_ULTRAWARM dimension type  
     * For hadean and extremely hot planets
     */
    public static DimensionType createUltrawarmDimension() {
        return new DimensionType(
                OptionalLong.empty(),           // fixed_time - normal day/night cycle
                true,                           // has_skylight - can see sky through heat haze
                false,                          // has_ceiling - no ceiling
                true,                           // ultrawarm - lava doesn't freeze, water evaporates
                NATURAL,
                1.0,                           // coordinate_scale - normal
                BED_WORKS,
                RESPAWN_ANCHOR_WORKS,
                MIN_Y,
                HEIGHT,
                LOGICAL_HEIGHT,
                BlockTags.INFINIBURN_NETHER,   // infiniburn - like nether
                DimensionTypes.OVERWORLD_ID,     // effects - overworld-like but hot
                0.1f,                          // ambient_light - slight heat glow
                new DimensionType.MonsterSettings(
                        PIGLIN_SAFE,
                        HAS_RAIDS,
                        UniformIntProvider.create(0, 7), // monster_spawn_light_level
                        0                                // monster_spawn_block_light_limit
                )
        );
    }

    /**
     * Create TERRADYNE_THICK_ATMOSPHERE dimension type
     * For planets with dense greenhouse atmospheres (Venus-like)
     */
    public static DimensionType createThickAtmosphereDimension() {
        return new DimensionType(
                OptionalLong.empty(),           // fixed_time - normal day/night cycle
                false,                          // has_skylight - thick atmosphere blocks sky
                true,                           // has_ceiling - atmosphere acts like ceiling
                false,                          // ultrawarm - heat from greenhouse, not ultrawarm
                NATURAL,
                1.0,                           // coordinate_scale - normal
                BED_WORKS,
                RESPAWN_ANCHOR_WORKS,
                MIN_Y,
                HEIGHT,
                LOGICAL_HEIGHT,
                BlockTags.INFINIBURN_OVERWORLD, // infiniburn
                DimensionTypes.THE_NETHER_ID,        // effects - nether-like for thick atmosphere
                0.3f,                          // ambient_light - greenhouse glow
                new DimensionType.MonsterSettings(
                        PIGLIN_SAFE,
                        HAS_RAIDS,
                        UniformIntProvider.create(0, 7), // monster_spawn_light_level
                        0                                // monster_spawn_block_light_limit
                )
        );
    }

    /**
     * Create TERRADYNE_STANDARD dimension type
     * For vacuum, thin atmosphere, or non-breathable atmosphere planets
     */
    public static DimensionType createStandardDimension() {
        return new DimensionType(
                OptionalLong.empty(),           // fixed_time - normal day/night cycle
                true,                           // has_skylight - clear space view
                false,                          // has_ceiling - no ceiling
                false,                          // ultrawarm - normal temperature range
                NATURAL,
                1.0,                           // coordinate_scale - normal
                BED_WORKS,
                RESPAWN_ANCHOR_WORKS,
                MIN_Y,
                HEIGHT,
                LOGICAL_HEIGHT,
                BlockTags.INFINIBURN_OVERWORLD, // infiniburn
                DimensionTypes.THE_END_ID,           // effects - end-like for airless feel
                0.0f,                          // ambient_light - normal lighting
                new DimensionType.MonsterSettings(
                        PIGLIN_SAFE,
                        HAS_RAIDS,
                        UniformIntProvider.create(0, 7), // monster_spawn_light_level
                        0                                // monster_spawn_block_light_limit
                )
        );
    }

    /**
     * Create TERRADYNE_TOXIC dimension type
     * For planets with hostile, toxic atmospheres
     */
    public static DimensionType createToxicDimension() {
        return new DimensionType(
                OptionalLong.empty(),           // fixed_time - normal day/night cycle
                true,                           // has_skylight - can see through toxic haze
                false,                          // has_ceiling - no ceiling
                false,                          // ultrawarm - normal temperature
                NATURAL,
                1.0,                           // coordinate_scale - normal
                BED_WORKS,
                RESPAWN_ANCHOR_WORKS,
                MIN_Y,
                HEIGHT,
                LOGICAL_HEIGHT,
                BlockTags.INFINIBURN_OVERWORLD, // infiniburn
                DimensionTypes.THE_NETHER_ID,        // effects - nether-like for hostile feel
                0.05f,                         // ambient_light - slight toxic glow
                new DimensionType.MonsterSettings(
                        PIGLIN_SAFE,
                        HAS_RAIDS,
                        UniformIntProvider.create(0, 7), // monster_spawn_light_level
                        0                                // monster_spawn_block_light_limit
                )
        );
    }

    /**
     * Select appropriate dimension type based on planet characteristics
     */
    public static ModDimensionTypes.TerradyneDimensionType selectDimensionType(PlanetModel planetModel) {
        double temperature = planetModel.getPlanetData().getAverageSurfaceTemp();
        double atmosphericDensity = planetModel.getPlanetData().getActualAtmosphericDensity();
        AtmosphereComposition atmosphere = planetModel.getConfig().getAtmosphereComposition();
        CrustComposition crust = planetModel.getConfig().getCrustComposition();
        double habitability = planetModel.getPlanetData().getHabitability();

        // ULTRAWARM: Hadean crust OR extremely hot planets
        if (crust == CrustComposition.HADEAN || temperature > 100.0) {
            return ModDimensionTypes.TerradyneDimensionType.ULTRAWARM;
        }

        // THICK_ATMOSPHERE: Dense greenhouse atmospheres that block skylight
        if (atmosphericDensity > 0.8 && 
            (atmosphere == AtmosphereComposition.CARBON_DIOXIDE || 
             atmosphere == AtmosphereComposition.WATER_VAPOR_RICH)) {
            return ModDimensionTypes.TerradyneDimensionType.THICK_ATMOSPHERE;
        }

        // TOXIC: Hostile atmosphere compositions
        if (atmosphere == AtmosphereComposition.HYDROGEN_SULFIDE) {
            return ModDimensionTypes.TerradyneDimensionType.TOXIC;
        }

        // HABITABLE: Oxygen-rich atmosphere with good habitability
        if (atmosphere == AtmosphereComposition.OXYGEN_RICH && 
            habitability > 0.5 && 
            atmosphericDensity > 0.3) {
            return ModDimensionTypes.TerradyneDimensionType.HABITABLE;
        }

        // STANDARD: Everything else (vacuum, thin atmosphere, noble gases, etc.)
        return ModDimensionTypes.TerradyneDimensionType.STANDARD;
    }

    /**
     * Get dimension type description for logging
     */
    public static String getDimensionTypeDescription(ModDimensionTypes.TerradyneDimensionType type) {
        return switch (type) {
            case HABITABLE -> "Habitable (oxygen-rich, good living conditions)";
            case ULTRAWARM -> "Ultrawarm (hadean/extremely hot planets)";
            case THICK_ATMOSPHERE -> "Thick Atmosphere (dense greenhouse effect)";
            case STANDARD -> "Standard (vacuum/thin/non-breathable atmosphere)";
            case TOXIC -> "Toxic (hostile atmosphere composition)";
        };
    }
}