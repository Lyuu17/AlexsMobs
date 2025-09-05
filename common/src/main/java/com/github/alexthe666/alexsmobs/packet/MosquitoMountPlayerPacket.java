package com.github.alexthe666.alexsmobs.packet;

import com.github.alexthe666.alexsmobs.entity.EntityCrimsonMosquito;
import com.github.alexthe666.alexsmobs.registry.AMPacketRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;

public class MosquitoMountPlayerPacket {

    public int riderId;
    public int mountId;

    public MosquitoMountPlayerPacket(int rider, int mount) {
        this.riderId = rider;
        this.mountId = mount;
    }

    private static MosquitoMountPlayerPacket read(PacketByteBuf buf) {
        return new MosquitoMountPlayerPacket(buf.readInt(), buf.readInt());
    }

    private void write(PacketByteBuf buf) {
        buf.writeInt(riderId);
        buf.writeInt(mountId);
    }

    private void handle(PlayerEntity player) {
        if (player.getWorld() != null) {
            var entity = player.getWorld().getEntityById(riderId);
            var mountEntity = player.getWorld().getEntityById(mountId);
            //TODO
            if ((entity instanceof EntityCrimsonMosquito /*|| entity instanceof EntityEnderiophage || entity instanceof EntityBaldEagle*/) && mountEntity instanceof PlayerEntity && entity.distanceTo(mountEntity) < 16D) {
                entity.startRiding(mountEntity, true);
            }
        }
    }

    public static void register() {
        AMPacketRegistry.CHANNEL.register(MosquitoMountPlayerPacket.class,
                MosquitoMountPlayerPacket::write,
                MosquitoMountPlayerPacket::read,
                (packet, context) -> packet.handle(context.get().getPlayer())
        );
    }
}