package com.github.alexthe666.alexsmobs.entity.ai;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.TemptGoal;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.recipe.Ingredient;

public class TameableAITempt extends TemptGoal {

    private static final TargetPredicate DEF = TargetPredicate.createNonAttackable().setBaseMaxDistance(10.0D).ignoreVisibility();
    private final AnimalEntity tameable;
    private int calmDown;
    private final TargetPredicate targetingConditions;
    private final Ingredient items;

    public TameableAITempt(AnimalEntity tameable, double speedIn, Ingredient temptItemsIn, boolean scaredByPlayerMovementIn) {
        super(tameable, speedIn, temptItemsIn, scaredByPlayerMovementIn);
        this.tameable = tameable;
        this.items = temptItemsIn;
        this.targetingConditions = DEF.copy().setPredicate(this::shouldFollowAM);
    }

    public boolean shouldFollowAM(LivingEntity p_148139_) {
        return this.items.test(p_148139_.getMainHandStack()) || this.items.test(p_148139_.getOffHandStack());
    }

    @Override
    public boolean canStart() {
        if (this.calmDown > 0) {
            --this.calmDown;
            return false;
        } else {
            this.closestPlayer = this.mob.getWorld().getClosestPlayer(this.targetingConditions, this.mob);
            return  (!(tameable instanceof TameableEntity) || !((TameableEntity)tameable).isTamed()) && this.closestPlayer != null;
        }
    }

    @Override
    public void stop() {
        super.stop();
        this.calmDown = toGoalTicks(100);
    }
}
