package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.entity.EntityElephant;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;
import java.util.List;

public class ElephantAIFollowCaravan extends Goal {
    public final EntityElephant elephant;
    private double speedModifier;
    private int distCheckCounter;

    public ElephantAIFollowCaravan(EntityElephant llamaIn, double speedModifierIn) {
        this.elephant = llamaIn;
        this.speedModifier = speedModifierIn;
        this.setControls(EnumSet.of(Goal.Control.MOVE));
    }

    @Override
    public boolean canStart() {
        if (elephant.aiItemFlag || elephant.getControllingPassenger() != null) {
            return false;
        }
        if (!this.elephant.isTusked() && !this.elephant.inCaravan() && !elephant.isSitting()) {
            double dist = 32D;
            List<EntityElephant> list = elephant.getWorld().getNonSpectatingEntities(EntityElephant.class, elephant.getBoundingBox().expand(dist, dist / 2, dist));
            EntityElephant elephant = null;
            double d0 = Double.MAX_VALUE;

            for (var entity : list) {
                if (entity.inCaravan() && !entity.hasCaravanTrail()) {
                    double d1 = this.elephant.squaredDistanceTo(entity);
                    if (!(d1 > d0)) {
                        d0 = d1;
                        elephant = entity;
                    }
                }
            }

            if (elephant == null) {
                for (var entity1 : list) {
                    if (entity1.isTusked() && !entity1.isBaby() && !entity1.hasCaravanTrail()) {
                        double d2 = this.elephant.squaredDistanceTo(entity1);
                        if (!(d2 > d0)) {
                            d0 = d2;
                            elephant = entity1;
                        }
                    }
                }
            }

            if (elephant == null) {
                return false;
            } else if (d0 < 5.0D) {
                return false;
            } else if (!elephant.isTusked() && !elephant.isBaby() && !this.firstIsTusk(elephant, 1)) {
                return false;
            } else {
                this.elephant.joinCaravan(elephant);
                return true;
            }
        } else {
            return false;
        }
    }

    /**
     * Returns whether an in-progress EntityAIBase should continue executing
     */
    @Override
    public boolean shouldContinue() {
        if (elephant.isSitting() || elephant.aiItemFlag) {
            return false;
        }
        if (this.elephant.inCaravan() && this.elephant.getCaravanHead().isAlive() && this.firstIsTusk(this.elephant, 0)) {
            double d0 = this.elephant.squaredDistanceTo(this.elephant.getCaravanHead());
            if (d0 > 676.0D) {
                if (this.speedModifier <= 1D) {
                    this.speedModifier *= 1.2D;
                    this.distCheckCounter = 40;
                    return true;
                }

                if (this.distCheckCounter == 0) {
                    return false;
                }
            }

            if (this.distCheckCounter > 0) {
                --this.distCheckCounter;
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    public void stop() {
        this.elephant.leaveCaravan();
        this.speedModifier = 1D;
    }

    @Override
    public void tick() {
        if (this.elephant.inCaravan() && !this.elephant.isSitting()) {
            var llamaentity = this.elephant.getCaravanHead();
            if (llamaentity != null) {
                double d0 = this.elephant.distanceTo(llamaentity);
                var vector3d = (new Vec3d(llamaentity.getX() - this.elephant.getX(), llamaentity.getY() - this.elephant.getY(), llamaentity.getZ() - this.elephant.getZ())).normalize().multiply(Math.max(d0 - 4.0D, 0.0D));
                if(elephant.getNavigation().isIdle()){
                    try {
                        this.elephant.getNavigation().startMovingTo(this.elephant.getX() + vector3d.x, this.elephant.getY() + vector3d.y, this.elephant.getZ() + vector3d.z, this.speedModifier);
                    } catch (NullPointerException e) {
                        AlexsMobs.LOGGER.warn("elephant encountered issue following caravan head");
                    }
                }

            }
        }
    }

    private boolean firstIsTusk(EntityElephant llama, int p_190858_2_) {
        if (p_190858_2_ > 8) {
            return false;
        } else if (llama.inCaravan()) {
            if (llama.getCaravanHead().isTusked() && !llama.getCaravanHead().isBaby()) {
                return true;
            } else {
                var llamaentity = llama.getCaravanHead();
                ++p_190858_2_;
                return this.firstIsTusk(llamaentity, p_190858_2_);
            }
        } else {
            return false;
        }
    }
}
