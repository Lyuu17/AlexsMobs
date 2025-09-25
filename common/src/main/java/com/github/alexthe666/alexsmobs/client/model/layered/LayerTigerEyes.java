package com.github.alexthe666.alexsmobs.client.model.layered;

import com.github.alexthe666.alexsmobs.client.model.ModelTiger;
import com.github.alexthe666.alexsmobs.client.render.entity.RenderTiger;
import com.github.alexthe666.alexsmobs.entity.EntityTiger;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;

public class LayerTigerEyes  extends FeatureRenderer<EntityTiger, ModelTiger> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/tiger/tiger_eyes.png");
    private static final Identifier TEXTURE_WHITE = new Identifier("alexsmobs:textures/entity/tiger/tiger_white_eyes.png");
    private static final Identifier TEXTURE_ANGRY = new Identifier("alexsmobs:textures/entity/tiger/tiger_angry_eyes.png");

    public LayerTigerEyes(RenderTiger render) {
        super(render);
    }

    @Override
    public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntityTiger tiger, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if(!tiger.isSleeping()){
            long roundedTime = tiger.getWorld().getTime() % 24000;
            boolean night = roundedTime >= 13000 && roundedTime <= 22000;
            BlockPos ratPos = tiger.getLightPosition();
            int i = tiger.getWorld().getLightLevel(LightType.SKY, ratPos);
            int j = tiger.getWorld().getLightLevel(LightType.BLOCK, ratPos);
            int brightness;
            if (night) {
                brightness = j;
            } else {
                brightness = Math.max(i, j);
            }
            if (brightness < 7 || tiger.getAngerTime() > 0) {
                var ivertexbuilder = bufferIn.getBuffer(RenderLayer.getEyes(tiger.getAngerTime() > 0 ? TEXTURE_ANGRY : tiger.isWhite() ? TEXTURE_WHITE : TEXTURE));
                this.getContextModel().render(matrixStackIn, ivertexbuilder, packedLightIn, LivingEntityRenderer.getOverlay(tiger, 0.0F), 1.0F, 1.0F, 1.0F, 1.0F);
            }
        }
    }
}
