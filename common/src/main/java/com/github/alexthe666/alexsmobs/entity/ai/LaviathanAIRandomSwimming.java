package com.github.alexthe666.alexsmobs.entity.ai;

import net.minecraft.entity.ai.FuzzyPositions;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public class LaviathanAIRandomSwimming extends LavaAndWaterAIRandomSwimming {


    public LaviathanAIRandomSwimming(PathAwareEntity creature, double speed, int chance) {
        super(creature, speed, chance);
    }

    @Nullable
    @Override
    protected Vec3d getWanderTarget() {
        var pos = this.mob.getBlockPos().add(FuzzyPositions.localFuzz(this.mob.getRandom(), 16, 5));

        for (int i = 0; pos != null && this.mob.getWorld().getBlockState(new BlockPos(pos)).getFluidState().isEmpty() && i++ < 10; pos = this.mob.getBlockPos().add(FuzzyPositions.localFuzz(this.mob.getRandom(), 16, 5))) {
        }
        if (this.mob.getWorld().getBlockState(new BlockPos(pos)).getFluidState().isEmpty()) {
            return null;
        }
        if(mob.getRandom().nextInt(3) == 0){
            while(!this.mob.getWorld().getBlockState(pos).getFluidState().isEmpty() && pos.getY() < mob.getWorld().getTopY()){
                pos = pos.up();
            }
            pos = pos.down();
        }
        return new Vec3d(pos.getX() + 0.5F, pos.getY() + 0.5F, pos.getZ() + 0.5F);
    }

}
