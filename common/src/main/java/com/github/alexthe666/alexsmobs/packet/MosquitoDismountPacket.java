package com.github.alexthe666.alexsmobs.packet;

import com.github.alexthe666.alexsmobs.entity.EntityBaldEagle;
import com.github.alexthe666.alexsmobs.entity.EntityCrimsonMosquito;
import com.github.alexthe666.alexsmobs.entity.EntityEnderiophage;
import com.github.alexthe666.alexsmobs.registry.AMPacketRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;

public class MosquitoDismountPacket {

    public int riderId;
    public int mountId;

    public MosquitoDismountPacket(int rider, int mount) {
        this.riderId = rider;
        this.mountId = mount;
    }

    private static MosquitoDismountPacket read(PacketByteBuf buf) {
        return new MosquitoDismountPacket(buf.readInt(), buf.readInt());
    }

    private void write(PacketByteBuf buf) {
        buf.writeInt(riderId);
        buf.writeInt(mountId);
    }

    private void handle(PlayerEntity player) {
        var rider = player.getWorld().getEntityById(riderId);
        var mount = player.getWorld().getEntityById(mountId);

        if (rider != null && mount != null && rider.hasVehicle() && mount.hasPassenger(rider)) {
            if (rider instanceof EntityCrimsonMosquito|| rider instanceof EntityBaldEagle || rider instanceof EntityEnderiophage) {
                rider.stopRiding();
            }
        }
    }

    public static void register() {
        AMPacketRegistry.CHANNEL.register(MosquitoDismountPacket.class,
                MosquitoDismountPacket::write,
                MosquitoDismountPacket::read,
                (packet, context) -> packet.handle(context.get().getPlayer())
        );
    }
}