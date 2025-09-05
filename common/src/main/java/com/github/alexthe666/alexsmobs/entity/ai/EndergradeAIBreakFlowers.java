package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntityEndergrade;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.ai.goal.MoveToTargetPosGoal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldView;

public class EndergradeAIBreakFlowers extends MoveToTargetPosGoal {

    private final EntityEndergrade endergrade;
    private int idleAtFlowerTime = 0;
    private boolean isAboveDestinationBear;

    public EndergradeAIBreakFlowers(EntityEndergrade bird) {
        super(bird, 1D, 32, 8);
        this.endergrade = bird;
    }

    @Override
    public boolean canStart() {
        return !endergrade.isBaby() && !endergrade.hasItemTarget && super.canStart();
    }

    @Override
    public boolean shouldContinue() {
        return !endergrade.hasItemTarget && super.shouldContinue();
    }

    @Override
    public void stop() {
        idleAtFlowerTime = 0;
        this.endergrade.stopWandering = false;
    }

    @Override
    public double getDesiredDistanceToTarget() {
        return 2D;
    }

    @Override
    public void tick() {
        super.tick();
        this.endergrade.stopWandering = true;
        var blockpos = this.getTargetPos();
        if (!isWithinXZDist(blockpos, this.mob.getPos(), this.getDesiredDistanceToTarget())) {
            this.isAboveDestinationBear = false;
            ++this.tryingTime;
            this.mob.getMoveControl().moveTo((double) ((float) blockpos.getX()) + 0.5D, blockpos.getY() - 0.5D, (double) ((float) blockpos.getZ()) + 0.5D, 1);
        } else {
            this.isAboveDestinationBear = true;
            --this.tryingTime;
        }

        if (this.hasReached() && Math.abs(endergrade.getY() - targetPos.getY()) <= 2) {
            endergrade.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, new Vec3d(blockpos.getX() + 0.5D, targetPos.getY(), targetPos.getZ() + 0.5));
            if (this.idleAtFlowerTime >= 20) {
                endergrade.bite();
                this.pollinate();
                this.stop();
            } else {
                ++this.idleAtFlowerTime;
            }
        }

    }

    private boolean isWithinXZDist(BlockPos blockpos, Vec3d positionVec, double distance) {
        return blockpos.getSquaredDistance(new BlockPos((int) positionVec.x, blockpos.getY(), (int) positionVec.z)) < distance * distance;
    }

    @Override
    protected boolean hasReached() {
        return this.isAboveDestinationBear;
    }

    private void pollinate() {
        endergrade.getWorld().breakBlock(targetPos, true);
        stop();
    }

    @Override
    protected boolean isTargetPos(WorldView worldIn, BlockPos pos) {
        return worldIn.getBlockState(pos).isIn(AMTagRegistry.ENDERGRADE_BREAKABLES);
    }
}
