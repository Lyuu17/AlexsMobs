package com.github.alexthe666.alexsmobs.block;

import com.github.alexthe666.alexsmobs.block.entity.EndPirateShipWheelBlockEntity;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

public class EndPirateShipWheelBlock extends BlockWithEntity implements AMSpecialRenderBlock{

    public static final DirectionProperty FACING = HorizontalFacingBlock.FACING;
    private static final VoxelShape SOUTH_AABB = Block.createCuboidShape(-2, -2, 0, 18, 18, 3);
    private static final VoxelShape NORTH_AABB = Block.createCuboidShape(-2, -2, 13, 18, 18, 16);
    private static final VoxelShape EAST_AABB = Block.createCuboidShape(0, -2, -2, 3, 18, 18);
    private static final VoxelShape WEST_AABB = Block.createCuboidShape(13, -2, -2, 16, 18, 18);
    private static final VoxelShape UP_AABB = Block.createCuboidShape(-2, 0, -2, 18, 3, 18);
    private static final VoxelShape DOWN_AABB = Block.createCuboidShape(-2, 13, -2, 16, 16, 18);

    public EndPirateShipWheelBlock() {
        super(Settings.create()
                .mapColor(MapColor.TERRACOTTA_WHITE)
                .nonOpaque()
                .sounds(BlockSoundGroup.ANCIENT_DEBRIS)
                .strength(1F)
                .luminance((i) -> 3)
                .noCollision()
                .requiresTool());
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState state2, WorldAccess level, BlockPos pos, BlockPos p_52801_) {
        return !state.canPlaceAt(level, pos) ? Blocks.AIR.getDefaultState() : super.getStateForNeighborUpdate(state, direction, state2, level, pos, p_52801_);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState p_54561_, BlockView p_54562_, BlockPos p_54563_, ShapeContext p_54564_) {
        return switch (p_54561_.get(FACING)) {
            case NORTH -> NORTH_AABB;
            case SOUTH -> SOUTH_AABB;
            case EAST -> EAST_AABB;
            case WEST -> WEST_AABB;
            case UP -> UP_AABB;
            default -> DOWN_AABB;
        };
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        boolean remove = false;
        Direction dir = state.get(FACING).getOpposite();
        BlockPos offset = pos.offset(dir);
        return remove || world.getBlockState(offset).isSideSolidFullSquare(world, offset, dir.getOpposite());
    }

    @Override
    public ActionResult onUse(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockHitResult hit) {
        if(worldIn.getBlockEntity(pos) instanceof EndPirateShipWheelBlockEntity wheel){
            boolean clockwise = false;
            var offset = hit.getPos().subtract(pos.getX(), pos.getY(), pos.getZ());
            switch (state.get(FACING)) {
                case NORTH -> clockwise = offset.x <= 0.5F;
                case SOUTH -> clockwise = offset.x >= 0.5F;
                case EAST -> clockwise = offset.z <= 0.5F;
                case WEST -> clockwise = offset.z >= 0.5F;
            }
            wheel.rotate(clockwise);
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new EndPirateShipWheelBlockEntity(pos, state);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        return this.getDefaultState().with(FACING, context.getHorizontalPlayerFacing().getOpposite());
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
            if (blockEntity instanceof EndPirateShipWheelBlockEntity endPirateShipWheelBlockEntity) {
                endPirateShipWheelBlockEntity.tick(world1, pos, state1, endPirateShipWheelBlockEntity);
            }
        };
    }
}
