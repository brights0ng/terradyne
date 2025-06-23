package net.starlight.terradyne.planet.physics;

/**
 * Primary atmospheric composition affecting climate and habitability
 */
public enum AtmosphereComposition {
    OXYGEN_RICH("Oxygen-Rich", "Earth-like breathable atmosphere"),
    CARBON_DIOXIDE("Carbon Dioxide", "Venus-like thick CO2, greenhouse effect"),
    METHANE("Methane", "Titan-like methane atmosphere, cold"),
    NITROGEN_RICH("Nitrogen-Rich", "Inert nitrogen atmosphere, stable"),
    NOBLE_GAS_MIXTURE("Noble Gas Mix", "Argon/neon atmosphere, chemically inert"),
    WATER_VAPOR_RICH("Water Vapor", "Steam atmosphere, very hot and humid"),
    HYDROGEN_SULFIDE("Hydrogen Sulfide", "Toxic volcanic atmosphere"),
    TRACE_ATMOSPHERE("Trace Atmosphere", "Mars-like thin atmosphere"),
    VACUUM("Vacuum", "Airless world, space-like conditions");

    private final String displayName;
    private final String description;

    AtmosphereComposition(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
