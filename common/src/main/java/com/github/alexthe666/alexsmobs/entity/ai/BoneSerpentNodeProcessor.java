package com.github.alexthe666.alexsmobs.entity.ai;

import net.minecraft.entity.ai.pathing.*;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.BlockView;
import org.jetbrains.annotations.Nullable;

public class BoneSerpentNodeProcessor extends PathNodeMaker {

    public BoneSerpentNodeProcessor() {
    }

    @Override
    public PathNode getStart() {
        return super.getNode(MathHelper.floor(this.entity.getBoundingBox().minX), MathHelper.floor(this.entity.getBoundingBox().minY + 0.5D), MathHelper.floor(this.entity.getBoundingBox().minZ));
    }

    @Override
    public TargetPathNode getNode(double x, double y, double z) {
        return new TargetPathNode(super.getNode(MathHelper.floor(x - (double)(this.entity.getWidth() / 2.0F)), MathHelper.floor(y + 0.5D), MathHelper.floor(z - (double)(this.entity.getWidth() / 2.0F))));
    }

    @Override
    public int getSuccessors(PathNode[] successors, PathNode node) {
        int i = 0;

        for(var direction : Direction.values()) {
            var pathpoint = this.getWaterNode(node.x + direction.getOffsetX(), node.y + direction.getOffsetY(), node.z + direction.getOffsetZ());
            if (pathpoint != null && !pathpoint.visited) {
                successors[i++] = pathpoint;
            }
        }

        return i;
    }

    @Override
    public PathNodeType getNodeType(BlockView blockaccessIn, int x, int y, int z, MobEntity entitylivingIn) {
        return this.getDefaultNodeType(blockaccessIn, x, y, z);
    }

    @Override
    public PathNodeType getDefaultNodeType(BlockView blockaccessIn, int x, int y, int z) {
        var blockpos = new BlockPos(x, y, z);
        var fluidstate = blockaccessIn.getFluidState(blockpos);
        var blockstate = blockaccessIn.getBlockState(blockpos);
        if (fluidstate.isEmpty() && blockstate.canPathfindThrough(blockaccessIn, blockpos.down(), NavigationType.WATER) && blockstate.isAir()) {
            return PathNodeType.BREACH;
        } else {
            return fluidstate.isIn(FluidTags.LAVA) || fluidstate.isIn(FluidTags.WATER) && blockstate.canPathfindThrough(blockaccessIn, blockpos, NavigationType.WATER) ? PathNodeType.WATER : PathNodeType.BLOCKED;
        }
    }

    @Nullable
    private PathNode getWaterNode(int p_186328_1_, int p_186328_2_, int p_186328_3_) {
        var pathnodetype = this.isFree(p_186328_1_, p_186328_2_, p_186328_3_);
        return pathnodetype != PathNodeType.BREACH && pathnodetype != PathNodeType.WATER && pathnodetype != PathNodeType.LAVA ? null : this.getNode(p_186328_1_, p_186328_2_, p_186328_3_);
    }

    /**
     * Returns a mapped point or creates and adds one
     */
    @Nullable
    @Override
    protected PathNode getNode(int x, int y, int z) {
        PathNode pathpoint = null;
        var pathnodetype = this.getDefaultNodeType(this.entity.getWorld(), x, y, z);
        float f = this.entity.getPathfindingPenalty(pathnodetype);
        if (f >= 0.0F) {
            pathpoint = super.getNode(x, y, z);
            pathpoint.type = pathnodetype;
            pathpoint.penalty = Math.max(pathpoint.penalty, f);
            if (this.cachedWorld.getFluidState(new BlockPos(x, y, z)).isEmpty()) {
                pathpoint.penalty += 8.0F;
            }
        }

        return pathpoint;
    }

    private PathNodeType isFree(int p_186327_1_, int p_186327_2_, int p_186327_3_) {
        var blockpos$mutable = new BlockPos.Mutable();

        for(int i = p_186327_1_; i < p_186327_1_ + this.entityBlockXSize; ++i) {
            for(int j = p_186327_2_; j < p_186327_2_ + this.entityBlockYSize; ++j) {
                for(int k = p_186327_3_; k < p_186327_3_ + this.entityBlockZSize; ++k) {
                    var fluidstate = this.cachedWorld.getFluidState(blockpos$mutable.set(i, j, k));
                    var blockstate = this.cachedWorld.getBlockState(blockpos$mutable.set(i, j, k));
                    if (fluidstate.isEmpty() && blockstate.canPathfindThrough(this.cachedWorld, blockpos$mutable.down(), NavigationType.WATER) && blockstate.isAir()) {
                        return PathNodeType.BREACH;
                    }

                    if (!fluidstate.isIn(FluidTags.WATER) && !fluidstate.isIn(FluidTags.LAVA)) {
                        return PathNodeType.BLOCKED;
                    }
                }
            }
        }

        var blockstate1 = this.cachedWorld.getBlockState(blockpos$mutable);
        return blockstate1.getFluidState().isIn(FluidTags.LAVA) || blockstate1.canPathfindThrough(this.cachedWorld, blockpos$mutable, NavigationType.WATER) ? PathNodeType.WATER : PathNodeType.BLOCKED;
    }
}
