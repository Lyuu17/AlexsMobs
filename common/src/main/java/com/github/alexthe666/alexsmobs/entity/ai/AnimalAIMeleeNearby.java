package com.github.alexthe666.alexsmobs.entity.ai;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;

public class AnimalAIMeleeNearby extends Goal {
    private final MobEntity entity;
    private final int range;
    private final double speed;
    private BlockPos fightStartPos = null;

    public AnimalAIMeleeNearby(MobEntity entity, int range, double speed) {
        this.setControls(EnumSet.of(Goal.Control.MOVE));
        this.entity = entity;
        this.range = range;
        this.speed = speed;
    }

    @Override
    public boolean canStart() {
        return entity.getTarget() != null && entity.getTarget().isAlive() && !entity.hasPassengers();
    }

    @Override
    public void start(){
        fightStartPos = entity.getSteppingPos();
    }

    @Override
    public void stop(){
        entity.getNavigation().stop();
        fightStartPos = null;
    }

    @Override
    public void tick(){
        if(entity.distanceTo(entity.getTarget()) < 3F + entity.getWidth() + entity.getTarget().getWidth()){
            entity.tryAttack(entity.getTarget());
            entity.lookAtEntity(entity.getTarget(), 180F, 180F);
        }else{
            if(fightStartPos != null){
                if(entity.squaredDistanceTo(Vec3d.ofCenter(fightStartPos)) < range * range){
                    entity.getNavigation().startMovingTo(entity.getTarget(), speed);

                }else{
                    entity.getNavigation().startMovingTo(fightStartPos.getX() + 0.5F, fightStartPos.getY() + 0.5F, fightStartPos.getZ() + 0.5F, 0.4F + speed);
                }

            }
        }

    }
}
