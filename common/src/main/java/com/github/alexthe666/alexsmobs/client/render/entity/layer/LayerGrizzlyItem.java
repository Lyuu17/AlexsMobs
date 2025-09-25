package com.github.alexthe666.alexsmobs.client.render.entity.layer;

import com.github.alexthe666.alexsmobs.client.model.ModelGrizzlyBear;
import com.github.alexthe666.alexsmobs.client.render.entity.RenderGrizzlyBear;
import com.github.alexthe666.alexsmobs.entity.EntityGrizzlyBear;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.util.math.RotationAxis;

public class LayerGrizzlyItem extends FeatureRenderer<EntityGrizzlyBear, ModelGrizzlyBear> {

    public LayerGrizzlyItem(RenderGrizzlyBear renderGrizzlyBear) {
        super(renderGrizzlyBear);
    }

    @Override
    public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntityGrizzlyBear entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        var itemstack = entitylivingbaseIn.getEquippedStack(EquipmentSlot.MAINHAND);
        var renderer = MinecraftClient.getInstance().getEntityRenderDispatcher().getHeldItemRenderer();
        matrixStackIn.push();
        if(entitylivingbaseIn.isBaby()){
            matrixStackIn.scale(0.35F, 0.35F, 0.35F);
            matrixStackIn.translate(0.0D, 2.75D, 0.125D);
            translateToHand(false, matrixStackIn);
            matrixStackIn.translate(0.2F, 0.7F, -0.4F);
            matrixStackIn.scale(2.8F, 2.8F, 2.8F);
        }else{
            translateToHand(false, matrixStackIn);
            matrixStackIn.translate(0.2F, 0.7F, -0.4F);
        }
        matrixStackIn.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(10F));
        matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(100F));
        matrixStackIn.scale(1, 1, 1);
        renderer.renderItem(entitylivingbaseIn, itemstack, ModelTransformationMode.GROUND, false, matrixStackIn, bufferIn, packedLightIn);
        matrixStackIn.pop();
    }

    protected void translateToHand(boolean left, MatrixStack matrixStack) {
        this.getContextModel().root.translateAndRotate(matrixStack);
        this.getContextModel().midbody.translateAndRotate(matrixStack);
        this.getContextModel().body.translateAndRotate(matrixStack);
        this.getContextModel().right_arm.translateAndRotate(matrixStack);
    }
}
