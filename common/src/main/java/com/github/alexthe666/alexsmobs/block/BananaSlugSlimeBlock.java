package com.github.alexthe666.alexsmobs.block;

import com.github.alexthe666.alexsmobs.registry.AMBlockRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.google.common.collect.Lists;
import com.iafenvoy.uranus.util.function.tuple.Tuple2;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import java.util.Queue;

public class BananaSlugSlimeBlock extends TransparentBlock {

    protected static final VoxelShape SHAPE = Block.createCuboidShape(1.0D, 1.0D, 1.0D, 15.0D, 15.0D, 15.0D);
    private static final int MAXIMUM_BLOCKS_DRAINED = 64;
    public static final int MAX_FLUID_SPREAD = 6;

    public BananaSlugSlimeBlock() {
        super(AbstractBlock.Settings.create()
                .mapColor(MapColor.YELLOW)
                .velocityMultiplier(0.4F)
                .jumpVelocityMultiplier(0.5F)
                .slipperiness(0.8F)
                .sounds(BlockSoundGroup.SLIME)
                .nonOpaque());
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.empty();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState p_54015_, BlockView p_54016_, BlockPos p_54017_, ShapeContext p_54018_) {
        return SHAPE;
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        entity.setVelocity(entity.getVelocity().multiply(0.8));
        super.onEntityCollision(state, world, pos, entity);
    }

    @Override
    public boolean isTransparent(BlockState p_48740_, BlockView p_48741_, BlockPos p_48742_) {
        return true;
    }

    @Override
    public void onBlockAdded(BlockState p_56811_, World p_56812_, BlockPos p_56813_, BlockState p_56814_, boolean p_56815_) {
        if (!p_56814_.isOf(p_56811_.getBlock())) {
            this.tryAbsorbWater(p_56812_, p_56813_);
        }
    }

    @Override
    public void neighborUpdate(BlockState p_56801_, World p_56802_, BlockPos p_56803_, Block p_56804_, BlockPos p_56805_, boolean p_56806_) {
        this.tryAbsorbWater(p_56802_, p_56803_);
        super.neighborUpdate(p_56801_, p_56802_, p_56803_, p_56804_, p_56805_, p_56806_);
    }

    protected void tryAbsorbWater(World level, BlockPos pos) {
        if (this.removeWaterBreadthFirstSearch(level, pos)) {
            level.playSound(null, pos, AMSoundRegistry.BANANA_SLUG_SLIME_EXPAND.get(), SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
    }

    private boolean removeWaterBreadthFirstSearch(World level, BlockPos pos) {
        Queue<Tuple2<BlockPos, Integer>> queue = Lists.newLinkedList();
        queue.add(new Tuple2<>(pos, 0));
        int i = 0;
        int fullBlocks = 0;
        FluidState lastFluidState = null;
        while (!queue.isEmpty()) {
            var tuple = queue.poll();
            BlockPos blockpos = tuple.getT1();
            BlockState state = level.getBlockState(blockpos);
            int j = tuple.getT2();
            if (!state.getFluidState().isEmpty()) {
                fullBlocks++;
                if (state.getBlock() instanceof FluidDrainable) {
                    ((FluidDrainable) state.getBlock()).tryDrainFluid(level, blockpos, state);
                    if(level.getBlockState(blockpos).isAir()){
                        level.setBlockState(blockpos, AMBlockRegistry.CRYSTALIZED_BANANA_SLUG_MUCUS.get().getDefaultState());
                    }
                }else{
                    level.setBlockState(blockpos, AMBlockRegistry.CRYSTALIZED_BANANA_SLUG_MUCUS.get().getDefaultState());
                }
            }
            for (var direction : Direction.values()) {
                BlockPos blockpos1 = blockpos.offset(direction);
                BlockState blockstate = level.getBlockState(blockpos1);
                FluidState fluidstate = level.getFluidState(blockpos1);
                if (lastFluidState != null && !fluidstate.isEmpty() && lastFluidState.getFluid() != fluidstate.getFluid()) {
                    continue;
                }
                if (blockstate.getBlock() instanceof Waterloggable) {
                    if (!fluidstate.isEmpty()) {
                        lastFluidState = fluidstate;
                    }
                    ++i;
                    fullBlocks++;
                    level.setBlockState(blockpos1, blockstate.with(Properties.WATERLOGGED, false));
                    if (j < MAX_FLUID_SPREAD) {
                        queue.add(new Tuple2<>(blockpos1, j + 1));
                    }
                } else if (blockstate.getBlock() instanceof FluidDrainable) {
                    if (!fluidstate.isEmpty()) {
                        lastFluidState = fluidstate;
                    }
                    ++i;
                    fullBlocks++;
                    ((FluidDrainable) blockstate.getBlock()).tryDrainFluid(level, blockpos1, blockstate);
                    if(level.getBlockState(blockpos).isAir()){
                        level.setBlockState(blockpos, AMBlockRegistry.CRYSTALIZED_BANANA_SLUG_MUCUS.get().getDefaultState());
                    }
                    if (j < MAX_FLUID_SPREAD) {
                        queue.add(new Tuple2<>(blockpos1, j + 1));
                    }
                } else if (blockstate.getBlock() instanceof FluidBlock) {
                    if (!fluidstate.isEmpty()) {
                        lastFluidState = fluidstate;
                    }
                    level.setBlockState(blockpos1, AMBlockRegistry.CRYSTALIZED_BANANA_SLUG_MUCUS.get().getDefaultState());
                    ++i;
                    if (blockstate.getFluidState().isStill()) {
                        fullBlocks++;
                    }
                    if (j < MAX_FLUID_SPREAD) {
                        queue.add(new Tuple2<>(blockpos1, j + 1));
                    }
                }
            }
            if (i > MAXIMUM_BLOCKS_DRAINED) {
                break;
            }
        }
        return fullBlocks > 0;
    }
}
