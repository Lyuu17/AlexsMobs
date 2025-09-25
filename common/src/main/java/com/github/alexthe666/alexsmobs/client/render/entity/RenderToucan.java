package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.entity.EntityToucan;
import com.github.alexthe666.alexsmobs.client.model.ModelToucan;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.jetbrains.annotations.NotNull;

public class RenderToucan extends MobEntityRenderer<EntityToucan, ModelToucan> {
    private static final Identifier TEXTURE_0 = new Identifier("alexsmobs:textures/entity/toucan/toucan_0.png");
    private static final Identifier TEXTURE_1 = new Identifier("alexsmobs:textures/entity/toucan/toucan_1.png");
    private static final Identifier TEXTURE_2 = new Identifier("alexsmobs:textures/entity/toucan/toucan_2.png");
    private static final Identifier TEXTURE_3 = new Identifier("alexsmobs:textures/entity/toucan/toucan_3.png");
    private static final Identifier TEXTURE_GOLDEN = new Identifier("alexsmobs:textures/entity/toucan/toucan_gold.png");
    private static final Identifier TEXTURE_SAM = new Identifier("alexsmobs:textures/entity/toucan/toucan_sam.png");

    public RenderToucan(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelToucan(), 0.2F);
        this.addFeature(new LayerGlint(this));
        this.addFeature(new LayerHeldItem(this));
    }

    @Override
    protected void scale(EntityToucan entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
        matrixStackIn.scale(0.9F, 0.9F, 0.9F);
    }

    @NotNull
    @Override
    public Identifier getTexture(EntityToucan entity) {
        if(entity.isSam()){
            return TEXTURE_SAM;
        }
        if(entity.isGolden()){
            return TEXTURE_GOLDEN;
        }
        return switch (entity.getVariant()) {
            case 3 -> TEXTURE_3;
            case 2 -> TEXTURE_2;
            case 1 -> TEXTURE_1;
            default -> TEXTURE_0;
        };
    }

    static class LayerGlint extends FeatureRenderer<EntityToucan, ModelToucan> {

        public LayerGlint(RenderToucan render) {
            super(render);
        }

        @Override
        public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntityToucan entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            if(entitylivingbaseIn.isEnchanted()){
                var vertexconsumer = ItemRenderer.getArmorGlintConsumer(bufferIn, RenderLayer.getArmorCutoutNoCull(TEXTURE_GOLDEN), false, true);
                this.getContextModel().render(matrixStackIn, vertexconsumer, packedLightIn, LivingEntityRenderer.getOverlay(entitylivingbaseIn, 0.0F), 1, 1, 1, 1.0F);
            }
        }
    }

    static class LayerHeldItem extends FeatureRenderer<EntityToucan, ModelToucan> {

        public LayerHeldItem(RenderToucan render) {
            super(render);
        }

        @Override
        public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntityToucan entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            var itemstack = entitylivingbaseIn.getEquippedStack(EquipmentSlot.MAINHAND);
            matrixStackIn.push();
            if (entitylivingbaseIn.isBaby()) {
                matrixStackIn.scale(0.5F, 0.5F, 0.5F);
                matrixStackIn.translate(0.0D, 1.5D, 0D);
            }
            matrixStackIn.push();
            translateToHand(matrixStackIn);
            matrixStackIn.translate(-0.07F, -0.1F, -0.25F);
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
}
