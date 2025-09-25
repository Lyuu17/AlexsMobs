package com.github.alexthe666.alexsmobs.entity.effect;

import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.world.event.GameEvent;

public class EffectPowerDown extends StatusEffect {

    private int lastDuration = -1;
    private int firstDuration = -1;

    public EffectPowerDown() {
        super(StatusEffectCategory.NEUTRAL, 0x00000);
        this.addAttributeModifier(EntityAttributes.GENERIC_MOVEMENT_SPEED, "7107DE5E-7CE8-4030-940E-514C1F160890", -1.0F, EntityAttributeModifier.Operation.MULTIPLY_BASE);
    }

    @Override
    public void applyUpdateEffect(LivingEntity entity, int amplifier) {
        if(entity.getVelocity().y > 0 && !entity.isInsideWaterOrBubbleColumn()){
            entity.setVelocity(entity.getVelocity().multiply(1, 0, 1));
        }
        if(firstDuration == lastDuration){
            entity.playSound(AMSoundRegistry.APRIL_FOOLS_POWER_OUTAGE.get(), 1.5F, 1);
            entity.emitGameEvent(GameEvent.ENTITY_ROAR);
        }
    }

    public int getActiveTime(){
        return firstDuration - lastDuration;
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        lastDuration = duration;
        if(duration <= 0){
            lastDuration = -1;
            firstDuration = -1;
        }
        if(firstDuration == -1){
            firstDuration = duration;
        }
        return duration > 0;
    }

    @Override
    public void onRemoved(LivingEntity entity, AttributeContainer map, int i) {
        lastDuration = -1;
        firstDuration = -1;
        super.onRemoved(entity, map, i);
    }

    @Override
    public void onApplied(LivingEntity entity, AttributeContainer map, int i) {
        lastDuration = -1;
        firstDuration = -1;
        super.onApplied(entity, map, i);
    }

    @Override
    public String getTranslationKey() {
        return "alexsmobs.potion.power_down";
    }
}
