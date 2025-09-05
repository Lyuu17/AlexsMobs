package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntityGrizzlyBear;
import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import com.google.common.base.Predicate;
import net.minecraft.entity.ai.FuzzyTargeting;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;
import java.util.List;

public class GrizzlyBearAIFleeBees extends Goal {
    private final double farSpeed;
    private final double nearSpeed;
    private final float avoidDistance;
    private final Predicate<BeeEntity> avoidTargetSelector;
    protected EntityGrizzlyBear entity;
    protected BeeEntity closestLivingEntity;
    private Path path;

    public GrizzlyBearAIFleeBees(EntityGrizzlyBear entityIn, float avoidDistanceIn, double farSpeedIn, double nearSpeedIn) {
        this.avoidTargetSelector = beeEntity -> beeEntity.isAlive() && GrizzlyBearAIFleeBees.this.entity.getVisibilityCache().canSee(beeEntity) && !GrizzlyBearAIFleeBees.this.entity.isTeammate(beeEntity) && beeEntity.getAngerTime() > 0;
        this.entity = entityIn;
        this.avoidDistance = avoidDistanceIn;
        this.farSpeed = farSpeedIn;
        this.nearSpeed = nearSpeedIn;
        this.setControls(EnumSet.of(Goal.Control.MOVE));
    }

    @Override
    public boolean canStart() {
        if (this.entity.isTamed()) {
            return false;
        }
        if(this.entity.isSitting() && !entity.forcedSit){
            this.entity.setSitting(false);
        }
        if(this.entity.isSitting()){
            return false;
        }
        List<BeeEntity> beeEntities = this.entity.getWorld().getEntitiesByClass(BeeEntity.class, this.entity.getBoundingBox().expand(avoidDistance, 8.0D, avoidDistance), this.avoidTargetSelector);
        if (beeEntities.isEmpty()) {
            return false;
        } else {
            this.closestLivingEntity = beeEntities.get(0);
            Vec3d vec3d = FuzzyTargeting.findFrom(this.entity, 16, 7, new Vec3d(this.closestLivingEntity.getX(), this.closestLivingEntity.getY(), this.closestLivingEntity.getZ()));
            if (vec3d == null) {
                return false;
            } else if (this.closestLivingEntity.squaredDistanceTo(vec3d.x, vec3d.y, vec3d.z) < this.closestLivingEntity.squaredDistanceTo(this.entity)) {
                return false;
            } else {
                this.path = entity.getNavigation().findPathTo(AMBlockPos.fromCoords(vec3d.x, vec3d.y, vec3d.z), 0);
                return this.path != null;
            }
        }
    }

    @Override
    public boolean shouldContinue() {
        return !entity.getNavigation().isIdle();
    }

    @Override
    public void start() {
        entity.getNavigation().startMovingAlong(this.path, farSpeed);
    }

    @Override
    public void stop() {
        this.entity.getNavigation().stop();
        this.closestLivingEntity = null;
    }

    @Override
    public void tick() {
        if(closestLivingEntity != null && closestLivingEntity.getAngerTime() <= 0){
            this.stop();
        }
        this.entity.getNavigation().setSpeed(getRunSpeed());
    }

    public double getRunSpeed() {
        return 0.7F;
    }
}

