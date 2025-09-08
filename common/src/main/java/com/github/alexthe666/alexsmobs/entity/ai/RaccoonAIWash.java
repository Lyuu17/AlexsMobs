package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntityRaccoon;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.event.GameEvent;

import java.util.EnumSet;

public class RaccoonAIWash extends Goal {
    private final EntityRaccoon raccoon;
    private BlockPos waterPos;
    private BlockPos targetPos;
    private int washTime = 0;
    private int executionChance = 30;
    private final Direction[] HORIZONTALS = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    public RaccoonAIWash(EntityRaccoon creature) {
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        this.raccoon = creature;
    }

    @Override
    public boolean canStart() {
        if(raccoon.getMainHandStack().isEmpty()){
            return false;
        }
        if (raccoon.lookForWaterBeforeEatingTimer > 0) {
            waterPos = generateTarget();
            if (waterPos != null) {
                targetPos = getLandPos(waterPos);
                return targetPos != null;
            }
        }
        return false;
    }

    @Override
    public void start() {
        this.raccoon.lookForWaterBeforeEatingTimer = 1800;
    }

    @Override
    public void stop() {
        targetPos = null;
        waterPos = null;
        washTime = 0;
        this.raccoon.setWashPos(null);
        this.raccoon.setWashing(false);
        this.raccoon.lookForWaterBeforeEatingTimer = 100;
        this.raccoon.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (targetPos != null && waterPos != null) {
            double dist = this.raccoon.squaredDistanceTo(Vec3d.ofCenter(waterPos));
            if (dist > 2 && this.raccoon.isWashing()) {
                this.raccoon.setWashing(false);
            }
            if (dist <= 1F) {
                double d0 = waterPos.getX() + 0.5D - this.raccoon.getX();
                double d2 = waterPos.getZ() + 0.5D - this.raccoon.getZ();
                float yaw = (float)(MathHelper.atan2(d2, d0) * (double)MathHelper.DEGREES_PER_RADIAN) - 90.0F;
                this.raccoon.setYaw(yaw);
                this.raccoon.headYaw= yaw;
                this.raccoon.bodyYaw = yaw;
                this.raccoon.getNavigation().stop();
                this.raccoon.setWashing(true);
                this.raccoon.setWashPos(waterPos);
                this.raccoon.lookForWaterBeforeEatingTimer = 0;
                if(washTime % 10 == 0){
                    this.raccoon.emitGameEvent(GameEvent.BLOCK_ACTIVATE);
                    this.raccoon.playSound(SoundEvents.ENTITY_GENERIC_SWIM, 0.7F, 0.5F + raccoon.getRandom().nextFloat());
                }
                washTime++;
                if(washTime > 100 || raccoon.isHoldingSugar() && washTime > 20){
                    this.stop();
                    if(!raccoon.isHoldingSugar()){
                        raccoon.onEatItem();
                    }
                    this.raccoon.postWashItem(raccoon.getMainHandStack());
                    if(this.raccoon.getMainHandStack().getItem().hasRecipeRemainder()){
                        this.raccoon.dropItem(this.raccoon.getMainHandStack().getItem().getRecipeRemainder());
                    }
                    this.raccoon.getMainHandStack().decrement(1);
                }
            }else{
                this.raccoon.getNavigation().startMovingTo(waterPos.getX(), waterPos.getY(), waterPos.getZ(), 1.2D);
            }

        }
    }

    @Override
    public boolean shouldContinue() {
        if(raccoon.getMainHandStack().isEmpty()){
            return false;
        }
        return targetPos != null && !this.raccoon.isTouchingWater() && EntityRaccoon.isRaccoonFood(this.raccoon.getMainHandStack());
    }

    public BlockPos generateTarget() {
        BlockPos blockpos = null;
        final Random random = this.raccoon.getRandom();
        int range = 32;
        for (int i = 0; i < 15; i++) {
            BlockPos blockpos1 = this.raccoon.getBlockPos().add(random.nextInt(range) - range / 2, 3, random.nextInt(range) - range / 2);
            while (this.raccoon.getWorld().isAir(blockpos1) && blockpos1.getY() > raccoon.getWorld().getBottomY()) {
                blockpos1 = blockpos1.down();
            }
            if (isConnectedToLand(blockpos1)) {
                blockpos = blockpos1;
            }
        }
        return blockpos;
    }

    public boolean isConnectedToLand(BlockPos pos) {
        if (this.raccoon.getWorld().getFluidState(pos).isIn(FluidTags.WATER)) {
            for (Direction dir : HORIZONTALS) {
                BlockPos offsetPos = pos.offset(dir);
                if (this.raccoon.getWorld().getFluidState(offsetPos).isEmpty() && this.raccoon.getWorld().getFluidState(offsetPos.up()).isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    public BlockPos getLandPos(BlockPos pos) {
        if (this.raccoon.getWorld().getFluidState(pos).isIn(FluidTags.WATER)) {
            for (Direction dir : HORIZONTALS) {
                BlockPos offsetPos = pos.offset(dir);
                if (this.raccoon.getWorld().getFluidState(offsetPos).isEmpty() && this.raccoon.getWorld().getFluidState(offsetPos.up()).isEmpty()) {
                    return offsetPos;
                }
            }
        }
        return null;
    }
}
