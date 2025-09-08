package com.github.alexthe666.alexsmobs.entity.ai;

import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.MathHelper;

public class FlyingAITargetDroppedItems extends CreatureAITargetItems {

    public FlyingAITargetDroppedItems(PathAwareEntity creature, boolean checkSight, boolean onlyNearby, int tickThreshold, int radius) {
        super(creature, checkSight, onlyNearby, tickThreshold, radius);
        this.executionChance = 1;
    }

    @Override
    public void stop() {
        super.stop();
        hunter.setItemFlag(false);
    }

    @Override
    public boolean canStart() {
        return super.canStart() && (mob.getTarget() == null || !mob.getTarget().isAlive());
    }

    @Override
    public boolean shouldContinue() {
        return super.shouldContinue() && (mob.getTarget() == null || !mob.getTarget().isAlive());
    }

    @Override
    protected void moveTo() {
        if (this.targetEntity != null) {
            hunter.setItemFlag(true);
            if (this.mob.distanceTo(targetEntity) < 2) {
                mob.getMoveControl().moveTo(this.targetEntity.getX(), targetEntity.getY(), this.targetEntity.getZ(), 1.5F);
                hunter.peck();
            }
            if (this.mob.distanceTo(this.targetEntity) > 8 || hunter.isFlying()) {
                hunter.setFlying(true);
                float f = (float) (mob.getX() - targetEntity.getX());
                float f1 = 1.8F;
                float f2 = (float) (mob.getZ() - targetEntity.getZ());
                float xzDist = MathHelper.sqrt(f * f + f2 * f2);

                if (!mob.canSee(targetEntity)) {
                    mob.getMoveControl().moveTo(this.targetEntity.getX(), 1 + mob.getY(), this.targetEntity.getZ(), 1.5F);
                } else {
                    if (xzDist < 5) {
                        f1 = 0;
                    }
                    mob.getMoveControl().moveTo(this.targetEntity.getX(), f1 + this.targetEntity.getY(), this.targetEntity.getZ(), 1.5F);
                }
            } else {
                this.mob.getNavigation().startMovingTo(this.targetEntity.getX(), this.targetEntity.getY(), this.targetEntity.getZ(), 1.5F);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        moveTo();
    }
}
