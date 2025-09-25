package com.github.alexthe666.alexsmobs.mixin;

import com.github.alexthe666.alexsmobs.AlexsMobsClient;
import com.github.alexthe666.alexsmobs.client.render.entity.AMRenderLayers;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class GuiMixin {

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;getFrozenTicks()I"
            )
    )
    private void renderCustomOverlay(DrawContext context, float tickDelta, CallbackInfo ci) {
        if (AlexsMobsClient.renderStaticScreenFor <= 0) {
            return;
        }

        if (MinecraftClient.getInstance().player.isAlive() && AlexsMobsClient.lastStaticTick != MinecraftClient.getInstance().world.getTime()) {
            AlexsMobsClient.renderStaticScreenFor--;
        }
        float staticLevel = (AlexsMobsClient.renderStaticScreenFor / 60F);

        float screenWidth = context.getScaledWindowWidth();
        float screenHeight = context.getScaledWindowWidth();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        float ageInTicks = MinecraftClient.getInstance().world.getTime() + tickDelta;
        float staticIndexX = (float) Math.sin(ageInTicks * 0.2F) * 2;
        float staticIndexY = (float) Math.cos(ageInTicks * 0.2F + 3F) * 2;
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, staticLevel);
        RenderSystem.setShaderTexture(0, AMRenderLayers.STATIC_TEXTURE);
        var tesselator = Tessellator.getInstance();
        var bufferbuilder = tesselator.getBuffer();
        bufferbuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        float minU = 10 * staticIndexX * 0.125F;
        float maxU = 10 * (0.5F + staticIndexX * 0.125F);
        float minV = 10 * staticIndexY * 0.125F;
        float maxV = 10 * (0.125F + staticIndexY * 0.125F);
        bufferbuilder.vertex(0.0D, screenHeight, -190.0D).texture(minU, maxV).next();
        bufferbuilder.vertex(screenWidth, screenHeight, -190.0D).texture(maxU, maxV).next();
        bufferbuilder.vertex(screenWidth, 0.0D, -190.0D).texture(maxU, minV).next();
        bufferbuilder.vertex(0.0D, 0.0D, -190.0D).texture(minU, minV).next();
        tesselator.draw();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        AlexsMobsClient.lastStaticTick = MinecraftClient.getInstance().world.getTime();
    }
}