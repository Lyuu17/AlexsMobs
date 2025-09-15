package com.github.alexthe666.alexsmobs;

import com.github.alexthe666.alexsmobs.registry.AMCreativeTabRegistry;
import com.github.alexthe666.alexsmobs.registry.AMMenuRegistry;
import com.github.alexthe666.alexsmobs.registry.AMModelLayerRegistry;
import com.github.alexthe666.alexsmobs.registry.AMRendererRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class AlexsMobsClient {

    public static void init() {
        AMCreativeTabRegistry.DEF_REG.register();
        AMMenuRegistry.init(false);
        AMRendererRegistry.registerEntityRenderers();
        AMRendererRegistry.registerBlockEntityRenderers();
        AMModelLayerRegistry.register();
    }

    public static void process() {

    }
}
