package net.terradyne.planet.terrain;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.noise.SimplexNoiseSampler;
import net.minecraft.util.math.random.Random;
import net.terradyne.planet.PlanetType;
import net.terradyne.planet.biome.IBiomeType;
import net.terradyne.planet.config.IPlanetConfig;
import net.terradyne.planet.model.IPlanetModel;

import java.util.Set;
public class DuneGenerator implements ITerrainGenerator {

    // Noise samplers - initialized per planet
    private final SimplexNoiseSampler baseTerrainNoise;
    private final SimplexNoiseSampler detailTerrainNoise;
    private final SimplexNoiseSampler largeDuneNoise;
    private final SimplexNoiseSampler mediumDuneNoise;
    private final SimplexNoiseSampler smallDuneNoise;
    private final SimplexNoiseSampler windNoise;
    private final SimplexNoiseSampler erosionNoise;

    public DuneGenerator(long seed) {
        Random random = Random.create(seed);
        this.baseTerrainNoise = new SimplexNoiseSampler(random);
        this.detailTerrainNoise = new SimplexNoiseSampler(random);
        this.largeDuneNoise = new SimplexNoiseSampler(random);
        this.mediumDuneNoise = new SimplexNoiseSampler(random);
        this.smallDuneNoise = new SimplexNoiseSampler(random);
        this.windNoise = new SimplexNoiseSampler(random);
        this.erosionNoise = new SimplexNoiseSampler(random);
    }

    // Reuse a single TerrainData instance to avoid allocations
    private final TerrainData workingTerrain = new TerrainData(0, 0,
            Blocks.STONE.getDefaultState(), Blocks.STONE.getDefaultState());

        @Override
        public TerrainData generateTerrain(int x, int z, TerrainContext context) {
            // Get noise samplers from context
            SimplexNoiseSampler baseNoise = context.getNoiseSampler("base");
            SimplexNoiseSampler largeDuneNoise = context.getNoiseSampler("large_dune");
        IPlanetModel model = context.getPlanetModel();
        IPlanetConfig config = model.getConfig();

        // Generate heights
        int baseHeight = generateBaseHeight(x, z, context);
        int duneContribution = 0;

        // Only add dune contribution if planet supports it
        if (model.hasLooseMaterialFormations()) {
            duneContribution = generateDuneContribution(x, z, context);
        }

        int surfaceHeight = baseHeight + duneContribution;

        // Get appropriate material
        BlockState material = getLooseMaterialBlock(config.getLooseMaterialType(), context);

        // Update the working terrain (reuse instance)
        workingTerrain.baseHeight = baseHeight;
        workingTerrain.surfaceHeight = surfaceHeight;
        workingTerrain.primaryBlock = material;
        workingTerrain.surfaceBlock = material;

        return workingTerrain;
    }

    private TerrainData generateBaseTerrain(int x, int z, TerrainContext context) {
        // Simple terrain for planets without loose material formations
        int baseHeight = generateBaseHeight(x, z, context);

        // Add small variations even without dunes
        double simpleVariation = detailTerrainNoise.sample(x * 0.004, 0, z * 0.004) *
                context.getPlanetModel().getConfig().getLooseMaterialDensity() * 6;

        double windDrift = windNoise.sample(x * 0.008, 0, z * 0.008) *
                context.getPlanetModel().getConfig().getWindStrength() * 3;

        int finalHeight = baseHeight + (int)Math.max(0, simpleVariation + windDrift);

        BlockState surfaceBlock = getSurfaceBlock(context);

        return new TerrainData(baseHeight, finalHeight, surfaceBlock, surfaceBlock);
    }

