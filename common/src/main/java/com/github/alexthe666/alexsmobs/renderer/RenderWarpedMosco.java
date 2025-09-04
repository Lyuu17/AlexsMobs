package com.github.alexthe666.alexsmobs.renderer;

import com.github.alexthe666.alexsmobs.entity.EntityWarpedMosco;
import com.github.alexthe666.alexsmobs.model.ModelWarpedMosco;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;

public class RenderWarpedMosco extends MobEntityRenderer<EntityWarpedMosco, ModelWarpedMosco> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/warped_mosco.png");
    private static final Identifier TEXTURE_EYES = new Identifier("alexsmobs:textures/entity/warped_mosco_glow.png");

    public RenderWarpedMosco(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelWarpedMosco(), 1F);
        this.addFeature(new WarpedMoscoGlowLayer(this));
    }

    @NotNull
    @Override
    public Identifier getTexture(EntityWarpedMosco entity) {
        return TEXTURE;
    }

    static class WarpedMoscoGlowLayer extends FeatureRenderer<EntityWarpedMosco, ModelWarpedMosco> {

        public WarpedMoscoGlowLayer(RenderWarpedMosco p_i50928_1_) {
            super(p_i50928_1_);
        }

        @Override
        public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntityWarpedMosco entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            var ivertexbuilder = bufferIn.getBuffer(AMRenderLayers.getEyesFlickering(TEXTURE_EYES, 0));
            float alpha = 0.5F + (MathHelper.cos(ageInTicks * 0.2F) + 1F) * 0.2F;
            this.getContextModel().render(matrixStackIn, ivertexbuilder, 240, LivingEntityRenderer.getOverlay(entitylivingbaseIn, 0.0F), 0.5F, 1.0F, 1.0F, alpha);
        }
    }
}
