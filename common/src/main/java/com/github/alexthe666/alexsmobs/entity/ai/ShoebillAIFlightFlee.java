package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntityShoebill;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;

import java.util.EnumSet;

public class ShoebillAIFlightFlee extends Goal {

    private final EntityShoebill bird;
    private BlockPos currentTarget = null;
    private int executionTime = 0;

    public ShoebillAIFlightFlee(EntityShoebill bird) {
        this.setControls(EnumSet.of(Control.MOVE));
        this.bird = bird;
    }

    @Override
    public void stop(){
        currentTarget = null;
        executionTime = 0;
        bird.setFlying(false);
    }

    @Override
    public boolean shouldContinue(){
        return bird.isFlying() && (executionTime < 15 || !bird.isOnGround());
    }

    @Override
    public boolean canStart() {
        return bird.revengeCooldown > 0 && bird.isOnGround();
    }

    @Override
    public void start(){
        if(bird.isOnGround()){
            bird.setFlying(true);
        }
    }

    @Override
    public void tick() {
        executionTime++;
        if (currentTarget == null) {
            if (bird.revengeCooldown == 0) {
                currentTarget = getBlockGrounding(bird.getPos());
            } else {
                currentTarget = getBlockInViewAway(bird.getPos());
            }
        }
        if (currentTarget != null) {
            bird.getNavigation().startMovingTo(currentTarget.getX() + 0.5F, currentTarget.getY() + 0.5F, currentTarget.getZ() + 0.5F, 1F);
            if(this.bird.squaredDistanceTo(Vec3d.ofCenter(currentTarget)) < 4){
                currentTarget = null;
            }
        }
        if (bird.revengeCooldown == 0 && (bird.isTouchingWater() || !bird.getWorld().isAir(bird.getBlockPos().down()))) {
            stop();
            bird.setFlying(false);
        }
    }
    public BlockPos getBlockInViewAway(Vec3d fleePos) {
        final float radius = 0.75F * (0.7F * 6) * -3 - bird.getRandom().nextInt(24);
        final float neg = bird.getRandom().nextBoolean() ? 1 : -1;
        final float renderYawOffset = bird.bodyYaw;
        final float angle = (Maths.STARTING_ANGLE * renderYawOffset) + 3.15F + (bird.getRandom().nextFloat() * neg);
        final double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
        final double extraZ = radius * MathHelper.cos(angle);
        BlockPos radialPos = AMBlockPos.fromCoords(fleePos.x + extraX, 0, fleePos.z + extraZ);
        BlockPos ground = bird.getWorld().getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, radialPos);
        int distFromGround = (int) bird.getY() - ground.getY();
        int flightHeight = 4 + bird.getRandom().nextInt(10);
        BlockPos newPos = radialPos.up(distFromGround > 8 ? flightHeight : (int) bird.getY() + bird.getRandom().nextInt(6) + 1);
        if (!bird.isTargetBlocked(Vec3d.ofCenter(newPos)) && bird.squaredDistanceTo(Vec3d.ofCenter(newPos)) > 6) {
            return newPos;
        }
        return null;
    }

    public BlockPos getBlockGrounding(Vec3d fleePos) {
        final float radius = 0.75F * (0.7F * 6) * -3 - bird.getRandom().nextInt(24);
        final float neg = bird.getRandom().nextBoolean() ? 1 : -1;
        final float renderYawOffset = bird.bodyYaw;
        final float angle = (Maths.STARTING_ANGLE * renderYawOffset) + 3.15F + (bird.getRandom().nextFloat() * neg);
        final double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
        final double extraZ = radius * MathHelper.cos(angle);
        BlockPos radialPos = AMBlockPos.fromCoords(fleePos.x + extraX, 0, fleePos.z + extraZ);
        BlockPos ground = bird.getWorld().getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, radialPos);
        if (!bird.isTargetBlocked(Vec3d.ofCenter(ground.up()))) {
            return ground;
        }
        return null;
    }
}
