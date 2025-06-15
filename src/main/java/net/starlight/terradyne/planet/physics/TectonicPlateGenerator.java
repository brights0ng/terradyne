package net.starlight.terradyne.planet.physics;

import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.noise.SimplexNoiseSampler;
import net.minecraft.util.math.random.Random;
import net.starlight.terradyne.Terradyne;

import java.util.*;

/**
 * Generates tectonic plates using Voronoi tessellation
 * Creates plate boundaries and assigns plate properties
 */
public class TectonicPlateGenerator {
    
    private final long seed;
    private final float tectonicScale;
    private final List<TectonicPlate> plates;
    private final int worldSize = 10000; // World size in blocks for plate generation
    
    public TectonicPlateGenerator(long seed, float tectonicScale) {
        this.seed = seed;
        this.tectonicScale = tectonicScale;
        this.plates = generatePlates();
    }
    
    /**
     * Generate tectonic plates based on scale
     */
    private List<TectonicPlate> generatePlates() {
        Random random = Random.create(seed);
        
        // Number of plates based on tectonic scale
        // 0.0 = 1 plate (no tectonics), 1.0 = ~12 plates (Earth-like)
        int plateCount = Math.max(1, (int)(tectonicScale * 12));
        
        List<TectonicPlate> generatedPlates = new ArrayList<>();
        
        // Generate plate centers using Poisson disk sampling for better distribution
        List<Vec2f> plateCenters = generatePlateCenters(random, plateCount);
        
        // Create plates
        for (int i = 0; i < plateCenters.size(); i++) {
            Vec2f center = plateCenters.get(i);
            
            // Assign plate type (1, 2, or 3 as per the document)
            int plateType = random.nextInt(3) + 1;
            
            // Base elevation offset for this plate (-30 to +30 blocks)
            float elevationOffset = (random.nextFloat() - 0.5f) * 60.0f;
            
            TectonicPlate plate = new TectonicPlate(
                i,                    // plate ID
                center.x,             // center X
                center.y,             // center Z
                plateType,            // type (1-3)
                elevationOffset       // base elevation
            );
            
            generatedPlates.add(plate);
        }
        
        Terradyne.LOGGER.info("Generated {} tectonic plates", generatedPlates.size());
        return generatedPlates;
    }
    
    /**
     * Generate well-distributed plate centers using Poisson disk sampling
     */
    private List<Vec2f> generatePlateCenters(Random random, int targetCount) {
        List<Vec2f> centers = new ArrayList<>();
        float minDistance = worldSize / (float)Math.sqrt(targetCount * 2);
        
        // Start with a random point
        centers.add(new Vec2f(
            random.nextFloat() * worldSize - worldSize/2,
            random.nextFloat() * worldSize - worldSize/2
        ));
        
        // Try to add more points
        int attempts = 0;
        while (centers.size() < targetCount && attempts < targetCount * 100) {
            float x = random.nextFloat() * worldSize - worldSize/2;
            float z = random.nextFloat() * worldSize - worldSize/2;
            Vec2f candidate = new Vec2f(x, z);
            
            // Check distance to all existing centers
            boolean validPosition = true;
            for (Vec2f existing : centers) {
                float dx = existing.x - candidate.x;
                float dy = existing.y - candidate.y;
                float distSq = dx * dx + dy * dy;
                
                if (distSq < minDistance * minDistance) {
                    validPosition = false;
                    break;
                }
            }
            
            if (validPosition) {
                centers.add(candidate);
            }
            attempts++;
        }
        
        return centers;
    }
    
    /**
     * Get the tectonic plate at a given world position
     * Uses Voronoi tessellation - finds the nearest plate center
     */
    public TectonicPlate getPlateAt(int worldX, int worldZ) {
        if (plates.size() == 1) {
            return plates.get(0); // Only one plate, no need to calculate
        }
        
        TectonicPlate nearestPlate = null;
        float nearestDistanceSq = Float.MAX_VALUE;
        
        for (TectonicPlate plate : plates) {
            float dx = worldX - plate.getCenterX();
            float dz = worldZ - plate.getCenterZ();
            float distanceSq = dx * dx + dz * dz;
            
            if (distanceSq < nearestDistanceSq) {
                nearestDistanceSq = distanceSq;
                nearestPlate = plate;
            }
        }
        
        return nearestPlate;
    }

