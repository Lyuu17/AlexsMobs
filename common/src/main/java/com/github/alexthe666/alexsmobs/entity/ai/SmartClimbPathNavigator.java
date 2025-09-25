package com.github.alexthe666.alexsmobs.entity.ai;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class SmartClimbPathNavigator extends MobNavigation {

    @Nullable
    private BlockPos pathToPosition;

    public SmartClimbPathNavigator(MobEntity entitylivingIn, World worldIn) {
        super(entitylivingIn, worldIn);
    }

    @Override
    public Path findPathTo(BlockPos p_26589_, int p_26590_) {
        this.pathToPosition = p_26589_;
        return super.findPathTo(p_26589_, p_26590_);
    }

    @Override
    public Path findPathTo(Entity p_26586_, int p_26587_) {
        this.pathToPosition = p_26586_.getBlockPos();
        return super.findPathTo(p_26586_, p_26587_);
    }

    public boolean moveTo(Entity p_26583_, double p_26584_) {
        Path path = this.findPathTo(p_26583_, 0);
        if (path != null) {
            return this.startMovingAlong(path, p_26584_);
        } else {
            this.pathToPosition = p_26583_.getBlockPos();
            this.speed = p_26584_;
            return true;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.isIdle()) {
            super.tick();
        } else if (this.pathToPosition != null) {
            var xzOff = new Vec3d(this.pathToPosition.getX() + 0.5F - this.entity.getX(), 0, this.pathToPosition.getZ() + 0.5F - this.entity.getZ());
            double dist = xzOff.length();
            if (dist < this.entity.getWidth() || this.entity.getY() > (double)this.pathToPosition.getY()) {
                this.pathToPosition = null;
            } else {
                this.entity.getMoveControl().moveTo((double)this.pathToPosition.getX(), (double)this.entity.getY(), (double)this.pathToPosition.getZ(), this.speed);
            }
        }
    }

    @Override
    protected void checkTimeouts(Vec3d vec) {
        if (this.tickCount - this.pathStartTime > 40) {
            if (vec.squaredDistanceTo(new Vec3d(this.pathStartPos.x, vec.y, this.pathStartPos.z)) < 2.25D) {
                this.stop();
            }
            this.pathStartTime = this.tickCount;
            this.pathStartPos = vec;
        }
    }
}
