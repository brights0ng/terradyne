package net.starlight.terradyne.starsystem;

import com.google.gson.annotations.SerializedName;

/**
 * Raw JSON representation of a celestial object definition
 * Loaded from data/[namespace]/terradyne/celestial_objects/*.json
 */
public class CelestialObjectDefinition {

    @SerializedName("type")
    public String type; // "terrestrial", "solar", "gaseous"

    @SerializedName("name")
    public String name; // Display name

    @SerializedName("physical_properties")
    public PhysicalProperties physicalProperties;

    @SerializedName("orbital_properties")
    public OrbitalProperties orbitalProperties;

    public static class PhysicalProperties {
        @SerializedName("circumference")
        public int circumference;

        @SerializedName("crust_composition")
        public String crustComposition;

        @SerializedName("atmosphere_composition")
        public String atmosphereComposition;

        @SerializedName("tectonic_activity")
        public double tectonicActivity;

        @SerializedName("water_content")
        public double waterContent;

        @SerializedName("crustal_thickness")
        public double crustalThickness;

        @SerializedName("atmospheric_density")
        public double atmosphericDensity;

        @SerializedName("rotation_period")
        public double rotationPeriod;

        @SerializedName("noise_scale")
        public double noiseScale;
    }

    public static class OrbitalProperties {
        @SerializedName("distance_from_parent")
        public double distanceFromParent; // km

        @SerializedName("distance_from_star")
        public double distanceFromStar; // million km

        @SerializedName("orbital_period")
        public double orbitalPeriod; // days

        @SerializedName("eccentricity")
        public double eccentricity;
    }
}