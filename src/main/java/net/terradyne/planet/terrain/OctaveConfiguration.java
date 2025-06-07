package net.terradyne.planet.terrain;

import net.terradyne.planet.terrain.octave.IUnifiedOctave;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration container for octave parameters
 * Allows biomes to specify custom parameters for each octave they use
 */
public class OctaveConfiguration {
    private final Class<? extends IUnifiedOctave> octaveClass;
    private final Map<String, Object> parameters;

    public OctaveConfiguration(Class<? extends IUnifiedOctave> octaveClass) {
        this.octaveClass = octaveClass;
        this.parameters = new HashMap<>();
    }

    /**
     * Add a parameter to this configuration
     */
    public OctaveConfiguration withParameter(String key, Object value) {
        parameters.put(key, value);
        return this;
    }

    /**
     * Get the octave class this configuration is for
     */
    public Class<? extends IUnifiedOctave> getOctaveClass() {
        return octaveClass;
    }

    /**
     * Get a parameter as double with default fallback
     */
    public double getDouble(String key, double defaultValue) {
        Object value = parameters.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }

    /**
     * Get a parameter as int with default fallback
     */
    public int getInt(String key, int defaultValue) {
        Object value = parameters.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    /**
     * Get a parameter as boolean with default fallback
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        Object value = parameters.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }

    /**
     * Get a parameter as string with default fallback
     */
    public String getString(String key, String defaultValue) {
        Object value = parameters.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        return defaultValue;
    }

    /**
     * Check if parameter exists
     */
    public boolean hasParameter(String key) {
        return parameters.containsKey(key);
    }

    /**
     * Get all parameters (for debugging)
     */
    public Map<String, Object> getAllParameters() {
        return new HashMap<>(parameters);
    }

    @Override
    public String toString() {
        return octaveClass.getSimpleName() + parameters.toString();
    }
}