    // The full getBoundaryInfoAt method should have this import added to the file:
// import net.minecraft.util.math.random.Random;

    /**
     * Get plate boundary info at a position
     * Returns distance to nearest boundary and the boundary type
     */
    public PlateBoundaryInfo getBoundaryInfoAt(int worldX, int worldZ) {
        if (plates.size() <= 1) {
            return new PlateBoundaryInfo(Float.MAX_VALUE, BoundaryType.NONE, 0.0f);
        }

        // Find two nearest plates
        TectonicPlate nearest = null;
        TectonicPlate secondNearest = null;
        float nearestDist = Float.MAX_VALUE;
        float secondNearestDist = Float.MAX_VALUE;

        for (TectonicPlate plate : plates) {
            float dx = worldX - plate.getCenterX();
            float dz = worldZ - plate.getCenterZ();
            float distance = (float)Math.sqrt(dx * dx + dz * dz);

            if (distance < nearestDist) {
                secondNearest = nearest;
                secondNearestDist = nearestDist;
                nearest = plate;
                nearestDist = distance;
            } else if (distance < secondNearestDist) {
                secondNearest = plate;
                secondNearestDist = distance;
            }
        }

        // FIX: Calculate approximate distance to boundary
        // The boundary is roughly at the midpoint between the two nearest plate centers
        // Distance to boundary = how far we are from that midpoint line

        // First, find the midpoint between the two plate centers
        float midX = (nearest.getCenterX() + secondNearest.getCenterX()) / 2.0f;
        float midZ = (nearest.getCenterZ() + secondNearest.getCenterZ()) / 2.0f;

        // Vector from nearest to second nearest plate
        float plateVecX = secondNearest.getCenterX() - nearest.getCenterX();
        float plateVecZ = secondNearest.getCenterZ() - nearest.getCenterZ();
        float plateVecLength = (float)Math.sqrt(plateVecX * plateVecX + plateVecZ * plateVecZ);

        // Normalize the vector
        plateVecX /= plateVecLength;
        plateVecZ /= plateVecLength;

        // Vector from midpoint to our position
        float toPointX = worldX - midX;
        float toPointZ = worldZ - midZ;

        // Project onto the perpendicular to get distance to boundary line
        // The boundary runs perpendicular to the line between plate centers
        float boundaryDistance = Math.abs(toPointX * plateVecX + toPointZ * plateVecZ);

        // Alternative simpler approach: use the ratio of distances
        // When we're exactly at the boundary, distances to both plates should be similar
        float distanceRatio = nearestDist / (nearestDist + secondNearestDist);
        // Convert ratio to distance estimate (0.5 = at boundary, 0 or 1 = at plate center)
        float boundaryProximity = Math.abs(distanceRatio - 0.5f) * 2.0f; // 0 at boundary, 1 at center
        boundaryDistance = boundaryProximity * (plateVecLength / 2.0f);

        // Determine boundary type based on plate types
        BoundaryType boundaryType = determineBoundaryType(nearest, secondNearest);

        // Calculate volatility based on distance to boundary
        float volatility = calculateVolatility(boundaryDistance, boundaryType);

        return new PlateBoundaryInfo(boundaryDistance, boundaryType, volatility);
    }

