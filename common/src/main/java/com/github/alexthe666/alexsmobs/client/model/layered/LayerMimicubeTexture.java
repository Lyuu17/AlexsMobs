package com.github.alexthe666.alexsmobs.client.model.layered;

import com.github.alexthe666.alexsmobs.client.model.ModelMimicube;
import com.github.alexthe666.alexsmobs.client.render.entity.RenderMimicube;
import com.github.alexthe666.alexsmobs.entity.EntityMimicube;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class LayerMimicubeTexture extends FeatureRenderer<EntityMimicube, ModelMimicube> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/mimicube_outer.png");

    public LayerMimicubeTexture(RenderMimicube render) {
        super(render);
    }

    @Override
    public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntityMimicube entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        var ivertexbuilder = bufferIn.getBuffer(RenderLayer.getEntityTranslucent(TEXTURE));
        this.getContextModel().render(matrixStackIn, ivertexbuilder, packedLightIn, LivingEntityRenderer.getOverlay(entitylivingbaseIn, 0.0F), 1.0F, 1.0F, 1.0F, 1.0F);
    }
}
