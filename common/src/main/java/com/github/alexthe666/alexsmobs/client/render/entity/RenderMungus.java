package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.entity.EntityMungus;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.github.alexthe666.alexsmobs.client.model.ModelMungus;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class RenderMungus extends MobEntityRenderer<EntityMungus, ModelMungus> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/mungus.png");
    private static final Identifier BEAM_TEXTURE = new Identifier("alexsmobs:textures/entity/mungus_beam.png");
    private static final Identifier TEXTURE_BEAM_OVERLAY = new Identifier("alexsmobs:textures/entity/mungus_beam_overlay.png");
    private static final Identifier TEXTURE_SACK_OVERLAY = new Identifier("alexsmobs:textures/entity/mungus_sack.png");
    private static final Identifier TEXTURE_SHOES = new Identifier("alexsmobs:textures/entity/mungus_shoes.png");
    private static final RenderLayer beamType = AMRenderLayers.getEyesNoFog(BEAM_TEXTURE);

    public RenderMungus(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelMungus(0), 0.5F);
        this.addFeature(new MungusSackLayer(this));
        this.addFeature(new MungusMushroomLayer(this));
    }

    @Override
    protected boolean isShaking(EntityMungus mungus) {
        return mungus.isReverting();
    }

    private static void vertex(VertexConsumer p_229108_0_, Matrix4f p_229108_1_, Matrix3f p_229108_2_, float p_229108_3_, float p_229108_4_, float p_229108_5_, int p_229108_6_, int p_229108_7_, int p_229108_8_, float p_229108_9_, float p_229108_10_) {
        p_229108_0_.vertex(p_229108_1_, p_229108_3_, p_229108_4_, p_229108_5_).color(p_229108_6_, p_229108_7_, p_229108_8_, 255).texture(p_229108_9_, p_229108_10_).overlay(OverlayTexture.DEFAULT_UV).light(240).normal(p_229108_2_, 0.0F, 1.0F, 0.0F).next();
    }

    @Override
    protected void setupTransforms(EntityMungus entityLiving, MatrixStack matrixStackIn, float ageInTicks, float rotationYaw, float partialTicks) {
        if (entityLiving.deathTime > 0) {
            matrixStackIn.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - rotationYaw));
            float f = ((float) entityLiving.deathTime + partialTicks - 1.0F) / 20.0F * 1.6F;
            f = MathHelper.sqrt(f);
            if (f > 1.0F) {
                f = 1.0F;
            }
            matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(f * -90));
        } else {
            super.setupTransforms(entityLiving, matrixStackIn, ageInTicks, rotationYaw, partialTicks);
        }
    }

    @Override
    protected float getLyingAngle(EntityMungus p_77037_1_) {
        return 0F;
    }

    @Override
    protected void scale(EntityMungus entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
        String s = Formatting.strip(entitylivingbaseIn.getName().getString());
        if (s != null && s.toLowerCase().contains("drip")) {
            matrixStackIn.translate(0F,  entitylivingbaseIn.isBaby() ? -0.075F : -0.15F, 0F);
        }
    }

    @Override
    public boolean shouldRender(EntityMungus livingEntityIn, Frustum camera, double camX, double camY, double camZ) {
        if (super.shouldRender(livingEntityIn, camera, camX, camY, camZ)) {
            return true;
        } else {
            if (livingEntityIn.getBeamTarget() != null) {
                var pos = livingEntityIn.getBeamTarget();
                if (pos != null) {
                    var vector3d = Vec3d.of(pos);
                    var vector3dCorner = Vec3d.of(pos).add(1, 1, 1);
                    var vector3d1 = this.getPosition(livingEntityIn, livingEntityIn.getStandingEyeHeight(), 1.0F);
                    return camera.isVisible(new Box(vector3d1.x, vector3d1.y, vector3d1.z, vector3d.x, vector3d.y, vector3d.z))
                            || camera.isVisible(new Box(vector3d1.x, vector3d1.y, vector3d1.z, vector3dCorner.x, vector3dCorner.y, vector3dCorner.z));
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
    public void render(EntityMungus entityIn, float entityYaw, float partialTicks, MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn) {
        super.render(entityIn, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
        var target = entityIn.getBeamTarget();
        if (target != null) {
            float f = 1.0F;
            float f1 = (float) entityIn.getWorld().getTime() + partialTicks;
            float f2 = -1.0F * (f1 * 0.15F % 1.0F);
            float f3 = 1.13F;
            if(entityIn.isBaby()){
                f3 = 0.555F;
            }
            matrixStackIn.push();
            matrixStackIn.translate(0.0D, f3, 0.0D);
            var vector3d = Vec3d.ofCenter(target, 0.15F);
            var vector3d1 = this.getPosition(entityIn, f3, partialTicks);
            var vector3d2 = vector3d.subtract(vector3d1);
            float f4 = (float) (vector3d2.length());
            vector3d2 = vector3d2.normalize();
            float f5 = (float) Math.acos(vector3d2.y);
            float f6 = (float) Math.atan2(vector3d2.z, vector3d2.x);
            matrixStackIn.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(((MathHelper.PI / 2F) - f6) * MathHelper.DEGREES_PER_RADIAN));
            matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(f5 * MathHelper.DEGREES_PER_RADIAN));
            int i = 1;
            float f7 = f1 * 0.05F * 1.5F;
            float f8 = 1F;
            int j = (int) (f8 * 255.0F);
            int k = (int) (f8 * 255.0F);
            int l = (int) (f8 * 255.0F);
            float f9 = 0.2F;
            float f10 = 0.282F;
            float f11 = MathHelper.cos(0 + 2.3561945F) * 0.8F;
            float f12 = MathHelper.sin(0 + 2.3561945F) * 0.8F;
            float f13 = MathHelper.cos(0 + Maths.QUARTER_PI) * 0.8F;
            float f14 = MathHelper.sin(0 + Maths.QUARTER_PI) * 0.8F;
            float f15 = MathHelper.cos(0 + 3.926991F) * 0.8F;
            float f16 = MathHelper.sin(0 + 3.926991F) * 0.8F;
            float f17 = MathHelper.cos(0 + 5.4977875F) * 0.8F;
            float f18 = MathHelper.sin(0 + 5.4977875F) * 0.8F;
            float f19 = MathHelper.cos(0 + MathHelper.PI) * 0.4F;
            float f20 = MathHelper.sin(0 + MathHelper.PI) * 0.4F;
            float f21 = MathHelper.cos(0 + 0.0F) * 0.4F;
            float f22 = MathHelper.sin(0 + 0.0F) * 0.4F;
            float f23 = MathHelper.cos(0 + (MathHelper.PI / 2F)) * 0.4F;
            float f24 = MathHelper.sin(0 + (MathHelper.PI / 2F)) * 0.4F;
            float f25 = MathHelper.cos(0 + (MathHelper.PI * 1.5F)) * 0.4F;
            float f26 = MathHelper.sin(0 + (MathHelper.PI * 1.5F)) * 0.4F;
            float f27 = 0.0F;
            float f28 = 0.4999F;
            float f29 = -1.0F + f2;
            float f30 = f4 * 0.5F + f29;
            VertexConsumer ivertexbuilder = bufferIn.getBuffer(beamType);
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
            if (entityIn.age % 4 > 1) {
                f31 = 0.5F;
            }

            vertex(ivertexbuilder, matrix4f, matrix3f, f11, f4, f12, j, k, l, 0.5F, f31 + 0.5F);
            vertex(ivertexbuilder, matrix4f, matrix3f, f13, f4, f14, j, k, l, 1.0F, f31 + 0.5F);
            vertex(ivertexbuilder, matrix4f, matrix3f, f17, f4, f18, j, k, l, 1.0F, f31);
            vertex(ivertexbuilder, matrix4f, matrix3f, f15, f4, f16, j, k, l, 0.5F, f31);
            matrixStackIn.pop();
        }

    }

    @Override
    public Identifier getTexture(EntityMungus entity) {
        return TEXTURE;
    }

    static class MungusSackLayer extends FeatureRenderer<EntityMungus, ModelMungus> {

        public MungusSackLayer(RenderMungus p_i50928_1_) {
            super(p_i50928_1_);
        }

        @Override
        public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntityMungus entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            VertexConsumer lead = bufferIn.getBuffer(AMRenderLayers.getEyesFlickering(TEXTURE_SACK_OVERLAY, 0));
            float alpha = 0.75F + (MathHelper.cos(ageInTicks * 0.2F) + 1F) * 0.125F;
            this.getContextModel().render(matrixStackIn, lead, 240, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, alpha);
            if (entitylivingbaseIn.getBeamTarget() != null) {
                VertexConsumer beam = bufferIn.getBuffer(AMRenderLayers.getGhost(TEXTURE_BEAM_OVERLAY));
                float beamAlpha = 0.75F + (MathHelper.cos(ageInTicks * 1) + 1F) * 0.125F;
                this.getContextModel().render(matrixStackIn, beam, 240, LivingEntityRenderer.getOverlay(entitylivingbaseIn, 0), 1.0F, 1.0F, 1.0F, beamAlpha);
            }
            String s = Formatting.strip(entitylivingbaseIn.getName().getString());
            if (s != null && s.toLowerCase().contains("drip")) {
                VertexConsumer shoeBuffer = bufferIn.getBuffer(AMRenderLayers.getEntityCutoutNoCull(TEXTURE_SHOES));
                matrixStackIn.push();
                this.getContextModel().renderShoes();
                this.getContextModel().render(matrixStackIn, shoeBuffer, packedLightIn, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);
                this.getContextModel().postRenderShoes();
                matrixStackIn.pop();
            }
        }
    }

    static class MungusMushroomLayer extends FeatureRenderer<EntityMungus, ModelMungus> {

        public MungusMushroomLayer(RenderMungus p_i50928_1_) {
            super(p_i50928_1_);
        }

        @Override
        public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntityMungus entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            var blockrendererdispatcher = MinecraftClient.getInstance().getBlockRenderManager();
            var blockstate = entitylivingbaseIn.getMushroomState();
            if (blockstate == null) {
                return;
            }
            int i = LivingEntityRenderer.getOverlay(entitylivingbaseIn, 0.0F);
            boolean altOrder = entitylivingbaseIn.isAltOrderMushroom();
            int mushroomCount = entitylivingbaseIn.getMushroomCount();
            matrixStackIn.push();
            if (entitylivingbaseIn.isBaby()) {
                matrixStackIn.scale(0.5F, 0.5F, 0.5F);
                matrixStackIn.translate(0.0D, 1.5D, 0D);
            }
            matrixStackIn.push();
            translateToBody(matrixStackIn);
            if (mushroomCount == 1 && !altOrder || mushroomCount >= 2) {
                matrixStackIn.push();
                matrixStackIn.translate(0.2F, -1.4F, 0.15D);
                matrixStackIn.scale(-1.0F, -1.0F, 1.0F);
                matrixStackIn.translate(-0.5D, -0.5D, -0.5D);
                blockrendererdispatcher.renderBlockAsEntity(blockstate, matrixStackIn, bufferIn, packedLightIn, i);
                matrixStackIn.pop();
            }
            if (mushroomCount == 1 && altOrder || mushroomCount >= 2) {
                matrixStackIn.push();
                matrixStackIn.translate(-0.2F, -1.5F, -0.2D);
                matrixStackIn.scale(-1.0F, -1.0F, 1.0F);
                matrixStackIn.translate(-0.5D, -0.5D, -0.5D);
                blockrendererdispatcher.renderBlockAsEntity(blockstate, matrixStackIn, bufferIn, packedLightIn, i);
                matrixStackIn.pop();
            }
            if (mushroomCount >= 3) {
                matrixStackIn.push();
                matrixStackIn.translate(0.76F, -0.4F, 0.1D);
                matrixStackIn.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90F));
                matrixStackIn.scale(-1.0F, -1.0F, 1.0F);
                matrixStackIn.translate(-0.5D, -0.5D, -0.5D);
                blockrendererdispatcher.renderBlockAsEntity(blockstate, matrixStackIn, bufferIn, packedLightIn, i);
                matrixStackIn.pop();
            }
            if (mushroomCount >= 4) {
                matrixStackIn.push();
                matrixStackIn.translate(-0.76F, -1.0F, 0.1D);
                matrixStackIn.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-60F));
                matrixStackIn.scale(-1.0F, -1.0F, 1.0F);
                matrixStackIn.translate(-0.5D, -0.5D, -0.5D);
                blockrendererdispatcher.renderBlockAsEntity(blockstate, matrixStackIn, bufferIn, packedLightIn, i);
                matrixStackIn.pop();
            }
            if (mushroomCount >= 5) {
                matrixStackIn.push();
                matrixStackIn.translate(-0.76F, -0.1F, 0.1D);
                matrixStackIn.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-100F));
                matrixStackIn.scale(-1.0F, -1.0F, 1.0F);
                matrixStackIn.translate(-0.5D, -0.5D, -0.5D);
                blockrendererdispatcher.renderBlockAsEntity(blockstate, matrixStackIn, bufferIn, packedLightIn, i);
                matrixStackIn.pop();
            }
            matrixStackIn.pop();
            matrixStackIn.pop();

        }

        protected void translateToBody(MatrixStack matrixStack) {
            this.getContextModel().root.translateAndRotate(matrixStack);
            this.getContextModel().body.translateAndRotate(matrixStack);
        }
    }

}
