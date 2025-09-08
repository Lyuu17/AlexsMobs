package com.github.alexthe666.alexsmobs.renderer;

import com.github.alexthe666.alexsmobs.entity.EntityRaccoon;
import com.github.alexthe666.alexsmobs.model.ModelRaccoon;
import com.github.alexthe666.alexsmobs.renderer.layer.LayerRaccoonEyes;
import com.github.alexthe666.alexsmobs.renderer.layer.LayerRaccoonItem;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;

public class RenderRaccoon extends MobEntityRenderer<EntityRaccoon, ModelRaccoon> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/raccoon.png");
    private static final Identifier TEXTURE_RIGBY = new Identifier("alexsmobs:textures/entity/raccoon_rigby.png");
    private static final Identifier TEXTURE_BANDANA = new Identifier("alexsmobs:textures/entity/raccoon_bandana.png");

    public RenderRaccoon(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelRaccoon(), 0.4F);
        this.addFeature(new LayerRaccoonEyes(this));
        this.addFeature(new LayerRaccoonItem(this));
        this.addFeature(new BandanaLayer(this));
    }

    @Override
    protected void scale(EntityRaccoon entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
        matrixStackIn.scale(0.75F, 0.75F, 0.75F);
    }

    @Override
    public Identifier getTexture(EntityRaccoon entity) {
        return entity.isRigby() ? TEXTURE_RIGBY : TEXTURE;
    }

    private static class BandanaLayer extends FeatureRenderer<EntityRaccoon, ModelRaccoon> {
        public BandanaLayer(RenderRaccoon renderRaccoon) {
            super(renderRaccoon);
        }

        @Override
        public void render(MatrixStack p_225628_1_, VertexConsumerProvider p_225628_2_, int p_225628_3_, EntityRaccoon raccoon, float p_225628_5_, float p_225628_6_, float p_225628_7_, float p_225628_8_, float p_225628_9_, float p_225628_10_) {
            if (raccoon.getColor() != null && !raccoon.isInvisible()) {
                float lvt_11_2_;
                float lvt_12_2_;
                float lvt_13_2_;
                if (raccoon.hasCustomName() && "jeb_".equals(raccoon.getName().getContent())) {
                    int lvt_15_1_ = raccoon.age / 25 + raccoon.getId();
                    int lvt_16_1_ = DyeColor.values().length;
                    int lvt_17_1_ = lvt_15_1_ % lvt_16_1_;
                    int lvt_18_1_ = (lvt_15_1_ + 1) % lvt_16_1_;
                    float lvt_19_1_ = ((float)(raccoon.age % 25) + p_225628_7_) / 25.0F;
                    float[] lvt_20_1_ = SheepEntity.getRgbColor(DyeColor.byId(lvt_17_1_));
                    float[] lvt_21_1_ = SheepEntity.getRgbColor(DyeColor.byId(lvt_18_1_));
                    lvt_11_2_ = lvt_20_1_[0] * (1.0F - lvt_19_1_) + lvt_21_1_[0] * lvt_19_1_;
                    lvt_12_2_ = lvt_20_1_[1] * (1.0F - lvt_19_1_) + lvt_21_1_[1] * lvt_19_1_;
                    lvt_13_2_ = lvt_20_1_[2] * (1.0F - lvt_19_1_) + lvt_21_1_[2] * lvt_19_1_;
                } else {
                    float[] lvt_14_2_ = SheepEntity.getRgbColor(raccoon.getColor());
                    lvt_11_2_ = lvt_14_2_[0];
                    lvt_12_2_ = lvt_14_2_[1];
                    lvt_13_2_ = lvt_14_2_[2];
                }
                this.getContextModel().render(p_225628_1_, p_225628_2_.getBuffer(AMRenderLayers.getEntityCutoutNoCull(TEXTURE_BANDANA)), p_225628_3_, OverlayTexture.DEFAULT_UV, lvt_11_2_, lvt_12_2_, lvt_13_2_, 1.0F);
            }
        }
    }
}
