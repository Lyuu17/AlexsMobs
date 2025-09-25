package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntityGeladaMonkey;
import com.iafenvoy.uranus.animation.IAnimatedEntity;
import net.minecraft.entity.ai.goal.Goal;

import java.util.EnumSet;

public class GeladaAIGroom extends Goal {

    private final EntityGeladaMonkey monkey;
    private int groomTime = 0;
    private int groomCooldown = 220;
    private EntityGeladaMonkey beingGroomed;

    public GeladaAIGroom(EntityGeladaMonkey monkey) {
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        this.monkey = monkey;
    }

    @Override
    public boolean canStart() {
        if(groomCooldown > 0){
            groomCooldown--;
            return false;
        }else{
            groomCooldown = 200 + monkey.getRandom().nextInt(1000);
            EntityGeladaMonkey nearestMonkey = null;
            for (var entity : monkey.getWorld().getNonSpectatingEntities(EntityGeladaMonkey.class, monkey.getBoundingBox().expand(15F))) {
                if (entity.getId() != monkey.getId() && monkey.canBeGroomed() && (nearestMonkey == null || monkey.distanceTo(nearestMonkey) > monkey.distanceTo(entity))) {
                    nearestMonkey = entity;
                }
            }
            beingGroomed = nearestMonkey;
            return beingGroomed != null;
        }
    }

    @Override
    public boolean shouldContinue() {
        return beingGroomed != null && beingGroomed.isAlive() && !beingGroomed.shouldStopBeingGroomed() && groomTime < 200 && (beingGroomed.groomerID == -1 || beingGroomed.groomerID == monkey.getId());
    }

    @Override
    public void stop(){
        groomTime = 0;
        monkey.isGrooming = false;
        if(beingGroomed != null){
            beingGroomed.groomerID = -1;
        }
        beingGroomed = null;
    }

    @Override
    public void tick() {
        double dist = monkey.distanceTo(beingGroomed);
        if(dist < monkey.getWidth() + 0.5F){
            monkey.isGrooming = true;
            beingGroomed.groomerID = monkey.getId();
            monkey.setSitting(true);
            groomTime++;
            if(groomTime % 50 == 0){
                monkey.heal(1);
            }
            if(monkey.getAnimation() == IAnimatedEntity.NO_ANIMATION){
                monkey.setAnimation(EntityGeladaMonkey.ANIMATION_GROOM);
            }
            monkey.getNavigation().stop();
            monkey.lookAtEntity(beingGroomed, 360, 360);
        }else{
            monkey.isGrooming = false;
            beingGroomed.groomerID = -1;
            monkey.setSitting(false);
            monkey.getNavigation().startMovingTo(beingGroomed, 1);
        }
    }
}
