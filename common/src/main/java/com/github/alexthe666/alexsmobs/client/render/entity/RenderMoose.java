package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.entity.EntityMoose;
import com.github.alexthe666.alexsmobs.client.model.ModelMoose;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class RenderMoose extends MobEntityRenderer<EntityMoose, ModelMoose> {
    private static final Identifier TEXTURE_ANTLERED = new Identifier("alexsmobs:textures/entity/moose_antlered.png");
    private static final Identifier TEXTURE_SNOWY_ANTLERED = new Identifier("alexsmobs:textures/entity/moose_snowy_antlered.png");
    private static final Identifier TEXTURE_SNOWY = new Identifier("alexsmobs:textures/entity/moose_snowy.png");
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/moose.png");

    public RenderMoose(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelMoose(), 0.8F);
        this.addFeature(new LayerSnow());
    }

    @Override
    protected void scale(EntityMoose entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
    }

    @Override
    public Identifier getTexture(EntityMoose entity) {
        return entity.isAntlered() && !entity.isBaby() ? TEXTURE_ANTLERED : TEXTURE;
    }

    class LayerSnow extends FeatureRenderer<EntityMoose, ModelMoose> {

        public LayerSnow() {
            super(RenderMoose.this);
        }

        @Override
        public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntityMoose entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            if (entitylivingbaseIn.isSnowy()) {
                VertexConsumer ivertexbuilder = bufferIn.getBuffer(RenderLayer.getEntityCutoutNoCull(entitylivingbaseIn.isAntlered() && !entitylivingbaseIn.isBaby() ? TEXTURE_SNOWY_ANTLERED : TEXTURE_SNOWY));
                this.getContextModel().render(matrixStackIn, ivertexbuilder, packedLightIn, LivingEntityRenderer.getOverlay(entitylivingbaseIn, 0.0F), 1.0F, 1.0F, 1.0F, 1.0F);
            }
        }
    }
}