    /**
     * Calculate volatility based on distance to boundary
     * NOW WITH ORGANIC, NOISY BOUNDARIES!
     */
    private float calculateVolatility(float boundaryDistance, BoundaryType boundaryType) {
        // Get noise at this position for organic boundary shapes
        // Use the seed to ensure consistent noise
        Random random = Random.create(seed);
        SimplexNoiseSampler boundaryNoise = new SimplexNoiseSampler(random);

        // Sample noise at the position to vary the cutoff distance
        // This creates organic, flowing boundary shapes
        float noiseValue = (float)boundaryNoise.sample(
                boundaryDistance * 0.02,  // Use distance as one coordinate
                boundaryType.ordinal() * 100.0,  // Separate noise for each boundary type
                0
        );

        // Base distance is 100, but varies from 50 to 150 based on noise
        float baseDistance = 100.0f;
        float distanceVariation = 50.0f;
        float maxDistance = baseDistance + (noiseValue * distanceVariation);

        // Add some high-frequency noise for extra organic feel
        float detailNoise = (float)boundaryNoise.sample(
                boundaryDistance * 0.1,
                boundaryType.ordinal() * 200.0,
                0
        ) * 0.3f;

        maxDistance += detailNoise * 10.0f;

        if (boundaryDistance > maxDistance) return 0.0f;

        // Volatility strength based on boundary type
        // These values now represent how much they modify the continental noise
        float maxVolatility = switch (boundaryType) {
            case DIVERGENT -> 0.7f;   // Moderate uplift (rift mountains)
            case TRANSFORM -> -0.8f;  // Deep valleys
            case CONVERGENT -> 1.2f;  // Strong uplift (tall mountain ranges)
            case NONE -> 0.0f;
        };

        // Organic falloff using noise-modified distance
        float normalizedDistance = boundaryDistance / maxDistance;

        // Use a smoother curve with some noise variation
        float falloff = 1.0f - (normalizedDistance * normalizedDistance);

        // Add some noise to the falloff itself for more organic transitions
        float falloffNoise = (float)boundaryNoise.sample(
                boundaryDistance * 0.05,
                boundaryType.ordinal() * 300.0,
                0
        ) * 0.2f;

        falloff = Math.max(0.0f, falloff + falloffNoise);
        falloff = Math.min(1.0f, falloff);

        return maxVolatility * falloff;
    }
    
    /**
     * Determine boundary type between two plates
     */
    private BoundaryType determineBoundaryType(TectonicPlate plate1, TectonicPlate plate2) {
        if (plate1 == null || plate2 == null) return BoundaryType.NONE;
        
        int type1 = plate1.getPlateType();
        int type2 = plate2.getPlateType();
        
        if (type1 == type2) {
            return BoundaryType.DIVERGENT; // Same number = divergent
        } else if (Math.abs(type1 - type2) == 1) {
            return BoundaryType.TRANSFORM; // Difference of 1 = transform
        } else {
            return BoundaryType.CONVERGENT; // Difference of 2 = convergent
        }
    }

    public Collection<Object> getPlates() {
        return Collections.singleton(plates);
    }

    /**
     * Tectonic plate data
     */
    public static class TectonicPlate {
        private final int id;
        private final float centerX;
        private final float centerZ;
        private final int plateType; // 1, 2, or 3
        private final float baseElevation;
        
        public TectonicPlate(int id, float centerX, float centerZ, int plateType, float baseElevation) {
            this.id = id;
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.plateType = plateType;
            this.baseElevation = baseElevation;
        }
        
        public int getId() { return id; }
        public float getCenterX() { return centerX; }
        public float getCenterZ() { return centerZ; }
        public int getPlateType() { return plateType; }
        public float getBaseElevation() { return baseElevation; }
    }
    
    /**
     * Information about plate boundaries
     */
    public static class PlateBoundaryInfo {
        public final float distanceToBoundary;
        public final BoundaryType boundaryType;
        public final float volatility;
        
        public PlateBoundaryInfo(float distanceToBoundary, BoundaryType boundaryType, float volatility) {
            this.distanceToBoundary = distanceToBoundary;
            this.boundaryType = boundaryType;
            this.volatility = volatility;
        }
    }
    
    /**
     * Types of plate boundaries
     */
    public enum BoundaryType {
        NONE,       // No boundary nearby
        DIVERGENT,  // Plates moving apart (rifts)
        TRANSFORM,  // Plates sliding past (valleys)
        CONVERGENT  // Plates colliding (mountains)
    }
}