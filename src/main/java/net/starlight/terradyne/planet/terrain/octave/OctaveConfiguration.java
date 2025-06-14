package net.starlight.terradyne.planet.terrain.octave;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for terrain octaves
 * Will be phased out as we move to passes
 */
@Deprecated
public class OctaveConfiguration {
    
    private final Class<? extends IUnifiedOctave> octaveClass;
    private final Map<String, Object> parameters;
    
    public OctaveConfiguration(Class<? extends IUnifiedOctave> octaveClass) {
        this.octaveClass = octaveClass;
        this.parameters = new HashMap<>();
    }
    
    public OctaveConfiguration withParameter(String key, Object value) {
        parameters.put(key, value);
        return this;
    }
    
    public Class<? extends IUnifiedOctave> getOctaveClass() {
        return octaveClass;
    }
    
    public double getDouble(String key, double defaultValue) {
        Object value = parameters.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }
}