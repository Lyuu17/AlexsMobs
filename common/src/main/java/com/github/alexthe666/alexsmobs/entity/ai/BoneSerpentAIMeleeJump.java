package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntityBoneSerpent;
import net.minecraft.entity.ai.goal.DiveJumpingGoal;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class BoneSerpentAIMeleeJump extends DiveJumpingGoal {
    private final EntityBoneSerpent dolphin;
    private int attackCooldown = 0;
    private boolean inWater;

    public BoneSerpentAIMeleeJump(EntityBoneSerpent dolphin) {
        this.dolphin = dolphin;
    }

    @Override
    public boolean canStart() {
        if (this.dolphin.getTarget() == null || this.dolphin.isOnGround() || !dolphin.isInLava() && !dolphin.isTouchingWater() || dolphin.jumpCooldown> 0) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public boolean shouldContinue() {
        double d0 = this.dolphin.getVelocity().y;
        return dolphin.getTarget() != null && dolphin.jumpCooldown > 0 && (!(d0 * d0 < (double) 0.03F) || this.dolphin.getPitch() == 0.0F || !(Math.abs(this.dolphin.getPitch()) < 10.0F) || !this.dolphin.isTouchingWater()) && !this.dolphin.isOnGround();
    }

    @Override
    public boolean canStop() {
        return false;
    }

    @Override
    public void start() {
        var target = this.dolphin.getTarget();
        if(target != null){
            double distanceXZ = dolphin.squaredDistanceTo(target.getX(), dolphin.getY(), target.getZ());
            if(distanceXZ < 150){
                dolphin.lookAtEntity(target, 260, 30);
                final double smoothX = MathHelper.clamp(Math.abs(target.getX() - dolphin.getX()), 0, 1);
                //double smoothY = MathHelper.clamp(Math.abs(target.getY() - dolphin.getY()), 0, 1);
                final double smoothZ = MathHelper.clamp(Math.abs(target.getZ() - dolphin.getZ()), 0, 1);
                final double d0 = (target.getX() - this.dolphin.getX()) * 0.3 * smoothX;
                //double d1 = Math.signum(target.getY() - this.dolphin.getY());
                final double d2 = (target.getZ() - this.dolphin.getZ()) * 0.3 * smoothZ;
                final float up = 1F + dolphin.getRandom().nextFloat() * 0.8F;
                this.dolphin.setVelocity(this.dolphin.getVelocity().add((double) d0 * 0.3D, up, (double) d2 * 0.3D));
                this.dolphin.getNavigation().stop();
                this.dolphin.jumpCooldown = dolphin.getRandom().nextInt(32) + 64;
            }else{
                dolphin.getNavigation().startMovingTo(target, 1.0F);
            }

        }
    }

    @Override
    public void stop() {
        this.dolphin.setPitch(0.0F);
        this.attackCooldown = 0;
    }

    @Override
    public void tick() {
        boolean flag = this.inWater;
        if (!flag) {
            var fluidstate = this.dolphin.getWorld().getFluidState(this.dolphin.getBlockPos());
            this.inWater = fluidstate.isIn(FluidTags.LAVA) || fluidstate.isIn(FluidTags.WATER);
        }
        if(attackCooldown > 0){
            attackCooldown--;
        }
        if (this.inWater && !flag) {
            this.dolphin.playSound(SoundEvents.ENTITY_DOLPHIN_JUMP, 1.0F, 1.0F);
        }
        var target = this.dolphin.getTarget();
        if(target != null){
            if(this.dolphin.distanceTo(target) < 3F && attackCooldown <= 0){
                this.dolphin.tryAttack(target);
                attackCooldown = 20;
            }
        }

        Vec3d vector3d = this.dolphin.getVelocity();
        if (vector3d.y * vector3d.y < (double) 0.1F && this.dolphin.getPitch() != 0.0F) {
            this.dolphin.setPitch(MathHelper.lerpAngleDegrees(this.dolphin.getPitch(), 0.0F, 0.2F));
        } else {
            double d0 = Math.sqrt(vector3d.horizontalLengthSquared());
            double d1 = Math.signum(-vector3d.y) * Math.acos(d0 / vector3d.length()) * (double) MathHelper.DEGREES_PER_RADIAN;
            this.dolphin.setPitch((float) d1);
        }

    }
}
