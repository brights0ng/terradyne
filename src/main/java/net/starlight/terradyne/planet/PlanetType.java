package net.starlight.terradyne.planet;

/**
 * Planet types that emerge from physical conditions
 * No longer used for direct creation - physics determines type
 */
public enum PlanetType {
    OCEANIC("Oceanic", "Water-rich worlds with deep oceans"),
    ROCKY("Rocky", "Airless or thin-atmosphere rocky worlds"),
    DESERT("Desert", "Arid worlds with minimal water"),
    VOLCANIC("Volcanic", "Tectonically active worlds"),
    ICY("Icy", "Frozen worlds with ice surfaces"),
    IRON("Iron", "Dense metallic worlds"),
    CARBON("Carbon", "Carbon-rich worlds");
    
    private final String displayName;
    private final String description;
    
    PlanetType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
}