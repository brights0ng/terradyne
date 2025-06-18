// PlanetBiomeSource.java
package net.starlight.terradyne.planet.world.biome;

import com.mojang.serialization.Codec;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;

import net.starlight.terradyne.planet.physics.PlanetData;

import java.util.stream.Stream;

/**
 * Simple biome source for planetary worlds.
 * For Stage 0.3, this is a placeholder that provides minimal biome functionality.
 * Will be expanded in later phases for realistic biome distribution.
 */
public class PlanetBiomeSource extends BiomeSource {

    // Simple codec for basic save/load functionality
    public static final Codec<PlanetBiomeSource> CODEC = Codec.unit(() -> {
        // This is a minimal codec implementation for Stage 0.3
        // In practice, we'd need to serialize the PlanetData
        // For now, return a dummy instance to prevent crashes
        System.err.println("Warning: PlanetBiomeSource codec used - this is a placeholder");
        return null; // This will need proper implementation for save/load
    });

    private final PlanetData planetData;

    // For Stage 0.3, we'll use a simple approach with existing biomes
    public PlanetBiomeSource(PlanetData planetData) {
        this.planetData = planetData;
    }

    @Override
    protected Codec<? extends BiomeSource> getCodec() {
        return CODEC; // TODO: Implement for save/load functionality
    }

    @Override
    protected Stream<RegistryEntry<Biome>> biomeStream() {
        // For now, just return an empty stream
        // This will be expanded when we implement proper biome registration
        return Stream.empty();
    }

    @Override
    public RegistryEntry<Biome> getBiome(int x, int y, int z, MultiNoiseUtil.MultiNoiseSampler noise) {
        // For Stage 0.3, this is a placeholder
        // We'll return null for now and handle this in dimension creation
        // Later phases will implement proper biome distribution based on:
        // - Temperature (latitude effects)
        // - Moisture (precipitation patterns)  
        // - Elevation (altitude effects)
        // - Planetary conditions

        return null; // TODO: Return appropriate biome based on planet conditions
    }

    /**
     * Get the planet data this biome source is using
     */
    public PlanetData getPlanetData() {
        return planetData;
    }

    /**
     * Get suggested biome type for this planet (for debugging)
     */
    public String getSuggestedBiomeType() {
        switch (planetData.getCrustComposition()) {
            case HADEAN:
                return "nether_wastes"; // Hot, hellish

            case REGOLITH:
                return "desert"; // Dry, dusty

            case BASALT:
                return "basalt_deltas"; // Volcanic

            case FERROUS:
                return "badlands"; // Rusty, iron-rich

            case SILICATE:
            default:
                // Check habitability for Earth-like worlds
                if (planetData.getHabitability() > 0.5) {
                    return "plains"; // Habitable
                } else if (planetData.getHabitability() > 0.2) {
                    return "savanna"; // Marginal
                } else {
                    return "desert"; // Hostile
                }
        }
    }

    /**
     * Debug information about biome selection
     */
    public String getDebugInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== PLANET BIOME SOURCE DEBUG ===\n");
        sb.append("Planet: ").append(planetData.getPlanetName()).append("\n");
        sb.append("Crust: ").append(planetData.getCrustComposition()).append("\n");
        sb.append("Habitability: ").append(String.format("%.2f", planetData.getHabitability())).append("\n");
        sb.append("Temperature: ").append(String.format("%.1f°C", planetData.getAverageSurfaceTemp())).append("\n");
        sb.append("Suggested Biome: ").append(getSuggestedBiomeType()).append("\n");

        return sb.toString();
    }
}