package net.terradyne.planet;

// PlanetType.java
public enum PlanetType {
    OCEANIC("Oceanic Body", "Water-rich worlds with deep oceans and small landmasses"),
    ROCKY("Rocky Body", "Earth-like terrestrial planets with varied terrain"),
    IRON("Iron Body", "Dense metallic worlds rich in iron and heavy metals"),
    DESERT("Desert Body", "Arid worlds with minimal water and sand/rock terrain"),
    HOTHOUSE("Hothouse Body", "Extreme greenhouse desert worlds with scorching temperatures"),
    VOLCANIC("Volcanic Body", "Geologically active worlds with frequent eruptions"),
    ICY("Icy Body", "Frozen worlds with ice-covered surfaces"),
    SUBSURFACE_OCEANIC("Subsurface Oceanic Body", "Icy worlds with liquid oceans beneath the surface"),
    CARBON("Carbon Body", "Carbon-rich worlds with diamond and graphite formations");

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

    // Check if this is a subclass of another type
    public boolean isSubclassOf(PlanetType parentType) {
        return switch (this) {
            case HOTHOUSE -> parentType == DESERT;
            case SUBSURFACE_OCEANIC -> parentType == ICY;
            default -> false;
        };
    }

    // Get the parent type for subclasses
    public PlanetType getParentType() {
        return switch (this) {
            case HOTHOUSE -> DESERT;
            case SUBSURFACE_OCEANIC -> ICY;
            default -> this;
        };
    }

    // Check if this type has subclasses
    public boolean hasSubclasses() {
        return this == DESERT || this == ICY;
    }

    // Convert string to planet type (flexible matching)
    public static PlanetType fromString(String name) {
        if (name == null || name.trim().isEmpty()) {
            return ROCKY; // Default fallback
        }

        // First try exact matches
        for (PlanetType type : values()) {
            if (type.name().equalsIgnoreCase(name) ||
                    type.displayName.equalsIgnoreCase(name)) {
                return type;
            }
        }

        // Try simplified names (remove spaces, underscores, "body", etc.)
        String normalized = name.toLowerCase()
                .replace(" ", "")
                .replace("_", "")
                .replace("-", "")
                .replace("body", "");

        return switch (normalized) {
            case "oceanic", "ocean", "water", "aquatic" -> OCEANIC;
            case "rocky", "rock", "terrestrial", "earthlike", "earth" -> ROCKY;
            case "iron", "metal", "metallic", "ferrous" -> IRON;
            case "desert", "arid", "dry", "sand", "sandy" -> DESERT;
            case "hothouse", "hot", "greenhouse", "venus", "extreme" -> HOTHOUSE;
            case "volcanic", "volcano", "lava", "magma", "igneous" -> VOLCANIC;
            case "icy", "ice", "frozen", "cold", "polar" -> ICY;
            case "subsurface", "subsurfaceoceanic", "icyocean", "europa" -> SUBSURFACE_OCEANIC;
            case "carbon", "diamond", "coal", "graphite" -> CARBON;
            default -> ROCKY; // Default fallback
        };
    }

    // Get all main types (exclude subclasses for simplified listing)
    public static PlanetType[] getMainTypes() {
        return new PlanetType[]{OCEANIC, ROCKY, IRON, DESERT, VOLCANIC, ICY, CARBON};
    }

    // Get subclasses of a type
    public static PlanetType[] getSubclasses(PlanetType parentType) {
        return switch (parentType) {
            case DESERT -> new PlanetType[]{HOTHOUSE};
            case ICY -> new PlanetType[]{SUBSURFACE_OCEANIC};
            default -> new PlanetType[]{};
        };
    }

    // PlanetType.java - Update isImplemented method
    public boolean isImplemented() {
        return switch (this) {
            case DESERT, HOTHOUSE, OCEANIC, ROCKY -> true; // Added ROCKY
            default -> false;
        };
    }

    // Get implementation status message
    public String getImplementationStatus() {
        if (isImplemented()) {
            return "§a✓ Implemented";
        } else {
            return "§c✗ Coming Soon";
        }
    }

    // Get all implemented types
    public static PlanetType[] getImplementedTypes() {
        return java.util.Arrays.stream(values())
                .filter(PlanetType::isImplemented)
                .toArray(PlanetType[]::new);
    }

    // Get suggestion strings for commands
    public String[] getCommandSuggestions() {
        return switch (this) {
            case OCEANIC -> new String[]{"oceanic", "ocean", "water"};
            case ROCKY -> new String[]{"rocky", "rock", "terrestrial", "earth"};
            case IRON -> new String[]{"iron", "metal", "metallic"};
            case DESERT -> new String[]{"desert", "arid", "sand"};
            case HOTHOUSE -> new String[]{"hothouse", "hot", "greenhouse"};
            case VOLCANIC -> new String[]{"volcanic", "volcano", "lava"};
            case ICY -> new String[]{"icy", "ice", "frozen"};
            case SUBSURFACE_OCEANIC -> new String[]{"subsurface", "europa"};
            case CARBON -> new String[]{"carbon", "diamond"};
        };
    }
}