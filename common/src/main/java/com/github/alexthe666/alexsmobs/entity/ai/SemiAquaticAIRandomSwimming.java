package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.ISemiAquatic;
import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.goal.WanderAroundGoal;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public class SemiAquaticAIRandomSwimming extends WanderAroundGoal {

    public SemiAquaticAIRandomSwimming(AnimalEntity creature, double speed, int chance) {
        super(creature, speed, chance, false);
    }

    public boolean canStart() {
        if (this.mob.hasPassengers() || ((ISemiAquatic)this.mob).shouldStopMoving() || this.mob.getTarget() != null || !this.mob.isTouchingWater() && !this.mob.isInLava() && this.mob instanceof ISemiAquatic && !((ISemiAquatic) this.mob).shouldEnterWater()) {
            return false;
        } else {
            if (!this.ignoringChance) {
                if (this.mob.getRandom().nextInt(this.chance) != 0) {
                    return false;
                }
            }
            final var vector3d = this.getWanderTarget();
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
    protected Vec3d getWanderTarget() {
        if(this.mob.hasPositionTarget() && this.mob.squaredDistanceTo(Vec3d.ofCenter(this.mob.getPositionTarget())) > this.mob.getPositionTargetRange() * this.mob.getPositionTargetRange()){
            return NoPenaltyTargeting.findTo(this.mob, 7, 3, Vec3d.ofBottomCenter(this.mob.getPositionTarget()), 1);
        }
        if(this.mob.getRandom().nextFloat() < 0.3F){
            var vector3d = findSurfaceTarget(this.mob, 15, 7);
            if(vector3d != null){
                return vector3d;
            }
        }
        var vector3d = NoPenaltyTargeting.find(this.mob, 7, 3);

        for(int i = 0; vector3d != null && !this.mob.getWorld().getFluidState(AMBlockPos.fromVec3(vector3d)).isIn(FluidTags.LAVA) && !this.mob.getWorld().getBlockState(AMBlockPos.fromVec3(vector3d)).canPathfindThrough(this.mob.getWorld(), AMBlockPos.fromVec3(vector3d), NavigationType.WATER) && i++ < 15; vector3d = NoPenaltyTargeting.find(this.mob, 10, 7)) {
        }

        return vector3d;
    }

    private boolean canJumpTo(BlockPos pos, int dx, int dz, int scale) {
        final var blockpos = pos.add(dx * scale, 0, dz * scale);
        return this.mob.getWorld().getFluidState(blockpos).isIn(FluidTags.LAVA) || this.mob.getWorld().getFluidState(blockpos).isIn(FluidTags.WATER) && !this.mob.getWorld().getBlockState(blockpos).blocksMovement();
    }

    private boolean isAirAbove(BlockPos pos, int dx, int dz, int scale) {
        return this.mob.getWorld().getBlockState(pos.add(dx * scale, 1, dz * scale)).isAir() && this.mob.getWorld().getBlockState(pos.add(dx * scale, 2, dz * scale)).isAir();
    }

    protected Vec3d findSurfaceTarget(PathAwareEntity creature, int i, int i1) {
        var upPos = creature.getBlockPos();
        while (creature.getWorld().getFluidState(upPos).isIn(FluidTags.WATER) || creature.getWorld().getFluidState(upPos).isIn(FluidTags.LAVA)) {
            upPos = upPos.up();
        }
        if (isAirAbove(upPos.down(), 0, 0, 0) && canJumpTo(upPos.down(), 0, 0, 0)) {
            return new Vec3d(upPos.getX() + 0.5F, upPos.getY() - 1F, upPos.getZ() + 0.5F);
        }
        return null;
    }
}
