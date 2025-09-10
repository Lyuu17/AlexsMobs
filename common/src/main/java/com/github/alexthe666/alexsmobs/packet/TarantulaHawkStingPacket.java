package com.github.alexthe666.alexsmobs.packet;

import com.github.alexthe666.alexsmobs.entity.EntityTarantulaHawk;
import com.github.alexthe666.alexsmobs.registry.AMEffectRegistry;
import com.github.alexthe666.alexsmobs.registry.AMPacketRegistry;
import dev.architectury.networking.NetworkManager;
import net.fabricmc.api.EnvType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.network.PacketByteBuf;

import java.util.function.Supplier;

public class TarantulaHawkStingPacket {

    public int hawk;
    public int spider;

    public TarantulaHawkStingPacket(int rider, int mount) {
        this.hawk = rider;
        this.spider = mount;
    }

    private static TarantulaHawkStingPacket read(PacketByteBuf buf) {
        return new TarantulaHawkStingPacket(buf.readInt(), buf.readInt());
    }

    private void write(PacketByteBuf buf) {
        buf.writeInt(hawk);
        buf.writeInt(spider);
    }

    private void handle(Supplier<NetworkManager.PacketContext> context) {
        var player = context.get().getPlayer();
        if (context.get().getEnvironment().toPlatform().equals(EnvType.CLIENT)) {
            player = MinecraftClient.getInstance().player;
        }

        if (player != null) {
            if (player.getWorld() != null) {
                var entity = player.getWorld().getEntityById(this.hawk);
                var spider = player.getWorld().getEntityById(this.spider);
                if (entity instanceof EntityTarantulaHawk && spider instanceof LivingEntity && ((LivingEntity) spider).getGroup() == EntityGroup.ARTHROPOD) {
                    ((LivingEntity) spider).addStatusEffect(new StatusEffectInstance(AMEffectRegistry.DEBILITATING_STING.get(), EntityTarantulaHawk.STING_DURATION));
                }
            }
        }
    }

    public static void register() {
        AMPacketRegistry.CHANNEL.register(TarantulaHawkStingPacket.class,
                TarantulaHawkStingPacket::write,
                TarantulaHawkStingPacket::read,
                TarantulaHawkStingPacket::handle
        );
    }
}