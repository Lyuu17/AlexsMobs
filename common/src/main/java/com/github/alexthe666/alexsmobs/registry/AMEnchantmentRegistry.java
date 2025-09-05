package com.github.alexthe666.alexsmobs.registry;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.enchantment.StraddleEnchantment;
import com.github.alexthe666.alexsmobs.enchantment.StraddleJumpEnchantment;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.registry.RegistryKeys;

public class AMEnchantmentRegistry {

    public static final DeferredRegister<Enchantment> DEF_REG = DeferredRegister.create(AlexsMobs.MOD_ID, RegistryKeys.ENCHANTMENT);

    public static final RegistrySupplier<Enchantment> STRADDLE_JUMP = DEF_REG.register("straddle_jump", () -> new StraddleJumpEnchantment(Enchantment.Rarity.COMMON, EnchantmentTarget.BREAKABLE, EquipmentSlot.MAINHAND));
    public static final RegistrySupplier<Enchantment> STRADDLE_LAVAWAX = DEF_REG.register("lavawax", () -> new StraddleEnchantment(Enchantment.Rarity.UNCOMMON, EnchantmentTarget.BREAKABLE, EquipmentSlot.MAINHAND));
    public static final RegistrySupplier<Enchantment> STRADDLE_SERPENTFRIEND = DEF_REG.register("serpentfriend", () -> new StraddleEnchantment(Enchantment.Rarity.RARE, EnchantmentTarget.BREAKABLE, EquipmentSlot.MAINHAND));
    public static final RegistrySupplier<Enchantment> STRADDLE_BOARDRETURN = DEF_REG.register("board_return", () -> new StraddleEnchantment(Enchantment.Rarity.UNCOMMON, EnchantmentTarget.BREAKABLE, EquipmentSlot.MAINHAND));
}
