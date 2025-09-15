package com.github.alexthe666.alexsmobs.packet;

import com.github.alexthe666.alexsmobs.registry.AMPacketRegistry;
import com.github.alexthe666.alexsmobs.screen.TransmutationTableScreenHandler;
import dev.architectury.networking.NetworkManager;
import net.fabricmc.api.EnvType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;

import java.util.function.Supplier;

public class TransmuteFromMenuPacket {

    private int playerId;
    private int choice;

    public TransmuteFromMenuPacket(int playerId, int choice) {
        this.playerId = playerId;
        this.choice = choice;
    }

    public static TransmuteFromMenuPacket read(PacketByteBuf buf) {
        return new TransmuteFromMenuPacket(buf.readInt(), buf.readInt());
    }

    private void write(PacketByteBuf buf) {
        buf.writeInt(playerId);
        buf.writeInt(choice);
    }

    private void handle(Supplier<NetworkManager.PacketContext> context) {
        var player = context.get().getPlayer();
        if (context.get().getEnvironment().toPlatform().equals(EnvType.CLIENT)) {
            player = MinecraftClient.getInstance().player;
        }

        if (player.getId() == this.playerId && player.currentScreenHandler instanceof TransmutationTableScreenHandler table) {
            table.transmute(player, this.choice);
        }
    }

    public static void register() {
        AMPacketRegistry.CHANNEL.register(TransmuteFromMenuPacket.class,
                TransmuteFromMenuPacket::write,
                TransmuteFromMenuPacket::read,
                TransmuteFromMenuPacket::handle
        );
    }

}