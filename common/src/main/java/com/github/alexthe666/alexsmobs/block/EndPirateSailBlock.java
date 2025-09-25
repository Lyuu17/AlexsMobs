package com.github.alexthe666.alexsmobs.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.MapColor;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;

import java.util.Locale;

public class EndPirateSailBlock extends Block {

    public static final BooleanProperty EASTORWEST = BooleanProperty.of("eastorwest");
    public static final EnumProperty<SailType> SAIL = EnumProperty.of("sail", SailType.class);
    protected static final VoxelShape EW_AABB = Block.createCuboidShape(7.0D, 0.0D, 0.0D, 9.0D, 16.0D, 16.0D);
    protected static final VoxelShape NS_AABB = Block.createCuboidShape(0.0D, 0.0D, 7.0D, 16.0D, 16.0D, 9.0D);

    public EndPirateSailBlock(boolean spectre) {
        super(Settings.create()
                .mapColor(MapColor.BLUE)
                .nonOpaque()
                .emissiveLighting((a, b, c) -> true)
                .sounds(BlockSoundGroup.WOOL)
                .luminance((state) -> 5)
                .requiresTool()
                .strength(0.4F));
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(EASTORWEST, Boolean.FALSE)
                .with(SAIL, SailType.SINGLE));
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView worldIn, BlockPos pos, ShapeContext context) {
        return state.get(EASTORWEST) ? EW_AABB : NS_AABB;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> p_58032_) {
        p_58032_.add(EASTORWEST, SAIL);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        var levelreader = context.getWorld();
        var blockpos = context.getBlockPos();
        var actualPos = context.getBlockPos().offset(context.getSide().getOpposite());
        var u = blockpos.up();
        var d = blockpos.down();
        var clickState = levelreader.getBlockState(actualPos);
        var upState = levelreader.getBlockState(u);
        var downState = levelreader.getBlockState(d);
        boolean axis = context.getSide().getAxis() == Direction.Axis.Y ? context.getHorizontalPlayerFacing().getAxis() == Direction.Axis.X : context.getSide().getAxis() != Direction.Axis.X;
        if(clickState.getBlock() instanceof EndPirateSailBlock){
            axis = clickState.get(EASTORWEST);
        }
        var axisState = getDefaultState().with(EASTORWEST, axis);
        return axisState.with(SAIL, getSailTypeFor(axisState, downState, upState));
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState state2, WorldAccess levelreader, BlockPos blockpos, BlockPos pos2) {
        var u = blockpos.up();
        var d = blockpos.down();
        var upState = levelreader.getBlockState(u);
        var downState = levelreader.getBlockState(d);
        return state.with(SAIL, getSailTypeFor(state, downState, upState));
    }

    private static SailType getSailTypeFor(BlockState us, BlockState below, BlockState above){
        if(below.getBlock() instanceof EndPirateSailBlock && below.get(EASTORWEST) == us.get(EASTORWEST)){
            return above.getBlock() instanceof EndPirateSailBlock ? SailType.MIDDLE : SailType.TOP;
        }else if(above.getBlock() instanceof EndPirateSailBlock && above.get(EASTORWEST) == us.get(EASTORWEST)){
            return SailType.BOTTOM;
        }else{
            return SailType.SINGLE;
        }
    }
    private enum SailType implements StringIdentifiable {
        SINGLE,
        TOP,
        MIDDLE,
        BOTTOM;

        @Override
        public String toString() {
            return this.asString();
        }

        @Override
        public String asString() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }
}
