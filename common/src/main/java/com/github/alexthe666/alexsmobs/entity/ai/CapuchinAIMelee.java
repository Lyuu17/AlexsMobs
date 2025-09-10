package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntityCapuchinMonkey;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.util.Hand;

public class CapuchinAIMelee extends MeleeAttackGoal {

    private final EntityCapuchinMonkey monkey;

    public CapuchinAIMelee(EntityCapuchinMonkey monkey, double speedIn, boolean useLongMemory) {
        super(monkey, speedIn, useLongMemory);
        this.monkey = monkey;
    }

    @Override
    public boolean canStart() {
        return super.canStart() && !monkey.attackDecision;
    }

    @Override
    public boolean shouldContinue() {
        return super.shouldContinue() && !monkey.attackDecision;
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
