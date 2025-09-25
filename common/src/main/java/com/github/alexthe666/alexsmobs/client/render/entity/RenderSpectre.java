package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.client.model.ModelSpectre;
import com.github.alexthe666.alexsmobs.entity.EntitySpectre;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.EyesFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class RenderSpectre extends MobEntityRenderer<EntitySpectre, ModelSpectre> {
    private static final Identifier TEXTURE_BONE = new Identifier("alexsmobs:textures/entity/spectre_bone.png");
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/spectre.png");
    private static final Identifier TEXTURE_EYES = new Identifier("alexsmobs:textures/entity/spectre_glow.png");
    private static final Identifier TEXTURE_LEAD = new Identifier("alexsmobs:textures/entity/spectre_lead.png");

    public RenderSpectre(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelSpectre(), 0.5F);
        this.addFeature(new SpectreEyesLayer(this));
        this.addFeature(new SpectreMembraneLayer(this));
    }

    @Override
    protected void scale(EntitySpectre entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
        matrixStackIn.scale(1.3F, 1.3F, 1.3F);
    }

    @Override
    protected int getBlockLight(EntitySpectre entityIn, BlockPos partialTicks) {
        return 15;
    }

    @Override
    public Identifier getTexture(EntitySpectre entity) {
        return TEXTURE_BONE;
    }

    public float getAlphaForRender(EntitySpectre entityIn, float partialTicks) {
        return ((float) Math.sin((entityIn.age + partialTicks) * 0.1F) + 1.5F) * 0.1F + 0.5F;
    }

    static class SpectreEyesLayer extends EyesFeatureRenderer<EntitySpectre, ModelSpectre> {

        public SpectreEyesLayer(RenderSpectre p_i50928_1_) {
            super(p_i50928_1_);
        }

        @Override
        public RenderLayer getEyesTexture() {
            return RenderLayer.getEyes(TEXTURE_EYES);
        }
    }

    class SpectreMembraneLayer extends FeatureRenderer<EntitySpectre, ModelSpectre> {

        public SpectreMembraneLayer(RenderSpectre p_i50928_1_) {
            super(p_i50928_1_);
        }

        @Override
        public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntitySpectre entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            VertexConsumer lvt_11_1_ = bufferIn.getBuffer(this.getRenderLayer());
            this.getContextModel().render(matrixStackIn, lvt_11_1_, 15728640, LivingEntityRenderer.getOverlay(entitylivingbaseIn, 0), 1.0F, 1.0F, 1.0F, getAlphaForRender(entitylivingbaseIn, partialTicks));
            if (entitylivingbaseIn.isLeashed()) {
                VertexConsumer lead = bufferIn.getBuffer(AMRenderLayers.getEntityCutoutNoCull(TEXTURE_LEAD));
                this.getContextModel().render(matrixStackIn, lead, 15728640, LivingEntityRenderer.getOverlay(entitylivingbaseIn, 0), 1.0F, 1.0F, 1.0F, 1.0F);
            }
        }

        public RenderLayer getRenderLayer() {
            return AMRenderLayers.getSpectreBones(TEXTURE);
        }
    }
}
