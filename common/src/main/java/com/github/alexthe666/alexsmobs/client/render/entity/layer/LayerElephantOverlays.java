package com.github.alexthe666.alexsmobs.client.render.entity.layer;

import com.github.alexthe666.alexsmobs.client.model.ModelElephant;
import com.github.alexthe666.alexsmobs.client.render.entity.RenderElephant;
import com.github.alexthe666.alexsmobs.entity.EntityElephant;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class LayerElephantOverlays extends FeatureRenderer<EntityElephant, ModelElephant> {

    private static final Identifier[] ELEPHANT_DECOR_TEXTURES = new Identifier[]{new Identifier("alexsmobs:textures/entity/elephant/decor/white.png"), new Identifier("alexsmobs:textures/entity/elephant/decor/orange.png"), new Identifier("alexsmobs:textures/entity/elephant/decor/magenta.png"), new Identifier("alexsmobs:textures/entity/elephant/decor/light_blue.png"), new Identifier("alexsmobs:textures/entity/elephant/decor/yellow.png"), new Identifier("alexsmobs:textures/entity/elephant/decor/lime.png"), new Identifier("alexsmobs:textures/entity/elephant/decor/pink.png"), new Identifier("alexsmobs:textures/entity/elephant/decor/gray.png"), new Identifier("alexsmobs:textures/entity/elephant/decor/light_gray.png"), new Identifier("alexsmobs:textures/entity/elephant/decor/cyan.png"), new Identifier("alexsmobs:textures/entity/elephant/decor/purple.png"), new Identifier("alexsmobs:textures/entity/elephant/decor/blue.png"), new Identifier("alexsmobs:textures/entity/elephant/decor/brown.png"), new Identifier("alexsmobs:textures/entity/elephant/decor/green.png"), new Identifier("alexsmobs:textures/entity/elephant/decor/red.png"), new Identifier("alexsmobs:textures/entity/elephant/decor/black.png")};
    private static final Identifier TRADER_TEXTURE = new Identifier("alexsmobs:textures/entity/elephant/decor/trader.png");

    private static final Identifier TEXTURE_CHEST = new Identifier("alexsmobs:textures/entity/elephant/elephant_chest.png");
    private final ModelElephant model = new ModelElephant(0.5F);

    public LayerElephantOverlays(RenderElephant renderElephant) {
        super(renderElephant);
    }

    public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntityElephant elephant, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if(elephant.isChested()){
            VertexConsumer ivertexbuilder = bufferIn.getBuffer(RenderLayer.getEntityCutout(TEXTURE_CHEST));
            this.getContextModel().render(matrixStackIn, ivertexbuilder, packedLightIn, LivingEntityRenderer.getOverlay(elephant, 0.0F), 1.0F, 1.0F, 1.0F, 1.0F);
        }
        var lvt_11_1_ = elephant.getColor();
        if(lvt_11_1_ != null || elephant.isTrader()) {
            Identifier lvt_12_3_;
            if (!elephant.isTrader()) {
                lvt_12_3_ = ELEPHANT_DECOR_TEXTURES[lvt_11_1_.getId()];
            }else{
                lvt_12_3_ = TRADER_TEXTURE;
            }

            ((ModelElephant) this.getContextModel()).copyStateTo(this.model);
            this.model.setAngles(elephant, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            VertexConsumer lvt_13_1_ = bufferIn.getBuffer(RenderLayer.getEntityCutoutNoCull(lvt_12_3_));
            this.model.render(matrixStackIn, lvt_13_1_, packedLightIn, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
