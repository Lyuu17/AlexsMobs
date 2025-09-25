package com.github.alexthe666.alexsmobs.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

public class PlatformEvent {

    @ExpectPlatform
    public static boolean getMobGriefingEvent(World world, Entity entity) {
        throw new AssertionError("Platform-specific code did not load");
    }
}
