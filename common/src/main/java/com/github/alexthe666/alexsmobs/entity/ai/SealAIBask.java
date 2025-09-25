package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntitySeal;
import net.minecraft.entity.ai.goal.Goal;

import java.util.EnumSet;

public class SealAIBask extends Goal {
    private final EntitySeal seal;

    public SealAIBask(EntitySeal seal) {
        this.seal = seal;
        this.setControls(EnumSet.of(Control.JUMP, Control.MOVE));
    }

    @Override
    public boolean shouldContinue() {
        return this.seal.isBasking() && !this.seal.isInsideWaterOrBubbleColumn();
    }

    @Override
    public boolean canStart() {
        if (this.seal.isInsideWaterOrBubbleColumn()) {
            return false;
        } else {
            return seal.getAttacker() == null && seal.getTarget() == null && seal.isBasking();
        }
    }

    @Override
    public void tick() {
        this.seal.getNavigation().stop();
    }

    @Override
    public void stop() {
        this.seal.setBasking(false);
    }
}
