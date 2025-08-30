package com.github.alexthe666.alexsmobs.entity.ai;

import net.minecraft.entity.ai.FuzzyTargeting;
import net.minecraft.entity.ai.goal.WanderAroundGoal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public class AnimalAIWanderRanged extends WanderAroundGoal {
    protected final float probability;
    protected final int xzRange;
    protected final int yRange;

    public AnimalAIWanderRanged(PathAwareEntity creature, int chance, double speedIn, int xzRange, int yRange) {
        this(creature, chance, speedIn, 0.001F, xzRange, yRange);
    }

    public AnimalAIWanderRanged(PathAwareEntity creature, int chance, double speedIn, float probabilityIn, int xzRange, int yRange) {
        super(creature, speedIn, chance);
        this.probability = probabilityIn;
        this.xzRange = xzRange;
        this.yRange = yRange;
    }

    @Override
    public boolean canStart() {
        //TODO
        if (this.mob.hasPassengers() /*&& !(this.mob instanceof EntityKangaroo)*/) {
            return false;
        } else {
            if (!this.ignoringChance) {
                if ( this.mob.getDespawnCounter() >= 100) {
                    return false;
                }

                if (this.mob.getRandom().nextInt(this.chance) != 0) {
                    return false;
                }
            }

            var lvt_1_1_ = this.getWanderTarget();
            if (lvt_1_1_ == null) {
                return false;
            } else {
                this.targetX = lvt_1_1_.x;
                this.targetY = lvt_1_1_.y;
                this.targetZ = lvt_1_1_.z;
                this.ignoringChance = false;
                return true;
            }
        }
    }

    @Nullable
    @Override
    protected Vec3d getWanderTarget() {
        if (this.mob.isInsideWaterOrBubbleColumn()) {
            var vector3d = FuzzyTargeting.find(this.mob, xzRange, yRange);
            return vector3d == null ? super.getWanderTarget() : vector3d;
        } else {
            return this.mob.getRandom().nextFloat() >= this.probability ? FuzzyTargeting.find(this.mob, xzRange, yRange) : super.getWanderTarget();
        }
    }
}
