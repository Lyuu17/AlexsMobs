package com.github.alexthe666.alexsmobs.renderer.layer;

import com.github.alexthe666.alexsmobs.entity.EntityCrimsonMosquito;
import com.github.alexthe666.alexsmobs.model.ModelCrimsonMosquito;
import com.github.alexthe666.alexsmobs.renderer.RenderCrimsonMosquito;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class LayerCrimsonMosquitoBlood extends FeatureRenderer<EntityCrimsonMosquito, ModelCrimsonMosquito> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/crimson_mosquito_blood.png");
    private static final Identifier TEXTURE_SICK = new Identifier("alexsmobs:textures/entity/crimson_mosquito_blood_blue.png");

    public LayerCrimsonMosquitoBlood(RenderCrimsonMosquito renderCrimsonMosquito) {
        super(renderCrimsonMosquito);
    }

    @Override
    public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntityCrimsonMosquito entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if(entitylivingbaseIn.getBloodLevel() > 0){
            var ivertexbuilder = bufferIn.getBuffer(RenderLayer.getEyes(entitylivingbaseIn.isSick() ? TEXTURE_SICK : TEXTURE));
            this.getContextModel().render(matrixStackIn, ivertexbuilder, packedLightIn, LivingEntityRenderer.getOverlay(entitylivingbaseIn, 0.0F), 1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
