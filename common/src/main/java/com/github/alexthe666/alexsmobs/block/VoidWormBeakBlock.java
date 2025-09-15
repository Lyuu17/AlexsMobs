package com.github.alexthe666.alexsmobs.block;

import com.github.alexthe666.alexsmobs.block.entity.VoidWormBeakBlockEntity;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class VoidWormBeakBlock extends BlockWithEntity {

    public static final DirectionProperty FACING = FacingBlock.FACING;
    public static final BooleanProperty POWERED = Properties.POWERED;
    private static final VoxelShape AABB = Block.createCuboidShape(0, 4, 0, 16, 12, 16);
    private static final VoxelShape AABB_VERTICAL = Block.createCuboidShape(0, 0, 4, 16, 16, 12);

    public VoidWormBeakBlock() {
        super(Settings.create()
                .mapColor(MapColor.PURPLE)
                .nonOpaque()
                .sounds(BlockSoundGroup.ANCIENT_DEBRIS)
                .strength(1F)
                .noCollision()
                .requiresTool());
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(POWERED, false));
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView worldIn, BlockPos pos, ShapeContext context) {
        return state.get(FACING).getAxis() == Direction.Axis.Y ? AABB_VERTICAL : AABB;
    }

    @Override
    public void neighborUpdate(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
        if(!worldIn.isClient){
            this.updateState(state, worldIn, pos, blockIn);
        }
    }

    @Override
    public void randomTick(BlockState state, ServerWorld worldIn, BlockPos pos, Random random) {
        if(!worldIn.isClient){
            this.updateState(state, worldIn, pos, state.getBlock());
        }
    }

    public void updateState(BlockState state, World worldIn, BlockPos pos, Block blockIn) {
        boolean flag = state.get(POWERED);
        boolean flag1 = worldIn.isReceivingRedstonePower(pos);

        if (flag1 != flag) {
            worldIn.setBlockState(pos, state.with(POWERED, flag1), 3);
            worldIn.updateNeighborsAlways(pos.down(), this);
        }
    }


    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new VoidWormBeakBlockEntity(pos, state);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        return this.getDefaultState().with(FACING, context.getSide()).with(POWERED, context.getWorld().isReceivingRedstonePower(context.getBlockPos()));
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
        builder.add(FACING, POWERED);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return (world1, pos, state1, blockEntity) -> {
            if (blockEntity instanceof VoidWormBeakBlockEntity voidWormBeakBlockEntity) {
                voidWormBeakBlockEntity.tick(world1, pos, state1, voidWormBeakBlockEntity);
            }
        };
    }
}
