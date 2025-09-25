package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.entity.EntityGiantSquid;
import com.github.alexthe666.alexsmobs.entity.EntityGiantSquidPart;
import com.github.alexthe666.alexsmobs.client.model.ModelGiantSquid;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class RenderGiantSquid extends MobEntityRenderer<EntityGiantSquid, ModelGiantSquid> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/giant_squid.png");
    private static final Identifier TEXTURE_BLUE = new Identifier("alexsmobs:textures/entity/giant_squid_blue.png");
    private static final Identifier TEXTURE_DEPRESSURIZED = new Identifier("alexsmobs:textures/entity/giant_squid_depressurized.png");

    public RenderGiantSquid(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelGiantSquid(), 1F);
        this.addFeature(new LayerDepressurization(this));
    }

    @Override
    protected float getLyingAngle(EntityGiantSquid squid) {
        return 0.0F;
    }

    @Override
    public boolean shouldRender(EntityGiantSquid livingEntityIn, Frustum camera, double camX, double camY, double camZ) {
        if(livingEntityIn.isCaptured() && livingEntityIn.isAlive()){
            return false;
        }
        if (super.shouldRender(livingEntityIn, camera, camX, camY, camZ)) {
            return true;
        } else {
            for (EntityGiantSquidPart part : livingEntityIn.allParts) {
                if (camera.isVisible(part.getBoundingBox())) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    protected void scale(EntityGiantSquid entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
    }

    @Override
    public Identifier getTexture(EntityGiantSquid entity) {
        return entity.isBlue() ? TEXTURE_BLUE : TEXTURE;
    }

    static class LayerDepressurization extends FeatureRenderer<EntityGiantSquid, ModelGiantSquid> {

        public LayerDepressurization(RenderGiantSquid render) {
            super(render);
        }

        @Override
        public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntityGiantSquid squid, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            var ivertexbuilder = bufferIn.getBuffer(RenderLayer.getEntityTranslucent(TEXTURE_DEPRESSURIZED));
            float alpha = squid.prevDepressurization + (squid.getDepressurization() - squid.prevDepressurization) * partialTicks;
            this.getContextModel().render(matrixStackIn, ivertexbuilder, packedLightIn, LivingEntityRenderer.getOverlay(squid, 0.0F), 1.0F, 1.0F, 1.0F, alpha);
        }
    }
}
