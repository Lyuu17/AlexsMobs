package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.client.model.ModelCosmaw;
import com.github.alexthe666.alexsmobs.client.render.entity.layer.LayerCosmawBasicGlow;
import com.github.alexthe666.alexsmobs.entity.EntityCosmaw;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public class RenderCosmaw extends MobEntityRenderer<EntityCosmaw, ModelCosmaw> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/cosmaw.png");
    private static final Identifier TEXTURE_GLOW = new Identifier("alexsmobs:textures/entity/cosmaw_glow.png");

    public RenderCosmaw(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelCosmaw(), 0.9F);
        this.addFeature(new LayerHeldItem());
        this.addFeature(new LayerCosmawBasicGlow(this, TEXTURE_GLOW));
    }

    @Override
    protected void scale(EntityCosmaw entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
        matrixStackIn.translate(0, -0.5F, 0);
    }

    @Override
    public Identifier getTexture(EntityCosmaw entity) {
        return TEXTURE;
    }

    class LayerHeldItem extends FeatureRenderer<EntityCosmaw, ModelCosmaw> {

        public LayerHeldItem() {
            super(RenderCosmaw.this);
        }

        @Override
        public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntityCosmaw entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            ItemStack itemstack = entitylivingbaseIn.getMainHandStack();
            matrixStackIn.push();
            translateToHand(matrixStackIn);
            matrixStackIn.translate(-0.0, 0.1F, -1.35F);
            matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-45F));
            matrixStackIn.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-180F));
            matrixStackIn.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(135F));
            matrixStackIn.scale(2, 2, 2);
            var renderer = MinecraftClient.getInstance().getEntityRenderDispatcher().getHeldItemRenderer();
            renderer.renderItem(entitylivingbaseIn, itemstack, ModelTransformationMode.GROUND, false, matrixStackIn, bufferIn, packedLightIn);
            matrixStackIn.pop();
        }

        protected void translateToHand(MatrixStack matrixStack) {
            this.getContextModel().root.translateAndRotate(matrixStack);
            this.getContextModel().body.translateAndRotate(matrixStack);
            this.getContextModel().mouthArm1.translateAndRotate(matrixStack);
            this.getContextModel().mouthArm2.translateAndRotate(matrixStack);

        }
    }
}
