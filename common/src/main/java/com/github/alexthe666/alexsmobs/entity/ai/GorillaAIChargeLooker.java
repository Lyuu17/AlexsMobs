package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntityGorilla;
import com.iafenvoy.uranus.animation.IAnimatedEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;

public class GorillaAIChargeLooker extends Goal {
    private final EntityGorilla gorilla;
    private final double range = 20D;
    private PlayerEntity starer;
    private final double speed;
    private int runDelay = 0;

    public GorillaAIChargeLooker(EntityGorilla gorilla, double speed) {
        this.setControls(EnumSet.of(Control.LOOK, Control.MOVE));
        this.gorilla = gorilla;
        this.speed = speed;
    }

    @Override
    public boolean canStart() {
        if (this.gorilla.isSilverback() && !this.gorilla.isTamed() && runDelay-- == 0) {
            runDelay = 100 + gorilla.getRandom().nextInt(200);
            var playerList = this.gorilla.getWorld().getEntitiesByClass(PlayerEntity.class, this.gorilla.getBoundingBox().expand(range, range, range), EntityPredicates.EXCEPT_SPECTATOR);
            PlayerEntity closestPlayer = null;
            for (var player : playerList) {
                if (isLookingAtMe(player)) {
                    if (closestPlayer == null || player.distanceTo(gorilla) < closestPlayer.distanceTo(gorilla)) {
                        closestPlayer = player;
                    }
                }
            }
            starer = closestPlayer;
            return starer != null;
        }
        return false;
    }

    @Override
    public boolean shouldContinue() {
        return starer != null && this.gorilla.isAlive();
    }

    @Override
    public void stop() {
        this.starer = null;
        this.gorilla.setSprinting(false);
        runDelay = 300 + gorilla.getRandom().nextInt(200);
    }

    @Override
    public void tick(){
        this.gorilla.setSitting(false);
        this.gorilla.poundChestCooldown = 50;
        if(this.gorilla.distanceTo(starer) > 1 + starer.getWidth() + this.gorilla.getWidth()){
            this.gorilla.getNavigation().startMovingTo(starer, speed);
            this.gorilla.setSprinting(!this.gorilla.isSitting() && !this.gorilla.isStanding());
        }else{
            this.gorilla.getNavigation().stop();
            this.gorilla.lookAtEntity(starer, 180, 30);
            this.gorilla.setSprinting(false);
            if(this.gorilla.getAnimation() == IAnimatedEntity.NO_ANIMATION){
                this.gorilla.setStanding(true);
                this.gorilla.maxStandTime = 45;
                this.gorilla.setAnimation(EntityGorilla.ANIMATION_POUNDCHEST);
            }
            if(this.gorilla.getAnimation() == EntityGorilla.ANIMATION_POUNDCHEST && this.gorilla.getAnimationTick() >= 10){
                this.stop();
            }
        }
    }

    private boolean isLookingAtMe(PlayerEntity player) {
        Vec3d vec3 = player.getRotationVec(1.0F).normalize();
        Vec3d vec31 = new Vec3d(gorilla.getX() - player.getX(), gorilla.getEyeY() - player.getEyeY(), gorilla.getZ() - player.getZ());
        double d0 = vec31.length();
        vec31 = vec31.normalize();
        double d1 = vec3.dotProduct(vec31);
        return d1 > 1.0D - 0.025D / d0 && player.canSee(gorilla);
    }
}
