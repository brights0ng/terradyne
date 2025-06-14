package net.starlight.terradyne.planet.terrain.pass;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for a generation pass
 * Stores parameters that control how the pass behaves
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
    
    // Parameter getters with defaults
    
    public double getDouble(String key, double defaultValue) {
        Object value = parameters.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }
    
    public int getInt(String key, int defaultValue) {
        Object value = parameters.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }
    
    public boolean getBoolean(String key, boolean defaultValue) {
        Object value = parameters.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }
    
    public BlockState getBlockState(String key, BlockState defaultValue) {
        Object value = parameters.get(key);
        if (value instanceof BlockState) {
            return (BlockState) value;
        }
        return defaultValue;
    }
    
    public String getString(String key, String defaultValue) {
        Object value = parameters.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        return defaultValue;
    }
}