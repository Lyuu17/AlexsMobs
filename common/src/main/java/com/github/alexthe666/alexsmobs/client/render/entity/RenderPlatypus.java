package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.entity.EntityPlatypus;
import com.github.alexthe666.alexsmobs.client.model.ModelPlatypus;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class RenderPlatypus extends MobEntityRenderer<EntityPlatypus, ModelPlatypus> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/platypus.png");
    private static final Identifier TEXTURE_PERRY = new Identifier("alexsmobs:textures/entity/platypus_perry.png");

    public RenderPlatypus(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelPlatypus(), 0.45F);
        this.addFeature(new FedoraLayer(this));
    }

    @Override
    protected void scale(EntityPlatypus entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
         matrixStackIn.scale(0.9F, 0.9F, 0.9F);
    }

    @Override
    public Identifier getTexture(EntityPlatypus entity) {
        return entity.isPerry() ? TEXTURE_PERRY : TEXTURE;
    }

    static class FedoraLayer extends FeatureRenderer<EntityPlatypus, ModelPlatypus> {
        private final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/platypus_fedora.png");

        public FedoraLayer(RenderPlatypus renderGrizzlyBear) {
            super(renderGrizzlyBear);
        }

        @Override
        public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntityPlatypus entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            if(entitylivingbaseIn.hasFedora()){
                var ivertexbuilder = bufferIn.getBuffer(RenderLayer.getEntityCutout(TEXTURE));
                this.getContextModel().render(matrixStackIn, ivertexbuilder, packedLightIn, LivingEntityRenderer.getOverlay(entitylivingbaseIn, 0.0F), 1.0F, 1.0F, 1.0F, 1.0F);
            }
        }
    }
}
