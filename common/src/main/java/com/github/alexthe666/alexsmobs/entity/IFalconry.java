package com.github.alexthe666.alexsmobs.entity;


import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

public interface IFalconry {

    void onLaunch(PlayerEntity player, Entity pointedEntity);

    default int getRidingFalcons(LivingEntity player) {
        int crowCount = 0;
        for (Entity e : player.getPassengerList()) {
            if (e instanceof IFalconry) {
                crowCount++;
            }
        }
        return crowCount;
    }

    float getHandOffset();
}
