package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntityMungus;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

import java.util.EnumSet;

public class MungusAITemptMushroom extends Goal {

    private static final TargetPredicate TEMP_TARGETING = TargetPredicate.createNonAttackable().setBaseMaxDistance(10.0D).ignoreVisibility();
    private final TargetPredicate targetingConditions;
    protected final EntityMungus mob;
    private final double speedModifier;
    private double px;
    private double py;
    private double pz;
    private int calmDown;
    private double pRotX;
    private double pRotY;
    protected PlayerEntity player;

    public MungusAITemptMushroom(EntityMungus p_25939_, double p_25940_) {
        this.mob = p_25939_;
        this.speedModifier = p_25940_;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
        this.targetingConditions = TEMP_TARGETING.copy();
    }

    @Override
    public boolean canStart() {
        if (this.calmDown > 0) {
            --this.calmDown;
            return false;
        } else {
            this.player = this.mob.getWorld().getClosestPlayer(this.targetingConditions, this.mob);
            if(this.player != null){
                return shouldFollow(this.player.getMainHandStack()) || shouldFollow(this.player.getOffHandStack());
            }
        }
        return false;
    }

    @Override
    public boolean shouldContinue() {
        return this.canStart();
    }

    @Override
    public void start() {
        this.px = this.player.getX();
        this.py = this.player.getY();
        this.pz = this.player.getZ();
    }

    @Override
    public void stop() {
        this.player = null;
        this.mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        this.mob.getLookControl().lookAt(this.player, (float)(this.mob.getMaxHeadRotation() + 20), (float)this.mob.getMaxLookPitchChange());
        if (this.mob.squaredDistanceTo(this.player) < 6.25D) {
            this.mob.getNavigation().stop();
        } else {
            this.mob.getNavigation().startMovingTo(this.player, this.speedModifier);
        }

    }

    protected boolean shouldFollow(ItemStack stack) {
        return mob.shouldFollowMushroom(stack) || stack.getItem() == AMItemRegistry.MUNGAL_SPORES.get();
    }
}
