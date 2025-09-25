package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntityMudskipper;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

public class MudskipperAIDisplay extends Goal {

    private static final TargetPredicate JOSTLE_PREDICATE = TargetPredicate.createNonAttackable().setBaseMaxDistance(16D).ignoreVisibility();
    protected EntityMudskipper partner;
    private EntityMudskipper mudskipper;
    private World world;
    private float angle;
    private Vec3d center = null;

    public MudskipperAIDisplay(EntityMudskipper mudskipper) {
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK, Control.TARGET));
        this.mudskipper = mudskipper;
        this.world = mudskipper.getWorld();
    }

    @Override
    public boolean canStart() {
        if (this.mudskipper.isDisplaying() || this.mudskipper.shouldFollow() || this.mudskipper.isSitting() || this.mudskipper.isInLove()  || this.mudskipper.hasPassengers() || mudskipper.hasVehicle() || this.mudskipper.isBaby() || this.mudskipper.getTarget() != null || this.mudskipper.isOnGround() || this.mudskipper.displayCooldown > 0) {
            return false;
        }
        if(this.mudskipper.instantlyTriggerDisplayAI || this.mudskipper.getRandom().nextInt(30) == 0){
            this.mudskipper.instantlyTriggerDisplayAI = false;
            if (this.mudskipper.getDisplayingPartner() instanceof EntityMudskipper) {
                partner = (EntityMudskipper) mudskipper.getDisplayingPartner();
                return partner.displayCooldown == 0;
            } else {
                EntityMudskipper possiblePartner = this.getNearbyMudskipper();
                if (possiblePartner != null) {
                    this.mudskipper.setDisplayingPartner(possiblePartner);
                    possiblePartner.setDisplayingPartner(mudskipper);
                    partner = possiblePartner;
                    partner.instantlyTriggerDisplayAI = true;
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void start(){
        this.mudskipper.displayTimer = 0;
        this.angle = 0;
        setDisplayDirection(this.mudskipper.getRandom().nextBoolean());
    }

    public void setDisplayDirection(boolean dir){
        this.mudskipper.displayDirection = dir;
        this.partner.displayDirection = !dir;
    }

    @Override
    public void stop() {
        this.center = null;
        this.mudskipper.setDisplaying(false);
        this.mudskipper.setDisplayingPartner(null);
        this.mudskipper.displayTimer = 0;
        this.angle = 0;
        this.mudskipper.getNavigation().stop();
        if (this.partner != null) {
            this.partner.setDisplaying(false);
            this.partner.setDisplayingPartner(null);
            this.partner.displayTimer = 0;
            this.partner = null;
        }

    }

    @Override
    public void tick() {
        if(partner != null){
            if(center == null || mudskipper.getRandom().nextInt(100) == 0){
                center = new Vec3d((mudskipper.getX() + partner.getX()) / 2F, (mudskipper.getY() + partner.getY()) / 2F, (mudskipper.getZ() + partner.getZ()) / 2F);
            }
            this.mudskipper.setDisplaying(true);
            float x = (float)(mudskipper.getX() - partner.getX());
            float y = Math.abs((float)(mudskipper.getY() - partner.getY()));
            float z = (float)(mudskipper.getZ() - partner.getZ());
            double distXZ = Math.sqrt((x * x + z * z));

            if (distXZ > 3F) {
                mudskipper.getNavigation().startMovingTo(partner, 1F);
            }else{
                float speed = mudskipper.getRandom().nextFloat() * 0.5F + 0.8F;
                if(mudskipper.displayDirection){
                    if(angle < 180){
                        angle += 10;
                    }else{
                        mudskipper.displayDirection = false;
                    }
                }
                if(!mudskipper.displayDirection){
                    if(angle > -180){
                        angle -= 10;
                    }else{
                        mudskipper.displayDirection = true;
                    }
                }
                if(distXZ < 0.8F){
                    if(this.mudskipper.isOnGround() && this.partner.isOnGround()){
                        this.mudskipper.lookAtEntity(this.partner, 360, 360);
                        this.setDisplayDirection(!mudskipper.displayDirection);
                        this.mudskipper.openMouth(10 + this.mudskipper.getRandom().nextInt(20));
                        this.partner.setVelocity(partner.getVelocity().add(0.2F * mudskipper.getRandom().nextFloat(), 0.35F, 0.2F * mudskipper.getRandom().nextFloat()));
                    }
                }
                var circle = getCirclingPosOf(center, 1.5F + mudskipper.getRandom().nextFloat());
                var dirVec = circle.subtract(mudskipper.getPos());
                float headAngle = -(float) (MathHelper.atan2(dirVec.x, dirVec.z) * (double) MathHelper.DEGREES_PER_RADIAN);
                this.mudskipper.getNavigation().startMovingTo(circle.x, circle.y, circle.z, speed);
                mudskipper.setYaw(headAngle);
                mudskipper.headYaw= headAngle;
                mudskipper.bodyYaw = headAngle;
                mudskipper.nextDisplayAngleFromServer = angle;
                this.mudskipper.displayTimer++;
                this.partner.displayTimer++;
                if(this.mudskipper.displayTimer > 400 || y > 2.0F){
                    this.mudskipper.getNavigation().stop();
                    this.partner.getNavigation().stop();
                    mudskipper.velocityDirty = true;
                    this.mudskipper.displayTimer = 0;
                    this.partner.displayTimer = 0;
                    this.mudskipper.displayCooldown = 200 + this.mudskipper.getRandom().nextInt(200);
                    this.partner.displayTimer = 0;
                    this.partner.displayCooldown = 200 + this.partner.getRandom().nextInt(200);
                    this.stop();
                }
            }
        }
    }

    public Vec3d getCirclingPosOf(Vec3d center, double circleDistance) {
        float cir = (Maths.STARTING_ANGLE * angle);
        double extraX = circleDistance * MathHelper.sin((cir));
        double extraZ = circleDistance * MathHelper.cos(cir);
        return center.add(extraX, 0, extraZ);
    }

    @Override
    public boolean shouldContinue() {
        return !this.mudskipper.isBaby() && !this.mudskipper.shouldFollow() && !this.mudskipper.isSitting() && !this.mudskipper.isInLove() && !this.mudskipper.hasPassengers() && this.mudskipper.getTarget() == null && partner != null && partner.isAlive() && mudskipper.displayCooldown == 0 && partner.displayCooldown == 0;
    }

    @Nullable
    private EntityMudskipper getNearbyMudskipper() {
        List<EntityMudskipper> skippers = this.world.getTargets(EntityMudskipper.class, JOSTLE_PREDICATE, this.mudskipper, this.mudskipper.getBoundingBox().expand(16.0D));
        double lvt_2_1_ = 1.7976931348623157E308D;
        EntityMudskipper lvt_4_1_ = null;

        for (var lvt_6_1_ : skippers) {
            if (this.mudskipper.canDisplayWith(lvt_6_1_) && this.mudskipper.squaredDistanceTo(lvt_6_1_) < lvt_2_1_) {
                lvt_4_1_ = lvt_6_1_;
                lvt_2_1_ = this.mudskipper.squaredDistanceTo(lvt_6_1_);
            }
        }

        return lvt_4_1_;
    }
}
