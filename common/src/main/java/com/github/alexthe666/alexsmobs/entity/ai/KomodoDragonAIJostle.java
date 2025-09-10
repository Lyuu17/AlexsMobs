package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntityKomodoDragon;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

public class KomodoDragonAIJostle  extends Goal {

    private static final TargetPredicate JOSTLE_PREDICATE = TargetPredicate.createNonAttackable().setBaseMaxDistance(16D).ignoreVisibility();
    protected EntityKomodoDragon targetKomodoDragon;
    private final EntityKomodoDragon komodo;
    private final World world;
    private float angle;

    public KomodoDragonAIJostle(EntityKomodoDragon moose) {
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK, Control.TARGET));
        this.komodo = moose;
        this.world = moose.getWorld();
    }

    @Override
    public boolean canStart() {
        if (this.komodo.isJostling() || this.komodo.isInLove() || this.komodo.isSitting() || this.komodo.hasPassengers() || this.komodo.shouldFollow() || komodo.hasVehicle() || this.komodo.isBaby() || this.komodo.getTarget() != null || this.komodo.jostleCooldown > 0) {
            return false;
        }
        if(this.komodo.instantlyTriggerJostleAI || this.komodo.getRandom().nextInt(30) == 0){
            this.komodo.instantlyTriggerJostleAI = false;
            if (this.komodo.getJostlingPartner() instanceof EntityKomodoDragon) {
                targetKomodoDragon = (EntityKomodoDragon) komodo.getJostlingPartner();
                return targetKomodoDragon.jostleCooldown == 0;
            } else {
                EntityKomodoDragon possiblePartner = this.getNearbyKomodoDragon();
                if (possiblePartner != null) {
                    this.komodo.setJostlingPartner(possiblePartner);
                    possiblePartner.setJostlingPartner(komodo);
                    targetKomodoDragon = possiblePartner;
                    targetKomodoDragon.instantlyTriggerJostleAI = true;
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void start(){
        this.komodo.jostleTimer = 0;
        this.angle = 0;
        setJostleDirection(this.komodo.getRandom().nextBoolean());
    }

    public void setJostleDirection(boolean dir){
        this.komodo.jostleDirection = dir;
        this.targetKomodoDragon.jostleDirection = dir;
    }

    @Override
    public void stop() {
        this.komodo.setJostling(false);
        this.komodo.setJostlingPartner(null);
        this.komodo.jostleTimer = 0;
        this.angle = 0;
        this.komodo.getNavigation().stop();
        if (this.targetKomodoDragon != null) {
            this.targetKomodoDragon.setJostling(false);
            this.targetKomodoDragon.setJostlingPartner(null);
            this.targetKomodoDragon.jostleTimer = 0;
            this.targetKomodoDragon = null;
        }
    }

    @Override
    public void tick() {
        if(targetKomodoDragon != null){
            this.komodo.lookAtEntity(targetKomodoDragon, 360, 180);
            this.komodo.setJostling(true);
            float x = (float)(komodo.getX() - targetKomodoDragon.getX());
            float y = Math.abs((float)(komodo.getY() - targetKomodoDragon.getY()));
            float z = (float)(komodo.getZ() - targetKomodoDragon.getZ());
            double distXZ = Math.sqrt((x * x + z * z));
            if (distXZ < 1.8F) {
                this.komodo.getNavigation().stop();
                this.komodo.getMoveControl().strafeTo(-0.5F, 0);
            } else if(distXZ > 2.4F) {
                this.komodo.setJostling(false);
                this.komodo.getNavigation().startMovingTo(targetKomodoDragon, 1);
            }else{
                this.komodo.lookAtEntity(targetKomodoDragon, 360, 180);
                float f = komodo.getRandom().nextFloat() - 0.5F;
                //perfect jostle condition
                if(komodo.jostleDirection){
                    if(angle < 10){
                        angle += 1;
                    }else{
                        komodo.jostleDirection = false;
                    }
                    this.komodo.getMoveControl().strafeTo(f * 1, -0.4F);
                }
                if(!komodo.jostleDirection){
                    if(angle > -10){
                        angle -= 1;
                    }else{
                        komodo.jostleDirection = true;
                    }
                    this.komodo.getMoveControl().strafeTo(f * 1, 0.4F);
                }
                if(this.komodo.getRandom().nextInt(15) == 0 && this.komodo.isOnGround()){
                    komodo.pushBackJostling(targetKomodoDragon, 0.1F);
                }
                komodo.nextJostleAngleFromServer = angle;
                this.komodo.jostleTimer++;
                this.targetKomodoDragon.jostleTimer++;
                if(this.komodo.jostleTimer > 500 || y > 2.0F){
                    komodo.velocityDirty = true;
                    if(komodo.isOnGround()){
                        komodo.pushBackJostling(targetKomodoDragon, 0.4F);
                    }
                    if(targetKomodoDragon.isOnGround()){
                        targetKomodoDragon.pushBackJostling(komodo, 0.4F);
                    }
                    this.komodo.jostleTimer = 0;
                    this.targetKomodoDragon.jostleTimer = 0;
                    this.komodo.jostleCooldown = 700 + this.komodo.getRandom().nextInt(2000);
                    this.targetKomodoDragon.jostleTimer = 0;
                    this.targetKomodoDragon.jostleCooldown = 700 + this.targetKomodoDragon.getRandom().nextInt(2000);
                    this.stop();
                }
            }
        }
    }

    @Override
    public boolean shouldContinue() {
        return !this.komodo.isBaby() && !this.komodo.isInLove() && !this.komodo.hasPassengers() && !this.komodo.isSitting() && this.komodo.getTarget() == null && targetKomodoDragon != null && targetKomodoDragon.isAlive() && komodo.jostleCooldown == 0 && targetKomodoDragon.jostleCooldown == 0;
    }

    @Nullable
    private EntityKomodoDragon getNearbyKomodoDragon() {
        List<EntityKomodoDragon> komodoDragons = this.world.getTargets(EntityKomodoDragon.class, JOSTLE_PREDICATE, this.komodo, this.komodo.getBoundingBox().expand(16.0D));
        double lvt_2_1_ = 1.7976931348623157E308D;
        EntityKomodoDragon lvt_4_1_ = null;

        for (var lvt_6_1_ : komodoDragons) {
            if (this.komodo.canJostleWith(lvt_6_1_) && this.komodo.squaredDistanceTo(lvt_6_1_) < lvt_2_1_) {
                lvt_4_1_ = lvt_6_1_;
                lvt_2_1_ = this.komodo.squaredDistanceTo(lvt_6_1_);
            }
        }

        return lvt_4_1_;
    }

}
