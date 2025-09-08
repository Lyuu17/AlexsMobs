package com.github.alexthe666.alexsmobs.entity.ai;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class AnimalAIFleeLight extends Goal {
    protected final PathAwareEntity creature;
    private double shelterX;
    private double shelterY;
    private double shelterZ;
    private final double movementSpeed;
    private final World world;
    private int executeChance = 50;
    private int lightLevel = 10;

    public AnimalAIFleeLight(PathAwareEntity creature, double p_i1623_2_) {
        this.creature = creature;
        this.movementSpeed = p_i1623_2_;
        this.world = creature.getWorld();
        this.setControls(EnumSet.of(Control.MOVE));
    }

    public AnimalAIFleeLight(PathAwareEntity creature, double p_i1623_2_, int chance, int level) {
        this.creature = creature;
        this.movementSpeed = p_i1623_2_;
        this.world = creature.getWorld();
        this.executeChance = chance;
        this.lightLevel = level;
        this.setControls(EnumSet.of(Control.MOVE));
    }

    @Override
    public boolean canStart() {
        if (this.creature.getTarget() != null || this.creature.getRandom().nextInt(executeChance) != 0) {
            return false;
        } else if (this.world.getLightLevel(this.creature.getBlockPos()) < lightLevel) {
            return false;
        } else {
            return this.isPossibleShelter();
        }
    }

    protected boolean isPossibleShelter() {
        var lvt_1_1_ = this.findPossibleShelter();
        if (lvt_1_1_ == null) {
            return false;
        } else {
            this.shelterX = lvt_1_1_.x;
            this.shelterY = lvt_1_1_.y;
            this.shelterZ = lvt_1_1_.z;
            return true;
        }
    }

    @Override
    public boolean shouldContinue() {
        return !this.creature.getNavigation().isIdle();
    }

    @Override
    public void start() {
        this.creature.getNavigation().startMovingTo(this.shelterX, this.shelterY, this.shelterZ, this.movementSpeed);
    }

    @Nullable
    protected Vec3d findPossibleShelter() {
        var lvt_1_1_ = this.creature.getRandom();
        var lvt_2_1_ = this.creature.getBlockPos();

        for(int lvt_3_1_ = 0; lvt_3_1_ < 10; ++lvt_3_1_) {
            var lvt_4_1_ = lvt_2_1_.add(lvt_1_1_.nextInt(20) - 10, lvt_1_1_.nextInt(6) - 3, lvt_1_1_.nextInt(20) - 10);
            if (this.creature.getWorld().getLightLevel(lvt_4_1_) < lightLevel) {
                return Vec3d.ofBottomCenter(lvt_4_1_);
            }
        }

        return null;
    }
}
