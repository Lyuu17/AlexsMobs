package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntityCapuchinMonkey;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.function.Predicate;

public class CapuchinAITargetBalloons extends Goal {

    private final EntityCapuchinMonkey monkey;
    protected final boolean shouldCheckSight;
    private final boolean nearbyOnly;
    private int targetSearchStatus;
    private int targetSearchDelay;
    private int targetUnseenTicks;
    protected Entity target;
    protected int unseenMemoryTicks = 60;
    protected final int targetChance;
    public static final Predicate<Entity> TARGET_BLOON = (balloon) -> balloon.getSavedEntityId() != null && (balloon.getType().isIn(AMTagRegistry.MONKEY_TARGET_WITH_DART) || balloon.getSavedEntityId().contains("balloom"));

    public CapuchinAITargetBalloons(EntityCapuchinMonkey mobIn, boolean checkSight) {
        this(mobIn, checkSight, false, 40);
    }

    public CapuchinAITargetBalloons(EntityCapuchinMonkey mobIn, boolean checkSight, boolean nearbyOnlyIn, int targetChance) {
        this.setControls(EnumSet.of(Goal.Control.TARGET));
        this.monkey = mobIn;
        this.shouldCheckSight = checkSight;
        this.nearbyOnly = nearbyOnlyIn;
        this.targetChance = targetChance;
    }

    @Override
    public boolean canStart() {
        if (this.targetChance > 0 && this.monkey.getRandom().nextInt(this.targetChance) != 0) {
            return false;
        } else {
            this.findNearestTarget();
            return this.target != null;
        }
    }

    protected Box getTargetableArea(double targetDistance) {
        return this.monkey.getBoundingBox().expand(targetDistance, targetDistance, targetDistance);
    }

    protected void findNearestTarget() {
        Entity closest = null;
        for(var bloon : this.monkey.getWorld().getEntitiesByClass(Entity.class, getTargetableArea(getTargetDistance()), TARGET_BLOON)){
            if(closest == null || closest.distanceTo(monkey) > bloon.distanceTo(monkey)){
                closest = bloon;
            }
        }
        this.target = closest;
    }

    @Override
    public boolean shouldContinue() {
        Entity livingentity = this.monkey.getDartTarget();
        if (livingentity == null) {
            livingentity = this.target;
        }

        if (livingentity == null) {
            return false;
        } else if (!livingentity.isAlive()) {
            return false;
        } else {
            var team = this.monkey.getScoreboardTeam();
            var team1 = livingentity.getScoreboardTeam();
            if (team != null && team1 == team) {
                return false;
            } else {
                double d0 = this.getTargetDistance();
                if (this.monkey.squaredDistanceTo(livingentity) > d0 * d0) {
                    return false;
                } else {
                    if (this.shouldCheckSight) {
                        if (this.monkey.getVisibilityCache().canSee(livingentity)) {
                            this.targetUnseenTicks = 0;
                        } else if (++this.targetUnseenTicks > this.unseenMemoryTicks) {
                            return false;
                        }
                    }

                    if (livingentity instanceof PlayerEntity && ((PlayerEntity)livingentity).getAbilities().invulnerable) {
                        return false;
                    } else {
                        this.monkey.setDartTarget(livingentity);
                        return true;
                    }
                }
            }
        }
    }

    protected double getTargetDistance() {
        return this.monkey.getAttributeValue(EntityAttributes.GENERIC_FOLLOW_RANGE);
    }

    @Override
    public void start() {
        this.monkey.setDartTarget(this.target);
        this.targetSearchStatus = 0;
        this.targetSearchDelay = 0;
        this.targetUnseenTicks = 0;
    }

    /**
     * Reset the task's internal state. Called when this task is interrupted by another one
     */
    @Override
    public void stop() {
        this.monkey.setTarget(null);
        this.monkey.setDartTarget(null);
        this.target = null;
    }

    /**
     * checks if is is a suitable target
     */
    protected boolean isSuitableTarget(@Nullable LivingEntity potentialTarget, TargetPredicate targetPredicate) {
        if (potentialTarget == null) {
            return false;
        } else if (!targetPredicate.test(this.monkey, potentialTarget)) {
            return false;
        } else if (!this.monkey.isInWalkTargetRange(potentialTarget.getBlockPos())) {
            return false;
        } else {
            if (this.nearbyOnly) {
                if (--this.targetSearchDelay <= 0) {
                    this.targetSearchStatus = 0;
                }

                if (this.targetSearchStatus == 0) {
                    this.targetSearchStatus = this.canEasilyReach(potentialTarget) ? 1 : 2;
                }

                if (this.targetSearchStatus == 2) {
                    return false;
                }
            }

            return true;
        }
    }

    /**
     * Checks to see if this entity can find a short path to the given target.
     */
    private boolean canEasilyReach(LivingEntity target) {
        this.targetSearchDelay = 10 + this.monkey.getRandom().nextInt(5);
        var path = this.monkey.getNavigation().findPathTo(target, 0);
        if (path == null) {
            return false;
        } else {
            var pathpoint = path.getEnd();
            if (pathpoint == null) {
                return false;
            } else {
                int i = pathpoint.x - MathHelper.floor(target.getX());
                int j = pathpoint.z - MathHelper.floor(target.getZ());
                return (double)(i * i + j * j) <= 2.25D;
            }
        }
    }

    public CapuchinAITargetBalloons setUnseenMemoryTicks(int unseenMemoryTicksIn) {
        this.unseenMemoryTicks = unseenMemoryTicksIn;
        return this;
    }
}
