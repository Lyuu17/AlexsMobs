package com.github.alexthe666.alexsmobs.enchantment;

import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.EquipmentSlot;

public class StraddleJumpEnchantment extends StraddleEnchantment {

    public StraddleJumpEnchantment(Rarity rarity, EnchantmentTarget enchantmentTarget, EquipmentSlot... types) {
        super(rarity, enchantmentTarget, types);
    }

    @Override
    public int getMinPower(int i) {
        return 4 + (i - 1) * 5;
    }

    @Override
    public int getMaxPower(int i) {
        return super.getMaxPower(i) + 10;
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }
}
