package com.github.alexthe666.alexsmobs.packet;

import com.github.alexthe666.alexsmobs.entity.EntityStraddleboard;
import com.github.alexthe666.alexsmobs.entity.IFalconry;
import com.github.alexthe666.alexsmobs.registry.AMPacketRegistry;
import dev.architectury.networking.NetworkManager;
import net.fabricmc.api.EnvType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;

import java.util.function.Supplier;

public class SyncEntityPosPacket {

    public int eagleId;
    public double posX;
    public double posY;
    public double posZ;

    public SyncEntityPosPacket(int eagleId, double posX, double posY, double posZ) {
        this.eagleId = eagleId;
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
    }

    private static SyncEntityPosPacket read(PacketByteBuf buf) {
        return new SyncEntityPosPacket(buf.readInt(), buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    private void write(PacketByteBuf buf) {
        buf.writeInt(eagleId);
        buf.writeDouble(posX);
        buf.writeDouble(posY);
        buf.writeDouble(posZ);
    }

    private void handle(Supplier<NetworkManager.PacketContext> context) {
        var player = context.get().getPlayer();
        if (context.get().getEnvironment().toPlatform().equals(EnvType.CLIENT)) {
            player = MinecraftClient.getInstance().player;
        }

        if (player != null) {
            if (player.getWorld() != null) {
                var entity = player.getWorld().getEntityById(eagleId);
                if (entity instanceof IFalconry || entity instanceof EntityStraddleboard) {
                    entity.setPos(posX, posY, posZ);
                    entity.teleport(posX, posY, posZ);
                }
            }
        }
    }

    public static void register() {
        AMPacketRegistry.CHANNEL.register(SyncEntityPosPacket.class,
                SyncEntityPosPacket::write,
                SyncEntityPosPacket::read,
                SyncEntityPosPacket::handle
        );
    }
}