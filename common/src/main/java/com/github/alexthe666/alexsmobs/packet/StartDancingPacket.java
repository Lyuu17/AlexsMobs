package com.github.alexthe666.alexsmobs.packet;

import com.github.alexthe666.alexsmobs.entity.IDancingMob;
import com.github.alexthe666.alexsmobs.registry.AMPacketRegistry;
import dev.architectury.networking.NetworkManager;
import net.fabricmc.api.EnvType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;

import java.util.function.Supplier;

public class StartDancingPacket {

    public int entityID;
    public boolean dance;
    public BlockPos jukeBox;

    public StartDancingPacket(int entityID, boolean dance, BlockPos jukeBox) {
        this.entityID = entityID;
        this.dance = dance;
        this.jukeBox = jukeBox;
    }

    private static StartDancingPacket read(PacketByteBuf buf) {
        return new StartDancingPacket(buf.readInt(), buf.readBoolean(), buf.readBlockPos());
    }

    private void write(PacketByteBuf buf) {
        buf.writeInt(this.entityID);
        buf.writeBoolean(this.dance);
        buf.writeBlockPos(this.jukeBox);
    }

    private void handle(Supplier<NetworkManager.PacketContext> context) {
        var player = context.get().getPlayer();
        if (context.get().getEnvironment().toPlatform().equals(EnvType.CLIENT)) {
            player = MinecraftClient.getInstance().player;
        }

        if (player != null) {
            if (player.getWorld() != null) {
                var entity = player.getWorld().getEntityById(this.entityID);
                if (entity instanceof IDancingMob dancingMob) {
                    dancingMob.setDancing(this.dance);
                    if (this.dance){
                        dancingMob.setJukeboxPos(this.jukeBox);
                    } else{
                        dancingMob.setJukeboxPos(null);
                    }
                }
            }
        }
    }

    public static void register() {
        AMPacketRegistry.CHANNEL.register(StartDancingPacket.class,
                StartDancingPacket::write,
                StartDancingPacket::read,
                StartDancingPacket::handle
        );
    }
}