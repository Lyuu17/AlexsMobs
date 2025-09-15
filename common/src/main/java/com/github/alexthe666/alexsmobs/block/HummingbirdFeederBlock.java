package com.github.alexthe666.alexsmobs.block;

import com.github.alexthe666.alexsmobs.registry.AMEffectRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import net.minecraft.block.*;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potions;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
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

public class HummingbirdFeederBlock extends Block {
    public static final IntProperty CONTENTS = IntProperty.of("contents", 0, 3);
    public static final BooleanProperty HANGING = Properties.HANGING;
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
    private static final VoxelShape AABB = Block.createCuboidShape(4, 0, 4, 12, 12, 12);
    private static final VoxelShape AABB_HANGING = Block.createCuboidShape(4, 0, 4, 12, 16, 12);

    public HummingbirdFeederBlock() {
        super(AbstractBlock.Settings.create()
                .mapColor(MapColor.ORANGE)
                .sounds(BlockSoundGroup.LANTERN)
                .strength(0.5F)
                .ticksRandomly()
                .nonOpaque());
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(CONTENTS, 0)
                .with(HANGING, false));
    }

    @Deprecated
    public VoxelShape getShape(BlockState state, BlockView worldIn, BlockPos pos, ShapeContext context) {
        return state.get(HANGING) ? AABB_HANGING : AABB;
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        var fluidstate = context.getWorld().getFluidState(context.getBlockPos());
        for(var direction : context.getPlacementDirections()) {
            if (direction.getAxis() == Direction.Axis.Y) {
                BlockState blockstate = this.getDefaultState().with(HANGING, Boolean.valueOf(direction == Direction.UP));
                if (blockstate.canPlaceAt(context.getWorld(), context.getBlockPos())) {
                    return blockstate.with(WATERLOGGED, fluidstate.getFluid() == Fluids.WATER);
                }
            }
        }

        return null;
    }

    protected static Direction getBlockConnected(BlockState state) {
        return state.get(HANGING) ? Direction.DOWN : Direction.UP;
    }

    @Override
    public ActionResult onUse(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockHitResult hit) {
        int contents = state.get(CONTENTS);
        var waterBottle = AMEffectRegistry.createPotion(Potions.WATER);
        var itemStack = player.getStackInHand(handIn);
        int setContent = -1;
        if(contents == 0){
            if(itemStack.isIn(AMTagRegistry.HUMMINGNBIRD_FEEDER_SWEETENERS)){
                setContent = 2;
                useItem(player, itemStack, false);
            }else if(itemStack.getItem() == waterBottle.getItem() && ItemStack.canCombine(waterBottle, itemStack)){
                setContent = 1;
                useItem(player, itemStack, true);
            }
        }else if(contents == 1){
            if(itemStack.isIn(AMTagRegistry.HUMMINGNBIRD_FEEDER_SWEETENERS)){
                setContent = 3;
                useItem(player, itemStack, false);
            }
        }else if(contents == 2){
            if(itemStack.getItem() == waterBottle.getItem() && ItemStack.canCombine(waterBottle, itemStack)){
                setContent = 3;
                useItem(player, itemStack, true);
            }
        }
        if(setContent >= 0){
            worldIn.setBlockState(pos, state.with(CONTENTS, setContent));
            return ActionResult.SUCCESS;
        }
        return ActionResult.FAIL;
    }

    public void useItem(PlayerEntity playerEntity, ItemStack stack, boolean dropBottle){
        if(!playerEntity.isCreative()){
            if(dropBottle){
                playerEntity.giveItemStack(new ItemStack(Items.GLASS_BOTTLE));
            }
            stack.decrement(1);
        }
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView worldIn, BlockPos pos) {
        var direction = getBlockConnected(state).getOpposite();
        return Block.sideCoversSmallSquare(worldIn, pos.offset(direction), direction.getOpposite());
    }

    // FIXME piston behavior should be a setting
    public PistonBehavior getPistonBehavior(BlockState state) {
        return PistonBehavior.DESTROY;
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState stateIn, Direction facing, BlockState facingState, WorldAccess worldIn, BlockPos currentPos, BlockPos facingPos) {
        if (stateIn.get(WATERLOGGED)) {
            worldIn.scheduleFluidTick(currentPos, Fluids.WATER, Fluids.WATER.getTickRate(worldIn));
        }

        return getBlockConnected(stateIn).getOpposite() == facing && !stateIn.canPlaceAt(worldIn, currentPos) ? Blocks.AIR.getDefaultState() : super.getStateForNeighborUpdate(stateIn, facing, facingState, worldIn, currentPos, facingPos);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    @Override
    public boolean canPathfindThrough(BlockState state, BlockView worldIn, BlockPos pos, NavigationType type) {
        return false;
    }

    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(CONTENTS, HANGING, WATERLOGGED);
    }
}
