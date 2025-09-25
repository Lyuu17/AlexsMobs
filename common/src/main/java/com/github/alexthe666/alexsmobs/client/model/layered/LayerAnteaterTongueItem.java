package com.github.alexthe666.alexsmobs.client.model.layered;

import com.github.alexthe666.alexsmobs.client.model.ModelAnteater;
import com.github.alexthe666.alexsmobs.client.model.ModelLeafcutterAnt;
import com.github.alexthe666.alexsmobs.client.render.entity.RenderAnteater;
import com.github.alexthe666.alexsmobs.entity.EntityAnteater;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public class LayerAnteaterTongueItem extends FeatureRenderer<EntityAnteater, ModelAnteater> {

    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/leafcutter_ant.png");
    private final ModelLeafcutterAnt ANT_MODEL = new ModelLeafcutterAnt();

    public LayerAnteaterTongueItem(RenderAnteater render) {
        super(render);
    }

    @Override
    public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntityAnteater anteater, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        ItemStack itemstack = anteater.getMainHandStack();
        if(!itemstack.isEmpty() || anteater.hasAntOnTongue()){
            double tongueM = Math.min(Math.sin(ageInTicks * 0.15F), 0);
            float scaleItem = -0.2F * (float) tongueM * (anteater.prevTongueProgress + (anteater.tongueProgress - anteater.prevTongueProgress) * partialTicks * 0.2F);

            matrixStackIn.push();
            if(anteater.isBaby()){
                matrixStackIn.scale(0.35F, 0.35F, 0.35F);
                matrixStackIn.translate(0.0D, 2.8D, 0D);
            }
            matrixStackIn.push();
            translateToTongue(matrixStackIn);
            if(anteater.isBaby()){
                matrixStackIn.translate(0.0D, 0.2F, -0.22D);
            }
            matrixStackIn.translate(-0.0, 0.0F, -0.35F);
            matrixStackIn.scale(scaleItem, scaleItem, scaleItem);
            if(anteater.hasAntOnTongue()){
                matrixStackIn.push();
                matrixStackIn.translate(0F, -1.35F, -0.01F);
                VertexConsumer ivertexbuilder = bufferIn.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE));
                ANT_MODEL.animateAnteater(anteater, partialTicks);
                ANT_MODEL.render(matrixStackIn, ivertexbuilder, packedLightIn, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);
                matrixStackIn.pop();

            }else{
                matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90F));
                var renderer = MinecraftClient.getInstance().getEntityRenderDispatcher().getHeldItemRenderer();
                renderer.renderItem(anteater, itemstack, ModelTransformationMode.GROUND, false, matrixStackIn, bufferIn, packedLightIn);
            }
            matrixStackIn.pop();
            matrixStackIn.pop();
        }
    }

    protected void translateToTongue(MatrixStack matrixStack) {
        this.getContextModel().root.translateAndRotate(matrixStack);
        this.getContextModel().body.translateAndRotate(matrixStack);
        this.getContextModel().head.translateAndRotate(matrixStack);
        this.getContextModel().snout.translateAndRotate(matrixStack);
        this.getContextModel().tongue1.translateAndRotate(matrixStack);
        this.getContextModel().tongue2.translateAndRotate(matrixStack);
    }
}
