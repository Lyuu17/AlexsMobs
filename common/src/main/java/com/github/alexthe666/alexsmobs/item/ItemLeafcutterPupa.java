package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.block.entity.LeafcutterAnthillBlockEntity;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.EntityLeafcutterAnt;
import com.github.alexthe666.alexsmobs.registry.AMBlockRegistry;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

public class ItemLeafcutterPupa extends Item {

    public ItemLeafcutterPupa(Settings props) {
        super(props);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos blockpos = context.getBlockPos();
        var blockstate = world.getBlockState(blockpos);
        if (blockstate.isIn(AMTagRegistry.LEAFCUTTER_PUPA_USABLE_ON) && world.getBlockState(blockpos.down()).isIn(AMTagRegistry.LEAFCUTTER_PUPA_USABLE_ON)) {
            var playerentity = context.getPlayer();
            if(playerentity != null){
                playerentity.emitGameEvent(GameEvent.BLOCK_PLACE);
            }
            world.playSound(playerentity, blockpos, SoundEvents.ITEM_AXE_STRIP, SoundCategory.BLOCKS, 1.0F, 1.0F);
            if (!world.isClient) {
                world.setBlockState(blockpos, AMBlockRegistry.LEAFCUTTER_ANTHILL.get().getDefaultState(), 11);
                world.setBlockState(blockpos.down(), AMBlockRegistry.LEAFCUTTER_ANT_CHAMBER.get().getDefaultState(), 11);
                var tileentity = world.getBlockEntity(blockpos);
                if (tileentity instanceof LeafcutterAnthillBlockEntity beehivetileentity) {
                    int j = Math.min(3, AMConfig.leafcutterAntColonySize);
                    for(int k = 0; k < j; ++k) {
                        EntityLeafcutterAnt beeentity = new EntityLeafcutterAnt(AMEntityRegistry.LEAFCUTTER_ANT.get(), world);
                        beeentity.setQueen(k == 0);
                        beehivetileentity.tryEnterHive(beeentity, false, 100);
                    }
                }
                if (playerentity != null && !playerentity.isCreative()) {
                    context.getStack().decrement(1);
                }
            }

            return ActionResult.success(world.isClient);
        } else {
            return ActionResult.PASS;
        }
    }
}
