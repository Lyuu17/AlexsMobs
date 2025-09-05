package com.github.alexthe666.alexsmobs.renderer;

import com.github.alexthe666.alexsmobs.entity.EntityGrizzlyBear;
import com.github.alexthe666.alexsmobs.model.ModelGrizzlyBear;
import com.github.alexthe666.alexsmobs.renderer.layer.LayerGrizzlyHoney;
import com.github.alexthe666.alexsmobs.renderer.layer.LayerGrizzlyItem;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class RenderGrizzlyBear extends MobEntityRenderer<EntityGrizzlyBear, ModelGrizzlyBear> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/grizzly_bear.png");
    private static final Identifier TEXTURE_SNOWY = new Identifier("alexsmobs:textures/entity/grizzly_bear_snowy.png");
    public static final Identifier TEXTURE_FREDDY = new Identifier("alexsmobs:textures/entity/grizzly_bear_freddy.png");
    private static final Identifier TEXTURE_FREDDY_EYES = new Identifier("alexsmobs:textures/entity/grizzly_bear_freddy_eyes.png");

    public RenderGrizzlyBear(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelGrizzlyBear(), 0.8F);
        this.addFeature(new LayerFreddyEyes());
        this.addFeature(new LayerGrizzlyHoney(this));
        this.addFeature(new LayerSnow());
        this.addFeature(new LayerGrizzlyItem(this));
    }

    @Override
    public boolean shouldRender(EntityGrizzlyBear livingEntityIn, Frustum camera, double camX, double camY, double camZ) {
        if (livingEntityIn.getAprilFoolsFlag() == 5) {
            return false;
        }
        return super.shouldRender(livingEntityIn, camera, camX, camY, camZ);
    }

    @Override
    public Identifier getTexture(EntityGrizzlyBear entity) {
        return entity.isFreddy() ? TEXTURE_FREDDY : TEXTURE;
    }

    class LayerSnow extends FeatureRenderer<EntityGrizzlyBear, ModelGrizzlyBear> {

        public LayerSnow() {
            super(RenderGrizzlyBear.this);
        }

        @Override
        public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntityGrizzlyBear entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            if (entitylivingbaseIn.isSnowy()) {
                var ivertexbuilder = bufferIn.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE_SNOWY));
                this.getContextModel().render(matrixStackIn, ivertexbuilder, packedLightIn, LivingEntityRenderer.getOverlay(entitylivingbaseIn, 0.0F), 1.0F, 1.0F, 1.0F, 1.0F);
            }
        }
    }

    class LayerFreddyEyes extends FeatureRenderer<EntityGrizzlyBear, ModelGrizzlyBear> {

        public LayerFreddyEyes() {
            super(RenderGrizzlyBear.this);
        }

        @Override
        public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntityGrizzlyBear entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            if (entitylivingbaseIn.getAprilFoolsFlag() == 4 && entitylivingbaseIn.age % 6 <= 2) {
                var ivertexbuilder = bufferIn.getBuffer(AMRenderLayers.getEyesNoFog(TEXTURE_FREDDY_EYES));
                this.getContextModel().render(matrixStackIn, ivertexbuilder, packedLightIn, LivingEntityRenderer.getOverlay(entitylivingbaseIn, 0.0F), 1.0F, 1.0F, 1.0F, 0.1F);
            }
        }
    }
}
