package com.github.alexthe666.alexsmobs.client.render.entity.layer;

import com.github.alexthe666.alexsmobs.client.model.ModelAncientDart;
import com.github.alexthe666.alexsmobs.client.model.ModelCapuchinMonkey;
import com.github.alexthe666.alexsmobs.client.render.entity.RenderCapuchinMonkey;
import com.github.alexthe666.alexsmobs.entity.EntityCapuchinMonkey;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public class LayerCapuchinItem extends FeatureRenderer<EntityCapuchinMonkey, ModelCapuchinMonkey> {

    public static final Identifier DART_TEXTURE = new Identifier("alexsmobs:textures/entity/ancient_dart.png");
    public static final ModelAncientDart DART_MODEL = new ModelAncientDart();

    public LayerCapuchinItem(RenderCapuchinMonkey render) {
        super(render);
    }

    public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntityCapuchinMonkey entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if(entitylivingbaseIn.hasDart()){
            matrixStackIn.push();
            if(entitylivingbaseIn.isBaby()){
                matrixStackIn.scale(0.35F, 0.35F, 0.35F);
                matrixStackIn.translate(0.5D, 2.6D, 0.15D);
                translateToHand(false, matrixStackIn);
                matrixStackIn.translate(-0.65, -0.75F, -0.1F);
                matrixStackIn.scale(2.8F, 2.8F, 2.8F);

            }else{
                translateToHand(false, matrixStackIn);
            }
            float f = 0.0F;
            if(entitylivingbaseIn.getAnimation() == EntityCapuchinMonkey.ANIMATION_THROW){
                if(entitylivingbaseIn.getAnimationTick() < 6){
                    f = Math.min(3, entitylivingbaseIn.getAnimationTick() + partialTicks) * 60;
                }else{
                    f = (12 - (entitylivingbaseIn.getAnimationTick() + partialTicks)) * 30;
                }
            }
            matrixStackIn.translate(0, 0.5F, 0F);
            matrixStackIn.scale(1.2F, 1.2F, 1.2F);
            matrixStackIn.push();
            matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(f));
            var ivertexbuilder = bufferIn.getBuffer(DART_MODEL.getLayer(DART_TEXTURE));
            DART_MODEL.render(matrixStackIn, ivertexbuilder, packedLightIn, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);
            matrixStackIn.pop();
            matrixStackIn.pop();

        }else if(entitylivingbaseIn.getAnimation() == EntityCapuchinMonkey.ANIMATION_THROW && entitylivingbaseIn.getAnimationTick() <= 5) {
            ItemStack itemstack = new ItemStack(Items.COBBLESTONE);
            matrixStackIn.push();
            if (entitylivingbaseIn.isBaby()) {
                matrixStackIn.scale(0.35F, 0.35F, 0.35F);
                matrixStackIn.translate(0.5D, 2.6D, 0.15D);
                translateToHand(false, matrixStackIn);
                matrixStackIn.translate(-0.4F, 0.75F, -0.0F);
                matrixStackIn.scale(2.8F, 2.8F, 2.8F);
            } else {
                translateToHand(false, matrixStackIn);
                matrixStackIn.translate(0.125F, 0.5F, 0.1F);
            }
            matrixStackIn.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-2.5F));
            matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90F));
            var renderer = MinecraftClient.getInstance().getEntityRenderDispatcher().getHeldItemRenderer();
            renderer.renderItem(entitylivingbaseIn, itemstack, ModelTransformationMode.GROUND, false, matrixStackIn, bufferIn, packedLightIn);
            matrixStackIn.pop();
        }
    }

    protected void translateToHand(boolean left, MatrixStack matrixStack) {
        this.getContextModel().root.translateAndRotate(matrixStack);
        this.getContextModel().body.translateAndRotate(matrixStack);
        this.getContextModel().arm_right.translateAndRotate(matrixStack);
    }
}
