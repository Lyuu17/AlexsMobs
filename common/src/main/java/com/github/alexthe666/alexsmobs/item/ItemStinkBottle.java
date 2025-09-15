package com.github.alexthe666.alexsmobs.item;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.block.Block;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;

public class ItemStinkBottle extends AMBlockItem {

    public ItemStinkBottle(RegistrySupplier<Block> blockSupplier, Item.Settings props) {
        super(blockSupplier, props);
    }

    @Override
    public ActionResult place(ItemPlacementContext context) {
        ActionResult result = super.place(context);
        if(result.isAccepted()){
            var bottle = new ItemStack(Items.GLASS_BOTTLE);
            if(context.getPlayer() == null){
                context.getWorld().spawnEntity(new ItemEntity(context.getWorld(),context.getBlockPos().getX() + 0.5F, context.getBlockPos().getY() + 0.5F, context.getBlockPos().getZ() + 0.5F, bottle));
            }else if(!context.getPlayer().giveItemStack(bottle)){
                context.getPlayer().dropItem(bottle, false);
            }
        }
        return result;
    }

    @Override
    public String getTranslationKey() {
        return this.getOrCreateTranslationKey();
    }
}
