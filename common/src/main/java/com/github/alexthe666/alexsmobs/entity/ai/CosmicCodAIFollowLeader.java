package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntityCosmicCod;
import com.mojang.datafixers.DataFixUtils;
import net.minecraft.entity.ai.goal.Goal;

import java.util.function.Predicate;

public class CosmicCodAIFollowLeader extends Goal {
    private final EntityCosmicCod mob;
    private int timeToRecalcPath;
    private int nextStartTick;

    public CosmicCodAIFollowLeader(EntityCosmicCod cod) {
        this.mob = cod;
        this.nextStartTick = this.getInterval(cod);
    }

    protected int getInterval(EntityCosmicCod p_25252_) {
        return toGoalTicks(100 + p_25252_.getRandom().nextInt(100) % 20);
    }

    @Override
    public boolean canStart() {
        if (this.mob.isGroupLeader() || this.mob.isCircling()) {
            return false;
        } else if (this.mob.hasGroupLeader()) {
            return true;
        } else if (this.nextStartTick > 0) {
            --this.nextStartTick;
            return false;
        } else {
            this.nextStartTick = this.getInterval(this.mob);
            Predicate<EntityCosmicCod> predicate = (p_25258_) -> p_25258_.canGroupGrow() || !p_25258_.hasGroupLeader();
            var list = this.mob.getWorld().getEntitiesByClass(EntityCosmicCod.class, this.mob.getBoundingBox().expand(8.0D, 8.0D, 8.0D), predicate);
            var cc = DataFixUtils.orElse(list.stream().filter(EntityCosmicCod::canGroupGrow).findAny(), this.mob);
            cc.createFromStream(list.stream().filter((p_25255_) -> !p_25255_.hasGroupLeader()));
            return this.mob.hasGroupLeader();
        }
    }

    @Override
    public boolean shouldContinue() {
        return this.mob.hasGroupLeader() && this.mob.inRangeOfGroupLeader() && !this.mob.isCircling();
    }

    @Override
    public void start() {
        this.timeToRecalcPath = 0;
    }

    @Override
    public void stop() {
        this.mob.leaveGroup();
    }

    @Override
    public void tick() {
        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = this.getTickCount(10);
            this.mob.moveToGroupLeader();
        }
    }
}