// MasterTerrainConfig.java
package net.starlight.terradyne.planet.terrain.config;

import net.starlight.terradyne.planet.physics.PlanetData;

/**
 * Master configuration for planetary terrain generation.
 * Translates planet physics parameters into concrete terrain generation settings,
 * coordinating all noise layers and height mapping for realistic world generation.
 */
public class MasterTerrainConfig {
    
    private final PlanetData planetData;
    private final NoiseLayerConfig[] noiseLayers;
    private final TerrainHeightSettings heightSettings;
    private final TerrainModifiers modifiers;
    private final long noiseSeed;
    
    private MasterTerrainConfig(PlanetData planetData, NoiseLayerConfig[] noiseLayers,
                               TerrainHeightSettings heightSettings, TerrainModifiers modifiers) {
        this.planetData = planetData;
        this.noiseLayers = noiseLayers;
        this.heightSettings = heightSettings;
        this.modifiers = modifiers;
        this.noiseSeed = planetData.getSeed();
    }
    
    // Getters
    public PlanetData getPlanetData() { return planetData; }
    public NoiseLayerConfig[] getNoiseLayers() { return noiseLayers; }
    public TerrainHeightSettings getHeightSettings() { return heightSettings; }
    public TerrainModifiers getModifiers() { return modifiers; }
    public long getNoiseSeed() { return noiseSeed; }
    
    /**
     * Create MasterTerrainConfig from PlanetData
     */
    public static MasterTerrainConfig fromPlanetData(PlanetData planetData) {
        // Create base layer configurations
        NoiseLayerConfig[] baseLayers = NoiseLayerConfig.DefaultConfigs.createDefaultLayerSet();
        
        // Adjust layers based on planet parameters
        NoiseLayerConfig[] adjustedLayers = new NoiseLayerConfig[baseLayers.length];
        for (int i = 0; i < baseLayers.length; i++) {
            adjustedLayers[i] = baseLayers[i].adjustForPlanet(
                planetData.getCircumference(),
                planetData.getCrustalThickness(),
                planetData.getAdjustedTectonicActivity(),
                planetData.getWaterErosion()
            );
        }
        
        // Apply planet-specific layer modifications
        adjustedLayers = applyPlanetTypeModifications(adjustedLayers, planetData);
        
        // Create height settings based on planet parameters
        TerrainHeightSettings heightSettings = createHeightSettings(planetData);
        
        // Create terrain modifiers
        TerrainModifiers modifiers = createTerrainModifiers(planetData);
        
        return new MasterTerrainConfig(planetData, adjustedLayers, heightSettings, modifiers);
    }
    
    /**
     * Apply planet-type specific modifications to layer configurations
     */
    private static NoiseLayerConfig[] applyPlanetTypeModifications(NoiseLayerConfig[] layers, PlanetData planetData) {
        NoiseLayerConfig[] modified = new NoiseLayerConfig[layers.length];
        
        for (int i = 0; i < layers.length; i++) {
            NoiseLayerConfig layer = layers[i];
            
            // Apply crust composition effects
            layer = applyCrustCompositionEffects(layer, planetData.getCrustComposition());
            
            // Apply age effects
            layer = applyPlanetAgeEffects(layer, planetData.getPlanetAge());
            
            // Apply atmosphere effects
            layer = applyAtmosphereEffects(layer, planetData.getAtmosphereComposition(), 
                                         planetData.getAdjustedAtmosphericDensity());
            
            modified[i] = layer;
        }
        
        return modified;
    }
    
