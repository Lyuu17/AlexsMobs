package com.github.alexthe666.alexsmobs.block;

import com.github.alexthe666.alexsmobs.block.entity.EndPirateAnchorBlockEntity;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class EndPirateAnchorBlock extends BlockWithEntity implements AMSpecialRenderBlock {

    public static final BooleanProperty EASTORWEST = BooleanProperty.of("eastorwest");
    public static final EnumProperty<PieceType> PIECE = EnumProperty.of("piece", PieceType.class);
    protected static final VoxelShape FULL_AABB_EW = Block.createCuboidShape(0.0D, 0.0D, 4D, 16.0D, 16.0D, 12.0D);
    protected static final VoxelShape FULL_AABB_NS = Block.createCuboidShape(4.0D, 0.0D, 0.0D, 12.0D, 16.0D, 16.0D);
    protected static final VoxelShape CHAIN_AABB = Block.createCuboidShape(4.0D, 0.0D, 4.0D, 12.0D, 16.0D, 12.0D);

    protected EndPirateAnchorBlock() {
        super(Settings.create()
                .mapColor(MapColor.BLACK)
                .slipperiness(0.97F)
                .strength(10.0F)
                .luminance((i) -> 6)
                .sounds(BlockSoundGroup.STONE).nonOpaque());
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(EASTORWEST, Boolean.FALSE)
                .with(PIECE, PieceType.ANCHOR));
    }

    public static boolean isClearForPlacement(WorldView reader, BlockPos center, boolean eastOrWest) {
        var offsets = EndPirateAnchorBlockEntity.getValidBBPositions(eastOrWest);
        for (var offset : offsets) {
            var check = center.add(offset);
            if (!reader.isAir(check) || !reader.getBlockState(check).isReplaceable()) {
                return false;
            }
        }
        return true;
    }

    public static void placeAnchor(World level, BlockPos pos, BlockState state) {
        var offsets = EndPirateAnchorBlockEntity.getValidBBPositions(state.get(EASTORWEST));
        for (var offset : offsets) {
            if (!offset.equals(BlockPos.ORIGIN)) {
                level.setBlockState(pos.add(offset), state.with(PIECE, PieceType.ANCHOR_SIDE), 2);
            }
        }
    }

    public static void removeAnchor(World level, BlockPos pos, BlockState state) {
        var offsets = EndPirateAnchorBlockEntity.getValidBBPositions(state.get(EASTORWEST));
        for (var offset : offsets) {
            level.setBlockState(pos.add(offset), Blocks.AIR.getDefaultState(), 67);
        }
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        var levelreader = context.getWorld();
        var blockpos = context.getBlockPos();
        var actualPos = context.getBlockPos().offset(context.getSide().getOpposite());
        var clickState = levelreader.getBlockState(actualPos);
        boolean axis = context.getHorizontalPlayerFacing().getAxis() == Direction.Axis.X;
        if (clickState.getBlock() instanceof EndPirateAnchorBlock) {
            axis = clickState.get(EASTORWEST);
        }
        return isClearForPlacement(levelreader, blockpos, axis) ? getDefaultState().with(EASTORWEST, axis) : null;
    }

    public boolean isLadder(BlockState state, WorldView world, BlockPos pos, LivingEntity entity) {
        return state.get(PIECE) == PieceType.CHAIN;
    }

    public boolean isScaffolding(BlockState state, WorldView world, BlockPos pos, LivingEntity entity) {
        return state.get(PIECE) == PieceType.CHAIN;
    }

    @Override
    public void onEntityCollision(BlockState state, World level, BlockPos pos, Entity entity) {
        if (entity instanceof LivingEntity livingEntity && state.get(PIECE) == PieceType.CHAIN) {
            if (livingEntity.horizontalCollision && !livingEntity.isTouchingWater()) {
                livingEntity.fallDistance = 0.0F;
                var motion = livingEntity.getVelocity();
                double d0 = MathHelper.clamp(motion.x, -0.15F, 0.15F);
                double d1 = MathHelper.clamp(motion.z, -0.15F, 0.15F);
                double d2 = 0.3D;
                if (d2 < 0.0D && livingEntity.isHoldingOntoLadder()) {
                    d2 = 0.0D;
                }
                motion = new Vec3d(d0, d2, d1);
                livingEntity.setVelocity(motion);
            }
        }
    }

    @Override
    public void neighborUpdate(BlockState state, World level, BlockPos pos, Block block, BlockPos p_52780_, boolean p_52781_) {
        if (state.get(PIECE) == PieceType.ANCHOR_SIDE) {
            for (int i = -2; i <= 2; i++) {
                for (int j = -3; j <= 3; j++) {
                    for (int k = -2; k <= 2; k++) {
                        var offsetPos = pos.add(i, j, k);
                        if (level.getBlockEntity(offsetPos) instanceof EndPirateAnchorBlockEntity anchor) {
                            if (!anchor.hasAllAnchorBlocks()) {
                                removeAnchor(level, offsetPos, level.getBlockState(offsetPos));
                                level.breakBlock(offsetPos, true);
                            }
                        }
                    }
                }
            }
        }
        if (!canSurviveAnchor(state, level, pos)) {
            level.setBlockState(pos, Blocks.AIR.getDefaultState(), 2);
        }
    }

    @Override
    public void onPlaced(World level, BlockPos pos, BlockState state, LivingEntity player, ItemStack stack) {
        placeAnchor(level, pos, state);
    }

    public boolean canSurviveAnchor(BlockState state, WorldView world, BlockPos pos) {
        if (state.get(PIECE) == PieceType.ANCHOR) {
            return true;
        } else if (state.get(PIECE) == PieceType.ANCHOR_SIDE) {
            for (int i = -1; i <= 1; i++) {
                for (int j = -3; j <= 0; j++) {
                    for (int k = -1; k <= 1; k++) {
                        BlockPos offsetPos = pos.add(i, j, k);
                        BlockState anchorState = world.getBlockState(offsetPos);
                        if (anchorState.getBlock() instanceof EndPirateAnchorBlock && anchorState.get(PIECE) == PieceType.ANCHOR && isPartOfAnchor(anchorState, world, offsetPos, pos, state.get(EASTORWEST))) {
                            return true;
                        }
                    }
                }
            }
        } else if (state.get(PIECE) == PieceType.CHAIN) {
            BlockPos below = pos.down();
            BlockState chainBelow = world.getBlockState(below);
            BlockState chainAbove = world.getBlockState(below);
            return chainBelow.getBlock() instanceof EndPirateAnchorBlock && (chainAbove.getBlock() instanceof EndPirateAnchorBlock || chainAbove.getBlock() instanceof EndPirateAnchorWinchBlock);
        }
        return false;
    }

    public boolean isPartOfAnchor(BlockState anchor, WorldView level, BlockPos center, BlockPos pos, boolean eastOrWest) {
        if (anchor.get(EndPirateAnchorBlock.EASTORWEST) == eastOrWest) {
            BlockPos offset = pos.subtract(center);
            return EndPirateAnchorBlockEntity.getValidBBPositions(eastOrWest).contains(offset);
        }
        return false;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> p_58032_) {
        p_58032_.add(EASTORWEST, PIECE);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView getter, BlockPos pos, ShapeContext context) {
        if (state.get(PIECE) == PieceType.CHAIN) {
            return CHAIN_AABB;
        }
        return state.get(EASTORWEST) ? FULL_AABB_NS : FULL_AABB_EW;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return state.get(PIECE) == PieceType.ANCHOR ? new EndPirateAnchorBlockEntity(pos, state) : null;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return state.get(PIECE) == PieceType.ANCHOR_SIDE ? BlockRenderType.INVISIBLE : BlockRenderType.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public List<ItemStack> getDroppedStacks(BlockState state, LootContextParameterSet.Builder builder) {
        return state.get(PIECE) == PieceType.ANCHOR ? super.getDroppedStacks(state, builder) : Collections.emptyList();
    }

    public enum PieceType implements StringIdentifiable {
        ANCHOR,
        ANCHOR_SIDE,
        CHAIN;

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
