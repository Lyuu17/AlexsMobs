package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class ItemTarantulaHawkElytra extends ArmorItem {

    public ItemTarantulaHawkElytra(Item.Settings props, AMArmorMaterial mat) {
        super(mat, Type.CHESTPLATE, props);
    }

    public static boolean isUsable(ItemStack stack) {
        return stack.getDamage() < stack.getMaxDamage() - 1;
    }

    @Override
    public TypedActionResult<ItemStack> use(World worldIn, PlayerEntity playerIn, Hand handIn) {
        return super.use(worldIn, playerIn, handIn);
    }

    // FIXME forge?
//    @Override
//    public boolean canElytraFly(ItemStack stack, LivingEntity entity) {
//        return ElytraItem.isUsable(stack);
//    }
//
//    @Override
//    public boolean elytraFlightTick(ItemStack stack, LivingEntity entity, int flightTicks) {
//        if (!entity.getWorld().isClient && (flightTicks + 1) % 20 == 0) {
//            stack.damage(1, entity, e -> e.sendEquipmentBreakStatus(EquipmentSlot.CHEST));
//        }
//        return true;
//    }

    @Override
    public boolean canRepair(ItemStack toRepair, ItemStack repair) {
        return repair.getItem() == AMItemRegistry.TARANTULA_HAWK_WING_FRAGMENT.get();
    }

    @Override
    public EquipmentSlot getSlotType() {
        return EquipmentSlot.CHEST;
    }

    // FIXME forge
//    @Nullable
//    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
//        return "alexsmobs:textures/armor/tarantula_hawk_elytra.png";
//    }
}
