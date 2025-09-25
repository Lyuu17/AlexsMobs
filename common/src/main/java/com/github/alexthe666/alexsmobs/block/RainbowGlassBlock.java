package com.github.alexthe666.alexsmobs.block;

import net.minecraft.block.AbstractGlassBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.MapColor;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

public class RainbowGlassBlock extends AbstractGlassBlock {

    public static final BooleanProperty UP = BooleanProperty.of("up");
    public static final BooleanProperty DOWN = BooleanProperty.of("down");
    public static final BooleanProperty EAST = BooleanProperty.of("east");
    public static final BooleanProperty WEST = BooleanProperty.of("west");
    public static final BooleanProperty NORTH = BooleanProperty.of("north");
    public static final BooleanProperty SOUTH = BooleanProperty.of("south");

    public RainbowGlassBlock() {
        super(Settings.create()
                .mapColor(MapColor.PURPLE)
                .slipperiness(0.97F)
                .strength(0.2F)
                .luminance((i) -> 11)
                .sounds(BlockSoundGroup.GLASS)
                .nonOpaque()
                .allowsSpawning(RainbowGlassBlock::noOption)
                .solidBlock(RainbowGlassBlock::noOption)
                .suffocates(RainbowGlassBlock::noOption)
                .blockVision(RainbowGlassBlock::noOption)
                .emissiveLighting(RainbowGlassBlock::yes));
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(UP, Boolean.FALSE)
                .with(DOWN, Boolean.FALSE)
                .with(EAST, Boolean.FALSE)
                .with(WEST, Boolean.FALSE)
                .with(NORTH, Boolean.FALSE)
                .with(SOUTH, Boolean.FALSE));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> p_58032_) {
        p_58032_.add(UP, DOWN, NORTH, SOUTH, EAST, WEST);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        WorldView worldView = context.getWorld();
        BlockPos blockpos = context.getBlockPos();
        BlockPos n = blockpos.north();
        BlockPos e = blockpos.east();
        BlockPos s = blockpos.south();
        BlockPos w = blockpos.west();
        BlockPos u = blockpos.up();
        BlockPos d = blockpos.down();
        BlockState northState = worldView.getBlockState(n);
        BlockState eastState = worldView.getBlockState(e);
        BlockState southState = worldView.getBlockState(s);
        BlockState westState = worldView.getBlockState(w);
        BlockState upState = worldView.getBlockState(u);
        BlockState downState = worldView.getBlockState(d);
        return getDefaultState()
                .with(NORTH, northState.isOf(this))
                .with(NORTH, northState.isOf(this))
                .with(EAST, eastState.isOf(this))
                .with(SOUTH, southState.isOf(this))
                .with(WEST, westState.isOf(this))
                .with(UP, upState.isOf(this))
                .with(DOWN, downState.isOf(this));
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState state2, WorldAccess levelreader, BlockPos blockpos, BlockPos pos2) {
        BlockPos n = blockpos.north();
        BlockPos e = blockpos.east();
        BlockPos s = blockpos.south();
        BlockPos w = blockpos.west();
        BlockPos u = blockpos.up();
        BlockPos d = blockpos.down();
        BlockState northState = levelreader.getBlockState(n);
        BlockState eastState = levelreader.getBlockState(e);
        BlockState southState = levelreader.getBlockState(s);
        BlockState westState = levelreader.getBlockState(w);
        BlockState upState = levelreader.getBlockState(u);
        BlockState downState = levelreader.getBlockState(d);
        return state.with(NORTH, northState.isOf(this))
                .with(NORTH, northState.isOf(this))
                .with(EAST, eastState.isOf(this))
                .with(SOUTH, southState.isOf(this))
                .with(WEST, westState.isOf(this))
                .with(UP, upState.isOf(this))
                .with(DOWN, downState.isOf(this));
    }

    private static Boolean noOption(BlockState p_50806_, BlockView p_50807_, BlockPos p_50808_) {
        return false;
    }

    private static Boolean yes(BlockState p_50806_, BlockView p_50807_, BlockPos p_50808_) {
        return false;
    }

    private static Boolean noOption(BlockState p_50779_, BlockView p_50780_, BlockPos p_50781_, EntityType<?> p_50782_) {
        return false;
    }


}
