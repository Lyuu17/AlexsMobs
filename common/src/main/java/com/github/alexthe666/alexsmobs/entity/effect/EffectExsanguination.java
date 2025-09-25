package com.github.alexthe666.alexsmobs.entity.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

public class EffectExsanguination extends StatusEffect {

    private int lastDuration = -1;

    public EffectExsanguination() {
        super(StatusEffectCategory.HARMFUL, 0XED5151);
    }

    @Override
    public void applyUpdateEffect(LivingEntity entity, int amplifier) {
        entity.damage(entity.getDamageSources().magic(), Math.min(amplifier + 1, Math.round(lastDuration / 20F)));
        for(int i = 0; i < 3; i++){
            entity.getWorld().addParticle(net.minecraft.particle.ParticleTypes.DAMAGE_INDICATOR, entity.getParticleX(1.0), entity.getRandomBodyY(), entity.getParticleZ(1.0), 0, 0, 0);
        }
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        lastDuration = duration;
        return duration > 0 && duration % 20 == 0;
    }

    @Override
    public String getTranslationKey() {
        return "alexsmobs.potion.exsanguination";
    }

}
