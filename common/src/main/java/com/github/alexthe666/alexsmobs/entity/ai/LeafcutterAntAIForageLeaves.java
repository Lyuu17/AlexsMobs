package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.EntityLeafcutterAnt;
import com.github.alexthe666.alexsmobs.platform.PlatformEvent;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.ai.goal.MoveToTargetPosGoal;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldView;

public class LeafcutterAntAIForageLeaves extends MoveToTargetPosGoal {

    private final EntityLeafcutterAnt ant;
    private int idleAtLeavesTime = 0;
    private int randomLeafCheckCooldown = 40;
    private BlockPos logStartPos = null;
    private BlockPos logTopPos = null;
    private final int searchRange;
    private final int verticalSearchRange;
    private int moveToCooldown = 0;

    public LeafcutterAntAIForageLeaves(EntityLeafcutterAnt LeafcutterAnt) {
        super(LeafcutterAnt, 1D, 15, 3);
        searchRange = 15;
        verticalSearchRange = 3;
        this.ant = LeafcutterAnt;
    }

    @Override
    public boolean canStart() {
        return !ant.isBaby() && !ant.hasLeaf() && !ant.isBaby() && !ant.isQueen() && super.canStart();
    }

    @Override
    public boolean shouldContinue() {
        return super.shouldContinue() && !ant.hasLeaf();
    }

    @Override
    public void stop() {
        idleAtLeavesTime = 0;
        logStartPos = null;
        logTopPos = null;
    }

    @Override
    public void start() {
        moveToCooldown = 10 + ant.getRandom().nextInt(10);
    }

    @Override
    public double getDesiredDistanceToTarget() {
        return 2.0D;
    }

    @Override
    public boolean shouldResetPath() {
        return this.tryingTime % 40 == 0 && logStartPos == null;
    }

