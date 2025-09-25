package com.github.alexthe666.alexsmobs.packet;

import com.github.alexthe666.alexsmobs.item.ILeftClick;
import com.github.alexthe666.alexsmobs.registry.AMPacketRegistry;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Hand;

import java.util.function.Supplier;

public class SwingArmPacket {

    public static final SwingArmPacket INSTANCE = new SwingArmPacket();

    private SwingArmPacket() {
    }

    private static SwingArmPacket read(PacketByteBuf buf) {
        return INSTANCE;
    }

    private void write(PacketByteBuf buf) {
    }

    private void handle(Supplier<NetworkManager.PacketContext> context) {
        var player = context.get().getPlayer();
        if (player != null) {
            var leftItem = player.getStackInHand(Hand.OFF_HAND);
            var rightItem = player.getStackInHand(Hand.MAIN_HAND);
            if(leftItem.getItem() instanceof ILeftClick){
                ((ILeftClick)leftItem.getItem()).onLeftClick(leftItem, player);
            }
            if(rightItem.getItem() instanceof ILeftClick){
                ((ILeftClick)rightItem.getItem()).onLeftClick(rightItem, player);
            }
        }
    }

    public static void register() {
        AMPacketRegistry.CHANNEL.register(SwingArmPacket.class,
                SwingArmPacket::write,
                SwingArmPacket::read,
                SwingArmPacket::handle
        );
    }

}
