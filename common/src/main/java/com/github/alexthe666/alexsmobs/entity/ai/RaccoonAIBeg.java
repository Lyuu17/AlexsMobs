package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntityRaccoon;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;

import java.util.EnumSet;

public class RaccoonAIBeg extends Goal {
    private static final TargetPredicate ENTITY_PREDICATE = TargetPredicate.createNonAttackable().setBaseMaxDistance(32D);
    protected final EntityRaccoon raccoon;
    private final double speed;
    protected PlayerEntity closestPlayer;
    private int delayTemptCounter;
    private boolean isRunning;

    public RaccoonAIBeg(EntityRaccoon raccoon, double speed) {
        this.raccoon = raccoon;
        this.speed = speed;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (this.delayTemptCounter > 0) {
            --this.delayTemptCounter;
            return false;
        } else {
            if(!this.raccoon.getMainHandStack().isEmpty()){
                return false;
            }
            this.closestPlayer = this.raccoon.getWorld().getClosestPlayer(ENTITY_PREDICATE, this.raccoon);
            if (this.closestPlayer == null) {
                return false;
            } else {
                return EntityRaccoon.isRaccoonFood(this.closestPlayer.getMainHandStack()) || EntityRaccoon.isRaccoonFood(this.closestPlayer.getOffHandStack());
            }
        }
    }

    @Override
    public boolean shouldContinue() {
        return this.raccoon.getMainHandStack().isEmpty() && this.canStart();
    }

    @Override
    public void start() {
        this.isRunning = true;
    }

    @Override
    public void stop() {
        this.closestPlayer = null;
        this.raccoon.getNavigation().stop();
        this.delayTemptCounter = 100;
        this.raccoon.setBegging(false);
        this.isRunning = false;
    }

    @Override
    public void tick() {
        this.raccoon.getLookControl().lookAt(this.closestPlayer, (float)(this.raccoon.getMaxHeadRotation() + 20), (float)this.raccoon.getMaxLookPitchChange());
        if (this.raccoon.squaredDistanceTo(this.closestPlayer) < 12D) {
            this.raccoon.getNavigation().stop();
            this.raccoon.setBegging(true);
        } else {
            this.raccoon.getNavigation().startMovingTo(this.closestPlayer, this.speed);
        }

    }

    public boolean isRunning() {
        return this.isRunning;
    }
}
