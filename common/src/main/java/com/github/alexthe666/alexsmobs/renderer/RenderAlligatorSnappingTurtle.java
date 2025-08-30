package com.github.alexthe666.alexsmobs.renderer;

import com.github.alexthe666.alexsmobs.entity.EntityAlligatorSnappingTurtle;
import com.github.alexthe666.alexsmobs.model.ModelAlligatorSnappingTurtle;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;

public class RenderAlligatorSnappingTurtle extends MobEntityRenderer<EntityAlligatorSnappingTurtle, ModelAlligatorSnappingTurtle> {
    private static final Identifier TEXTURE_MOSS = new Identifier("alexsmobs:textures/entity/alligator_snapping_turtle_moss.png");
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/alligator_snapping_turtle.png");

    public RenderAlligatorSnappingTurtle(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelAlligatorSnappingTurtle(), 0.75F);
        this.addFeature(new AlligatorSnappingTurtleMossLayer(this));
    }

    @Override
    protected void scale(EntityAlligatorSnappingTurtle entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
        float d = entitylivingbaseIn.getTurtleScale() < 0.01F ? 1F : entitylivingbaseIn.getTurtleScale();
        matrixStackIn.scale(d, d, d);
    }

    @NotNull
    @Override
    public Identifier getTexture(EntityAlligatorSnappingTurtle entity) {
        return TEXTURE;
    }

    static class AlligatorSnappingTurtleMossLayer extends FeatureRenderer<EntityAlligatorSnappingTurtle, ModelAlligatorSnappingTurtle> {

        public AlligatorSnappingTurtleMossLayer(RenderAlligatorSnappingTurtle p_i50928_1_) {
            super(p_i50928_1_);
        }

        @Override
        public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntityAlligatorSnappingTurtle entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            if(entitylivingbaseIn.getMoss() > 0){
                float mossAlpha = 0.15F * MathHelper.clamp(entitylivingbaseIn.getMoss(), 0, 10);
                var mossbuffer = bufferIn.getBuffer(AMRenderLayers.getEntityTranslucent(TEXTURE_MOSS));
                this.getContextModel().render(matrixStackIn, mossbuffer, packedLightIn, LivingEntityRenderer.getOverlay(entitylivingbaseIn, 0), 1.0F, 1.0F, 1.0F, Math.min(1.0F, mossAlpha));
            }
        }
    }
}
