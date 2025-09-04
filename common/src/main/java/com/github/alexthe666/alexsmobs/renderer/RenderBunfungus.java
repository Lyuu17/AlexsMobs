package com.github.alexthe666.alexsmobs.renderer;

import com.github.alexthe666.alexsmobs.entity.EntityBunfungus;
import com.github.alexthe666.alexsmobs.model.ModelBunfungus;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public class RenderBunfungus extends MobEntityRenderer<EntityBunfungus, ModelBunfungus> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/bunfungus.png");
    private static final Identifier TEXTURE_SLEEPING = new Identifier("alexsmobs:textures/entity/bunfungus_sleeping.png");

    public RenderBunfungus(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelBunfungus(), 0.6F);
        this.addFeature(new LayerHeldItem(this));
    }

    @Override
    protected void scale(EntityBunfungus rabbit, MatrixStack matrixStackIn, float partialTickTime) {
        float f = rabbit.prevTransformTime + (rabbit.transformsIn() - rabbit.prevTransformTime) * partialTickTime;
        float f1 = (EntityBunfungus.MAX_TRANSFORM_TIME - f) / (float)EntityBunfungus.MAX_TRANSFORM_TIME;
        float f2 = f1 * 0.7F + 0.3F;
        matrixStackIn.scale(f2, f2, f2);
    }

    @Override
    public Identifier getTexture(EntityBunfungus entity) {
        return entity.isSleeping() ? TEXTURE_SLEEPING : TEXTURE;
    }

    static class LayerHeldItem extends FeatureRenderer<EntityBunfungus, ModelBunfungus> {

        public LayerHeldItem(RenderBunfungus render) {
            super(render);
        }

        @Override
        public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntityBunfungus entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            ItemStack itemstack = entitylivingbaseIn.getEquippedStack(EquipmentSlot.MAINHAND);
            matrixStackIn.push();
            if (entitylivingbaseIn.isBaby()) {
                matrixStackIn.scale(0.5F, 0.5F, 0.5F);
                matrixStackIn.translate(0.0D, 1.5D, 0D);
            }
            matrixStackIn.push();
            translateToHand(matrixStackIn);
            matrixStackIn.translate(0.3F, 0.45F, -0.15F);
            matrixStackIn.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90F));
            matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90F));
            matrixStackIn.scale(1.15F, 1.15F, 1.15F);
            var renderer = MinecraftClient.getInstance().getEntityRenderDispatcher().getHeldItemRenderer();
            renderer.renderItem(entitylivingbaseIn, itemstack, ModelTransformationMode.GROUND, false, matrixStackIn, bufferIn, packedLightIn);
            matrixStackIn.pop();
            matrixStackIn.pop();
        }

        protected void translateToHand(MatrixStack matrixStack) {
            this.getContextModel().root.translateAndRotate(matrixStack);
            this.getContextModel().body.translateAndRotate(matrixStack);
            this.getContextModel().right_arm.translateAndRotate(matrixStack);

        }
    }
}
