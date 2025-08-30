package com.github.alexthe666.alexsmobs.entity.ai;

import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class GroundPathNavigatorWide extends MobNavigation {
    private float distanceModifier = 0.75F;

    public GroundPathNavigatorWide(MobEntity entitylivingIn, World worldIn) {
        super(entitylivingIn, worldIn);
    }

    public GroundPathNavigatorWide(MobEntity entitylivingIn, World worldIn, float distanceModifier) {
        super(entitylivingIn, worldIn);
        this.distanceModifier = distanceModifier;
    }

    protected void followThePath() {
        var vector3d = this.getPos();
        this.nodeReachProximity = this.entity.getWidth() * distanceModifier;
        var vector3i = this.currentPath.getCurrentNodePos();
        double d0 = Math.abs(this.entity.getX() - ((double)vector3i.getX() + 0.5D));
        double d1 = Math.abs(this.entity.getY() - (double)vector3i.getY());
        double d2 = Math.abs(this.entity.getZ() - ((double)vector3i.getZ() + 0.5D));
        boolean flag = d0 < (double)this.nodeReachProximity && d2 < (double)this.nodeReachProximity && d1 < 1.0D;
        if (flag || this.canJumpToNext(this.currentPath.getCurrentNode().type) && this.shouldTargetNextNodeInDirection(vector3d)) {
            this.currentPath.next();
        }

        this.checkTimeouts(vector3d);
    }

    private boolean shouldTargetNextNodeInDirection(Vec3d currentPosition) {
        if (this.currentPath.getCurrentNodeIndex() + 1 >= this.currentPath.getLength()) {
            return false;
        } else {
            var vector3d = Vec3d.ofBottomCenter(this.currentPath.getCurrentNodePos());
            if (!currentPosition.isInRange(vector3d, 2.0D)) {
                return false;
            } else {
                var vector3d1 = Vec3d.ofBottomCenter(this.currentPath.getNodePos(this.currentPath.getCurrentNodeIndex() + 1));
                var vector3d2 = vector3d1.subtract(vector3d);
                var vector3d3 = currentPosition.subtract(vector3d);
                return vector3d2.dotProduct(vector3d3) > 0.0D;
            }
        }
    }

}
