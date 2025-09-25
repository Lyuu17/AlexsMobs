package com.github.alexthe666.alexsmobs.block;

import com.github.alexthe666.alexsmobs.block.entity.EndPirateDoorBlockEntity;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.DoorHinge;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.*;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class EndPirateDoorBlock extends BlockWithEntity {

    public static final DirectionProperty HORIZONTAL_FACING = HorizontalFacingBlock.FACING;
    public static final BooleanProperty OPEN = BooleanProperty.of("open");
    public static final BooleanProperty POWERED = Properties.POWERED;
    public static final EnumProperty<DoorHinge> HINGE = Properties.DOOR_HINGE;
    public static final IntProperty SEGMENT = IntProperty.of("segment", 0, 2);
    protected static final float AABB_DOOR_THICKNESS = 3.0F;
    protected static final VoxelShape SOUTH_AABB = Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 2.0D);
    protected static final VoxelShape NORTH_AABB = Block.createCuboidShape(0.0D, 0.0D, 14.0D, 16.0D, 16.0D, 16.0D);
    protected static final VoxelShape WEST_AABB = Block.createCuboidShape(14.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    protected static final VoxelShape EAST_AABB = Block.createCuboidShape(0.0D, 0.0D, 0.0D, 2.0D, 16.0D, 16.0D);

    public EndPirateDoorBlock() {
        super(Settings.create()
                .mapColor(MapColor.PURPLE)
                .nonOpaque()
                .sounds(BlockSoundGroup.GLASS)
                .luminance((state) -> 3)
                .requiresTool()
                .strength(1.5F));
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(SEGMENT, 0)
                .with(OPEN, false)
                .with(HINGE, DoorHinge.RIGHT)
                .with(HORIZONTAL_FACING, Direction.NORTH));
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView getter, BlockPos pos, ShapeContext context) {
        var direction = state.get(HORIZONTAL_FACING);
        boolean flag = !state.get(OPEN);
        boolean flag1 = state.get(HINGE) == DoorHinge.RIGHT;
        return switch (direction) {
            case SOUTH -> flag ? SOUTH_AABB : (flag1 ? EAST_AABB : WEST_AABB);
            case WEST -> flag ? WEST_AABB : (flag1 ? SOUTH_AABB : NORTH_AABB);
            case NORTH -> flag ? NORTH_AABB : (flag1 ? WEST_AABB : EAST_AABB);
            default -> flag ? EAST_AABB : (flag1 ? NORTH_AABB : SOUTH_AABB);
        };
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState state2, WorldAccess level, BlockPos pos, BlockPos p_52801_) {
        if(state.get(SEGMENT) == 0){
            return !state.canPlaceAt(level, pos) || !level.getBlockState(pos.up()).isOf(this) || !level.getBlockState(pos.up(2)).isOf(this) ? Blocks.AIR.getDefaultState() : super.getStateForNeighborUpdate(state, direction, state2, level, pos, p_52801_);

        }
        return !state.canPlaceAt(level, pos) ? Blocks.AIR.getDefaultState() : super.getStateForNeighborUpdate(state, direction, state2, level, pos, p_52801_);
    }

    @Override
    public BlockState rotate(BlockState p_185499_1_, BlockRotation p_185499_2_) {
        return p_185499_1_.with(HORIZONTAL_FACING, p_185499_2_.rotate(p_185499_1_.get(HORIZONTAL_FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mir) {
        return state.rotate(mir.getRotation(state.get(HORIZONTAL_FACING)));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(SEGMENT, HORIZONTAL_FACING, OPEN, HINGE, POWERED);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return state.get(SEGMENT) == 0 ? BlockRenderType.ENTITYBLOCK_ANIMATED : BlockRenderType.INVISIBLE;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return state.get(SEGMENT) == 0 ? new EndPirateDoorBlockEntity(pos, state) : null;
    }

    @Override
    public void neighborUpdate(BlockState state, World level, BlockPos pos, Block block, BlockPos p_52780_, boolean p_52781_) {

        boolean flag = level.isReceivingRedstonePower(pos);
        flag = switch (state.get(SEGMENT)) {
            case 0 -> flag || level.isReceivingRedstonePower(pos.up()) || level.isReceivingRedstonePower(pos.up(2));
            case 1 -> flag || level.isReceivingRedstonePower(pos.down()) || level.isReceivingRedstonePower(pos.up());
            case 2 -> flag || level.isReceivingRedstonePower(pos.down()) || level.isReceivingRedstonePower(pos.down(2));
            default -> flag;
        };

        if (!this.getDefaultState().isOf(block) && flag != state.get(POWERED)) {
            if (flag != state.get(OPEN)) {
               // level.emitGameEvent(flag ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
            }
            Direction swap = state.get(HINGE) == DoorHinge.LEFT ? state.get(HORIZONTAL_FACING).rotateYClockwise() : state.get(HORIZONTAL_FACING).rotateYCounterclockwise();
            BlockPos relative = pos.offset(swap);
            BlockState neighbor = level.getBlockState(relative);
            if(neighbor.getBlock() == this && state.get(HINGE) != neighbor.get(HINGE)){
                openDoorAt(level, relative, flag, flag);
            }
            openDoorAt(level, pos, flag, flag);

        }

    }

    @Override
    public ActionResult onUse(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockHitResult hit) {
        boolean open = state.get(OPEN);
        boolean powered = state.get(POWERED);
        Direction swap = state.get(HINGE) == DoorHinge.LEFT ? state.get(HORIZONTAL_FACING).rotateYClockwise() : state.get(HORIZONTAL_FACING).rotateYCounterclockwise();
        BlockPos relative = pos.offset(swap);
        BlockState neighbor = worldIn.getBlockState(relative);
        if(neighbor.getBlock() == this && state.get(HINGE) != neighbor.get(HINGE)){
            openDoorAt(worldIn, relative, !open, powered);
        }
        openDoorAt(worldIn, pos, !open, powered);
        return ActionResult.success(worldIn.isClient);
    }

    public static void openDoorAt(World worldIn, BlockPos pos, boolean open, boolean powered) {
        var te = getDoorTE(worldIn, pos);
        if(te != null){
            BlockPos bottom = te.getPos();
            for(int i = 0; i <= 2; i++){
                BlockPos up = bottom.up(i);
                if(worldIn.getBlockState(up).getBlock() instanceof EndPirateDoorBlock){
                    worldIn.setBlockState(up, worldIn.getBlockState(up).with(OPEN, open).with(POWERED, powered), 10);
                }
            }
        }
    }

    public static EndPirateDoorBlockEntity getDoorTE(World worldIn, BlockPos pos) {
        for (int i = 0; i <= 2; i++){
            if(worldIn.getBlockEntity(pos.down(i)) instanceof EndPirateDoorBlockEntity e){
                return e;
            }
        }
        return null;
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext p_52739_) {
        var blockpos = p_52739_.getBlockPos();
        var level = p_52739_.getWorld();
        if (blockpos.getY() < level.getTopY() - 1 && level.getBlockState(blockpos.up()).canReplace(p_52739_) && level.getBlockState(blockpos.up(2)).canReplace(p_52739_)) {
            boolean flag = level.isReceivingRedstonePower(blockpos) || level.isReceivingRedstonePower(blockpos.up());
            return this.getDefaultState().with(HORIZONTAL_FACING, p_52739_.getHorizontalPlayerFacing()).with(HINGE, this.getHinge(p_52739_)).with(OPEN, flag).with(SEGMENT, 0);
        } else {
            return null;
        }
    }

    @Override
    public void onPlaced(World p_52749_, BlockPos p_52750_, BlockState p_52751_, LivingEntity p_52752_, ItemStack p_52753_) {
        p_52749_.setBlockState(p_52750_.up(), p_52751_.with(SEGMENT, 1), 3);
        p_52749_.setBlockState(p_52750_.up(2), p_52751_.with(SEGMENT, 2), 3);
    }

    private DoorHinge getHinge(ItemPlacementContext p_52805_) {
        BlockView blockgetter = p_52805_.getWorld();
        BlockPos blockpos = p_52805_.getBlockPos();
        Direction direction = p_52805_.getHorizontalPlayerFacing();
        BlockPos blockpos1 = blockpos.up();
        Direction direction1 = direction.rotateYCounterclockwise();
        BlockPos blockpos2 = blockpos.offset(direction1);
        BlockState blockstate = blockgetter.getBlockState(blockpos2);
        BlockPos blockpos3 = blockpos1.offset(direction1);
        BlockState blockstate1 = blockgetter.getBlockState(blockpos3);
        Direction direction2 = direction.rotateYClockwise();
        BlockPos blockpos4 = blockpos.offset(direction2);
        BlockState blockstate2 = blockgetter.getBlockState(blockpos4);
        BlockPos blockpos5 = blockpos1.offset(direction2);
        BlockState blockstate3 = blockgetter.getBlockState(blockpos5);
        int i = (blockstate.isFullCube(blockgetter, blockpos2) ? -1 : 0) + (blockstate1.isFullCube(blockgetter, blockpos3) ? -1 : 0) + (blockstate2.isFullCube(blockgetter, blockpos4) ? 1 : 0) + (blockstate3.isFullCube(blockgetter, blockpos5) ? 1 : 0);
        boolean flag = blockstate.isOf(this) && blockstate.get(SEGMENT) == 0;
        boolean flag1 = blockstate2.isOf(this) && blockstate2.get(SEGMENT) == 0;
        if ((!flag || flag1) && i <= 0) {
            if ((!flag1 || flag) && i >= 0) {
                int j = direction.getOffsetX();
                int k = direction.getOffsetZ();
                Vec3d vec3 = p_52805_.getHitPos();
                double d0 = vec3.x - (double)blockpos.getX();
                double d1 = vec3.z - (double)blockpos.getZ();
                return (j >= 0 || !(d1 < 0.5D)) && (j <= 0 || !(d1 > 0.5D)) && (k >= 0 || !(d0 > 0.5D)) && (k <= 0 || !(d0 < 0.5D)) ? DoorHinge.LEFT : DoorHinge.RIGHT;
            } else {
                return DoorHinge.LEFT;
            }
        } else {
            return DoorHinge.RIGHT;
        }
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        return switch (state.get(SEGMENT)) {
            case 0 -> world.getBlockState(pos.down()).isSideSolidFullSquare(world, pos.down(), Direction.UP);
            case 1 -> world.getBlockState(pos.down()).isOf(this) && world.getBlockState(pos.up()).isOf(this);
            case 2 -> world.getBlockState(pos.down()).isOf(this) && world.getBlockState(pos.down(2)).isOf(this);
            default -> false;
        };
    }

    @Override
    public List<ItemStack> getDroppedStacks(BlockState state, LootContextParameterSet.Builder builder) {
        return state.get(SEGMENT) == 0? super.getDroppedStacks(state, builder) : Collections.emptyList();
    }
}
