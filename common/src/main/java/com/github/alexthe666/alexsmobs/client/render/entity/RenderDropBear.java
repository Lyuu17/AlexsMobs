package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.entity.EntityDropBear;
import com.github.alexthe666.alexsmobs.client.model.ModelDropBear;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class RenderDropBear extends MobEntityRenderer<EntityDropBear, ModelDropBear> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/dropbear.png");
    private static final Identifier TEXTURE_EYES = new Identifier("alexsmobs:textures/entity/dropbear_eyes.png");

    public RenderDropBear(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelDropBear(), 0.7F);
        this.addFeature(new EyeLayer(this));
    }

    @Override
    protected void scale(EntityDropBear entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
    }

    @Override
    public Identifier getTexture(EntityDropBear entity) {
        return TEXTURE;
    }

    static class EyeLayer extends FeatureRenderer<EntityDropBear, ModelDropBear> {

        public EyeLayer(RenderDropBear render) {
            super(render);
        }

        @Override
        public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntityDropBear entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            var ivertexbuilder = bufferIn.getBuffer(RenderLayer.getEyes(TEXTURE_EYES));
            this.getContextModel().render(matrixStackIn, ivertexbuilder, packedLightIn, LivingEntityRenderer.getOverlay(entitylivingbaseIn, 0.0F), 1.0F, 1.0F, 1.0F, 1.0F);

        }
    }
}