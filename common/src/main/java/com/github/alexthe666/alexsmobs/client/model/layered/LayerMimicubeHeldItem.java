package com.github.alexthe666.alexsmobs.client.model.layered;

import com.github.alexthe666.alexsmobs.client.model.ModelMimicube;
import com.github.alexthe666.alexsmobs.client.render.entity.RenderMimicube;
import com.github.alexthe666.alexsmobs.entity.EntityMimicube;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class LayerMimicubeHeldItem extends FeatureRenderer<EntityMimicube, ModelMimicube> {

    public LayerMimicubeHeldItem(RenderMimicube render) {
        super(render);
    }

    @Override
    public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntityMimicube entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        ItemStack itemRight = entitylivingbaseIn.getMainHandStack();
        ItemStack itemLeft = entitylivingbaseIn.getOffHandStack();
        float rightSwap = MathHelper.lerp(partialTicks, entitylivingbaseIn.prevRightSwapProgress, entitylivingbaseIn.rightSwapProgress) * 0.2F;
        float leftSwap = MathHelper.lerp(partialTicks, entitylivingbaseIn.prevLeftSwapProgress, entitylivingbaseIn.leftSwapProgress) * 0.2F;
        float attackprogress = MathHelper.lerp(partialTicks, entitylivingbaseIn.prevAttackProgress, entitylivingbaseIn.attackProgress);
        double bob1 = Math.cos(ageInTicks * 0.1F) * 0.1F + 0.1F;
        double bob2 = Math.sin(ageInTicks * 0.1F) * 0.1F + 0.1F;
        if (!itemRight.isEmpty()) {
            matrixStackIn.push();
            translateToHand(false, matrixStackIn);
            matrixStackIn.translate(-0.5F, 0.1F - bob1, -0.1F);
            matrixStackIn.scale(0.9F * (1F - rightSwap), 0.9F * (1F - rightSwap), 0.9F * (1F - rightSwap));
            matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180));
            matrixStackIn.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));
            if(itemRight.getItem() instanceof ShieldItem){
                matrixStackIn.translate(-0.1F,  0, -0.4F);
                matrixStackIn.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90));
            }
            matrixStackIn.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-10));
            matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(360 * rightSwap));
            matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-40 * attackprogress));
            MinecraftClient.getInstance().getItemRenderer().renderItem(itemRight, ModelTransformationMode.THIRD_PERSON_RIGHT_HAND, rightSwap > 0 ? (int) (-100 * rightSwap) : packedLightIn, LivingEntityRenderer.getOverlay(entitylivingbaseIn, 0.0F), matrixStackIn, bufferIn, entitylivingbaseIn.getWorld(), 0);
            matrixStackIn.pop();
        }
        if (!itemLeft.isEmpty()) {
            matrixStackIn.push();
            translateToHand(false, matrixStackIn);
            matrixStackIn.translate(0.45F,  0.1F - bob2, -0.1F);
            matrixStackIn.scale(0.9F * (1F - leftSwap), 0.9F * (1F - leftSwap), 0.9F * (1F - leftSwap));
            matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180));
            matrixStackIn.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));
            int clampedLight = (int) Math.floor(packedLightIn * (1F - leftSwap));
            if(itemLeft.getItem() instanceof ShieldItem){
                matrixStackIn.translate(-0.2F,  0, -0.4F);
                matrixStackIn.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90));
            }
            matrixStackIn.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(10));
            matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(360 * leftSwap));
            MinecraftClient.getInstance().getItemRenderer().renderItem(itemLeft, ModelTransformationMode.THIRD_PERSON_RIGHT_HAND, leftSwap > 0 ? (int) (-100 * leftSwap) : packedLightIn, LivingEntityRenderer.getOverlay(entitylivingbaseIn, 0.0F), matrixStackIn, bufferIn, entitylivingbaseIn.getWorld(), 0);
            matrixStackIn.pop();
        }
    }


    protected void translateToHand(boolean left, MatrixStack matrixStack) {
        this.getContextModel().root.translateAndRotate(matrixStack);
        this.getContextModel().innerbody.translateAndRotate(matrixStack);
    }
}
