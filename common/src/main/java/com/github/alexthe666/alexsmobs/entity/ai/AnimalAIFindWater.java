package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.ISemiAquatic;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;

import java.util.EnumSet;

public class AnimalAIFindWater extends Goal {
    private final PathAwareEntity creature;
    private BlockPos targetPos;
    private final int executionChance = 30;

    public AnimalAIFindWater(PathAwareEntity creature) {
        this.creature = creature;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (this.creature.isOnGround() && !this.creature.getWorld().getFluidState(this.creature.getBlockPos()).isIn(FluidTags.WATER)) {
            if (this.creature instanceof ISemiAquatic && ((ISemiAquatic) this.creature).shouldEnterWater() && (this.creature.getTarget() != null || this.creature.getRandom().nextInt(executionChance) == 0)) {
                targetPos = generateTarget();
                return targetPos != null;
            }
        }
        return false;
    }

    @Override
    public void start() {
        if (targetPos != null) {
            this.creature.getNavigation().startMovingTo(targetPos.getX(), targetPos.getY(), targetPos.getZ(), 1D);
        }
    }

    @Override
    public void tick() {
        if (targetPos != null) {
            this.creature.getNavigation().startMovingTo(targetPos.getX(), targetPos.getY(), targetPos.getZ(), 1D);
        }
    }

    @Override
    public boolean shouldContinue() {
        if (this.creature instanceof ISemiAquatic && !((ISemiAquatic) this.creature).shouldEnterWater()) {
            this.creature.getNavigation().stop();
            return false;
        }
        return !this.creature.getNavigation().isIdle()
                && targetPos != null
                && !this.creature.getWorld().getFluidState(this.creature.getBlockPos()).isIn(FluidTags.WATER);
    }

    public BlockPos generateTarget() {
        BlockPos blockpos = null;
        final var random = this.creature.getRandom();
        final int range = this.creature instanceof ISemiAquatic ? ((ISemiAquatic) this.creature).getWaterSearchRange() : 14;
        for(int i = 0; i < 15; i++) {
            BlockPos blockPos = this.creature.getBlockPos().add(random.nextInt(range) - range/2, 3, random.nextInt(range) - range/2);
            while (this.creature.getWorld().isAir(blockPos) && blockPos.getY() > 1) {
                blockPos = blockPos.down();
            }

            if (this.creature.getWorld().getFluidState(blockPos).isIn(FluidTags.WATER)) {
                blockpos = blockPos;
            }
        }
        return blockpos;
    }
}
