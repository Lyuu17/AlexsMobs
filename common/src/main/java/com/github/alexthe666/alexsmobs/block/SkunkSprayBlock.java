package com.github.alexthe666.alexsmobs.block;

import com.github.alexthe666.alexsmobs.registry.AMBlockRegistry;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import com.github.alexthe666.alexsmobs.registry.AMParticleRegistry;
import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

import java.util.ArrayList;

public class SkunkSprayBlock extends MultifaceGrowthBlock implements Waterloggable {

    public static final IntProperty AGE = Properties.AGE_3;
    private static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;

    public SkunkSprayBlock() {
        super(AbstractBlock.Settings.create()
                .mapColor(MapColor.LIME)
                .nonOpaque()
                .ticksRandomly()
                .noCollision()
                .breakInstantly()
                .sounds(BlockSoundGroup.FROGSPAWN));
        this.setDefaultState(this.getDefaultState()
                .with(WATERLOGGED, Boolean.FALSE)
                .with(AGE, 0));
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState state2, WorldAccess levelAccessor, BlockPos pos, BlockPos pos2) {
        if (state.get(WATERLOGGED)) {
            levelAccessor.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(levelAccessor));
        }

        return super.getStateForNeighborUpdate(state, direction, state2, levelAccessor, pos, pos2);
    }

    @Override
    public void randomTick(BlockState state, ServerWorld level, BlockPos pos, Random randomSource) {
        this.tick(state, level, pos, randomSource);
    }

    public void tick(BlockState state, ServerWorld level, BlockPos pos, Random random) {
        if (random.nextInt(8) == 0) {
            var blockpos$mutableblockpos = new BlockPos.Mutable();
            for (Direction direction : Direction.values()) {
                blockpos$mutableblockpos.set(pos, direction);
                BlockState blockstate = level.getBlockState(blockpos$mutableblockpos);
                if (blockstate.isOf(this) && !this.incrementAge(blockstate, level, blockpos$mutableblockpos)) {
                    level.scheduleBlockTick(blockpos$mutableblockpos, this, MathHelper.nextInt(random, 50, 100));
                }
            }
            this.incrementAge(state, level, pos);
        } else {
            level.scheduleBlockTick(pos, this, MathHelper.nextInt(random, 50, 100));
        }
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> definition) {
        super.appendProperties(definition);
        definition.add(WATERLOGGED, AGE);
    }

    @Override
    public ActionResult onUse(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockHitResult hit) {
        ItemStack itemStack = player.getStackInHand(handIn);
        if(itemStack.isOf(Items.GLASS_BOTTLE)) {
           Direction dir = hit.getSide().getOpposite();
           if(hasDirection(state, dir)){
               worldIn.setBlockState(pos, removeStinkFace(state, dir));
               ItemStack bottle = new ItemStack(AMItemRegistry.STINK_BOTTLE.get());
               if(!player.giveItemStack(bottle)){
                   player.dropItem(bottle, false);
               }
               if(!player.isCreative()){
                   itemStack.decrement(1);
               }
               return ActionResult.SUCCESS;
           }
        }
        return super.onUse(state, worldIn, pos, player, handIn, hit);
    }

    public static BlockState removeStinkFace(BlockState state, Direction faceProperty) {
        var blockstate = state.with(getProperty(faceProperty), Boolean.FALSE);
        return hasAnyDirection(blockstate) ? blockstate : Blocks.AIR.getDefaultState();
    }

    private boolean incrementAge(BlockState state, World level, BlockPos pos) {
        int i = state.get(AGE);
        if (i < 3) {
            level.setBlockState(pos, state.with(AGE, i + 1), 2);
            return false;
        } else {
            level.setBlockState(pos, Blocks.AIR.getDefaultState(), 2);
            return true;
        }
    }

    @Override
    public boolean canReplace(BlockState state, ItemPlacementContext context) {
        return !context.getStack().isOf(AMBlockRegistry.SKUNK_SPRAY.get().asItem()) || super.canReplace(state, context);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    @Override
    public LichenGrower getGrower() {
        return null;
    }

    @Override
    public boolean isTransparent(BlockState state, BlockView level, BlockPos pos) {
        return state.getFluidState().isEmpty();
    }

    @Override
    public void randomDisplayTick(BlockState blockState, World level, BlockPos pos, Random randomSource) {
        if (randomSource.nextInt(2) == 0) {
            var faces = new ArrayList<>(collectDirections(blockState));
            Direction direction = null;
            if (faces.size() == 1) {
                direction = faces.get(0);
            } else if (faces.size() > 1) {
                direction = Util.getRandom(faces, randomSource);
            }
            if (direction != null) {
                double d0 = direction.getOffsetX() == 0 ? randomSource.nextDouble() : 0.5D + (double) direction.getOffsetX() * 0.8D;
                double d1 = direction.getOffsetY() == 0 ? randomSource.nextDouble() : 0.5D + (double) direction.getOffsetY() * 0.8D;
                double d2 = direction.getOffsetZ() == 0 ? randomSource.nextDouble() : 0.5D + (double) direction.getOffsetZ() * 0.8D;
                level.addParticle(AMParticleRegistry.SMELLY.get(), (double) pos.getX() + d0, (double) pos.getY() + d1, (double) pos.getZ() + d2, 0.0D, 0.0D, 0.0D);
            }
        }
    }
}
