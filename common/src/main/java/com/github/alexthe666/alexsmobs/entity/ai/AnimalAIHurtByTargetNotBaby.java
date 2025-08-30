package com.github.alexthe666.alexsmobs.entity.ai;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;

public class AnimalAIHurtByTargetNotBaby extends RevengeGoal {

    private final AnimalEntity animal;

    public AnimalAIHurtByTargetNotBaby(AnimalEntity creatureIn, Class<?>... excludeReinforcementTypes) {
        super(creatureIn, excludeReinforcementTypes);
        this.animal = creatureIn;
    }

    @Override
    public void start() {
        super.start();
        if (animal.isBaby()) {
            this.callSameTypeForRevenge();
            this.stop();
        }

    }

    @Override
    protected void setMobEntityTarget(MobEntity mobIn, LivingEntity targetIn) {
        if (!mobIn.isBaby()) {
            super.setMobEntityTarget(mobIn, targetIn);
        }
    }
}
