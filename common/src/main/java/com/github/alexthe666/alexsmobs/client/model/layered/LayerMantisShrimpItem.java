package com.github.alexthe666.alexsmobs.client.model.layered;

import com.github.alexthe666.alexsmobs.client.model.ModelMantisShrimp;
import com.github.alexthe666.alexsmobs.client.render.entity.RenderMantisShrimp;
import com.github.alexthe666.alexsmobs.entity.EntityMantisShrimp;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;

public class LayerMantisShrimpItem extends FeatureRenderer<EntityMantisShrimp, ModelMantisShrimp> {

    public LayerMantisShrimpItem(RenderMantisShrimp render) {
        super(render);
    }

    @Override
    public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntityMantisShrimp entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        ItemStack itemstack = entitylivingbaseIn.getEquippedStack(EquipmentSlot.MAINHAND);
        matrixStackIn.push();
        boolean left = entitylivingbaseIn.isLeftHanded();
        if(entitylivingbaseIn.isBaby()){
            matrixStackIn.scale(0.5F, 0.5F, 0.5F);
            matrixStackIn.translate(0.0D, 1.5D, 0D);
        }
        matrixStackIn.push();
        translateToHand(matrixStackIn, left);
        matrixStackIn.translate(left ? 0.075F : -0.075F, 0.45F, -0.125F);
        if(!MinecraftClient.getInstance().getItemRenderer().getModels().getModel(itemstack).hasDepth()){
            matrixStackIn.translate(0F, 0F, 0.05F);
            matrixStackIn.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(left ? -40F : 40F));
        }
        matrixStackIn.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-2.5F));
        matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-180F));
        matrixStackIn.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180F));
        matrixStackIn.scale(1.2F, 1.2F, 1.2F);
        var renderer = MinecraftClient.getInstance().getEntityRenderDispatcher().getHeldItemRenderer();
        renderer.renderItem(entitylivingbaseIn, itemstack, ModelTransformationMode.GROUND, false, matrixStackIn, bufferIn, packedLightIn);
        matrixStackIn.pop();
        matrixStackIn.pop();
    }

    protected void translateToHand(MatrixStack matrixStack, boolean left) {
        this.getContextModel().root.translateAndRotate(matrixStack);
        this.getContextModel().body.translateAndRotate(matrixStack);
        this.getContextModel().head.translateAndRotate(matrixStack);
        if(left){
            this.getContextModel().arm_left.translateAndRotate(matrixStack);
            this.getContextModel().fist_left.translateAndRotate(matrixStack);
        }else{
            this.getContextModel().arm_right.translateAndRotate(matrixStack);
            this.getContextModel().fist_right.translateAndRotate(matrixStack);
        }
    }
}
