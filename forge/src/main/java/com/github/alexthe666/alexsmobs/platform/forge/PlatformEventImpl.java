package com.github.alexthe666.alexsmobs.platform.forge;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;

public class PlatformEventImpl {

    public static boolean getMobGriefingEvent(World world, Entity entity) {
        return ForgeEventFactory.getMobGriefingEvent(world, entity);
    }
}
