package net.terradyne.planet.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.mixin.registry.sync.RegistriesAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.noise.SimplexNoiseSampler;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.terradyne.planet.model.DesertModel;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class DesertBiomeSource extends BiomeSource {
    public static final Codec<DesertBiomeSource> CODEC = Codec.unit(DesertBiomeSource::new);

    private DesertModel desertModel;
    private DesertBiomeCalculator.BiomeWeights biomeWeights;
    private SimplexNoiseSampler biomeNoise;

    public DesertBiomeSource() {}

    public DesertBiomeSource(DesertModel desertModel) {
        this.desertModel = desertModel;
        this.biomeWeights = DesertBiomeCalculator.calculateBiomeWeights(desertModel);
        this.biomeNoise = new SimplexNoiseSampler(Random.create(desertModel.getConfig().getSeed()));
    }

    @Override
    protected Codec<? extends BiomeSource> getCodec() {
        return CODEC;
    }

    @Override
    protected Stream<RegistryEntry<Biome>> biomeStream() {
        return Stream.empty();
    }

    private Map<DesertBiomeType, RegistryKey<Biome>> createBiomeKeyMap() {
        Map<DesertBiomeType, RegistryKey<Biome>> map = new HashMap<>();

        map.put(DesertBiomeType.DUNE_SEA, ModBiomes.DUNE_SEA);
        map.put(DesertBiomeType.SCORCHING_WASTE, ModBiomes.SCORCHING_WASTE);
        map.put(DesertBiomeType.GRANITE_MESAS, ModBiomes.GRANITE_MESAS);
        // ... add others as you create them

        return map;
    }



    @Override
    public RegistryEntry<Biome> getBiome(int x, int y, int z, MultiNoiseUtil.MultiNoiseSampler sampler) {
        // For now, let's return null and handle this in the chunk generator
        // The terrain generation will work based on getBiomeTypeAt()
        return null;
    }

    public IBiomeType getBiomeTypeAt(int worldX, int worldZ) {
        if (biomeWeights == null) {
            return DesertBiomeType.DUNE_SEA;
        }
        return biomeWeights.selectBiomeAt(worldX, worldZ, biomeNoise);
    }

    public DesertModel getDesertModel() { return desertModel; }
    public DesertBiomeCalculator.BiomeWeights getBiomeWeights() { return biomeWeights; }
}