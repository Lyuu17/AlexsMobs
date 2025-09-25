package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.entity.EntityMosquitoSpit;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.LlamaSpitEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.projectile.LlamaSpitEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class RenderMosquitoSpit extends EntityRenderer<EntityMosquitoSpit> {
    private static final Identifier SPIT_TEXTURE = new Identifier("alexsmobs:textures/entity/mosquito_spit.png");
    private final LlamaSpitEntityModel<LlamaSpitEntity> model;

    public RenderMosquitoSpit(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn);
        this.model = new LlamaSpitEntityModel<>(renderManagerIn.getPart(EntityModelLayers.LLAMA_SPIT));
    }

    @Override
    public void render(EntityMosquitoSpit entityIn, float entityYaw, float partialTicks, MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn) {
        matrixStackIn.push();
        matrixStackIn.translate(0.0D, 0.15F, 0.0D);
        matrixStackIn.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(MathHelper.lerp(partialTicks, entityIn.prevYaw, entityIn.getYaw()) - 90.0F));
        matrixStackIn.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathHelper.lerp(partialTicks, entityIn.prevPitch, entityIn.getPitch())));
        var ivertexbuilder = bufferIn.getBuffer(this.model.getLayer(SPIT_TEXTURE));
        this.model.render(matrixStackIn, ivertexbuilder, 240, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);
        matrixStackIn.pop();
        super.render(entityIn, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
    }

    @Override
    public Identifier getTexture(EntityMosquitoSpit entity) {
        return SPIT_TEXTURE;
    }
}
