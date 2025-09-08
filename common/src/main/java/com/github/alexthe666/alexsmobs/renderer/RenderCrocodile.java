package com.github.alexthe666.alexsmobs.renderer;

import com.github.alexthe666.alexsmobs.entity.EntityCrocodile;
import com.github.alexthe666.alexsmobs.model.ModelCrocodile;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class RenderCrocodile extends MobEntityRenderer<EntityCrocodile, ModelCrocodile> {
    private static final Identifier TEXTURE_0 = new Identifier("alexsmobs:textures/entity/crocodile_0.png");
    private static final Identifier TEXTURE_1 = new Identifier("alexsmobs:textures/entity/crocodile_1.png");
    private static final Identifier TEXTURE_CROWN = new Identifier("alexsmobs:textures/entity/crocodile_crown.png");

    public RenderCrocodile(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelCrocodile(), 0.8F);
        this.addFeature(new CrownLayer(this));
    }

    @Override
    protected void scale(EntityCrocodile entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
        matrixStackIn.scale(0.9F, 0.9F, 0.9F);
    }

    @Override
    public Identifier getTexture(EntityCrocodile entity) {
        return entity.isDesert() ? TEXTURE_1 : TEXTURE_0;
    }

    static class CrownLayer extends FeatureRenderer<EntityCrocodile, ModelCrocodile> {

        public CrownLayer(RenderCrocodile p_i50928_1_) {
            super(p_i50928_1_);
        }

        @Override
        public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntityCrocodile entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            if (entitylivingbaseIn.isCrowned()) {
                var shoeBuffer = bufferIn.getBuffer(AMRenderLayers.getEntityCutoutNoCull(TEXTURE_CROWN));
                matrixStackIn.push();
                this.getContextModel().render(matrixStackIn, shoeBuffer, packedLightIn, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);
                matrixStackIn.pop();
            }
        }
    }


}
