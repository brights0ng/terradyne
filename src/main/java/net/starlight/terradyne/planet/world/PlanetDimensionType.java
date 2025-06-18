// PlanetDimensionType.java
package net.starlight.terradyne.planet.world;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.dimension.DimensionTypes;

import java.util.OptionalLong;

import net.starlight.terradyne.planet.physics.PlanetData;

import java.util.OptionalLong;

/**
 * Handles dimension type registration and configuration for planetary worlds.
 * Creates custom dimension types with atmosphere-based environmental effects.
 */
public class PlanetDimensionType {

    // Single dimension type that all planets will use
    public static final RegistryKey<DimensionType> PLANET_DIMENSION_TYPE =
            RegistryKey.of(RegistryKeys.DIMENSION_TYPE, new Identifier("terradyne", "planet"));

    /**
     * Create the base planet dimension type (used by data generation)
     */
    public static DimensionType createPlanetDimensionType() {
        return new DimensionType(
                OptionalLong.empty(), // fixed_time - empty means normal day/night cycle
                true, // has_skylight - planets have sky lighting
                false, // has_ceiling - no bedrock ceiling like nether
                false, // ultrawarm - not like nether
                true, // natural - allows water and lava
                1.0, // coordinate_scale - 1:1 coordinate mapping
                true, // bed_works - beds function normally
                false, // respawn_anchor_works - respawn anchors don't work (not nether)
                0, // min_y - will be overridden by chunk generator
                320, // height - will be overridden by chunk generator
                320, // logical_height - will be overridden by chunk generator
                BlockTags.INFINIBURN_OVERWORLD, // infiniburn - standard fire blocks
                DimensionTypes.OVERWORLD_ID, // effects - use overworld effects as base
                0.0f, // ambient_light - no ambient lighting
                new DimensionType.MonsterSettings(false, false, UniformIntProvider.create(0, 0), 0)
                // monster_settings - no special monster spawning for now
        );
    }

    /**
     * Create a dimension options for a specific planet (this will be used by DimensionManager)
     */
    public static DimensionOptions createPlanetDimensionOptions(
            net.minecraft.registry.DynamicRegistryManager registryManager,
            PlanetChunkGenerator chunkGenerator) {
        return new DimensionOptions(
                registryManager.get(RegistryKeys.DIMENSION_TYPE).entryOf(PLANET_DIMENSION_TYPE),
                chunkGenerator
        );
    }

    /**
     * Calculate sky color based on planet atmosphere
     */
    public static int calculateSkyColor(PlanetData planetData) {
        PlanetData.AtmosphereComposition atmosphere = planetData.getAtmosphereComposition();
        double density = planetData.getAdjustedAtmosphericDensity();
        double temperature = planetData.getAverageSurfaceTemp();

        // Base color calculation
        int baseColor = getBaseAtmosphereColor(atmosphere);

        // Apply atmospheric density effects
        baseColor = applyDensityEffects(baseColor, density);

        // Apply temperature effects
        baseColor = applyTemperatureEffects(baseColor, temperature);

        return baseColor;
    }

    /**
     * Get base color for atmosphere type
     */
    private static int getBaseAtmosphereColor(PlanetData.AtmosphereComposition atmosphere) {
        switch (atmosphere) {
            case OXYGEN_RICH:
                return 0x7FB8E1; // Earth-like blue

            case CARBON_DIOXIDE:
                return 0xE6B87D; // Mars-like butterscotch

            case METHANE:
                return 0xFFA366; // Titan-like orange

            case NITROGEN_RICH:
                return 0xB8C6DB; // Pale blue-gray

            case NOBLE_GAS_MIXTURE:
                return 0xE6E6FA; // Lavender

            case WATER_VAPOR_RICH:
                return 0xF0F8FF; // Very pale blue (steamy)

            case HYDROGEN_SULFIDE:
                return 0xFFD700; // Toxic yellow

            case TRACE_ATMOSPHERE:
                return 0x2F1B69; // Deep space purple

            case VACUUM:
                return 0x000000; // Black space

            default:
                return 0x7FB8E1; // Default to Earth-like
        }
    }

    /**
     * Apply atmospheric density effects to color
     */
    private static int applyDensityEffects(int baseColor, double density) {
        // Higher density = more saturated colors
        // Lower density = more washed out, toward black

        int r = (baseColor >> 16) & 0xFF;
        int g = (baseColor >> 8) & 0xFF;
        int b = baseColor & 0xFF;

        // Scale colors based on density (0.0 = black, 1.0 = full color)
        double densityFactor = Math.sqrt(density); // Square root for more gradual effect

        r = (int)(r * densityFactor);
        g = (int)(g * densityFactor);
        b = (int)(b * densityFactor);

        // Clamp values
        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        b = Math.max(0, Math.min(255, b));

        return (r << 16) | (g << 8) | b;
    }

