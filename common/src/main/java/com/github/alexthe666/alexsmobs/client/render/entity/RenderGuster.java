package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.entity.EntityGuster;
import com.github.alexthe666.alexsmobs.client.model.ModelGuster;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.EyesFeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public class RenderGuster extends MobEntityRenderer<EntityGuster, ModelGuster> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/guster.png");
    private static final Identifier TEXTURE_GOOGLY = new Identifier("alexsmobs:textures/entity/guster_silly.png");
    private static final Identifier TEXTURE_EYES = new Identifier("alexsmobs:textures/entity/guster_eye.png");
    private static final Identifier TEXTURE_RED = new Identifier("alexsmobs:textures/entity/guster_red.png");
    private static final Identifier TEXTURE_SOUL = new Identifier("alexsmobs:textures/entity/guster_soul.png");
    private static final Identifier TEXTURE_SOUL_EYES = new Identifier("alexsmobs:textures/entity/guster_eye_soul.png");

    public RenderGuster(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelGuster(), 0.25F);
        this.addFeature(new GusterEyesLayer(this));
    }

    @Nullable
    protected RenderLayer getRenderLayer(EntityGuster p_230496_1_, boolean p_230496_2_, boolean p_230496_3_, boolean p_230496_4_) {
        Identifier Identifier = this.getTexture(p_230496_1_);
        if (p_230496_3_) {
            return RenderLayer.getEntityTranslucent(Identifier);
        } else if (p_230496_2_) {
            return RenderLayer.getEntityTranslucent(Identifier);
        } else {
            return p_230496_4_ ? RenderLayer.getOutline(Identifier) : null;
        }
    }

    @Override
    public Identifier getTexture(EntityGuster entity) {
        return entity.isGooglyEyes() ? TEXTURE_GOOGLY : entity.getVariant() == 2 ? TEXTURE_SOUL : entity.getVariant() == 1 ? TEXTURE_RED : TEXTURE;
    }

    static class GusterEyesLayer extends EyesFeatureRenderer<EntityGuster, ModelGuster> {

        public GusterEyesLayer(RenderGuster p_i50928_1_) {
            super(p_i50928_1_);
        }

        @Override
        public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntityGuster entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            if(!entitylivingbaseIn.isGooglyEyes()){
                VertexConsumer ivertexbuilder = bufferIn.getBuffer(entitylivingbaseIn.getVariant() == 2 ? AMRenderLayers.getEyesNoCull(TEXTURE_SOUL_EYES) : AMRenderLayers.getEyesNoCull(TEXTURE_EYES));
                this.getContextModel().render(matrixStackIn, ivertexbuilder, 15728640, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);
            }
        }

        @Override
        public RenderLayer getEyesTexture() {
            return AMRenderLayers.getEyesNoCull(TEXTURE_EYES);
        }
    }
}
