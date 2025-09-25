package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.entity.EntitySandShot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

import java.util.function.Predicate;

public class ItemPocketSand extends Item {

    public static final Predicate<ItemStack> IS_SAND = (stack) -> stack.isIn(ItemTags.SAND);

    public ItemPocketSand(Settings properties) {
        super(properties);
    }

    public ItemStack findAmmo(PlayerEntity entity) {
        if(entity.isCreative()){
            return ItemStack.EMPTY;
        }
        for(int i = 0; i < entity.getInventory().size(); ++i) {
            var itemstack1 = entity.getInventory().getStack(i);
            if (IS_SAND.test(itemstack1)) {
                return itemstack1;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public TypedActionResult<ItemStack> use(World worldIn, PlayerEntity livingEntityIn, Hand handIn) {
        ItemStack itemstack = livingEntityIn.getStackInHand(handIn);
        ItemStack ammo = findAmmo(livingEntityIn);
        if(livingEntityIn.isCreative()){
            ammo = new ItemStack(Items.SAND);
        }
        if (!worldIn.isClient && !ammo.isEmpty()) {
            livingEntityIn.emitGameEvent(GameEvent.ITEM_INTERACT_START);
            worldIn.playSound(null, livingEntityIn.getX(), livingEntityIn.getY(), livingEntityIn.getZ(), SoundEvents.BLOCK_SAND_BREAK, SoundCategory.PLAYERS, 0.5F, 0.4F + (livingEntityIn.getRandom().nextFloat() * 0.4F + 0.8F));
            boolean left = false;
            if (livingEntityIn.getActiveHand() == Hand.OFF_HAND && livingEntityIn.getMainArm() == Arm.RIGHT || livingEntityIn.getActiveHand() == Hand.MAIN_HAND && livingEntityIn.getMainArm() == Arm.LEFT) {
                left = true;
            }
            EntitySandShot blood = new EntitySandShot(worldIn, livingEntityIn, !left);
            Vec3d vector3d = livingEntityIn.getRotationVec(1.0F);
            blood.shoot(vector3d.x, vector3d.y, vector3d.z, 1.2F, 11);
            if (!worldIn.isClient) {
                worldIn.spawnEntity(blood);
            }
            livingEntityIn.getItemCooldownManager().set(this, 2);
            ammo.decrement(1);
            itemstack.damage(1, livingEntityIn, (player) -> {
                player.sendToolBreakStatus(livingEntityIn.getActiveHand());
            });
        }
        livingEntityIn.incrementStat(Stats.USED.getOrCreateStat(this));
        return TypedActionResult.success(itemstack, worldIn.isClient());
    }


}
