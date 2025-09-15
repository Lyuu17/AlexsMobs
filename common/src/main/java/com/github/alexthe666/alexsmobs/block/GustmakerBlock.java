package com.github.alexthe666.alexsmobs.block;

import com.github.alexthe666.alexsmobs.entity.EntityGust;
import net.minecraft.block.*;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class GustmakerBlock extends Block {
    public static final DirectionProperty FACING = FacingBlock.FACING;
    public static final BooleanProperty TRIGGERED = Properties.TRIGGERED;

    public GustmakerBlock() {
        super(AbstractBlock.Settings.create()
                .mapColor(MapColor.PALE_YELLOW)
                .requiresTool()
                .strength(1.5F));
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(TRIGGERED, Boolean.FALSE));
    }

    public static Vec3d getDispensePosition(BlockPos coords, Direction dir) {
        double d0 = coords.getX() + 0.5D + 0.7D * (double) dir.getOffsetX();
        double d1 = coords.getY() + 0.15D + 0.7D * (double) dir.getOffsetY();
        double d2 = coords.getZ() + 0.5D + 0.7D * (double) dir.getOffsetZ();
        return new Vec3d(d0, d1, d2);
    }

    @Override
    public void neighborUpdate(BlockState state, World worldIn, BlockPos pos, net.minecraft.block.Block blockIn, BlockPos fromPos, boolean isMoving) {
        tickGustmaker(state, worldIn, pos, false);
    }

    @Override
    public void randomTick(BlockState state, ServerWorld worldIn, BlockPos pos, Random random) {
        tickGustmaker(state, worldIn, pos, true);
    }

    public void tickGustmaker(BlockState state, World worldIn, BlockPos pos, boolean tickOff) {
        boolean flag = worldIn.isReceivingRedstonePower(pos) || worldIn.isReceivingRedstonePower(pos.down()) || worldIn.isReceivingRedstonePower(pos.up());
        boolean flag1 = state.get(TRIGGERED);
        if (flag && !flag1) {
            if(worldIn.isChunkLoaded(pos)){
                var dispensePosition = getDispensePosition(pos, state.get(FACING));
                var gustDir = Vec3d.of(state.get(FACING).getVector()).multiply(0.1, 0.1, 0.1);
                var gust = new EntityGust(worldIn);
                gust.setGustDir((float) gustDir.x, (float) gustDir.y, (float) gustDir.z);
                gust.setPos(dispensePosition.x, dispensePosition.y, dispensePosition.z);
                if(state.get(FACING).getAxis() == Direction.Axis.Y){
                    gust.setVertical(true);
                }
                if (!worldIn.isClient) {
                    worldIn.spawnEntity(gust);
                }
            }
            worldIn.setBlockState(pos, state.with(TRIGGERED, Boolean.TRUE), 2);
            worldIn.scheduleBlockTick(pos, this, 20);
        } else if (flag1) {
            if (tickOff) {
                worldIn.scheduleBlockTick(pos, this, 20);
                worldIn.setBlockState(pos, state.with(TRIGGERED, Boolean.FALSE), 2);
            }
        }
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        return this.getDefaultState().with(FACING, context.getPlayerLookDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rot) {
        return state.with(FACING, rot.rotate(state.get(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirrorIn) {
        return state.rotate(mirrorIn.getRotation(state.get(FACING)));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, TRIGGERED);
    }
}
