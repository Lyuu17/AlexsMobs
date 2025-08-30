package com.github.alexthe666.alexsmobs.entity.ai;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.passive.AnimalEntity;

import java.util.List;

public class AnimalAIFollowParentRanged extends Goal {
    private final AnimalEntity childAnimal;
    private AnimalEntity parentAnimal;
    private final double moveSpeed;
    private int delayCounter;
    private float range = 8F;
    private float minDist = 3F;

    public AnimalAIFollowParentRanged(AnimalEntity animal, double p_i1626_2_, float range, float minDist) {
        this.childAnimal = animal;
        this.moveSpeed = p_i1626_2_;
        this.range = range;
        this.minDist = minDist;
    }

    @Override
    public boolean canStart() {
        if (this.childAnimal.getBreedingAge() >= 0) {
            return false;
        } else {
            List<? extends AnimalEntity> lvt_1_1_ = this.childAnimal.getWorld().getNonSpectatingEntities(this.childAnimal.getClass(), this.childAnimal.getBoundingBox().expand(range, range * 0.5D, range));
            AnimalEntity lvt_2_1_ = null;
            double lvt_3_1_ = 1.7976931348623157E308D;
            var var5 = lvt_1_1_.iterator();

            while(var5.hasNext()) {
                var lvt_6_1_ = (AnimalEntity)var5.next();
                if (lvt_6_1_.getBreedingAge() >= 0) {
                    double lvt_7_1_ = this.childAnimal.squaredDistanceTo(lvt_6_1_);
                    if (lvt_7_1_ <= lvt_3_1_) {
                        lvt_3_1_ = lvt_7_1_;
                        lvt_2_1_ = lvt_6_1_;
                    }
                }
            }

            if (lvt_2_1_ == null) {
                return false;
            } else if (lvt_3_1_ < minDist * minDist) {
                return false;
            } else {
                this.parentAnimal = lvt_2_1_;
                return true;
            }
        }
    }

    @Override
    public boolean shouldContinue() {
        if (this.childAnimal.getBreedingAge() >= 0) {
            return false;
        } else if (!this.parentAnimal.isAlive()) {
            return false;
        } else {
            double lvt_1_1_ = this.childAnimal.squaredDistanceTo(this.parentAnimal);
            return lvt_1_1_ >= minDist * minDist && lvt_1_1_ <= range * range;
        }
    }

    @Override
    public void start() {
        this.delayCounter = 0;
    }

    @Override
    public void stop() {
        this.parentAnimal = null;
    }

    @Override
    public void tick() {
        if (--this.delayCounter <= 0) {
            this.delayCounter = 10;
            this.childAnimal.getNavigation().startMovingTo(this.parentAnimal, this.moveSpeed);
        }
    }
}