    /**
     * Apply temperature effects to color
     */
    private static int applyTemperatureEffects(int baseColor, double temperature) {
        // Very hot = shift toward red
        // Very cold = shift toward blue

        int r = (baseColor >> 16) & 0xFF;
        int g = (baseColor >> 8) & 0xFF;
        int b = baseColor & 0xFF;

        if (temperature > 100) {
            // Hot worlds - increase red component
            double heatFactor = Math.min(1.0, (temperature - 100) / 400.0); // Max at 500°C
            r = Math.min(255, r + (int)(heatFactor * 50));
            g = Math.max(0, g - (int)(heatFactor * 20));
            b = Math.max(0, b - (int)(heatFactor * 30));
        } else if (temperature < -50) {
            // Cold worlds - increase blue component
            double coldFactor = Math.min(1.0, (-50 - temperature) / 150.0); // Max at -200°C
            r = Math.max(0, r - (int)(coldFactor * 30));
            g = Math.max(0, g - (int)(coldFactor * 20));
            b = Math.min(255, b + (int)(coldFactor * 40));
        }

        return (r << 16) | (g << 8) | b;
    }

    /**
     * Calculate fog color based on atmosphere (similar to sky color but different intensity)
     */
    public static int calculateFogColor(PlanetData planetData) {
        int skyColor = calculateSkyColor(planetData);

        // Make fog slightly darker and more muted than sky
        int r = ((skyColor >> 16) & 0xFF) * 3 / 4; // 75% intensity
        int g = ((skyColor >> 8) & 0xFF) * 3 / 4;
        int b = (skyColor & 0xFF) * 3 / 4;

        return (r << 16) | (g << 8) | b;
    }

    /**
     * Get dimension effects identifier for this planet
     */
    public static Identifier getDimensionEffectsId(String planetName) {
        return new Identifier("terradyne", "planet_" + planetName.toLowerCase().replace(" ", "_"));
    }

    /**
     * Check if planet should have special lighting effects
     */
    public static boolean shouldHaveCustomLighting(PlanetData planetData) {
        // Vacuum and trace atmospheres have very different lighting
        return planetData.getAtmosphereComposition() == PlanetData.AtmosphereComposition.VACUUM ||
                planetData.getAtmosphereComposition() == PlanetData.AtmosphereComposition.TRACE_ATMOSPHERE;
    }

    /**
     * Calculate ambient light level for planet (0.0 to 1.0)
     */
    public static float calculateAmbientLight(PlanetData planetData) {
        double density = planetData.getAdjustedAtmosphericDensity();

        switch (planetData.getAtmosphereComposition()) {
            case VACUUM:
                return 0.0f; // No atmosphere = no ambient light

            case TRACE_ATMOSPHERE:
                return 0.1f; // Very little ambient light

            case WATER_VAPOR_RICH:
                return 0.3f; // Steamy atmospheres scatter light

            default:
                // Standard atmospheric scattering based on density
                return (float)(0.15f * density); // 0 to 0.15 range
        }
    }

    /**
     * Get debug info about dimension effects
     */
    public static String getDimensionDebugInfo(PlanetData planetData) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== DIMENSION EFFECTS DEBUG ===\n");
        sb.append("Planet: ").append(planetData.getPlanetName()).append("\n");
        sb.append("Atmosphere: ").append(planetData.getAtmosphereComposition()).append("\n");
        sb.append("Density: ").append(String.format("%.2f", planetData.getAdjustedAtmosphericDensity())).append("\n");
        sb.append("Temperature: ").append(String.format("%.1f°C", planetData.getAverageSurfaceTemp())).append("\n");

        int skyColor = calculateSkyColor(planetData);
        sb.append("Sky Color: #").append(String.format("%06X", skyColor)).append("\n");

        int fogColor = calculateFogColor(planetData);
        sb.append("Fog Color: #").append(String.format("%06X", fogColor)).append("\n");

        float ambientLight = calculateAmbientLight(planetData);
        sb.append("Ambient Light: ").append(String.format("%.2f", ambientLight)).append("\n");

        sb.append("Custom Lighting: ").append(shouldHaveCustomLighting(planetData)).append("\n");

        return sb.toString();
    }
}