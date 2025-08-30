package com.github.alexthe666.alexsmobs.item;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;

public class BlockItemAMRender extends AMBlockItem {

    public BlockItemAMRender(RegistrySupplier<Block> blockSupplier, BlockItem.Settings props) {
        super(blockSupplier, props);
    }

    //FIXME forge
//    @Override
//    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
//        consumer.accept((IClientItemExtensions) AlexsMobs.PROXY.getISTERProperties());
//    }
}
