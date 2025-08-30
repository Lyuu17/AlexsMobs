package com.github.alexthe666.alexsmobs.fabric;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import net.fabricmc.api.ModInitializer;

public final class AlexsMobsFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        AlexsMobs.init();
    }
}