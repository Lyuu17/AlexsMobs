package com.github.alexthe666.alexsmobs.renderer.layer;

import com.github.alexthe666.alexsmobs.entity.EntityRaccoon;
import com.github.alexthe666.alexsmobs.model.ModelRaccoon;
import com.github.alexthe666.alexsmobs.renderer.RenderRaccoon;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;

public class LayerRaccoonEyes extends FeatureRenderer<EntityRaccoon, ModelRaccoon> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/raccoon_eyes.png");

    public LayerRaccoonEyes(RenderRaccoon render) {
        super(render);
    }

    public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntityRaccoon raccoon, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        long roundedTime = raccoon.getWorld().getTime() % 24000;
        boolean night = roundedTime >= 13000 && roundedTime <= 22000;
        BlockPos ratPos = raccoon.getLightPosition();
        int i = raccoon.getWorld().getLightLevel(LightType.SKY, ratPos);
        int j = raccoon.getWorld().getLightLevel(LightType.BLOCK, ratPos);
        int brightness;
        if (night) {
            brightness = j;
        } else {
            brightness = Math.max(i, j);
        }
        if (brightness < 7 && !raccoon.isRigby()) {
            var ivertexbuilder = bufferIn.getBuffer(RenderLayer.getEyes(TEXTURE));
            this.getContextModel().render(matrixStackIn, ivertexbuilder, packedLightIn, LivingEntityRenderer.getOverlay(raccoon, 0.0F), 1.0F, 1.0F, 1.0F, 1.0F);
        }

    }
}
