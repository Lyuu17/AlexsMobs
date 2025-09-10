package com.github.alexthe666.alexsmobs.renderer;

import com.github.alexthe666.alexsmobs.entity.EntityGust;
import com.github.alexthe666.alexsmobs.model.ModelGuster;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class RenderGust extends EntityRenderer<EntityGust> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/guster.png");
    private final ModelGuster model = new ModelGuster();

    public RenderGust(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn);
    }

    @Override
    public void render(EntityGust entityIn, float entityYaw, float partialTicks, MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn) {
        matrixStackIn.push();
        matrixStackIn.translate(0.0D, (double)0.5F, 0.0D);
        if(!entityIn.getVertical()){
            matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180F));
        }else{
            matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-180F));

        }
        matrixStackIn.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(MathHelper.lerp(partialTicks, entityIn.prevYaw, entityIn.getYaw()) - 90.0F));
        matrixStackIn.scale(0.5F, 0.5F, 0.5F);
        var ivertexbuilder = bufferIn.getBuffer(RenderLayer.getEntityTranslucent(TEXTURE));
        this.model.hideEyes();
        this.model.render(matrixStackIn, ivertexbuilder, packedLightIn, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);
        this.model.animateGust(entityIn, 0, 0, entityIn.age + partialTicks);
        this.model.showEyes();
        matrixStackIn.pop();
        super.render(entityIn, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
    }

    @Override
    public Identifier getTexture(EntityGust entity) {
        return TEXTURE;
    }
}
