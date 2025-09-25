package com.github.alexthe666.alexsmobs.client.model.layered;

import com.github.alexthe666.alexsmobs.client.model.ModelSeal;
import com.github.alexthe666.alexsmobs.client.render.entity.RenderSeal;
import com.github.alexthe666.alexsmobs.entity.EntitySeal;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;

public class LayerSealItem extends FeatureRenderer<EntitySeal, ModelSeal> {

    public LayerSealItem(RenderSeal render) {
        super(render);
    }

    @Override
    public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntitySeal entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        ItemStack itemstack = entitylivingbaseIn.getMainHandStack();
        matrixStackIn.push();
        if(entitylivingbaseIn.isBaby()){
            matrixStackIn.scale(0.5F, 0.5F, 0.5F);
            matrixStackIn.translate(0.0D, 1.5D, 0D);
        }
        matrixStackIn.push();
        translateToHand(matrixStackIn);
        if(entitylivingbaseIn.isBaby()){
            matrixStackIn.translate(0.0D, 0.1F, -0.6D);
        }
        matrixStackIn.translate(-0.1F, 0.15F, -0.6F);
        matrixStackIn.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-45F));
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
