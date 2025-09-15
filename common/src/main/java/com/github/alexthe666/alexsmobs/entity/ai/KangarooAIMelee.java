package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntityKangaroo;
import com.iafenvoy.uranus.animation.IAnimatedEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

public class KangarooAIMelee extends MeleeAttackGoal {

    private final EntityKangaroo kangaroo;
    private BlockPos waterPos;
    private int waterCheckTick = 0;
    private int waterTimeout = 0;

    public KangarooAIMelee(EntityKangaroo kangaroo, double speedIn, boolean useLongMemory) {
        super(kangaroo, speedIn, useLongMemory);
        this.kangaroo = kangaroo;
    }

    @Override
    public boolean canStart() {
        return super.canStart();
    }

    @Override
    public void tick() {
        boolean dontSuper = false;
        var target = kangaroo.getTarget();
        if (target != null) {
            if (target == kangaroo.getAttacker()) {
                if (target.distanceTo(kangaroo) < kangaroo.getWidth() + 1F && target.isTouchingWater()) {
                    target.setVelocity(target.getVelocity().add(0, -0.09, 0));
                    target.setAir(target.getAir() - 30);
                }
                if (waterPos == null || !kangaroo.getWorld().getFluidState(waterPos).isIn(FluidTags.WATER)) {
                    kangaroo.setVisualFlag(0);
                    waterCheckTick++;
                    waterPos = generateWaterPos();
                } else {
                    kangaroo.setPathfindingPenalty(PathNodeType.WATER, 0);
                    kangaroo.setPathfindingPenalty(PathNodeType.WATER_BORDER, 0);
                    double localSpeed = MathHelper.clamp(kangaroo.squaredDistanceTo(waterPos.getX(), waterPos.getY(), waterPos.getZ()) * 0.5F, 1D, 2.3D);
                    kangaroo.getMoveControl().moveTo(waterPos.getX(), waterPos.getY(), waterPos.getZ(), localSpeed);
                    if (kangaroo.isTouchingWater()){
                        waterTimeout++;
                    }
                    if(waterTimeout < 1400){
                        dontSuper = true;
                        attack(target, kangaroo.squaredDistanceTo(target));
                    }
                    if (kangaroo.isTouchingWater() || kangaroo.squaredDistanceTo(Vec3d.ofCenter(waterPos)) < 10) {
                        kangaroo.totalMovingProgress = 0;
                    }
                    if(kangaroo.squaredDistanceTo(Vec3d.ofCenter(waterPos)) > 10){
                        kangaroo.setVisualFlag(0);
                    }
                    if (kangaroo.squaredDistanceTo(Vec3d.ofCenter(waterPos)) < 3 && kangaroo.isTouchingWater()) {
                        kangaroo.setStanding(true);
                        kangaroo.maxStandTime = 100;
                        kangaroo.getLookControl().lookAt(target, 360, 180);
                        kangaroo.setVisualFlag(1);
                    }
                }
            }else{

            }
            if (!dontSuper) {
                super.tick();
            }
        }
    }

    @Override
    public boolean shouldContinue() {
        return waterPos != null && this.kangaroo.getTarget() != null || super.shouldContinue();
    }

    @Override
    public void stop() {
        super.stop();
        waterCheckTick = 0;
        waterTimeout = 0;
        waterPos = null;
        kangaroo.setVisualFlag(0);
        kangaroo.setPathfindingPenalty(PathNodeType.WATER, 8);
        kangaroo.setPathfindingPenalty(PathNodeType.WATER_BORDER, 8);
    }

    public BlockPos generateWaterPos() {
        BlockPos blockpos = null;
        final Random random = this.kangaroo.getRandom();
        int range = 15;
        for (int i = 0; i < 15; i++) {
            BlockPos blockpos1 = this.kangaroo.getBlockPos().add(random.nextInt(range) - range / 2, 3, random.nextInt(range) - range / 2);
            while (this.kangaroo.getWorld().isAir(blockpos1) && blockpos1.getY() > 1) {
                blockpos1 = blockpos1.down();
            }
            if (this.kangaroo.getWorld().getFluidState(blockpos1).isIn(FluidTags.WATER)) {
                blockpos = blockpos1;
            }
        }
        return blockpos;
    }

    @Override
    protected void attack(LivingEntity enemy, double distToEnemySqr) {
        double d0 = this.getSquaredMaxAttackDistance(enemy) + 5D;
        if (distToEnemySqr <= d0) {
            if(kangaroo.isTouchingWater()){
                float f1 = kangaroo.getYaw() * MathHelper.RADIANS_PER_DEGREE;
                kangaroo.setVelocity(kangaroo.getVelocity().add(-MathHelper.sin(f1) * 0.3F, 0.0D, MathHelper.cos(f1) * 0.3F));
                enemy.takeKnockback(1F, enemy.getX() - kangaroo.getX(), enemy.getZ() - kangaroo.getZ());

            }
            this.resetCooldown();
            if(kangaroo.getAnimation() == IAnimatedEntity.NO_ANIMATION){
                if(kangaroo.getRandom().nextBoolean()){
                    kangaroo.setAnimation(EntityKangaroo.ANIMATION_KICK);
                }else{
                    if(!kangaroo.getMainHandStack().isEmpty()){
                        kangaroo.setAnimation(kangaroo.isLeftHanded() ? EntityKangaroo.ANIMATION_PUNCH_L : EntityKangaroo.ANIMATION_PUNCH_R);
                    }else{
                        kangaroo.setAnimation(kangaroo.getRandom().nextBoolean() ? EntityKangaroo.ANIMATION_PUNCH_R : EntityKangaroo.ANIMATION_PUNCH_L);
                    }
                }
            }
        }
    }

}