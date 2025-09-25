package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.entity.EntityMurmur;
import com.github.alexthe666.alexsmobs.client.model.ModelMurmurBody;
import com.github.alexthe666.alexsmobs.client.model.ModelMurmurHead;
import com.github.alexthe666.alexsmobs.client.model.ModelMurmurNeck;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class RenderMurmurBody extends MobEntityRenderer<EntityMurmur, ModelMurmurBody> {
    public static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/murmur.png");
    public static final Identifier TEXTURE_ANGRY = new Identifier("alexsmobs:textures/entity/murmur_angry.png");
    public static boolean renderWithHead = false;
    private static final ModelMurmurNeck NECK_MODEL = new ModelMurmurNeck();
    private static final ModelMurmurHead HEAD_MODEL = new ModelMurmurHead();

    public RenderMurmurBody(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelMurmurBody(), 0.5F);
    }

    @Override
    protected void scale(EntityMurmur entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
        matrixStackIn.scale(0.85F, 0.85F, 0.85F);
    }

    @Override
    public void render(EntityMurmur body, float entityYaw, float partialTicks, MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn) {
        super.render(body, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
        if (renderWithHead || body.shouldRenderFakeHead()) {
            float f = MathHelper.lerpAngleDegrees(partialTicks, body.prevBodyYaw, body.bodyYaw);
            float f7 = this.getAnimationProgress(body, partialTicks);
            Identifier loc = this.getTexture(body);
            int overlayCoords = getOverlay(body, this.getAnimationCounter(body, partialTicks));
            matrixStackIn.push();
            this.setupTransforms(body, matrixStackIn, f7, f, partialTicks);
            matrixStackIn.scale(-1.0F, -1.0F, 1.0F);
            matrixStackIn.push();
            matrixStackIn.translate(0, -2.9F, 0);
            scale(body, matrixStackIn, partialTicks);
            HEAD_MODEL.resetToDefaultPose();
            HEAD_MODEL.animateHair(f7);
            HEAD_MODEL.render(matrixStackIn, bufferIn.getBuffer(RenderLayer.getEntityCutoutNoCull(loc)), packedLightIn, overlayCoords, 1, 1F, 1, 1);
            matrixStackIn.translate(0, 0.5F, 0);
            NECK_MODEL.resetToDefaultPose();
            NECK_MODEL.setAttributes(0.5F, 0, 0, 0);
            NECK_MODEL.render(matrixStackIn, bufferIn.getBuffer(RenderLayer.getEntityCutoutNoCull(loc)), packedLightIn, overlayCoords, 1, 1F, 1, 1);
            matrixStackIn.pop();
            matrixStackIn.pop();
        }
    }

    @Override
    public Identifier getTexture(EntityMurmur entity) {
        return entity.isAngry() ? TEXTURE_ANGRY : TEXTURE;
    }
}
