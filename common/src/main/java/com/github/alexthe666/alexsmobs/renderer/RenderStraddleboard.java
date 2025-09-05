package com.github.alexthe666.alexsmobs.renderer;

import com.github.alexthe666.alexsmobs.entity.EntityStraddleboard;
import com.github.alexthe666.alexsmobs.model.ModelStraddleboard;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionf;

public class RenderStraddleboard extends EntityRenderer<EntityStraddleboard> {
    private static final Identifier TEXTURE_OVERLAY = new Identifier("alexsmobs:textures/entity/straddleboard_overlay.png");
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/straddleboard.png");
    private static final ModelStraddleboard BOARD_MODEL = new ModelStraddleboard();

    public RenderStraddleboard(EntityRendererFactory.Context renderManager) {
        super(renderManager);
    }

    @Override
    public Identifier getTexture(EntityStraddleboard entity) {
        return TEXTURE;
    }

    @Override
    public void render(EntityStraddleboard entityIn, float entityYaw, float partialTicks, MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn) {
        matrixStackIn.push();
        matrixStackIn.multiply(new Quaternionf().rotateY(180F * MathHelper.RADIANS_PER_DEGREE));
        matrixStackIn.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(MathHelper.lerp(partialTicks, entityIn.prevYaw, entityIn.getYaw()) + 180));
        matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(MathHelper.lerp(partialTicks, entityIn.prevPitch, entityIn.getPitch())));
        matrixStackIn.push();
        boolean lava =  entityIn.hasPassengers();
        float f2 = entityIn.getRockingAngle(partialTicks);
        if (!MathHelper.approximatelyEquals(f2, 0.0F)) {
            matrixStackIn.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(entityIn.getRockingAngle(partialTicks)));
        }
        int k = entityIn.getColor();
        float r = (float)(k >> 16 & 255) / 255.0F;
        float g = (float)(k >> 8 & 255) / 255.0F;
        float b = (float)(k & 255) / 255.0F;
        float boardRot = entityIn.prevBoardRot + partialTicks * (entityIn.getBoardRot() - entityIn.prevBoardRot);
        matrixStackIn.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(boardRot));
        matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180));
        matrixStackIn.translate(0, -1.5F - Math.abs(boardRot * 0.007F) - (lava ? 0 : 0.25F), 0);
        BOARD_MODEL.animateBoard(entityIn, entityIn.age + partialTicks);
        var ivertexbuilder2 = bufferIn.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE_OVERLAY));
        BOARD_MODEL.render(matrixStackIn, ivertexbuilder2, packedLightIn, OverlayTexture.DEFAULT_UV, r, g, b, 1.0F);
        var ivertexbuilder = bufferIn.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE));
        BOARD_MODEL.render(matrixStackIn, ivertexbuilder, packedLightIn, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);
        matrixStackIn.pop();
        matrixStackIn.pop();


    }

}
