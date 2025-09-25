package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.client.model.ModelFart;
import com.github.alexthe666.alexsmobs.entity.EntityFart;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class RenderFart extends EntityRenderer<EntityFart> {
    private static final Identifier FART_TEXTURE = new Identifier("alexsmobs:textures/entity/fart.png");
    private static final ModelFart MODEL = new ModelFart();

    public RenderFart(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn);
    }

    @Override
    public void render(EntityFart entityIn, float entityYaw, float partialTicks, MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn) {
        float f = Math.min(entityIn.age + partialTicks, 30F) / 30F;
        float alpha = 1F - f;
        matrixStackIn.push();
        matrixStackIn.translate(0.0D, (double)0.15F, 0.0D);
        matrixStackIn.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(MathHelper.lerp(partialTicks, entityIn.prevYaw, entityIn.getYaw()) - 180.0F));
        matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(MathHelper.lerp(partialTicks, entityIn.prevPitch, entityIn.getPitch())));
        var ivertexbuilder = bufferIn.getBuffer(RenderLayer.getEntityTranslucent(FART_TEXTURE));
        MODEL.setAngles(entityIn, 0.0F, 0.0F, partialTicks, 0.0F, 0.0F);
        MODEL.render(matrixStackIn, ivertexbuilder, packedLightIn, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, alpha);
        matrixStackIn.pop();
        super.render(entityIn, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
    }

    /**
     * Returns the location of an entity's texture.
     */
    @Override
    public Identifier getTexture(EntityFart entity) {
        return FART_TEXTURE;
    }
}
