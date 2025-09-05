package com.github.alexthe666.alexsmobs.effect;

import com.github.alexthe666.alexsmobs.entity.EntityEnderiophage;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

public class EffectEnderFlu extends StatusEffect {

    private int lastDuration = -1;

    public EffectEnderFlu() {
        super(StatusEffectCategory.HARMFUL, 0X6836AA);
    }

    public void applyUpdateEffect(LivingEntity entity, int amplifier) {
        if (lastDuration == 1) {
            int phages = amplifier + 1;
            entity.damage(entity.getDamageSources().magic(), phages * 10);
            for (int i = 0; i < phages; i++) {
                EntityEnderiophage phage = AMEntityRegistry.ENDERIOPHAGE.get().create(entity.getWorld());
                phage.copyPositionAndRotation(entity);
                phage.onSpawnFromEffect();
                phage.setSkinForDimension();
                if (!entity.getWorld().isClient) {
                    phage.setStandardFleeTime();
                    entity.getWorld().spawnEntity(phage);
                }
            }
        }
    }

    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        lastDuration = duration;
        return duration > 0;
    }

    public String getTranslationKey() {
        return "alexsmobs.potion.ender_flu";
    }

}