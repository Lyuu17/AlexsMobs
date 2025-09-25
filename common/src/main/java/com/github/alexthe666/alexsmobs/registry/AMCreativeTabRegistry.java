package com.github.alexthe666.alexsmobs.registry;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.item.CustomTabBehavior;
import com.github.alexthe666.alexsmobs.item.HiddenItemCreative;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;

public class AMCreativeTabRegistry {


    public static final DeferredRegister<ItemGroup> DEF_REG = DeferredRegister.create(AlexsMobs.MOD_ID, RegistryKeys.ITEM_GROUP);

    public static final RegistrySupplier<ItemGroup> TAB = DEF_REG.register("alexsmobs_tab",
            () -> ItemGroup.create(ItemGroup.Row.TOP, 1)
                    .displayName(Text.translatable("itemGroup." + AlexsMobs.MOD_ID))
//                    .withTabsBefore(ItemGroups.SPAWN_EGGS)
                    .icon(() -> new ItemStack(AMItemRegistry.TAB_ICON.get()))
                    .entries((enabledFeatures, output) -> {
                        for (var item : AMItemRegistry.DEF_REG) {
                            if (item.get() instanceof HiddenItemCreative) {
                                continue;
                            }

                            if (item.get() instanceof CustomTabBehavior customTabBehavior) {
                                customTabBehavior.fillItemCategory(output);
                            } else {
                                output.add(item.get());
                            }
                        }
                    })
                    .build());
}
