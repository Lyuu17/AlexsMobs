package com.github.alexthe666.alexsmobs.packet;

import com.github.alexthe666.alexsmobs.block.entity.TransmutationTableBlockEntity;
import com.github.alexthe666.alexsmobs.registry.AMPacketRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;
import java.util.function.Supplier;

public class RequestTransmutationRerollPacket {
    private final BlockPos pos;
    private final UUID uuid;

    public RequestTransmutationRerollPacket(BlockPos pos, UUID uuid) {
        this.pos = pos;
        this.uuid = uuid;
    }

    private static RequestTransmutationRerollPacket read(PacketByteBuf buf) {
        return new RequestTransmutationRerollPacket(buf.readBlockPos(), buf.readUuid());
    }

    private void write(PacketByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUuid(uuid);
    }

    private void handle(Supplier<NetworkManager.PacketContext> context) {
        var player = context.get().getPlayer();
        if (player.getWorld().isClient) {
            return;
        }

        context.get().queue(() -> {
            var be = player.getWorld().getBlockEntity(pos);
            if (be instanceof TransmutationTableBlockEntity table) {
                var targetPlayer = be.getWorld().getPlayerByUuid(uuid);
                if (targetPlayer != null) {
                    table.getWorld().playSound(null, be.getPos(), AMSoundRegistry.TRANSMUTE_ITEM.get(), SoundCategory.BLOCKS, 1F, 0.9F + player.getRandom().nextFloat() * 0.2F);
                    table.randomizeResults(pos, player);
                }
            }
        });
    }

    public static void register() {
        AMPacketRegistry.CHANNEL.register(RequestTransmutationRerollPacket.class,
                RequestTransmutationRerollPacket::write,
                RequestTransmutationRerollPacket::read,
                RequestTransmutationRerollPacket::handle
        );
    }
}
