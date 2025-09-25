package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.entity.util.RainbowUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

public class ItemRainbowJelly extends Item {

    public ItemRainbowJelly(Item.Settings tab) {
        super(tab);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity playerIn, LivingEntity target, Hand hand) {
        int i = RainbowUtil.getRainbowTypeFromStack(stack);
        if (RainbowUtil.getRainbowType(target) != i) {
            RainbowUtil.setRainbowType(target, i);
            var random = playerIn.getRandom();
            for (int j = 0; j < 6 + random.nextInt(3); j++) {
                double d2 = random.nextGaussian() * 0.02D;
                double d0 = random.nextGaussian() * 0.02D;
                double d1 = random.nextGaussian() * 0.02D;
                playerIn.getWorld().addParticle(new ItemStackParticleEffect(ParticleTypes.ITEM, stack), target.getX() + (double) (random.nextFloat() * target.getWidth()) - (double) target.getWidth() * 0.5F, target.getY() + target.getHeight() * 0.5F + (double) (random.nextFloat() * target.getHeight() * 0.5F), target.getZ() + (double) (random.nextFloat() * target.getWidth()) - (double) target.getWidth() * 0.5F, d0, d1, d2);
            }
            target.emitGameEvent(GameEvent.ITEM_INTERACT_START);
            target.playSound(SoundEvents.ENTITY_SLIME_SQUISH_SMALL, 1F, target.getSoundPitch());
            if (!playerIn.isCreative()) {
                stack.decrement(1);
            }
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    @Override
    public ItemStack finishUsing(ItemStack st, World level, LivingEntity e) {
        RainbowUtil.setRainbowType(e, RainbowUtil.getRainbowTypeFromStack(st));
        return this.isFood() ? e.eatFood(level, st) : st;
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        if (stack.getItem().isFood()) {
            return 64;
        } else {
            return 0;
        }
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return super.hasGlint(stack) || RainbowUtil.getRainbowTypeFromStack(stack) > 1;
    }

    public enum RainbowType {
        RAINBOW, TRANS, NONBI, BI, ACE, WEEZER, BRAZIL;

        public static RainbowType getFromString(String name) {
            if (name.contains("nonbi") || name.contains("non-bi")) {
                return NONBI;
            }else if (name.contains("trans")) {
                return TRANS;
            }else if (name.contains("bi")) {
                return BI;
            }else if (name.contains("asexual") || name.contains("ace")) {
                return ACE;
            }else if (name.contains("weezer")) {
                return WEEZER;
            }else if (name.contains("brazil")) {
                return BRAZIL;
            }
            return RAINBOW;
        }
    }
}
