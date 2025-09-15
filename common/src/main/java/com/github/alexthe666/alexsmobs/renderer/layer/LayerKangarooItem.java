package com.github.alexthe666.alexsmobs.renderer.layer;

import com.github.alexthe666.alexsmobs.entity.EntityKangaroo;
import com.github.alexthe666.alexsmobs.model.ModelKangaroo;
import com.github.alexthe666.alexsmobs.renderer.RenderKangaroo;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;

public class LayerKangarooItem extends FeatureRenderer<EntityKangaroo, ModelKangaroo> {

    public LayerKangarooItem(RenderKangaroo render) {
        super(render);
    }

    @Override
    public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntityKangaroo entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        ItemStack itemstack = entitylivingbaseIn.getEquippedStack(EquipmentSlot.MAINHAND);
        matrixStackIn.push();
        boolean left = entitylivingbaseIn.isLeftHanded();
        if(entitylivingbaseIn.isBaby()){
            matrixStackIn.scale(0.5F, 0.5F, 0.5F);
            matrixStackIn.translate(0.0D, 1.5D, 0D);
        }
        matrixStackIn.push();
        translateToHand(matrixStackIn, left);
        matrixStackIn.translate(0F, 0.75F, -0.125F);

        matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-110F));
        matrixStackIn.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180F));
        matrixStackIn.scale(0.8F, 0.8F, 0.8F);
        var renderer = MinecraftClient.getInstance().getEntityRenderDispatcher().getHeldItemRenderer();
        renderer.renderItem(entitylivingbaseIn, itemstack, left ? ModelTransformationMode.THIRD_PERSON_LEFT_HAND : ModelTransformationMode.THIRD_PERSON_RIGHT_HAND, false, matrixStackIn, bufferIn, packedLightIn);
        matrixStackIn.pop();
        matrixStackIn.pop();
    }

    protected void translateToHand(MatrixStack matrixStack, boolean left) {
        this.getContextModel().root.translateAndRotate(matrixStack);
        this.getContextModel().body.translateAndRotate(matrixStack);
        this.getContextModel().chest.translateAndRotate(matrixStack);
        if(left){
            this.getContextModel().arm_left.translateAndRotate(matrixStack);
        }else{
            this.getContextModel().arm_right.translateAndRotate(matrixStack);
        }
    }
}
