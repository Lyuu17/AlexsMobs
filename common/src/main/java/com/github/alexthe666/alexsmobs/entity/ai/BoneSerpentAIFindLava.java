package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntityBoneSerpent;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;

import java.util.EnumSet;

public class BoneSerpentAIFindLava extends Goal {
    private final EntityBoneSerpent creature;
    private BlockPos targetPos;

    public BoneSerpentAIFindLava(EntityBoneSerpent creature) {
        this.creature = creature;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if ((this.creature.jumpCooldown == 0 || this.creature.isOnGround()) && !this.creature.getWorld().getFluidState(this.creature.getBlockPos()).isIn(FluidTags.WATER) && !this.creature.getWorld().getFluidState(this.creature.getBlockPos()).isIn(FluidTags.LAVA)){
            targetPos = generateTarget();
            return targetPos != null;
        }
        return false;
    }

    @Override
    public void start() {
        if(targetPos != null){
            this.creature.getNavigation().startMovingTo(targetPos.getX(), targetPos.getY(), targetPos.getZ(), 0.5D);
        }
    }

    @Override
    public boolean shouldContinue() {
        return !this.creature.getNavigation().isIdle() && targetPos != null && !this.creature.getWorld().getFluidState(this.creature.getBlockPos()).isIn(FluidTags.WATER) && !this.creature.getWorld().getFluidState(this.creature.getBlockPos()).isIn(FluidTags.LAVA);
    }

    public BlockPos generateTarget() {
        BlockPos blockpos = null;
        final var random = this.creature.getRandom();
        int range = 16;
        for(int i = 0; i < 15; i++){
            var blockpos1 = this.creature.getBlockPos().add(random.nextInt(range) - range/2, 3, random.nextInt(range) - range/2);
            while(this.creature.getWorld().isAir(blockpos1) && blockpos1.getY() > 1){
                blockpos1 = blockpos1.down();
            }
            if(this.creature.getWorld().getFluidState(blockpos1).isIn(FluidTags.WATER) || this.creature.getWorld().getFluidState(blockpos1).isIn(FluidTags.LAVA)){
                blockpos = blockpos1;
            }
        }
        return blockpos;
    }
}
