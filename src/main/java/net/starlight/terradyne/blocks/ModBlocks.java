package net.starlight.terradyne.blocks;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.PillarBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.starlight.terradyne.Terradyne;

public class ModBlocks {
    
    // Block definitions
    public static final Block QUARTZITE = registerBlock("quartzite",
            new Block(FabricBlockSettings.create()
                    .strength(2.0f, 7.0f)  // Slightly harder than granite
                    .requiresTool()         // Needs pickaxe
                    .sounds(BlockSoundGroup.STONE)  // Same sound as granite
            ));
    public static final Block SALT_BLOCK = registerBlock("salt_block",
            new Block(FabricBlockSettings.create()
                    .strength(2.0f, 3.0f)  // Slightly harder than granite
                    .requiresTool()         // Needs pickaxe
                    .sounds(BlockSoundGroup.STONE)  // Same sound as granite
            ));
    public static final Block LIMESTONE = registerBlock("limestone",
            new Block(FabricBlockSettings.create()
                    .strength(2.0f, 5.0f)  // Slightly harder than granite
                    .requiresTool()         // Needs pickaxe
                    .sounds(BlockSoundGroup.STONE)  // Same sound as granite
            ));
    public static final Block DOLOMITE = registerBlock("dolomite",
            new Block(FabricBlockSettings.create()
                    .strength(2.0f, 6.0f)  // Slightly harder than granite
                    .requiresTool()         // Needs pickaxe
                    .sounds(BlockSoundGroup.STONE)  // Same sound as granite
            ));

    /**
     * Register a block and its corresponding item
     */
    private static Block registerBlock(String name, Block block) {
        registerBlockItem(name, block);
        return Registry.register(Registries.BLOCK, new Identifier(Terradyne.MOD_ID, name), block);
    }

    /**
     * Register the block item for inventory/creative menu
     */
    private static Item registerBlockItem(String name, Block block) {
        return Registry.register(Registries.ITEM, new Identifier(Terradyne.MOD_ID, name),
                new BlockItem(block, new FabricItemSettings()));
    }

    /**
     * Initialize all mod blocks - called from main mod class
     */
    public static void initialize() {
        Terradyne.LOGGER.info("✓ Registering mod blocks");

        // Add to creative inventory
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(content -> {
            content.add(QUARTZITE);
        });
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(content -> {
            content.add(SALT_BLOCK);
        });
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(content -> {
            content.add(LIMESTONE);
        });
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(content -> {
            content.add(DOLOMITE);
        });
    }
}