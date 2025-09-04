package com.github.alexthe666.alexsmobs.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

public class EffectMosquitoRepellent extends StatusEffect {

    public EffectMosquitoRepellent() {
        super(StatusEffectCategory.BENEFICIAL, 0XCC7E70);
    }

    public void applyEffectTick(LivingEntity entity, int amplifier) {
    }

    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration > 0;
    }

    public String getDescriptionId() {
        return "alexsmobs.potion.mosquito_repellent";
    }
}
