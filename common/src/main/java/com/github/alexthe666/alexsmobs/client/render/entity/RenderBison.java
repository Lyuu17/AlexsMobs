package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.entity.EntityBison;
import com.github.alexthe666.alexsmobs.client.model.ModelBison;
import com.github.alexthe666.alexsmobs.client.model.ModelBisonBaby;
import com.iafenvoy.uranus.client.model.AdvancedEntityModel;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class RenderBison extends MobEntityRenderer<EntityBison, AdvancedEntityModel<EntityBison>> {
    private static final Identifier TEXTURE_BABY = new Identifier("alexsmobs:textures/entity/bison_baby.png");
    private static final Identifier TEXTURE_BABY_SNOWY = new Identifier("alexsmobs:textures/entity/bison_baby_snowy.png");
    private static final Identifier TEXTURE_SNOWY = new Identifier("alexsmobs:textures/entity/bison_snowy.png");
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/bison.png");
    private static final Identifier TEXTURE_SHEARED = new Identifier("alexsmobs:textures/entity/bison_sheared.png");
    private final ModelBison modelBison = new ModelBison();
    private final ModelBisonBaby modelBaby = new ModelBisonBaby();

    public RenderBison(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelBison(), 0.8F);
        this.addFeature(new LayerSnow());
    }

    @Override
    protected void scale(EntityBison entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
        if(entitylivingbaseIn.isBaby()){
            model = modelBaby;
        }else{
            model = modelBison;
        }
    }

    @Override
    public Identifier getTexture(EntityBison entity) {
        return entity.isBaby() ? TEXTURE_BABY : entity.isSheared() ? TEXTURE_SHEARED : TEXTURE;
    }

    class LayerSnow extends FeatureRenderer<EntityBison, AdvancedEntityModel<EntityBison>> {

        public LayerSnow() {
            super(RenderBison.this);
        }

        @Override
        public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntityBison entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            if (entitylivingbaseIn.isSnowy()) {
                var ivertexbuilder = bufferIn.getBuffer(RenderLayer.getEntityCutoutNoCull(entitylivingbaseIn.isBaby() ? TEXTURE_BABY_SNOWY : TEXTURE_SNOWY));
                this.getContextModel().render(matrixStackIn, ivertexbuilder, packedLightIn, LivingEntityRenderer.getOverlay(entitylivingbaseIn, 0.0F), 1.0F, 1.0F, 1.0F, 1.0F);
            }
        }
    }
}
