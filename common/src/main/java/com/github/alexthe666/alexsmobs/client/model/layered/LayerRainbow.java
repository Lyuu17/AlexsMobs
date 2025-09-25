package com.github.alexthe666.alexsmobs.client.model.layered;

import com.github.alexthe666.alexsmobs.client.render.entity.AMRenderLayers;
import com.github.alexthe666.alexsmobs.entity.util.RainbowUtil;
import com.github.alexthe666.alexsmobs.item.ItemRainbowJelly;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;

public class LayerRainbow extends FeatureRenderer {

    private final FeatureRendererContext<?, ?> parent;

    public LayerRainbow(FeatureRendererContext<?, ?> parent) {
        super(parent);
        this.parent = parent;
    }

    @Override
    public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, Entity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        int i = RainbowUtil.getRainbowType((LivingEntity)entity);
        if(entity instanceof LivingEntity && i > 0) {
            var rainbowType = ItemRainbowJelly.RainbowType.values()[MathHelper.clamp(i - 1, 0,ItemRainbowJelly.RainbowType.values().length - 1)];
            var ivertexbuilder = bufferIn.getBuffer(getRenderLayer(rainbowType));
            float alpha = 0.5F;
            matrixStackIn.push();
            this.getContextModel().render(matrixStackIn, ivertexbuilder, packedLightIn, LivingEntityRenderer.getOverlay((LivingEntity)entity, 0), 1, 1, 1, alpha);
            matrixStackIn.pop();
        }
    }

    private RenderLayer getRenderLayer(ItemRainbowJelly.RainbowType rainbowType) {
        return switch (rainbowType) {
            case TRANS -> AMRenderLayers.TRANS_GLINT;
            case NONBI -> AMRenderLayers.NONBI_GLINT;
            case BI -> AMRenderLayers.BI_GLINT;
            case ACE -> AMRenderLayers.ACE_GLINT;
            case WEEZER -> AMRenderLayers.WEEZER_GLINT;
            case BRAZIL -> AMRenderLayers.BRAZIL_GLINT;
            default -> AMRenderLayers.RAINBOW_GLINT;
        };
    }
}
