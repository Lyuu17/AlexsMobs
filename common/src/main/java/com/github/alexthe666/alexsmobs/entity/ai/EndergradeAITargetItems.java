package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntityEndergrade;
import com.google.common.base.Predicate;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ai.goal.TrackTargetGoal;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

public class EndergradeAITargetItems<T extends ItemEntity> extends TrackTargetGoal {
    protected final Sorter theNearestAttackableTargetSorter;
    protected final Predicate<? super ItemEntity> targetEntitySelector;
    protected int executionChance;
    protected boolean mustUpdate;
    protected ItemEntity targetEntity;
    private EntityEndergrade endergrade;

    public EndergradeAITargetItems(EntityEndergrade creature, boolean checkSight) {
        this(creature, checkSight, false);
        this.setControls(EnumSet.of(Control.MOVE));
    }

    public EndergradeAITargetItems(EntityEndergrade creature, boolean checkSight, boolean onlyNearby) {
        this(creature, 10, checkSight, onlyNearby, null);
    }

    public EndergradeAITargetItems(EntityEndergrade creature, int chance, boolean checkSight, boolean onlyNearby, @Nullable final Predicate<? super T> targetSelector) {
        super(creature, checkSight, onlyNearby);
        this.executionChance = chance;
        this.endergrade = creature;
        this.theNearestAttackableTargetSorter = new Sorter(creature);
        this.targetEntitySelector = (Predicate<ItemEntity>) item -> {
            var stack = item.getStack();
            return !stack.isEmpty() && endergrade.canTargetItem(stack);
        };
        this.setControls(EnumSet.of(Control.MOVE));
    }

    @Override
    public boolean canStart() {
        if (this.mob.hasVehicle() || mob.hasPassengers() && mob.getControllingPassenger() != null) {
            return false;
        }
        if(!mob.getStackInHand(Hand.MAIN_HAND).isEmpty()){
            return false;
        }
        if (!this.mustUpdate) {
            long worldTime = this.mob.getWorld().getTime() % 10;
            if (this.mob.getDespawnCounter() >= 100 && worldTime != 0) {
                return false;
            }
            if (this.mob.getRandom().nextInt(this.executionChance) != 0 && worldTime != 0) {
                return false;
            }
        }
        List<ItemEntity> list = this.mob.getWorld().getEntitiesByClass(ItemEntity.class, this.getTargetableArea(this.getFollowRange()), this.targetEntitySelector);
        if (list.isEmpty()) {
            return false;
        } else {
            Collections.sort(list, this.theNearestAttackableTargetSorter);
            this.targetEntity = list.get(0);
            this.endergrade.stopWandering = true;
            this.endergrade.hasItemTarget = true;
            this.mustUpdate = false;
            return true;
        }
    }

    @Override
    protected double getFollowRange() {
        return 16D;
    }

    protected Box getTargetableArea(double targetDistance) {
        var renderCenter = new Vec3d(this.mob.getX() + 0.5, this.mob.getY()+ 0.5, this.mob.getZ() + 0.5D);
        double renderRadius = 9;
        var aabb = new Box(-renderRadius, -renderRadius, -renderRadius, renderRadius, renderRadius, renderRadius);
        return aabb.offset(renderCenter);
    }

    @Override
    public void start() {
        this.mob.getMoveControl().moveTo(this.targetEntity.getX(), this.targetEntity.getY(), this.targetEntity.getZ(), 1);
        super.start();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.targetEntity == null || this.targetEntity != null && !this.targetEntity.isAlive()) {
            this.stop();
        }else{
            this.mob.getMoveControl().moveTo(this.targetEntity.getX(), this.targetEntity.getY(), this.targetEntity.getZ(), 1);
        }
        if (this.targetEntity != null && this.targetEntity.isAlive() && this.mob.squaredDistanceTo(this.targetEntity) < 2.0D && mob.getStackInHand(Hand.MAIN_HAND).isEmpty()) {
            var duplicate = this.targetEntity.getStack().copy();
            endergrade.bite();
            duplicate.setCount(1);
            if (!mob.getStackInHand(Hand.MAIN_HAND).isEmpty() && !mob.getWorld().isClient) {
                mob.dropStack(mob.getStackInHand(Hand.MAIN_HAND), 0.0F);
            }
            mob.setStackInHand(Hand.MAIN_HAND, duplicate);
            endergrade.onGetItem(targetEntity);
            this.targetEntity.getStack().decrement(1);
            stop();
        }
    }

    @Override
    public void stop() {
        targetEntity = null;
        this.endergrade.hasItemTarget = false;
        endergrade.stopWandering = false;
    }

    public void makeUpdate() {
        this.mustUpdate = true;
    }

    @Override
    public boolean shouldContinue() {
        return this.mob.getMoveControl().isMoving();
    }

    public record Sorter(EntityEndergrade theEntity) implements Comparator<Entity> {
        public int compare(Entity p_compare_1_, Entity p_compare_2_) {
            final double d0 = this.theEntity.squaredDistanceTo(p_compare_1_);
            final double d1 = this.theEntity.squaredDistanceTo(p_compare_2_);
            return Double.compare(d0, d1);
        }
    }

}