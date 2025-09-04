package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.goal.WanderAroundGoal;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public class AnimalAISwimBottom extends WanderAroundGoal {
    public AnimalAISwimBottom(PathAwareEntity p_i48937_1_, double p_i48937_2_, int p_i48937_4_) {
        super(p_i48937_1_, p_i48937_2_, p_i48937_4_);
    }

    @Nullable
    @Override
    protected Vec3d getWanderTarget() {
        var vec = NoPenaltyTargeting.find(this.mob, 10, 7);

        for(int var2 = 0; vec != null && !this.mob.getWorld().getBlockState(AMBlockPos.fromVec3(vec)).canPathfindThrough(this.mob.getWorld(), AMBlockPos.fromVec3(vec), NavigationType.WATER) && var2++ < 10; vec = NoPenaltyTargeting.find(this.mob, 10, 7)) {
        }
        int yDrop = 1 + this.mob.getRandom().nextInt(3);
        if(vec != null){
            var pos = AMBlockPos.fromVec3(vec);
            while(this.mob.getWorld().getFluidState(pos).isIn(FluidTags.WATER) && this.mob.getWorld().getBlockState(pos).canPathfindThrough(this.mob.getWorld(), AMBlockPos.fromVec3(vec), NavigationType.WATER) && pos.getY() > 1){
                pos = pos.down();
            }
            pos = pos.up();
            int yUp = 0;
            while(this.mob.getWorld().getFluidState(pos).isIn(FluidTags.WATER) && this.mob.getWorld().getBlockState(pos).canPathfindThrough(this.mob.getWorld(), AMBlockPos.fromVec3(vec), NavigationType.WATER) && yUp < yDrop){
                pos = pos.up();
                yUp++;
            }
            return Vec3d.ofCenter(pos);
        }

        return vec;
    }
}
