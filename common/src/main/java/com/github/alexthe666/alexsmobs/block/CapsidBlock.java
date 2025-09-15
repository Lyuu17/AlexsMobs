package com.github.alexthe666.alexsmobs.block;


import com.github.alexthe666.alexsmobs.block.entity.CapsidBlockEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class CapsidBlock extends BlockWithEntity {

    public static final DirectionProperty HORIZONTAL_FACING = HorizontalFacingBlock.FACING;
    public CapsidBlock() {
        super(Settings.create()
                .mapColor(MapColor.PURPLE)
                .nonOpaque()
                .allowsSpawning(CapsidBlock::spawnOption)
                .solidBlock(CapsidBlock::isntSolid)
                .sounds(BlockSoundGroup.GLASS)
                .luminance((state) -> 5)
                .requiresTool()
                .strength(1.5F));
    }

    @Override
    public BlockState rotate(BlockState p_185499_1_, BlockRotation p_185499_2_) {
        return p_185499_1_
                .with(HORIZONTAL_FACING, p_185499_2_.rotate(p_185499_1_.get(HORIZONTAL_FACING)));
    }

    @Override
    public BlockState mirror(BlockState p_185471_1_, BlockMirror p_185471_2_) {
        return p_185471_1_.rotate(p_185471_2_.getRotation(p_185471_1_.get(HORIZONTAL_FACING)));
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        return this.getDefaultState().with(HORIZONTAL_FACING, context.getHorizontalPlayerFacing());
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(HORIZONTAL_FACING);
    }

    private static Boolean spawnOption(BlockState state, BlockView reader, BlockPos pos, EntityType<?> entity) {
        return false;
    }

    private static boolean isntSolid(BlockState state, BlockView reader, BlockPos pos) {
        return false;
    }

    @Environment(EnvType.CLIENT)
    public boolean isSideInvisible(BlockState state, BlockState stateFrom, Direction direction) {
        return stateFrom.getBlock() == this || super.isSideInvisible(state, stateFrom, direction);
    }

    @Override
    public ActionResult onUse(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockHitResult hit) {
        var heldItem = player.getStackInHand(handIn);
        if (worldIn.getBlockEntity(pos) instanceof CapsidBlockEntity capsid && (!player.isSneaking()  && heldItem.getItem() != this.asItem())) {
            var copy = heldItem.copy();
            copy.setCount(1);
            if(capsid.getStack(0).isEmpty()){
                capsid.setStack(0, copy);
                if(!player.isCreative()){
                    heldItem.decrement(1);
                }
                return ActionResult.SUCCESS;
            }else if(ItemStack.canCombine(capsid.getStack(0), copy) && capsid.getStack(0).getMaxCount() > capsid.getStack(0).getCount() + copy.getCount()){
                capsid.getStack(0).increment(1);
                if(!player.isCreative()){
                    heldItem.decrement(1);
                }
                return ActionResult.SUCCESS;
            }else{
                dropStack(worldIn, pos, capsid.getStack(0).copy());
                capsid.setStack(0, ItemStack.EMPTY);
                return ActionResult.SUCCESS;
            }
        }
        return ActionResult.PASS;
    }

    @Override
    public void onStateReplaced(BlockState state, World worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
        var tileentity = worldIn.getBlockEntity(pos);
        if (tileentity instanceof CapsidBlockEntity) {
            ItemScatterer.spawn(worldIn, pos, (CapsidBlockEntity) tileentity);
            worldIn.updateComparators(pos, this);
        }
        super.onStateReplaced(state, worldIn, pos, newState, isMoving);
    }

    @Override
    public BlockRenderType getRenderType(BlockState p_149645_1_) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CapsidBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return (world1, pos, state1, blockEntity) -> {
            if (blockEntity instanceof CapsidBlockEntity capsidBlockEntity) {
                capsidBlockEntity.tick(world1, pos, state1, capsidBlockEntity);
            }
        };
    }
}
