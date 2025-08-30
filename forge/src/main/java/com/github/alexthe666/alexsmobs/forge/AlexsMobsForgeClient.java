package com.github.alexthe666.alexsmobs.forge;

import com.github.alexthe666.alexsmobs.AlexsMobsClient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class AlexsMobsForgeClient {

    @SubscribeEvent
    public static void init(FMLClientSetupEvent event) {
        event.enqueueWork(AlexsMobsClient::process);
    }
}