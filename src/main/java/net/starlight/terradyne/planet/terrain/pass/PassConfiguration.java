package net.starlight.terradyne.planet.terrain.pass;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration container for generation pass parameters
 * Allows biomes to specify custom parameters for each pass they use
 */
public class PassConfiguration {
    private final Class<? extends IGenerationPass> passClass;
    private final Map<String, Object> parameters;
    private final int priority;

    public PassConfiguration(Class<? extends IGenerationPass> passClass, int priority) {
        this.passClass = passClass;
        this.priority = priority;
        this.parameters = new HashMap<>();
    }

    /**
     * Add a parameter to this configuration
     */
    public PassConfiguration withParameter(String key, Object value) {
        parameters.put(key, value);
        return this;
    }

    /**
     * Get the pass class this configuration is for
     */
    public Class<? extends IGenerationPass> getPassClass() {
        return passClass;
    }

    /**
     * Get the priority for this pass
     */
    public int getPriority() {
        return priority;
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
     * Get a parameter as BlockState with default fallback
     */
    public BlockState getBlockState(String key, BlockState defaultValue) {
        Object value = parameters.get(key);
        if (value instanceof BlockState) {
            return (BlockState) value;
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
        return passClass.getSimpleName() + " (priority=" + priority + ") " + parameters.toString();
    }
}