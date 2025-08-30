package com.github.alexthe666.alexsmobs.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ItemFuel extends Item {

    private final int burnTime;

    public ItemFuel(Item.Settings props, int burnTime) {
        super(props);
        this.burnTime = burnTime;
    }

    public int getBurnTime(ItemStack itemStack) {
        return burnTime;
    }
}
