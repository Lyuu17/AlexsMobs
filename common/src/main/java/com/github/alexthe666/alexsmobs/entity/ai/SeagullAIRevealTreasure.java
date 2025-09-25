package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntitySeagull;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;

public class SeagullAIRevealTreasure extends Goal {

    private final EntitySeagull seagull;
    private BlockPos sitPos;

    public SeagullAIRevealTreasure(EntitySeagull entitySeagull) {
        this.seagull = entitySeagull;
        this.setControls(EnumSet.of(Control.MOVE, Control.TARGET));
    }

    @Override
    public boolean canStart() {
        return seagull.getTreasurePos() != null && seagull.treasureSitTime > 0;
    }

    @Override
    public void start(){
        seagull.aiItemFlag = true;
        sitPos = seagull.getSeagullGround(seagull.getTreasurePos());
    }

    @Override
    public void stop(){
        sitPos = null;
        seagull.setSitting(false);
        seagull.aiItemFlag = false;
    }

    @Override
    public void tick(){
        if(sitPos != null){
            if(seagull.squaredDistanceTo(new Vec3d(sitPos.getX() + 0.5F, seagull.getY(), sitPos.getZ() + 0.5F)) > 2.5F){
                seagull.getMoveControl().moveTo(sitPos.getX() + 0.5F, sitPos.getY() + 2, sitPos.getZ() + 0.5F, 1F);
                if(!seagull.isOnGround()){
                    seagull.setFlying(true);
                }
            }else{
                Vec3d vec = Vec3d.ofCenter(sitPos, 1.0F);
                if(vec.subtract(seagull.getPos()).length() > 0.04F){
                    seagull.setVelocity(vec.subtract(seagull.getPos()).multiply(0.2F));
                }
                seagull.eatItem();
                seagull.treasureSitTime = Math.min(seagull.treasureSitTime, 100);
                seagull.setFlying(false);
                seagull.setSitting(true);
            }
        }
    }

}
