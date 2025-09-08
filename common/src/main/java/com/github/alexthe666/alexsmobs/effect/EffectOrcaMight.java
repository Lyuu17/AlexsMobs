package com.github.alexthe666.alexsmobs.effect;

import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

public class EffectOrcaMight extends StatusEffect {

    public EffectOrcaMight() {
        super(StatusEffectCategory.BENEFICIAL, 0X4A4A52);
        this.addAttributeModifier(EntityAttributes.GENERIC_ATTACK_SPEED, "03C3C89D-7037-4B42-869F-B146BCB64D3A", 3D, EntityAttributeModifier.Operation.ADDITION);
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return duration > 0;
    }

    @Override
    public String getTranslationKey() {
        return "alexsmobs.potion.orcas_might";
    }

}