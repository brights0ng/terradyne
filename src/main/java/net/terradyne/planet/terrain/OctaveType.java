package net.terradyne.planet.terrain;

/**
 * Unified octave type classification system
 * Matches the design document exactly - biomes select which types they want
 */
public enum OctaveType {
    /**
     * Large-scale base terrain - continental features, tectonic plates
     * Lowest frequency, highest amplitude
     * Priority: 100 (applied first)
     */
    FOUNDATION,

    /**
     * Major additive features - large dunes, mountain ranges, mesas
     * Low-medium frequency, high amplitude
     * Priority: 200
     */
    ADDITIVE_MAJOR,

    /**
     * Medium additive features - dune ridges, hills, rock formations
     * Medium frequency, medium amplitude
     * Priority: 250
     */
    ADDITIVE_MEDIUM,

    /**
     * Fine surface details - ripples, texture, small rocks
     * High frequency, low amplitude
     * Priority: 300
     */
    ADDITIVE_FINE,

    /**
     * Major carving features - deep canyons, valleys, lava channels
     * Low-medium frequency, high negative amplitude
     * Priority: 400 (applied after additive features)
     */
    SUBTRACTIVE_MAJOR,

    /**
     * Minor carving features - gullies, small channels, erosion
     * Medium-high frequency, medium negative amplitude
     * Priority: 450
     */
    SUBTRACTIVE_MINOR,

    /**
     * Wind-direction dependent features - elongated dunes, erosion patterns
     * Variable frequency, aligned with wind direction
     * Priority: 275 (between medium and fine additive)
     */
    WIND_ALIGNED,

    /**
     * Specialized planet-specific features - lava flows, salt patterns, ice formations
     * Variable frequency and amplitude, highly specialized
     * Priority: 350
     */
    SPECIALIZED;

    /**
     * Get the application priority for this octave type
     * Lower numbers = applied first (foundation)
     * Higher numbers = applied last (fine details)
     */
    public int getPriority() {
        return switch (this) {
            case FOUNDATION -> 100;
            case ADDITIVE_MAJOR -> 200;
            case ADDITIVE_MEDIUM -> 250;
            case WIND_ALIGNED -> 275;
            case ADDITIVE_FINE -> 300;
            case SPECIALIZED -> 350;
            case SUBTRACTIVE_MAJOR -> 400;
            case SUBTRACTIVE_MINOR -> 450;
        };
    }

    /**
     * Check if this octave type adds height (positive contribution)
     */
    public boolean isAdditive() {
        return switch (this) {
            case FOUNDATION, ADDITIVE_MAJOR, ADDITIVE_MEDIUM, ADDITIVE_FINE, WIND_ALIGNED -> true;
            case SUBTRACTIVE_MAJOR, SUBTRACTIVE_MINOR -> false;
            case SPECIALIZED -> true; // Most specialized features are additive
        };
    }

    /**
     * Check if this octave type removes height (negative contribution)
     */
    public boolean isSubtractive() {
        return switch (this) {
            case SUBTRACTIVE_MAJOR, SUBTRACTIVE_MINOR -> true;
            default -> false;
        };
    }

    /**
     * Get a description of what this octave type does
     */
    public String getDescription() {
        return switch (this) {
            case FOUNDATION -> "Large-scale continental base terrain";
            case ADDITIVE_MAJOR -> "Major terrain features (dunes, mountains, mesas)";
            case ADDITIVE_MEDIUM -> "Medium terrain features (ridges, hills)";
            case ADDITIVE_FINE -> "Fine surface details (ripples, texture)";
            case SUBTRACTIVE_MAJOR -> "Major erosion (deep canyons, valleys)";
            case SUBTRACTIVE_MINOR -> "Minor erosion (gullies, channels)";
            case WIND_ALIGNED -> "Wind-directional features (elongated dunes)";
            case SPECIALIZED -> "Planet-specific specialized features";
        };
    }
}