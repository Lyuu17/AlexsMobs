package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

import java.util.Random;

public class ItemBearDust extends Item implements CustomTabBehavior{

    private final Random random = new Random();

    public ItemBearDust(Item.Settings props) {
        super(props);
    }

    @Override
    public TypedActionResult<ItemStack> use(World worldIn, PlayerEntity playerIn, Hand handIn) {
        var itemstack = playerIn.getStackInHand(handIn);
        playerIn.emitGameEvent(GameEvent.ITEM_INTERACT_START);
        worldIn.playSound(null, playerIn.getX(), playerIn.getY(), playerIn.getZ(), AMSoundRegistry.BEAR_DUST.get(), SoundCategory.PLAYERS, 0.75F, (random.nextFloat() * 0.2F + 0.9F));
        playerIn.getItemCooldownManager().set(this, 3);
        playerIn.incrementStat(Stats.USED.getOrCreateStat(this));
        return TypedActionResult.success(itemstack);
    }

    @Override
    public void fillItemCategory(ItemGroup.Entries contents) {
    }
}
