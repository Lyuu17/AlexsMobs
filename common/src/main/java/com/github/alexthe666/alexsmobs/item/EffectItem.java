package com.github.alexthe666.alexsmobs.item;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.List;
import java.util.Map;

public class EffectItem extends HiddenItemCreative implements IItemRender {

    private static List<StatusEffect> mobEffectList = null;

    public EffectItem(Settings settings) {
        super(settings);
    }

    @Override
    public void render(ItemStack stack, ModelTransformationMode mode, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        // RenderSystem.enableAlphaTest();
        RenderSystem.enableDepthTest();
        StatusEffect effect;
        if (stack.getNbt() != null && stack.getNbt().contains("DisplayEffect")) {
            String displayID = stack.getNbt().getString("DisplayEffect");
            effect = Registries.STATUS_EFFECT.get(new Identifier(displayID));
        } else {
            if (mobEffectList == null) {
                mobEffectList = Registries.STATUS_EFFECT.getEntrySet().stream().map(Map.Entry::getValue).toList();
            }
            int size = mobEffectList.size();
            int time = (int) (Util.getMeasuringTimeMs() / 500);
            effect = mobEffectList.get(time % size);
            if (effect == null) {
                effect = StatusEffects.SPEED;
            }
        }
        if (effect == null) {
            effect = StatusEffects.SPEED;
        }
        var potionspriteuploader = MinecraftClient.getInstance().getStatusEffectSpriteManager();
        matrices.push();
        matrices.translate(0, 0, 0.5F);
        var sprite = potionspriteuploader.getSprite(effect);
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, sprite.getAtlasId());
        var tessellator = Tessellator.getInstance();
        var bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR_LIGHT);
        var mx = matrices.peek().getPositionMatrix();
        int br = 255;
        bufferbuilder.vertex(mx, (float) 1, (float) 1, (float) 0).texture(sprite.getMaxU(), sprite.getMinV()).color(br, br, br, 255).light(light).next();
        bufferbuilder.vertex(mx, (float) 0, (float) 1, (float) 0).texture(sprite.getMinU(), sprite.getMinV()).color(br, br, br, 255).light(light).next();
        bufferbuilder.vertex(mx, (float) 0, (float) 0, (float) 0).texture(sprite.getMinU(), sprite.getMaxV()).color(br, br, br, 255).light(light).next();
        bufferbuilder.vertex(mx, (float) 1, (float) 0, (float) 0).texture(sprite.getMaxU(), sprite.getMaxV()).color(br, br, br, 255).light(light).next();
        tessellator.draw();
        matrices.pop();
    }
}