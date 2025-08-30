package com.github.alexthe666.alexsmobs;

import com.github.alexthe666.alexsmobs.registry.AMRendererRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class AlexsMobsClient {

    public static void init() {
        AMRendererRegistry.registerEntityRenderers();
    }

    public static void process() {

    }
}
