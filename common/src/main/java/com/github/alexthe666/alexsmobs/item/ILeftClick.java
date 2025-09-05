package com.github.alexthe666.alexsmobs.item;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

public interface ILeftClick {

    boolean onLeftClick(ItemStack stack, LivingEntity playerIn);
}
