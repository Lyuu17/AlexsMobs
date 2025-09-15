package com.github.alexthe666.alexsmobs.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public class EffectSunbird extends StatusEffect {
    public final boolean curse;

    public EffectSunbird(boolean curse) {
        super(curse ? StatusEffectCategory.HARMFUL : StatusEffectCategory.BENEFICIAL, 0XFFEAB9);
        this.curse = curse;
    }

    @Override
    public void applyUpdateEffect(LivingEntity entity, int amplifier) {
        if (curse) {
            if (entity.isFallFlying()) {
                if (entity instanceof PlayerEntity) {
                    ((PlayerEntity) entity).stopFallFlying();
                }
            }
            boolean forceFall = false;
            if (entity instanceof PlayerEntity player) {
                if (!player.isCreative() || !player.getAbilities().flying) {
                    forceFall = true;
                }
            }
            if ((forceFall || !(entity instanceof PlayerEntity)) && !entity.isOnGround()) {
                entity.setVelocity(entity.getVelocity().add(0, -0.2F, 0));
            }
        } else {
            entity.fallDistance = 0.0F;
            if (entity.isFallFlying()) {
                if (entity.getPitch() < -10) {
                    float pitchMulti = Math.abs(entity.getPitch()) / 90F;
                    entity.setVelocity(entity.getVelocity().add(0, 0.02 + pitchMulti * 0.02, 0));
                }
            } else if (!entity.isOnGround() && !entity.isInSneakingPose()) {
                Vec3d vector3d = entity.getVelocity();
                if (vector3d.y < 0.0D) {
                    entity.setVelocity(vector3d.multiply(1.0D, 0.6D, 1.0D));
                }
            }

        }
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return duration > 0;
    }

    @Override
    public String getTranslationKey() {
        return curse ? "alexsmobs.potion.sunbird_curse" : "alexsmobs.potion.sunbird_blessing";
    }

}