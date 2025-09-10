package com.github.alexthe666.alexsmobs.entity.ai;

import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class EtherealMoveController extends MoveControl {

    private final MobEntity parentEntity;
    private final float speedGeneral;

    public EtherealMoveController(MobEntity parentEntity, float speedGeneral) {
        super(parentEntity);
        this.parentEntity = parentEntity;
        this.speedGeneral = speedGeneral;
    }

    @Override
    public void tick() {
        if (this.state == MoveControl.State.MOVE_TO) {
            Vec3d vector3d = new Vec3d(this.targetX - parentEntity.getX(), this.targetY - parentEntity.getY(), this.targetZ - parentEntity.getZ());
            double d0 = vector3d.length();

            parentEntity.setVelocity(parentEntity.getVelocity().add(vector3d.multiply(this.speed * speedGeneral * 0.025D / d0)));

            double yAdd = this.targetY - parentEntity.getY();
            if(d0 > parentEntity.getWidth()){
                parentEntity.setVelocity(parentEntity.getVelocity().add(0.0D, (double) parentEntity.getMovementSpeed() * speedGeneral * MathHelper.clamp(yAdd, -1, 1) * 0.6F, 0.0D));
                Vec3d vector3d1 = parentEntity.getVelocity();
                parentEntity.setYaw(-((float) MathHelper.atan2(vector3d1.x, vector3d1.z)) * MathHelper.DEGREES_PER_RADIAN);
                parentEntity.bodyYaw = parentEntity.getYaw();
            }
        } else if (this.state == State.STRAFE || this.state == State.JUMPING) {
            this.state = State.WAIT;
        }
    }

}
