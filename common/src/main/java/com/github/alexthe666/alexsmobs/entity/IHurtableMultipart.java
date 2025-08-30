package com.github.alexthe666.alexsmobs.entity;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public interface IHurtableMultipart {

    void onAttackedFromServer(LivingEntity parent, float damage, DamageSource damageSource);

    default Vec3d calcOffsetVec(float offsetZ, float xRot, float yRot){
        return new Vec3d(0, 0, offsetZ).rotateX(xRot * MathHelper.RADIANS_PER_DEGREE).rotateY(-yRot * MathHelper.RADIANS_PER_DEGREE);
    }

    default float limitAngle(float sourceAngle, float targetAngle, float maximumChange) {
        float f = MathHelper.wrapDegrees(targetAngle - sourceAngle);
        if (f > maximumChange) {
            f = maximumChange;
        }

        if (f < -maximumChange) {
            f = -maximumChange;
        }

        float f1 = sourceAngle + f;
        if (f1 < 0.0F) {
            f1 += 360.0F;
        } else if (f1 > 360.0F) {
            f1 -= 360.0F;
        }

        return f1;
    }
}
