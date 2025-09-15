package com.github.alexthe666.alexsmobs.registry;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.screen.TransmutationTableScreen;
import com.github.alexthe666.alexsmobs.screen.TransmutationTableScreenHandler;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public class AMMenuRegistry {

    public static final ScreenHandlerType<TransmutationTableScreenHandler> TRANSMUTATION_TABLE = MenuRegistry.ofExtended(TransmutationTableScreenHandler::new);

    private static <H extends ScreenHandler, S extends Screen & ScreenHandlerProvider<H>> void register(boolean serverInit, String id, ScreenHandlerType<? extends H> type, MenuRegistry.ScreenFactory<H, S> factory) {
        if (serverInit) {
            Registry.register(Registries.SCREEN_HANDLER, new Identifier(AlexsMobs.MOD_ID, id), type);
        } else {
            MenuRegistry.registerScreenFactory(type, factory);
        }
    }

    public static void init(boolean serverInit) {
        register(serverInit, "transmutation_table", TRANSMUTATION_TABLE, TransmutationTableScreen::new);
    }
}
