package com.github.alexthe666.alexsmobs.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ItemPigshoes extends Item {

    public ItemPigshoes(Item.Settings props) {
        super(props);
    }

    @Override
    public int getEnchantability() {
        return 1;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    // TODO make this an ArmorItem
    // FIXME forge
//    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
//        return enchantment.category == EnchantmentCategory.ARMOR_FEET && !enchantment.isCurse() && enchantment != Enchantments.UNBREAKING && enchantment != Enchantments.MENDING;
//    }
}
