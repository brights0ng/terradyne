package net.starlight.terradyne.planet.terrain.octave;

import net.starlight.terradyne.Terradyne;
import net.starlight.terradyne.planet.terrain.octave.IUnifiedOctave;
import net.starlight.terradyne.planet.terrain.octave.TectonicFoundationOctave;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for terrain octaves
 * Will be phased out as we transition to passes
 */
@Deprecated
public class OctaveRegistry {
    
    private static final Map<Class<? extends IUnifiedOctave>, IUnifiedOctave> octaveInstances = new HashMap<>();
    private static boolean initialized = false;
    
    public static synchronized void initialize() {
        if (initialized) return;
        
        // Register terrain octaves
        registerOctave(new TectonicFoundationOctave());
        registerOctave(new ContinentalFoundationOctave());
        
        initialized = true;
        Terradyne.LOGGER.info("✅ Octave Registry: {} octaves registered (legacy system)", octaveInstances.size());
    }
    
    public static void registerOctave(IUnifiedOctave octave) {
        octaveInstances.put(octave.getClass(), octave);
    }
    
    public static IUnifiedOctave getOctave(Class<? extends IUnifiedOctave> octaveClass) {
        return octaveInstances.get(octaveClass);
    }
}