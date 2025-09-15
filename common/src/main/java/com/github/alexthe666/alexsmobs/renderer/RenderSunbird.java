package com.github.alexthe666.alexsmobs.renderer;

import com.github.alexthe666.alexsmobs.entity.EntitySunbird;
import com.github.alexthe666.alexsmobs.model.ModelSunbird;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class RenderSunbird extends MobEntityRenderer<EntitySunbird, ModelSunbird> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/sunbird.png");
    private static final Identifier TEXTURE_GLOW = new Identifier("alexsmobs:textures/entity/sunbird_glow.png");

    public RenderSunbird(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelSunbird(), 0.5F);
        this.addFeature(new LayerScorch(this));
    }

    private static void vertex(VertexConsumer p_114090_, Matrix4f p_114091_, Matrix3f p_114092_, int p_114093_, float p_114094_, float p_114095_, int p_114096_, int p_114097_) {
        p_114090_.vertex(p_114091_, p_114094_, p_114095_, 0.0F).color(255, 255, 255, 100).texture((float) p_114096_, (float) p_114097_).overlay(OverlayTexture.DEFAULT_UV).light(p_114093_).normal(p_114092_, 0.0F, 1.0F, 0.0F).next();
    }

    @Override
    public void render(EntitySunbird entity, float yaw, float partialTicks, MatrixStack poseStack, VertexConsumerProvider buffer, int light) {
        super.render(entity, yaw, partialTicks, poseStack, buffer, light);
        final float ageInTicks = entity.age + partialTicks;
        final float scale = (12.0F + (float) Math.sin(ageInTicks * 0.3F)) * entity.getScorchProgress(partialTicks);
        if(scale > 0.0F) {
            poseStack.push();
            poseStack.translate(0, entity.getHeight() * 0.5F, 0);
            poseStack.multiply(this.dispatcher.getRotation());
            poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
            poseStack.push();
            poseStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(ageInTicks * 8F));
            poseStack.translate(-scale * 0.5F, -scale * 0.5F, 0);
            MatrixStack.Entry posestack$pose = poseStack.peek();
            Matrix4f matrix4f = posestack$pose.getPositionMatrix();
            Matrix3f matrix3f = posestack$pose.getNormalMatrix();
            VertexConsumer vertexconsumer = buffer.getBuffer(AMRenderLayers.getSunbirdShine());
            vertex(vertexconsumer, matrix4f, matrix3f, light, 0.0F, 0, 0, 1);
            vertex(vertexconsumer, matrix4f, matrix3f, light, scale, 0, 1, 1);
            vertex(vertexconsumer, matrix4f, matrix3f, light, scale, scale, 1, 0);
            vertex(vertexconsumer, matrix4f, matrix3f, light, 0.0F, scale, 0, 0);
            poseStack.pop();
            poseStack.pop();
        }
    }

    @Override
    protected void scale(EntitySunbird entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
    }

    @Override
    protected int getBlockLight(EntitySunbird entityIn, BlockPos partialTicks) {
        return 15;
    }

    @NotNull
    @Override
    public Identifier getTexture(EntitySunbird entity) {
        return TEXTURE;
    }

    static class LayerScorch extends FeatureRenderer<EntitySunbird, ModelSunbird> {

        public LayerScorch(RenderSunbird p_i50928_1_) {
            super(p_i50928_1_);
        }

        @Override
        public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntitySunbird entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            VertexConsumer scorch = bufferIn.getBuffer(AMRenderLayers.getEyesAlphaEnabled(TEXTURE_GLOW));
            float alpha = entitylivingbaseIn.getScorchProgress(partialTicks);
            this.getContextModel().render(matrixStackIn, scorch, 240, LivingEntityRenderer.getOverlay(entitylivingbaseIn, 0), 1.0F, 1.0F, 1.0F, alpha);
        }
    }
}
