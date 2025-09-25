package com.github.alexthe666.alexsmobs.client.render.entity;


import com.github.alexthe666.alexsmobs.client.model.ModelTiger;
import com.github.alexthe666.alexsmobs.client.model.layered.LayerTigerEyes;
import com.github.alexthe666.alexsmobs.entity.EntityTiger;
import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.LightType;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

public class RenderTiger extends MobEntityRenderer<EntityTiger, ModelTiger> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/tiger/tiger.png");
    private static final Identifier TEXTURE_ANGRY = new Identifier("alexsmobs:textures/entity/tiger/tiger_angry.png");
    private static final Identifier TEXTURE_SLEEPING = new Identifier("alexsmobs:textures/entity/tiger/tiger_sleeping.png");
    private static final Identifier TEXTURE_WHITE = new Identifier("alexsmobs:textures/entity/tiger/tiger_white.png");
    private static final Identifier TEXTURE_ANGRY_WHITE = new Identifier("alexsmobs:textures/entity/tiger/tiger_white_angry.png");
    private static final Identifier TEXTURE_SLEEPING_WHITE = new Identifier("alexsmobs:textures/entity/tiger/tiger_white_sleeping.png");

    public RenderTiger(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelTiger(), 0.6F);
        this.addFeature(new LayerTigerEyes(this));
    }

    @Override
    protected void scale(EntityTiger entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
    }

    @Override
    public void render(EntityTiger entityIn, float entityYaw, float partialTicks, MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn) {
//FIXME forge
//        if (net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.RenderLivingEvent.Pre<EntityTiger, ModelTiger>(entityIn, this, partialTicks, matrixStackIn, bufferIn, packedLightIn)))
//            return;
        matrixStackIn.push();
        this.model.handSwingProgress = this.getHandSwingProgress(entityIn, partialTicks);

        boolean shouldSit = entityIn.hasVehicle() && (entityIn.getVehicle() != null /* FIXME forge && entityIn.getVehicle().shouldRiderSit()*/);
        this.model.riding = shouldSit;
        this.model.child = entityIn.isBaby();
        float f = MathHelper.lerpAngleDegrees(partialTicks, entityIn.prevBodyYaw, entityIn.bodyYaw);
        float f1 = MathHelper.lerpAngleDegrees(partialTicks, entityIn.prevHeadYaw, entityIn.headYaw);
        float f2 = f1 - f;
        if (shouldSit && entityIn.getVehicle() instanceof LivingEntity livingentity) {
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
        this.setupTransforms(entityIn, matrixStackIn, f7, f, partialTicks);
        matrixStackIn.scale(-1.0F, -1.0F, 1.0F);
        this.scale(entityIn, matrixStackIn, partialTicks);
        matrixStackIn.translate(0.0D, -1.501F, 0.0D);
        float f8 = 0.0F;
        float f5 = 0.0F;
        if (!shouldSit && entityIn.isAlive()) {
            f8 = entityIn.limbAnimator.getSpeed(partialTicks);
            f5 = entityIn.limbAnimator.getPos(partialTicks);
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
        if (rendertype != null) {
            float stealthLevel = entityIn.prevStealthProgress + (entityIn.stealthProgress - entityIn.prevStealthProgress) * partialTicks;
            this.shadowRadius = 0.6F * (1 - stealthLevel * 0.1F);
            var ivertexbuilder = bufferIn.getBuffer(rendertype);
            int i = getOverlay(entityIn, this.getAnimationCounter(entityIn, partialTicks));
            this.model.render(matrixStackIn, ivertexbuilder, packedLightIn, i, 1.0F, 1.0F, 1.0F, flag1 ? 0.15F : MathHelper.clamp(1 - stealthLevel * 0.1F, 0, 1));
        }

        if (!entityIn.isSpectator()) {
            for (var layerrenderer : this.features) {
                layerrenderer.render(matrixStackIn, bufferIn, packedLightIn, entityIn, f5, f8, partialTicks, f7, f2, f6);
            }
        }

        matrixStackIn.pop();
        var entity = entityIn.getHoldingEntity();
        if (entity != null) {
            this.renderLeash(entityIn, partialTicks, matrixStackIn, bufferIn, entity);
        }
        // FIXME forge
//        var renderNameplateEvent = new RenderNameTagEvent(entityIn, entityIn.getDisplayName(), this, matrixStackIn, bufferIn, packedLightIn, partialTicks);
//        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(renderNameplateEvent);
//        if (renderNameplateEvent.getResult() != net.minecraftforge.eventbus.api.Event.Result.DENY && (renderNameplateEvent.getResult() == net.minecraftforge.eventbus.api.Event.Result.ALLOW || this.shouldRenderName(entityIn))) {
//            this.renderNameTag(entityIn, renderNameplateEvent.getContent(), matrixStackIn, bufferIn, packedLightIn);
//        }
//        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.RenderLivingEvent.Post<EntityTiger, ModelTiger>(entityIn, this, partialTicks, matrixStackIn, bufferIn, packedLightIn));
    }

    private <E extends Entity> void renderLeash(EntityTiger tiger, float p_115463_, MatrixStack p_115464_, VertexConsumerProvider p_115465_, E p_115466_) {
        p_115464_.push();
        var vec3 = p_115466_.getLeashPos(p_115463_);
        double d0 = (double)(MathHelper.lerp(p_115463_, tiger.bodyYaw, tiger.prevBodyYaw) * MathHelper.RADIANS_PER_DEGREE) + (Math.PI / 2D);
        var vec31 = tiger.getLeashOffset(p_115463_);
        double d1 = Math.cos(d0) * vec31.z + Math.sin(d0) * vec31.x;
        double d2 = Math.sin(d0) * vec31.z - Math.cos(d0) * vec31.x;
        double d3 = MathHelper.lerp(p_115463_, tiger.prevX, tiger.getX()) + d1;
        double d4 = MathHelper.lerp(p_115463_, tiger.prevY, tiger.getY()) + vec31.y;
        double d5 = MathHelper.lerp(p_115463_, tiger.prevZ, tiger.getZ()) + d2;
        p_115464_.translate(d1, vec31.y, d2);
        float f = (float)(vec3.x - d3);
        float f1 = (float)(vec3.y - d4);
        float f2 = (float)(vec3.z - d5);
        float f3 = 0.025F;
        var vertexconsumer = p_115465_.getBuffer(RenderLayer.getLeash());
        var matrix4f = p_115464_.peek().getPositionMatrix();
        float f4 = (float) (MathHelper.fastInverseSqrt(f * f + f2 * f2) * 0.025F / 2.0F);
        float f5 = f2 * f4;
        float f6 = f * f4;
        var blockpos = AMBlockPos.fromVec3(tiger.getCameraPosVec(p_115463_));
        var blockpos1 = AMBlockPos.fromVec3(p_115466_.getCameraPosVec(p_115463_));
        int i = this.getBlockLight(tiger, blockpos);
        int j = this.getBlockLight(tiger, blockpos1);
        int k = tiger.getWorld().getLightLevel(LightType.SKY, blockpos);
        int l = tiger.getWorld().getLightLevel(LightType.SKY, blockpos1);

        for(int i1 = 0; i1 <= 24; ++i1) {
            addVertexPair(vertexconsumer, matrix4f, f, f1, f2, i, j, k, l, 0.025F, 0.025F, f5, f6, i1, false);
        }

        for(int j1 = 24; j1 >= 0; --j1) {
            addVertexPair(vertexconsumer, matrix4f, f, f1, f2, i, j, k, l, 0.025F, 0.0F, f5, f6, j1, true);
        }

        p_115464_.pop();
    }

    @Override
    protected int getBlockLight(EntityTiger p_114496_, BlockPos p_114497_) {
        return p_114496_.isOnFire() ? 15 : p_114496_.getWorld().getLightLevel(LightType.BLOCK, p_114497_);
    }

    private static void addVertexPair(VertexConsumer p_174308_, Matrix4f p_174309_, float p_174310_, float p_174311_, float p_174312_, int p_174313_, int p_174314_, int p_174315_, int p_174316_, float p_174317_, float p_174318_, float p_174319_, float p_174320_, int p_174321_, boolean p_174322_) {
        float f = (float)p_174321_ / 24.0F;
        int i = (int)MathHelper.lerp(f, (float)p_174313_, (float)p_174314_);
        int j = (int)MathHelper.lerp(f, (float)p_174315_, (float)p_174316_);
        int k = LightmapTextureManager.pack(i, j);
        float f1 = p_174321_ % 2 == (p_174322_ ? 1 : 0) ? 0.7F : 1.0F;
        float f2 = 0.5F * f1;
        float f3 = 0.4F * f1;
        float f4 = 0.3F * f1;
        float f5 = p_174310_ * f;
        float f6 = p_174311_ > 0.0F ? p_174311_ * f * f : p_174311_ - p_174311_ * (1.0F - f) * (1.0F - f);
        float f7 = p_174312_ * f;
        p_174308_.vertex(p_174309_, f5 - p_174319_, f6 + p_174318_, f7 + p_174320_).color(f2, f3, f4, 1.0F).light(k).next();
        p_174308_.vertex(p_174309_, f5 + p_174319_, f6 + p_174317_ - p_174318_, f7 - p_174320_).color(f2, f3, f4, 1.0F).light(k).next();
    }

    protected int getBlockLight2(Entity entityIn, BlockPos partialTicks) {
        return entityIn.isOnFire() ? 15 : entityIn.getWorld().getLightLevel(LightType.BLOCK, partialTicks);
    }

    @Nullable
    @Override
    protected RenderLayer getRenderLayer(EntityTiger tiger, boolean b0, boolean b1, boolean b2) {
        if (tiger.isStealth()) {
            Identifier Identifier = this.getTexture(tiger);
            return RenderLayer.getItemEntityTranslucentCull(Identifier);
        } else {
            return super.getRenderLayer(tiger, b0, b1, b2);
        }
    }

    @Override
    public Identifier getTexture(EntityTiger entity) {
        if (entity.isSleeping()) {
            return entity.isWhite() ? TEXTURE_SLEEPING_WHITE : TEXTURE_SLEEPING;
        } else if (entity.getAngerTime() > 0) {
            return entity.isWhite() ? TEXTURE_ANGRY_WHITE : TEXTURE_ANGRY;
        } else {
            return entity.isWhite() ? TEXTURE_WHITE : TEXTURE;
        }
    }
}
