package com.github.alexthe666.alexsmobs.client.render.entity.layer;

import com.github.alexthe666.alexsmobs.client.model.ModelTusklin;
import com.github.alexthe666.alexsmobs.client.render.entity.RenderTusklin;
import com.github.alexthe666.alexsmobs.entity.EntityTusklin;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class LayerTusklinGear extends FeatureRenderer<EntityTusklin, ModelTusklin> {
    private static final Identifier TEXTURE_SADDLE = new Identifier("alexsmobs:textures/entity/tusklin_saddle.png");
    private static final Identifier TEXTURE_SHOES = new Identifier("alexsmobs:textures/entity/tusklin_hooves.png");

    public LayerTusklinGear(RenderTusklin render) {
        super(render);
    }

    public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntityTusklin entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if(entitylivingbaseIn.isSaddled()){
            var ivertexbuilder = bufferIn.getBuffer(RenderLayer.getEntityCutout(TEXTURE_SADDLE));
            this.getContextModel().render(matrixStackIn, ivertexbuilder, packedLightIn, LivingEntityRenderer.getOverlay(entitylivingbaseIn, 0.0F), 1.0F, 1.0F, 1.0F, 1.0F);
        }
        if(!entitylivingbaseIn.getShoeStack().isEmpty()){
            var ivertexbuilder = ItemRenderer.getArmorGlintConsumer(bufferIn, RenderLayer.getArmorCutoutNoCull(TEXTURE_SHOES), false, entitylivingbaseIn.getShoeStack().hasGlint());
            this.getContextModel().render(matrixStackIn, ivertexbuilder, packedLightIn, LivingEntityRenderer.getOverlay(entitylivingbaseIn, 0.0F), 1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
