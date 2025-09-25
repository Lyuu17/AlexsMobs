package com.github.alexthe666.alexsmobs.block;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.block.entity.TransmutationTableBlockEntity;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.packet.UpdateTransmutablesToDisplayPacket;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.stat.Stats;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class TransmutationTableBlock extends BlockWithEntity implements AMSpecialRenderBlock {

    public static final DirectionProperty FACING = HorizontalFacingBlock.FACING;
    private static final VoxelShape BASE_AABB = Block.createCuboidShape(1, 0, 1, 15, 5, 15);
    private static final VoxelShape ARMS_NS = Block.createCuboidShape(1, 5, 5.5F, 15, 16, 10.5F);
    private static final VoxelShape ARMS_EW = Block.createCuboidShape(5.5F, 5, 1, 10.5F, 16, 15);
    private static final VoxelShape NS_AABB = VoxelShapes.union(BASE_AABB, ARMS_NS);
    private static final VoxelShape EW_AABB = VoxelShapes.union(BASE_AABB, ARMS_EW);

    public TransmutationTableBlock() {
        super(Settings.create()
                .pistonBehavior(PistonBehavior.BLOCK)
                .mapColor(DyeColor.BLACK)
                .nonOpaque()
                .luminance((block) -> 2)
                .emissiveLighting((block, world, pos) -> true)
                .sounds(BlockSoundGroup.STONE).strength(1F).requiresTool());
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(FACING, Direction.NORTH));
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView worldIn, BlockPos pos, ShapeContext context) {
        return state.get(FACING).getAxis() == Direction.Axis.Z ? NS_AABB : EW_AABB;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new TransmutationTableBlockEntity(pos, state);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        return this.getDefaultState().with(FACING, context.getHorizontalPlayerFacing().getOpposite());
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
        builder.add(FACING);
    }

    @Override
    public ActionResult onUse(BlockState state, World level, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult result) {
        if (level.isClient) {
            return ActionResult.SUCCESS;
        }

        var handler = (ExtendedMenuProvider) level.getBlockEntity(pos);
        MenuRegistry.openExtendedMenu((ServerPlayerEntity) player, handler);

        player.incrementStat(Stats.INTERACT_WITH_LOOM);
        BlockEntity te = level.getBlockEntity(pos);
        if (te instanceof TransmutationTableBlockEntity table) {
            AlexsMobs.sendMSGToAll(new UpdateTransmutablesToDisplayPacket(player.getId(), table.getPossibility(0), table.getPossibility(1), table.getPossibility(2)));
        }
        return ActionResult.CONSUME;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return (world1, pos, state1, blockEntity) -> {
            if (blockEntity instanceof TransmutationTableBlockEntity transmutationTable) {
                transmutationTable.tick(world1, pos, state1, transmutationTable);
            }
        };
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (AMConfig.transmutingTableExplodes){
            world.createExplosion(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 3F, false, World.ExplosionSourceType.BLOCK);
        }
        super.onBreak(world, pos, state, player);
    }
}

