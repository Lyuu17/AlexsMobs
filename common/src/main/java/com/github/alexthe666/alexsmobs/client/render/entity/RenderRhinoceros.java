package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.client.model.ModelRhinoceros;
import com.github.alexthe666.alexsmobs.entity.EntityRhinoceros;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class RenderRhinoceros extends MobEntityRenderer<EntityRhinoceros, ModelRhinoceros> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/rhinoceros.png");
    private static final Identifier TEXTURE_ANGRY = new Identifier("alexsmobs:textures/entity/rhinoceros_angry.png");
    private static final Identifier TEXTURE_POTION = new Identifier("alexsmobs:textures/entity/rhinoceros_potion.png");

    public RenderRhinoceros(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelRhinoceros(), 0.9F);
        this.addFeature(new PotionLayer(this));
    }

    @Override
    protected void scale(EntityRhinoceros rabbit, MatrixStack matrixStackIn, float partialTickTime) {
        matrixStackIn.scale(1.1F, 1.1F, 1.1F);
    }

    @Override
    public Identifier getTexture(EntityRhinoceros entity) {
        return entity.isAngry() ? TEXTURE_ANGRY : TEXTURE;
    }

    private static class PotionLayer extends FeatureRenderer<EntityRhinoceros, ModelRhinoceros> {
        public PotionLayer(RenderRhinoceros parent) {
            super(parent);
        }

        @Override
        public void render(MatrixStack p_225628_1_, VertexConsumerProvider p_225628_2_, int p_225628_3_, EntityRhinoceros rhino, float p_225628_5_, float p_225628_6_, float p_225628_7_, float p_225628_8_, float p_225628_9_, float p_225628_10_) {
            int color = rhino.getPotionColor();
            if (color != -1 && !rhino.isInvisible()) {
                float r = (float) (color >> 16 & 255) / 255.0F;
                float g = (float) (color >> 8 & 255) / 255.0F;
                float b = (float) (color & 255) / 255.0F;
                this.getContextModel().render(p_225628_1_, p_225628_2_.getBuffer(AMRenderLayers.getEntityCutoutNoCull(TEXTURE_POTION)), p_225628_3_, OverlayTexture.DEFAULT_UV, r, g, b, 1.0F);
            }
        }
    }
}
