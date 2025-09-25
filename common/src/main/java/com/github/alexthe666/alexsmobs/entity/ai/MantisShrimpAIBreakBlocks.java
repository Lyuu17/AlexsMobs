package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntityMantisShrimp;
import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import net.minecraft.block.BlockState;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.item.Item;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.WorldView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

public class MantisShrimpAIBreakBlocks extends Goal {

    private final EntityMantisShrimp mantisShrimp;
    private int idleAtFlowerTime = 0;
    private int timeoutCounter = 0;
    private int searchCooldown = 0;
    private boolean isAboveDestinationBear;
    private BlockPos destinationBlock;
    private final BlockSorter targetSorter;

    public MantisShrimpAIBreakBlocks(EntityMantisShrimp mantisShrimp) {
        super();
        this.mantisShrimp = mantisShrimp;
        this.targetSorter = new BlockSorter(mantisShrimp);
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    public boolean canStart() {
        if (!mantisShrimp.isBaby() && (mantisShrimp.getTarget() == null || !mantisShrimp.getTarget().isAlive()) && mantisShrimp.getCommand() == 3 && !mantisShrimp.getMainHandStack().isEmpty()) {
            if(searchCooldown <= 0){
                resetTarget();
                searchCooldown = 100 + mantisShrimp.getRandom().nextInt(200);
                return destinationBlock != null;
            }else{
                searchCooldown--;
            }
        }
        return false;
    }

    @Override
    public boolean shouldContinue() {
        return destinationBlock != null && timeoutCounter < 1200 && (mantisShrimp.getTarget() == null || !mantisShrimp.getTarget().isAlive()) && mantisShrimp.getCommand() == 3 && !mantisShrimp.getMainHandStack().isEmpty();
    }

    @Override
    public void stop() {
        searchCooldown = 50;
        timeoutCounter = 0;
        destinationBlock = null;
    }

    public double getTargetDistanceSq() {
        return 2.3D;
    }

    @Override
    public void tick() {
        BlockPos blockpos = destinationBlock;
        float yDist = (float) Math.abs(blockpos.getY() - mantisShrimp.getY() - mantisShrimp.getHeight()/2);
        this.mantisShrimp.getNavigation().startMovingTo((double) ((float) blockpos.getX()) + 0.5D, blockpos.getY() + 0.5D, (double) ((float) blockpos.getZ()) + 0.5D, 1);
        if (!isWithinXZDist(blockpos, mantisShrimp.getPos(), this.getTargetDistanceSq()) || yDist > 2F) {
            this.isAboveDestinationBear = false;
            ++this.timeoutCounter;
        } else {
            this.isAboveDestinationBear = true;
            --this.timeoutCounter;
        }
        if(timeoutCounter > 2400){
            stop();
        }
        if (this.getIsAboveDestination()) {
            mantisShrimp.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, new Vec3d(destinationBlock.getX() + 0.5D, destinationBlock.getY(), destinationBlock.getZ() + 0.5));
            if (this.idleAtFlowerTime >= 2) {
                idleAtFlowerTime = 0;
                this.breakBlock();
                this.stop();
            } else {
                mantisShrimp.punch();
                ++this.idleAtFlowerTime;
            }
        }
    }

    private void resetTarget() {
        List<BlockPos> allBlocks = new ArrayList<>();
        int radius = 16;
        for (var pos : BlockPos.stream(this.mantisShrimp.getBlockPos().add(-radius, -radius, -radius), this.mantisShrimp.getBlockPos().add(radius, radius, radius)).map(BlockPos::toImmutable).toList()) {
            if (!mantisShrimp.getWorld().isAir(pos) && shouldMoveTo(mantisShrimp.getWorld(), pos)) {
                if(!mantisShrimp.isTouchingWater() || isBlockTouchingWater(pos)){
                    allBlocks.add(pos);
                }
            }
        }
        if (!allBlocks.isEmpty()) {
            allBlocks.sort(this.targetSorter);
            for(BlockPos pos : allBlocks){
                if(hasLineOfSightBlock(pos)){
                    this.destinationBlock = pos;
                    return;
                }
            }
        }
        destinationBlock = null;
    }

    private boolean isBlockTouchingWater(BlockPos pos) {
        for(var dir : Direction.values()){
            if(mantisShrimp.getWorld().getFluidState(pos.offset(dir)).isIn(FluidTags.WATER)){
                return true;
            }
        }
        return false;
    }

    private boolean isWithinXZDist(BlockPos blockpos, Vec3d positionVec, double distance) {
        return blockpos.getSquaredDistance(AMBlockPos.fromCoords(positionVec.x, blockpos.getY(), positionVec.z)) < distance * distance;
    }

    protected boolean getIsAboveDestination() {
        return this.isAboveDestinationBear;
    }

    private void breakBlock() {
        if (shouldMoveTo(mantisShrimp.getWorld(), destinationBlock)) {
            BlockState state = mantisShrimp.getWorld().getBlockState(destinationBlock);
            if(!mantisShrimp.getWorld().isAir(destinationBlock) /*FIXME forge && net.minecraftforge.common.ForgeHooks.canEntityDestroy(mantisShrimp.getWorld(), destinationBlock, mantisShrimp)*/ && state.getHardness(mantisShrimp.getWorld(), destinationBlock) >= 0){
                mantisShrimp.getWorld().breakBlock(destinationBlock, true);
            }
        }
    }

    private boolean hasLineOfSightBlock(BlockPos destinationBlock) {
        var Vector3d = new Vec3d(mantisShrimp.getX(), mantisShrimp.getEyeY(), mantisShrimp.getZ());
        var blockVec = Vec3d.ofCenter(destinationBlock);
        var result = mantisShrimp.getWorld().raycast(new RaycastContext(Vector3d, blockVec, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mantisShrimp));
        return result.getBlockPos().equals(destinationBlock);
    }

    protected boolean shouldMoveTo(WorldView worldIn, BlockPos pos) {
        Item blockItem = worldIn.getBlockState(pos).getBlock().asItem();
        return mantisShrimp.getMainHandStack().getItem() == blockItem;
    }

    public record BlockSorter(Entity entity) implements Comparator<BlockPos> {
        @Override
        public int compare(BlockPos pos1, BlockPos pos2) {
            final double distance1 = this.getDistance(pos1);
            final double distance2 = this.getDistance(pos2);
            return Double.compare(distance1, distance2);
        }

        private double getDistance(BlockPos pos) {
            final double deltaX = this.entity.getX() - (pos.getX() + 0.5);
            final double deltaY = this.entity.getY() + this.entity.getStandingEyeHeight() - (pos.getY() + 0.5);
            final double deltaZ = this.entity.getZ() - (pos.getZ() + 0.5);
            return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
        }
    }
}
