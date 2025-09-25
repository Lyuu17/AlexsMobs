package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.entity.EntityMosquitoSpit;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

import java.util.function.Predicate;

public class ItemBloodSprayer extends Item {

    public static final Predicate<ItemStack> IS_BLOOD = (stack) -> stack.getItem() == AMItemRegistry.BLOOD_SAC.get();

    public ItemBloodSprayer(Item.Settings properties) {
        super(properties);
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return isUsable(stack) ? Integer.MAX_VALUE : 0;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BOW;
    }

    public static boolean isUsable(ItemStack stack) {
        return stack.getDamage() < stack.getMaxDamage() - 1;
    }

    @Override
    public TypedActionResult<ItemStack> use(World worldIn, PlayerEntity playerIn, Hand handIn) {

        var itemstack = playerIn.getStackInHand(handIn);
        playerIn.setCurrentHand(handIn);
        if(!isUsable(itemstack)){
            ItemStack ammo = findAmmo(playerIn);
            boolean flag = playerIn.isCreative();
            if(!ammo.isEmpty()){
                ammo.decrement(1);
                flag = true;
            }
            if(flag){
                itemstack.setDamage(0);
            }
        }
        return TypedActionResult.consume(itemstack);
    }

    public ItemStack findAmmo(PlayerEntity entity) {
        if(entity.isCreative()){
            return ItemStack.EMPTY;
        }
        for(int i = 0; i < entity.getInventory().size(); ++i) {
            var itemstack1 = entity.getInventory().getStack(i);
            if (IS_BLOOD.test(itemstack1)) {
                return itemstack1;
            }
        }
        return ItemStack.EMPTY;
    }

    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return !ItemStack.areItemsEqual(oldStack, newStack);
    }

    @Override
    public boolean isItemBarVisible(ItemStack itemStack) {
        return super.isItemBarVisible(itemStack) && isUsable(itemStack);
    }

    @Override
    public void usageTick(World worldIn, LivingEntity livingEntityIn, ItemStack stack, int count) {
        if(isUsable(stack)) {
            if (count % 2 == 0) {
                boolean left = false;
                if (livingEntityIn.getActiveHand() == Hand.OFF_HAND && livingEntityIn.getMainArm() == Arm.RIGHT || livingEntityIn.getActiveHand() == Hand.MAIN_HAND && livingEntityIn.getMainArm() == Arm.LEFT) {
                    left = true;
                }
                var blood = new EntityMosquitoSpit(worldIn, livingEntityIn, !left);
                var vector3d = livingEntityIn.getRotationVec(1.0F);
                var rand = worldIn.getRandom();
                livingEntityIn.emitGameEvent(GameEvent.ITEM_INTERACT_START);
                livingEntityIn.playSound(SoundEvents.BLOCK_LAVA_POP,1.0F, 1.2F + (rand.nextFloat() - rand.nextFloat()) * 0.2F);
                blood.shoot(vector3d.x, vector3d.y, vector3d.z, 1F, 10);
                if (!worldIn.isClient) {
                    worldIn.spawnEntity(blood);
                }
                stack.damage(1, livingEntityIn, (player) -> {
                    player.sendToolBreakStatus(livingEntityIn.getActiveHand());
                });
            }
        }else{
            if(livingEntityIn instanceof PlayerEntity){
                ItemStack ammo = findAmmo((PlayerEntity) livingEntityIn);
                boolean flag = ((PlayerEntity) livingEntityIn).isCreative();
                if(!ammo.isEmpty()){
                    ammo.decrement(1);
                    flag = true;
                }
                if(flag){
                    ((PlayerEntity) livingEntityIn).getItemCooldownManager().set(this, 20);
                    stack.setDamage(0);
                }
                livingEntityIn.stopUsingItem();
            }
        }
    }
}
