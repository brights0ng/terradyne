package net.starlight.terradyne.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.dimension.v1.FabricDimensions;
import net.fabricmc.fabric.impl.dimension.FabricDimensionInternals;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import net.starlight.terradyne.planet.physics.*;
import net.starlight.terradyne.planet.terrain.TerrainHeightMapper;
import net.starlight.terradyne.planet.world.PlanetChunkGenerator;
import net.starlight.terradyne.planet.world.PlanetDimensionManager;

/**
 * Commands for creating and managing planetary dimensions.
 * Provides in-game interface for testing the planet generation system.
 */
public class PlanetCommands {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("terradyne")
                .then(CommandManager.literal("create")
                        .then(CommandManager.argument("planet_type", StringArgumentType.string())
                                .executes(PlanetCommands::createPlanet)))
                .then(CommandManager.literal("tp")
                        .then(CommandManager.argument("planet_name", StringArgumentType.string())
                                .executes(PlanetCommands::teleportToPlanet)))
                .then(CommandManager.literal("list")
                        .executes(PlanetCommands::listPlanets))
                .then(CommandManager.literal("info")
                        .executes(PlanetCommands::showCurrentPlanetInfo))
                .then(CommandManager.literal("debug")
                        .executes(PlanetCommands::showDebugInfo))
                .then(CommandManager.literal("test_terrain")
                        .then(CommandManager.argument("planet_type", StringArgumentType.string())
                                .executes(PlanetCommands::testTerrain)))
        );
    }

    /**
     * Create a planet dimension
     * Usage: /terradyne create <planet_type>
     * planet_type: earthlike, marslike, venuslike, moonlike, hadean
     */
    private static int createPlanet(CommandContext<ServerCommandSource> context) {
        String planetType = StringArgumentType.getString(context, "planet_type").toLowerCase();

        try {
            // Create planet config based on type
            PlanetConfig config = createPlanetConfig(planetType);
            if (config == null) {
                context.getSource().sendMessage(Text.literal("Unknown planet type: " + planetType));
                context.getSource().sendMessage(Text.literal("Available types: earthlike, marslike, venuslike, moonlike, hadean"));
                return 0;
            }

            // Create planet model
            PlanetModel planetModel = PlanetModel.fromConfig(config);
            planetModel.create();

            // Create dimension
            RegistryKey<World> dimensionKey = PlanetDimensionManager.createPlanetDimension(
                    context.getSource().getServer(), planetModel);

            if (dimensionKey != null) {
                context.getSource().sendMessage(Text.literal("✓ Created planet: " + config.getPlanetName()));
                context.getSource().sendMessage(Text.literal("Use '/terradyne tp " + config.getPlanetName() + "' to visit"));
                return 1;
            } else {
                context.getSource().sendMessage(Text.literal("✗ Failed to create planet dimension"));
                return 0;
            }

        } catch (Exception e) {
            context.getSource().sendMessage(Text.literal("✗ Error creating planet: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Teleport to a planet
     * Usage: /terradyne tp <planet_name>
     */
    private static int teleportToPlanet(CommandContext<ServerCommandSource> context) {
        String planetName = StringArgumentType.getString(context, "planet_name");

        ServerCommandSource source = context.getSource();
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            source.sendMessage(Text.literal("This command can only be used by players"));
            return 0;
        }

        boolean success = PlanetDimensionManager.teleportPlayerToPlanet(player, planetName);

        if (success) {
            player.sendMessage(Text.literal("✓ Teleported to planet: " + planetName));
            return 1;
        } else {
            player.sendMessage(Text.literal("✗ Failed to teleport to planet: " + planetName));
            player.sendMessage(Text.literal("Planet may not exist. Use '/terradyne list' to see available planets"));
            return 0;
        }
    }

    /**
     * List all created planets
     * Usage: /terradyne list
     */
    private static int listPlanets(CommandContext<ServerCommandSource> context) {
        String[] planetNames = PlanetDimensionManager.getAllPlanetNames();

        if (planetNames.length == 0) {
            context.getSource().sendMessage(Text.literal("No planets created yet"));
            context.getSource().sendMessage(Text.literal("Use '/terradyne create <type>' to create a planet"));
            return 0;
        }

        context.getSource().sendMessage(Text.literal("=== Created Planets ==="));
        for (String planetName : planetNames) {
            PlanetModel model = PlanetDimensionManager.getPlanetModel(planetName);
            if (model != null) {
                PlanetData data = model.getPlanetData();
                context.getSource().sendMessage(Text.literal("• " + planetName +
                        " (" + data.getCrustComposition() + ", " +
                        String.format("%.1f°C", data.getAverageSurfaceTemp()) + ")"));
            } else {
                context.getSource().sendMessage(Text.literal("• " + planetName + " (data unavailable)"));
            }
        }

        return planetNames.length;
    }

    /**
     * Show info about current planet
     * Usage: /terradyne info
     */
    private static int showCurrentPlanetInfo(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        // Try to determine current planet from dimension
        // This is a simplified approach - in practice, we'd need better dimension tracking

        source.sendMessage(Text.literal("=== Current World Info ==="));
        source.sendMessage(Text.literal("Dimension: " + source.getWorld().getRegistryKey().getValue()));

        // If we're on a planet dimension, show planet info
        // For now, this is basic - could be enhanced to detect planet dimensions

        return 1;
    }

    /**
     * Show debug information
     * Usage: /terradyne debug
     */
    private static int showDebugInfo(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        // Show dimension manager debug info
        String debugInfo = PlanetDimensionManager.getDebugInfo();

        // Split into multiple messages to avoid chat length limits
        String[] lines = debugInfo.split("\n");
        for (String line : lines) {
            source.sendMessage(Text.literal(line));
        }

        return 1;
    }

    /**
     * Test terrain generation in the current world
     * Usage: /terradyne test_terrain <planet_type>
     */
    private static int testTerrain(CommandContext<ServerCommandSource> context) {
        String planetType = StringArgumentType.getString(context, "planet_type").toLowerCase();

        ServerCommandSource source = context.getSource();
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            source.sendMessage(Text.literal("This command can only be used by players"));
            return 0;
        }

        try {
            // Create planet config
            PlanetConfig config = createPlanetConfig(planetType);
            if (config == null) {
                player.sendMessage(Text.literal("Unknown planet type: " + planetType));
                player.sendMessage(Text.literal("Available types: earthlike, marslike, venuslike, moonlike, hadean"));
                return 0;
            }

            player.sendMessage(Text.literal("🌍 Generating " + config.getPlanetName() + " terrain around you..."));
            player.sendMessage(Text.literal("⚠️ This will replace terrain in nearby chunks!"));

            // Create planet model
            PlanetModel planetModel = PlanetModel.fromConfig(config);
            planetModel.create();

            // Get the chunk generator
            PlanetChunkGenerator chunkGenerator = planetModel.getChunkGenerator();

            // Get player position
            BlockPos playerPos = player.getBlockPos();
            ServerWorld world = player.getServerWorld();

            // Generate terrain in a 3x3 chunk area around the player
            int chunkX = playerPos.getX() >> 4; // playerPos.getX() / 16
            int chunkZ = playerPos.getZ() >> 4; // playerPos.getZ() / 16

            int chunksGenerated = 0;
            int blocksPlaced = 0;

            // Generate 3x3 chunks around player
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int targetChunkX = chunkX + dx;
                    int targetChunkZ = chunkZ + dz;

                    blocksPlaced += generatePlanetChunk(world, chunkGenerator, targetChunkX, targetChunkZ);
                    chunksGenerated++;
                }
            }

            player.sendMessage(Text.literal("✓ Generated " + chunksGenerated + " chunks with " + blocksPlaced + " blocks"));
            player.sendMessage(Text.literal("🎯 Planet: " + config.getPlanetName() +
                    " (" + String.format("%.1f°C", planetModel.getPlanetData().getAverageSurfaceTemp()) + ")"));
            player.sendMessage(Text.literal("🏔️ Height range: " +
                    planetModel.getTerrainConfig().getHeightSettings().minHeight + " to " +
                    planetModel.getTerrainConfig().getHeightSettings().maxHeight));
            player.sendMessage(Text.literal("🌊 Sea level: " +
                    planetModel.getTerrainConfig().getHeightSettings().seaLevel));

            return chunksGenerated;

        } catch (Exception e) {
            player.sendMessage(Text.literal("✗ Error generating terrain: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Generate planet terrain for a single chunk
     */
    private static int generatePlanetChunk(ServerWorld world, PlanetChunkGenerator chunkGenerator,
                                           int chunkX, int chunkZ) {
        int blocksPlaced = 0;

        try {
            // Calculate world coordinates for this chunk
            int startX = chunkX << 4; // chunkX * 16
            int startZ = chunkZ << 4; // chunkZ * 16

            // Generate terrain for each column in the chunk
            for (int localX = 0; localX < 16; localX++) {
                for (int localZ = 0; localZ < 16; localZ++) {
                    int worldX = startX + localX;
                    int worldZ = startZ + localZ;

                    // Get terrain column from our generator
                    TerrainHeightMapper.TerrainColumn terrainColumn = generateTerrainColumn(chunkGenerator, worldX, worldZ);

                    // Place blocks in the world
                    blocksPlaced += placeTerrainColumn(world, worldX, worldZ, terrainColumn);
                }
            }

        } catch (Exception e) {
            System.err.println("Error generating chunk (" + chunkX + ", " + chunkZ + "): " + e.getMessage());
        }

        return blocksPlaced;
    }

    /**
     * Create planet config based on type string
     */
    private static PlanetConfig createPlanetConfig(String type) {
        switch (type) {
            case "earthlike":
            case "earth":
                return new TestPlanetConfig.EarthLike();

            case "marslike":
            case "mars":
                return new TestPlanetConfig.MarsLike();

            case "venuslike":
            case "venus":
                return new TestPlanetConfig.VenusLike();

            case "moonlike":
            case "moon":
                return new TestPlanetConfig.MoonLike();

            case "hadean":
            case "hell":
                return new TestPlanetConfig.HadeanWorld();

            default:
                return null;
        }
    }

    /**
    * Generate terrain column using our chunk generator
    */
    private static TerrainHeightMapper.TerrainColumn generateTerrainColumn(PlanetChunkGenerator chunkGenerator,
                                                                       int worldX, int worldZ) {
    // Sample noise from our planetary noise system
    double noiseValue = chunkGenerator.getPlanetModel().getNoiseSystem().sampleTerrain(worldX, worldZ);

    // Calculate erosion factor (simplified)
    double erosionFactor = 0.5; // Could be enhanced later

    // Generate terrain column using our height mapper
    TerrainHeightMapper heightMapper = new TerrainHeightMapper(chunkGenerator.getPlanetModel().getTerrainConfig());
    return heightMapper.generateTerrainColumn(noiseValue, erosionFactor);
    }

    /**
    * Place terrain column blocks in the world
    */
    private static int placeTerrainColumn(ServerWorld world, int worldX, int worldZ,
                                      TerrainHeightMapper.TerrainColumn terrainColumn) {
        int blocksPlaced = 0;

        try {
            // Clear the column first (from bedrock to build height)
            for (int y = world.getBottomY(); y < world.getTopY(); y++) {
                BlockPos pos = new BlockPos(worldX, y, worldZ);
                world.setBlockState(pos, net.minecraft.block.Blocks.AIR.getDefaultState());
            }

            // Place our terrain blocks
            for (int y = world.getBottomY(); y < world.getTopY(); y++) {
                String blockId = terrainColumn.getBlock(y);

                if (!blockId.equals("minecraft:air")) {
                    BlockPos pos = new BlockPos(worldX, y, worldZ);

                    // Convert block ID to block state
                    try {
                        net.minecraft.util.Identifier identifier = new net.minecraft.util.Identifier(blockId);
                        net.minecraft.block.Block block = net.minecraft.registry.Registries.BLOCK.get(identifier);
                        world.setBlockState(pos, block.getDefaultState());
                        blocksPlaced++;
                    } catch (Exception e) {
                        // Fallback to stone if block ID is invalid
                        world.setBlockState(pos, net.minecraft.block.Blocks.STONE.getDefaultState());
                        blocksPlaced++;
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error placing terrain column at (" + worldX + ", " + worldZ + "): " + e.getMessage());
        }

        return blocksPlaced;
    }
}
