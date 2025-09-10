package com.github.alexthe666.alexsmobs.block;

import net.minecraft.block.*;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class BlockEnderResidue extends AbstractGlassBlock {

    public static final IntProperty AGE = Properties.AGE_3;
    public static final BooleanProperty SLOW_DECAY = BooleanProperty.of("slow_decay");

    public BlockEnderResidue() {
        super(AbstractBlock.Settings.create()
                .mapColor(MapColor.LIGHT_BLUE)
                .nonOpaque()
                .postProcess((i, j, k) -> true)
                .emissiveLighting((i, j, k) -> true)
                .luminance(i -> 3)
                .strength(0.2F)
                .sounds(BlockSoundGroup.AMETHYST_BLOCK)
                .ticksRandomly()
                .nonOpaque());
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(AGE, 0)
                .with(SLOW_DECAY, false));
    }

    @Override
    public void randomTick(BlockState p_53588_, ServerWorld p_53589_, BlockPos p_53590_, Random p_53591_) {
        this.scheduledTick(p_53588_, p_53589_, p_53590_, p_53591_);
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld level, BlockPos pos, Random random) {
        if (random.nextInt(state.get(SLOW_DECAY) ? 15 : 5) == 0) {
            var blockpos$mutableblockpos = new BlockPos.Mutable();
            for (var direction : Direction.values()) {
                blockpos$mutableblockpos.set(pos, direction);
                BlockState blockstate = level.getBlockState(blockpos$mutableblockpos);
                if (blockstate.isOf(this) && !this.incrementAge(blockstate, level, blockpos$mutableblockpos)) {
                    level.scheduleBlockTick(blockpos$mutableblockpos, this, MathHelper.nextInt(random, 20, 40));
                }
            }
            this.incrementAge(state, level, pos);
        } else {
            level.scheduleBlockTick(pos, this, MathHelper.nextInt(random, 20, 40));
        }
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
    public void neighborUpdate(BlockState p_53579_, World p_53580_, BlockPos p_53581_, Block p_53582_, BlockPos p_53583_, boolean p_53584_) {
        super.neighborUpdate(p_53579_, p_53580_, p_53581_, p_53582_, p_53583_, p_53584_);
    }

    private boolean fewerNeigboursThan(BlockView p_53566_, BlockPos p_53567_, int p_53568_) {
        int i = 0;
        var blockpos$mutableblockpos = new BlockPos.Mutable();

        for (Direction direction : Direction.values()) {
            blockpos$mutableblockpos.set(p_53567_, direction);
            if (p_53566_.getBlockState(blockpos$mutableblockpos).isOf(this)) {
                ++i;
                if (i >= p_53568_) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> p_53586_) {
        p_53586_.add(AGE, SLOW_DECAY);
    }

    @Override
    public ItemStack getPickStack(BlockView p_53570_, BlockPos p_53571_, BlockState p_53572_) {
        return ItemStack.EMPTY;
    }
}