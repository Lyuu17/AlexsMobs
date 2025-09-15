package com.github.alexthe666.alexsmobs.block;

import com.github.alexthe666.alexsmobs.block.entity.SculkBoomerBlockEntity;
import com.github.alexthe666.alexsmobs.registry.AMBlockEntityRegistry;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.event.listener.GameEventListener;
import org.jetbrains.annotations.Nullable;

public class SculkBoomerBlock extends BlockWithEntity {
    public static final BooleanProperty POWERED = Properties.POWERED;
    public static final BooleanProperty OPEN = BooleanProperty.of("open");

    public SculkBoomerBlock() {
        super(AbstractBlock.Settings.create()
                .mapColor(MapColor.BLUE)
                .strength(3.0F, 12.0F)
                .sounds(BlockSoundGroup.SCULK_CATALYST));
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(OPEN, false)
                .with(POWERED, false));
    }

    public void neighborUpdate(BlockState state, World worldIn, BlockPos pos, net.minecraft.block.Block blockIn, BlockPos fromPos, boolean isMoving) {
        if(!worldIn.isClient){
            this.updateState(state, worldIn, pos, blockIn);
        }
    }

    public void randomTick(BlockState state, ServerWorld worldIn, BlockPos pos, Random random) {
        if(!worldIn.isClient){
            this.updateState(state, worldIn, pos, state.getBlock());
        }
    }

    public void updateState(BlockState state, World worldIn, BlockPos pos, Block blockIn) {
        boolean flag = state.get(POWERED);
        boolean flag1 = worldIn.isReceivingRedstonePower(pos);

        if (flag1 != flag) {
            worldIn.setBlockState(pos, state.with(POWERED, flag1), 3);
            worldIn.updateNeighborsAlways(pos.down(), this);
        }
    }


    public BlockState getPlacementState(ItemPlacementContext context) {
        return this.getDefaultState()
                .with(OPEN, false)
                .with(POWERED, context.getWorld().isReceivingRedstonePower(context.getBlockPos()));
    }

//    public BlockRenderLayer getRenderLayer(BlockState state) {
//        return BlockRenderType.MODEL;
//    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new SculkBoomerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World p_222100_, BlockState p_222101_, BlockEntityType<T> p_222102_) {
        return checkType(p_222102_, AMBlockEntityRegistry.SCULK_BOOMER.get(), SculkBoomerBlockEntity::commonTick);
    }


    @Nullable
    @Override
    public <T extends BlockEntity> GameEventListener getGameEventListener(ServerWorld p_222092_, T p_222093_) {
        return p_222093_ instanceof SculkBoomerBlockEntity ? (SculkBoomerBlockEntity)p_222093_ : null;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(POWERED, OPEN);
    }
}
