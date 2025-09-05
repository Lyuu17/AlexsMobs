package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.IFollower;
import net.minecraft.block.LeavesBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.LandPathNodeMaker;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;

import java.util.EnumSet;

public class FlyingAIFollowOwner extends Goal {
    private final TameableEntity tameable;
    private LivingEntity owner;
    private final WorldView world;
    private final double followSpeed;
    private final EntityNavigation navigator;
    private int timeToRecalcPath;
    private final float maxDist;
    private final float minDist;
    private float oldWaterCost;
    private final boolean teleportToLeaves;
    private final IFollower follower;

    public FlyingAIFollowOwner(TameableEntity tameable, double speed, float minDist, float maxDist, boolean teleportToLeaves) {
        this.tameable = tameable;
        this.world = tameable.getWorld();
        this.followSpeed = speed;
        this.navigator = tameable.getNavigation();
        this.minDist = minDist;
        this.maxDist = maxDist;
        this.teleportToLeaves = teleportToLeaves;
        follower = (IFollower)tameable;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }

    /**
     * Returns whether execution should begin. You can also read and cache any state necessary for execution in this
     * method as well.
     */
    @Override
    public boolean canStart() {
        LivingEntity livingentity = this.tameable.getOwner();
        if (livingentity == null) {
            return false;
        } else if (livingentity.isSpectator()) {
            return false;
        } else if (this.tameable.isSitting()) {
            return false;
        } else if (this.tameable.squaredDistanceTo(livingentity) < (double)(this.minDist * this.minDist) || isInCombat()) {
            return false;
        } else {
            this.owner = livingentity;
            return follower.shouldFollow();
        }
    }

    /**
     * Returns whether an in-progress EntityAIBase should continue executing
     */
    @Override
    public boolean shouldContinue() {
        if (this.tameable.isSitting() || isInCombat()) {
            return false;
        } else {
            return this.tameable.squaredDistanceTo(this.owner) > (double)(this.maxDist * this.maxDist);
        }
    }

    private boolean isInCombat() {
        var owner = tameable.getOwner();
        if(owner != null){
            return tameable.distanceTo(owner) < 30 && tameable.getTarget() != null && tameable.getTarget().isAlive();
        }
        return false;
    }

    /**
     * Execute a one shot task or start executing a continuous task
     */
    @Override
    public void start() {
        this.timeToRecalcPath = 0;
        this.oldWaterCost = this.tameable.getPathfindingPenalty(PathNodeType.WATER);
        this.tameable.setPathfindingPenalty(PathNodeType.WATER, 0.0F);
    }

    /**
     * Reset the task's internal state. Called when this task is interrupted by another one
     */
    @Override
    public void stop() {
        this.owner = null;
        this.navigator.stop();
        this.tameable.setPathfindingPenalty(PathNodeType.WATER, this.oldWaterCost);
    }

    /**
     * Keep ticking a continuous task that has already been started
     */
    @Override
    public void tick() {
        this.tameable.getLookControl().lookAt(this.owner, 10.0F, (float)this.tameable.getMaxLookPitchChange());
        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = 10;
            if (!this.tameable.isLeashed() && !this.tameable.hasVehicle()) {
                if (this.tameable.squaredDistanceTo(this.owner) >= 144.0D) {
                    this.tryToTeleportNearEntity();
                }
                follower.followEntity(tameable, owner, followSpeed);
            }
        }
    }

    private void tryToTeleportNearEntity() {
        var blockpos = this.owner.getBlockPos();

        for(int i = 0; i < 10; ++i) {
            int j = this.getRandomNumber(-3, 3);
            int k = this.getRandomNumber(-1, 1);
            int l = this.getRandomNumber(-3, 3);
            boolean flag = this.tryToTeleportToLocation(blockpos.getX() + j, blockpos.getY() + k, blockpos.getZ() + l);
            if (flag) {
                return;
            }
        }

    }

    private boolean tryToTeleportToLocation(int x, int y, int z) {
        if (Math.abs((double)x - this.owner.getX()) < 2.0D && Math.abs((double)z - this.owner.getZ()) < 2.0D) {
            return false;
        } else if (!this.isTeleportFriendlyBlock(new BlockPos(x, y, z))) {
            return false;
        } else {
            this.tameable.refreshPositionAndAngles((double)x + 0.5D, (double)y, (double)z + 0.5D, this.tameable.getYaw(), this.tameable.getPitch());
            this.navigator.stop();
            return true;
        }
    }

    private boolean isTeleportFriendlyBlock(BlockPos pos) {
        if(this.world.getBlockState(pos).isAir()){
            BlockPos blockpos = pos.subtract(this.tameable.getBlockPos());
            return this.world.isSpaceEmpty(this.tameable, this.tameable.getBoundingBox().offset(blockpos));
        }
        PathNodeType pathnodetype = LandPathNodeMaker.getLandNodeType(this.world, pos.mutableCopy());
        if (pathnodetype != PathNodeType.WALKABLE) {
            return false;
        } else {
            var blockstate = this.world.getBlockState(pos.down());
            if (!this.teleportToLeaves && blockstate.getBlock() instanceof LeavesBlock) {
                return false;
            } else {
                BlockPos blockpos = pos.subtract(this.tameable.getBlockPos());
                return this.world.isSpaceEmpty(this.tameable, this.tameable.getBoundingBox().offset(blockpos));
            }
        }
    }

    private int getRandomNumber(int min, int max) {
        return this.tameable.getRandom().nextInt(max - min + 1) + min;
    }
}

