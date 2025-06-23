package net.starlight.terradyne.planet.physics;

/**
 * Primary crust composition types affecting geology and terrain
 */
public enum CrustComposition {
    SILICATE("Silicate", "Silicate rock crust, lends to habitable life"),
    FERROUS("Ferrous", "Iron-rich rock, deep red hue"),
    CARBONACEOUS("Carbonaceous", "Carbon rock surface, diamond and graphite features"),
    REGOLITHIC("Regolithic", "Broken rock rubble surface, littered with craters"),
    SULFURIC("Sulfuric", "Volcanic sulfur compounds, yellow tints"),
    HALLIDE("Hallide", "Dried ocean beds, crystalline formations"),
    BASALTIC("Basaltic", "Dead volcanic rock surfaces, dark"),
    METALLIC("Metallic", "Hard metallic alloy crust, likely core of a dead gas giant"),
    HADEAN("Hadean", "Active volcanic surface, fresh igneous rock");

    private final String displayName;
    private final String description;

    CrustComposition(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
