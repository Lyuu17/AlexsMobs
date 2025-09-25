package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.entity.EntityMimicOctopus;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.github.alexthe666.alexsmobs.client.model.ModelMimicOctopus;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class RenderMimicOctopus extends MobEntityRenderer<EntityMimicOctopus, ModelMimicOctopus> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/mimic_octopus.png");
    private static final Identifier TEXTURE_OVERLAY = new Identifier("alexsmobs:textures/entity/mimic_octopus_overlay.png");
    private static final Identifier TEXTURE_CREEPER = new Identifier("alexsmobs:textures/entity/mimic_octopus_creeper.png");
    private static final Identifier TEXTURE_GUARDIAN = new Identifier("alexsmobs:textures/entity/mimic_octopus_guardian.png");
    private static final Identifier TEXTURE_PUFFERFISH = new Identifier("alexsmobs:textures/entity/mimic_octopus_pufferfish.png");
    private static final Identifier TEXTURE_MIMICUBE = new Identifier("alexsmobs:textures/entity/mimic_octopus_mimicube.png");
    private static final Identifier TEXTURE_EYES = new Identifier("alexsmobs:textures/entity/mimic_octopus_eyes.png");
    private static final Identifier GUARDIAN_BEAM_TEXTURE = new Identifier("textures/entity/guardian_beam.png");
    private static final RenderLayer BEAM_RENDER_TYPE = RenderLayer.getEntityCutoutNoCull(GUARDIAN_BEAM_TEXTURE);

    public RenderMimicOctopus(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelMimicOctopus(), 0.4F);
        this.addFeature(new OverlayLayer(this));
    }

    private static void vertex(VertexConsumer p_229108_0_, Matrix4f p_229108_1_, Matrix3f p_229108_2_, float p_229108_3_, float p_229108_4_, float p_229108_5_, int p_229108_6_, int p_229108_7_, int p_229108_8_, float p_229108_9_, float p_229108_10_) {
        p_229108_0_.vertex(p_229108_1_, p_229108_3_, p_229108_4_, p_229108_5_).color(p_229108_6_, p_229108_7_, p_229108_8_, 255).texture(p_229108_9_, p_229108_10_).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(p_229108_2_, 0.0F, 1.0F, 0.0F).next();
    }

    @Override
    public void render(EntityMimicOctopus entityIn, float entityYaw, float partialTicks, MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn) {
        var livingentity = entityIn.getGuardianLaser();
        if (livingentity != null) {
            float f = entityIn.getLaserAttackAnimationScale(partialTicks);
            float f1 = (float) entityIn.getWorld().getTime() + partialTicks;
            float f2 = f1 * 0.5F % 1.0F;
            float f3 = entityIn.getStandingEyeHeight();
            matrixStackIn.push();
            matrixStackIn.translate(0.0D, f3, 0.0D);
            Vec3d vector3d = this.getPosition(livingentity, (double) livingentity.getHeight() * 0.5D, partialTicks);
            Vec3d vector3d1 = this.getPosition(entityIn, f3, partialTicks);
            Vec3d vector3d2 = vector3d.subtract(vector3d1);
            float f4 = (float) (vector3d2.length() + 1.0D);
            vector3d2 = vector3d2.normalize();
            float f5 = (float) Math.acos(vector3d2.y);
            float f6 = (float) Math.atan2(vector3d2.z, vector3d2.x);
            matrixStackIn.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(((MathHelper.PI / 2F) - f6) * MathHelper.DEGREES_PER_RADIAN));
            matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(f5 * MathHelper.DEGREES_PER_RADIAN));
            int i = 1;
            float f7 = f1 * 0.05F * -1.5F;
            float f8 = f * f;
            int j = 64 + (int) (f8 * 191.0F);
            int k = 32 + (int) (f8 * 191.0F);
            int l = 128 - (int) (f8 * 64.0F);
            float f9 = 0.2F;
            float f10 = 0.282F;
            float f11 = MathHelper.cos(f7 + 2.3561945F) * 0.282F;
            float f12 = MathHelper.sin(f7 + 2.3561945F) * 0.282F;
            float f13 = MathHelper.cos(f7 + Maths.QUARTER_PI) * 0.282F;
            float f14 = MathHelper.sin(f7 + Maths.QUARTER_PI) * 0.282F;
            float f15 = MathHelper.cos(f7 + 3.926991F) * 0.282F;
            float f16 = MathHelper.sin(f7 + 3.926991F) * 0.282F;
            float f17 = MathHelper.cos(f7 + 5.4977875F) * 0.282F;
            float f18 = MathHelper.sin(f7 + 5.4977875F) * 0.282F;
            float f19 = MathHelper.cos(f7 + MathHelper.PI) * 0.2F;
            float f20 = MathHelper.sin(f7 + MathHelper.PI) * 0.2F;
            float f21 = MathHelper.cos(f7 + 0.0F) * 0.2F;
            float f22 = MathHelper.sin(f7 + 0.0F) * 0.2F;
            float f23 = MathHelper.cos(f7 + (MathHelper.PI / 2F)) * 0.2F;
            float f24 = MathHelper.sin(f7 + (MathHelper.PI / 2F)) * 0.2F;
            float f25 = MathHelper.cos(f7 + (MathHelper.PI * 1.5F)) * 0.2F;
            float f26 = MathHelper.sin(f7 + (MathHelper.PI * 1.5F)) * 0.2F;
            float f27 = 0.0F;
            float f28 = 0.4999F;
            float f29 = -1.0F + f2;
            float f30 = f4 * 2.5F + f29;
            VertexConsumer ivertexbuilder = bufferIn.getBuffer(BEAM_RENDER_TYPE);
            MatrixStack.Entry matrixstack$entry = matrixStackIn.peek();
            Matrix4f matrix4f = matrixstack$entry.getPositionMatrix();
            Matrix3f matrix3f = matrixstack$entry.getNormalMatrix();
            vertex(ivertexbuilder, matrix4f, matrix3f, f19, f4, f20, j, k, l, 0.4999F, f30);
            vertex(ivertexbuilder, matrix4f, matrix3f, f19, 0.0F, f20, j, k, l, 0.4999F, f29);
            vertex(ivertexbuilder, matrix4f, matrix3f, f21, 0.0F, f22, j, k, l, 0.0F, f29);
            vertex(ivertexbuilder, matrix4f, matrix3f, f21, f4, f22, j, k, l, 0.0F, f30);
            vertex(ivertexbuilder, matrix4f, matrix3f, f23, f4, f24, j, k, l, 0.4999F, f30);
            vertex(ivertexbuilder, matrix4f, matrix3f, f23, 0.0F, f24, j, k, l, 0.4999F, f29);
            vertex(ivertexbuilder, matrix4f, matrix3f, f25, 0.0F, f26, j, k, l, 0.0F, f29);
            vertex(ivertexbuilder, matrix4f, matrix3f, f25, f4, f26, j, k, l, 0.0F, f30);
            float f31 = 0.0F;
            if (entityIn.age % 2 == 0) {
                f31 = 0.5F;
            }

            vertex(ivertexbuilder, matrix4f, matrix3f, f11, f4, f12, j, k, l, 0.5F, f31 + 0.5F);
            vertex(ivertexbuilder, matrix4f, matrix3f, f13, f4, f14, j, k, l, 1.0F, f31 + 0.5F);
            vertex(ivertexbuilder, matrix4f, matrix3f, f17, f4, f18, j, k, l, 1.0F, f31);
            vertex(ivertexbuilder, matrix4f, matrix3f, f15, f4, f16, j, k, l, 0.5F, f31);
            matrixStackIn.pop();
        }
        super.render(entityIn, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);

    }

    @Override
    protected void scale(EntityMimicOctopus octo, MatrixStack matrixStackIn, float partialTickTime) {
        matrixStackIn.translate(0, -0.02F, 0);
        matrixStackIn.scale(0.9F * octo.getScaleFactor(), 0.9F * octo.getScaleFactor(), 0.9F * octo.getScaleFactor());
    }

    @Override
    public boolean shouldRender(EntityMimicOctopus livingEntityIn, Frustum camera, double camX, double camY, double camZ) {
        if (super.shouldRender(livingEntityIn, camera, camX, camY, camZ)) {
            return true;
        } else {
            if (livingEntityIn.hasGuardianLaser()) {
                var livingentity = livingEntityIn.getGuardianLaser();
                if (livingentity != null) {
                    var vector3d = this.getPosition(livingentity, (double) livingentity.getHeight() * 0.5D, 1.0F);
                    var vector3d1 = this.getPosition(livingEntityIn, livingEntityIn.getStandingEyeHeight(), 1.0F);
                    return camera.isVisible(new Box(vector3d1.x, vector3d1.y, vector3d1.z, vector3d.x, vector3d.y, vector3d.z));
                }
            }

            return false;
        }
    }

    private Vec3d getPosition(LivingEntity entityLivingBaseIn, double p_177110_2_, float p_177110_4_) {
        double d0 = MathHelper.lerp(p_177110_4_, entityLivingBaseIn.lastRenderX, entityLivingBaseIn.getX());
        double d1 = MathHelper.lerp(p_177110_4_, entityLivingBaseIn.lastRenderY, entityLivingBaseIn.getY()) + p_177110_2_;
        double d2 = MathHelper.lerp(p_177110_4_, entityLivingBaseIn.lastRenderZ, entityLivingBaseIn.getZ());
        return new Vec3d(d0, d1, d2);
    }

    @Override
    public Identifier getTexture(EntityMimicOctopus entity) {
        return TEXTURE;
    }

    static class OverlayLayer extends FeatureRenderer<EntityMimicOctopus, ModelMimicOctopus> {

        public OverlayLayer(RenderMimicOctopus render) {
            super(render);
        }

        @Override
        public void render(MatrixStack matrixStackIn, VertexConsumerProvider buffer, int packedLightIn, EntityMimicOctopus entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            float transProgress = entitylivingbaseIn.prevTransProgress + (entitylivingbaseIn.transProgress - entitylivingbaseIn.prevTransProgress) * partialTicks;
            float colorProgress = (entitylivingbaseIn.prevColorShiftProgress + (entitylivingbaseIn.colorShiftProgress - entitylivingbaseIn.prevColorShiftProgress) * partialTicks) * 0.2F;
            float r = 1F;
            float g = 1F;
            float b = 1F;
            float a = 1F;
            float startR = 1.0F;
            float startG = 1.0F;
            float startB = 1.0F;
            float startA = 1.0F;
            float finR = 1.0F;
            float finG = 1.0F;
            float finB = 1.0F;
            float finA = 1.0F;
            if (entitylivingbaseIn.getPrevMimicState() == EntityMimicOctopus.MimicState.OVERLAY) {
                if (entitylivingbaseIn.getPrevMimickedBlock() != null) {
                    int j = OctopusColorRegistry.getBlockColor(entitylivingbaseIn.getPrevMimickedBlock());
                    startR = (float) (j >> 16 & 255) / 255.0F;
                    startG = (float) (j >> 8 & 255) / 255.0F;
                    startB = (float) (j & 255) / 255.0F;
                } else {
                    startA = 0.0F;
                }
            }
            if ((entitylivingbaseIn.getMimicState() == EntityMimicOctopus.MimicState.OVERLAY)) {
                if (entitylivingbaseIn.getMimickedBlock() != null) {
                    int i = OctopusColorRegistry.getBlockColor(entitylivingbaseIn.getMimickedBlock());
                    finR = (float) (i >> 16 & 255) / 255.0F;
                    finG = (float) (i >> 8 & 255) / 255.0F;
                    finB = (float) (i & 255) / 255.0F;
                } else {
                    finA = 0.0F;
                }
                r = startR + (finR - startR) * colorProgress;
                g = startG + (finG - startG) * colorProgress;
                b = startB + (finB - startB) * colorProgress;
                a = startA + (finA - startA) * colorProgress;
            }
            if (a == 1.0F) {
                a *= 0.9F + 0.1F * (float) Math.sin(entitylivingbaseIn.age * 0.1F);
            }
            if (entitylivingbaseIn.getPrevMimicState() != null) {
                float alphaPrev = 1 - transProgress * 0.2F;
                VertexConsumer prev = buffer.getBuffer(AMRenderLayers.getEntityTranslucent(getFor(entitylivingbaseIn.getPrevMimicState())));
                if(entitylivingbaseIn.getPrevMimicState() == entitylivingbaseIn.getMimicState()){
                    alphaPrev *= a;
                }
                this.getContextModel().render(matrixStackIn, prev, packedLightIn, getOverlay(entitylivingbaseIn, 0), r, g, b, alphaPrev);
            }
            float alphaCurrent = transProgress * 0.2F;
            VertexConsumer current = buffer.getBuffer(AMRenderLayers.getEntityTranslucent(getFor(entitylivingbaseIn.getMimicState())));
            this.getContextModel().render(matrixStackIn, current, packedLightIn, getOverlay(entitylivingbaseIn, 0), r, g, b, a * alphaCurrent);
            VertexConsumer eyes = buffer.getBuffer(AMRenderLayers.getEntityTranslucent(TEXTURE_EYES));
            this.getContextModel().render(matrixStackIn, eyes, packedLightIn, getOverlay(entitylivingbaseIn, 0), 1.0F, 1.0F, 1.0F, 1.0F);
        }

        public Identifier getFor(EntityMimicOctopus.MimicState state) {
            if (state == EntityMimicOctopus.MimicState.CREEPER) {
                return TEXTURE_CREEPER;
            }
            if (state == EntityMimicOctopus.MimicState.GUARDIAN) {
                return TEXTURE_GUARDIAN;
            }
            if (state == EntityMimicOctopus.MimicState.PUFFERFISH) {
                return TEXTURE_PUFFERFISH;
            }
            if (state == EntityMimicOctopus.MimicState.MIMICUBE) {
                return TEXTURE_MIMICUBE;
            }
            return TEXTURE_OVERLAY;
        }
    }
}
