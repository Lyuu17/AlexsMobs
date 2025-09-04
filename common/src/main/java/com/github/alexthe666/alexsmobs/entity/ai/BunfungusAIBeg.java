package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntityBunfungus;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.EnumSet;

public class BunfungusAIBeg extends Goal {

    private static final TargetPredicate ENTITY_PREDICATE = TargetPredicate.createNonAttackable().setBaseMaxDistance(32D);
    protected final EntityBunfungus jerboa;
    private final double speed;
    protected PlayerEntity closestPlayer;
    private int delayTemptCounter;
    private boolean isRunning;

    public BunfungusAIBeg(EntityBunfungus jerboa, double speed) {
        this.jerboa = jerboa;
        this.speed = speed;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (this.delayTemptCounter > 0) {
            --this.delayTemptCounter;
            return false;
        } else {
            this.closestPlayer = this.jerboa.getWorld().getClosestPlayer(ENTITY_PREDICATE, this.jerboa);
            if (this.closestPlayer == null) {
                return false;
            } else {
                boolean food = isFood(this.closestPlayer.getMainHandStack()) || isFood(this.closestPlayer.getOffHandStack());
                return food;
            }
        }
    }

    private boolean isFood(ItemStack stack) {
        return stack.getItem() == Items.CARROT;
    }

    @Override
    public boolean shouldContinue() {
        return this.jerboa.getMainHandStack().isEmpty() && this.canStart();
    }

    @Override
    public void start() {
        this.isRunning = true;
    }

    @Override
    public void stop() {
        this.closestPlayer = null;
        this.jerboa.getNavigation().stop();
        this.delayTemptCounter = 100;
        this.jerboa.setBegging(false);
        this.isRunning = false;
    }

    @Override
    public void tick() {
        this.jerboa.setSleeping(false);
        this.jerboa.getLookControl().lookAt(this.closestPlayer, (float)(this.jerboa.getMaxHeadRotation() + 20), (float)this.jerboa.getMaxLookPitchChange());
        if (this.jerboa.squaredDistanceTo(this.closestPlayer) < 12D) {
            this.jerboa.getNavigation().stop();
            this.jerboa.setBegging(true);
        } else {
            this.jerboa.getNavigation().startMovingTo(this.closestPlayer, this.speed);
        }

    }

    public boolean isRunning() {
        return this.isRunning;
    }
}
