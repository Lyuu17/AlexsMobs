package com.github.alexthe666.alexsmobs.renderer.layer;

import com.github.alexthe666.alexsmobs.entity.EntitySoulVulture;
import com.github.alexthe666.alexsmobs.model.ModelSoulVulture;
import com.github.alexthe666.alexsmobs.renderer.AMRenderLayers;
import com.github.alexthe666.alexsmobs.renderer.RenderSoulVulture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class LayerSoulVultureGlow extends FeatureRenderer<EntitySoulVulture, ModelSoulVulture> {
    private static final Identifier TEXTURE_GLOW = new Identifier("alexsmobs:textures/entity/soul_vulture/soul_vulture_glow.png");
    private static final Identifier TEXTURE_0 = new Identifier("alexsmobs:textures/entity/soul_vulture/soul_vulture_flames_0.png");
    private static final Identifier TEXTURE_1 = new Identifier("alexsmobs:textures/entity/soul_vulture/soul_vulture_flames_1.png");
    private static final Identifier TEXTURE_2 = new Identifier("alexsmobs:textures/entity/soul_vulture/soul_vulture_flames_2.png");

    public LayerSoulVultureGlow(RenderSoulVulture renderSoulVulture) {
        super(renderSoulVulture);
    }

    public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntitySoulVulture entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        this.getContextModel().render(matrixStackIn, bufferIn.getBuffer(AMRenderLayers.getGhost(TEXTURE_GLOW)), 240, LivingEntityRenderer.getOverlay(entitylivingbaseIn, 0.0F), 1.0F, 1.0F, 1.0F, 1F);
        if(entitylivingbaseIn.hasSoulHeart()){
            this.getContextModel().render(matrixStackIn, bufferIn.getBuffer(AMRenderLayers.getGhost(getFlames(entitylivingbaseIn.age))), 240, LivingEntityRenderer.getOverlay(entitylivingbaseIn, 0.0F), 1.0F, 1.0F, 1.0F, 1F);
        }
    }

    private Identifier getFlames(int tickCount) {
        final int i = tickCount / 3 % 3;
        return switch (i) {
            case 2 -> TEXTURE_2;
            case 1 -> TEXTURE_1;
            default -> TEXTURE_0;
        };
    }
}
