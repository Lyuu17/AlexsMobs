package com.github.alexthe666.alexsmobs.renderer;

import com.github.alexthe666.alexsmobs.entity.EntityLaviathan;
import com.github.alexthe666.alexsmobs.entity.EntityLaviathanPart;
import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import com.github.alexthe666.alexsmobs.model.ModelLaviathan;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import org.joml.Matrix4f;

public class RenderLaviathan extends MobEntityRenderer<EntityLaviathan, ModelLaviathan> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/laviathan.png");
    private static final Identifier TEXTURE_GLOW = new Identifier("alexsmobs:textures/entity/laviathan_glow.png");
    private static final Identifier TEXTURE_OBSIDIAN = new Identifier("alexsmobs:textures/entity/laviathan_obsidian.png");
    private static final Identifier TEXTURE_GEAR = new Identifier("alexsmobs:textures/entity/laviathan_gear.png");
    private static final Identifier TEXTURE_HELMET = new Identifier("alexsmobs:textures/entity/laviathan_helmet.png");
    private static final float REINS_COLOR_R = 98F / 255F;
    private static final float REINS_COLOR_G = 77F / 255F;
    private static final float REINS_COLOR_B = 52F / 255F;
    private static final float REINS_COLOR_R2 = 58F / 255F;
    private static final float REINS_COLOR_G2 = 40F / 255F;
    private static final float REINS_COLOR_B2 = 34F / 255F;
    public static boolean renderWithoutShaking = false;

    public RenderLaviathan(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelLaviathan(), 4.0F);
        this.addFeature(new LayerOverlays(this));
    }

    private static void addVertexPairAlex(VertexConsumer p_174308_, Matrix4f p_174309_, float p_174310_, float p_174311_, float p_174312_, int p_174313_, int p_174314_, int p_174315_, int p_174316_, float p_174317_, float p_174318_, float p_174319_, float p_174320_, int p_174321_, boolean p_174322_) {
        float f = (float) p_174321_ / 24.0F;
        int i = (int) MathHelper.lerp(f, (float) p_174313_, (float) p_174314_);
        int j = (int) MathHelper.lerp(f, (float) p_174315_, (float) p_174316_);
        int k = LightmapTextureManager.pack(i, j);
        float f2 = REINS_COLOR_R;
        float f3 = REINS_COLOR_G;
        float f4 = REINS_COLOR_B;
        if (p_174321_ % 2 == (p_174322_ ? 1 : 0)) {
            f2 = REINS_COLOR_R2;
            f3 = REINS_COLOR_G2;
            f4 = REINS_COLOR_B2;
        }
        float f5 = p_174310_ * f;
        float f6 = p_174311_ > 0.0F ? p_174311_ * f * f : p_174311_ - p_174311_ * (1.0F - f) * (1.0F - f);
        float f7 = p_174312_ * f;
        p_174308_.vertex(p_174309_, f5 - p_174319_, f6 + p_174318_, f7 + p_174320_).color(f2, f3, f4, 1.0F).light(k).next();
        p_174308_.vertex(p_174309_, f5 + p_174319_, f6 + p_174317_ - p_174318_, f7 - p_174320_).color(f2, f3, f4, 1.0F).light(k).next();
    }

    @Override
    public boolean shouldRender(EntityLaviathan livingEntityIn, Frustum camera, double camX, double camY, double camZ) {
        if (super.shouldRender(livingEntityIn, camera, camX, camY, camZ)) {
            return true;
        } else {
            for (EntityLaviathanPart part : livingEntityIn.allParts) {
                if (camera.isVisible(part.getBoundingBox())) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public void render(EntityLaviathan mob, float p_115456_, float partialTick, MatrixStack ms, VertexConsumerProvider p_115459_, int p_115460_) {
        super.render(mob, p_115456_, partialTick, ms, p_115459_, p_115460_);
        Entity entity = mob.getControllingPassenger();
        if (entity != null) {
            double d0 = MathHelper.lerp(partialTick, mob.lastRenderX, mob.getX());
            double d1 = MathHelper.lerp(partialTick, mob.lastRenderY, mob.getY());
            double d2 = MathHelper.lerp(partialTick, mob.lastRenderZ, mob.getZ());
            ms.push();
            ms.translate(-d0, -d1, -d2);
            this.renderRein(mob, partialTick, ms, p_115459_, entity, true);
            this.renderRein(mob, partialTick, ms, p_115459_, entity, false);
            ms.pop();
        }
    }

    @Override
    protected void scale(EntityLaviathan entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
    }

    @Override
    protected boolean isShaking(EntityLaviathan entity) {
        return entity.isWet() && !entity.isObsidian() && !renderWithoutShaking;
    }

    @Override
    public Identifier getTexture(EntityLaviathan entity) {
        return entity.isObsidian() ? TEXTURE_OBSIDIAN : TEXTURE;
    }

    private float getHeadShakeForReins(EntityLaviathan mob, float partialTick) {
        float hh1 = mob.prevHeadHeight;
        float hh2 = mob.getHeadHeight();
        float rawHeadHeight = (hh1 + (hh2 - hh1) * partialTick) / 3F;
        float clampedNeckRot = MathHelper.clamp(-rawHeadHeight, -1, 1);
        float headStillProgress = 1F - Math.abs(clampedNeckRot);
        float swim = MathHelper.lerp(partialTick, mob.prevSwimProgress, mob.swimProgress);
        float limbSwingAmount = mob.limbAnimator.getSpeed(partialTick);
        float swing = mob.limbAnimator.getPos() + partialTick;
        float swingAmount = limbSwingAmount * swim * 0.2F * headStillProgress;
        float swimSpeed = mob.swimProgress >= 5F ? 0.3F : 0.9F;
        float swimDegree = 0.5F + swim * 0.05F;
        float boxOffset = (float) (-21 * 3.141592653589793D / (double) (2 * 3));
        float moveScale = 1;
        return 1.3F * MathHelper.cos(swing * swimSpeed * moveScale + boxOffset * (float) 2) * swingAmount * swimDegree * moveScale;
    }

    private float getHeadBobForReins(EntityLaviathan mob, float partialTick) {
        float swing = mob.age + partialTick;
        float swingAmount = 1.0F;
        float idleSpeed = 0.04f;
        float idleDegree = 0.3f;
        float boxOffset = (float) (9 * 3.141592653589793D / (double) (2 * 3));
        float moveScale = 1;
        return 0.8F * MathHelper.cos(swing * idleSpeed * moveScale + boxOffset * (float) 2) * swingAmount * idleDegree * moveScale;
    }

    private <E extends Entity> void renderRein(EntityLaviathan mob, float partialTick, MatrixStack p_115464_, VertexConsumerProvider p_115465_, E rider, boolean left) {
        p_115464_.push();
        Entity head = mob.headPart;
        if (head == null) {
            return;
        }
        float limbSwingAmount = mob.limbAnimator.getSpeed(partialTick);
        float shake = getHeadShakeForReins(mob, partialTick);
        float headYaw = Math.abs(mob.getHeadYaw(partialTick)) / 50F;
        float headPitch = 1F - Math.abs((mob.prevHeadHeight + (mob.getHeadHeight() - mob.prevHeadHeight) * partialTick) / 3F);
        float yawAdd = (1F - headYaw) * 0.4F * (1F - limbSwingAmount * 0.7F) - headPitch * 0.2F;
        Vec3d vec3 = rider instanceof LivingEntity ? getReinPosition((LivingEntity) rider, partialTick, left, shake) : rider.getLeashPos(partialTick);
        double d0 = (double) (MathHelper.lerp(partialTick, mob.bodyYaw, mob.prevBodyYaw) * MathHelper.RADIANS_PER_DEGREE) + (Math.PI / 2D);
        Vec3d vec31 = new Vec3d((left ? -0.05F - yawAdd : 0.05F + yawAdd) + shake, 0.45F - headYaw * 0.2F + getHeadBobForReins(mob, partialTick), 0.1F);
        double d1 = Math.cos(d0) * vec31.z + Math.sin(d0) * vec31.x;
        double d2 = Math.sin(d0) * vec31.z - Math.cos(d0) * vec31.x;
        double d3 = MathHelper.lerp(partialTick, head.prevX, head.getX()) + d1;
        double d4 = MathHelper.lerp(partialTick, head.prevY, head.getY()) + vec31.y;
        double d5 = MathHelper.lerp(partialTick, head.prevZ, head.getZ()) + d2;
        p_115464_.translate(d3, d4, d5);
        float f = (float) (vec3.x - d3);
        float f1 = (float) (vec3.y - d4);
        float f2 = (float) (vec3.z - d5);
        //float f3 = 0.025F;
        VertexConsumer vertexconsumer = p_115465_.getBuffer(RenderLayer.getLeash());
        Matrix4f matrix4f = p_115464_.peek().getPositionMatrix();
        float f4 = (float) (MathHelper.fastInverseSqrt(f * f + f2 * f2) * 0.025F / 2.0F);
        float f5 = f2 * f4;
        float f6 = f * f4;
        var blockpos = AMBlockPos.fromVec3(mob.getCameraPosVec(partialTick));
        var blockpos1 = AMBlockPos.fromVec3(rider.getCameraPosVec(partialTick));
        int i = this.getBlockLight(mob, blockpos);
        int j = mob.getWorld().getLightLevel(LightType.BLOCK, blockpos1);
        int k = mob.getWorld().getLightLevel(LightType.SKY, blockpos);
        int l = mob.getWorld().getLightLevel(LightType.SKY, blockpos1);
        float width = 0.05F;
        for (int i1 = 0; i1 <= 24; ++i1) {
            addVertexPairAlex(vertexconsumer, matrix4f, f, f1, f2, i, j, k, l, width, width, f5, f6, i1, false);
        }
        for (int j1 = 24; j1 >= 0; --j1) {
            addVertexPairAlex(vertexconsumer, matrix4f, f, f1, f2, i, j, k, l, width, width, f5, f6, j1, true);
        }
        p_115464_.pop();
    }

    private Vec3d getReinPosition(LivingEntity entity, float p_36374_, boolean left, float shake) {
        double d0 = 0.4D * (left ? -1.0D : 1.0D) - 0;
        float f = MathHelper.lerp(p_36374_ * 0.5F, entity.getPitch(), entity.prevPitch) * MathHelper.RADIANS_PER_DEGREE;
        float f1 = MathHelper.lerp(p_36374_, entity.prevBodyYaw, entity.bodyYaw) * MathHelper.RADIANS_PER_DEGREE;
        if (!entity.isFallFlying() && !entity.isUsingRiptide()) {
            if (entity.isInSwimmingPose()) {
                return entity.getLerpedPos(p_36374_).add((new Vec3d(d0, 0.3D, -0.34D)).rotateX(-f).rotateY(-f1));
            } else {
                double d5 = entity.getBoundingBox().getYLength() - 1.0D;
                double d6 = entity.isInSneakingPose() ? -0.2D : 0.07D;
                return entity.getLerpedPos(p_36374_).add((new Vec3d(d0, d5, d6)).rotateY(-f1));
            }
        } else {
            Vec3d vec3 = entity.getRotationVec(p_36374_);
            Vec3d vec31 = entity.getVelocity();
            double d1 = vec31.horizontalLengthSquared();
            double d2 = vec3.horizontalLengthSquared();
            float f2;
            if (d1 > 0.0D && d2 > 0.0D) {
                double d3 = (vec31.x * vec3.x + vec31.z * vec3.z) / Math.sqrt(d1 * d2);
                double d4 = vec31.x * vec3.z - vec31.z * vec3.x;
                f2 = (float) (Math.signum(d4) * Math.acos(d3));
            } else {
                f2 = 0.0F;
            }

            return entity.getLerpedPos(p_36374_).add((new Vec3d(d0, -0.11D, 0.85D)).rotateZ(-f2).rotateX(-f).rotateY(-f1));
        }
    }

    static class LayerOverlays extends FeatureRenderer<EntityLaviathan, ModelLaviathan> {

        public LayerOverlays(RenderLaviathan render) {
            super(render);
        }

        @Override
        public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntityLaviathan laviathan, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            if (!laviathan.isObsidian()) {
                var ivertexbuilder = bufferIn.getBuffer(RenderLayer.getEyes(TEXTURE_GLOW));
                this.getContextModel().render(matrixStackIn, ivertexbuilder, packedLightIn, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);
            }
            if (laviathan.hasBodyGear()) {
                var ivertexbuilder = bufferIn.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE_GEAR));
                this.getContextModel().render(matrixStackIn, ivertexbuilder, packedLightIn, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);
            }
            if (laviathan.hasHeadGear()) {
                var ivertexbuilder = bufferIn.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE_HELMET));
                this.getContextModel().render(matrixStackIn, ivertexbuilder, packedLightIn, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);
            }
        }

    }
}
