package com.github.alexthe666.alexsmobs.entity.ai;

import net.minecraft.entity.ai.FuzzyTargeting;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.EnumSet;

public class AnimalAILeapRandomly extends Goal {

    private final PathAwareEntity mob;
    private final int chance;
    private final int maxLeapDistance;
    private Vec3d leapToPos = null;

    public AnimalAILeapRandomly(PathAwareEntity mob, int chance, int maxLeapDistance) {
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
        this.mob = mob;
        this.chance = chance;
        this.maxLeapDistance = maxLeapDistance;
    }

    @Override
    public boolean canStart() {
        if(mob.getRandom().nextInt(this.chance) == 0 && mob.isOnGround() && mob.getNavigation().isIdle()){
            var found = FuzzyTargeting.find(mob, maxLeapDistance, maxLeapDistance);
            if(found != null && mob.squaredDistanceTo(found) < maxLeapDistance * maxLeapDistance && hasLineOfSightBlock(found)){
                leapToPos = found;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean shouldContinue() {
        return leapToPos != null && mob.squaredDistanceTo(leapToPos) < maxLeapDistance * maxLeapDistance && hasLineOfSightBlock(leapToPos);
    }

    private boolean hasLineOfSightBlock(Vec3d blockVec) {
        var Vector3d = new Vec3d(mob.getX(), mob.getEyeY(), mob.getZ());
        var result = mob.getWorld().raycast(new RaycastContext(Vector3d, blockVec, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mob));
        return blockVec.distanceTo(result.getPos()) < 1.2F;
    }

    @Override
    public void stop() {
        super.stop();
        leapToPos = null;
    }

    @Override
    public void start(){
        if(leapToPos != null){
            var vector3d = this.mob.getVelocity();
            var vector3d1 = new Vec3d(this.leapToPos.x - this.mob.getX(), 0.0D, this.leapToPos.z - this.mob.getZ());
            if (vector3d1.lengthSquared() > 1.0E-7D) {
                vector3d1 = vector3d1.normalize().multiply(0.9D).add(vector3d.multiply(0.8D));
            }
            //TODO
//            if(this.mob instanceof EntityBunfungus){
//                ((EntityBunfungus) this.mob).onJump();
//            }
            this.mob.setVelocity(vector3d1.x, 0.6F, vector3d1.z);
            mob.setYaw(-((float) MathHelper.atan2(vector3d1.x, vector3d1.z)) * MathHelper.DEGREES_PER_RADIAN);
            mob.bodyYaw = mob.getYaw();
            mob.headYaw = mob.getYaw();

            leapToPos = null;
        }
    }
}
