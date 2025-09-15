package com.github.alexthe666.alexsmobs.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;

public class ItemInventoryOnly extends Item implements CustomTabBehavior {

    public ItemInventoryOnly(Item.Settings properties) {
        super(properties);
    }

    @Override
    public void fillItemCategory(ItemGroup.Entries contents) {

    }
}
