package com.github.alexthe666.alexsmobs.misc;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public class ModTagsCompat {
    public static final TagKey<Block> FORGE_ORES =
        TagKey.of(RegistryKeys.BLOCK, new Identifier("forge", "ores"));
    
    public static final TagKey<Block> COMMON_ORES =
        TagKey.of(RegistryKeys.BLOCK, new Identifier("c", "ores"));

    public static final TagKey<Block> COMMON_RAW_ORES =
            TagKey.of(RegistryKeys.BLOCK, new Identifier("c", "raw_ores"));

    public static final TagKey<Item> FORGE_SEEDS =
            TagKey.of(RegistryKeys.ITEM, new Identifier("forge", "seeds"));

    public static final TagKey<Item> COMMON_SEEDS =
            TagKey.of(RegistryKeys.ITEM, new Identifier("c", "seeds"));

    public static final TagKey<Item> FORGE_SHEARS =
            TagKey.of(RegistryKeys.ITEM, new Identifier("forge", "shears"));

    public static final TagKey<Item> COMMON_SHEARS =
            TagKey.of(RegistryKeys.ITEM, new Identifier("c", "shears"));

    public static final TagKey<Item> FORGE_WOODEN_CHESTS =
            TagKey.of(RegistryKeys.ITEM, new Identifier("c", "chests/wooden"));

    public static final TagKey<Item> COMMON_WOODEN_CHESTS =
            TagKey.of(RegistryKeys.ITEM, new Identifier("c", "chests"));

    public static boolean isAnyOre(BlockState state) {
        return state.isIn(FORGE_ORES) || state.isIn(COMMON_ORES) || state.isIn(COMMON_RAW_ORES);
    }

    public static boolean isAnySeeds(ItemStack stack) {
        return stack.isIn(FORGE_SEEDS) || stack.isIn(COMMON_SEEDS);
    }

    public static boolean isAnyShear(ItemStack stack) {
        return stack.isIn(FORGE_SHEARS) || stack.isIn(COMMON_SHEARS);
    }

    public static boolean isAnyWoodenChest(ItemStack stack) {
        return stack.isIn(FORGE_WOODEN_CHESTS) || stack.isIn(COMMON_WOODEN_CHESTS);
    }
}