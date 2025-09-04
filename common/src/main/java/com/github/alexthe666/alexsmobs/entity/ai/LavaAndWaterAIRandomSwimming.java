package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.goal.WanderAroundGoal;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public class LavaAndWaterAIRandomSwimming extends WanderAroundGoal {
    public LavaAndWaterAIRandomSwimming(PathAwareEntity creature, double speed, int chance) {
        super(creature, speed, chance, false);
    }

    @Override
    public boolean canStart() {
        if (this.mob.hasPassengers() || mob.getTarget() != null) {
            return false;
        } else {
            if (!this.ignoringChance) {
                int i = this.mob.isInLava() || this.mob.isTouchingWater() ? this.chance : this.chance * 2;
                if (this.mob.getRandom().nextInt(i) != 0) {
                    return false;
                }
            }
            var vector3d = this.getWanderTarget();
            if (vector3d == null) {
                return false;
            } else {
                this.targetX = vector3d.x;
                this.targetY = vector3d.y;
                this.targetZ = vector3d.z;
                this.ignoringChance = false;
                return true;
            }
        }
    }

    @Nullable
    @Override
    protected Vec3d getWanderTarget() {
        if(this.mob.getRandom().nextFloat() < (this.mob.isInLava() ? 0.7F : 0.3F)){
            var vector3d = findSurfaceTarget(this.mob, 32, 16);
            if(vector3d != null){
                return vector3d;
            }
        }
        var vector3d = NoPenaltyTargeting.find(this.mob, 32, 16);

        for(int i = 0; vector3d != null && !this.mob.getWorld().getBlockState(AMBlockPos.fromVec3(vector3d)).canPathfindThrough(this.mob.getWorld(), AMBlockPos.fromVec3(vector3d), NavigationType.WATER) && i++ < 10; vector3d = NoPenaltyTargeting.find(this.mob, 10, 7)) {
        }

        return vector3d;
    }

    private boolean canJumpTo(BlockPos pos, int dx, int dz, int scale) {
        var blockpos = pos.add(dx * scale, 0, dz * scale);
        return (this.mob.getWorld().getFluidState(blockpos).isIn(FluidTags.WATER) && !this.mob.getWorld().getBlockState(blockpos).blocksMovement() || this.mob.getWorld().getFluidState(blockpos).isIn(FluidTags.LAVA));
    }

    private boolean isAirAbove(BlockPos pos, int dx, int dz, int scale) {
        return this.mob.getWorld().getBlockState(pos.add(dx * scale, 1, dz * scale)).isAir() && this.mob.getWorld().getBlockState(pos.add(dx * scale, 2, dz * scale)).isAir();
    }

    protected Vec3d findSurfaceTarget(PathAwareEntity creature, int i, int i1) {
        var creaturePos = creature.getPos();
        BlockPos upPos = creature.getBlockPos();
        while(creature.getWorld().getFluidState(upPos).isIn(FluidTags.LAVA) || creature.getWorld().getFluidState(upPos).isIn(FluidTags.WATER)){
            upPos = upPos.up();
        }
        if(isAirAbove(upPos.down(), 0, 0, 0) && canJumpTo(upPos.down(), 0, 0, 0)){
            return new Vec3d(upPos.getX() + 0.5F, upPos.getY() - 0.5F, upPos.getZ() + 0.5F);
        }
        return null;
    }
}
