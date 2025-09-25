package com.github.alexthe666.alexsmobs.client.render.entity.layer;

import com.github.alexthe666.alexsmobs.client.model.ModelCockroach;
import com.github.alexthe666.alexsmobs.client.model.layered.ModelSombrero;
import com.github.alexthe666.alexsmobs.client.render.entity.RenderCockroach;
import com.github.alexthe666.alexsmobs.entity.EntityCockroach;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import com.github.alexthe666.alexsmobs.registry.AMModelLayerRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public class LayerCockroachMaracas extends FeatureRenderer<EntityCockroach, ModelCockroach> {

    private final ItemStack stack;
    private final ModelSombrero sombrero;
    private static final Identifier SOMBRERO_TEX = new Identifier("alexsmobs:textures/armor/sombrero.png");

    public LayerCockroachMaracas(RenderCockroach render, EntityRendererFactory.Context renderManagerIn) {
        super(render);
        stack = new ItemStack(AMItemRegistry.MARACA.get());
        this.sombrero = new ModelSombrero<>(renderManagerIn.getPart(AMModelLayerRegistry.SOMBRERO));
    }

    public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntityCockroach entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if(entitylivingbaseIn.hasMaracas()){
            var renderer = MinecraftClient.getInstance().getEntityRenderDispatcher().getHeldItemRenderer();
            matrixStackIn.push();
            if (entitylivingbaseIn.isBaby()) {
                matrixStackIn.scale(0.65F, 0.65F, 0.65F);
                matrixStackIn.translate(0.0D, 0.815D, 0.125D);
            }
            matrixStackIn.push();
            translateToHand(0, matrixStackIn);
            matrixStackIn.translate(-0.25F, 0.0F, 0);
            matrixStackIn.scale(1.4F, 1.4F, 1.4F);
            matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90F));
            matrixStackIn.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(60F));
            renderer.renderItem(entitylivingbaseIn, stack, ModelTransformationMode.GROUND, false, matrixStackIn, bufferIn, packedLightIn);
            matrixStackIn.pop();
            matrixStackIn.push();
            translateToHand(1, matrixStackIn);
            matrixStackIn.translate(0.25F, 0.0F, 0);
            matrixStackIn.scale(1.4F, 1.4F, 1.4F);
            matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90F));
            matrixStackIn.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-120F));
            renderer.renderItem(entitylivingbaseIn, stack, ModelTransformationMode.GROUND, false, matrixStackIn, bufferIn, packedLightIn);
            matrixStackIn.pop();
            matrixStackIn.push();
            translateToHand(2, matrixStackIn);
            matrixStackIn.translate(-0.35F, 0.0F, 0);
            matrixStackIn.scale(1.4F, 1.4F, 1.4F);
            matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90F));
            matrixStackIn.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(60F));
            renderer.renderItem(entitylivingbaseIn, stack, ModelTransformationMode.GROUND, false, matrixStackIn, bufferIn, packedLightIn);
            matrixStackIn.pop();
            matrixStackIn.push();
            translateToHand(3, matrixStackIn);
            matrixStackIn.translate(0.35F, 0.0F, 0);
            matrixStackIn.scale(1.4F, 1.4F, 1.4F);
            matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90F));
            matrixStackIn.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-120F));
            renderer.renderItem(entitylivingbaseIn, stack, ModelTransformationMode.GROUND, false, matrixStackIn, bufferIn, packedLightIn);
            matrixStackIn.pop();
            if(!entitylivingbaseIn.isHeadless()){
                matrixStackIn.push();
                translateToHand(4, matrixStackIn);
                matrixStackIn.translate(0F, -0.4F, -0.01F);
                matrixStackIn.translate(0F, entitylivingbaseIn.danceProgress * 0.045F, entitylivingbaseIn.danceProgress * -0.09F);
                matrixStackIn.scale(0.8F, 0.8F, 0.8F);
                matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(60F * entitylivingbaseIn.danceProgress * 0.2F));
                var ivertexbuilder = bufferIn.getBuffer(RenderLayer.getEntityCutoutNoCull(SOMBRERO_TEX));
                sombrero.render(matrixStackIn, ivertexbuilder, packedLightIn, LivingEntityRenderer.getOverlay(entitylivingbaseIn, 0.0F), 1.0F, 1.0F, 1.0F, 1.0F);
                matrixStackIn.pop();
            }
            matrixStackIn.pop();
        }
    }

    protected void translateToHand(int hand, MatrixStack matrixStack) {
        this.getContextModel().root.translateAndRotate(matrixStack);
        this.getContextModel().abdomen.translateAndRotate(matrixStack);
        if (hand == 0) {
            this.getContextModel().right_leg_front.translateAndRotate(matrixStack);
        } else if (hand == 1) {
            this.getContextModel().left_leg_front.translateAndRotate(matrixStack);
        } else if (hand == 2) {
            this.getContextModel().right_leg_mid.translateAndRotate(matrixStack);
        } else if (hand == 3) {
            this.getContextModel().left_leg_mid.translateAndRotate(matrixStack);
        }else{
            this.getContextModel().neck.translateAndRotate(matrixStack);
            this.getContextModel().head.translateAndRotate(matrixStack);
        }
    }
}