    @Override
    public void tick() {
        if(moveToCooldown > 0){
            moveToCooldown--;
        }
        if (randomLeafCheckCooldown > 0) {
            randomLeafCheckCooldown--;
        } else {
            randomLeafCheckCooldown = 30 + ant.getRandom().nextInt(50);
            for (var dir : Direction.values()) {
                var offset = this.ant.getBlockPos().offset(dir);
                if (isTargetPos(this.ant.getWorld(), offset) && ant.getRandom().nextInt(1) == 0) {
                    targetPos = offset;
                    logStartPos = null;
                }
            }
        }

        if (ant.getAttachmentFacing() == Direction.UP) {
            this.ant.getMoveControl().moveTo(targetPos.getX() + 0.5F, targetPos.getY() - 1D, targetPos.getZ() + 0.5F, 1);
            this.ant.setVelocity(ant.getVelocity().add(0, 0.5, 0));
            if (ant.getRandom().nextInt(2) == 0 && isTargetPos(this.ant.getWorld(), ant.getBlockPos().up())) {
                targetPos = ant.getBlockPos().up();
            }
        } else if (targetPos.getY() > ant.getY() + 2F || logStartPos != null) {
            ant.getNavigation().stop();
            if (ant.getRandom().nextInt(5) == 0 && isTargetPos(this.ant.getWorld(), ant.getBlockPos().down())) {
                targetPos = ant.getBlockPos().down();
            }
            if (logStartPos != null) {
                double xDif = logStartPos.getX() + 0.5 - ant.getX();
                double zDif = logStartPos.getZ() + 0.5 - ant.getZ();
                float f = (float) (MathHelper.atan2(zDif, xDif) * (double) MathHelper.DEGREES_PER_RADIAN) - 90.0F;
                ant.setYaw(f);
                ant.bodyYaw = ant.getYaw();
                Vec3d vec = new Vec3d(logStartPos.getX() + 0.5, ant.getY(), logStartPos.getZ() + 0.5);
                vec = vec.subtract(ant.getPos());
                if (ant.isOnGround() || ant.isClimbing())
                    this.ant.setVelocity(vec.normalize().multiply(0.1, 0, 0.1).add(0, ant.getVelocity().y, 0));
                if(moveToCooldown <= 0){
                    moveToCooldown = 20 + ant.getRandom().nextInt(30);
                    this.ant.getNavigation().startMovingTo(logStartPos.getX(), ant.getY(), logStartPos.getZ(), 1);
                }
                if (Math.abs(xDif) < 0.6 && Math.abs(zDif) < 0.6) {
                    ant.setVelocity(ant.getVelocity().multiply(0D, 1D, 0D));
                    this.ant.getMoveControl().moveTo(logStartPos.getX() + 0.5D, ant.getY() + 2, logStartPos.getZ() + 0.5D, 1);
                    BlockPos test = new BlockPos(logStartPos.getX(), (int) ant.getY(), logStartPos.getZ());
                    if (!ant.getWorld().getBlockState(test).isIn(BlockTags.LOGS) && ant.getAttachmentFacing() == Direction.DOWN) {
                        this.stop();
                        return;
                    }
                }
            } else {
                for (int i = 0; i < 15; i++) {
                    var test = targetPos.add(6 - ant.getRandom().nextInt(12), -ant.getRandom().nextInt(7), 6 - ant.getRandom().nextInt(12));
                    if (ant.getWorld().getBlockState(test).isIn(BlockTags.LOGS)) {
                        logStartPos = test;
                        break;
                    }
                }
            }
            tryingTime++;
        } else {
            super.tick();
            logStartPos = null;
        }
        if (this.hasReached() || ant.getBlockPos().up().equals(targetPos)) {
            ant.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, new Vec3d(targetPos.getX() + 0.5D, targetPos.getY(), targetPos.getZ() + 0.5));
            ant.setAnimation(EntityLeafcutterAnt.ANIMATION_BITE);
            if (this.idleAtLeavesTime >= 6) {
                ant.setLeafHarvestedPos(targetPos);
                ant.setLeafHarvestedState(ant.getWorld().getBlockState(targetPos));
                if (!ant.hasLeaf()) {
                    this.breakLeaves();
                }
                ant.setLeaf(true);
                stop();
                this.idleAtLeavesTime = 0;
            } else {
                ++this.idleAtLeavesTime;
            }
        }

    }

    private void breakLeaves() {
        BlockState blockstate = ant.getWorld().getBlockState(this.targetPos);
        if (blockstate.isIn(AMTagRegistry.LEAFCUTTER_ANT_BREAKABLES)) {
            if (PlatformEvent.getMobGriefingEvent(ant.getWorld(), ant)) {
                ant.getWorld().breakBlock(targetPos, false);
                if (ant.getRandom().nextFloat() > AMConfig.leafcutterAntBreakLeavesChance) {
                    ant.getWorld().setBlockState(targetPos, blockstate);
                }
            }
        }
    }

    @Override
    protected boolean isTargetPos(WorldView worldIn, BlockPos pos) {
        return worldIn.getBlockState(pos).isIn(AMTagRegistry.LEAFCUTTER_ANT_BREAKABLES);
    }

    @Override
    protected boolean findTargetPos() {
        int i = this.searchRange;
        int j = this.verticalSearchRange;
        BlockPos blockpos = this.mob.getBlockPos();
        if(ant.hasHive() && ant.getHivePos() != null){
            blockpos = ant.getHivePos();
            i *= 2;
        }
        var blockpos$mutableblockpos = new BlockPos.Mutable();

        for (int k = this.lowestY; k <= j; k = k > 0 ? -k : 1 - k) {
            for (int l = 0; l < i; ++l) {
                for (int i1 = 0; i1 <= l; i1 = i1 > 0 ? -i1 : 1 - i1) {
                    for (int j1 = i1 < l && i1 > -l ? l : 0; j1 <= l; j1 = j1 > 0 ? -j1 : 1 - j1) {
                        blockpos$mutableblockpos.set(blockpos, i1, k - 1, j1);
                        if (this.mob.isInWalkTargetRange(blockpos$mutableblockpos) && this.isTargetPos(this.mob.getWorld(), blockpos$mutableblockpos)) {
                            this.targetPos = blockpos$mutableblockpos;
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }
}
