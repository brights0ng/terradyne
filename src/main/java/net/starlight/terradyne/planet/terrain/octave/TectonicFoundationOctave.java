package net.starlight.terradyne.planet.terrain.octave;

import net.starlight.terradyne.planet.PlanetType;
import net.starlight.terradyne.planet.physics.PlanetPhysicsModel;
import net.starlight.terradyne.planet.physics.TectonicPlateGenerator.TectonicPlate;

import java.util.Set;

/**
 * Foundation octave based on tectonic plates
 * For now, creates a flat world where each plate has a different elevation
 */
public class TectonicFoundationOctave implements IUnifiedOctave {
    
    @Override
    public double generateHeightContribution(int x, int z, OctaveContext context, 
                                           OctaveConfiguration config) {
        // Get the physics model from context
        if (!(context.getPlanetModel() instanceof PlanetPhysicsModel)) {
            // Fallback for non-physics planets
            return 0.0;
        }
        
        PlanetPhysicsModel physicsModel = (PlanetPhysicsModel) context.getPlanetModel();
        
        // Get the tectonic plate at this position
        TectonicPlate plate = physicsModel.getTectonicPlateAt(x, z);
        
        // For now, just return the plate's base elevation
        // This creates a flat world where each plate has a different height
        return plate.getBaseElevation();
    }
    
    @Override
    public double getPrimaryFrequency() {
        // Tectonic features are the largest scale
        return 0.0001;
    }
    
    @Override
    public Set<PlanetType> getSupportedPlanetTypes() {
        // All planets use tectonic foundation
        return Set.of(PlanetType.values());
    }
    
    @Override
    public String getOctaveName() {
        return "TectonicFoundation";
    }
    
    @Override
    public String getParameterDocumentation() {
        return """
            Tectonic Foundation Octave:
            Creates base terrain elevation from tectonic plates.
            Currently creates a flat world with different elevations per plate.
            
            No configuration parameters - reads directly from planet physics model.
            """;
    }
}