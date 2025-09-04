package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntityLaviathan;
import com.github.alexthe666.alexsmobs.entity.IHerdPanic;
import com.google.common.base.Predicate;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.FuzzyTargeting;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

public class AnimalAIHerdPanic extends Goal {
    protected final PathAwareEntity creature;
    protected final double speed;
    protected final Predicate<? super PathAwareEntity> targetEntitySelector;
    protected double randPosX;
    protected double randPosY;
    protected double randPosZ;
    protected boolean running;

    public AnimalAIHerdPanic(PathAwareEntity creature, double speedIn) {
        this.creature = creature;
        this.speed = speedIn;
        this.setControls(EnumSet.of(Goal.Control.MOVE));
        this.targetEntitySelector = new Predicate<PathAwareEntity>() {
            @Override
            public boolean apply(@Nullable PathAwareEntity animal) {
                if (animal instanceof IHerdPanic && animal.getType() == creature.getType()) {
                    return ((IHerdPanic) animal).canPanic();
                }
                return false;
            }
        };
    }

    @Override
    public boolean canStart() {
        if (this.creature.getLastAttacker() == null || !this.creature.getLastAttacker().isAlive()) {
            return false;
        } else {
            if (this.creature.isOnFire() && !this.creature.isFireImmune()) {
                var blockpos = this.getRandPos(this.creature.getWorld(), this.creature, 5, 4);
                if (blockpos != null) {
                    this.randPosX = blockpos.getX();
                    this.randPosY = blockpos.getY();
                    this.randPosZ = blockpos.getZ();
                    return true;
                }
            }
            if (this.creature.getLastAttacker() != null && this.creature instanceof IHerdPanic && ((IHerdPanic) this.creature).canPanic()) {
                List<? extends PathAwareEntity> list = this.creature.getWorld().getEntitiesByClass(this.creature.getClass(), this.getTargetableArea(), this.targetEntitySelector);
                for (var creatureEntity : list) {
                    creatureEntity.setAttacker(this.creature.getLastAttacker());
                }
                return this.findRandomPositionFrom(this.creature.getLastAttacker());
            }
            return this.findRandomPosition();
        }
    }

    private boolean findRandomPositionFrom(LivingEntity revengeTarget) {
        Vec3d vector3d;
        if(this.creature instanceof EntityLaviathan){
            vector3d = NoPenaltyTargeting.findFrom(this.creature, 32, 16, revengeTarget.getPos());
        }else{
            vector3d = FuzzyTargeting.findFrom(this.creature, 16, 7, revengeTarget.getPos());
        }
        if (vector3d == null) {
            return false;
        } else {
            this.randPosX = vector3d.x;
            this.randPosY = vector3d.y;
            this.randPosZ = vector3d.z;
            return true;
        }
    }

    protected Box getTargetableArea() {
        var renderCenter = new Vec3d(this.creature.getX() + 0.5, this.creature.getY() + 0.5D, this.creature.getZ() + 0.5D);
        double searchRadius = 15;
        var aabb = new Box(-searchRadius, -searchRadius, -searchRadius, searchRadius, searchRadius, searchRadius);
        return aabb.offset(renderCenter);
    }

    protected boolean findRandomPosition() {
        var vector3d = FuzzyTargeting.find(this.creature, 5, 4);
        if (vector3d == null) {
            return false;
        } else {
            this.randPosX = vector3d.x;
            this.randPosY = vector3d.y;
            this.randPosZ = vector3d.z;
            return true;
        }
    }

    public boolean isRunning() {
        return this.running;
    }

    /**
     * Execute a one shot task or start executing a continuous task
     */
    @Override
    public void start() {
        if (this.creature instanceof IHerdPanic) {
            ((IHerdPanic) this.creature).onPanic();
        }
        this.creature.getNavigation().startMovingTo(this.randPosX, this.randPosY, this.randPosZ, this.speed);

        this.running = true;
    }

    /**
     * Reset the task's internal state. Called when this task is interrupted by another one
     */
    @Override
    public void stop() {
        this.running = false;
    }

    /**
     * Returns whether an in-progress EntityAIBase should continue executing
     */
    @Override
    public boolean shouldContinue() {
        return !this.creature.getNavigation().isIdle();
    }

    @Nullable
    protected BlockPos getRandPos(BlockView worldIn, Entity entityIn, int horizontalRange, int verticalRange) {
        var blockpos = entityIn.getBlockPos();
        int i = blockpos.getX();
        int j = blockpos.getY();
        int k = blockpos.getZ();
        float f = (float) (horizontalRange * horizontalRange * verticalRange * 2);
        BlockPos blockpos1 = null;
        var blockpos$mutable = new BlockPos.Mutable();

        for (int l = i - horizontalRange; l <= i + horizontalRange; ++l) {
            for (int i1 = j - verticalRange; i1 <= j + verticalRange; ++i1) {
                for (int j1 = k - horizontalRange; j1 <= k + horizontalRange; ++j1) {
                    blockpos$mutable.set(l, i1, j1);
                    if (worldIn.getFluidState(blockpos$mutable).isIn(FluidTags.WATER)) {
                        float f1 = (float) ((l - i) * (l - i) + (i1 - j) * (i1 - j) + (j1 - k) * (j1 - k));
                        if (f1 < f) {
                            f = f1;
                            blockpos1 = new BlockPos(blockpos$mutable);
                        }
                    }
                }
            }
        }

        return blockpos1;
    }
}
