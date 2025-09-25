package com.github.alexthe666.alexsmobs.entity.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

public class EffectLavaVision extends StatusEffect {

    public EffectLavaVision() {
        super(StatusEffectCategory.BENEFICIAL, 0XFF6A00);
    }

    @Override
    public void applyUpdateEffect(LivingEntity LivingEntityIn, int amplifier) {
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return duration > 0;
    }

    @Override
    public String getTranslationKey() {
        return "alexsmobs.potion.lava_vision";
    }

}