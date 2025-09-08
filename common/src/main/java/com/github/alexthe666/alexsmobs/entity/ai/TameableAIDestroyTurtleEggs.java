package com.github.alexthe666.alexsmobs.entity.ai;

import net.minecraft.block.Blocks;
import net.minecraft.entity.ai.goal.StepAndDestroyBlockGoal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

public class TameableAIDestroyTurtleEggs extends StepAndDestroyBlockGoal {

    public TameableAIDestroyTurtleEggs(TameableEntity creatureIn, double speed, int yMax) {
        super(Blocks.TURTLE_EGG, creatureIn, speed, yMax);
        cooldown = 800;
    }

    @Override
    public boolean canStart() {
        return !((TameableEntity)mob).isTamed() && super.canStart();
    }

    @Override
    public boolean shouldContinue() {
        return !((TameableEntity)mob).isTamed() && super.shouldContinue();
    }

    @Override
    public void tickStepping(WorldAccess worldIn, BlockPos pos) {
        worldIn.playSound(null, pos, SoundEvents.ENTITY_ZOMBIE_DESTROY_EGG, SoundCategory.HOSTILE, 0.5F, 0.9F + this.mob.getRandom().nextFloat() * 0.2F);
    }

    @Override
    protected int getInterval(PathAwareEntity mob) {
        return toGoalTicks(800 + mob.getRandom().nextInt(800));
    }

    @Override
    public void onDestroyBlock(World worldIn, BlockPos pos) {
        worldIn.playSound(null, pos, SoundEvents.ENTITY_TURTLE_EGG_BREAK, SoundCategory.BLOCKS, 0.7F, 0.9F + worldIn.random.nextFloat() * 0.2F);
    }

    @Override
    public double getDesiredDistanceToTarget() {
        return 1.14D;
    }
}
