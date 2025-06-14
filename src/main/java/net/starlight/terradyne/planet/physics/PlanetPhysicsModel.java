package net.starlight.terradyne.planet.physics;

import net.starlight.terradyne.planet.PlanetType;
import net.starlight.terradyne.planet.physics.TectonicPlateGenerator.TectonicPlate;
import net.starlight.terradyne.planet.physics.TectonicPlateGenerator.PlateBoundaryInfo;

/**
 * Physics-based planet model
 * Caches tectonic plates and provides physics calculations
 */
public class PlanetPhysicsModel implements IPlanetModel {
    private final PlanetPhysicsConfig config;
    private final TectonicPlateGenerator tectonicGenerator;
    
    // Calculated planet properties
    private final float gravity;
    private final float atmosphericPressure;
    
    public PlanetPhysicsModel(PlanetPhysicsConfig config) {
        this.config = config;
        
        // Generate and cache tectonic plates
        this.tectonicGenerator = new TectonicPlateGenerator(
            config.getSeed(), 
            config.getTectonicScale()
        );
        
        // Calculate properties
        this.gravity = config.getGravity();
        this.atmosphericPressure = calculateAtmosphericPressure();
    }
    
    private float calculateAtmosphericPressure() {
        // Atmospheric pressure in Earth atmospheres
        return config.getAtmosphereDensity();
    }
    
    /**
     * Get base terrain height at a position
     * For now, just returns plate elevation (flat world with plate height variation)
     */
    public float getBaseTerrainHeight(int worldX, int worldZ) {
        // Get the plate at this position
        TectonicPlate plate = tectonicGenerator.getPlateAt(worldX, worldZ);
        
        // For now, just return sea level + plate elevation
        // This creates a flat world where each plate has a different height
        return 64.0f + plate.getBaseElevation();
    }
    
    /**
     * Get volatility at a position (for future mountain/valley generation)
     */
    public float getVolatilityAt(int worldX, int worldZ) {
        PlateBoundaryInfo boundaryInfo = tectonicGenerator.getBoundaryInfoAt(worldX, worldZ);
        return boundaryInfo.volatility;
    }
    
    /**
     * Get the tectonic plate at a position
     */
    public TectonicPlate getTectonicPlateAt(int worldX, int worldZ) {
        return tectonicGenerator.getPlateAt(worldX, worldZ);
    }
    
    /**
     * Get plate boundary information at a position
     */
    public PlateBoundaryInfo getPlateBoundaryInfoAt(int worldX, int worldZ) {
        return tectonicGenerator.getBoundaryInfoAt(worldX, worldZ);
    }
    
    // IPlanetModel implementation
    @Override
    public IPlanetConfig getConfig() { return config; }
    
    @Override
    public PlanetType getType() { return config.getType(); }
    
    @Override
    public float getGravity() { return gravity; }
    
    @Override
    public float getAtmosphericPressure() { return atmosphericPressure; }
    
    @Override
    public float getErosionRate() {
        // Will be calculated based on wind/water later
        return 0.0f;
    }
    
    @Override
    public boolean hasLooseMaterialFormations() {
        // Will depend on environmental conditions later
        return false;
    }
    
    @Override
    public float getLooseMaterialFormationHeight() {
        return 0.0f;
    }
    
    @Override
    public float getSolidMaterialExposure() {
        // Will depend on erosion later
        return 1.0f;
    }
    
    // Physics model specific getters
    public TectonicPlateGenerator getTectonicGenerator() { return tectonicGenerator; }
}