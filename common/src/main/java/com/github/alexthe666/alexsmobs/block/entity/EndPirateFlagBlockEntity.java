package com.github.alexthe666.alexsmobs.block.entity;

import com.github.alexthe666.alexsmobs.registry.AMBlockEntityRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class EndPirateFlagBlockEntity extends BlockEntity {

    public int ticksExisted;

    public EndPirateFlagBlockEntity(BlockPos pos, BlockState state) {
        super(AMBlockEntityRegistry.END_PIRATE_FLAG.get(), pos, state);
    }

//    @OnlyIn(Dist.CLIENT)
//    public AABB getRenderBoundingBox() {
//        return new AABB(worldPosition.offset(-2, -2, -2), worldPosition.offset(2, 2, 2));
//    }

    public void tick(World level, BlockPos pos, BlockState state, EndPirateFlagBlockEntity entity) {
        ticksExisted++;
    }
}

