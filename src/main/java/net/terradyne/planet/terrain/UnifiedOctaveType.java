package net.terradyne.planet.terrain;

/**
 * Categories of unified octaves for organization and sorting
 */
public enum UnifiedOctaveType {
    FOUNDATION,         // Large-scale base terrain (lowest frequency)
    MAJOR_FEATURES,     // Large terrain features (dunes, mesas)
    MEDIUM_FEATURES,    // Medium-scale patterns (ridges, channels)
    FINE_DETAILS,       // High-frequency surface texture
    CARVING,            // Subtractive features (canyons, valleys)
    WIND_ALIGNED        // Wind-direction based features
}