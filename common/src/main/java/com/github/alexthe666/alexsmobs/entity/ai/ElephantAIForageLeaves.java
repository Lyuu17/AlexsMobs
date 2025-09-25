package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntityElephant;
import com.github.alexthe666.alexsmobs.platform.PlatformEvent;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import com.iafenvoy.uranus.animation.IAnimatedEntity;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ai.goal.MoveToTargetPosGoal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.WorldView;

public class ElephantAIForageLeaves extends MoveToTargetPosGoal {

    private final EntityElephant elephant;
    private int idleAtLeavesTime = 0;
    private boolean isAboveDestinationBear;

    private int moveCooldown = 0;

    public ElephantAIForageLeaves(EntityElephant elephant) {
        super(elephant, 0.7D, 32, 5);
        this.elephant = elephant;
    }

    @Override
    public boolean canStart() {
        return !elephant.isBaby() && elephant.getControllingPassenger() == null && elephant.getControllingVillager() == null && elephant.getMainHandStack().isEmpty() && !elephant.aiItemFlag && super.canStart();
    }

    @Override
    public void stop() {
        idleAtLeavesTime = 0;
    }

    @Override
    public void start() {
        super.start();
        moveCooldown = 30 + elephant.getRandom().nextInt(50);
    }

    @Override
    public double getDesiredDistanceToTarget() {
        return 4D;
    }

    @Override
    public boolean shouldResetPath() {
        return moveCooldown == 0;
    }

    @Override
    public void tick() {
        if (moveCooldown > 0) {
            moveCooldown--;
        }
        var blockpos = this.getTargetPos();
        if (!isWithinXZDist(blockpos, this.mob.getPos(), this.getDesiredDistanceToTarget())) {
            this.isAboveDestinationBear = false;
            ++this.tryingTime;
            if (this.shouldResetPath()) {
                moveCooldown = 30 + elephant.getRandom().nextInt(50);
                this.mob.getNavigation().startMovingTo((double) ((float) blockpos.getX()) + 0.5D, blockpos.getY(), (double) ((float) blockpos.getZ()) + 0.5D, this.speed);
            }
        } else {
            this.isAboveDestinationBear = true;
            this.tryingTime = 0;
        }

        if (this.hasReached() && Math.abs(elephant.getY() - targetPos.getY()) <= 3) {
            elephant.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, new Vec3d(targetPos.getX() + 0.5D, targetPos.getY(), targetPos.getZ() + 0.5));
            if (elephant.getY() + 2 < targetPos.getY()) {
                if (elephant.getAnimation() == IAnimatedEntity.NO_ANIMATION) {
                    elephant.setAnimation(EntityElephant.ANIMATION_BREAKLEAVES);
                }
                elephant.setStanding(true);
                elephant.maxStandTime = 15;
            } else {
                elephant.setAnimation(EntityElephant.ANIMATION_BREAKLEAVES);
                elephant.setStanding(false);
            }
            if (this.idleAtLeavesTime >= 10) {
                this.breakLeaves();
            } else {
                ++this.idleAtLeavesTime;
            }
        }

    }

    @Override
    protected void startMovingToTarget() {}

    @Override
    protected int getInterval(PathAwareEntity p_203109_1_) {
        return 100 + p_203109_1_.getRandom().nextInt(200);
    }

    private boolean isWithinXZDist(BlockPos blockpos, Vec3d positionVec, double distance) {
        return blockpos.getSquaredDistance(new BlockPos((int) positionVec.x, blockpos.getY(), (int) positionVec.z)) < distance * distance;
    }

    @Override
    protected boolean hasReached() {
        return this.isAboveDestinationBear;
    }

    private void breakLeaves() {
        if (PlatformEvent.getMobGriefingEvent(elephant.getWorld(), elephant)) {
            var blockstate = elephant.getWorld().getBlockState(this.targetPos);
            if (blockstate.isIn(AMTagRegistry.ELEPHANT_FOODBLOCKS)) {
                elephant.getWorld().breakBlock(targetPos, false);
                final var rand = this.elephant.getRandom();
                var stack = new ItemStack(blockstate.getBlock().asItem());
                var itementity = new ItemEntity(elephant.getWorld(), targetPos.getX() + rand.nextFloat(), targetPos.getY() + rand.nextFloat(), targetPos.getZ() + rand.nextFloat(), stack);
                itementity.setToDefaultPickupDelay();
                elephant.getWorld().spawnEntity(itementity);
                if (blockstate.isIn(AMTagRegistry.DROPS_ACACIA_BLOSSOMS) && rand.nextInt(30) == 0) {
                    var banana = new ItemStack(AMItemRegistry.ACACIA_BLOSSOM.get());
                    var itementity2 = new ItemEntity(elephant.getWorld(), targetPos.getX() + rand.nextFloat(), targetPos.getY() + rand.nextFloat(), targetPos.getZ() + rand.nextFloat(), banana);
                    itementity2.setToDefaultPickupDelay();
                    elephant.getWorld().spawnEntity(itementity2);
                }
                stop();
            }
        }
    }

    @Override
    protected boolean isTargetPos(WorldView worldIn, BlockPos pos) {
        return !elephant.aiItemFlag && worldIn.getBlockState(pos).isIn(AMTagRegistry.ELEPHANT_FOODBLOCKS) && canSeeBlock(pos);
    }

    private boolean canSeeBlock(BlockPos destinationBlock) {
        final var Vector3d = new Vec3d(elephant.getX(), elephant.getEyeY(), elephant.getZ());
        final var blockVec = Vec3d.ofCenter(destinationBlock);
        final var result = elephant.getWorld().raycast(new RaycastContext(Vector3d, blockVec, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, elephant));
        return result.getBlockPos().equals(destinationBlock);
    }
}
