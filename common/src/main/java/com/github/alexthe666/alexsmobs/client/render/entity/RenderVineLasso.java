package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.entity.EntityVineLasso;
import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class RenderVineLasso extends EntityRenderer<EntityVineLasso> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/vine_lasso.png");

    private static final float VINES_COLOR_R = 96F / 255F;
    private static final float VINES_COLOR_G = 143F / 255F;
    private static final float VINES_COLOR_B = 62F / 255F;
    private static final float VINES_COLOR_R2 = 166F / 255F;
    private static final float VINES_COLOR_G2 = 191F / 255F;
    private static final float VINES_COLOR_B2 = 97F / 255F;


    public RenderVineLasso(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn);
    }

    @Override
    public void render(EntityVineLasso entityIn, float entityYaw, float partialTicks, MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn) {
        matrixStackIn.push();
        matrixStackIn.translate(0.0D, 0.25F, 0.0D);
        matrixStackIn.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(MathHelper.lerp(partialTicks, entityIn.prevYaw, entityIn.getYaw()) - 180F));
        matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(MathHelper.lerp(partialTicks, entityIn.prevPitch, entityIn.getPitch())));
        matrixStackIn.translate(0.0D, -0.1F, 0.0D);
        matrixStackIn.push();
        matrixStackIn.scale(0.45F, 0.45F, 0.45F);
        renderCircle(matrixStackIn, bufferIn, packedLightIn);
        matrixStackIn.pop();

        matrixStackIn.pop();
        super.render(entityIn, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
        var holder = MinecraftClient.getInstance().player;
        if (holder != null) {
            double d0 = MathHelper.lerp(partialTicks, entityIn.lastRenderX, entityIn.getX());
            double d1 = MathHelper.lerp(partialTicks, entityIn.lastRenderY, entityIn.getY());
            double d2 = MathHelper.lerp(partialTicks, entityIn.lastRenderZ, entityIn.getZ());
            matrixStackIn.push();
            matrixStackIn.translate(-d0, -d1, -d2);
            renderVine(entityIn, partialTicks, matrixStackIn, bufferIn, holder, holder.getMainArm() != Arm.LEFT, -0.4F);
            matrixStackIn.pop();
        }
    }

    private void renderCircle(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn) {
        matrixStackIn.push();
        VertexConsumer ivertexbuilder = bufferIn.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE));
        MatrixStack.Entry lvt_19_1_ = matrixStackIn.peek();
        Matrix4f lvt_20_1_ = lvt_19_1_.getPositionMatrix();
        Matrix3f lvt_21_1_ = lvt_19_1_.getNormalMatrix();
        this.drawVertex(lvt_20_1_, lvt_21_1_, ivertexbuilder, -1, 0, -1, 0, 0, 1, 0, 1, packedLightIn);
        this.drawVertex(lvt_20_1_, lvt_21_1_, ivertexbuilder, -1, 0, 1, 0, 1, 1, 0, 1, packedLightIn);
        this.drawVertex(lvt_20_1_, lvt_21_1_, ivertexbuilder, 1, 0, 1, 1, 1, 1, 0, 1, packedLightIn);
        this.drawVertex(lvt_20_1_, lvt_21_1_, ivertexbuilder, 1, 0, -1, 1, 0, 1, 0, 1, packedLightIn);
        matrixStackIn.pop();
    }

    public static <E extends Entity> void renderVine(Entity mob, float partialTick, MatrixStack p_115464_, VertexConsumerProvider p_115465_, LivingEntity player, boolean left, float zOffset) {
        p_115464_.push();
        float bodyRot = mob instanceof LivingEntity ? ((LivingEntity) mob).bodyYaw : mob.getYaw();
        float bodyRot0 = mob instanceof LivingEntity ? ((LivingEntity) mob).prevBodyYaw : mob.prevYaw;
        Vec3d vec3 = player.getLeashPos(partialTick);
        double d0 = (double) (MathHelper.lerp(partialTick, bodyRot, bodyRot0) * MathHelper.RADIANS_PER_DEGREE) + (Math.PI / 2D);
        Vec3d vec31 = new Vec3d((left ? -0.05F : 0.05F), mob.getStandingEyeHeight(), zOffset);
        double d1 = Math.cos(d0) * vec31.z + Math.sin(d0) * vec31.x;
        double d2 = Math.sin(d0) * vec31.z - Math.cos(d0) * vec31.x;
        double d3 = MathHelper.lerp(partialTick, mob.prevX, mob.getX()) + d1;
        double d4 = MathHelper.lerp(partialTick, mob.prevY, mob.getY()) + vec31.y;
        double d5 = MathHelper.lerp(partialTick, mob.prevZ, mob.getZ()) + d2;
        p_115464_.translate(d3, d4, d5);
        float f = (float) (vec3.x - d3);
        float f1 = (float) (vec3.y - d4);
        float f2 = (float) (vec3.z - d5);
        VertexConsumer vertexconsumer = p_115465_.getBuffer(RenderLayer.getLeash());
        Matrix4f matrix4f = p_115464_.peek().getPositionMatrix();
        float f4 = (float) (MathHelper.fastInverseSqrt(f * f + f2 * f2) * 0.025F / 2.0F);
        float f5 = f2 * f4;
        float f6 = f * f4;
        BlockPos blockpos = AMBlockPos.fromVec3(mob.getCameraPosVec(partialTick));
        BlockPos blockpos1 = AMBlockPos.fromVec3(player.getCameraPosVec(partialTick));
        int i = getVineLightLevel(mob, blockpos);
        int j = mob.getWorld().getLightLevel(LightType.BLOCK, blockpos1);
        int k = mob.getWorld().getLightLevel(LightType.SKY, blockpos);
        int l = mob.getWorld().getLightLevel(LightType.SKY, blockpos1);
        float width = 0.1F;
        for (int i1 = 0; i1 <= 24; ++i1) {
            addVertexPairAlex(vertexconsumer, matrix4f, f, f1, f2, i, j, k, l, width, width, f5, f6, i1, false);
        }
        for (int j1 = 24; j1 >= 0; --j1) {
            addVertexPairAlex(vertexconsumer, matrix4f, f, f1, f2, i, j, k, l, width, width, f5, f6, j1, true);
        }
        p_115464_.pop();
    }

    protected static int getVineLightLevel(Entity p_114496_, BlockPos p_114497_) {
        return p_114496_.isOnFire() ? 15 : p_114496_.getWorld().getLightLevel(LightType.BLOCK, p_114497_);
    }


    private static Vec3d getVinePosition(LivingEntity entity, float p_36374_, boolean left, float shake) {
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
            var vec3 = entity.getRotationVec(p_36374_);
            var vec31 = entity.getVelocity();
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


    private static void addVertexPairAlex(VertexConsumer p_174308_, Matrix4f p_174309_, float p_174310_, float p_174311_, float p_174312_, int p_174313_, int p_174314_, int p_174315_, int p_174316_, float p_174317_, float p_174318_, float p_174319_, float p_174320_, int p_174321_, boolean p_174322_) {
        float f = (float) p_174321_ / 24.0F;
        int i = (int) MathHelper.lerp(f, (float) p_174313_, (float) p_174314_);
        int j = (int) MathHelper.lerp(f, (float) p_174315_, (float) p_174316_);
        int k = LightmapTextureManager.pack(i, j);
        float f2 = VINES_COLOR_R;
        float f3 = VINES_COLOR_G;
        float f4 = VINES_COLOR_B;
        if (p_174321_ % 2 == (p_174322_ ? 1 : 0)) {
            f2 = VINES_COLOR_R2;
            f3 = VINES_COLOR_G2;
            f4 = VINES_COLOR_B2;
        }
        float f5 = p_174310_ * f;
        float f6 = p_174311_ > 0.0F ? p_174311_ * f * f : p_174311_ - p_174311_ * (1.0F - f) * (1.0F - f);
        float f7 = p_174312_ * f;
        p_174308_.vertex(p_174309_, f5 - p_174319_, f6 + p_174318_, f7 + p_174320_).color(f2, f3, f4, 1.0F).light(k).next();
        p_174308_.vertex(p_174309_, f5 + p_174319_, f6 + p_174317_ - p_174318_, f7 - p_174320_).color(f2, f3, f4, 1.0F).light(k).next();
    }

    @NotNull
    @Override
    public Identifier getTexture(EntityVineLasso entity) {
        return TEXTURE;
    }

    public void drawVertex(Matrix4f p_229039_1_, Matrix3f p_229039_2_, VertexConsumer p_229039_3_, int p_229039_4_, int p_229039_5_, int p_229039_6_, float p_229039_7_, float p_229039_8_, int p_229039_9_, int p_229039_10_, int p_229039_11_, int p_229039_12_) {
        p_229039_3_.vertex(p_229039_1_, (float) p_229039_4_, (float) p_229039_5_, (float) p_229039_6_).color(255, 255, 255, 255).texture(p_229039_7_, p_229039_8_).overlay(OverlayTexture.DEFAULT_UV).light(p_229039_12_).normal(p_229039_2_, (float) p_229039_9_, (float) p_229039_11_, (float) p_229039_10_).next();
    }
}
