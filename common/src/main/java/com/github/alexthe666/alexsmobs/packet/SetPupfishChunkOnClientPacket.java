package com.github.alexthe666.alexsmobs.packet;

import com.github.alexthe666.alexsmobs.AlexsMobsClient;
import com.github.alexthe666.alexsmobs.registry.AMPacketRegistry;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.PacketByteBuf;

import java.util.function.Supplier;

public class SetPupfishChunkOnClientPacket {

    public int chunkX;
    public int chunkZ;

    public SetPupfishChunkOnClientPacket(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    private static SetPupfishChunkOnClientPacket read(PacketByteBuf buf) {
        return new SetPupfishChunkOnClientPacket(buf.readInt(), buf.readInt());
    }

    private void write(PacketByteBuf buf) {
        buf.writeInt(chunkX);
        buf.writeInt(chunkZ);
    }

    private void handle(Supplier<NetworkManager.PacketContext> context) {
        AlexsMobsClient.setPupfishChunkForItem(this.chunkX, this.chunkZ);
    }

    public static void register() {
        AMPacketRegistry.CHANNEL.register(SetPupfishChunkOnClientPacket.class,
                SetPupfishChunkOnClientPacket::write,
                SetPupfishChunkOnClientPacket::read,
                SetPupfishChunkOnClientPacket::handle
        );
    }
}