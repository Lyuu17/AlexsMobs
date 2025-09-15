package com.github.alexthe666.alexsmobs.block;

import com.github.alexthe666.alexsmobs.entity.EntityTriops;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.FrogspawnBlock;
import net.minecraft.block.MapColor;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

public class TriopsEggsBlock extends FrogspawnBlock {
    public TriopsEggsBlock() {
        super(AbstractBlock.Settings.create()
                .mapColor(MapColor.ORANGE)
                .breakInstantly()
                .nonOpaque()
                .noCollision()
                .sounds(BlockSoundGroup.FROGSPAWN)
                .offset(OffsetType.XZ));
    }

    @Override
    public void scheduledTick(BlockState blockState, ServerWorld serverLevel, BlockPos blockPos, Random randomSource) {
        if (!this.canPlaceAt(blockState, serverLevel, blockPos)) {
            serverLevel.breakBlock(blockPos, false);
        } else if (serverLevel.getFluidState(blockPos.down()).isIn(FluidTags.WATER)) {
            serverLevel.breakBlock(blockPos, false);
            int i = 2 + randomSource.nextInt(2);
            for (int j = 1; j <= i; ++j) {
                EntityTriops tadpole = AMEntityRegistry.TRIOPS.get().create(serverLevel);
                if (tadpole != null) {
                    double d0 = blockPos.getX();
                    double d1 = blockPos.getZ();
                    int k = randomSource.nextBetweenExclusive(1, 361);
                    tadpole.refreshPositionAndAngles(d0, (double) blockPos.getY() - 0.5D, d1, (float) k, 0.0F);
                    tadpole.setPersistent();
                    tadpole.setBabyAge(-12000);
                    serverLevel.spawnEntity(tadpole);
                }
            }
        }
    }
}
