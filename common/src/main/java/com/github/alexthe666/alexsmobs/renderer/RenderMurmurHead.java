package com.github.alexthe666.alexsmobs.renderer;

import com.github.alexthe666.alexsmobs.entity.EntityMurmurHead;
import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import com.github.alexthe666.alexsmobs.model.ModelMurmurHead;
import com.github.alexthe666.alexsmobs.model.ModelMurmurNeck;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class RenderMurmurHead extends MobEntityRenderer<EntityMurmurHead, ModelMurmurHead> {

    private static final ModelMurmurNeck NECK_MODEL = new ModelMurmurNeck();
    public static final int MAX_NECK_SEGMENTS = 128;

    public RenderMurmurHead(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelMurmurHead(), 0.3F);
    }

    @Override
    protected void scale(EntityMurmurHead entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
        matrixStackIn.scale(0.85F, 0.85F, 0.85F);
    }

    @Override
    public boolean shouldRender(EntityMurmurHead livingEntityIn, Frustum camera, double camX, double camY, double camZ) {
        if (super.shouldRender(livingEntityIn, camera, camX, camY, camZ)) {
            return true;
        } else if(livingEntityIn.hasNeckBottom()){
            Vec3d vector3d = livingEntityIn.getNeckBottom(1.0F);
            Vec3d vector3d1 = livingEntityIn.getNeckTop(1.0F);
            return camera.isVisible(new Box(vector3d1.x, vector3d1.y, vector3d1.z, vector3d.x, vector3d.y, vector3d.z));
        }else{
            return false;
        }
    }

    @Override
    protected float getLyingAngle(EntityMurmurHead head) {
        return 0.0F;
    }

    @Override
    public void render(EntityMurmurHead head, float entityYaw, float partialTicks, MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn) {
        super.render(head, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
        matrixStackIn.push();
        if (head.hasNeckBottom()) {
            float headYaw = MathHelper.lerpAngleDegrees(partialTicks, head.prevBodyYaw, head.bodyYaw);
            var renderingAt = new Vec3d(MathHelper.lerp(partialTicks, head.prevX, head.getX()), MathHelper.lerp(partialTicks, head.prevY, head.getY()), MathHelper.lerp(partialTicks, head.prevZ, head.getZ()));
            var bottom = head.getNeckBottom(partialTicks).subtract(renderingAt);
            var top = head.getNeckTop(partialTicks).subtract(renderingAt);
            var moveDownFrom = bottom.subtract(top);
            var moveUpTowards = top.subtract(bottom);
            var renderType = RenderLayer.getEntityCutoutNoCull(getTexture(head));
            int overlayCoords = getOverlay(head, this.getAnimationCounter(head, partialTicks));
            matrixStackIn.translate(moveDownFrom.x, moveDownFrom.y - 0.5F, moveDownFrom.z);
            var currentNeckButt = Vec3d.ZERO;
            int segmentCount = 0;
            while (segmentCount < MAX_NECK_SEGMENTS && currentNeckButt.distanceTo(moveUpTowards) > 0.2) {
                double remainingDistance = Math.min(currentNeckButt.distanceTo(moveUpTowards), 1F);
                var linearVec = moveUpTowards.subtract(currentNeckButt);
                var powVec = new Vec3d(modifyVecAngle(linearVec.x), modifyVecAngle(linearVec.y), modifyVecAngle(linearVec.z));
                var smoothedVec = remainingDistance < 1F ? linearVec : powVec;
                var next = smoothedVec.normalize().multiply(remainingDistance).add(currentNeckButt);
                int neckLight = getLightColor(head, bottom.add(currentNeckButt).add(renderingAt));
                renderNeckCube(currentNeckButt, next, matrixStackIn, bufferIn.getBuffer(renderType), neckLight, overlayCoords, headYaw);
                currentNeckButt = next;
                segmentCount++;
            }
        }
        matrixStackIn.pop();
    }

    private double modifyVecAngle(double dimension) {
        float abs = (float) Math.abs(dimension);
        return Math.signum(dimension) * MathHelper.clamp(Math.pow(abs, 0.1), 0.01 * abs, abs);
    }

    public static void renderNeckCube(Vec3d from, Vec3d to, MatrixStack poseStack, VertexConsumer buffer, int packedLightIn, int overlayCoords, float additionalYaw) {
        var sub = from.subtract(to);
        double d = sub.horizontalLength();
        float rotY = (float) (MathHelper.atan2(sub.x, sub.z) * (double) MathHelper.DEGREES_PER_RADIAN);
        float rotX = (float) (-(MathHelper.atan2(sub.y, d) * (double) MathHelper.DEGREES_PER_RADIAN)) - 90.0F;
        poseStack.push();
        poseStack.translate(from.x, from.y, from.z);
        NECK_MODEL.setAttributes((float) sub.length(), rotX, rotY, additionalYaw);
        NECK_MODEL.render(poseStack, buffer, packedLightIn, overlayCoords, 1, 1F, 1, 1);
        poseStack.pop();
    }

    private int getLightColor(EntityMurmurHead head, Vec3d vec3) {
        BlockPos blockpos = AMBlockPos.fromVec3(vec3);
        if(head.getWorld().isChunkLoaded(blockpos)){
            int i = WorldRenderer.getLightmapCoordinates(head.getWorld(), blockpos);
            int j = WorldRenderer.getLightmapCoordinates(head.getWorld(), blockpos.up());
            int k = i & 255;
            int l = j & 255;
            int i1 = i >> 16 & 255;
            int j1 = j >> 16 & 255;
            return (Math.max(k, l)) | (Math.max(i1, j1)) << 16;
        }else{
            return 0;
        }
    }

    @Override
    public Identifier getTexture(EntityMurmurHead entity) {
        return entity.isAngry() ? RenderMurmurBody.TEXTURE_ANGRY : RenderMurmurBody.TEXTURE;
    }
}
