package com.github.alexthe666.alexsmobs.renderer.layer;

import com.github.alexthe666.alexsmobs.entity.EntityRaccoon;
import com.github.alexthe666.alexsmobs.model.ModelRaccoon;
import com.github.alexthe666.alexsmobs.renderer.RenderRaccoon;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.util.math.RotationAxis;

public class LayerRaccoonItem extends FeatureRenderer<EntityRaccoon, ModelRaccoon> {

    public LayerRaccoonItem(RenderRaccoon render) {
        super(render);
    }

    public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntityRaccoon entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        var itemstack = entitylivingbaseIn.getEquippedStack(EquipmentSlot.MAINHAND);
        matrixStackIn.push();
        boolean inHand = entitylivingbaseIn.begProgress > 0 || entitylivingbaseIn.standProgress > 0 || entitylivingbaseIn.washProgress > 0;
        if(entitylivingbaseIn.isBaby()){
            matrixStackIn.scale(0.5F, 0.5F, 0.5F);
            matrixStackIn.translate(0.0D, 1.5D, 0D);
        }
        matrixStackIn.push();
        translateToHand(inHand, matrixStackIn);
        if(inHand){
            matrixStackIn.translate(0.2F, 0.4F, 0F);
            matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90F * entitylivingbaseIn.washProgress * 0.2F));
        }else {
            matrixStackIn.translate(0, 0.1F, -0.35F);
        }
        matrixStackIn.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-2.5F));
        matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90F));
        var renderer = MinecraftClient.getInstance().getEntityRenderDispatcher().getHeldItemRenderer();
        renderer.renderItem(entitylivingbaseIn, itemstack, ModelTransformationMode.GROUND, false, matrixStackIn, bufferIn, packedLightIn);
        matrixStackIn.pop();
        matrixStackIn.pop();
    }

    protected void translateToHand(boolean inHand, MatrixStack matrixStack) {
        if(inHand){
            this.getContextModel().root.translateAndRotate(matrixStack);
            this.getContextModel().body.translateAndRotate(matrixStack);
            this.getContextModel().arm_right.translateAndRotate(matrixStack);
        }else{
            this.getContextModel().root.translateAndRotate(matrixStack);
            this.getContextModel().body.translateAndRotate(matrixStack);
            this.getContextModel().head.translateAndRotate(matrixStack);
        }
    }
}
