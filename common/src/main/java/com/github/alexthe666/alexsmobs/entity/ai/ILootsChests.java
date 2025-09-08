package com.github.alexthe666.alexsmobs.entity.ai;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

public interface ILootsChests {

    boolean isLootable(Inventory inventory);

    boolean shouldLootItem(ItemStack stack);
}
