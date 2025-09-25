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

    public BlockPos blockPos;
    public ItemStack heldStack;

    public UpdateCapsidPacket(BlockPos blockPos, ItemStack heldStack) {
        this.blockPos = blockPos;
        this.heldStack = heldStack;
    }

    private static UpdateCapsidPacket read(PacketByteBuf buf) {
        return new UpdateCapsidPacket(buf.readBlockPos(), buf.readItemStack());
    }

    private void write(PacketByteBuf buf) {
        buf.writeBlockPos(this.blockPos);
        buf.writeItemStack(this.heldStack);
    }

    private void handle(Supplier<NetworkManager.PacketContext> context) {
        var player = context.get().getPlayer();
        if (context.get().getEnvironment().toPlatform().equals(EnvType.CLIENT)) {
            player = MinecraftClient.getInstance().player;
        }

        if (player != null) {
            if (player.getWorld() != null) {
                if (player.getWorld().getBlockEntity(this.blockPos) != null) {
                    if (player.getWorld().getBlockEntity(this.blockPos) instanceof CapsidBlockEntity podium) {
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