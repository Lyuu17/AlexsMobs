package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntityMudBall;
import com.github.alexthe666.alexsmobs.entity.EntityMudskipper;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.event.GameEvent;

import java.util.EnumSet;

public class MudskipperAIAttack extends Goal {
    private final EntityMudskipper entity;
    private int shootCooldown = 0;
    private boolean strafed = false;

    public MudskipperAIAttack(EntityMudskipper mob) {
        this.entity = mob;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        return this.entity.getTarget() != null && this.entity.getTarget().isAlive();
    }

    @Override
    public void stop(){
        this.shootCooldown = 0;
        this.strafed = false;
    }

    @Override
    public void tick() {
        var target = this.entity.getTarget();
        boolean keepFollowing = true;
        if(shootCooldown > 0){
            shootCooldown--;
        }
        if(this.entity.getVisibilityCache().canSee(target)){
            float dist = this.entity.distanceTo(target);
            if(dist < this.entity.getWidth() + target.getWidth() + 3.0F){
                keepFollowing = false;
                this.entity.lookAtEntity(target, 360, 360);
                this.entity.getMoveControl().strafeTo(-3.0F, 0);
                strafed = true;
            }
            if(dist < 8 && shootCooldown == 0){
                EntityMudBall mudball = new EntityMudBall(this.entity.getWorld(), this.entity);
                double d0 = target.getX() - mudball.getX();
                double d1 = target.getBodyY(0.3F) - mudball.getY();
                double d2 = target.getZ() - mudball.getZ();
                float f = MathHelper.sqrt((float) (d0 * d0 + d2 * d2)) * 0.4F;
                mudball.shoot(d0, d1 + (double) f, d2, 1F, 10.0F);
                if (!this.entity.isSilent()) {
                    this.entity.emitGameEvent(GameEvent.PROJECTILE_SHOOT);
                    this.entity.getWorld().playSound(null, this.entity.getX(), this.entity.getY(), this.entity.getZ(), AMSoundRegistry.MUDSKIPPER_SPIT.get(), this.entity.getSoundCategory(), 1.0F, 1.0F + (this.entity.getRandom().nextFloat() - this.entity.getRandom().nextFloat()) * 0.2F);
                }
                this.entity.getWorld().spawnEntity(mudball);
                shootCooldown = 10 + entity.getRandom().nextInt(10);
                this.entity.openMouth(10);
            }
        }
        if(keepFollowing){
            if(strafed){
                this.entity.getMoveControl().strafeTo(0, 0);
                strafed = false;
            }
            this.entity.getNavigation().startMovingTo(target, 1.5F);
        }else{
            this.entity.getNavigation().stop();
        }
    }
}

