package com.github.alexthe666.alexsmobs.entity.ai;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.tag.FluidTags;

import java.util.EnumSet;

public class AnimalAIWadeSwimming extends Goal {
    private final MobEntity entity;

    public AnimalAIWadeSwimming(MobEntity entity) {
        this.entity = entity;
        this.setControls(EnumSet.of(Control.JUMP));
        entity.getNavigation().setCanSwim(true);
    }

    @Override
    public boolean canStart() {
        return this.entity.isTouchingWater() && this.entity.getFluidHeight(FluidTags.WATER) > 1F || this.entity.isInLava();
    }

    @Override
    public void tick() {
        if (this.entity.getRandom().nextFloat() < 0.8F) {
            this.entity.getJumpControl().setActive();
        }
    }
}
