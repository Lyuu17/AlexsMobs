package com.github.alexthe666.alexsmobs.block.entity;

import com.github.alexthe666.alexsmobs.block.EndPirateAnchorBlock;
import com.github.alexthe666.alexsmobs.registry.AMBlockEntityRegistry;
import com.google.common.collect.Lists;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.List;

public class EndPirateAnchorBlockEntity extends BlockEntity {

    private static final List<BlockPos> VALID_OFFSET_BOXES_NS = Lists.newArrayList(
            new BlockPos(0, 0, 0),
            new BlockPos(1, 0, 0),
            new BlockPos(-1, 0, 0),
            new BlockPos(1, 1, 0),
            new BlockPos(-1, 1, 0),
            new BlockPos(0, 1, 0),
            new BlockPos(0, 2, 0)
    );

    private static final List<BlockPos> VALID_OFFSET_BOXES_EW = Lists.newArrayList(
            new BlockPos(0, 0, 0),
            new BlockPos(0, 0, 1),
            new BlockPos(0, 0, -1),
            new BlockPos(0, 1, 1),
            new BlockPos(0, 1, -1),
            new BlockPos(0, 1, 0),
            new BlockPos(0, 2, 0)
    );

    public EndPirateAnchorBlockEntity(BlockPos pos, BlockState state) {
        super(AMBlockEntityRegistry.END_PIRATE_ANCHOR.get(), pos, state);
    }

    @Environment(EnvType.CLIENT)
    public Box getRenderBoundingBox() {
        return new Box(pos.add(-1, 0, -1), pos.add(1, 3, 1));
    }

    public static List<BlockPos> getValidBBPositions(boolean eastOrWest){
        return eastOrWest ? VALID_OFFSET_BOXES_EW : VALID_OFFSET_BOXES_NS;
    }

    public boolean hasAllAnchorBlocks() {
        for(var pos : EndPirateAnchorBlockEntity.getValidBBPositions(this.getCachedState().get(EndPirateAnchorBlock.EASTORWEST))){
            if(!(getWorld().getBlockState(this.getPos().add(pos)).getBlock() instanceof EndPirateAnchorBlock)){
                return false;
            }
        }
        return true;
    }
}
