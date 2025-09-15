package com.github.alexthe666.alexsmobs.packet;

import com.github.alexthe666.alexsmobs.entity.EntityKangaroo;
import com.github.alexthe666.alexsmobs.registry.AMPacketRegistry;
import dev.architectury.networking.NetworkManager;
import net.fabricmc.api.EnvType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;

import java.util.function.Supplier;

public class KangarooEatPacket {

    public int kangaroo;
    public ItemStack stack;

    public KangarooEatPacket(int kangaroo, ItemStack stack) {
        this.kangaroo = kangaroo;
        this.stack = stack;
    }

    private static KangarooEatPacket read(PacketByteBuf buf) {
        return new KangarooEatPacket(buf.readInt(), buf.readItemStack());
    }

    private void write(PacketByteBuf buf) {
        buf.writeInt(kangaroo);
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
                if (entity instanceof EntityKangaroo kangaroo && ((EntityKangaroo) entity).kangarooInventory != null) {
                    for (int i = 0; i < 7; i++) {
                        double d2 = kangaroo.getRandom().nextGaussian() * 0.02D;
                        double d0 = kangaroo.getRandom().nextGaussian() * 0.02D;
                        double d1 = kangaroo.getRandom().nextGaussian() * 0.02D;
                        entity.getWorld().addParticle(new ItemStackParticleEffect(ParticleTypes.ITEM, this.stack), entity.getX() + (double) (kangaroo.getRandom().nextFloat() * entity.getWidth()) - (double) entity.getWidth() * 0.5F, entity.getY() + entity.getHeight() * 0.5F + (double) (kangaroo.getRandom().nextFloat() * entity.getHeight() * 0.5F), entity.getZ() + (double) (kangaroo.getRandom().nextFloat() * entity.getWidth()) - (double) entity.getWidth() * 0.5F, d0, d1, d2);
                    }
                }
            }
        }
    }

    public static void register() {
        AMPacketRegistry.CHANNEL.register(KangarooEatPacket.class,
                KangarooEatPacket::write,
                KangarooEatPacket::read,
                KangarooEatPacket::handle
        );
    }
}