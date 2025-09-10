package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntityHummingbird;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class HummingbirdAIWander extends Goal {
    private final EntityHummingbird fly;
    private final int rangeXZ;
    private final int rangeY;
    private final int chance;
    private final float speed;
    private Vec3d moveToPoint = null;

    public HummingbirdAIWander(EntityHummingbird fly, int rangeXZ, int rangeY, int chance, float speed) {
        this.setControls(EnumSet.of(Goal.Control.MOVE));
        this.fly = fly;
        this.rangeXZ = rangeXZ;
        this.rangeY = rangeY;
        this.chance = chance;
        this.speed = speed;
    }

    @Override
    public boolean canStart() {
        return fly.hummingStill > 10 && fly.getRandom().nextInt(chance) == 0 && !fly.getMoveControl().isMoving();
    }

    @Override
    public void stop() {
        moveToPoint = null;
    }

    @Override
    public boolean shouldContinue() {
        return moveToPoint != null && fly.squaredDistanceTo(moveToPoint) > 0.85D;
    }

    @Override
    public void start() {
        moveToPoint = this.getRandomLocation();
        if (moveToPoint != null) {
            fly.getMoveControl().moveTo(moveToPoint.x, moveToPoint.y, moveToPoint.z, speed);
        }
    }

    @Override
    public void tick() {
        if (moveToPoint != null) {
            fly.getMoveControl().moveTo(moveToPoint.x, moveToPoint.y, moveToPoint.z, speed);
        }
    }

    @Nullable
    private Vec3d getRandomLocation() {
        final Random random = this.fly.getRandom();
        BlockPos blockpos = null;
        BlockPos origin = fly.getFeederPos() == null ? this.fly.getBlockPos() : fly.getFeederPos();
        for(int i = 0; i < 15; i++){
            var blockpos1 = origin.add(random.nextInt(rangeXZ) - rangeXZ/2, 1, random.nextInt(rangeXZ) - rangeXZ/2);
            while(fly.getWorld().isAir(blockpos1) && blockpos1.getY() > 0){
                blockpos1 = blockpos1.down();
            }
            blockpos1 = blockpos1.up(1 + random.nextInt(3));
            if(this.fly.getWorld().isAir(blockpos1.down()) && this.fly.canBlockBeSeen(blockpos1) && this.fly.getWorld().isAir(blockpos1) && !this.fly.getWorld().isAir(blockpos1.down(2))){
                blockpos = blockpos1;
            }
        }
        return blockpos == null ? null : new Vec3d(blockpos.getX() +  0.5D, blockpos.getY() +  0.5D, blockpos.getZ() + 0.5D);
    }

    public boolean canBlockPosBeSeen(BlockPos pos) {
        double x = pos.getX() + 0.5F;
        double y = pos.getY() + 0.5F;
        double z = pos.getZ() + 0.5F;
        HitResult result = fly.getWorld().raycast(new RaycastContext(new Vec3d(fly.getX(), fly.getY() + (double) fly.getStandingEyeHeight(), fly.getZ()), new Vec3d(x, y, z), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, fly));
        double dist = result.getPos().squaredDistanceTo(x, y, z);
        return dist <= 1.0D || result.getType() == HitResult.Type.MISS;
    }

}
