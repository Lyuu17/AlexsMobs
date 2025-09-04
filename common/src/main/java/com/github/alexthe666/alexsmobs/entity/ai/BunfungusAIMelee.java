package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntityBunfungus;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;

public class BunfungusAIMelee extends Goal {

    private final EntityBunfungus chungus;
    private boolean hasJumped = false;
    private int jumpCooldown = 0;

    public BunfungusAIMelee(EntityBunfungus chungus) {
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        this.chungus = chungus;
    }

    @Override
    public boolean canStart() {
        if(chungus.getTarget() != null && chungus.getTarget().isAlive()){
            hasJumped = false;
            return true;
        }
        return false;
    }

    @Override
    public void tick(){
        if(jumpCooldown > 0){
            jumpCooldown--;
        }
        double dist = chungus.distanceTo(chungus.getTarget()) - chungus.getTarget().getWidth();
        if(dist < 2.0D){
            if(hasJumped){
                if(!chungus.isOnGround()){
                    chungus.getTarget().damage(chungus.getDamageSources().mobAttack(chungus), 10);
                }
                hasJumped = false;
            }else{
                if(chungus.getRandom().nextBoolean()){
                    chungus.setAnimation(EntityBunfungus.ANIMATION_SLAM);
                }else{
                    chungus.setAnimation(EntityBunfungus.ANIMATION_BELLY);
                }
            }
        }else if(dist < 5.0D || !chungus.canSee(chungus.getTarget()) || jumpCooldown > 0 || chungus.isInsideWaterOrBubbleColumn()){
            chungus.getNavigation().startMovingTo(chungus.getTarget(), 1.0D);
        }else{
            chungus.getNavigation().stop();
            if(chungus.isOnGround()){
                var vector3d = this.chungus.getVelocity();
                var vector3d1 = new Vec3d(chungus.getTarget().getX() - this.chungus.getX(), 0.0D, chungus.getTarget().getZ() - this.chungus.getZ());
                if (vector3d1.lengthSquared() > 1.0E-7D) {
                    vector3d1 = vector3d1.normalize().multiply(0.9D).add(vector3d.multiply(0.8D));
                }
                this.chungus.onJump();
                this.chungus.setVelocity(vector3d1.x, 0.6F, vector3d1.z);
                chungus.setYaw(-((float) MathHelper.atan2(vector3d1.x, vector3d1.z)) * MathHelper.DEGREES_PER_RADIAN);
                chungus.bodyYaw = chungus.getYaw();
                hasJumped = true;
                jumpCooldown = 10;
            }
        }
    }
}