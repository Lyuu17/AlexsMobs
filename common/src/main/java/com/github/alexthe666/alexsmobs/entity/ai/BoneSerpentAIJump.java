package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntityBoneSerpent;
import net.minecraft.entity.ai.goal.DiveJumpingGoal;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class BoneSerpentAIJump extends DiveJumpingGoal {
    private static final int[] JUMP_DISTANCES = new int[]{0, 1, 4, 5, 6, 7};
    private final EntityBoneSerpent dolphin;
    private final int interval;
    private boolean inWater;

    public BoneSerpentAIJump(EntityBoneSerpent dolphin, int p_i50329_2_) {
        this.dolphin = dolphin;
        this.interval = p_i50329_2_;
    }

    @Override
    public boolean canStart() {
        if (this.dolphin.getRandom().nextInt(this.interval) != 0 || dolphin.getTarget() != null) {
            return false;
        } else {
            var direction = this.dolphin.getMovementDirection();
            int i = direction.getOffsetX();
            int j = direction.getOffsetZ();
            BlockPos blockpos = this.dolphin.getBlockPos();
            for (int k : JUMP_DISTANCES) {
                if (!this.canJumpTo(blockpos, i, j, k) || !this.isAirAbove(blockpos, i, j, k)) {
                    return false;
                }
            }
            return true;
        }
    }

    private boolean canJumpTo(BlockPos pos, int dx, int dz, int scale) {
        BlockPos blockpos = pos.add(dx * scale, 0, dz * scale);
        return (this.dolphin.getWorld().getFluidState(blockpos).isIn(FluidTags.WATER) || this.dolphin.getWorld().getFluidState(blockpos).isIn(FluidTags.LAVA)) && !this.dolphin.getWorld().getBlockState(blockpos).blocksMovement();
    }

    private boolean isAirAbove(BlockPos pos, int dx, int dz, int scale) {
        return this.dolphin.getWorld().getBlockState(pos.add(dx * scale, 1, dz * scale)).isAir() && this.dolphin.getWorld().getBlockState(pos.add(dx * scale, 2, dz * scale)).isAir();
    }

    /**
     * Returns whether an in-progress EntityAIBase should continue executing
     */
    @Override
    public boolean shouldContinue() {
        double d0 = this.dolphin.getVelocity().y;
        return dolphin.jumpCooldown > 0 && (!(d0 * d0 < (double) 0.03F) || this.dolphin.getPitch() == 0.0F || !(Math.abs(this.dolphin.getPitch()) < 10.0F) || !this.dolphin.isTouchingWater()) && !this.dolphin.isOnGround();
    }

    @Override
    public boolean canStop() {
        return false;
    }

    /**
     * Execute a one shot task or start executing a continuous task
     */
    @Override
    public void start() {
        var direction = this.dolphin.getMovementDirection();
        float up = 0.7F + dolphin.getRandom().nextFloat() * 0.8F;
        this.dolphin.setVelocity(this.dolphin.getVelocity().add((double) direction.getOffsetX() * 0.6D, up, (double) direction.getOffsetZ() * 0.6D));
        this.dolphin.getNavigation().stop();
        this.dolphin.jumpCooldown = dolphin.getRandom().nextInt(32) + 32;
    }

    /**
     * Reset the task's internal state. Called when this task is interrupted by another one
     */
    @Override
    public void stop() {
        this.dolphin.setPitch(0.0F);
    }

    /**
     * Keep ticking a continuous task that has already been started
     */
    @Override
    public void tick() {
        boolean flag = this.inWater;
        if (!flag) {
            var fluidstate = this.dolphin.getWorld().getFluidState(this.dolphin.getBlockPos());
            this.inWater = fluidstate.isIn(FluidTags.LAVA) || fluidstate.isIn(FluidTags.WATER);
        }

        if (this.inWater && !flag) {
            this.dolphin.playSound(SoundEvents.ENTITY_DOLPHIN_JUMP, 1.0F, 1.0F);
        }

        Vec3d vector3d = this.dolphin.getVelocity();
        if (vector3d.y * vector3d.y < (double) 0.1F && this.dolphin.getPitch() != 0.0F) {
            this.dolphin.setPitch(MathHelper.lerpAngleDegrees(this.dolphin.getPitch(), 0.0F, 0.2F));
        } else {
            final double d0 = Math.sqrt(vector3d.horizontalLengthSquared());
            final double d1 = Math.signum(-vector3d.y) * Math.acos(d0 / vector3d.length()) * (double) MathHelper.DEGREES_PER_RADIAN;
            this.dolphin.setPitch((float) d1);
        }

    }
}
