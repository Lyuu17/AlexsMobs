package com.github.alexthe666.alexsmobs.entity.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

public class EffectOiled extends StatusEffect {

    public EffectOiled() {
        super(StatusEffectCategory.BENEFICIAL, 0XFFE89C);
    }

    @Override
    public void applyUpdateEffect(LivingEntity entity, int amplifier) {
       if (entity.isWet()){
           if(!entity.isSneaking()){
               entity.setVelocity(entity.getVelocity().add(0, 0.1D, 0));
           }else{
               entity.fallDistance = 0;
           }
           if (!entity.isOnGround()) {
               var vector3d = entity.getVelocity();
               entity.setVelocity(vector3d.multiply(1.0D, 0.9D, 1.0D));

           }
       }
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return duration > 0;
    }

    @Override
    public String getTranslationKey() {
        return "alexsmobs.potion.oiled";
    }

}