package com.github.alexthe666.alexsmobs.fabric;

import com.github.alexthe666.alexsmobs.AlexsMobsClient;
import net.fabricmc.api.ClientModInitializer;

public final class AlexsMobsFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        AlexsMobsClient.init();
    }
}