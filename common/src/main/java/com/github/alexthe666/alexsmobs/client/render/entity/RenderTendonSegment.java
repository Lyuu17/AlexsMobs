package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.client.model.ModelMurmurNeck;
import com.github.alexthe666.alexsmobs.client.model.ModelTendonClaw;
import com.github.alexthe666.alexsmobs.entity.EntityTendonSegment;
import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class RenderTendonSegment extends EntityRenderer<EntityTendonSegment> {

    private static final Identifier CLAW_TEXTURE = new Identifier("alexsmobs:textures/entity/tendon_whip_claw.png");
    private static final ModelTendonClaw CLAW_MODEL = new ModelTendonClaw();

    public RenderTendonSegment(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn);
    }

    @Override
    public boolean shouldRender(EntityTendonSegment entity, Frustum frustum, double x, double y, double z) {
        var next = entity.getFromEntity();
        return next != null && frustum.isVisible(entity.getBoundingBox().union(next.getBoundingBox())) || super.shouldRender(entity, frustum, x, y, z);
    }

    @Override
    public void render(EntityTendonSegment entity, float yaw, float partialTicks, MatrixStack poseStack, VertexConsumerProvider buffer, int light) {
        super.render(entity, yaw, partialTicks, poseStack, buffer, light);
        poseStack.push();
        var fromEntity = entity.getFromEntity();
        float x = (float)MathHelper.lerp(partialTicks, entity.prevX, entity.getX());
        float y = (float)MathHelper.lerp(partialTicks, entity.prevY, entity.getY());
        float z = (float)MathHelper.lerp(partialTicks, entity.prevZ, entity.getZ());

        if (fromEntity != null) {
            float progress = (entity.prevProgress + (entity.getProgress() - entity.prevProgress) * partialTicks) / EntityTendonSegment.MAX_EXTEND_TIME;
            var distVec = getPositionOfPriorMob(entity, fromEntity, partialTicks).subtract(x, y, z);
            var to = distVec.multiply(1F - progress);
            var from = distVec;
            int segmentCount = 0;
            var currentNeckButt = from;
            VertexConsumer neckConsumer;
            if(entity.hasGlint()){
                neckConsumer = AMRenderLayers.createMergedVertexConsumer(buffer.getBuffer(AMRenderLayers.getDirectEntityGlint()), buffer.getBuffer(RenderLayer.getEntityCutoutNoCull(RenderMurmurBody.TEXTURE)));
            }else{
                neckConsumer = buffer.getBuffer(RenderLayer.getEntityCutoutNoCull(RenderMurmurBody.TEXTURE));
            }
            ModelMurmurNeck.THIN = true;
            double remainingDistance = to.distanceTo(from);
            while (segmentCount < RenderMurmurHead.MAX_NECK_SEGMENTS && remainingDistance > 0) {
                remainingDistance = Math.min(from.distanceTo(to), 0.5F);
                var linearVec = to.subtract(currentNeckButt);
                var powVec = new Vec3d(modifyVecAngle(linearVec.x), modifyVecAngle(linearVec.y), modifyVecAngle(linearVec.z));
                var smoothedVec = powVec;
                var next = smoothedVec.normalize().multiply(remainingDistance).add(currentNeckButt);
                int neckLight = getLightColor(entity, to.add(currentNeckButt).add(x, y, z));
                RenderMurmurHead.renderNeckCube(currentNeckButt, next, poseStack, neckConsumer, neckLight, OverlayTexture.DEFAULT_UV, 0);
                currentNeckButt = next;
                segmentCount++;
            }
            ModelMurmurNeck.THIN = false;
            VertexConsumer clawConsumer;
            if(entity.hasGlint()){
                clawConsumer = AMRenderLayers.createMergedVertexConsumer(buffer.getBuffer(AMRenderLayers.getDirectEntityGlint()), buffer.getBuffer(RenderLayer.getEntityCutoutNoCull(CLAW_TEXTURE)));
            }else{
                clawConsumer = buffer.getBuffer(RenderLayer.getEntityCutoutNoCull(CLAW_TEXTURE));
            }
            if(entity.hasClaw() || entity.isRetracting()){
                poseStack.push();
                poseStack.translate(to.x, to.y, to.z);
                float rotY = (float) (MathHelper.atan2(to.x, to.z) * (double) MathHelper.DEGREES_PER_RADIAN);
                float rotX = (float) (-(MathHelper.atan2(to.y, to.horizontalLength()) * (double) MathHelper.DEGREES_PER_RADIAN));
                CLAW_MODEL.setAttributes(rotX, rotY, 1 - progress);
                CLAW_MODEL.render(poseStack, clawConsumer, getLightColor(entity, to.add(x, y, z)), OverlayTexture.DEFAULT_UV, 1, 1F, 1, 1F);
                poseStack.pop();
            }
        }
        poseStack.pop();
    }

    private Vec3d getPositionOfPriorMob(EntityTendonSegment segment, Entity mob, float partialTicks){
        double d4 = MathHelper.lerp(partialTicks, mob.prevX, mob.getX());
        double d5 = MathHelper.lerp(partialTicks, mob.prevY, mob.getY());
        double d6 = MathHelper.lerp(partialTicks, mob.prevZ, mob.getZ());
        float f3 = 0;
        if(mob instanceof PlayerEntity player && segment.isCreator(mob)){
            float f = player.getHandSwingProgress(partialTicks);
            float f1 = MathHelper.sin(MathHelper.sqrt(f) * MathHelper.PI);
            float f2 = MathHelper.lerp(partialTicks, player.prevBodyYaw, player.bodyYaw) * MathHelper.RADIANS_PER_DEGREE;
            int i = player.getMainArm() == Arm.RIGHT ? 1 : -1;
            double d0 = (double) MathHelper.sin(f2);
            double d1 = (double) MathHelper.cos(f2);
            double d2 = (double) i * 0.35D;
            ItemStack itemstack = player.getMainHandStack();
            if (!itemstack.isOf(AMItemRegistry.TENDON_WHIP.get())) {
                i = -i;
            }
            if ((this.dispatcher.gameOptions == null || this.dispatcher.gameOptions.getPerspective().isFirstPerson()) && player == MinecraftClient.getInstance().player) {
                double d7 = 960.0D / (double)this.dispatcher.gameOptions.getFov().getValue().intValue();
                Vec3d vec3 = this.dispatcher.camera.getProjection().getPosition((float) i * 0.6F, -1);
                vec3 = vec3.multiply(d7);
                vec3 = vec3.rotateY(f1 * 0.25F);
                vec3 = vec3.rotateX(-f1 * 0.35F);
                d4 = MathHelper.lerp(partialTicks, player.prevX, player.getX()) + vec3.x;
                d5 = MathHelper.lerp(partialTicks, player.prevY, player.getY()) + vec3.y;
                d6 = MathHelper.lerp(partialTicks, player.prevZ, player.getZ()) + vec3.z;
                f3 = player.getStandingEyeHeight() * 0.4F;
            } else {
                d4 = MathHelper.lerp(partialTicks, player.prevX, player.getX()) - d1 * d2 - d0 * 0.2D;
                d5 = player.prevY + (double) player.getStandingEyeHeight() + (player.getY() - player.prevY) * (double) partialTicks - 1D;
                d6 = MathHelper.lerp(partialTicks, player.prevZ, player.getZ()) - d0 * d2 + d1 * 0.2D;
                f3 = (player.isInSneakingPose() ? -0.1875F : 0.0F) - player.getStandingEyeHeight() * 0.3F;
            }
        }

        return new Vec3d(d4, d5 + f3, d6);
    }

    private double modifyVecAngle(double dimension) {
        float abs = (float) Math.abs(dimension);
        return Math.signum(dimension) * MathHelper.clamp(Math.pow(abs, 0.1), 0.05 * abs, abs);
    }

    private int getLightColor(Entity head, Vec3d vec3) {
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
    public Identifier getTexture(EntityTendonSegment entity) {
        return null;
    }

}