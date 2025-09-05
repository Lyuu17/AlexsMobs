package com.github.alexthe666.alexsmobs.registry;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.packet.*;
import dev.architectury.networking.NetworkChannel;
import net.minecraft.util.Identifier;

public class AMPacketRegistry {

    public static final NetworkChannel CHANNEL = NetworkChannel.create(new Identifier(AlexsMobs.MOD_ID, "main"));

    public static void register() {
        HurtMultipartPacket.register();
        InteractMultipartPacket.register();
        MosquitoDismountPacket.register();
        MosquitoMountPlayerPacket.register();
        SyncEntityPosPacket.register();
    }
}
