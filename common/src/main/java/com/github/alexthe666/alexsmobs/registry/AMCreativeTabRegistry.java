package com.github.alexthe666.alexsmobs.registry;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class AMCreativeTabRegistry {


    public static final DeferredRegister<CreativeModeTab> DEF_REG = DeferredRegister.create(AlexsMobs.MOD_ID, Registries.CREATIVE_MODE_TAB);

    public static final RegistrySupplier<CreativeModeTab> TAB = DEF_REG.register("alexsmobs_tab",
            () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 1)
                    .title(Component.translatable("itemGroup." + AlexsMobs.MOD_ID))
//                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> new ItemStack(AMItemRegistry.TAB_ICON.get()))
                    .displayItems((enabledFeatures, output) -> {
                        for(RegistryObject<Item> item : AMItemRegistry.DEF_REG.getEntries()){
                            if(item.get() instanceof CustomTabBehavior customTabBehavior){
                                customTabBehavior.fillItemCategory(output);
                            }else{
                                output.accept(item.get());
                            }
                        }
                    })
                    .build());
}
