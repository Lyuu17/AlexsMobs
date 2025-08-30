package com.github.alexthe666.alexsmobs.block;

import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class BlockBananaPeel extends PlantBlock {

    protected static final VoxelShape SHAPE_COLLISON = Block.createCuboidShape(0, 0, 0, 16.0D, 9.0D, 16.0D);
    protected static final VoxelShape SHAPE = Block.createCuboidShape(2.0D, 0.0D, 2.0D, 14.0D, 4.0D, 14.0D);

    public BlockBananaPeel() {
        super(AbstractBlock.Settings.create()
                .dynamicBounds()
                .sounds(BlockSoundGroup.WET_GRASS)
                .noCollision()
                .requiresTool()
                .strength(0.2F)
                .slipperiness(0.9999999999F));
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
    }

    @Override
    protected boolean canPlantOnTop(BlockState state, BlockView worldIn, BlockPos pos) {
        return hasTopRim(worldIn, pos);
    }

    public AbstractBlock.OffsetType getOffsetType() {
        return AbstractBlock.OffsetType.XZ;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView worldIn, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView worldIn, BlockPos pos, ShapeContext context) {
        return SHAPE_COLLISON;
    }

    @Override
    public VoxelShape getSidesShape(BlockState state, BlockView reader, BlockPos pos) {
        return SHAPE_COLLISON;
    }

    @Override
    public VoxelShape getCameraCollisionShape(BlockState state, BlockView reader, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

}
