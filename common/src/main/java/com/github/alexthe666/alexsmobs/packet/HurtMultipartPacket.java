package com.github.alexthe666.alexsmobs.packet;

import com.github.alexthe666.alexsmobs.entity.IHurtableMultipart;
import com.github.alexthe666.alexsmobs.entity.IMultipartEntity;
import com.github.alexthe666.alexsmobs.registry.AMPacketRegistry;
import dev.architectury.networking.NetworkManager;
import net.fabricmc.api.EnvType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import java.util.function.Supplier;

public class HurtMultipartPacket {

    public int part;
    public int parent;
    public float damage;
    public String damageType;

    public HurtMultipartPacket(int part, int parent, float damage) {
        this.part = part;
        this.parent = parent;
        this.damage = damage;
        this.damageType = "";
    }

    public HurtMultipartPacket(int part, int parent, float damage, String damageType) {
        this.part = part;
        this.parent = parent;
        this.damage = damage;
        this.damageType = damageType;
    }

    private static HurtMultipartPacket read(PacketByteBuf buf) {
        return new HurtMultipartPacket(buf.readInt(), buf.readInt(), buf.readFloat(), buf.readString());
    }

    private void write(PacketByteBuf buf) {
        buf.writeInt(part);
        buf.writeInt(parent);
        buf.writeFloat(damage);
        buf.writeString(damageType);
    }

    private void handle(Supplier<NetworkManager.PacketContext> context) {

        var player = context.get().getPlayer();
        if (context.get().getEnvironment().toPlatform().equals(EnvType.CLIENT)) {
            player = MinecraftClient.getInstance().player;
        }

        if (player != null) {
            if (player.getWorld() != null) {
                var part = player.getWorld().getEntityById(this.part);
                var parent = player.getWorld().getEntityById(this.parent);
                var registry = player.getWorld().getRegistryManager().getOptional(RegistryKeys.DAMAGE_TYPE).get();
                DamageType dmg = registry.get(new Identifier(this.damageType));
                if (dmg != null) {
                    RegistryEntry<DamageType> holder = registry.getEntry(registry.getRawId(dmg)).orElseGet(null);
                    if (holder != null) {
                        var source = new DamageSource(registry.getEntry(registry.getRawId(dmg)).get());
                        if (part instanceof IHurtableMultipart && parent instanceof LivingEntity) {
                            ((IHurtableMultipart) part).onAttackedFromServer((LivingEntity) parent, this.damage, source);
                        }
                        if (part == null && parent instanceof IMultipartEntity multipartEntity) {
                            if (multipartEntity.isMultipartEntity()) {
                                parent.damage(source, this.damage);
                            }
                        }
                    }
                }

            }
        }
    }

    public static void register() {
        AMPacketRegistry.CHANNEL.register(HurtMultipartPacket.class,
                HurtMultipartPacket::write,
                HurtMultipartPacket::read,
                HurtMultipartPacket::handle
        );
    }
}