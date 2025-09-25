package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntityGorilla;
import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import com.github.alexthe666.alexsmobs.platform.PlatformEvent;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import com.iafenvoy.uranus.animation.IAnimatedEntity;
import net.minecraft.block.BlockState;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ai.goal.MoveToTargetPosGoal;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldView;

public class GorillaAIForageLeaves extends MoveToTargetPosGoal {

    private final EntityGorilla gorilla;
    private int idleAtLeavesTime = 0;
    private boolean isAboveDestinationBear;

    public GorillaAIForageLeaves(EntityGorilla gorilla) {
        super(gorilla, 1D, 32, 3);
        this.gorilla = gorilla;
    }

    @Override
    public boolean canStart() {
        return !gorilla.isBaby() && gorilla.getMainHandStack().isEmpty() && super.canStart();
    }

    @Override
    public void stop() {
        idleAtLeavesTime = 0;
    }

    @Override
    public double getDesiredDistanceToTarget() {
        return 2D;
    }

    @Override
    public void tick() {
        super.tick();
        BlockPos blockpos = this.getTargetPos();
        if (!isWithinXZDist(blockpos, this.mob.getPos(), this.getDesiredDistanceToTarget())) {
            this.isAboveDestinationBear = false;
            ++this.tryingTime;
            if (this.shouldResetPath()) {
                this.mob.getNavigation().startMovingTo((double) ((float) blockpos.getX()) + 0.5D, blockpos.getY(), (double) ((float) blockpos.getZ()) + 0.5D, this.speed);
            }
        } else {
            this.isAboveDestinationBear = true;
            --this.tryingTime;
        }

        if (this.hasReached() && Math.abs(gorilla.getY() - targetPos.getY()) <= 3) {
            gorilla.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, new Vec3d(targetPos.getX() + 0.5D, targetPos.getY(), targetPos.getZ() + 0.5));
            if (gorilla.getY() + 2 < targetPos.getY()) {
                gorilla.setAnimation(gorilla.getRandom().nextBoolean() ? EntityGorilla.ANIMATION_BREAKBLOCK_L : EntityGorilla.ANIMATION_BREAKBLOCK_R);
                gorilla.maxStandTime = 60;
                gorilla.setStanding(true);
            } else {
                if (gorilla.getAnimation() == IAnimatedEntity.NO_ANIMATION) {
                    gorilla.setAnimation(gorilla.getRandom().nextBoolean() ? EntityGorilla.ANIMATION_BREAKBLOCK_L : EntityGorilla.ANIMATION_BREAKBLOCK_R);
                }
            }
            if (this.idleAtLeavesTime >= 20) {
                this.breakLeaves();
            } else {
                ++this.idleAtLeavesTime;
            }
        }

    }

    private boolean isWithinXZDist(BlockPos blockpos, Vec3d positionVec, double distance) {
        return blockpos.getSquaredDistance(AMBlockPos.fromCoords(positionVec.x, blockpos.getY(), positionVec.z)) < distance * distance;
    }

    @Override
    protected boolean hasReached() {
        return this.isAboveDestinationBear;
    }

    private void breakLeaves() {
        if (PlatformEvent.getMobGriefingEvent(gorilla.getWorld(), gorilla)) {
            BlockState blockstate = gorilla.getWorld().getBlockState(this.targetPos);
            if (blockstate.isIn(AMTagRegistry.GORILLA_BREAKABLES)) {
                gorilla.getWorld().breakBlock(targetPos, false);
                final var rand = this.gorilla.getRandom();
                var stack = new ItemStack(blockstate.getBlock().asItem());
                var itementity = new ItemEntity(gorilla.getWorld(), targetPos.getX() + rand.nextFloat(), targetPos.getY() + rand.nextFloat(), targetPos.getZ() + rand.nextFloat(), stack);
                itementity.setToDefaultPickupDelay();
                gorilla.getWorld().spawnEntity(itementity);
                if(blockstate.isIn(AMTagRegistry.DROPS_BANANAS) && rand.nextInt(30) == 0){
                    var banana = new ItemStack(AMItemRegistry.BANANA.get());
                    var itementity2 = new ItemEntity(gorilla.getWorld(), targetPos.getX() + rand.nextFloat(), targetPos.getY() + rand.nextFloat(), targetPos.getZ() + rand.nextFloat(), banana);
                    itementity2.setToDefaultPickupDelay();
                    gorilla.getWorld().spawnEntity(itementity2);

                }
                stop();
            }
        }
    }

    @Override
    protected boolean isTargetPos(WorldView worldIn, BlockPos pos) {
        return worldIn.getBlockState(pos).isIn(AMTagRegistry.GORILLA_BREAKABLES);
    }
}
