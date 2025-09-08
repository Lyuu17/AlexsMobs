package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.EntityCrow;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import net.minecraft.block.CropBlock;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.ai.goal.MoveToTargetPosGoal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.WorldView;

public class CrowAICircleCrops extends MoveToTargetPosGoal {

    private final EntityCrow crow;
    private int idleAtFlowerTime = 0;
    private boolean isAboveDestinationBear;
    float circlingTime = 0;
    float circleDistance = 2;
    float maxCirclingTime = 80;
    float yLevel = 2;
    boolean clockwise = false;
    boolean circlePhase = false;

    public CrowAICircleCrops(EntityCrow bird) {
        super(bird, 1D, 32, 8);
        this.crow = bird;
    }

    @Override
    public void start() {
        super.start();
        circlePhase = true;
        clockwise = crow.getRandom().nextBoolean();
        yLevel = 1 + crow.getRandom().nextInt(3);
        circleDistance = 1 + crow.getRandom().nextInt(3);
    }

    @Override
    public boolean canStart() {
        return !crow.isBaby() && AMConfig.crowsStealCrops && (crow.getTarget() == null || !crow.getTarget().isAlive()) && !crow.isTamed() && crow.fleePumpkinFlag == 0 && !crow.aiItemFlag && super.canStart();
    }

    @Override
    public boolean shouldContinue() {
        return targetPos != null && AMConfig.crowsStealCrops && (crow.getTarget() == null || !crow.getTarget().isAlive()) && !crow.isTamed() && !crow.aiItemFlag && crow.fleePumpkinFlag == 0 && super.shouldContinue();
    }

    @Override
    public void stop() {
        idleAtFlowerTime = 0;
        circlingTime = 0;
        tryingTime = 0;
        targetPos = BlockPos.ORIGIN;
    }

    @Override
    public double getDesiredDistanceToTarget() {
        return 1D;
    }

    @Override
    public void tick() {
        if(targetPos == null){
            return;
        }
        var blockpos = this.getTargetPos();
        if(circlePhase){
            this.tryingTime = 0;
            BlockPos circlePos = getVultureCirclePos(blockpos);
            if (circlePos != null) {
                crow.setFlying(true);
                crow.getMoveControl().moveTo(circlePos.getX() + 0.5D, circlePos.getY() + 0.5D, circlePos.getZ() + 0.5D, 0.7F);
            }
            circlingTime++;
            if(circlingTime > 200){
                circlingTime = 0;
                circlePhase = false;
            }
        }else{
            super.tick();
            if(crow.isOnGround()){
                crow.setFlying(false);
            }
            if (!isWithinXZDist(blockpos, this.mob.getPos(), this.getDesiredDistanceToTarget())) {
                this.isAboveDestinationBear = false;
                ++this.tryingTime;
                this.mob.getNavigation().startMovingTo((double) ((float) blockpos.getX()) + 0.5D, blockpos.getY() - 0.5D, (double) ((float) blockpos.getZ()) + 0.5D, 1);
            } else {
                this.isAboveDestinationBear = true;
                --this.tryingTime;
            }

            if (this.hasReached()) {
                crow.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, new Vec3d(targetPos.getX() + 0.5D, targetPos.getY(), targetPos.getZ() + 0.5));
                if (this.idleAtFlowerTime >= 5) {
                    this.destroyCrop();
                    this.stop();
                } else {
                    crow.peck();
                    ++this.idleAtFlowerTime;
                }
            }
        }
    }

    public BlockPos getVultureCirclePos(BlockPos target) {
        float angle = (Maths.EIGHT_STARTING_ANGLE * (clockwise ? -circlingTime : circlingTime));
        double extraX = circleDistance * MathHelper.sin((angle));
        double extraZ = circleDistance * MathHelper.cos(angle);
        BlockPos pos = AMBlockPos.fromCoords(target.getX() + 0.5F + extraX, target.getY() + 1 + yLevel, target.getZ() + 0.5F + extraZ);
        if (crow.getWorld().isAir(pos)) {
            return pos;
        }
        return null;
    }

    private boolean isWithinXZDist(BlockPos blockpos, Vec3d positionVec, double distance) {
        return blockpos.getSquaredDistance(AMBlockPos.fromCoords(positionVec.x, blockpos.getY(), positionVec.z)) < distance * distance;
    }

    @Override
    protected boolean hasReached() {
        return this.isAboveDestinationBear;
    }

    private void destroyCrop() {
        if(!canSeeBlock(targetPos)){
            stop();
            tryingTime = 1200;
            return;
        }
        if(crow.getWorld().getBlockState(targetPos).getBlock() instanceof CropBlock block){
            if(crow.getWorld().getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)){
                int cropAge = block.getAge(crow.getWorld().getBlockState(targetPos));
                if(cropAge > 0){
                    crow.getWorld().setBlockState(targetPos, block.withAge(Math.max(0, cropAge - 1)));
                }else{
                    crow.getWorld().breakBlock(targetPos, true);
                }
            }
        }else{
            if(crow.getWorld().getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) {
                crow.getWorld().breakBlock(targetPos, true);
            }
        }
        stop();
        tryingTime = 1200;
    }


    private boolean canSeeBlock(BlockPos destinationBlock) {
        final var Vector3d = new Vec3d(crow.getX(), crow.getEyeY(), crow.getZ());
        final var blockVec = Vec3d.ofCenter(destinationBlock);
        final var result = crow.getWorld().raycast(new RaycastContext(Vector3d, blockVec, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, crow));
        return result.getBlockPos().equals(destinationBlock);
    }

    @Override
    protected boolean isTargetPos(WorldView worldIn, BlockPos pos) {
        return worldIn.getBlockState(pos).isIn(AMTagRegistry.CROW_FOODBLOCKS);
    }
}
