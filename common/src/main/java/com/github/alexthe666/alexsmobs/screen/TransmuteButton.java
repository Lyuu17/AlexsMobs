package com.github.alexthe666.alexsmobs.screen;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

public class TransmuteButton extends ButtonWidget {
    private final Screen parent;

    public TransmuteButton(Screen parent, int x, int y, PressAction onPress) {
        super(x, y, 117, 19, ScreenTexts.EMPTY, onPress, ButtonWidget.DEFAULT_NARRATION_SUPPLIER);
        this.parent = parent;
    }

    @Override
    public void renderButton(DrawContext guiGraphics, int x, int y, float partialTick) {
        int color = 8453920;
        int cost = AMConfig.transmutingExperienceCost;
        if(!canBeTransmuted(cost)){
            color = 16736352;
        }else if (this.active && this.isHovered()) {
            guiGraphics.drawTexture(TransmutationTableScreen.TEXTURE, this.getX(), this.getY(), 0, 201, 117, 19);
            color = 0XC7FFD0;
        }
        guiGraphics.getMatrices().push();
        guiGraphics.drawText(MinecraftClient.getInstance().textRenderer, Text.translatable("alexsmobs.container.transmutation_table.cost").append(" " + cost), this.getX() + 21, this.getY() + (this.height - 8) / 2, color, false);
        guiGraphics.getMatrices().pop();
    }

    public boolean canBeTransmuted(int cost){
        return MinecraftClient.getInstance().player.experienceLevel >= cost || MinecraftClient.getInstance().player.getAbilities().creativeMode;
    }

    @Override
    public void playDownSound(SoundManager sounds) {
        if(canBeTransmuted(AMConfig.transmutingExperienceCost)){
            super.playDownSound(sounds);
        }
    }

    @Override
    public void onPress() {
        if(canBeTransmuted(AMConfig.transmutingExperienceCost)){
            super.onPress();
        }
        this.hovered = false;
        this.setFocused(false);
    }
}