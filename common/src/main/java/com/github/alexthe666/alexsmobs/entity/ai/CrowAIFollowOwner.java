package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.entity.EntityCrow;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import com.github.alexthe666.alexsmobs.packet.CrowMountPlayerPacket;
import net.minecraft.block.LeavesBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.LandPathNodeMaker;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldView;

import java.util.EnumSet;

public class CrowAIFollowOwner extends Goal {
    private final EntityCrow crow;
    private final WorldView world;
    private final double followSpeed;
    private final EntityNavigation navigator;
    private final float maxDist;
    private final float minDist;
    private final boolean teleportToLeaves;
    float circlingTime = 0;
    float circleDistance = 1;
    float yLevel = 2;
    boolean clockwise = false;
    private LivingEntity owner;
    private int timeToRecalcPath;
    private float oldWaterCost;
    private int maxCircleTime;

    public CrowAIFollowOwner(EntityCrow p_i225711_1_, double p_i225711_2_, float p_i225711_4_, float p_i225711_5_, boolean p_i225711_6_) {
        this.crow = p_i225711_1_;
        this.world = p_i225711_1_.getWorld();
        this.followSpeed = p_i225711_2_;
        this.navigator = p_i225711_1_.getNavigation();
        this.minDist = p_i225711_4_;
        this.maxDist = p_i225711_5_;
        this.teleportToLeaves = p_i225711_6_;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        var lvt_1_1_ = this.crow.getOwner();
        if (lvt_1_1_ == null) {
            return false;
        } else if (lvt_1_1_.isSpectator()) {
            return false;
        } else if (this.crow.isSitting() || crow.hasVehicle()) {
            return false;
        } else if (crow.getCommand() != 1) {
            return false;
        } else if (this.crow.squaredDistanceTo(lvt_1_1_) < (double) (this.minDist * this.minDist)) {
            return false;
        } else {
            this.owner = lvt_1_1_;
            return (crow.getTarget() == null || !crow.getTarget().isAlive());
        }
    }

    @Override
    public boolean shouldContinue() {
        if (this.crow.isSitting()) {
            return false;
        } else {
            return crow.getCommand() == 1 && !crow.hasVehicle() && (crow.getTarget() == null || !crow.getTarget().isAlive());
        }
    }

    @Override
    public void start() {
        this.timeToRecalcPath = 0;
        this.oldWaterCost = this.crow.getPathfindingPenalty(PathNodeType.WATER);
        this.crow.setPathfindingPenalty(PathNodeType.WATER, 0.0F);
        clockwise = crow.getRandom().nextBoolean();
        yLevel = crow.getRandom().nextInt(1);
        circlingTime = 0;
        maxCircleTime = 20 + crow.getRandom().nextInt(100);
        circleDistance = 1F + crow.getRandom().nextFloat() * 2F;
    }

    @Override
    public void stop() {
        this.owner = null;
        this.navigator.stop();
        circlingTime = 0;
        this.crow.setPathfindingPenalty(PathNodeType.WATER, this.oldWaterCost);
    }

