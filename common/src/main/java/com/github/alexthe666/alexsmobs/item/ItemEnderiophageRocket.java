package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.entity.EntityEnderiophageRocket;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class ItemEnderiophageRocket extends Item {

    public ItemEnderiophageRocket(Item.Settings group) {
        super(group);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        var world = context.getWorld();
        if (!world.isClient) {
            var itemstack = context.getStack();
            var vector3d = context.getHitPos();
            var direction = context.getSide();
            var fireworkrocketentity = new EntityEnderiophageRocket(world, context.getPlayer(), vector3d.x + (double)direction.getOffsetX() * 0.15D, vector3d.y + (double)direction.getOffsetY() * 0.15D, vector3d.z + (double)direction.getOffsetZ() * 0.15D, itemstack);
            world.spawnEntity(fireworkrocketentity);
            if(!context.getPlayer().isCreative()){
                itemstack.decrement(1);
            }
        }
        return ActionResult.success(world.isClient);
    }

    @Override
    public TypedActionResult<ItemStack> use(World worldIn, PlayerEntity playerIn, Hand handIn) {
        if (playerIn.isFallFlying()) {
            ItemStack itemstack = playerIn.getStackInHand(handIn);
            if (!worldIn.isClient) {
                worldIn.spawnEntity(new EntityEnderiophageRocket(worldIn, itemstack, playerIn));
                if (!playerIn.getAbilities().creativeMode) {
                    itemstack.decrement(1);
                }
            }

            return TypedActionResult.success(playerIn.getStackInHand(handIn), worldIn.isClient());
        } else {
            return TypedActionResult.pass(playerIn.getStackInHand(handIn));
        }
    }

}