    private int generateBaseHeight(int x, int z, TerrainContext context) {
        IPlanetModel model = context.getPlanetModel();
        IPlanetConfig config = model.getConfig();

        // Large-scale terrain features
        double baseNoise = baseTerrainNoise.sample(x * 0.003, 0, z * 0.003) * 15;
        double detailNoise = detailTerrainNoise.sample(x * 0.012, 0, z * 0.012) * 6;
        double fineNoise = detailTerrainNoise.sample(x * 0.045, 0, z * 0.045) * 2;

        // Planet type affects terrain roughness
        double roughnessFactor = switch (model.getType()) {
            case VOLCANIC -> 1.8;
            case ROCKY -> 1.4;
            case ICY -> 0.9;
            case IRON -> 0.0;
            case DESERT -> 1.2;
            case OCEANIC -> 0.5;
            case HOTHOUSE -> 1.2;
            case SUBSURFACE_OCEANIC -> 0.5;
            case CARBON -> 1.0;
        };

        double totalNoise = (baseNoise + detailNoise + fineNoise) * roughnessFactor;

        // Erosion smooths terrain
        double erosionFactor = 1.0 - (model.getErosionRate() * 0.25);
        totalNoise *= erosionFactor;

        // Base height varies by planet type
        int planetBaseHeight = switch (model.getType()) {
            case OCEANIC -> 40;      // Lower for water
            case VOLCANIC -> 70;   // Higher for volcanic activity
            case ICY -> 60;        // Medium height
            case ROCKY -> 65;      // Medium-high
            default -> 55;         // Standard
        };

        return (int) (planetBaseHeight + totalNoise);
    }

    private int generateDuneContribution(int x, int z, TerrainContext context) {
        IPlanetModel model = context.getPlanetModel();
        IPlanetConfig config = model.getConfig();

        float maxFormationHeight = Math.max(model.getLooseMaterialFormationHeight(), 60.0f);

        // === EXISTING DUNE ALGORITHM (as shown before) ===
        // 1. Large sweeping formations
        double majorFormations = (largeDuneNoise.sample(x * 0.00016, 0, z * 0.0008) + 1.0) * 0.5 *
                maxFormationHeight * 0.35;

        // 2. Wave-like patterns
        double wave1 = (mediumDuneNoise.sample(x * 0.00028, 0, z * 0.0014) + 1.0) * 0.5 *
                maxFormationHeight * 0.22;
        double wave2 = (smallDuneNoise.sample(x * 0.00048, 0, z * 0.0024) + 1.0) * 0.5 *
                maxFormationHeight * 0.18;

        // 3. Wind-aligned formations
        double windStrength = config.getWindStrength();
        double windAngle = windNoise.sample(x * 0.00006, 0, z * 0.0003) * Math.PI;
        double windAlignedX = x * Math.cos(windAngle) - z * Math.sin(windAngle);
        double windAlignedZ = x * Math.sin(windAngle) + z * Math.cos(windAngle);

        double windRidges = (erosionNoise.sample(windAlignedX * 0.00012, 0, windAlignedZ * 0.0006) + 1.0) * 0.5 *
                windStrength * maxFormationHeight * 0.2;

        // 4. Medium-scale variations
        double mediumDetail1 = (windNoise.sample(x * 0.0012, 0, z * 0.006) + 1.0) * 0.5 *
                maxFormationHeight * 0.12;
        double mediumDetail2 = (erosionNoise.sample(x * 0.0008, 0, z * 0.004) + 1.0) * 0.5 *
                maxFormationHeight * 0.08;

        // 5. Fine ripples (multiple octaves)
        double ripples1 = (detailTerrainNoise.sample(x * 0.0032, 0, z * 0.016) + 1.0) * 0.5 *
                maxFormationHeight * 0.08;
        double ripples2 = (baseTerrainNoise.sample(x * 0.0048, 0, z * 0.024) + 1.0) * 0.5 *
                maxFormationHeight * 0.06;
        double ripples3 = (largeDuneNoise.sample(x * 0.0064, 0, z * 0.032) + 1.0) * 0.5 *
                maxFormationHeight * 0.04;

        // 6. Micro-variations
        double microTexture1 = (mediumDuneNoise.sample(x * 0.008, 0, z * 0.04) + 1.0) * 0.5 *
                maxFormationHeight * 0.05;
        double microTexture2 = (smallDuneNoise.sample(x * 0.012, 0, z * 0.06) + 1.0) * 0.5 *
                maxFormationHeight * 0.03;

        // 7. Erosion patterns
        double erosionBreakup = (erosionNoise.sample(x * 0.002, 0, z * 0.01) + 1.0) * 0.5 *
                model.getErosionRate() * maxFormationHeight * 0.1;

        // 8. Wind chaos
        double windChaos = (windNoise.sample(x * 0.004, 0, z * 0.02) + 1.0) * 0.5 *
                config.getWindStrength() * maxFormationHeight * 0.07;

        // 9. Base undulation
        double baseUndulation = (baseTerrainNoise.sample(x * 0.001, 0, z * 0.005) + 1.0) * 0.5 *
                maxFormationHeight * 0.1;

        // Combine layers
        double primaryLayer = majorFormations + Math.max(wave1, wave2) + windRidges;
        double detailLayer = mediumDetail1 + mediumDetail2 + erosionBreakup + windChaos;
        double fineLayer = ripples1 + ripples2 + ripples3 + microTexture1 + microTexture2;
        double baseLayer = baseUndulation;

        double totalContribution = primaryLayer +
                (detailLayer * 0.8) +
                (fineLayer * 0.6) +
                baseLayer;

        // Ensure minimum variation
        double minContribution = config.getLooseMaterialDensity() * 12;

        return (int) Math.max(minContribution, totalContribution);
    }

