package com.github.alexthe666.alexsmobs.entity.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

public class EffectKnockbackResistance extends StatusEffect {

    public EffectKnockbackResistance() {
        super(StatusEffectCategory.BENEFICIAL, 0X865337);
        this.addAttributeModifier(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, "03C3C89D-7037-4B42-869F-B146BCB64D2F", 0.5D, EntityAttributeModifier.Operation.ADDITION);
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
        return "alexsmobs.potion.knockback_resistance";
    }

}
