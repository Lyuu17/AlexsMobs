package com.github.alexthe666.alexsmobs.packet;

import com.github.alexthe666.alexsmobs.registry.AMPacketRegistry;
import com.github.alexthe666.alexsmobs.screen.TransmutationTableScreen;
import dev.architectury.networking.NetworkManager;
import net.fabricmc.api.EnvType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class UpdateTransmutablesToDisplayPacket {

    private int playerId;
    public ItemStack stack1;
    public ItemStack stack2;
    public ItemStack stack3;

    public UpdateTransmutablesToDisplayPacket(int playerId, @NotNull ItemStack stack1, @NotNull ItemStack stack2, @NotNull ItemStack stack3) {
        this.stack1 = stack1;
        this.stack2 = stack2;
        this.stack3 = stack3;
        this.playerId = playerId;
    }

    public static UpdateTransmutablesToDisplayPacket read(PacketByteBuf buf) {
        return new UpdateTransmutablesToDisplayPacket(buf.readInt(), buf.readItemStack(), buf.readItemStack(), buf.readItemStack());
    }

    private void write(PacketByteBuf buf) {
        buf.writeInt(playerId);
        buf.writeItemStack(stack1);
        buf.writeItemStack(stack2);
        buf.writeItemStack(stack3);
    }

    private void handle(Supplier<NetworkManager.PacketContext> context) {
        var player = context.get().getPlayer();
        if (context.get().getEnvironment().toPlatform().equals(EnvType.CLIENT)) {
            player = MinecraftClient.getInstance().player;
        }

        if (player.getId() == this.playerId) {
            if (MinecraftClient.getInstance().currentScreen instanceof TransmutationTableScreen transmutationTableScreen) {
                transmutationTableScreen.setDisplayTransmuteResult(0, this.stack1);
                transmutationTableScreen.setDisplayTransmuteResult(1, this.stack2);
                transmutationTableScreen.setDisplayTransmuteResult(2, this.stack3);
            }
        }
    }

    public static void register() {
        AMPacketRegistry.CHANNEL.register(UpdateTransmutablesToDisplayPacket.class,
                UpdateTransmutablesToDisplayPacket::write,
                UpdateTransmutablesToDisplayPacket::read,
                UpdateTransmutablesToDisplayPacket::handle
        );
    }
}