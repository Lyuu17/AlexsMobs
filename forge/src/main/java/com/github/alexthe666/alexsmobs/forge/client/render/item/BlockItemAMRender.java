package com.github.alexthe666.alexsmobs.forge.client.render.item;

import com.github.alexthe666.alexsmobs.client.render.item.AMItemstackRenderer;
import com.github.alexthe666.alexsmobs.item.AMBlockItem;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.block.Block;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.item.BlockItem;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

public class BlockItemAMRender extends AMBlockItem {

    public BlockItemAMRender(RegistrySupplier<Block> blockSupplier, BlockItem.Settings props) {
        super(blockSupplier, props);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BuiltinModelItemRenderer getCustomRenderer() {
                return new AMItemstackRenderer();
            }
        });
    }
}
