package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.client.model.ModelSeagull;
import com.github.alexthe666.alexsmobs.entity.EntitySeagull;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public class RenderSeagull extends MobEntityRenderer<EntitySeagull, ModelSeagull> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/seagull.png");
    private static final Identifier TEXTURE_WINGULL = new Identifier("alexsmobs:textures/entity/seagull_wingull.png");

    public RenderSeagull(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelSeagull(), 0.2F);
        this.addFeature(new LayerHeldItem(this));
    }

    @Override
    protected void scale(EntitySeagull entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
    }

    @Override
    public Identifier getTexture(EntitySeagull entity) {
        return entity.isWingull() ? TEXTURE_WINGULL : TEXTURE;
    }

    static class LayerHeldItem extends FeatureRenderer<EntitySeagull, ModelSeagull> {

        public LayerHeldItem(RenderSeagull render) {
            super(render);
        }

        @Override
        public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntitySeagull entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            var itemstack = entitylivingbaseIn.getEquippedStack(EquipmentSlot.MAINHAND);
            matrixStackIn.push();
            if (entitylivingbaseIn.isBaby()) {
                matrixStackIn.scale(0.5F, 0.5F, 0.5F);
                matrixStackIn.translate(0.0D, 1.5D, 0D);
            }
            matrixStackIn.push();
            translateToHand(matrixStackIn);
            matrixStackIn.translate(0, -0.24F, -0.25F);
            matrixStackIn.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-2.5F));
            matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90F));
            var renderer = MinecraftClient.getInstance().getEntityRenderDispatcher().getHeldItemRenderer();
            renderer.renderItem(entitylivingbaseIn, itemstack, ModelTransformationMode.GROUND, false, matrixStackIn, bufferIn, packedLightIn);
            matrixStackIn.pop();
            matrixStackIn.pop();
        }

        protected void translateToHand(MatrixStack matrixStack) {
            this.getContextModel().root.translateAndRotate(matrixStack);
            this.getContextModel().body.translateAndRotate(matrixStack);
            this.getContextModel().head.translateAndRotate(matrixStack);

        }
    }
}
