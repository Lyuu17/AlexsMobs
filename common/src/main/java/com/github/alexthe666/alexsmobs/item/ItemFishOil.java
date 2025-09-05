package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.registry.AMEffectRegistry;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

public class ItemFishOil extends Item {
    public ItemFishOil(Settings p_i225737_1_) {
        super(p_i225737_1_);
    }

    @Override
    public ItemStack finishUsing(ItemStack p_77654_1_, World p_77654_2_, LivingEntity p_77654_3_) {
        super.finishUsing(p_77654_1_, p_77654_2_, p_77654_3_);
        if(AMConfig.fishOilMeme){
            p_77654_3_.addStatusEffect(new StatusEffectInstance(AMEffectRegistry.OILED.get(), 1200, 0));
        }
        if (p_77654_3_ instanceof ServerPlayerEntity lvt_4_1_) {
            Criteria.CONSUME_ITEM.trigger(lvt_4_1_, p_77654_1_);
            lvt_4_1_.incrementStat(Stats.USED.getOrCreateStat(this));
        }

        if (p_77654_1_.isEmpty()) {
            return new ItemStack(Items.GLASS_BOTTLE);
        } else {
            if (p_77654_3_ instanceof PlayerEntity lvt_5_1_ && !lvt_5_1_.getAbilities().creativeMode) {
                ItemStack lvt_4_2_ = new ItemStack(Items.GLASS_BOTTLE);
                if (!lvt_5_1_.getInventory().insertStack(lvt_4_2_)) {
                    lvt_5_1_.dropItem(lvt_4_2_, false);
                }
            }

            return p_77654_1_;
        }
    }

    @Override
    public int getMaxUseTime(ItemStack p_77626_1_) {
        return 40;
    }

    @Override
    public UseAction getUseAction(ItemStack p_77661_1_) {
        return UseAction.DRINK;
    }

    @Override
    public SoundEvent getDrinkSound() {
        return SoundEvents.ITEM_HONEY_BOTTLE_DRINK;
    }

    @Override
    public SoundEvent getEatSound() {
        return SoundEvents.ITEM_HONEY_BOTTLE_DRINK;
    }

    @Override
    public TypedActionResult<ItemStack> use(World level, PlayerEntity player, Hand interactionHand) {
        return ItemUsage.consumeHeldItem(level, player, interactionHand);
    }
}
