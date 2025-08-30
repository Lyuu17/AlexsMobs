package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.ISemiAquatic;
import net.minecraft.entity.ai.goal.WanderAroundGoal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class BottomFeederAIWander extends WanderAroundGoal {
    private int waterChance = 0;
    private int landChance = 0;
    private int range = 5;

    public BottomFeederAIWander(PathAwareEntity creature, double speed, int waterChance, int landChance) {
        super(creature, speed, waterChance);
        this.waterChance = waterChance;
        this.landChance = landChance;
    }

    public BottomFeederAIWander(PathAwareEntity creature, double speed, int waterChance, int landChance, int range) {
        super(creature, speed, waterChance);
        this.waterChance = waterChance;
        this.landChance = landChance;
        this.range = range;
    }

    @Override
    public boolean canStart(){
        if (mob instanceof ISemiAquatic && ((ISemiAquatic) mob).shouldStopMoving()) {
            return false;
        }
        chance = mob.isTouchingWater() ? waterChance : landChance;
        return super.canStart();
    }

    @Override
    public boolean shouldContinue() {
        if (mob instanceof ISemiAquatic && ((ISemiAquatic) mob).shouldStopMoving())
            return false;

        return super.shouldContinue();
    }

    @Nullable
    @Override
    protected Vec3d getWanderTarget() {
        if(this.mob.isTouchingWater()) {
            BlockPos blockpos = null;
            final var random = this.mob.getRandom();
            for (int i = 0; i < 15; i++) {
                var blockPos = this.mob.getBlockPos().add(random.nextInt(range) - range / 2, 3, random.nextInt(range) - range / 2);
                while ((this.mob.getWorld().isAir(blockPos) || this.mob.getWorld().getFluidState(blockPos).isIn(FluidTags.WATER)) && blockPos.getY() > 1) {
                    blockPos = blockPos.down();
                }
                if (isBottomOfSeafloor(this.mob.getWorld(), blockPos.up())) {
                    blockpos = blockPos;
                }
            }

            return blockpos != null ? new Vec3d(blockpos.getX() + 0.5F, blockpos.getY() + 0.5F, blockpos.getZ() + 0.5F) : null;
        }else{
            return super.getWanderTarget();

        }
    }

    private boolean isBottomOfSeafloor(World world, BlockPos pos){
        return world.getFluidState(pos).isIn(FluidTags.WATER)
                && world.getFluidState(pos.down()).isEmpty()
                && world.getBlockState(pos.down()).isOpaque();
    }
}
