package com.github.alexthe666.alexsmobs.entity.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

public class EffectFear extends StatusEffect {

    public EffectFear() {
        super(StatusEffectCategory.NEUTRAL, 0X7474F7);
        this.addAttributeModifier(EntityAttributes.GENERIC_MOVEMENT_SPEED, "7107DE5E-7CE8-4030-940E-514C1F160890", -1.0F, EntityAttributeModifier.Operation.MULTIPLY_BASE);
    }

    @Override
    public void applyUpdateEffect(LivingEntity entity, int amplifier) {
        if(entity.getVelocity().y > 0 && !entity.isInsideWaterOrBubbleColumn()){
            entity.setVelocity(entity.getVelocity().multiply(1, 0, 1));
        }
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return duration > 0;
    }

    @Override
    public String getTranslationKey() {
        return "alexsmobs.potion.fear";
    }
}
