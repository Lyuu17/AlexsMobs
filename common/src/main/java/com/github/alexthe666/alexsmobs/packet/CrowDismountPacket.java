package com.github.alexthe666.alexsmobs.packet;

import com.github.alexthe666.alexsmobs.entity.EntityCrow;
import com.github.alexthe666.alexsmobs.registry.AMPacketRegistry;
import dev.architectury.networking.NetworkManager;
import net.fabricmc.api.EnvType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;

import java.util.function.Supplier;

public class CrowDismountPacket {

    public int rider;
    public int mount;

    public CrowDismountPacket(int rider, int mount) {
        this.rider = rider;
        this.mount = mount;
    }

    private static CrowDismountPacket read(PacketByteBuf buf) {
        return new CrowDismountPacket(buf.readInt(), buf.readInt());
    }

    private void write(PacketByteBuf buf) {
        buf.writeInt(rider);
        buf.writeInt(mount);
    }

    private void handle(Supplier<NetworkManager.PacketContext> context) {
        var player = context.get().getPlayer();
        if (context.get().getEnvironment().toPlatform().equals(EnvType.CLIENT)) {
            player = MinecraftClient.getInstance().player;
        }

        if (player != null) {
            if (player.getWorld() != null) {
                var entity = player.getWorld().getEntityById(this.rider);
                var mountEntity = player.getWorld().getEntityById(this.mount);
                if (entity instanceof EntityCrow && mountEntity != null) {
                    entity.stopRiding();
                }
            }
        }
    }

    public static void register() {
        AMPacketRegistry.CHANNEL.register(CrowDismountPacket.class,
                CrowDismountPacket::write,
                CrowDismountPacket::read,
                CrowDismountPacket::handle
        );
    }
}