package com.github.alexthe666.alexsmobs.block;

import com.github.alexthe666.alexsmobs.block.entity.EndPirateFlagBlockEntity;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

public class EndPirateFlagBlock extends BlockWithEntity {

    public static final DirectionProperty FACING = HorizontalFacingBlock.FACING;
    private static final VoxelShape AABB = Block.createCuboidShape(6, 0, 6, 10, 16, 10);

    public EndPirateFlagBlock() {
        super(Settings.create()
                .mapColor(MapColor.TERRACOTTA_WHITE)
                .nonOpaque()
                .sounds(BlockSoundGroup.WOOD)
                .strength(1F)
                .luminance((i) -> 15)
                .noCollision()
                .requiresTool());
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(FACING, Direction.NORTH));
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState state2, WorldAccess level, BlockPos pos, BlockPos p_52801_) {
        return !state.canPlaceAt(level, pos) ? Blocks.AIR.getDefaultState() : super.getStateForNeighborUpdate(state, direction, state2, level, pos, p_52801_);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState p_54561_, BlockView p_54562_, BlockPos p_54563_, ShapeContext p_54564_) {
        return AABB;
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        return sideCoversSmallSquare(world, pos.down(), Direction.UP) || sideCoversSmallSquare(world, pos.up(), Direction.DOWN);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new EndPirateFlagBlockEntity(pos, state);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        return this.getDefaultState()
                .with(FACING, context.getHorizontalPlayerFacing());
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rot) {
        return state.with(FACING, rot.rotate(state.get(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirrorIn) {
        return state.rotate(mirrorIn.getRotation(state.get(FACING)));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return (world1, pos, state1, blockEntity) -> {
            if (blockEntity instanceof EndPirateFlagBlockEntity endPirateFlagBlockEntity) {
                endPirateFlagBlockEntity.tick(world1, pos, state1, endPirateFlagBlockEntity);
            }
        };
    }

    public void randomDisplayTick(BlockState p_53094_, World p_53095_, BlockPos p_53096_, Random p_53097_) {
        if (p_53097_.nextInt(5) == 0) {
            final double d0 = (double)p_53096_.getX() + 0.55D - (double)(p_53097_.nextFloat() * 0.1F);
            final double d1 = (double)p_53096_.getY() + 0.55D - (double)(p_53097_.nextFloat() * 0.1F);
            final double d2 = (double)p_53096_.getZ() + 0.55D - (double)(p_53097_.nextFloat() * 0.1F);
            final double d3 = (double)(0.4F - (p_53097_.nextFloat() + p_53097_.nextFloat()) * 0.4F);
            p_53095_.addParticle(ParticleTypes.END_ROD, d0, d1 +  d3, d2, p_53097_.nextGaussian() * 0.005D, p_53097_.nextGaussian() * 0.005D, p_53097_.nextGaussian() * 0.005D);
        }

    }
}
