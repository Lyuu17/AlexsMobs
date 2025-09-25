package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.entity.EntityVoidWormShot;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.github.alexthe666.alexsmobs.client.model.ModelVoidWormShot;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;

public class RenderVoidWormShot extends EntityRenderer<EntityVoidWormShot> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/void_worm/void_worm_shot.png");
    private static final ModelVoidWormShot MODEL = new ModelVoidWormShot();

    public RenderVoidWormShot(EntityRendererFactory.Context renderManager) {
        super(renderManager);
    }

    @NotNull
    @Override
    public Identifier getTexture(EntityVoidWormShot entity) {
        return TEXTURE;
    }

    @Override
    public void render(EntityVoidWormShot entityIn, float entityYaw, float partialTicks, MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn) {
        matrixStackIn.push();
        matrixStackIn.multiply((new Quaternionf()).rotateX(Maths.rad(180)));
        matrixStackIn.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(MathHelper.lerp(partialTicks, entityIn.prevYaw, entityIn.getYaw())));
        matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(MathHelper.lerp(partialTicks, entityIn.prevPitch, entityIn.getPitch())));
        matrixStackIn.push();
        MODEL.animate(entityIn, entityIn.age + partialTicks);
        float home = (entityIn.prevStopHomingProgress + (entityIn.getStopHomingProgress() - entityIn.prevStopHomingProgress) * partialTicks) / EntityVoidWormShot.HOME_FOR;
        float colorize = home;
        matrixStackIn.translate(0, -1.5F, 0);
        VertexConsumer ivertexbuilder = bufferIn.getBuffer(AMRenderLayers.getFullBright(getTexture(entityIn)));
        MODEL.render(matrixStackIn, ivertexbuilder, 210, OverlayTexture.DEFAULT_UV, Math.max(colorize, 0.2F), Math.max(colorize, 0.2F), 1.0F, 1.0F);
        matrixStackIn.pop();
        matrixStackIn.pop();


    }

}
