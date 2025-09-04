package com.github.alexthe666.alexsmobs.renderer;

import com.github.alexthe666.alexsmobs.entity.EntityHemolymph;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class RenderHemolymph extends EntityRenderer<EntityHemolymph> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/hemolymph.png");

    public RenderHemolymph(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn);
    }

    @Override
    public void render(EntityHemolymph p_225623_1_, float p_225623_2_, float p_225623_3_, MatrixStack p_225623_4_, VertexConsumerProvider p_225623_5_, int p_225623_6_) {
        p_225623_4_.push();
        p_225623_4_.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(MathHelper.lerp(p_225623_3_, p_225623_1_.prevYaw, p_225623_1_.getYaw()) - 90.0F));
        p_225623_4_.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathHelper.lerp(p_225623_3_, p_225623_1_.prevPitch, p_225623_1_.getPitch())));
        float lvt_17_1_ = 0;
        if (lvt_17_1_ > 0.0F) {
            float lvt_18_1_ = -MathHelper.sin(lvt_17_1_ * 3.0F) * lvt_17_1_;
            p_225623_4_.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(lvt_18_1_));
        }

        p_225623_4_.multiply(RotationAxis.POSITIVE_X.rotationDegrees(45.0F));
        p_225623_4_.scale(0.05625F, 0.05625F, 0.05625F);
        p_225623_4_.translate(-4.0D, 0.0D, 0.0D);
        VertexConsumer lvt_18_2_ = p_225623_5_.getBuffer(RenderLayer.getEntityCutout(this.getTexture(p_225623_1_)));
        var lvt_19_1_ = p_225623_4_.peek();
        Matrix4f lvt_20_1_ = lvt_19_1_.getPositionMatrix();
        Matrix3f lvt_21_1_ = lvt_19_1_.getNormalMatrix();
        this.drawVertex(lvt_20_1_, lvt_21_1_, lvt_18_2_, -7, -2, -2, 0.0F, 0.15625F, -1, 0, 0, 240);
        this.drawVertex(lvt_20_1_, lvt_21_1_, lvt_18_2_, -7, -2, 2, 0.15625F, 0.15625F, -1, 0, 0, 240);
        this.drawVertex(lvt_20_1_, lvt_21_1_, lvt_18_2_, -7, 2, 2, 0.15625F, 0.3125F, -1, 0, 0, 240);
        this.drawVertex(lvt_20_1_, lvt_21_1_, lvt_18_2_, -7, 2, -2, 0.0F, 0.3125F, -1, 0, 0, 240);
        this.drawVertex(lvt_20_1_, lvt_21_1_, lvt_18_2_, -7, 2, -2, 0.0F, 0.15625F, 1, 0, 0, 240);
        this.drawVertex(lvt_20_1_, lvt_21_1_, lvt_18_2_, -7, 2, 2, 0.15625F, 0.15625F, 1, 0, 0, 240);
        this.drawVertex(lvt_20_1_, lvt_21_1_, lvt_18_2_, -7, -2, 2, 0.15625F, 0.3125F, 1, 0, 0, 240);
        this.drawVertex(lvt_20_1_, lvt_21_1_, lvt_18_2_, -7, -2, -2, 0.0F, 0.3125F, 1, 0, 0, 240);

        for(int lvt_22_1_ = 0; lvt_22_1_ < 4; ++lvt_22_1_) {
            p_225623_4_.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
            this.drawVertex(lvt_20_1_, lvt_21_1_, lvt_18_2_, -8, -2, 0, 0.0F, 0.0F, 0, 1, 0, 240);
            this.drawVertex(lvt_20_1_, lvt_21_1_, lvt_18_2_, 8, -2, 0, 0.5F, 0.0F, 0, 1, 0, 240);
            this.drawVertex(lvt_20_1_, lvt_21_1_, lvt_18_2_, 8, 2, 0, 0.5F, 0.15625F, 0, 1, 0, 240);
            this.drawVertex(lvt_20_1_, lvt_21_1_, lvt_18_2_, -8, 2, 0, 0.0F, 0.15625F, 0, 1, 0, 240);
        }

        p_225623_4_.pop();
        super.render(p_225623_1_, p_225623_2_, p_225623_3_, p_225623_4_, p_225623_5_, p_225623_6_);
    }

    public void drawVertex(Matrix4f p_229039_1_, Matrix3f p_229039_2_, VertexConsumer p_229039_3_, int p_229039_4_, int p_229039_5_, int p_229039_6_, float p_229039_7_, float p_229039_8_, int p_229039_9_, int p_229039_10_, int p_229039_11_, int p_229039_12_) {
        p_229039_3_.vertex(p_229039_1_, (float)p_229039_4_, (float)p_229039_5_, (float)p_229039_6_)
                .color(255, 255, 255, 255)
                .texture(p_229039_7_, p_229039_8_)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(p_229039_12_)
                .normal(p_229039_2_, (float)p_229039_9_, (float)p_229039_11_, (float)p_229039_10_)
                .next();
    }

    @Override
    public Identifier getTexture(EntityHemolymph entity) {
        return TEXTURE;
    }
}
