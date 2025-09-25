package com.github.alexthe666.alexsmobs.platform.fabric;

import net.minecraft.entity.Entity;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;

public class PlatformEventImpl {

    public static boolean getMobGriefingEvent(World world, Entity entity) {
        return world.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING);
    }
}
