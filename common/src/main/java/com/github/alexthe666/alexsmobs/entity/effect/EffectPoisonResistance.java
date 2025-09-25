package com.github.alexthe666.alexsmobs.entity.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffects;

public class EffectPoisonResistance extends StatusEffect {

    public EffectPoisonResistance() {
        super(StatusEffectCategory.BENEFICIAL, 0X51FFAF);
    }

    @Override
    public void applyUpdateEffect(LivingEntity LivingEntityIn, int amplifier) {
        if(LivingEntityIn.hasStatusEffect(StatusEffects.POISON)){
            LivingEntityIn.removeStatusEffect(StatusEffects.POISON);
        }
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return duration > 0;
    }

    @Override
    public String getTranslationKey() {
        return "alexsmobs.potion.poison_resistance";
    }

}
