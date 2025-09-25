package com.github.alexthe666.alexsmobs.platform.fabric;

import com.github.alexthe666.alexsmobs.item.AMBlockItem;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.block.Block;
import net.minecraft.item.Item;

public class PlatformRegisterItemRendererImpl {

    public static AMBlockItem register(RegistrySupplier<Block> blockRegistrySupplier, Item.Settings settings) {
        return new AMBlockItem(blockRegistrySupplier, settings);
    }
}