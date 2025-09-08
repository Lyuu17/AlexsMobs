package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.entity.EntityCockroachEgg;
import com.github.alexthe666.alexsmobs.entity.EntityEmuEgg;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

import java.util.Random;

public class ItemAnimalEgg extends Item {

    private final Random random = new Random();

    public ItemAnimalEgg(Settings properties) {
        super(properties);
    }

    @Override
    public TypedActionResult<ItemStack> use(World worldIn, PlayerEntity playerIn, Hand handIn) {
        var itemstack = playerIn.getStackInHand(handIn);
        playerIn.emitGameEvent(GameEvent.ITEM_INTERACT_START);
        worldIn.playSound(null, playerIn.getX(), playerIn.getY(), playerIn.getZ(), SoundEvents.ENTITY_EGG_THROW, SoundCategory.PLAYERS, 0.5F, 0.4F / (random.nextFloat() * 0.4F + 0.8F));
        if (!worldIn.isClient) {
            ThrownItemEntity eggentity;
            if(this == AMItemRegistry.EMU_EGG.get()){
                eggentity = new EntityEmuEgg(worldIn, playerIn);
            }else{
                eggentity = new EntityCockroachEgg(worldIn, playerIn);
            }
            eggentity.setItem(itemstack);
            eggentity.setVelocity(playerIn, playerIn.getPitch(), playerIn.getYaw(), 0.0F, 1.5F, 1.0F);
            worldIn.spawnEntity(eggentity);
        }

        playerIn.incrementStat(Stats.USED.getOrCreateStat(this));
        if (!playerIn.getAbilities().creativeMode) {
            itemstack.decrement(1);
        }

        return TypedActionResult.success(itemstack, worldIn.isClient());
    }
}
