package com.github.alexthe666.alexsmobs.renderer;

import com.github.alexthe666.alexsmobs.entity.EntityKomodoDragon;
import com.github.alexthe666.alexsmobs.model.ModelKomodoDragon;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class RenderKomodoDragon extends MobEntityRenderer<EntityKomodoDragon, ModelKomodoDragon> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/komodo_dragon.png");
    private static final Identifier TEXTURE_SADDLE = new Identifier("alexsmobs:textures/entity/komodo_dragon_saddle.png");
    private static final Identifier TEXTURE_MAID = new Identifier("alexsmobs:textures/entity/komodo_dragon_maid.png");

    public RenderKomodoDragon(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelKomodoDragon(0.0F), 0.6F);
        this.addFeature(new LayerSaddle(this));
    }

    @Override
    protected void scale(EntityKomodoDragon entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
    }

    @Override
    public Identifier getTexture(EntityKomodoDragon entity) {
        return TEXTURE;
    }

    static class LayerSaddle extends FeatureRenderer<EntityKomodoDragon, ModelKomodoDragon> {

        private static final ModelKomodoDragon MAID_MODEL = new ModelKomodoDragon(0.3F);
        private static final ModelKomodoDragon SADDLE_MODEL = new ModelKomodoDragon(0.5F);

        public LayerSaddle(RenderKomodoDragon render) {
            super(render);
        }

        @Override
        public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntityKomodoDragon entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            if(entitylivingbaseIn.isMaid()){
                VertexConsumer maid = bufferIn.getBuffer(AMRenderLayers.getEntityCutoutNoCull(TEXTURE_MAID));
                this.getContextModel().copyStateTo(MAID_MODEL);
                MAID_MODEL.animateModel(entitylivingbaseIn, limbSwing, limbSwingAmount, partialTicks);
                MAID_MODEL.setAngles(entitylivingbaseIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
                MAID_MODEL.render(matrixStackIn, maid, packedLightIn, LivingEntityRenderer.getOverlay(entitylivingbaseIn, 0), 1.0F, 1.0F, 1.0F, 1.0F);
            }
            if(entitylivingbaseIn.isSaddled()){
                VertexConsumer saddle = bufferIn.getBuffer(AMRenderLayers.getEntityCutoutNoCull(TEXTURE_SADDLE));
                this.getContextModel().copyStateTo(SADDLE_MODEL);
                SADDLE_MODEL.animateModel(entitylivingbaseIn, limbSwing, limbSwingAmount, partialTicks);
                SADDLE_MODEL.setAngles(entitylivingbaseIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
                SADDLE_MODEL.render(matrixStackIn, saddle, packedLightIn, LivingEntityRenderer.getOverlay(entitylivingbaseIn, 0), 1.0F, 1.0F, 1.0F, 1.0F);
            }
        }
    }
}