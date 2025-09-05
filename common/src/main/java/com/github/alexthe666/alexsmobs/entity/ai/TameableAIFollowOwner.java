package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.IFollower;
import net.minecraft.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.entity.passive.TameableEntity;

public class TameableAIFollowOwner extends FollowOwnerGoal {

    private final IFollower follower;
    private final TameableEntity tameable;

    public TameableAIFollowOwner(TameableEntity tameable, double speed, float minDist, float maxDist, boolean teleportToLeaves) {
        super(tameable, speed, minDist, maxDist, teleportToLeaves);
        this.follower = (IFollower)tameable;
        this.tameable = tameable;
    }

    @Override
    public boolean canStart(){
        return super.canStart() && follower.shouldFollow() && !isInCombat();
    }

    @Override
    public boolean shouldContinue(){
        return super.shouldContinue() && follower.shouldFollow() && !isInCombat();
    }

    private boolean isInCombat() {
        var owner = tameable.getOwner();
        if(owner != null){
            return tameable.distanceTo(owner) < 30 && tameable.getTarget() != null && tameable.getTarget().isAlive();
        }
        return false;
    }
}
