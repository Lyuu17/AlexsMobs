package com.github.alexthe666.alexsmobs.renderer;

import com.github.alexthe666.alexsmobs.entity.EntityEnderiophage;
import com.github.alexthe666.alexsmobs.model.ModelEnderiophage;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.EyesFeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public class RenderEnderiophage extends MobEntityRenderer<EntityEnderiophage, ModelEnderiophage> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/enderiophage.png");
    private static final Identifier TEXTURE_GLOW = new Identifier("alexsmobs:textures/entity/enderiophage_glow.png");
    private static final Identifier TEXTURE_OVERWORLD = new Identifier("alexsmobs:textures/entity/enderiophage_overworld.png");
    private static final Identifier TEXTURE_OVERWORLD_GLOW = new Identifier("alexsmobs:textures/entity/enderiophage_overworld_glow.png");
    private static final Identifier TEXTURE_NETHER = new Identifier("alexsmobs:textures/entity/enderiophage_nether.png");
    private static final Identifier TEXTURE_NETHER_GLOW = new Identifier("alexsmobs:textures/entity/enderiophage_nether_glow.png");

    public RenderEnderiophage(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelEnderiophage(), 0.5F);
        this.addFeature(new EnderiophageEyesLayer(this));
    }

    @Nullable
    @Override
    protected RenderLayer getRenderLayer(EntityEnderiophage p_230496_1_, boolean p_230496_2_, boolean p_230496_3_, boolean p_230496_4_) {
        Identifier Identifier = this.getTexture(p_230496_1_);
        if (p_230496_3_) {
            return RenderLayer.getItemEntityTranslucentCull(Identifier);
        } else if (p_230496_2_) {
            return RenderLayer.getEntityTranslucent(Identifier);
        } else {
            return p_230496_4_ ? RenderLayer.getOutline(Identifier) : null;
        }
    }

    @Override
    protected void scale(EntityEnderiophage entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
        float scale = entitylivingbaseIn.prevEnderiophageScale + (entitylivingbaseIn.getPhageScale() - entitylivingbaseIn.prevEnderiophageScale) * partialTickTime;
        matrixStackIn.scale(0.8F * scale, 0.8F * scale, 0.8F * scale);
    }

    @Override
    public Identifier getTexture(EntityEnderiophage entity) {
        return entity.getVariant() == 2 ? TEXTURE_NETHER : entity.getVariant() == 1 ? TEXTURE_OVERWORLD : TEXTURE;
    }

    static class EnderiophageEyesLayer extends EyesFeatureRenderer<EntityEnderiophage, ModelEnderiophage> {

        public EnderiophageEyesLayer(RenderEnderiophage p_i50928_1_) {
            super(p_i50928_1_);
        }

        @Override
        public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntityEnderiophage entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            var ivertexbuilder = bufferIn.getBuffer(this.getRenderLayer(entitylivingbaseIn));
            this.getContextModel().render(matrixStackIn, ivertexbuilder, 15728640, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);
        }

        @Override
        public RenderLayer getEyesTexture() {
            return AMRenderLayers.getGhost(TEXTURE_GLOW);
        }

        public RenderLayer getRenderLayer(EntityEnderiophage entity) {
            return AMRenderLayers.getGhost(entity.getVariant() == 2 ? TEXTURE_NETHER_GLOW : entity.getVariant() == 1 ? TEXTURE_OVERWORLD_GLOW : TEXTURE_GLOW);
        }
    }

}
