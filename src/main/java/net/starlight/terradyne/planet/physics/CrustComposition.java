package net.starlight.terradyne.planet.physics;

/**
 * Primary crust composition types affecting geology and terrain
 */
public enum CrustComposition {
    SILICATE_RICH("Silicate-Rich", "Standard rocky composition, moderate reflectivity"),
    IRON_RICH("Iron-Rich", "Metallic surface, low reflectivity, magnetic"),
    CARBON_RICH("Carbon-Rich", "Diamond/graphite formations, very low reflectivity"),
    ICE_RICH("Ice-Rich", "Frozen water/methane, high reflectivity, cold"),
    SULFUR_RICH("Sulfur-Rich", "Volcanic sulfur compounds, yellow tints"),
    SALT_RICH("Salt-Rich", "Dried ocean beds, crystalline formations"),
    BASALTIC("Basaltic", "Volcanic rock surfaces, dark, recent volcanism"),
    GRANITE("Granite", "Light-colored igneous rock, high reflectivity"),
    SANDSTONE("Sandstone", "Sedimentary rock, moderate reflectivity, layered");

    private final String displayName;
    private final String description;

    CrustComposition(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
