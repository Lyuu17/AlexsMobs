package com.github.alexthe666.alexsmobs.event;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.client.render.item.AMItemstackRenderer;
import com.github.alexthe666.alexsmobs.item.ILeftClick;
import com.github.alexthe666.alexsmobs.packet.SwingArmPacket;
import dev.architectury.event.events.client.ClientTooltipEvent;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ClientEvents {

    @Environment(EnvType.CLIENT)
    public static void init() {

        TickEvent.PLAYER_POST.register(instance -> {
            AMItemstackRenderer.incrementTick();
        });

        ClientTooltipEvent.ITEM.register((stack, lines, flag) -> {
            var tag = stack.getNbt();
            if (tag != null && tag.contains("BisonFur") && tag.getBoolean("BisonFur")) {
                lines.add(Text.translatable("item.alexsmobs.insulated_with_fur").formatted(Formatting.AQUA));
            }
        });

        InteractionEvent.CLIENT_LEFT_CLICK_AIR.register((player, hand) -> {
            boolean flag = false;
            var leftItem = player.getOffHandStack();
            var rightItem = player.getMainHandStack();
            if(leftItem.getItem() instanceof final ILeftClick iLeftClick){
                iLeftClick.onLeftClick(leftItem, player);
                flag = true;
            }
            if(rightItem.getItem() instanceof final ILeftClick iLeftClick){
                iLeftClick.onLeftClick(rightItem, player);
                flag = true;
            }
            if (flag && player.getWorld().isClient) {
                AlexsMobs.sendMSGToServer(SwingArmPacket.INSTANCE);
            }
        });
    }
}
