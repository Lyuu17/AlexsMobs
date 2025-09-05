package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.entity.EntityStraddleboard;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DyeableItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.List;
import java.util.function.Predicate;

public class ItemStraddleboard extends Item implements DyeableItem {

    private static final Predicate<? super Entity> ENTITY_PREDICATE = EntityPredicates.EXCEPT_SPECTATOR.and(Entity::canHit);

    public ItemStraddleboard(Item.Settings properties) {
        super(properties);
    }

    @Override
    public int getColor(ItemStack p_200886_1_) {
        NbtCompound lvt_2_1_ = p_200886_1_.getSubNbt("display");
        return lvt_2_1_ != null && lvt_2_1_.contains("color", 99) ? lvt_2_1_.getInt("color") : 0XADC3D7;
    }

    //FIXME forge
//    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
//        return super.canApplyAtEnchantingTable(stack, enchantment) && enchantment != Enchantments.UNBREAKING && enchantment != Enchantments.MENDING;
//    }

    @Override
    public int getEnchantability() {
        return 1;
    }

    @Override
    public TypedActionResult<ItemStack> use(World worldIn, PlayerEntity playerIn, Hand handIn) {
        var itemstack = playerIn.getStackInHand(handIn);
        var raytraceresult = raycast(worldIn, playerIn, RaycastContext.FluidHandling.ANY);
        if (raytraceresult.getType() == HitResult.Type.MISS) {
            return TypedActionResult.pass(itemstack);
        } else {
            var vector3d = playerIn.getRotationVec(1.0F);
            List<Entity> list = worldIn.getOtherEntities(playerIn, playerIn.getBoundingBox().stretch(vector3d.multiply(5.0D)).expand(1.0D), ENTITY_PREDICATE);
            if (!list.isEmpty()) {
                var vector3d1 = playerIn.getCameraPosVec(1.0F);
                for (Entity entity : list) {
                    var axisalignedbb = entity.getBoundingBox().expand(entity.getTargetingMargin());
                    if (axisalignedbb.contains(vector3d1)) {
                        return TypedActionResult.pass(itemstack);
                    }
                }
            }

            if (raytraceresult.getType() == HitResult.Type.BLOCK) {
                var boatentity = new EntityStraddleboard(worldIn, raytraceresult.getPos().x, raytraceresult.getPos().y, raytraceresult.getPos().z);
                boatentity.setDefaultColor(!this.hasColor(itemstack));
                boatentity.setItemStack(itemstack.copy());
                boatentity.setColor(this.getColor(itemstack));
                boatentity.setYaw(playerIn.getYaw());
                if (!worldIn.isSpaceEmpty(boatentity, boatentity.getBoundingBox().expand(-0.1D))) {
                    return TypedActionResult.fail(itemstack);
                } else {
                    if (!worldIn.isClient) {
                        worldIn.spawnEntity(boatentity);
                        if (!playerIn.getAbilities().creativeMode) {
                            itemstack.decrement(1);
                        }
                    }

                    playerIn.incrementStat(Stats.USED.getOrCreateStat(this));
                    return TypedActionResult.success(itemstack, worldIn.isClient());
                }
            } else {
                return TypedActionResult.pass(itemstack);
            }
        }
    }
}
