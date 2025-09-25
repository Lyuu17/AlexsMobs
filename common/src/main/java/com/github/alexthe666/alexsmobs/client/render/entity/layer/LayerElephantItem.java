package com.github.alexthe666.alexsmobs.client.render.entity.layer;

import com.github.alexthe666.alexsmobs.client.model.ModelElephant;
import com.github.alexthe666.alexsmobs.client.render.entity.RenderElephant;
import com.github.alexthe666.alexsmobs.entity.EntityElephant;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

public class LayerElephantItem extends FeatureRenderer<EntityElephant, ModelElephant> {

    public LayerElephantItem(RenderElephant render) {
        super(render);
    }

    public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntityElephant entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        var itemstack = entitylivingbaseIn.getMainHandStack();
        matrixStackIn.push();
        if(entitylivingbaseIn.isBaby()){
            matrixStackIn.scale(0.35F, 0.35F, 0.35F);
            matrixStackIn.translate(0.0D, 2.8D, 0D);
        }
        matrixStackIn.push();
        translateToHand(matrixStackIn);
        if(entitylivingbaseIn.isBaby()){
            matrixStackIn.translate(0.0D, 0.2F, -0.22D);
        }
        matrixStackIn.translate(-0.0, 1.0F, 0.15F);
        matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180F));
        matrixStackIn.scale(1.3F, 1.3F, 1.3F);
        if(MinecraftClient.getInstance().getItemRenderer().getModels().getModel(itemstack).hasDepth()){
            matrixStackIn.translate(-0.05F, -0.1F, -0.15F);
            matrixStackIn.scale(2, 2, 2);
        }
        var renderer = MinecraftClient.getInstance().getEntityRenderDispatcher().getHeldItemRenderer();
        renderer.renderItem(entitylivingbaseIn, itemstack, ModelTransformationMode.GROUND, false, matrixStackIn, bufferIn, packedLightIn);
        matrixStackIn.pop();
        matrixStackIn.pop();
    }

    protected void translateToHand(MatrixStack matrixStack) {
        this.getContextModel().root.translateAndRotate(matrixStack);
        this.getContextModel().body.translateAndRotate(matrixStack);
        this.getContextModel().head.translateAndRotate(matrixStack);
        this.getContextModel().trunk1.translateAndRotate(matrixStack);
        this.getContextModel().trunk2.translateAndRotate(matrixStack);

    }
}
