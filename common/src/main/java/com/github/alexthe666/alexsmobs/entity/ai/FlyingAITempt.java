package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.ITargetsDroppedItems;
import net.minecraft.entity.ai.goal.TemptGoal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.recipe.Ingredient;

public class FlyingAITempt extends TemptGoal {

    public FlyingAITempt(PathAwareEntity mob, double speed, Ingredient ingredient, boolean skittish) {
        super(mob, speed, ingredient, skittish);
    }

    public void tick() {
        super.tick();
        if(mob instanceof ITargetsDroppedItems hasFlyingItemAI && mob.isOnGround()){
            hasFlyingItemAI.setFlying(false);
        }
    }
}
