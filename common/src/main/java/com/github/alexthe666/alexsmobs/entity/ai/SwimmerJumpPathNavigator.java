package com.github.alexthe666.alexsmobs.entity.ai;

import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.PathNodeNavigator;
import net.minecraft.entity.ai.pathing.WaterPathNodeMaker;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.DebugInfoSender;
import net.minecraft.util.Util;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

public class SwimmerJumpPathNavigator extends EntityNavigation {

    public SwimmerJumpPathNavigator(MobEntity entitylivingIn, World worldIn) {
        super(entitylivingIn, worldIn);
    }

    protected PathNodeNavigator createPathNodeNavigator(int p_179679_1_) {
        this.nodeMaker = new WaterPathNodeMaker(true);
        return new PathNodeNavigator(this.nodeMaker, p_179679_1_);
    }

    /**
     * If on ground or swimming and can swim
     */
    @Override
    protected boolean isAtValidPosition() {
        return true;
    }

    @Override
    protected Vec3d getPos() {
        return new Vec3d(this.entity.getX(), this.entity.getBodyY(0.5D), this.entity.getZ());
    }

    @Override
    public void tick() {
        ++this.tickCount;
        if (this.inRecalculationCooldown) {
            this.recalculatePath();
        }

        if (!this.isIdle()) {
            if (this.isAtValidPosition()) {
                this.continueFollowingPath();
            } else if (this.currentPath != null && !this.currentPath.isFinished()) {
                var vector3d = this.currentPath.getNodePosition(this.entity);
                if (MathHelper.floor(this.entity.getX()) == MathHelper.floor(vector3d.x) && MathHelper.floor(this.entity.getY()) == MathHelper.floor(vector3d.y) && MathHelper.floor(this.entity.getZ()) == MathHelper.floor(vector3d.z)) {
                    this.currentPath.next();
                }
            }

            DebugInfoSender.sendPathfindingData(this.world, this.entity, this.currentPath, this.nodeReachProximity);
            if (!this.isIdle()) {
                var vector3d1 = this.currentPath.getNodePosition(this.entity);
                this.entity.getMoveControl().moveTo(vector3d1.x, vector3d1.y, vector3d1.z, this.speed);
            }
        }
    }

    @Override
    protected void continueFollowingPath() {
        if (this.currentPath != null) {
            var vector3d = this.getPos();
            final float f = this.entity.getWidth();
            float f1 = f > 0.75F ? f * 0.75F : 0.75F - f / 2.0F;
            var vector3d1 = this.entity.getVelocity();
            if (Math.abs(vector3d1.x) > 0.2D || Math.abs(vector3d1.z) > 0.2D) {
                f1 = (float)((double)f1 * vector3d1.length() * 6.0D);
            }

            var vector3d2 = Vec3d.ofBottomCenter(this.currentPath.getCurrentNodePos());
            if (Math.abs(this.entity.getX() - vector3d2.x) < (double)f1 && Math.abs(this.entity.getZ() - vector3d2.z) < (double)f1 && Math.abs(this.entity.getY() - vector3d2.y) < (double)(f1 * 2.0F)) {
                this.currentPath.next();
            }

            for(int j = Math.min(this.currentPath.getCurrentNodeIndex() + 6, this.currentPath.getLength() - 1); j > this.currentPath.getCurrentNodeIndex(); --j) {
                vector3d2 = this.currentPath.getNodePosition(this.entity, j);
                if (!(vector3d2.squaredDistanceTo(vector3d) > 36.0D) && this.canPathDirectlyThrough(vector3d, vector3d2)) {
                    this.currentPath.setCurrentNodeIndex(j);
                    break;
                }
            }

            this.checkTimeouts(vector3d);
        }
    }

    @Override
    protected void checkTimeouts(Vec3d positionVec3) {
        if (this.tickCount - this.pathStartTime > 100) {
            if (positionVec3.squaredDistanceTo(this.pathStartPos) < 2.25D) {
                this.stop();
            }

            this.pathStartTime = this.tickCount;
            this.pathStartPos = positionVec3;
        }

        if (this.currentPath != null && !this.currentPath.isFinished()) {
            var vector3i = this.currentPath.getCurrentNodePos();
            if (vector3i.equals(this.lastNodePosition)) {
                this.currentNodeMs += Util.getMeasuringTimeMs() - this.lastActiveTickMs;
            } else {
                this.lastNodePosition = vector3i;
                double d0 = positionVec3.distanceTo(Vec3d.ofCenter(this.lastNodePosition));
                this.currentNodeTimeout = this.entity.getMovementSpeed() > 0.0F ? d0 / (double)this.entity.getMovementSpeed() * 100.0D : 0.0D;
            }

            if (this.currentNodeTimeout > 0.0D && (double)this.currentNodeMs > this.currentNodeTimeout * 2.0D) {
                this.lastNodePosition = Vec3i.ZERO;
                this.currentNodeMs = 0L;
                this.currentNodeTimeout = 0.0D;
                this.stop();
            }

            this.lastActiveTickMs = Util.getMeasuringTimeMs();
        }

    }

    /**
     * Checks if the specified entity can safely walk to the specified location.
     */
    @Override
    protected boolean canPathDirectlyThrough(Vec3d posVec31, Vec3d posVec32) {
        var vector3d = new Vec3d(posVec32.x, posVec32.y + (double)this.entity.getHeight() * 0.5D, posVec32.z);
        return this.world.raycast(new RaycastContext(posVec31, vector3d, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this.entity)).getType() == HitResult.Type.MISS;
    }

    public boolean isStableDestination(BlockPos pos) {
        return !this.world.getBlockState(pos).isOpaqueFullCube(this.world, pos);
    }

    public void setCanFloat(boolean canSwim) {
    }
}
