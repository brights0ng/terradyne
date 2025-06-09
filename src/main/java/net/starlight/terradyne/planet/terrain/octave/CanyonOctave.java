package net.starlight.terradyne.planet.terrain.octave;

import net.starlight.terradyne.planet.PlanetType;
import net.starlight.terradyne.planet.terrain.MasterNoiseProvider;
import net.starlight.terradyne.planet.terrain.OctaveConfiguration;
import net.starlight.terradyne.planet.terrain.OctaveContext;

import java.util.Set;

/**
 * COMPLETELY REWRITTEN Canyon Octave - Creates dramatic winding canyon systems
 *
 * This octave creates realistic canyon networks that look like they were carved
 * by water flow over millions of years. Features meandering channels, tributary
 * systems, and realistic canyon wall profiles.
 */
public class CanyonOctave implements IUnifiedOctave {

    @Override
    public double generateHeightContribution(int x, int z, OctaveContext context,
                                             OctaveConfiguration config) {
        MasterNoiseProvider noise = context.getNoiseProvider();

        // Configuration parameters
        double maxDepth = config.getDouble("maxDepth", 40.0);
        double channelFrequency = config.getDouble("channelFrequency", 0.003);
        double meandering = config.getDouble("meandering", 1.2);
        double wallSteepness = config.getDouble("wallSteepness", 3.0);
        double tributaryDensity = config.getDouble("tributaryDensity", 0.8);

        // === STEP 1: CREATE MAIN CANYON CHANNELS ===
        // Primary canyon system using ridge noise for natural meandering
        double primaryChannel = createMeanderingChannel(noise, x, z,
                channelFrequency, meandering);

        // Secondary canyon network at different scale and orientation
        double secondaryChannel = createMeanderingChannel(noise, x, z,
                channelFrequency * 1.8, meandering * 0.7);

        // === STEP 2: CREATE TRIBUTARY SYSTEM ===
        // Smaller tributaries that feed into main canyons
        double tributaries = createTributaryNetwork(noise, x, z,
                channelFrequency * 3.5, tributaryDensity);

        // === STEP 3: COMBINE CHANNEL NETWORKS ===
        // Take the deepest channel at each point (channels don't add, they carve)
        double combinedChannels = Math.min(primaryChannel,
                Math.min(secondaryChannel * 0.7, tributaries * 0.4));

        // === STEP 4: CREATE REALISTIC CANYON WALLS ===
        // Convert ridge distance to realistic canyon depth profile
        double canyonDepth = createCanyonProfile(combinedChannels, wallSteepness);

        // === STEP 5: ADD CANYON FLOOR DETAIL ===
        // Canyon floors should have some rockfall and debris
        double floorDetail = 0.0;
        if (canyonDepth > maxDepth * 0.5) {
            floorDetail = noise.sampleAt(x * 0.02, 0, z * 0.02) * 2.0;
        }

        // === STEP 6: ADD SIDE CANYON ALCOVES ===
        // Natural slot canyons and alcoves carved into canyon walls
        double alcoves = createAlcoveSystem(noise, x, z, combinedChannels, channelFrequency);

        // === STEP 7: SCALE TO FINAL DEPTH ===
        double finalDepth = (canyonDepth * maxDepth) + floorDetail + alcoves;

        // Return negative value to carve terrain (canyons remove height)
        return -Math.max(0.0, finalDepth);
    }

    /**
     * Create meandering channel using multiple noise octaves
     */
    private double createMeanderingChannel(MasterNoiseProvider noise, int x, int z,
                                           double frequency, double meandering) {
        // Create meandering effect by sampling noise at curved coordinates
        double meander1 = noise.sampleAt(x * frequency * 0.5, 0, z * frequency * 0.5) * meandering;
        double meander2 = noise.sampleAt(x * frequency * 0.3, 0, z * frequency * 0.7) * meandering * 0.6;

        // Offset coordinates by meander amount
        double offsetX = x + meander1 * 50.0 + meander2 * 30.0;
        double offsetZ = z + meander1 * 30.0 + meander2 * 50.0;

        // Sample ridge noise at meandering coordinates
        return noise.sampleRidge(offsetX * frequency, 0, offsetZ * frequency);
    }

