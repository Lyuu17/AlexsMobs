package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import net.minecraft.entity.ai.FuzzyTargeting;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class AnimalAIFleeAdult extends Goal {
    private final AnimalEntity childAnimal;
    private AnimalEntity parentAnimal;
    private final double moveSpeed;
    private final double fleeDistance;
    private int delayCounter;
    private Path path;

    public AnimalAIFleeAdult(AnimalEntity animal, double speed, double fleeDistance) {
        this.childAnimal = animal;
        this.moveSpeed = speed;
        this.fleeDistance = fleeDistance;
    }

    @Override
    public boolean canStart() {
        if (this.childAnimal.getBreedingAge() >= 0) {
            return false;
        } else {
            List<? extends AnimalEntity> list = this.childAnimal.getWorld().getNonSpectatingEntities(this.childAnimal.getClass(), this.childAnimal.getBoundingBox().expand(fleeDistance, 4.0D, fleeDistance));
            AnimalEntity animalentity = null;
            double d0 = Double.MAX_VALUE;

            for(var animalentity1 : list) {
                if (animalentity1.getBreedingAge() >= 0) {
                    double d1 = this.childAnimal.squaredDistanceTo(animalentity1);
                    if (!(d1 > d0)) {
                        d0 = d1;
                        animalentity = animalentity1;
                    }
                }
            }

            if (animalentity == null) {
                return false;
            } else if (d0 > 19.0D) {
                return false;
            } else {
                this.parentAnimal = animalentity;
                var vec3d = FuzzyTargeting.findFrom(this.childAnimal, (int) fleeDistance, 7, new Vec3d(this.parentAnimal.getX(), this.parentAnimal.getY(), this.parentAnimal.getZ()));
                if (vec3d == null) {
                    return false;
                } else if (this.parentAnimal.squaredDistanceTo(vec3d.x, vec3d.y, vec3d.z) < this.parentAnimal.squaredDistanceTo(this.childAnimal)) {
                    return false;
                } else {
                    this.path = childAnimal.getNavigation().findPathTo(AMBlockPos.fromVec3(vec3d), 0);
                    return this.path != null;
                }
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
            return !childAnimal.getNavigation().isIdle();
        }
    }

    @Override
    public void start() {
        childAnimal.getNavigation().startMovingAlong(this.path, moveSpeed);
    }

    @Override
    public void stop() {
        this.parentAnimal = null;
        this.childAnimal.getNavigation().stop();
        this.path = null;
    }

    @Override
    public void tick() {
    }
}
