package net.starlight.terradyne.planet.physics;

import net.minecraft.util.math.Vec2f;
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
        
        // Calculate distance to boundary (midpoint between two nearest plates)
        float boundaryDistance = (secondNearestDist - nearestDist) / 2.0f;
        
        // Determine boundary type based on plate types
        BoundaryType boundaryType = determineBoundaryType(nearest, secondNearest);
        
        // Calculate volatility based on distance to boundary
        float volatility = calculateVolatility(boundaryDistance, boundaryType);
        
        return new PlateBoundaryInfo(boundaryDistance, boundaryType, volatility);
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
    
    /**
     * Calculate volatility based on distance to boundary
     */
    private float calculateVolatility(float boundaryDistance, BoundaryType boundaryType) {
        // No volatility far from boundaries
        if (boundaryDistance > 1000.0f) return 0.0f;
        
        // Volatility strength based on boundary type
        float maxVolatility = switch (boundaryType) {
            case DIVERGENT -> 1.0f;   // Positive volatility (rifts)
            case TRANSFORM -> -0.5f;  // Negative volatility (valleys)
            case CONVERGENT -> 1.5f;  // Strong positive (mountains)
            case NONE -> 0.0f;
        };
        
        // Smooth falloff based on distance
        float falloff = 1.0f - (boundaryDistance / 1000.0f);
        return maxVolatility * falloff * falloff; // Quadratic falloff
    }
    
    // Getters
    public List<TectonicPlate> getPlates() { return plates; }
    
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