package com.github.alexthe666.alexsmobs.renderer.layer;

import com.github.alexthe666.alexsmobs.entity.EntityWarpedToad;
import com.github.alexthe666.alexsmobs.model.ModelWarpedToad;
import com.github.alexthe666.alexsmobs.renderer.AMRenderLayers;
import com.github.alexthe666.alexsmobs.renderer.RenderWarpedToad;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class LayerWarpedToadGlow extends FeatureRenderer<EntityWarpedToad, ModelWarpedToad> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/warped_toad_glow.png");
    private static final Identifier TEXTURE_BLINKING = new Identifier("alexsmobs:textures/entity/warped_toad_glow_blink.png");

    public LayerWarpedToadGlow(RenderWarpedToad renderWarpedToad) {
        super(renderWarpedToad);
    }

    @Override
    public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntityWarpedToad entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if(!entitylivingbaseIn.isBased()){
            var ivertexbuilder = bufferIn.getBuffer(AMRenderLayers.getEyesFlickering(entitylivingbaseIn.isBlinking() ? TEXTURE_BLINKING : TEXTURE, 0));
            final float alpha = 0.75F + (MathHelper.cos(ageInTicks * 0.2F) + 1F) * 0.125F;
            this.getContextModel().render(matrixStackIn, ivertexbuilder, 240, LivingEntityRenderer.getOverlay(entitylivingbaseIn, 0.0F), 1.0F, 1.0F, 1.0F, alpha);
        }
    }
}
