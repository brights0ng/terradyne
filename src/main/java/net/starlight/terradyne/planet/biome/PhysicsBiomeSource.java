package net.starlight.terradyne.planet.biome;

import com.mojang.serialization.Codec;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.starlight.terradyne.planet.physics.PlanetPhysicsConfig;
import net.starlight.terradyne.planet.physics.PlanetPhysicsModel;

import java.util.stream.Stream;

/**
 * Biome source for physics-based planets
 * For now, returns a single test biome everywhere
 * Later will determine biomes from physical conditions
 */
public class PhysicsBiomeSource extends BiomeSource {
    public static final Codec<PhysicsBiomeSource> CODEC = Codec.unit(PhysicsBiomeSource::new);
    
    private final PlanetPhysicsModel model;
    private final RegistryEntry<Biome> testBiome;
    
    // Default constructor for codec
    public PhysicsBiomeSource() {
        this.model = null;
        this.testBiome = null;
    }
    
    public PhysicsBiomeSource(PlanetPhysicsModel model, MinecraftServer server) {
        this.model = model;
        
        // For now, just use plains biome everywhere
        // Later, biomes will emerge from physical conditions
        Registry<Biome> biomeRegistry = server.getRegistryManager().get(RegistryKeys.BIOME);
        this.testBiome = biomeRegistry.getEntry(BiomeKeys.PLAINS).orElseThrow();
    }
    
    @Override
    protected Codec<? extends BiomeSource> getCodec() {
        return CODEC;
    }
    
    @Override
    protected Stream<RegistryEntry<Biome>> biomeStream() {
        if (testBiome == null) return Stream.empty();
        return Stream.of(testBiome);
    }
    
    @Override
    public RegistryEntry<Biome> getBiome(int x, int y, int z, MultiNoiseUtil.MultiNoiseSampler sampler) {
        if (testBiome == null) return null;
        
        // For now, return the same biome everywhere
        // Later, this will check:
        // - Temperature at this location
        // - Water availability
        // - Wind speed
        // - Elevation
        // - Substrate type (from plate geology)
        // And assign appropriate biome
        
        return testBiome;
    }
    
    /**
     * Get the biome type at world coordinates
     * For compatibility with existing chunk generator
     */
//    public IBiomeType getBiomeTypeAt(int worldX, int worldZ) {
//        // Check if this is a debug planet
//        if (model != null && model.getConfig() instanceof PlanetPhysicsConfig physicsConfig) {
//            // If tectonic activity is 0 and water height is 0, assume debug mode
//            if (physicsConfig.getTectonicActivity() == 0.0f && physicsConfig.getWaterHeight() == 0.0f) {
//                return PhysicsBiomeType.DEBUG_PLATES;
//            }
//        }
//
//        // For now, return our test biome type everywhere
//        return PhysicsBiomeType.TECTONIC_TEST;
//    }
}