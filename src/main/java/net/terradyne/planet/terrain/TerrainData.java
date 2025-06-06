package net.terradyne.planet.terrain;

import net.minecraft.block.BlockState;

public class TerrainData {
    public int baseHeight;              // Bedrock height
    public int surfaceHeight;           // Terrain surface height
    public BlockState primaryBlock;     // Main terrain block
    public BlockState surfaceBlock;     // Surface layer block
    public BlockState liquidBlock;      // Liquid block (null if none)
    public int liquidLevel;             // Liquid height (-1 if none)

    // Simple constructor
    public TerrainData(int baseHeight, int surfaceHeight,
                       BlockState primaryBlock, BlockState surfaceBlock) {
        this.baseHeight = baseHeight;
        this.surfaceHeight = surfaceHeight;
        this.primaryBlock = primaryBlock;
        this.surfaceBlock = surfaceBlock;
        this.liquidBlock = null;
        this.liquidLevel = -1;
    }

    // Constructor with liquid
    public TerrainData(int baseHeight, int surfaceHeight,
                       BlockState primaryBlock, BlockState surfaceBlock,
                       BlockState liquidBlock, int liquidLevel) {
        this.baseHeight = baseHeight;
        this.surfaceHeight = surfaceHeight;
        this.primaryBlock = primaryBlock;
        this.surfaceBlock = surfaceBlock;
        this.liquidBlock = liquidBlock;
        this.liquidLevel = liquidLevel;
    }

    // Fast blend - modifies this instance
    public void blendWith(TerrainData other, float weight) {
        if (weight <= 0f) return;
        if (weight >= 1f) {
            copyFrom(other);
            return;
        }

        // Linear interpolation for heights
        this.baseHeight = (int) (baseHeight + weight * (other.baseHeight - baseHeight));
        this.surfaceHeight = (int) (surfaceHeight + weight * (other.surfaceHeight - surfaceHeight));

        // Threshold-based block selection
        if (weight > 0.5f) {
            this.primaryBlock = other.primaryBlock;
            this.surfaceBlock = other.surfaceBlock;
        }

        // Simple liquid handling
        if (other.liquidBlock != null && weight > 0.3f) {
            this.liquidBlock = other.liquidBlock;
            this.liquidLevel = other.liquidLevel;
        }
    }

    // Fast copy - modifies this instance
    public void copyFrom(TerrainData other) {
        this.baseHeight = other.baseHeight;
        this.surfaceHeight = other.surfaceHeight;
        this.primaryBlock = other.primaryBlock;
        this.surfaceBlock = other.surfaceBlock;
        this.liquidBlock = other.liquidBlock;
        this.liquidLevel = other.liquidLevel;
    }

    // Quick checks
    public boolean hasLiquid() {
        return liquidBlock != null && liquidLevel >= 0;
    }

    public void setLiquid(BlockState block, int level) {
        this.liquidBlock = block;
        this.liquidLevel = level;
    }

    public void removeLiquid() {
        this.liquidBlock = null;
        this.liquidLevel = -1;
    }
}