package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntityCaiman;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;

public class CaimanAIMelee extends Goal {
    private final EntityCaiman caiman;
    private int grabTime = 0;

    public CaimanAIMelee(EntityCaiman caiman) {
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
        this.caiman = caiman;
    }

    @Override
    public boolean canStart() {
        var target = caiman.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public void stop(){
        caiman.setHeldMobId(-1);
        grabTime = 0;
    }

    @Override
    public void tick() {
        if(grabTime < 0){
            grabTime++;
        }
        var target = caiman.getTarget();
        if(target != null){
            double bbWidth = (caiman.getWidth() + target.getWidth()) / 2D;
            double dist = caiman.distanceTo(target);
            boolean flag = false;
            if(dist < bbWidth + 2F){
               if(grabTime >= 0){
                   if(grabTime % 25 == 0){
                       target.damage(caiman.isTamed() ? caiman.getDamageSources().drown() : caiman.getDamageSources().mobAttack(caiman), (float) caiman.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE));
                   }
                   grabTime++;
                   var shakePreyPos = caiman.getShakePreyPos();
                   var minus = new Vec3d(shakePreyPos.x - target.getX(), 0, shakePreyPos.z - target.getZ()).normalize();
                   target.setVelocity(target.getVelocity().multiply(0.6F, 0.6F, 0.6F).add(minus.multiply(0.35F)));
                   flag = true;
                   if(grabTime > getGrabDuration()){
                       grabTime = -10;
                   }
               }
            }
            caiman.setHeldMobId(flag ? target.getId() : -1);
            if(dist > bbWidth && !flag){
                caiman.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, target.getEyePos());
                caiman.getNavigation().startMovingTo(target, 1.2F);
            }
        }
    }

    private int getGrabDuration() {
        if(caiman.isTamed() && caiman.tameAttackFlag){
            return 300;
        }
        return 2;
    }
}