    /**
     * Create tributary network that connects to main channels
     */
    private double createTributaryNetwork(MasterNoiseProvider noise, int x, int z,
                                          double frequency, double density) {
        // Tributaries branch at multiple scales
        double largeTrib = noise.sampleRidge(x * frequency, 0, z * frequency * 1.4);
        double mediumTrib = noise.sampleRidge(x * frequency * 2.0, 0, z * frequency * 1.8) * 0.7;
        double smallTrib = noise.sampleRidge(x * frequency * 4.0, 0, z * frequency * 3.2) * 0.5;

        // Combine tributaries - take the best (deepest) connection
        double combinedTribs = Math.min(largeTrib, Math.min(mediumTrib, smallTrib));

        // Apply density factor
        return combinedTribs * density;
    }

    /**
     * Create better connected canyon networks
     */
    private double createConnectedCanyonNetwork(MasterNoiseProvider noise, int x, int z,
                                                double frequency, double connectionStrength) {
        // Primary canyon network
        double mainCanyon = noise.sampleRidge(x * frequency, 0, z * frequency * 0.7);

        // Secondary network at different angle
        double secondaryCanyon = noise.sampleRidge(x * frequency * 0.8, 0, z * frequency * 1.2);

        // Connection channels between networks
        double connections = noise.sampleRidge(x * frequency * 1.5, 0, z * frequency * 0.9);

        // Take the minimum (deepest) value for better connectivity
        double combined = Math.min(mainCanyon, Math.min(secondaryCanyon, connections * connectionStrength));

        return combined;
    }

    /**
     * Convert ridge distance to realistic canyon wall profile
     */
    private double createCanyonProfile(double ridgeDistance, double steepness) {
        // Ridge distance of 0.0 = center of canyon (deepest)
        // Ridge distance of 1.0 = canyon rim (no carving)

        if (ridgeDistance >= 0.8) {
            return 0.0; // No canyon here
        }

        // Create V-shaped canyon profile
        double profile = Math.max(0.0, 0.8 - ridgeDistance) / 0.8;

        // Apply steepness factor - higher steepness = more dramatic walls
        profile = Math.pow(profile, 1.0 / steepness);

        return profile;
    }

    /**
     * Create alcove and slot canyon systems
     */
    private double createAlcoveSystem(MasterNoiseProvider noise, int x, int z,
                                      double mainChannel, double frequency) {
        // Only create alcoves near existing canyon walls
        if (mainChannel > 0.6 || mainChannel < 0.2) {
            return 0.0;
        }

        // Small slot canyons perpendicular to main channels
        double slotCanyons = noise.sampleRidge(x * frequency * 8.0, 0, z * frequency * 6.0);

        if (slotCanyons < 0.3) {
            // Create narrow slot canyon effect
            double slotDepth = (0.3 - slotCanyons) / 0.3;
            return slotDepth * 8.0; // Moderate depth for slots
        }

        return 0.0;
    }

    @Override
    public double getPrimaryFrequency() { return 0.003; }

    @Override
    public Set<PlanetType> getSupportedPlanetTypes() {
        return Set.of(PlanetType.DESERT, PlanetType.HOTHOUSE, PlanetType.ROCKY, PlanetType.VOLCANIC);
    }

    @Override
    public String getOctaveName() { return "AdvancedCanyon"; }

    @Override
    public String getParameterDocumentation() {
        return """
            Advanced Canyon Octave Parameters:
            - maxDepth (double, default 40.0): Maximum canyon depth
            - channelFrequency (double, default 0.003): Size/frequency of main channels
            - meandering (double, default 1.2): How much channels meander and curve
            - wallSteepness (double, default 3.0): Steepness of canyon walls (higher = steeper)
            - tributaryDensity (double, default 0.8): Density of tributary canyon network
            """;
    }
}