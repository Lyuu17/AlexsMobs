package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.client.model.ModelPollenBall;
import com.github.alexthe666.alexsmobs.entity.EntityPollenBall;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class RenderPollenBall extends EntityRenderer<EntityPollenBall> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/pollen_ball.png");
    private static final ModelPollenBall MODEL_POLLEN_BALL = new ModelPollenBall();

    public RenderPollenBall(EntityRendererFactory.Context renderManager) {
        super(renderManager);
    }

    @Override
    public Identifier getTexture(EntityPollenBall entity) {
        return TEXTURE;
    }

    @Override
    public void render(EntityPollenBall entityIn, float entityYaw, float partialTicks, MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn) {
        matrixStackIn.push();
        matrixStackIn.push();
        matrixStackIn.translate(0.0D, -0.25F, 0.0D);
        matrixStackIn.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(MathHelper.lerp(partialTicks, entityIn.prevYaw, entityIn.getYaw()) - 180F));
        matrixStackIn.push();
        matrixStackIn.translate(0, 0.5F, 0);
        matrixStackIn.scale(1F, 1F, 1F);
        var ivertexbuilder = bufferIn.getBuffer(AMRenderLayers.getFullBright(getTexture(entityIn)));
        MODEL_POLLEN_BALL.render(matrixStackIn, ivertexbuilder, packedLightIn, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);
        matrixStackIn.pop();
        matrixStackIn.pop();
        matrixStackIn.pop();
    }

}
