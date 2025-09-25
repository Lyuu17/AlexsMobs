package com.github.alexthe666.alexsmobs.client.render.entity;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;

public class RenderNothing extends LivingEntityRenderer {

    public RenderNothing(EntityRendererFactory.Context context) {
        super(context, null, 0);
    }

    @Override
    public void render(LivingEntity entity, float f, float f1, MatrixStack stack, VertexConsumerProvider buf, int i) {
    }

    @Override
    protected boolean hasLabel(LivingEntity entity) {
        return super.hasLabel(entity) && (entity.shouldRenderName() || entity.hasCustomName() && entity == this.dispatcher.targetedEntity);
    }

    @Override
    public Identifier getTexture(Entity entity) {
        return null;
    }
}
