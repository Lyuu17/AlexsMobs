package com.github.alexthe666.alexsmobs.enchantment;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.item.ItemStraddleboard;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

public class StraddleEnchantment extends Enchantment {

    public StraddleEnchantment(Rarity r, EnchantmentTarget type, EquipmentSlot... types) {
        super(r, type, types);
    }

    @Override
    public int getMinPower(int i) {
        return 6 + (i + 1) * 6;
    }

    @Override
    public int getMaxPower(int i) {
        return super.getMaxPower(i) + 10;
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    @Override
    public boolean isAvailableForEnchantedBookOffer() {
        return super.isAvailableForEnchantedBookOffer() && AMConfig.straddleboardEnchants;
    }

    @Override
    public boolean isAvailableForRandomSelection() {
        return super.isAvailableForRandomSelection() && AMConfig.straddleboardEnchants;
    }

    @Override
    public boolean isAcceptableItem(ItemStack stack) {
        return stack.getItem() instanceof ItemStraddleboard;
    }
}
