package com.github.alexthe666.alexsmobs.entity.ai;

import net.minecraft.entity.Flutterer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;

import java.util.EnumSet;

public class TameableAIRide extends Goal {

    private final PathAwareEntity tameableEntity;
    private LivingEntity player;
    private final double speed;
    private final boolean strafe;

    public TameableAIRide(PathAwareEntity dragon, double speed) {
        this(dragon, speed, true);
    }

    public TameableAIRide(PathAwareEntity dragon, double speed, boolean strafe) {
        this.tameableEntity = dragon;
        this.speed = speed;
        this.strafe = strafe;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (tameableEntity.getControllingPassenger() instanceof PlayerEntity && tameableEntity.hasPassengers()) {
            player = tameableEntity.getControllingPassenger();
            return true;
        } else {
            tameableEntity.setSprinting(false);
            return false;
        }
    }

    @Override
    public void start() {
        tameableEntity.getNavigation().stop();
    }

    @Override
    public void tick() {
        tameableEntity.setStepHeight(1F);
        tameableEntity.getNavigation().stop();
        tameableEntity.setTarget(null);
        double x = tameableEntity.getX();
        double y = tameableEntity.getY();
        double z = tameableEntity.getZ();
        if (strafe) {
            tameableEntity.sidewaysSpeed = player.sidewaysSpeed * 0.15F;
        }
        if (shouldMoveForward() && tameableEntity.hasPassengers()) {
            tameableEntity.setSprinting(true);
            var lookVec = player.getRotationVector();
            if (shouldMoveBackwards()) {
                lookVec = lookVec.rotateY(MathHelper.PI);
            }
            x += lookVec.x * 10;
            z += lookVec.z * 10;
            y += modifyYPosition(lookVec.y);
            tameableEntity.getMoveControl().moveTo(x, y, z, speed);
        } else {
            tameableEntity.setSprinting(false);
        }
    }

    public double modifyYPosition(double lookVecY) {
        return tameableEntity instanceof Flutterer ? lookVecY * 10 : 0;
    }

    public boolean shouldMoveForward() {
        return player.forwardSpeed != 0;
    }

    public boolean shouldMoveBackwards() {
        return player.forwardSpeed < 0;
    }
}