    private BlockState getLooseMaterialBlock(IPlanetConfig.LooseMaterialType type, TerrainContext context) {
        return switch (type) {
            case SAND -> {
                // Hot = red sand, cold = normal sand
                yield context.getPlanetModel().getConfig().getSurfaceTemperature() > 45 ?
                        Blocks.RED_SAND.getDefaultState() : Blocks.SAND.getDefaultState();
            }
            case SNOW -> Blocks.SNOW_BLOCK.getDefaultState();
            case VOLCANIC_ASH -> Blocks.GRAY_CONCRETE_POWDER.getDefaultState();
            case DUST -> Blocks.BROWN_CONCRETE_POWDER.getDefaultState();
            case REGOLITH -> Blocks.GRAVEL.getDefaultState();
            case ORGANIC_MATTER -> Blocks.COARSE_DIRT.getDefaultState();
        };
    }

    private BlockState getSurfaceBlock(TerrainContext context) {
        IPlanetModel model = context.getPlanetModel();

        // If planet has loose material, use that
        if (model.getConfig().getLooseMaterialDensity() > 0.3f) {
            return getLooseMaterialBlock(model.getConfig().getLooseMaterialType(), context);
        }

        // Otherwise use solid surface based on planet type
        return switch (model.getType()) {
            case VOLCANIC -> Blocks.BASALT.getDefaultState();
            case ROCKY -> Blocks.COBBLESTONE.getDefaultState();
            case ICY -> Blocks.ICE.getDefaultState();
            case IRON -> Blocks.DEEPSLATE.getDefaultState();
            case DESERT -> Blocks.SANDSTONE.getDefaultState();
            case OCEANIC -> Blocks.STONE.getDefaultState();
            case HOTHOUSE -> Blocks.RED_SANDSTONE.getDefaultState();
            case SUBSURFACE_OCEANIC -> Blocks.SAND.getDefaultState();
            case CARBON -> Blocks.SMOOTH_BASALT.getDefaultState();
        };
    }

    @Override
    public Set<PlanetType> getSupportedPlanetTypes() {
        return Set.of(PlanetType.values()); // Works on ALL planet types!
    }

    @Override
    public boolean appliesToBiome(IBiomeType biome) {
        // This generator can work on any biome - it checks planet config internally
        return true;
    }

    @Override
    public int getPriority() {
        return 100; // Base terrain priority
    }
}