    @Override
    public void tick() {
        this.crow.getLookControl().lookAt(this.owner, 10.0F, (float) this.crow.getMaxLookPitchChange());
        if (!this.crow.isLeashed() && !this.crow.hasVehicle()) {
            double dist = this.crow.squaredDistanceTo(this.owner);
            if (--this.timeToRecalcPath <= 0) {
                this.timeToRecalcPath = 10;
                if (dist >= 144.0D && !crow.aiItemFlag) {
                    crow.setFlying(true);
                    crow.getMoveControl().moveTo(owner.getX(), owner.getY() + owner.getStandingEyeHeight() + 0.2F, owner.getZ(), 1F);
                    this.tryToTeleportNearEntity();
                    circlingTime = 0;
                }
            }

            if (!crow.aiItemFlag) {
                if (this.crow.isFlying()) {
                    circlingTime++;
                }
                if(circlingTime > maxCircleTime && crow.getRidingCrows(owner) < 2){
                    crow.getMoveControl().moveTo(owner.getX(), owner.getY() + owner.getStandingEyeHeight() + 0.2F, owner.getZ(), 0.7F);
                    if(crow.distanceTo(owner) < 2){
                        crow.startRiding(owner, true);
                        if (!crow.getWorld().isClient) {
                            AlexsMobs.sendMSGToAll(new CrowMountPlayerPacket(crow.getId(), owner.getId()));
                        }
                    }
                }else{
                    var circlePos = getVultureCirclePos(owner.getPos());
                    if (circlePos == null) {
                        circlePos = owner.getPos();
                    }
                    crow.setFlying(true);
                    crow.getMoveControl().moveTo(circlePos.x, circlePos.y + owner.getStandingEyeHeight() + 0.2F, circlePos.z, 0.7F);

                }

            }
        }
    }

    public Vec3d getVultureCirclePos(Vec3d target) {
        final float angle = (Maths.EIGHT_STARTING_ANGLE * (clockwise ? -circlingTime : circlingTime));
        final double extraX = circleDistance * MathHelper.sin((angle));
        final double extraZ = circleDistance * MathHelper.cos(angle);
        var pos = new Vec3d(target.x + extraX, target.y + yLevel, target.z + extraZ);
        if (crow.getWorld().isAir(AMBlockPos.fromVec3(pos))) {
            return pos;
        }
        return null;
    }

    private void tryToTeleportNearEntity() {
        var lvt_1_1_ = this.owner.getBlockPos();

        for (int lvt_2_1_ = 0; lvt_2_1_ < 10; ++lvt_2_1_) {
            int lvt_3_1_ = this.getRandomNumber(-3, 3);
            int lvt_4_1_ = this.getRandomNumber(-1, 1);
            int lvt_5_1_ = this.getRandomNumber(-3, 3);
            boolean lvt_6_1_ = this.tryToTeleportToLocation(lvt_1_1_.getX() + lvt_3_1_, lvt_1_1_.getY() + lvt_4_1_, lvt_1_1_.getZ() + lvt_5_1_);
            if (lvt_6_1_) {
                return;
            }
        }

    }

    private boolean tryToTeleportToLocation(int p_226328_1_, int p_226328_2_, int p_226328_3_) {
        if (Math.abs((double) p_226328_1_ - this.owner.getX()) < 2.0D && Math.abs((double) p_226328_3_ - this.owner.getZ()) < 2.0D) {
            return false;
        } else if (!this.isTeleportFriendlyBlock(new BlockPos(p_226328_1_, p_226328_2_, p_226328_3_))) {
            return false;
        } else {
            this.crow.refreshPositionAndAngles((double) p_226328_1_ + 0.5D, p_226328_2_, (double) p_226328_3_ + 0.5D, this.crow.getYaw(), this.crow.getPitch());
            this.navigator.stop();
            return true;
        }
    }

    private boolean isTeleportFriendlyBlock(BlockPos p_226329_1_) {
        var lvt_2_1_ = LandPathNodeMaker.getLandNodeType(this.world, p_226329_1_.mutableCopy());
        if (lvt_2_1_ != PathNodeType.WALKABLE) {
            return false;
        } else {
            var lvt_3_1_ = this.world.getBlockState(p_226329_1_.down());
            if (!this.teleportToLeaves && lvt_3_1_.getBlock() instanceof LeavesBlock) {
                return false;
            } else {
                BlockPos lvt_4_1_ = p_226329_1_.subtract(this.crow.getBlockPos());
                return this.world.isSpaceEmpty(this.crow, this.crow.getBoundingBox().offset(lvt_4_1_));
            }
        }
    }

    private int getRandomNumber(int p_226327_1_, int p_226327_2_) {
        return this.crow.getRandom().nextInt(p_226327_2_ - p_226327_1_ + 1) + p_226327_1_;
    }
}
