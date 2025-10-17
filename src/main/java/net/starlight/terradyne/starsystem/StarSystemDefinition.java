package net.starlight.terradyne.starsystem;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Raw JSON representation of a star system definition
 * Loaded from data/[namespace]/terradyne/star_systems/*.json
 */
public class StarSystemDefinition {
    
    @SerializedName("name")
    public String name;
    
    @SerializedName("star")
    public OrbitNode star;
    
    public static class OrbitNode {
        @SerializedName("object")
        public String object; // Identifier string (namespace:name)
        
        @SerializedName("orbiting")
        public List<OrbitNode> orbiting;
    }
}