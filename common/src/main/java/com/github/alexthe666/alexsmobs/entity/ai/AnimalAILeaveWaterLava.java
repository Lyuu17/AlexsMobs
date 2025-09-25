package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.ISemiAquatic;
import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import net.minecraft.entity.ai.FuzzyTargeting;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.EnumSet;

public class AnimalAILeaveWaterLava extends Goal {
    private final PathAwareEntity creature;
    private BlockPos targetPos;
    private final int executionChance = 30;

    public AnimalAILeaveWaterLava(PathAwareEntity creature) {
        this.creature = creature;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (this.creature.getWorld().getFluidState(this.creature.getBlockPos()).isIn(FluidTags.WATER) || this.creature.getWorld().getFluidState(this.creature.getBlockPos()).isIn(FluidTags.LAVA)){
            if(this.creature instanceof ISemiAquatic && ((ISemiAquatic) this.creature).shouldLeaveWater() && (this.creature.getTarget() != null || this.creature.getRandom().nextInt(executionChance) == 0)){
                targetPos = generateTarget();
                return targetPos != null;
            }
        }
        return false;
    }

    @Override
    public void start() {
        if(targetPos != null){
            this.creature.getNavigation().startMovingTo(targetPos.getX(), targetPos.getY(), targetPos.getZ(), 1D);
        }
    }

    @Override
    public void tick() {
        if(targetPos != null){
            this.creature.getNavigation().startMovingTo(targetPos.getX(), targetPos.getY(), targetPos.getZ(), 1D);
        }
        if(this.creature.horizontalCollision && (this.creature.isTouchingWater() || this.creature.isInLava())){
            var f1 = creature.getYaw() * MathHelper.RADIANS_PER_DEGREE;
            creature.setVelocity(creature.getVelocity().add(-MathHelper.sin(f1) * 0.2F, 0.1D, MathHelper.cos(f1) * 0.2F));

        }
    }

    @Override
    public boolean shouldContinue() {
        if(this.creature instanceof ISemiAquatic && !((ISemiAquatic) this.creature).shouldLeaveWater()){
            this.creature.getNavigation().stop();
            return false;
        }
        return !this.creature.getNavigation().isIdle() && targetPos != null && !this.creature.getWorld().getFluidState(targetPos).isIn(FluidTags.WATER)  && !this.creature.getWorld().getFluidState(targetPos).isIn(FluidTags.LAVA);
    }

    public BlockPos generateTarget() {
        var vector3d = FuzzyTargeting.find(this.creature, 23, 7);
        int tries = 0;
        while(vector3d != null && tries < 8){
            boolean waterDetected = false;
            for(var blockpos1 : BlockPos.iterate(MathHelper.floor(vector3d.x - 2.0D), MathHelper.floor(vector3d.y - 1.0D), MathHelper.floor(vector3d.z - 2.0D), MathHelper.floor(vector3d.x + 2.0D), MathHelper.floor(vector3d.y), MathHelper.floor(vector3d.z + 2.0D))) {
                if (this.creature.getWorld().getFluidState(blockpos1).isIn(FluidTags.WATER) || this.creature.getWorld().getFluidState(blockpos1).isIn(FluidTags.LAVA)) {
                    waterDetected = true;
                    break;
                }
            }
            if(waterDetected){
                vector3d = FuzzyTargeting.find(this.creature, 23, 7);
            }else{
                return AMBlockPos.fromVec3(vector3d);
            }
            tries++;
        }
        return null;
    }
}
