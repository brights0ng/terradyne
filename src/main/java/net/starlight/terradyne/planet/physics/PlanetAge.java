package net.starlight.terradyne.planet.physics;

/**
 * Planet age categories for physics-based generation
 */
public enum PlanetAge {
    INFANT("Infant", "Recently formed, still cooling, heavy bombardment"),
    YOUNG("Young", "Active geology, developing atmosphere"),
    MATURE("Mature", "Stable systems, potential for complex geology"),
    OLD("Old", "Cooling core, reduced geological activity"),
    DEAD("Dead", "No geological activity, atmosphere may be lost");

    private final String displayName;
    private final String description;

    PlanetAge(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}

