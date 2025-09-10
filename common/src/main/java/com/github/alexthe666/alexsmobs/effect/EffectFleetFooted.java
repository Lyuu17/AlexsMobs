package com.github.alexthe666.alexsmobs.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

import java.util.UUID;

public class EffectFleetFooted extends StatusEffect {

    private static final UUID SPRINT_JUMP_SPEED_MODIFIER = UUID.fromString("7E0292F2-9434-48D5-A29F-9583AF7DF29A");
    private static final EntityAttributeModifier SPRINT_JUMP_SPEED_BONUS = new EntityAttributeModifier(SPRINT_JUMP_SPEED_MODIFIER, "fleetfooted speed bonus", 0.2F, EntityAttributeModifier.Operation.ADDITION);
    private int lastDuration = -1;
    private int removeEffectAfter = 0;

    public EffectFleetFooted() {
        super(StatusEffectCategory.BENEFICIAL, 0X685441);
    }

    @Override
    public void applyUpdateEffect(LivingEntity entity, int amplifier) {
        var modifiableattributeinstance = entity.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        boolean applyEffect = entity.isSprinting() && !entity.isOnGround() && lastDuration > 2;
        if(removeEffectAfter > 0){
            removeEffectAfter--;
        }
        if (applyEffect) {
            if(!modifiableattributeinstance.hasModifier(SPRINT_JUMP_SPEED_BONUS)){
                modifiableattributeinstance.addPersistentModifier(SPRINT_JUMP_SPEED_BONUS);
            }
            removeEffectAfter = 5;
        }
        if (removeEffectAfter <= 0 || lastDuration < 2) {
            modifiableattributeinstance.removeModifier(SPRINT_JUMP_SPEED_BONUS);
        }
    }

    @Override
    public void onRemoved(LivingEntity livingEntity, AttributeContainer attributeMap, int level) {
        var modifiableattributeinstance = livingEntity.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if(modifiableattributeinstance != null && modifiableattributeinstance.hasModifier(SPRINT_JUMP_SPEED_BONUS)){
            modifiableattributeinstance.removeModifier(SPRINT_JUMP_SPEED_BONUS);
        }
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        lastDuration = duration;
        return duration > 0;
    }

    @Override
    public String getTranslationKey() {
        return "alexsmobs.potion.fleet_footed";
    }

}