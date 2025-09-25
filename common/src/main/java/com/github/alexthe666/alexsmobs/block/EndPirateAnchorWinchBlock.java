package com.github.alexthe666.alexsmobs.block;

import com.github.alexthe666.alexsmobs.block.entity.EndPirateAnchorWinchBlockEntity;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class EndPirateAnchorWinchBlock extends BlockWithEntity implements AMSpecialRenderBlock{

    public static final BooleanProperty EASTORWEST = BooleanProperty.of("eastorwest");
    public static final BooleanProperty POWERED = Properties.POWERED;
    protected static final VoxelShape FULL_AABB_EW = Block.createCuboidShape(3.0D, 3.0D, 0.0D, 13.0D, 13.0D, 16.0D);
    protected static final VoxelShape FULL_AABB_NS = Block.createCuboidShape(0.0D, 3.0D, 3.0D, 16.0D, 13.0D, 13.0D);

    protected EndPirateAnchorWinchBlock() {
        super(Settings.create()
                .mapColor(MapColor.BLACK)
                .slipperiness(0.97F)
                .strength(10.0F)
                .luminance((i) -> 6)
                .sounds(BlockSoundGroup.STONE)
                .nonOpaque());
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(EASTORWEST, Boolean.FALSE)
                .with(POWERED, Boolean.FALSE));
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(EASTORWEST, POWERED);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView getter, BlockPos pos, ShapeContext context) {
        return state.get(EASTORWEST) ? FULL_AABB_EW : FULL_AABB_NS;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        boolean axis = context.getSide().getAxis() == Direction.Axis.Y ? context.getHorizontalPlayerFacing().getAxis() == Direction.Axis.X : context.getSide().getAxis() != Direction.Axis.X;
        return getDefaultState().with(EASTORWEST, axis);
    }

    @Override
    public void onPlaced(World level, BlockPos pos, BlockState state, LivingEntity player, ItemStack stack) {
        if(level.getBlockEntity(pos) instanceof EndPirateAnchorWinchBlockEntity winch){
            winch.recalculateChains();
        }
    }

    @Override
    public void neighborUpdate(BlockState state, World level, BlockPos pos, Block block, BlockPos p_52780_, boolean p_52781_) {
        boolean flag = level.isReceivingRedstonePower(pos);
        if(level.getBlockEntity(pos) instanceof EndPirateAnchorWinchBlockEntity winch){
            if(flag != state.get(POWERED)){
                level.setBlockState(pos, state.with(POWERED, flag), 3);
            }
        }
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new EndPirateAnchorWinchBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return (world1, pos, state1, blockEntity) -> {
            if (blockEntity instanceof EndPirateAnchorWinchBlockEntity endPirateAnchorWinchBlockEntity) {
                endPirateAnchorWinchBlockEntity.tick(world1, pos, state1, endPirateAnchorWinchBlockEntity);
            }
        };
    }
}
