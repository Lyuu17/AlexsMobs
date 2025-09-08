package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntityFroststalker;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;

public class FroststalkerAIMelee extends Goal {

    private final EntityFroststalker froststalker;
    private boolean willJump = false;
    private boolean hasJumped = false;
    private boolean clockwise = false;
    private int pursuitTime = 0;
    private int maxPursuitTime = 0;
    private BlockPos pursuitPos = null;
    private int startingOrbit = 0;

    public FroststalkerAIMelee(EntityFroststalker froststalker) {
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
        this.froststalker = froststalker;
    }

    @Override
    public boolean canStart() {
        if(froststalker.getTarget() != null && froststalker.getTarget().isAlive()){
            if(froststalker.isValidLeader(froststalker.getTarget())){
                return froststalker.getAttacker() != null && froststalker.getAttacker().equals(froststalker.getTarget());
            }else{
                return !froststalker.isFleeingFire();
            }
        }
        return false;
    }

    @Override
    public boolean shouldContinue() {
        var target = froststalker.getTarget();
        return target != null && !froststalker.isValidLeader(target);
    }

    @Override
    public void start() {
        willJump = froststalker.getRandom().nextInt(2) == 0;
        hasJumped = false;
        clockwise = froststalker.getRandom().nextBoolean();
        pursuitPos = null;
        pursuitTime = 0;
        maxPursuitTime = 40 + froststalker.getRandom().nextInt(40);
        startingOrbit = froststalker.getRandom().nextInt(360);
        this.froststalker.frostJump();
    }

    @Override
    public void tick() {
        froststalker.setBipedal(true);
        froststalker.standFor(20);
        var target = froststalker.getTarget();
        boolean flag = false;
        if ((hasJumped || froststalker.isTackling()) && froststalker.isOnGround()) {
            hasJumped = false;
            willJump = false;
            froststalker.setTackling(false);
        }
        if (target != null && target.isAlive()) {
            if (pursuitTime < maxPursuitTime) {
                pursuitTime++;
                pursuitPos = getBlockNearTarget(target);

                float extraSpeed = 0.2F * Math.max(5F - froststalker.distanceTo(target), 0F);
                if (pursuitPos != null) {
                    froststalker.getNavigation().startMovingTo(pursuitPos.getX(), pursuitPos.getY(), pursuitPos.getZ(), 1.0F + extraSpeed);
                }else{
                    froststalker.getNavigation().startMovingTo(target, 1.0F);
                }
            } else if (willJump && pursuitTime == maxPursuitTime) {
                froststalker.lookAtEntity(target, 180F, 10F);
                if (froststalker.distanceTo(target) > 10F) {
                    froststalker.getNavigation().startMovingTo(target, 1.0F);
                } else if (froststalker.isOnGround() && froststalker.canSee(target)) {
                    this.froststalker.setTackling(true);
                    hasJumped = true;
                    Vec3d vector3d = this.froststalker.getVelocity();
                    Vec3d vector3d1 = new Vec3d(target.getX() - this.froststalker.getX(), 0.0D, target.getZ() - this.froststalker.getZ());
                    if (vector3d1.lengthSquared() > 1.0E-7D) {
                        vector3d1 = vector3d1.normalize().multiply(0.9D).add(vector3d.multiply(0.8D));
                    }
                    this.froststalker.setVelocity(vector3d1.x, 0.6F, vector3d1.z);
                } else {
                    flag = true;
                }
            } else {
                if (!froststalker.isTackling()) {
                    froststalker.getNavigation().startMovingTo(target, 1.0F);
                }
            }
            if (froststalker.isTackling() && froststalker.distanceTo(target) <= froststalker.getWidth() + target.getWidth() + 1.1F && froststalker.canSee(target)) {
                target.damage(froststalker.getDamageSources().mobAttack(froststalker), (float) froststalker.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE));
                start();
            }
            if (!flag) {
                if (froststalker.distanceTo(target) <= froststalker.getWidth() + target.getWidth() + 1.1F && froststalker.canSee(target)) {
                    if (pursuitTime == maxPursuitTime) {
                        if (!froststalker.isTackling()) {
                            froststalker.tryAttack(target);
                        }
                        start();
                    }
                }
            }
        }
        if (target != null && !froststalker.isOnGround()) {
            froststalker.lookAtEntity(target, 180F, 10F);
            froststalker.bodyYaw = froststalker.getYaw();
        }
    }

    public BlockPos getBlockNearTarget(LivingEntity target) {
        float radius = froststalker.getRandom().nextInt(5) + 3 + target.getWidth();
//        float neg = froststalker.getRandom().nextBoolean() ? 1 : -1;
//        float renderYawOffset = froststalker.bodyYaw;
        int orbit = (int) (startingOrbit + (pursuitTime / (float) maxPursuitTime) * 360);
        float angle = (Maths.STARTING_ANGLE * (clockwise ? -orbit : orbit));
        double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
        double extraZ = radius * MathHelper.cos(angle);
        BlockPos circlePos = AMBlockPos.fromCoords(target.getX() + extraX, target.getEyeY(), target.getZ() + extraZ);
        while (!froststalker.getWorld().getBlockState(circlePos).isAir() && circlePos.getY() < froststalker.getWorld().getTopY()) {
            circlePos = circlePos.up();
        }
        while (!froststalker.getWorld().getBlockState(circlePos.down()).hasSolidTopSurface(froststalker.getWorld(), circlePos.down(), froststalker) && circlePos.getY() > 1) {
            circlePos = circlePos.down();
        }
        if (froststalker.getPathfindingFavor(circlePos) > -1) {
            return circlePos;
        }
        return null;
    }

    @Override
    public void stop() {
        froststalker.setTackling(false);
    }
}