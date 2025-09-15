package com.github.alexthe666.alexsmobs.renderer;

import com.github.alexthe666.alexsmobs.entity.EntityStraddler;
import com.github.alexthe666.alexsmobs.model.ModelStraddler;
import com.github.alexthe666.alexsmobs.model.ModelStradpole;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class RenderStraddler extends MobEntityRenderer<EntityStraddler, ModelStraddler> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/straddler.png");
    private static final ModelStradpole STRADPOLE_MODEL = new ModelStradpole();

    public RenderStraddler(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelStraddler(), 0.6F);
        this.addFeature(new StradpoleLayer(this));
    }

    @Override
    protected void scale(EntityStraddler entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
        matrixStackIn.scale(1.2F, 1.2F, 1.2F);
    }

    @Override
    public Identifier getTexture(EntityStraddler entity) {
        return TEXTURE;
    }

    static class StradpoleLayer extends FeatureRenderer<EntityStraddler, ModelStraddler> {

        public StradpoleLayer(RenderStraddler p_i50928_1_) {
            super(p_i50928_1_);
        }

        @Override
        public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntityStraddler straddler, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            int t = straddler.getAnimationTick();
            if(straddler.getAnimation() == EntityStraddler.ANIMATION_LAUNCH && t < 20 && t > 6){
                matrixStackIn.push();
                translateToModel(matrixStackIn);
                final float back = t <= 15 ? (t-6) * 0.05F : 0.25F;
                matrixStackIn.translate(0F, -2.5F + back * 0.5F, 0.35F + back);
                var ivertexbuilder = bufferIn.getBuffer(RenderLayer.getEntityTranslucent(RenderStradpole.TEXTURE));
                STRADPOLE_MODEL.render(matrixStackIn, ivertexbuilder, packedLightIn, LivingEntityRenderer.getOverlay(straddler, 0.0F), 1.0F, 1.0F, 1.0F, 1.0F);
                matrixStackIn.pop();
            }
        }

        protected void translateToModel(MatrixStack matrixStack) {
            this.getContextModel().root.translateAndRotate(matrixStack);
            this.getContextModel().body.translateAndRotate(matrixStack);
        }
    }
}