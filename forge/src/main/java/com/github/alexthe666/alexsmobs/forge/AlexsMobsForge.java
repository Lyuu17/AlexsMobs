package com.github.alexthe666.alexsmobs.forge;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.AlexsMobsClient;
import dev.architectury.platform.Platform;
import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(AlexsMobs.MOD_ID)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public final class AlexsMobsForge {

    public AlexsMobsForge() {
        EventBuses.registerModEventBus(AlexsMobs.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());

        AlexsMobs.init();
        if (Platform.getEnv() == Dist.CLIENT)
            AlexsMobsClient.init();
    }

    @SubscribeEvent
    public static void init(FMLCommonSetupEvent event) {
        event.enqueueWork(AlexsMobs::process);
    }
}