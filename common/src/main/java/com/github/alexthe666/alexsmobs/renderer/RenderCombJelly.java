package com.github.alexthe666.alexsmobs.renderer;

import com.github.alexthe666.alexsmobs.entity.EntityCombJelly;
import com.github.alexthe666.alexsmobs.model.ModelCombJelly;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public class RenderCombJelly extends MobEntityRenderer<EntityCombJelly, ModelCombJelly> {
    private static final Identifier TEXTURE_0 = new Identifier("alexsmobs:textures/entity/comb_jelly_blue.png");
    private static final Identifier TEXTURE_1 = new Identifier("alexsmobs:textures/entity/comb_jelly_green.png");
    private static final Identifier TEXTURE_2 = new Identifier("alexsmobs:textures/entity/comb_jelly_red.png");
    private static final Identifier TEXTURE_OVERLAY = new Identifier("alexsmobs:textures/entity/comb_jelly_overlay.png");
    private static final ModelCombJelly STRIPES_MODEL = new ModelCombJelly(0.05F);
    public RenderCombJelly(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelCombJelly(0.0F), 0.3F);
        this.addFeature(new RainbowLayer(this));
    }

    @Override
    protected void scale(EntityCombJelly jelly, MatrixStack matrixStackIn, float partialTickTime) {
        matrixStackIn.scale(jelly.getJellyScale(), jelly.getJellyScale(), jelly.getJellyScale());
    }

    @Override
    protected float getLyingAngle(EntityCombJelly jelly) {
        return 0.0F;
    }

    @Nullable
    @Override
    protected RenderLayer getRenderLayer(EntityCombJelly jelly, boolean normal, boolean invis, boolean outline) {
        Identifier Identifier = this.getTexture(jelly);
        if (invis) {
            return RenderLayer.getItemEntityTranslucentCull(Identifier);
        } else if (normal) {
            return RenderLayer.getEntityTranslucent(Identifier);
        } else {
            return outline ? RenderLayer.getOutline(Identifier) : null;
        }
    }

    @Override
    public Identifier getTexture(EntityCombJelly entity) {
        return entity.getVariant() == 0 ? TEXTURE_0 : entity.getVariant() == 1 ? TEXTURE_1 : TEXTURE_2;
    }

    static class RainbowLayer extends FeatureRenderer<EntityCombJelly, ModelCombJelly> {

        public RainbowLayer(RenderCombJelly render) {
            super(render);
        }

        @Override
        public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntityCombJelly entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            VertexConsumer rainbow = AMRenderLayers.createMergedVertexConsumer(bufferIn.getBuffer(AMRenderLayers.COMBJELLY_RAINBOW_GLINT), bufferIn.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE_OVERLAY)));
            STRIPES_MODEL.setAngles(entitylivingbaseIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            STRIPES_MODEL.render(matrixStackIn, rainbow, packedLightIn, OverlayTexture.DEFAULT_UV, 1, 1, 1, 1.0F);
        }
    }
}
