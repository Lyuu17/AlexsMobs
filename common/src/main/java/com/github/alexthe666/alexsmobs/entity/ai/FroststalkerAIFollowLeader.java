package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntityFroststalker;
import com.mojang.datafixers.DataFixUtils;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;

import java.util.List;
import java.util.function.Predicate;

public class FroststalkerAIFollowLeader extends Goal {
    private static final int INTERVAL_TICKS = 200;
    private final EntityFroststalker mob;
    private int timeToRecalcPath;
    private int nextStartTick;

    public FroststalkerAIFollowLeader(EntityFroststalker froststalker) {
        this.mob = froststalker;
        this.nextStartTick = this.getInterval(froststalker);
    }

    protected int getInterval(EntityFroststalker froststalker) {
        return 100 + froststalker.getRandom().nextInt(200) % 40;
    }

    @Override
    public boolean canStart() {
        if (this.mob.hasFollowers()) {
            return false;
        } else if (this.mob.isFollower()) {
            return true;
        } else if (this.nextStartTick > 0) {
            --this.nextStartTick;
            return false;
        } else {
            this.nextStartTick = this.getInterval(this.mob);
            Predicate<EntityFroststalker> froststalkerPredicate = (p_25258_) -> {
                return p_25258_.canBeFollowed() || !p_25258_.isFollower();
            };
            float range = 60F;
            var playerList = this.mob.getWorld().getEntitiesByClass(PlayerEntity.class, this.mob.getBoundingBox().expand(range, range, range), EntityFroststalker.VALID_LEADER_PLAYERS);
            PlayerEntity closestPlayer = null;
            for(var player : playerList){
                if(closestPlayer == null || player.distanceTo(mob) < closestPlayer.distanceTo(mob)){
                    closestPlayer = player;
                }
            }
            if(closestPlayer == null){
                List<EntityFroststalker> list = this.mob.getWorld().getEntitiesByClass(EntityFroststalker.class, this.mob.getBoundingBox().expand(range, range, range), froststalkerPredicate);
                EntityFroststalker entityFroststalker = DataFixUtils.orElse(list.stream().filter(EntityFroststalker::canBeFollowed).findAny(), this.mob);
                entityFroststalker.addFollowers(list.stream().filter((p_25255_) -> !p_25255_.isFollower()));
            }else{
                this.mob.startFollowing(closestPlayer);
            }

            return this.mob.isFollower();
        }
    }

    @Override
    public boolean shouldContinue() {
        return this.mob.isFollower() && this.mob.inRangeOfLeader();
    }

    @Override
    public void start() {
        this.timeToRecalcPath = 0;
    }

    @Override
    public void stop() {
        this.mob.stopFollowing();
    }

    @Override
    public void tick() {
        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = 10;
            this.mob.pathToLeader();

        }
    }
}