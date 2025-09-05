package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntityGrizzlyBear;
import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import com.iafenvoy.uranus.animation.IAnimatedEntity;
import net.minecraft.block.BeehiveBlock;
import net.minecraft.block.entity.BeehiveBlockEntity;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.ai.goal.MoveToTargetPosGoal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldView;

public class GrizzlyBearAIBeehive extends MoveToTargetPosGoal {

    private final EntityGrizzlyBear bear;
    private int idleAtHiveTime = 0;
    private boolean isAboveDestinationBear;

    public GrizzlyBearAIBeehive(EntityGrizzlyBear bear) {
        super(bear, 1D, 32, 8);
        this.bear = bear;
    }

    @Override
    public boolean canStart() {
        return !bear.isBaby() && super.canStart();
    }

    @Override
    public void stop() {
        idleAtHiveTime = 0;
    }

    @Override
    public double getDesiredDistanceToTarget() {
        return 2D;
    }

    @Override
    public void tick() {
        super.tick();
        var blockpos = this.getTargetPos();
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

        if (this.hasReached() && Math.abs(bear.getY() - this.targetPos.getY()) <= 3) {
            bear.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, new Vec3d(this.targetPos.getX() + 0.5D, this.targetPos.getY(), this.targetPos.getZ() + 0.5));
            if (bear.getY() + 2 < this.targetPos.getY()) {
                bear.setAnimation(EntityGrizzlyBear.ANIMATION_MAUL);
                bear.maxStandTime = 60;
                bear.setStanding(true);
            } else {
                if (bear.getAnimation() == IAnimatedEntity.NO_ANIMATION) {
                    bear.setAnimation(bear.getRandom().nextBoolean() ? EntityGrizzlyBear.ANIMATION_SWIPE_L : EntityGrizzlyBear.ANIMATION_SWIPE_R);

                }
            }
            if (this.idleAtHiveTime >= 20) {
                this.eatHive();
            } else {
                ++this.idleAtHiveTime;
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

    private void eatHive() {
        // FIXME forge
//        if (net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(bear.getWorld(), bear)) {
//            var blockstate = bear.getWorld().getBlockState(this.blockPos);
//            if (blockstate.is(AMTagRegistry.GRIZZLY_BEEHIVE)) {
//                if (bear.getWorld().getBlockEntity(this.blockPos) instanceof BeehiveBlockEntity) {
//                    final Random rand = this.bear.getRandom();
//                    BeehiveBlockEntity beehivetileentity = (BeehiveBlockEntity) bear.getWorld().getBlockEntity(this.blockPos);
//                    beehivetileentity.emptyAllLivingFromHive(null, blockstate, BeehiveBlockEntity.BeeReleaseStatus.EMERGENCY);
//                    bear.getWorld().updateNeighbourForOutputSignal(this.blockPos, blockstate.getBlock());
//                    ItemStack stack = new ItemStack(Items.HONEYCOMB);
//                    int level = 0;
//                    if (blockstate.getBlock() instanceof BeehiveBlock) {
//                        level = blockstate.getValue(BeehiveBlock.HONEY_LEVEL);
//                    }
//                    for (int i = 0; i < level; i++) {
//                        ItemEntity itementity = new ItemEntity(bear.getWorld(), blockPos.getX() + rand.nextFloat(), blockPos.getY() + rand.nextFloat(), blockPos.getZ() + rand.nextFloat(), stack);
//                        itementity.setToDefaultPickupDelay();
//                        bear.getWorld().spawnEntity(itementity);
//                    }
//                    bear.getWorld().destroyBlock(blockPos, false);
//                    if (blockstate.getBlock() instanceof BeehiveBlock) {
//                        bear.getWorld().setBlockState(blockPos, blockstate.setValue(BeehiveBlock.HONEY_LEVEL, 0));
//                    }
//                    double d0 = 15;
//                    for (Bee bee : bear.getWorld().getEntitiesOfClass(Bee.class, new AABB((double) blockPos.getX() - d0, (double) blockPos.getY() - d0, (double) blockPos.getZ() - d0, (double) blockPos.getX() + d0, (double) blockPos.getY() + d0, (double) blockPos.getZ() + d0))) {
//                        bee.setAngerTime(100);
//                        bee.setTarget(bear);
//                        bee.setStayOutOfHiveCountdown(400);
//                    }
//                    stop();
//                }
//            }
//        }
    }

    @Override
    protected boolean isTargetPos(WorldView worldIn, BlockPos pos) {
        if (worldIn.getBlockState(pos).isIn(AMTagRegistry.GRIZZLY_BEEHIVE)) {
            if (worldIn.getBlockEntity(pos) instanceof BeehiveBlockEntity && worldIn.getBlockState(pos).getBlock() instanceof BeehiveBlock) {
                int i = worldIn.getBlockState(pos).get(BeehiveBlock.HONEY_LEVEL);
                return i > 0;
            }
        }
        return false;
    }
}
