package com.github.alexthe666.alexsmobs.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class TransmutationTableScreen extends HandledScreen<TransmutationTableScreenHandler> {
    public static final Identifier TEXTURE = new Identifier("alexsmobs:textures/gui/transmutation_table.png");
    private final ItemStack[] transmuteStacks = new ItemStack[3];
    private int tickCount = 0;
    private TransmuteButton transmuteBtn1;
    private TransmuteButton transmuteBtn2;
    private TransmuteButton transmuteBtn3;

    public TransmutationTableScreen(TransmutationTableScreenHandler menu, PlayerInventory inventory, Text name) {
        super(menu, inventory, name);
        this.backgroundHeight = 201;
    }

    @Override
    protected void init() {
        super.init();
        int i = this.x;
        int j = this.y;
        this.addDrawableChild(transmuteBtn1 = new TransmuteButton(this, i + 30, j + 16, (button) -> {
            this.handler.onButtonClick(client.player, 0);
        }));
        this.addDrawableChild(transmuteBtn2 = new TransmuteButton(this, i + 30, j + 35, (button) -> {
            this.handler.onButtonClick(client.player, 1);
        }));
        this.addDrawableChild(transmuteBtn3 = new TransmuteButton(this, i + 30, j + 54, (button) -> {
            this.handler.onButtonClick(client.player, 2);
        }));
        transmuteBtn1.visible = false;
        transmuteBtn2.visible = false;
        transmuteBtn3.visible = false;
    }

    @Override
    public void render(DrawContext guiGraphics, int x, int y, float partialTick) {
        this.renderBackground(guiGraphics);
        this.drawBackground(guiGraphics, partialTick, x, y);
        super.render(guiGraphics, x, y, partialTick);
        this.renderItemsTransmute(guiGraphics, x, y);
        this.drawMouseoverTooltip(guiGraphics, x, y);
    }

    @Override
    protected void drawBackground(DrawContext guiGraphics, float f, int x, int y) {
        guiGraphics.drawTexture(TEXTURE, this.x, this.y, 0, 0, this.backgroundWidth, this.backgroundHeight);
    }

    @Override
    protected void handledScreenTick() {
        tickCount++;
        boolean thingIn = !this.handler.getSlot(0).getStack().isEmpty();
        transmuteBtn1.visible = !getDisplayTransmuteResult(0).isEmpty() && thingIn;
        transmuteBtn2.visible = !getDisplayTransmuteResult(1).isEmpty() && thingIn;
        transmuteBtn3.visible = !getDisplayTransmuteResult(2).isEmpty() && thingIn;
    }

    @Override
    protected void drawForeground(DrawContext guiGraphics, int x, int y) {
        this.titleX = (this.backgroundWidth - this.textRenderer.getWidth(this.title)) / 2;
        guiGraphics.drawText(this.textRenderer, this.title, this.titleX, this.titleY, 0X4EFF21, false);
    }

    protected void renderItemsTransmute(DrawContext guiGraphics, int x, int y) {
        if (!this.handler.getSlot(0).getStack().isEmpty()) {
            guiGraphics.drawItem(getDisplayTransmuteResult(0), this.x + 31, this.y + 17);
            guiGraphics.drawItem(getDisplayTransmuteResult(1), this.x + 31, this.y + 36);
            guiGraphics.drawItem(getDisplayTransmuteResult(2), this.x + 31, this.y + 55);
        }
    }

    public void setDisplayTransmuteResult(int slot, ItemStack stack){
        transmuteStacks[MathHelper.clamp(slot, 0, 2)] = stack;
    }

    public ItemStack getDisplayTransmuteResult(int slot){
        ItemStack stack = transmuteStacks[MathHelper.clamp(slot, 0, 2)];
        return stack == null ? ItemStack.EMPTY : stack;
    }

}
