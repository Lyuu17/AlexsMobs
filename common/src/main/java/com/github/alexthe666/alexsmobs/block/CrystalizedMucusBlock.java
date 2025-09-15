package com.github.alexthe666.alexsmobs.block;

import com.github.alexthe666.alexsmobs.registry.AMBlockRegistry;
import net.minecraft.block.AbstractGlassBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.MapColor;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;

public class CrystalizedMucusBlock extends AbstractGlassBlock {

    public static final int DECAY_DISTANCE = 7;
    public static final IntProperty DISTANCE = Properties.DISTANCE_1_7;
    public static final BooleanProperty PERSISTENT = Properties.PERSISTENT;
    private static final int TICK_DELAY = 1;

    public CrystalizedMucusBlock() {
        super(Settings.create()
                .mapColor(MapColor.YELLOW)
                .strength(0.1F)
                .sounds(BlockSoundGroup.GLASS)
                .nonOpaque()
                .suffocates((s, s1, s2) -> false)
                .blockVision((s, s1, s2) -> false));
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(DISTANCE, 7)
                .with(PERSISTENT, Boolean.FALSE));
    }

    @Override
    public boolean hasRandomTicks(BlockState p_54449_) {
        return p_54449_.get(DISTANCE) == 7 && !p_54449_.get(PERSISTENT);
    }

    @Override
    public void randomTick(BlockState p_221379_, ServerWorld p_221380_, BlockPos p_221381_, Random p_221382_) {
        if (this.decaying(p_221379_)) {
            dropStacks(p_221379_, p_221380_, p_221381_);
            p_221380_.removeBlock(p_221381_, true);
        }
    }

    protected boolean decaying(BlockState p_221386_) {
        return !p_221386_.get(PERSISTENT) && p_221386_.get(DISTANCE) == 7;
    }

    @Override
    public void scheduledTick(BlockState p_221369_, ServerWorld p_221370_, BlockPos p_221371_, Random p_221372_) {
        p_221370_.setBlockState(p_221371_, updateDistance(p_221369_, p_221370_, p_221371_), 3);
    }

    @Override
    public int getOpacity(BlockState p_54460_, BlockView p_54461_, BlockPos p_54462_) {
        return 1;
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState p_54440_, Direction p_54441_, BlockState p_54442_, WorldAccess p_54443_, BlockPos p_54444_, BlockPos p_54445_) {
        int i = getDistanceAt(p_54442_) + 1;
        if (i != 1 || p_54440_.get(DISTANCE) != i) {
            p_54443_.scheduleBlockTick(p_54444_, this, 1);
        }

        return p_54440_;
    }

    private static BlockState updateDistance(BlockState p_54436_, WorldAccess p_54437_, BlockPos p_54438_) {
        int i = 7;
        var blockpos$mutableblockpos = new BlockPos.Mutable();

        for(var direction : Direction.values()) {
            blockpos$mutableblockpos.set(p_54438_, direction);
            i = Math.min(i, getDistanceAt(p_54437_.getBlockState(blockpos$mutableblockpos)) + 1);
            if (i == 1) {
                break;
            }
        }

        return p_54436_.with(DISTANCE, i);
    }

    private static int getDistanceAt(BlockState p_54464_) {
        if (p_54464_.isOf(AMBlockRegistry.BANANA_SLUG_SLIME_BLOCK.get())) {
            return 0;
        } else {
            return p_54464_.getBlock() instanceof CrystalizedMucusBlock ? p_54464_.get(DISTANCE) : 7;
        }
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> p_54447_) {
        p_54447_.add(DISTANCE, PERSISTENT);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext p_54424_) {
        var blockstate = this.getDefaultState().with(PERSISTENT, Boolean.TRUE);
        return updateDistance(blockstate, p_54424_.getWorld(), p_54424_.getBlockPos());
    }
}
