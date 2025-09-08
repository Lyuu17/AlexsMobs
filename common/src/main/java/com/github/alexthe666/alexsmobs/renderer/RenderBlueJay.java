package com.github.alexthe666.alexsmobs.renderer;

import com.github.alexthe666.alexsmobs.entity.EntityBlueJay;
import com.github.alexthe666.alexsmobs.entity.EntityRaccoon;
import com.github.alexthe666.alexsmobs.model.ModelBlueJay;
import com.github.alexthe666.alexsmobs.model.ModelRaccoon;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public class RenderBlueJay extends MobEntityRenderer<EntityBlueJay, ModelBlueJay> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/blue_jay.png");
    private static final Identifier TEXTURE_SHINY = new Identifier("alexsmobs:textures/entity/blue_jay_shiny.png");

    public RenderBlueJay(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelBlueJay(), 0.2F);
        this.addFeature(new LayerShiny());
    }

    @Override
    protected void scale(EntityBlueJay mob, MatrixStack matrixStackIn, float partialTicks) {
        matrixStackIn.scale(0.9F, 0.9F, 0.9F);
        if(mob.hasVehicle() && mob.getVehicle() != null) {
            if (mob.getVehicle() instanceof EntityRaccoon entityRaccoon) {
                var raccoonRenderer = MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(entityRaccoon);
                if (raccoonRenderer instanceof LivingEntityRenderer && ((LivingEntityRenderer) raccoonRenderer).getModel() instanceof ModelRaccoon raccoonModel) {
                    float begProgress = entityRaccoon.prevBegProgress + (entityRaccoon.begProgress - entityRaccoon.prevBegProgress) * partialTicks;
                    float standProgress0 = entityRaccoon.prevStandProgress + (entityRaccoon.standProgress - entityRaccoon.prevStandProgress) * partialTicks;
                    float sitProgress = entityRaccoon.prevSitProgress + (entityRaccoon.sitProgress - entityRaccoon.prevSitProgress) * partialTicks;
                    float standProgress = Math.max(Math.max(begProgress, standProgress0) - sitProgress, 0);
                    matrixStackIn.translate(0F, -1.03F - sitProgress * 0.01F, 0F);
                    Vec3d vec = raccoonModel.getRidingPosition(new Vec3d(0, 0, -0.1F + standProgress * 0.1F));
                    matrixStackIn.translate(vec.x, vec.y , vec.z);
                }
            }
        }
    }

    @Override
    public Identifier getTexture(EntityBlueJay entity) {
        return TEXTURE;
    }

    class LayerShiny extends FeatureRenderer<EntityBlueJay, ModelBlueJay> {

        public LayerShiny() {
            super(RenderBlueJay.this);
        }

        @Override
        public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntityBlueJay entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            if(entitylivingbaseIn.getFeedTime() > 0){
                var ivertexbuilder = bufferIn.getBuffer(RenderLayer.getEntityTranslucent(TEXTURE_SHINY));
                float alpha = (float) (1F + Math.sin(ageInTicks * 0.3F)) * 0.1F + 0.8F;
                this.getContextModel().render(matrixStackIn, ivertexbuilder, packedLightIn, LivingEntityRenderer.getOverlay(entitylivingbaseIn, 0.0F), 1.0F, 1.0F, 1.0F, alpha);
            }
        }
    }
}
