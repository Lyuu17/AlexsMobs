package com.github.alexthe666.alexsmobs.packet;

import com.github.alexthe666.alexsmobs.registry.AMPacketRegistry;
import dev.architectury.networking.NetworkManager;
import net.fabricmc.api.EnvType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Hand;

import java.util.function.Supplier;

public class InteractMultipartPacket {

    public boolean offhand;
    public int parent;

    public InteractMultipartPacket(int parent, boolean offhand) {
        this.parent = parent;
        this.offhand = offhand;
    }

    public InteractMultipartPacket() {
    }

    public static InteractMultipartPacket read(PacketByteBuf buf) {
        return new InteractMultipartPacket(buf.readInt(), buf.readBoolean());
    }

    public void write(PacketByteBuf buf) {
        buf.writeInt(parent);
        buf.writeBoolean(offhand);
    }

    private void handle(Supplier<NetworkManager.PacketContext> context) {
        var player = context.get().getPlayer();
        if (context.get().getEnvironment().toPlatform().equals(EnvType.CLIENT)) {
            player = MinecraftClient.getInstance().player;
        }

        if (player != null) {
            if (player.getWorld() != null) {
                var parent = player.getWorld().getEntityById(this.parent);
                if (player.distanceTo(parent) < 20 && parent instanceof MobEntity) {
                    player.interact(parent, this.offhand ? Hand.OFF_HAND : Hand.MAIN_HAND);
                }
            }
        }
    }

    public static void register() {
        AMPacketRegistry.CHANNEL.register(InteractMultipartPacket.class,
                InteractMultipartPacket::write,
                InteractMultipartPacket::read,
                InteractMultipartPacket::handle
        );
    }
}