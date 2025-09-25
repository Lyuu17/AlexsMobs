package com.github.alexthe666.alexsmobs.entity.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

public class EffectTigersBlessing extends StatusEffect {

    public EffectTigersBlessing() {
        super(StatusEffectCategory.BENEFICIAL, 0XFFD75E);
    }

    @Override
    public void applyUpdateEffect(LivingEntity entity, int amplifier) {
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return duration > 0;
    }

    @Override
    public String getTranslationKey() {
        return "alexsmobs.potion.tigers_blessing";
    }
}
