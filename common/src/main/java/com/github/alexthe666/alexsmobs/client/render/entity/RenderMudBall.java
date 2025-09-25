package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.entity.EntityMudBall;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class RenderMudBall extends EntityRenderer<EntityMudBall> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/mud_ball.png");

    public RenderMudBall(EntityRendererFactory.Context p_173962_) {
        super(p_173962_);
    }

    @Override
    public void render(EntityMudBall entityMudBall, float f, float f2, MatrixStack p_114083_, VertexConsumerProvider p_114084_, int p_114085_) {
        p_114083_.push();
        p_114083_.scale(0.7F, 0.7F, 0.7F);
        p_114083_.multiply(this.dispatcher.getRotation());
        p_114083_.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
        MatrixStack.Entry $$6 = p_114083_.peek();
        Matrix4f $$7 = $$6.getPositionMatrix();
        Matrix3f $$8 = $$6.getNormalMatrix();
        VertexConsumer $$9 = p_114084_.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE));
        vertex($$9, $$7, $$8, p_114085_, 0.0F, 0, 0, 1);
        vertex($$9, $$7, $$8, p_114085_, 1.0F, 0, 1, 1);
        vertex($$9, $$7, $$8, p_114085_, 1.0F, 1, 1, 0);
        vertex($$9, $$7, $$8, p_114085_, 0.0F, 1, 0, 0);
        p_114083_.pop();
        super.render(entityMudBall, f, f2, p_114083_, p_114084_, p_114085_);
    }

    private static void vertex(VertexConsumer p_114090_, Matrix4f p_114091_, Matrix3f p_114092_, int p_114093_, float p_114094_, int p_114095_, int p_114096_, int p_114097_) {
        p_114090_.vertex(p_114091_, p_114094_ - 0.5F, (float)p_114095_ - 0.25F, 0.0F).color(255, 255, 255, 255).texture((float)p_114096_, (float)p_114097_).overlay(OverlayTexture.DEFAULT_UV).light(p_114093_).normal(p_114092_, 0.0F, 1.0F, 0.0F).next();
    }

    @Override
    public Identifier getTexture(EntityMudBall mudball) {
        return TEXTURE;
  }
}
