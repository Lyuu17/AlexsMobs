package com.github.alexthe666.alexsmobs.block;

import net.minecraft.block.*;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

public class BisonCarpetBlock extends CarpetBlock {

    protected static final VoxelShape SELECTION_SHAPE = Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D);

    public BisonCarpetBlock() {
        super(AbstractBlock.Settings.create()
                .mapColor(MapColor.BROWN)
                .strength(0.6F, 1.0F)
                .sounds(BlockSoundGroup.WOOL));
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SELECTION_SHAPE;
    }

}
