package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntityHummingbird;
import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import net.minecraft.block.CropBlock;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.ai.goal.MoveToTargetPosGoal;
import net.minecraft.item.BoneMealItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldView;

public class HummingbirdAIPollinate extends MoveToTargetPosGoal {

    private final EntityHummingbird bird;
    private int idleAtFlowerTime = 0;
    private boolean isAboveDestinationBear;

    public HummingbirdAIPollinate(EntityHummingbird bird) {
        super(bird, 1D, 32, 8);
        this.bird = bird;
    }

    @Override
    public boolean canStart() {
        return !bird.isBaby() && bird.pollinateCooldown == 0 && super.canStart();
    }

    @Override
    public void stop() {
        idleAtFlowerTime = 0;
    }

    @Override
    public double getDesiredDistanceToTarget() {
        return 3D;
    }

    @Override
    public void tick() {
        super.tick();
        BlockPos blockpos = this.getTargetPos();
        if (!isWithinXZDist(blockpos, this.mob.getPos(), this.getDesiredDistanceToTarget())) {
            this.isAboveDestinationBear = false;
            ++this.tryingTime;
            double speedLoc = this.speed;
            if(this.mob.squaredDistanceTo(blockpos.getX() + 0.5D, blockpos.getY() + 0.5D, blockpos.getZ() + 0.5D) >= 3){
                speedLoc = this.speed * 0.3D;
            }
            this.mob.getMoveControl().moveTo((double) ((float) blockpos.getX()) + 0.5D, blockpos.getY(), (double) ((float) blockpos.getZ()) + 0.5D, speedLoc);

        } else {
            this.isAboveDestinationBear = true;
            --this.tryingTime;
        }

        if (this.hasReached() && Math.abs(bird.getY() - targetPos.getY()) <= 2) {
            bird.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, new Vec3d(targetPos.getX() + 0.5D, targetPos.getY(), targetPos.getZ() + 0.5));
            if (this.idleAtFlowerTime >= 20) {
                this.pollinate();
                this.stop();
            } else {
                ++this.idleAtFlowerTime;
            }
        }

    }

    private boolean isGrowable(BlockPos pos, ServerWorld world) {
        var blockstate = world.getBlockState(pos);
        var block = blockstate.getBlock();
        return block instanceof CropBlock && !((CropBlock)block).isMature(blockstate);
    }

    private boolean isWithinXZDist(BlockPos blockpos, Vec3d positionVec, double distance) {
        return blockpos.getSquaredDistance(AMBlockPos.fromCoords(positionVec.x, blockpos.getY(), positionVec.z)) < distance * distance;
    }

    @Override
    protected boolean hasReached() {
        return this.isAboveDestinationBear;
    }

    private void pollinate() {
        bird.getWorld().syncWorldEvent(2005, targetPos, 0);
        bird.setCropsPollinated(bird.getCropsPollinated() + 1);
        bird.pollinateCooldown = 200;
        if(bird.getCropsPollinated() > 3){
            if(isGrowable(targetPos, (ServerWorld) bird.getWorld())){
                BoneMealItem.useOnFertilizable(new ItemStack(Items.BONE_MEAL), bird.getWorld(), targetPos);
            }
            bird.setCropsPollinated(0);
        }
    }

    @Override
    protected boolean isTargetPos(WorldView world, BlockPos pos) {
        if (world.getBlockState(pos).isIn(AMTagRegistry.HUMMINGBIRD_POLLINATES)) {
            return bird.pollinateCooldown == 0 && bird.canBlockBeSeen(pos);
        }
        return false;
    }
}
