package com.github.alexthe666.alexsmobs.client.render.entity.layer;

import com.github.alexthe666.alexsmobs.client.model.ModelCosmaw;
import com.github.alexthe666.alexsmobs.client.render.entity.RenderCosmaw;
import com.github.alexthe666.alexsmobs.entity.EntityCosmaw;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class LayerCosmawBasicGlow extends FeatureRenderer<EntityCosmaw, ModelCosmaw> {
    private final Identifier texture;
    private final RenderLayer renderType;

    public LayerCosmawBasicGlow(RenderCosmaw render, Identifier texture) {
        super(render);
        this.texture = texture;
        this.renderType = RenderLayer.getEyes(texture);
    }

    @Override
    public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntityCosmaw entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        var ivertexbuilder = bufferIn.getBuffer(renderType);
        this.getContextModel().render(matrixStackIn, ivertexbuilder, packedLightIn, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);

    }
}
