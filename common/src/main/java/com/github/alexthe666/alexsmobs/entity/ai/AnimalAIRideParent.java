package com.github.alexthe666.alexsmobs.entity.ai;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.passive.AnimalEntity;

import java.util.List;

public class AnimalAIRideParent extends Goal {
    private final AnimalEntity childAnimal;
    private AnimalEntity parentAnimal;
    private final double moveSpeed;
    private int delayCounter;

    public AnimalAIRideParent(AnimalEntity animal, double speed) {
        this.childAnimal = animal;
        this.moveSpeed = speed;
    }

    @Override
    public boolean canStart() {
        if (this.childAnimal.getBreedingAge() >= 0 || this.childAnimal.hasVehicle()) {
            return false;
        } else {
            List<? extends AnimalEntity> list = this.childAnimal.getWorld().getNonSpectatingEntities(this.childAnimal.getClass(), this.childAnimal.getBoundingBox().expand(8.0D, 4.0D, 8.0D));
            AnimalEntity animalentity = null;
            double d0 = Double.MAX_VALUE;

            for(var animalentity1 : list) {
                if (animalentity1.getBreedingAge() >= 0 && animalentity1.getPassengerList().isEmpty()) {
                    double d1 = this.childAnimal.squaredDistanceTo(animalentity1);
                    if (!(d1 > d0)) {
                        d0 = d1;
                        animalentity = animalentity1;
                    }
                }
            }

            if (animalentity == null) {
                return false;
            } else if (d0 < 2.0D) {
                return false;
            } else {
                this.parentAnimal = animalentity;
                return true;
            }
        }
    }

    /**
     * Returns whether an in-progress EntityAIBase should continue executing
     */
    @Override
    public boolean shouldContinue() {
        if (this.childAnimal.getBreedingAge() >= 0) {
            return false;
        } else if (parentAnimal == null || !this.parentAnimal.isAlive() || !this.parentAnimal.getPassengerList().isEmpty()) {
            return false;
        } else {
            double d0 = this.childAnimal.squaredDistanceTo(this.parentAnimal);
            return !(d0 < 2.0D) && !(d0 > 256.0D) && !this.childAnimal.isConnectedThroughVehicle(this.parentAnimal);
        }
    }

    /**
     * Execute a one shot task or start executing a continuous task
     */
    @Override
    public void start() {
        this.delayCounter = 0;
    }

    /**
     * Reset the task's internal state. Called when this task is interrupted by another one
     */
    @Override
    public void stop() {
        this.parentAnimal = null;
    }

    /**
     * Keep ticking a continuous task that has already been started
     */
    @Override
    public void tick() {
        if (--this.delayCounter <= 0) {
            this.delayCounter = 10;
            this.childAnimal.getNavigation().startMovingTo(this.parentAnimal, this.moveSpeed);
        }
        if(this.childAnimal.distanceTo(this.parentAnimal) < 2.0D){
            this.childAnimal.startRiding(this.parentAnimal, false);
            this.stop();
        }
    }
}
