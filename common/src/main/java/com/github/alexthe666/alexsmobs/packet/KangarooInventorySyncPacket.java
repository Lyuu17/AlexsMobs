package com.github.alexthe666.alexsmobs.packet;

import com.github.alexthe666.alexsmobs.entity.EntityKangaroo;
import com.github.alexthe666.alexsmobs.registry.AMPacketRegistry;
import dev.architectury.networking.NetworkManager;
import net.fabricmc.api.EnvType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;

import java.util.function.Supplier;

public class KangarooInventorySyncPacket {

    public int kangaroo;
    public int slotId;
    public ItemStack stack;

    public KangarooInventorySyncPacket(int kangaroo, int slotId, ItemStack stack) {
        this.kangaroo = kangaroo;
        this.slotId = slotId;
        this.stack = stack;
    }

    public static KangarooInventorySyncPacket read(PacketByteBuf buf) {
        return new KangarooInventorySyncPacket(buf.readInt(), buf.readInt(), buf.readItemStack());
    }

    private void write(PacketByteBuf buf) {
        buf.writeInt(kangaroo);
        buf.writeInt(slotId);
        buf.writeItemStack(stack);
    }

    private void handle(Supplier<NetworkManager.PacketContext> context) {
        var player = context.get().getPlayer();
        if (context.get().getEnvironment().toPlatform().equals(EnvType.CLIENT)) {
            player = MinecraftClient.getInstance().player;
        }

        if (player != null) {
            if (player.getWorld() != null) {
                var entity = player.getWorld().getEntityById(this.kangaroo);
                if (entity instanceof EntityKangaroo && ((EntityKangaroo) entity).kangarooInventory != null) {
                    if (this.slotId < 0) {

                    } else {
                        ((EntityKangaroo) entity).kangarooInventory.setStack(this.slotId, this.stack);
                    }
                }
            }
        }
    }

    public static void register() {
        AMPacketRegistry.CHANNEL.register(KangarooInventorySyncPacket.class,
                KangarooInventorySyncPacket::write,
                KangarooInventorySyncPacket::read,
                KangarooInventorySyncPacket::handle
        );
    }
}