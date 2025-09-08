package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntityElephant;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.passive.MerchantEntity;

public class ElephantAIVillagerRide extends Goal {

    private final EntityElephant elephant;
    private MerchantEntity villager;
    private final double speed;

    public ElephantAIVillagerRide(EntityElephant dragon, double speed) {
        elephant = dragon;
        this.speed = speed;
    }

    @Override
    public boolean canStart() {
        if(elephant.getControllingVillager() != null){
           villager = elephant.getControllingVillager();
            return true;
        }
        return false;
    }

    @Override
    public void start() {
    }

    @Override
    public void tick() {
        if(this.villager.getNavigation().isFollowingPath()){
            this.elephant.getNavigation().startMovingAlong(this.villager.getNavigation().getCurrentPath(), 1.6D);
        }
    }
}
