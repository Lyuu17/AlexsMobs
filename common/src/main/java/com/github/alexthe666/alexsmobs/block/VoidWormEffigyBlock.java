package com.github.alexthe666.alexsmobs.block;

import net.minecraft.block.*;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

public class VoidWormEffigyBlock extends Block {
    public static final DirectionProperty FACING = FacingBlock.FACING;
    private static final VoxelShape UP_SHAPE = VoxelShapes.union(Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 7.0D, 16.0D),
            Block.createCuboidShape(4.0D, 6.0D, 4.0D, 12.0D, 16.0D, 12.0D));
    private static final VoxelShape DOWN_SHAPE = VoxelShapes.union(Block.createCuboidShape(0.0D, 9.0D, 0.0D, 16.0D, 16.0D, 16.0D),
            Block.createCuboidShape(4.0D, 0.0D, 4.0D, 12.0D, 10.0D, 12.0D));
    private static final VoxelShape SOUTH_SHAPE = VoxelShapes.union(Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 7.0D),
            Block.createCuboidShape(4.0D, 4.0D, 6.0D, 12.0D, 12.0D, 16.0D));
    private static final VoxelShape NORTH_SHAPE = VoxelShapes.union(Block.createCuboidShape(0.0D, 0.0D, 9.0D, 16.0D, 16.0D, 16.0D),
            Block.createCuboidShape(4.0D, 4.0D, 0.0D, 12.0D, 12.0D, 10.0D));
    private static final VoxelShape EAST_SHAPE = VoxelShapes.union(Block.createCuboidShape(0.0D, 0.0D, 0.0D, 7.0D, 16.0D, 16.0D),
            Block.createCuboidShape(6.0D, 4.0D, 4.0D, 16.0D, 12.0D, 12.0D));
    private static final VoxelShape WEST_SHAPE = VoxelShapes.union(Block.createCuboidShape(9.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D),
            Block.createCuboidShape(0.0D, 4.0D, 4.0D, 10.0D, 12.0D, 12.0D));

    public VoidWormEffigyBlock() {
        super(Settings.create()
                .mapColor(MapColor.PINK)
                .requiresTool()
                .strength(1.5F));
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        return this.getDefaultState().with(FACING, context.getSide());
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

    @Override
    public VoxelShape getOutlineShape(BlockState p_54561_, BlockView p_54562_, BlockPos p_54563_, ShapeContext p_54564_) {
        return switch (p_54561_.get(FACING)) {
            case NORTH -> NORTH_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case EAST -> EAST_SHAPE;
            case WEST -> WEST_SHAPE;
            case UP -> UP_SHAPE;
            default -> DOWN_SHAPE;
        };
    }
}
