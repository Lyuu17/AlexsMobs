package com.github.alexthe666.alexsmobs.renderer;

import com.github.alexthe666.alexsmobs.entity.EntityFarseer;
import com.github.alexthe666.alexsmobs.model.ModelFarseer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class RenderFarseer extends MobEntityRenderer<EntityFarseer, ModelFarseer> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/farseer/farseer.png");
    private static final Identifier TEXTURE_ANGRY = new Identifier("alexsmobs:textures/entity/farseer/farseer_angry.png");
    private static final Identifier TEXTURE_CLAWS = new Identifier("alexsmobs:textures/entity/farseer/farseer_claws.png");
    private static final Identifier TEXTURE_EYE = new Identifier("alexsmobs:textures/entity/farseer/farseer_eye.png");
    private static final Identifier TEXTURE_SCARS = new Identifier("alexsmobs:textures/entity/farseer/farseer_scars.png");
    private static final Identifier[] PORTAL_TEXTURES = new Identifier[]{
        new Identifier("alexsmobs:textures/entity/farseer/portal_0.png"),
        new Identifier("alexsmobs:textures/entity/farseer/portal_1.png"),
        new Identifier("alexsmobs:textures/entity/farseer/portal_2.png"),
        new Identifier("alexsmobs:textures/entity/farseer/portal_3.png")};
    private static final float HALF_SQRT_3 = (float)(Math.sqrt(3.0D) / 2.0D);
    private static final ModelFarseer EYE_MODEL = new ModelFarseer(0.1f);
    private static final ModelFarseer SCARS_MODEL = new ModelFarseer(0.05f);
    private static final ModelFarseer AFTERIMAGE_MODEL = new ModelFarseer(0.05f);

    public RenderFarseer(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelFarseer(0.0F), 0.9F);
        this.addFeature(new LayerOverlay());
    }

    @Override
    public boolean shouldRender(EntityFarseer livingEntityIn, Frustum camera, double camX, double camY, double camZ) {
        if (super.shouldRender(livingEntityIn, camera, camX, camY, camZ)) {
            return true;
        } else {
            if (livingEntityIn.hasLaser()) {
                var livingentity = livingEntityIn.getLaserTarget();
                if (livingentity != null) {
                    Vec3d vector3d = this.getPosition(livingentity, (double) livingentity.getHeight() * 0.5D, 1.0F);
                    Vec3d vector3d1 = this.getPosition(livingEntityIn, livingEntityIn.getStandingEyeHeight(), 1.0F);
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
    public void render(EntityFarseer entityIn, float entityYaw, float partialTicks, MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn) {
//FIXME forge
//        if (net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.RenderLivingEvent.Pre<EntityFarseer, ModelFarseer>(entityIn, this, partialTicks, matrixStackIn, bufferIn, packedLightIn)))
//            return;
        LivingEntity laserTarget = entityIn.getLaserTarget();
        float faceCameraAmount = entityIn.getFacingCameraAmount(partialTicks);
        var camera = this.dispatcher.getRotation();

        matrixStackIn.push();
        this.model.handSwingProgress = this.getHandSwingProgress(entityIn, partialTicks);

        boolean shouldSit = entityIn.hasVehicle() /* FIXME forge && (entityIn.getVehicle() != null && entityIn.getVehicle().shouldRiderSit())*/;
        this.model.riding = shouldSit;
        this.model.child = entityIn.isBaby();
        float f = MathHelper.lerpAngleDegrees(partialTicks, entityIn.prevBodyYaw, entityIn.bodyYaw);
        float f1 = MathHelper.lerpAngleDegrees(partialTicks, entityIn.prevHeadYaw, entityIn.headYaw);
        float f2 = f1 - f;
        if (shouldSit && entityIn.getVehicle() instanceof LivingEntity) {
            var livingentity = (LivingEntity) entityIn.getVehicle();
            f = MathHelper.lerpAngleDegrees(partialTicks, livingentity.prevBodyYaw, livingentity.bodyYaw);
            f2 = f1 - f;
            float f3 = MathHelper.wrapDegrees(f2);
            if (f3 < -85.0F) {
                f3 = -85.0F;
            }

            if (f3 >= 85.0F) {
                f3 = 85.0F;
            }

            f = f1 - f3;
            if (f3 * f3 > 2500.0F) {
                f += f3 * 0.2F;
            }

            f2 = f1 - f;
        }

        float f6 = MathHelper.lerp(partialTicks, entityIn.prevPitch, entityIn.getPitch());
        if (entityIn.getPose() == EntityPose.SLEEPING) {
            var direction = entityIn.getSleepingDirection();
            if (direction != null) {
                float f4 = entityIn.getEyeHeight(EntityPose.STANDING) - 0.1F;
                matrixStackIn.translate((float) (-direction.getOffsetX()) * f4, 0.0D, (float) (-direction.getOffsetZ()) * f4);
            }
        }

        float f7 = this.getAnimationProgress(entityIn, partialTicks);
        if(faceCameraAmount != 0){
            matrixStackIn.multiply(camera);
            matrixStackIn.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
        }
        this.setupTransforms(entityIn, matrixStackIn, f7, f, partialTicks);
        matrixStackIn.scale(-1.0F, -1.0F, 1.0F);
        this.scale(entityIn, matrixStackIn, partialTicks);
        matrixStackIn.translate(0.0D, -1.501F, 0.0D);
        float f8 = 0.0F;
        float f5 = 0.0F;
        if (!shouldSit && entityIn.isAlive()) {
            f8 = entityIn.limbAnimator.getPos(partialTicks);
            f5 = entityIn.limbAnimator.getPos() - entityIn.limbAnimator.getSpeed() * (1.0F - partialTicks);
            if (entityIn.isBaby()) {
                f5 *= 3.0F;
            }

            if (f8 > 1.0F) {
                f8 = 1.0F;
            }
        }

        this.model.animateModel(entityIn, f5, f8, partialTicks);
        this.model.setAngles(entityIn, f5, f8, f7, f2, f6);
        var minecraft = MinecraftClient.getInstance();
        boolean flag = this.isVisible(entityIn);
        boolean flag1 = !flag && !entityIn.isInvisibleTo(minecraft.player);
        boolean flag2 = minecraft.hasOutline(entityIn);
        var rendertype = this.getRenderLayer(entityIn, flag, flag1, flag2);
        EYE_MODEL.setAngles(entityIn, f5, f8, f7, f2, f6);
        SCARS_MODEL.setAngles(entityIn, f5, f8, f7, f2, f6);
        AFTERIMAGE_MODEL.setAngles(entityIn, f5, f8, f7, f2, f6);
        if (rendertype != null) {
            float portalLevel = entityIn.getFarseerOpacity(partialTicks);
            this.shadowRadius = 0.9F * portalLevel;
            int i = getOverlay(entityIn, this.getAnimationCounter(entityIn, partialTicks));
            this.renderFarseerModel(matrixStackIn, bufferIn, rendertype, partialTicks, packedLightIn, i, flag1 ? 0.15F : MathHelper.clamp(portalLevel, 0, 1), entityIn);
        }
        if (!entityIn.isSpectator()) {
            for (var layerrenderer : this.features) {
                layerrenderer.render(matrixStackIn, bufferIn, packedLightIn, entityIn, f5, f8, partialTicks, f7, f2, f6);
            }
        }

        matrixStackIn.pop();
        // FIXME forge
//        var renderNameplateEvent = new RenderNameTagEvent(entityIn, entityIn.getDisplayName(), this, matrixStackIn, bufferIn, packedLightIn, partialTicks);
//        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(renderNameplateEvent);
//        if (renderNameplateEvent.getResult() != net.minecraftforge.eventbus.api.Event.Result.DENY && (renderNameplateEvent.getResult() == net.minecraftforge.eventbus.api.Event.Result.ALLOW || this.shouldRenderName(entityIn))) {
//            this.renderNameTag(entityIn, renderNameplateEvent.getContent(), matrixStackIn, bufferIn, packedLightIn);
//        }
//        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.RenderLivingEvent.Post<EntityFarseer, ModelFarseer>(entityIn, this, partialTicks, matrixStackIn, bufferIn, packedLightIn));

        //emergence portal
        if(entityIn.getAnimation() == EntityFarseer.ANIMATION_EMERGE){
            matrixStackIn.push();
            matrixStackIn.scale(3.0F, 3.0F, 3.0F);
            matrixStackIn.multiply(camera);
            matrixStackIn.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
            MatrixStack.Entry posestack$pose = matrixStackIn.peek();
            Matrix4f matrix4f = posestack$pose.getPositionMatrix();
            Matrix3f matrix3f = posestack$pose.getNormalMatrix();
            int portalTexture = MathHelper.clamp(entityIn.getPortalFrame(), 0, PORTAL_TEXTURES.length - 1);
            VertexConsumer portalStatic = AMRenderLayers.createMergedVertexConsumer(bufferIn.getBuffer(AMRenderLayers.STATIC_PORTAL), bufferIn.getBuffer(RenderLayer.getEntityTranslucent(PORTAL_TEXTURES[portalTexture])));
            float portalAlpha =  entityIn.getPortalOpacity(partialTicks);
            portalVertex(portalStatic, matrix4f, matrix3f, packedLightIn, 0.0F, 0, 0, 1, portalAlpha);
            portalVertex(portalStatic, matrix4f, matrix3f, packedLightIn, 1.0F, 0, 1, 1, portalAlpha);
            portalVertex(portalStatic, matrix4f, matrix3f, packedLightIn, 1.0F, 1, 1, 0, portalAlpha);
            portalVertex(portalStatic, matrix4f, matrix3f, packedLightIn, 0.0F, 1, 0, 0, portalAlpha);
            matrixStackIn.pop();
        }
        //laser target
        if(entityIn.hasLaser() && laserTarget != null && !laserTarget.isRemoved()){
            float laserProgress = (entityIn.prevLaserLvl + (entityIn.getLaserAttackLvl() - entityIn.prevLaserLvl) * partialTicks) / (float)EntityFarseer.LASER_ATTACK_DURATION;
            float laserHeight = entityIn.getStandingEyeHeight();
            //float angryProgress = entityIn.prevAngryProgress + (entityIn.angryProgress - entityIn.prevAngryProgress) * partialTicks;
            var angryShake = Vec3d.ZERO;
            double d0 = MathHelper.lerp(partialTicks, laserTarget.prevX, laserTarget.getX()) - MathHelper.lerp(partialTicks, entityIn.prevX, entityIn.getX()) - angryShake.x;
            double d1 = MathHelper.lerp(partialTicks, laserTarget.prevY, laserTarget.getY()) + laserTarget.getStandingEyeHeight() - MathHelper.lerp(partialTicks, entityIn.prevY, entityIn.getY()) - angryShake.y - laserHeight;
            double d2 = MathHelper.lerp(partialTicks, laserTarget.prevZ, laserTarget.getZ()) - MathHelper.lerp(partialTicks, entityIn.prevZ, entityIn.getZ()) - angryShake.z;
            double d4 = Math.sqrt(d0 * d0 + d2 * d2);
            float laserY = (float) (MathHelper.atan2(d2, d0) * (double) MathHelper.DEGREES_PER_RADIAN) - 90.0F;
            float laserX = (float) (-(MathHelper.atan2(d1, d4) * (double) MathHelper.DEGREES_PER_RADIAN));
            VertexConsumer beamStatic = bufferIn.getBuffer(AMRenderLayers.getFarseerBeam());
            matrixStackIn.push();
            matrixStackIn.translate(0, laserHeight, 0);
            matrixStackIn.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(laserY));
            matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(laserX));
            matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
            float length = entityIn.getLaserDistance() * laserProgress;
            float width = (1.5F - laserProgress) * 2F;
            float speed = 1F + laserProgress * laserProgress * 5F;
            MatrixStack.Entry posestack$pose = matrixStackIn.peek();
            Matrix4f matrix4f = posestack$pose.getPositionMatrix();
            Matrix3f matrix3f = posestack$pose.getNormalMatrix();
            int j = 255;
            //apparently its static? should be moving so has to be moved manually through UV
            long systemTime = Util.getMeasuringTimeMs() * 7L;
            float u = (float)(systemTime % 30000L) / 30000.0F;
            float v = (float)Math.floor((systemTime % 3000L) / 3000.0F * 4.0F) * 0.25F + (float)Math.sin(systemTime / 30000F) * 0.05F + ((float)(systemTime % 20000L) / 20000.0F * speed);
            laserOriginVertex(beamStatic, matrix4f, matrix3f, j, u, v);
            laserLeftCornerVertex(beamStatic, matrix4f, matrix3f, length, width,  u, v);
            laserRightCornerVertex(beamStatic, matrix4f, matrix3f, length, width,  u, v);
            laserLeftCornerVertex(beamStatic, matrix4f, matrix3f, length, width,  u, v);

            matrixStackIn.pop();
        }

    }

    private void renderFarseerModel(MatrixStack matrixStackIn, VertexConsumerProvider source, RenderLayer defRenderType, float partialTicks, int packedLightIn, int overlayColors, float alphaIn, EntityFarseer entityIn) {
        if(entityIn.hasLaser()){
            VertexConsumer staticyInsides = AMRenderLayers.createMergedVertexConsumer(source.getBuffer(AMRenderLayers.STATIC_ENTITY), source.getBuffer(RenderLayer.getEntityTranslucent(TEXTURE_EYE)));
            EYE_MODEL.render(matrixStackIn, staticyInsides, packedLightIn, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1F);
        }
        VertexConsumer consumer;
        float hurt = Math.max(entityIn.hurtTime, entityIn.deathTime);
        float defAlpha = alphaIn * 0.2F;
        float afterimageSpeed = 0.3F;
        if(hurt > 0){
            afterimageSpeed = Math.min(hurt / 20F, 1F) + 0.3F;
            VertexConsumer staticyScars = AMRenderLayers.createMergedVertexConsumer(source.getBuffer(AMRenderLayers.STATIC_ENTITY), source.getBuffer(RenderLayer.getEntityTranslucent(TEXTURE_SCARS)));
            SCARS_MODEL.render(matrixStackIn, staticyScars, packedLightIn, overlayColors, 1.0F, 1.0F, 1.0F, 0.3F);
        }
        this.model.render(matrixStackIn, source.getBuffer(defRenderType), packedLightIn, overlayColors, 1.0F, 1.0F, 1.0F, alphaIn);

        matrixStackIn.push();
        matrixStackIn.pop();

        AFTERIMAGE_MODEL.eye.showModel = false;
        var afterimage = RenderLayer.getEntityTranslucentEmissive(this.getTexture(entityIn));
        var colorOffset = entityIn.getLatencyOffsetVec(10, partialTicks).multiply(-0.2F).add(entityIn.angryShakeVec.multiply(0.3F));
        var redOffset = colorOffset.add(entityIn.calculateAfterimagePos(partialTicks, false, afterimageSpeed));
        var blueOffset = colorOffset.add(entityIn.calculateAfterimagePos(partialTicks, true, afterimageSpeed));
        float scale = (float) MathHelper.clamp(colorOffset.length() * 0.1F, 0, 1F);
        float angryProgress = entityIn.prevAngryProgress + (entityIn.angryProgress - entityIn.prevAngryProgress) * partialTicks;
        float afterimageAlpha1 = defAlpha * Math.max(((float) Math.sin((entityIn.age + partialTicks) * 0.2F) + 1F) * 0.3F, angryProgress * 0.2F);
        float afterimageAlpha2 = defAlpha * Math.max(((float) Math.cos((entityIn.age + partialTicks) * 0.2F) + 1F) * 0.3F, angryProgress * 0.2F);

        matrixStackIn.push();
        matrixStackIn.scale(scale + 1F, scale + 1F, scale + 1F);
        matrixStackIn.push();
        matrixStackIn.translate(redOffset.x, redOffset.y, redOffset.z);
        AFTERIMAGE_MODEL.render(matrixStackIn, source.getBuffer(afterimage), 240, overlayColors, 1.0F, 0F, 0F, afterimageAlpha1);
        matrixStackIn.pop();
        matrixStackIn.push();
        matrixStackIn.translate(blueOffset.x, blueOffset.y, blueOffset.z);
        AFTERIMAGE_MODEL.render(matrixStackIn, source.getBuffer(afterimage), 240, overlayColors, 0F, 0F, 1.0F, afterimageAlpha2);
        matrixStackIn.pop();
        matrixStackIn.pop();
        AFTERIMAGE_MODEL.eye.showModel = true;
    }

    private static void laserOriginVertex(VertexConsumer p_114220_, Matrix4f p_114221_, Matrix3f p_114092_, int p_114222_, float xOffset, float yOffset) {
        p_114220_.vertex(p_114221_, 0.0F, 0.0F, 0.0F).color(255, 255, 255, 255).texture(xOffset + 0.5F, yOffset).overlay(OverlayTexture.DEFAULT_UV).light(240).normal(p_114092_, 0.0F, 1.0F, 0.0F).next();
    }

    private static void laserLeftCornerVertex(VertexConsumer p_114215_, Matrix4f p_114216_, Matrix3f p_114092_, float p_114217_, float p_114218_, float xOffset, float yOffset) {
        p_114215_.vertex(p_114216_, -HALF_SQRT_3 * p_114218_, p_114217_, 0).color(255, 255, 255, 0).texture(xOffset, yOffset + 1).overlay(OverlayTexture.DEFAULT_UV).light(240).normal(p_114092_, 0.0F, -1.0F, 0.0F).next();
    }

    private static void laserRightCornerVertex(VertexConsumer p_114224_, Matrix4f p_114225_, Matrix3f p_114092_, float p_114226_, float p_114227_, float xOffset, float yOffset) {
        p_114224_.vertex(p_114225_, HALF_SQRT_3 * p_114227_, p_114226_, 0).color(255, 255, 255, 0).texture(xOffset + 1, yOffset + 1).overlay(OverlayTexture.DEFAULT_UV).light(240).normal(p_114092_, 0.0F, -1.0F, 0.0F).next();
    }

    private static void portalVertex(VertexConsumer p_114090_, Matrix4f p_114091_, Matrix3f p_114092_, int p_114093_, float p_114094_, int p_114095_, int p_114096_, int p_114097_, float alpha) {
        p_114090_.vertex(p_114091_, p_114094_ - 0.5F, (float)p_114095_ - 0.25F, 0.0F).color(1F, 1F, 1F,  alpha).texture((float)p_114096_, (float)p_114097_).overlay(OverlayTexture.DEFAULT_UV).light(240).normal(p_114092_, 0.0F, -1.0F, 0.0F).next();
    }

    @Override
    protected void setupTransforms(EntityFarseer farseer, MatrixStack matrixStackIn, float f1, float f2, float f3) {
        float invCameraAmount = 1F - farseer.getFacingCameraAmount(MinecraftClient.getInstance().getTickDelta());

        if (this.isShaking(farseer)) {
            f2 += (float)(Math.cos((double)farseer.age * 3.25D) * Math.PI * (double)0.4F);
        }

        if (!farseer.isInPose(EntityPose.SLEEPING)) {
            matrixStackIn.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((180.0F - f2 * invCameraAmount)));
        }

        if (farseer.deathTime > 0) {
            float f = ((float)farseer.deathTime + f3 - 1.0F) / 20.0F * 1.6F;
            f = MathHelper.sqrt(f);
            if (f > 1.0F) {
                f = 1.0F;
            }

            matrixStackIn.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(f * this.getLyingAngle(farseer) * invCameraAmount) );
        } else if (shouldFlipUpsideDown(farseer)) {
            matrixStackIn.translate(0.0D, (double)(farseer.getHeight() + 0.1F), 0.0D);
            matrixStackIn.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
        }

    }

    @Nullable
    @Override
    protected RenderLayer getRenderLayer(EntityFarseer farseer, boolean normal, boolean invis, boolean outline) {
        Identifier Identifier = this.getTexture(farseer);
        if (invis || farseer.getAnimation() == EntityFarseer.ANIMATION_EMERGE) {
            return RenderLayer.getItemEntityTranslucentCull(Identifier);
        } else if (normal) {
            return this.model.getLayer(Identifier);
        } else {
            return outline ? RenderLayer.getOutline(Identifier) : null;
        }
    }

    @Override
    public Identifier getTexture(EntityFarseer entity) {
        return entity.isAngry() ? TEXTURE_ANGRY : TEXTURE;
    }

    class LayerOverlay extends FeatureRenderer<EntityFarseer, ModelFarseer> {

        public LayerOverlay() {
            super(RenderFarseer.this);
        }

        public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntityFarseer entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            if (entitylivingbaseIn.getAnimation() == EntityFarseer.ANIMATION_EMERGE) {
                var ivertexbuilder = bufferIn.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE_CLAWS));
                this.getContextModel().render(matrixStackIn, ivertexbuilder, packedLightIn, LivingEntityRenderer.getOverlay(entitylivingbaseIn, 0.0F), 1.0F, 1.0F, 1.0F, 1.0F);
            }

        }
    }
}
