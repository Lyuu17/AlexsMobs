package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntityCrocodile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.util.Hand;

public class CrocodileAIMelee extends MeleeAttackGoal {

    private final EntityCrocodile crocodile;

    public CrocodileAIMelee(EntityCrocodile crocodile, double speedIn, boolean useLongMemory) {
        super(crocodile, speedIn, useLongMemory);
        this.crocodile = crocodile;
    }

    @Override
    public boolean canStart() {
        return super.canStart() && crocodile.getPassengerList().isEmpty();
    }

    @Override
    public boolean shouldContinue() {
        return super.shouldContinue() && crocodile.getPassengerList().isEmpty();
    }

    @Override
    protected void attack(LivingEntity enemy, double distToEnemySqr) {
        double d0 = this.getSquaredMaxAttackDistance(enemy);
        if (distToEnemySqr <= d0) {
            this.resetCooldown();
            this.mob.swingHand(Hand.MAIN_HAND);
            this.mob.tryAttack(enemy);
        }
    }
}
