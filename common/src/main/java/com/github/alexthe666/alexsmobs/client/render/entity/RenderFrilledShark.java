package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.entity.EntityFrilledShark;
import com.github.alexthe666.alexsmobs.client.model.ModelFrilledShark;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class RenderFrilledShark extends MobEntityRenderer<EntityFrilledShark, ModelFrilledShark> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/frilled_shark.png");
    private static final Identifier TEXTURE_DEPRESSURIZED = new Identifier("alexsmobs:textures/entity/frilled_shark_depressurized.png");
    private static final Identifier TEXTURE_KAIJU = new Identifier("alexsmobs:textures/entity/frilled_shark_kaiju.png");
    private static final Identifier TEXTURE_KAIJU_DEPRESSURIZED = new Identifier("alexsmobs:textures/entity/frilled_shark_kaiju_depressurized.png");
    private static final Identifier TEXTURE_TEETH = new Identifier("alexsmobs:textures/entity/frilled_shark_teeth.png");

    public RenderFrilledShark(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelFrilledShark(), 0.4F);
        this.addFeature(new TeethLayer(this));
    }

    @Override
    protected void scale(EntityFrilledShark entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
        matrixStackIn.scale(0.85F, 0.85F, 0.85F);
    }

    @Override
    public Identifier getTexture(EntityFrilledShark entity) {
        return entity.isKaiju() ? (entity.isDepressurized() ? TEXTURE_KAIJU_DEPRESSURIZED : TEXTURE_KAIJU) : (entity.isDepressurized() ? TEXTURE_DEPRESSURIZED : TEXTURE);
    }

    static class TeethLayer extends FeatureRenderer<EntityFrilledShark, ModelFrilledShark> {

        public TeethLayer(RenderFrilledShark render) {
            super(render);
        }

        @Override
        public void render(MatrixStack matrixStackIn, VertexConsumerProvider buffer, int packedLightIn, EntityFrilledShark entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            var glintBuilder = buffer.getBuffer(AMRenderLayers.getEyesFlickering(TEXTURE_TEETH, 240));
            this.getContextModel().render(matrixStackIn, glintBuilder, 240, OverlayTexture.DEFAULT_UV, 1, 1, 1, 1);

        }
    }
}