    /**
     * Apply crust composition effects to layer configuration
     */
    private static NoiseLayerConfig applyCrustCompositionEffects(NoiseLayerConfig layer, 
                                                               PlanetData.CrustComposition crust) {
        NoiseLayerConfig.Builder builder = new NoiseLayerConfig.Builder(layer.getLayerType())
            .enabled(layer.isEnabled())
            .frequency(layer.getFrequency())
            .amplitude(layer.getAmplitude())
            .warpStrength(layer.getWarpStrength())
            .blendMode(layer.getBlendMode())
            .blendWeight(layer.getBlendWeight())
            .octaves(layer.getOctaves())
            .persistence(layer.getPersistence())
            .lacunarity(layer.getLacunarity());
        
        // Apply crust-specific modifications
        switch (crust) {
            case HADEAN:
                // Molten worlds: chaotic, high-amplitude terrain
                builder.amplitude(layer.getAmplitude() * 1.5)
                       .warpStrength(layer.getWarpStrength() * 1.8)
                       .shapeMode(NoiseLayerConfig.ShapeMode.RIDGED);
                break;
                
            case REGOLITH:
                // Weathered worlds: smooth, low-amplitude terrain
                builder.amplitude(layer.getAmplitude() * 0.6)
                       .warpStrength(layer.getWarpStrength() * 0.5)
                       .shapeMode(NoiseLayerConfig.ShapeMode.BILLOWY);
                break;
                
            case BASALT:
                // Volcanic worlds: sharp, ridged terrain
                if (layer.getLayerType() == NoiseLayerConfig.LayerType.MOUNTAIN_RANGES) {
                    builder.shapeMode(NoiseLayerConfig.ShapeMode.RIDGED)
                           .amplitude(layer.getAmplitude() * 1.2);
                }
                break;
                
            case METAL:
                // Metallic worlds: sharp, crystalline terrain
                builder.shapeMode(NoiseLayerConfig.ShapeMode.TERRACE)
                       .amplitude(layer.getAmplitude() * 0.8);
                break;
                
            case SILICATE:
            case FERROUS:
            case CARBON:
            case SULFUR:
            case HALIDE:
            default:
                // Use default shape mode for these compositions
                builder.shapeMode(layer.getShapeMode());
                break;
        }
        
        return builder.build();
    }
    
    /**
     * Apply planet age effects to layer configuration
     */
    private static NoiseLayerConfig applyPlanetAgeEffects(NoiseLayerConfig layer, PlanetData.PlanetAge age) {
        double ageModifier = 1.0;
        
        switch (age) {
            case INFANT:
                // Young planets: chaotic, rough terrain
                ageModifier = 1.4;
                break;
            case YOUNG:
                // Moderate roughness
                ageModifier = 1.1;
                break;
            case OLD:
                // Stable terrain
                ageModifier = 0.9;
                break;
            case DEAD:
                // Heavily eroded, smooth terrain
                ageModifier = 0.7;
                break;
        }
        
        return new NoiseLayerConfig.Builder(layer.getLayerType())
            .enabled(layer.isEnabled())
            .frequency(layer.getFrequency())
            .amplitude(layer.getAmplitude() * ageModifier)
            .warpStrength(layer.getWarpStrength() * ageModifier)
            .blendMode(layer.getBlendMode())
            .shapeMode(layer.getShapeMode())
            .blendWeight(layer.getBlendWeight())
            .octaves(layer.getOctaves())
            .persistence(layer.getPersistence())
            .lacunarity(layer.getLacunarity())
            .build();
    }
    
    /**
     * Apply atmospheric effects to layer configuration
     */
    private static NoiseLayerConfig applyAtmosphereEffects(NoiseLayerConfig layer, 
                                                         PlanetData.AtmosphereComposition atmosphere,
                                                         double density) {
        // Thick atmospheres can create wind erosion effects
        double windErosionFactor = density * 0.3;
        
        // Apply wind erosion primarily to surface detail
        if (layer.getLayerType() == NoiseLayerConfig.LayerType.SURFACE_DETAIL) {
            double smoothingFactor = 1.0 - windErosionFactor;
            
            return new NoiseLayerConfig.Builder(layer.getLayerType())
                .enabled(layer.isEnabled())
                .frequency(layer.getFrequency())
                .amplitude(layer.getAmplitude() * smoothingFactor)
                .warpStrength(layer.getWarpStrength() * smoothingFactor)
                .blendMode(layer.getBlendMode())
                .shapeMode(layer.getShapeMode())
                .blendWeight(layer.getBlendWeight())
                .octaves(layer.getOctaves())
                .persistence(layer.getPersistence())
                .lacunarity(layer.getLacunarity())
                .build();
        }
        
        return layer; // No change for other layers
    }
    
