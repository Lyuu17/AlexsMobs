package com.github.alexthe666.alexsmobs.world.gen.feature;

import com.github.alexthe666.alexsmobs.block.entity.LeafcutterAnthillBlockEntity;
import com.github.alexthe666.alexsmobs.entity.EntityLeafcutterAnt;
import com.github.alexthe666.alexsmobs.registry.AMBlockRegistry;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.mojang.serialization.Codec;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

import java.util.Random;

public class FeatureLeafcutterAnthill extends Feature<DefaultFeatureConfig> {

    public FeatureLeafcutterAnthill(Codec<DefaultFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        if (context.getWorld().getRandom().nextFloat() > 0.0175F) {
            return false;
        }
        int x = 8;
        int z = 8;
        var pos = context.getOrigin();
        int y = context.getWorld().getTopY(Heightmap.Type.WORLD_SURFACE_WG, pos.getX() + x, pos.getZ() + z);
        var heightPos = new BlockPos(pos.getX() + x, y, pos.getZ() + z);
        if(!context.getWorld().getFluidState(heightPos.down()).isEmpty()){
            return false;
        }
        int outOfGround = 2 + context.getWorld().getRandom().nextInt(2);
        for (int i = 0; i < outOfGround; i++) {
            float size = outOfGround - i;
            int lvt_8_1_ = (int) (Math.floor(size) * context.getWorld().getRandom().nextFloat()) + 2;
            int lvt_10_1_ = (int) (Math.floor(size) * context.getWorld().getRandom().nextFloat()) + 2;
            float radius = (float) (lvt_8_1_ + lvt_10_1_) * 0.333F;
            for (var lvt_13_1_ : BlockPos.iterate(heightPos.add(-lvt_8_1_, 0, -lvt_10_1_), heightPos.add(lvt_8_1_, 3, lvt_10_1_))) {
                if (lvt_13_1_.getSquaredDistance(heightPos) <= (double) (radius * radius)) {
                    var block = Blocks.COARSE_DIRT.getDefaultState();
                    if (context.getWorld().getRandom().nextFloat() < 0.2F) {
                        block = Blocks.DIRT.getDefaultState();
                    }
                    context.getWorld().setBlockState(lvt_13_1_, block, 4);
                }
            }
        }
        var chunkSeedRandom = new Random(pos.asLong());
        outOfGround -= chunkSeedRandom.nextInt(1) + 1;
        heightPos = heightPos.add(-chunkSeedRandom.nextInt(2), 0, -chunkSeedRandom.nextInt(2));
        if (context.getWorld().getBlockState(heightPos.up(outOfGround + 1)).getBlock() != AMBlockRegistry.LEAFCUTTER_ANTHILL.get() && context.getWorld().getBlockState(heightPos.up(outOfGround - 1)).getBlock() != AMBlockRegistry.LEAFCUTTER_ANTHILL.get()) {
            context.getWorld().setBlockState(heightPos.up(outOfGround), AMBlockRegistry.LEAFCUTTER_ANTHILL.get().getDefaultState(), 4);
            var tileentity = context.getWorld().getBlockEntity(heightPos.up(outOfGround));
            if (tileentity instanceof LeafcutterAnthillBlockEntity beehivetileentity) {
                int j = 3 + chunkSeedRandom.nextInt(3);
                if(beehivetileentity.hasNoAnts()){
                    for(int k = 0; k < j; ++k) {
                        var beeentity = new EntityLeafcutterAnt(AMEntityRegistry.LEAFCUTTER_ANT.get(), context.getWorld().toServerWorld());
                        beeentity.setQueen(k == 0);
                        beehivetileentity.tryEnterHive(beeentity, false, context.getWorld().getRandom().nextInt(599));
                    }
                }
            }
            if(context.getWorld().getRandom().nextBoolean()){
                context.getWorld().setBlockState(heightPos.up(outOfGround).north(), Blocks.COARSE_DIRT.getDefaultState(), 4);
                context.getWorld().setBlockState(heightPos.up(outOfGround - 1).north(), Blocks.COARSE_DIRT.getDefaultState(), 4);
                context.getWorld().setBlockState(heightPos.up(outOfGround - 2).north(), Blocks.COARSE_DIRT.getDefaultState(), 4);
            }
            if(context.getWorld().getRandom().nextBoolean()){
                context.getWorld().setBlockState(heightPos.up(outOfGround).east(), Blocks.COARSE_DIRT.getDefaultState(), 4);
                context.getWorld().setBlockState(heightPos.up(outOfGround - 1).east(), Blocks.COARSE_DIRT.getDefaultState(), 4);
                context.getWorld().setBlockState(heightPos.up(outOfGround - 2).east(), Blocks.COARSE_DIRT.getDefaultState(), 4);
            }
            if(context.getWorld().getRandom().nextBoolean()){
                context.getWorld().setBlockState(heightPos.up(outOfGround).south(), Blocks.COARSE_DIRT.getDefaultState(), 4);
                context.getWorld().setBlockState(heightPos.up(outOfGround - 1).south(), Blocks.COARSE_DIRT.getDefaultState(), 4);
                context.getWorld().setBlockState(heightPos.up(outOfGround - 2).south(), Blocks.COARSE_DIRT.getDefaultState(), 4);
            }
            if(context.getWorld().getRandom().nextBoolean()){
                context.getWorld().setBlockState(heightPos.up(outOfGround).west(), Blocks.COARSE_DIRT.getDefaultState(), 4);
                context.getWorld().setBlockState(heightPos.up(outOfGround - 1).west(), Blocks.COARSE_DIRT.getDefaultState(), 4);
                context.getWorld().setBlockState(heightPos.up(outOfGround - 2).west(), Blocks.COARSE_DIRT.getDefaultState(), 4);
            }
            for(int airs = 1; airs < 3; airs++){
                context.getWorld().setBlockState(heightPos.up(outOfGround + airs), Blocks.AIR.getDefaultState(), 4);
            }
        }
        int i = outOfGround;
        int down = context.getWorld().getRandom().nextInt(2) + 1;
        while (i > -down) {
            i--;
            context.getWorld().setBlockState(heightPos.up(i), AMBlockRegistry.LEAFCUTTER_ANT_CHAMBER.get().getDefaultState(), 4);
        }
        float size = chunkSeedRandom.nextInt(1) + 1;
        int lvt_8_1_ = (int) (Math.floor(size) * context.getWorld().getRandom().nextFloat()) + 1;
        int lvt_9_1_ = (int) (Math.floor(size) * context.getWorld().getRandom().nextFloat()) + 1;
        int lvt_10_1_ = (int) (Math.floor(size) * context.getWorld().getRandom().nextFloat()) + 1;
        float radius = (float) (lvt_8_1_ + lvt_9_1_ + lvt_10_1_) * 0.333F + 0.5F;
        heightPos = heightPos.down(down + lvt_9_1_).add(chunkSeedRandom.nextInt(2), 0, chunkSeedRandom.nextInt(2));
        for (BlockPos lvt_13_1_ : BlockPos.iterate(heightPos.add(-lvt_8_1_, -lvt_9_1_, -lvt_10_1_), heightPos.add(lvt_8_1_, lvt_9_1_, lvt_10_1_))) {
            if (lvt_13_1_.getSquaredDistance(heightPos) < (double) (radius * radius)) {
                var block = AMBlockRegistry.LEAFCUTTER_ANT_CHAMBER.get().getDefaultState();
                context.getWorld().setBlockState(lvt_13_1_, block, 4);
            }
        }
        return true;
    }
}