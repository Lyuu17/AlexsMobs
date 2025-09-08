package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

import java.util.Random;

public class ItemMaraca extends Item {

    private final Random random = new Random();

    public ItemMaraca(Item.Settings property) {
        super(property);
    }

    public TypedActionResult<ItemStack> use(World worldIn, PlayerEntity playerIn, Hand handIn) {
        var itemstack = playerIn.getStackInHand(handIn);
        playerIn.emitGameEvent(GameEvent.ITEM_INTERACT_START);
        worldIn.playSound(null, playerIn.getX(), playerIn.getY(), playerIn.getZ(), AMSoundRegistry.MARACA.get(), SoundCategory.PLAYERS, 0.5F, (random.nextFloat() * 0.4F + 0.8F));
        playerIn.getItemCooldownManager().set(this, 3);
        playerIn.incrementStat(Stats.USED.getOrCreateStat(this));
        return TypedActionResult.success(itemstack);
    }
}