    /**
     * Create height settings based on planet parameters
     */
    private static TerrainHeightSettings createHeightSettings(PlanetData planetData) {
        // Base height calculation from crustal thickness
        int maxHeight = planetData.getCrustalThickness() / 10; // Convert km to blocks
        
        // Ensure reasonable bounds (Minecraft world height: 0-320)
        maxHeight = Math.max(50, Math.min(300, maxHeight));
        
        // Sea level calculation
        int seaLevel = (int)(maxHeight * 0.4); // 40% of max height
        
        // Adjust based on water content
        double waterContent = planetData.getAdjustedWaterContent();
        if (waterContent > 0.8) {
            // Ocean worlds: higher sea level
            seaLevel = (int)(maxHeight * 0.6);
        } else if (waterContent < 0.2) {
            // Dry worlds: lower sea level
            seaLevel = (int)(maxHeight * 0.2);
        }
        
        return new TerrainHeightSettings(maxHeight, seaLevel, 5); // 5 block min height
    }
    
    /**
     * Create terrain modifiers based on planet parameters
     */
    private static TerrainModifiers createTerrainModifiers(PlanetData planetData) {
        // Calculate scale factors
        double planetSizeScale = planetData.getCircumference() / 40000.0; // Relative to Earth
        double tectonicScale = 0.5 + (planetData.getAdjustedTectonicActivity() * 0.5);
        double erosionScale = 1.0 - (planetData.getWaterErosion() * 0.4);
        
        // Coordinate scaling for consistent features across planet sizes
        double coordinateScale = Math.sqrt(planetSizeScale); // Square root scaling for better proportion
        
        return new TerrainModifiers(coordinateScale, tectonicScale, erosionScale);
    }
    
    /**
     * Get enabled layers only
     */
    public NoiseLayerConfig[] getEnabledLayers() {
        return java.util.Arrays.stream(noiseLayers)
            .filter(NoiseLayerConfig::isEnabled)
            .toArray(NoiseLayerConfig[]::new);
    }
    
    /**
     * Get layer by type
     */
    public NoiseLayerConfig getLayer(NoiseLayerConfig.LayerType type) {
        for (NoiseLayerConfig layer : noiseLayers) {
            if (layer.getLayerType() == type) {
                return layer;
            }
        }
        return null;
    }
    
    /**
     * Debug information
     */
    public String getDebugInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== TERRAIN CONFIG DEBUG ===\n");
        sb.append("Planet: ").append(planetData.getPlanetName()).append("\n");
        sb.append("Circumference: ").append(planetData.getCircumference()).append(" km\n");
        sb.append("Crustal Thickness: ").append(planetData.getCrustalThickness()).append(" km\n");
        sb.append("Height Range: ").append(heightSettings.minHeight).append("-").append(heightSettings.maxHeight).append("\n");
        sb.append("Sea Level: ").append(heightSettings.seaLevel).append("\n");
        sb.append("Coordinate Scale: ").append(String.format("%.3f", modifiers.coordinateScale)).append("\n");
        sb.append("Tectonic Scale: ").append(String.format("%.3f", modifiers.tectonicScale)).append("\n");
        sb.append("Erosion Scale: ").append(String.format("%.3f", modifiers.erosionScale)).append("\n");
        sb.append("\nLayers:\n");
        for (NoiseLayerConfig layer : noiseLayers) {
            sb.append("  ").append(layer.toString()).append("\n");
        }
        return sb.toString();
    }
    
    /**
     * Terrain height settings
     */
    public static class TerrainHeightSettings {
        public final int maxHeight;
        public final int seaLevel;
        public final int minHeight;
        
        public TerrainHeightSettings(int maxHeight, int seaLevel, int minHeight) {
            this.maxHeight = maxHeight;
            this.seaLevel = seaLevel;
            this.minHeight = minHeight;
        }
        
        /**
         * Get total height range
         */
        public int getHeightRange() {
            return maxHeight - minHeight;
        }
        
        /**
         * Convert noise value (-1 to 1) to world height
         */
        public int noiseToWorldHeight(double noiseValue) {
            // Linear mapping from -1..1 to minHeight..maxHeight
            return (int)(minHeight + ((noiseValue + 1.0) * 0.5 * getHeightRange()));
        }
        
        /**
         * Check if height is below sea level
         */
        public boolean isBelowSeaLevel(int height) {
            return height < seaLevel;
        }
    }
    
    /**
     * Terrain modifier values
     */
    public static class TerrainModifiers {
        public final double coordinateScale;
        public final double tectonicScale;
        public final double erosionScale;
        
        public TerrainModifiers(double coordinateScale, double tectonicScale, double erosionScale) {
            this.coordinateScale = coordinateScale;
            this.tectonicScale = tectonicScale;
            this.erosionScale = erosionScale;
        }
    }
}