package com.github.alexthe666.alexsmobs.packet;

import com.github.alexthe666.alexsmobs.block.entity.CapsidBlockEntity;
import com.github.alexthe666.alexsmobs.registry.AMPacketRegistry;
import dev.architectury.networking.NetworkManager;
import net.fabricmc.api.EnvType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;

import java.util.function.Supplier;

public class UpdateCapsidPacket {

    public long blockPos;
    public ItemStack heldStack;

    public UpdateCapsidPacket(long blockPos, ItemStack heldStack) {
        this.blockPos = blockPos;
        this.heldStack = heldStack;
    }

    private static UpdateCapsidPacket read(PacketByteBuf buf) {
        return new UpdateCapsidPacket(buf.readInt(), buf.readItemStack());
    }

    private void write(PacketByteBuf buf) {
        buf.writeLong(this.blockPos);
        buf.writeItemStack(this.heldStack);
    }

    private void handle(Supplier<NetworkManager.PacketContext> context) {
        var player = context.get().getPlayer();
        if (context.get().getEnvironment().toPlatform().equals(EnvType.CLIENT)) {
            player = MinecraftClient.getInstance().player;
        }

        if (player != null) {
            if (player.getWorld() != null) {
                BlockPos pos = BlockPos.fromLong(this.blockPos);
                if (player.getWorld().getBlockEntity(pos) != null) {
                    if (player.getWorld().getBlockEntity(pos) instanceof CapsidBlockEntity podium) {
                        podium.setStack(0, this.heldStack);
                    }
                }
            }
        }
    }

    public static void register() {
        AMPacketRegistry.CHANNEL.register(UpdateCapsidPacket.class,
                UpdateCapsidPacket::write,
                UpdateCapsidPacket::read,
                UpdateCapsidPacket::handle
        );
    }
}