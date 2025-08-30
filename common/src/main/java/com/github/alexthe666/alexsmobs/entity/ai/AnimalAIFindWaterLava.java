package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.ISemiAquatic;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;

import java.util.EnumSet;

public class AnimalAIFindWaterLava extends Goal {
    private final PathAwareEntity creature;
    private BlockPos targetPos;
    private final int executionChance = 30;
    private final double speed;

    public AnimalAIFindWaterLava(PathAwareEntity creature) {
        this(creature, 1.0F);
    }

    public AnimalAIFindWaterLava(PathAwareEntity creature, double speed) {
        this.creature = creature;
        this.speed = speed;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (this.creature.isOnGround() && !this.creature.getWorld().getFluidState(this.creature.getBlockPos()).isIn(FluidTags.WATER) && !this.creature.getWorld().getFluidState(this.creature.getBlockPos()).isIn(FluidTags.LAVA)){
            if(this.creature instanceof ISemiAquatic && ((ISemiAquatic) this.creature).shouldEnterWater() && (this.creature.getTarget() != null || this.creature.getRandom().nextInt(executionChance) == 0)){
                targetPos = generateTarget();
                return targetPos != null;
            }
        }
        return false;
    }

    @Override
    public void start() {
        if(targetPos != null){
            this.creature.getNavigation().startMovingTo(targetPos.getX(), targetPos.getY(), targetPos.getZ(), speed);
        }
    }

    @Override
    public void tick() {
        if(targetPos != null){
            this.creature.getNavigation().startMovingTo(targetPos.getX(), targetPos.getY(), targetPos.getZ(), speed);
        }
    }

    @Override
    public boolean shouldContinue() {
        if(this.creature instanceof ISemiAquatic && !((ISemiAquatic) this.creature).shouldEnterWater()){
            this.creature.getNavigation().stop();
            return false;
        }
        return !this.creature.getNavigation().isIdle() && targetPos != null && !this.creature.getWorld().getFluidState(this.creature.getBlockPos()).isIn(FluidTags.LAVA) && !this.creature.getWorld().getFluidState(this.creature.getBlockPos()).isIn(FluidTags.WATER);
    }

    public BlockPos generateTarget() {
        BlockPos blockpos = null;
        final var random = this.creature.getRandom();
        int range = this.creature instanceof ISemiAquatic ? ((ISemiAquatic) this.creature).getWaterSearchRange() : 14;
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
