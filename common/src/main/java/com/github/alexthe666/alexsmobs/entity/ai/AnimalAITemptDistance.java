package com.github.alexthe666.alexsmobs.entity.ai;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.recipe.Ingredient;

import java.util.EnumSet;

public class AnimalAITemptDistance extends Goal {
    private final TargetPredicate targetingConditions;
    protected final PathAwareEntity mob;
    private final double speedModifier;
    private double px;
    private double py;
    private double pz;
    private double pRotX;
    private double pRotY;
    protected PlayerEntity player;
    private int calmDown;
    private boolean isRunning;
    private final Ingredient items;
    private final boolean canScare;

    public AnimalAITemptDistance(PathAwareEntity p_25939_, double p_25940_, Ingredient p_25941_, boolean p_25942_, double distance) {
        this.mob = p_25939_;
        this.speedModifier = p_25940_;
        this.items = p_25941_;
        this.canScare = p_25942_;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        this.targetingConditions = TargetPredicate.createNonAttackable().setBaseMaxDistance(distance).ignoreVisibility().copy().setPredicate(this::shouldFollow);
    }

    @Override
    public boolean canStart() {
        if (this.calmDown > 0) {
            --this.calmDown;
            return false;
        } else {
            this.player = this.mob.getWorld().getClosestPlayer(this.targetingConditions, this.mob);
            return this.player != null;
        }
    }

    private boolean shouldFollow(LivingEntity p_148139_) {
        return this.items.test(p_148139_.getMainHandStack()) || this.items.test(p_148139_.getOffHandStack());
    }

    @Override
    public boolean shouldContinue() {
        if (this.canScare()) {
            if (this.mob.squaredDistanceTo(this.player) < 36.0D) {
                if (this.player.squaredDistanceTo(this.px, this.py, this.pz) > 0.010000000000000002D) {
                    return false;
                }

                if (Math.abs((double)this.player.getPitch() - this.pRotX) > 5.0D || Math.abs((double)this.player.getYaw() - this.pRotY) > 5.0D) {
                    return false;
                }
            } else {
                this.px = this.player.getX();
                this.py = this.player.getY();
                this.pz = this.player.getZ();
            }

            this.pRotX = this.player.getPitch();
            this.pRotY = this.player.getYaw();
        }

        return this.canStart();
    }

    protected boolean canScare() {
        return this.canScare;
    }

    @Override
    public void start() {
        this.px = this.player.getX();
        this.py = this.player.getY();
        this.pz = this.player.getZ();
        this.isRunning = true;
    }

    @Override
    public void stop() {
        this.player = null;
        this.mob.getNavigation().stop();
        this.calmDown = 100;
        this.isRunning = false;
    }

    @Override
    public void tick() {
        this.mob.getLookControl().lookAt(this.player, (float)(this.mob.getMaxHeadRotation() + 20), (float)this.mob.getMaxLookPitchChange());
        if (this.mob.squaredDistanceTo(this.player) < 6.25D) {
            this.mob.getNavigation().stop();
        } else {
            this.mob.getNavigation().startMovingTo(this.player, this.speedModifier);
        }

    }

    public boolean isRunning() {
        return this.isRunning;
    }
}