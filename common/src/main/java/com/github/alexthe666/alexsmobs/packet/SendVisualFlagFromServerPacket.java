package com.github.alexthe666.alexsmobs.packet;

import com.github.alexthe666.alexsmobs.registry.AMPacketRegistry;
import dev.architectury.networking.NetworkManager;
import net.fabricmc.api.EnvType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;

import java.util.function.Supplier;

public class SendVisualFlagFromServerPacket {

    public int entityID;
    public int flag;

    public SendVisualFlagFromServerPacket(int entityID, int flag) {
        this.entityID = entityID;
        this.flag = flag;
    }

    private static SendVisualFlagFromServerPacket read(PacketByteBuf buf) {
        return new SendVisualFlagFromServerPacket(buf.readInt(), buf.readInt());
    }

    private void write(PacketByteBuf buf) {
        buf.writeInt(entityID);
        buf.writeInt(flag);
    }

    private void handle(Supplier<NetworkManager.PacketContext> context) {
        var player = context.get().getPlayer();
        if (context.get().getEnvironment().toPlatform().equals(EnvType.CLIENT)) {
            player = MinecraftClient.getInstance().player;
        }

        if (player != null) {
            if (player.getWorld() != null) {
                //FIXME
//                var entity = player.getWorld().getEntityById(this.entityID);
//                AlexsMobs.PROXY.processVisualFlag(entity, this.flag);
            }
        }
    }

    public static void register() {
        AMPacketRegistry.CHANNEL.register(SendVisualFlagFromServerPacket.class,
                SendVisualFlagFromServerPacket::write,
                SendVisualFlagFromServerPacket::read,
                SendVisualFlagFromServerPacket::handle
        );
    }
}