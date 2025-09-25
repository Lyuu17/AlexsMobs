package com.github.alexthe666.alexsmobs.platform;

import com.github.alexthe666.alexsmobs.item.AMBlockItem;
import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.block.Block;
import net.minecraft.item.Item;

public class PlatformRegisterItemRenderer {

    @ExpectPlatform
    public static AMBlockItem register(RegistrySupplier<Block> blockRegistrySupplier, Item.Settings settings) {
        throw new AssertionError("Platform-specific code did not load");
    }
}
