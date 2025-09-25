package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.client.model.ModelSkreecher;
import com.github.alexthe666.alexsmobs.entity.EntitySkreecher;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class RenderSkreecher extends MobEntityRenderer<EntitySkreecher, ModelSkreecher> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/skreecher.png");
    private static final Identifier TEXTURE_GLOW = new Identifier("alexsmobs:textures/entity/skreecher_glow.png");

    public RenderSkreecher(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelSkreecher(), 0.35F);
        this.addFeature(new LayerScorch(this));
    }

    @Override
    protected void scale(EntitySkreecher entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
    }

    @Override
    public Identifier getTexture(EntitySkreecher entity) {
        return TEXTURE;
    }

    static class LayerScorch extends FeatureRenderer<EntitySkreecher, ModelSkreecher> {

        public LayerScorch(RenderSkreecher render) {
            super(render);
        }

        @Override
        public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntitySkreecher entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            VertexConsumer scorch = bufferIn.getBuffer(AMRenderLayers.getEyesAlphaEnabled(TEXTURE_GLOW));
            float alpha = (float)Math.sin((entitylivingbaseIn.age + partialTicks) * 0.1F) * 0.35F + 0.5F;
            this.getContextModel().render(matrixStackIn, scorch, 240, LivingEntityRenderer.getOverlay(entitylivingbaseIn, 0), 1.0F, 1.0F, 1.0F, alpha);
        }
    }
}
