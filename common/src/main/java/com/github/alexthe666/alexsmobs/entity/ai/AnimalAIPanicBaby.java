package com.github.alexthe666.alexsmobs.entity.ai;

import net.minecraft.entity.ai.goal.EscapeDangerGoal;
import net.minecraft.entity.passive.AnimalEntity;

public class AnimalAIPanicBaby extends EscapeDangerGoal {

    private final AnimalEntity animal;

    public AnimalAIPanicBaby(AnimalEntity creatureIn, double speed) {
        super(creatureIn, speed);
        this.animal = creatureIn;
    }

    @Override
    public boolean canStart() {
        return animal.isBaby() && super.canStart();
    }
}
