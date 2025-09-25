package com.github.alexthe666.alexsmobs.client.render.entity;


import com.github.alexthe666.alexsmobs.client.model.ModelUnderminerDwarf;
import com.github.alexthe666.alexsmobs.client.render.entity.layer.LayerUnderminerItem;
import com.github.alexthe666.alexsmobs.entity.EntityUnderminer;
import com.github.alexthe666.alexsmobs.registry.AMModelLayerRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.OverlayVertexConsumer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RenderUnderminer extends MobEntityRenderer<EntityUnderminer, EntityModel<EntityUnderminer>> {
    private static final Identifier TEXTURE_DWARF = new Identifier("alexsmobs:textures/entity/underminer_dwarf.png");
    private static final Identifier TEXTURE_0 = new Identifier("alexsmobs:textures/entity/underminer_0.png");
    private static final Identifier TEXTURE_1 = new Identifier("alexsmobs:textures/entity/underminer_1.png");
    public static final List<Identifier> BREAKING_LOCATIONS = IntStream.range(0, 10).mapToObj((destroyStage) -> new Identifier("alexsmobs:textures/block/ghostly_pickaxe/destroy_stage_" + destroyStage + ".png")).collect(Collectors.toList());
    private static final ModelUnderminerDwarf DWARF_MODEL = new ModelUnderminerDwarf();
    private static BipedEntityModel<EntityUnderminer> NORMAL_MODEL = null;
    private static final List<RenderLayer> DESTROY_TYPES = BREAKING_LOCATIONS.stream().map(AMRenderLayers::getGhostCrumbling).toList();
    public static boolean renderWithPickaxe = false;

    public RenderUnderminer(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, DWARF_MODEL, 0.4F);
        NORMAL_MODEL = new BipedEntityModel<>(MinecraftClient.getInstance().getEntityModelLoader().getModelPart(AMModelLayerRegistry.UNDERMINER));
        this.addFeature(new LayerUnderminerItem(this));
    }

    @Override
    protected void scale(EntityUnderminer entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
        matrixStackIn.scale(0.925F, 0.925F, 0.925F);
    }

    @Override
    public boolean shouldRender(EntityUnderminer livingEntityIn, Frustum camera, double camX, double camY, double camZ) {
        if (super.shouldRender(livingEntityIn, camera, camX, camY, camZ)) {
            return true;
        } else {
            if (livingEntityIn.getMiningPos() != null) {
                var pos = livingEntityIn.getMiningPos();
                if (pos != null) {
                    var vector3d = Vec3d.of(pos);
                    var vector3dCorner = Vec3d.of(pos).add(1, 1, 1);
                    return camera.isVisible(new Box(vector3d.x, vector3d.y, vector3d.z, vector3dCorner.x, vector3dCorner.y, vector3dCorner.z));
                }
            }
            return false;
        }
    }

    @Override
    protected float getLyingAngle(EntityUnderminer entityUnderminer) {
        return 0.0F;
    }

    @Override
    public void render(EntityUnderminer entityIn, float entityYaw, float partialTicks, MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn) {
//FIXME forge
//        if (net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.RenderLivingEvent.Pre<EntityUnderminer, EntityModel<EntityUnderminer>>(entityIn, this, partialTicks, matrixStackIn, bufferIn, packedLightIn)))
//            return;
        matrixStackIn.push();
        this.model.handSwingProgress = this.getHandSwingProgress(entityIn, partialTicks);

        boolean shouldSit = entityIn.hasVehicle() /* FIXME forge && (entityIn.getVehicle() != null && entityIn.getVehicle().shouldRiderSit())*/;
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
        if (entityIn.isDwarf()) {
            this.model = DWARF_MODEL;
        } else {
            this.model = NORMAL_MODEL;
        }
        this.model.animateModel(entityIn, f5, f8, partialTicks);
        this.model.setAngles(entityIn, f5, f8, f7, f2, f6);
        var minecraft = MinecraftClient.getInstance();
        boolean flag = this.isVisible(entityIn);
        boolean flag1 = !flag && !entityIn.isInvisibleTo(minecraft.player);
        boolean flag2 = minecraft.hasOutline(entityIn);
        var rendertype = this.getRenderLayer(entityIn, flag, flag1, flag2);
        if (rendertype != null && !entityIn.isFullyHidden()) {
            float hide = (entityIn.prevHidingProgress + (entityIn.hidingProgress - entityIn.prevHidingProgress) * partialTicks) * 0.1F;
            float alpha = (1F - hide) * 0.6F;
            this.shadowRadius = 0.9F * alpha;
            int i = getOverlay(entityIn, this.getAnimationCounter(entityIn, partialTicks));
            this.renderUnderminerModel(matrixStackIn, bufferIn, rendertype, partialTicks, packedLightIn, i, flag1 ? 0.15F : MathHelper.clamp(alpha, 0, 1), entityIn);
        } else {
            this.shadowRadius = 0;
        }
        if (!entityIn.isSpectator()) {
            for (var layerrenderer : this.features) {
                layerrenderer.render(matrixStackIn, bufferIn, packedLightIn, entityIn, f5, f8, partialTicks, f7, f2, f6);
            }
        }

        matrixStackIn.pop();
        //FIXME forge
//        RenderNameTagEvent renderNameplateEvent = new RenderNameTagEvent(entityIn, entityIn.getDisplayName(), this, matrixStackIn, bufferIn, packedLightIn, partialTicks);
//        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(renderNameplateEvent);
//        if (renderNameplateEvent.getResult() != net.minecraftforge.eventbus.api.Event.Result.DENY && (renderNameplateEvent.getResult() == net.minecraftforge.eventbus.api.Event.Result.ALLOW || this.shouldRenderName(entityIn))) {
//            this.renderNameTag(entityIn, renderNameplateEvent.getContent(), matrixStackIn, bufferIn, packedLightIn);
//        }
//        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.RenderLivingEvent.Post<EntityUnderminer, EntityModel<EntityUnderminer>>(entityIn, this, partialTicks, matrixStackIn, bufferIn, packedLightIn));

        var miningPos = entityIn.getMiningPos();
        if (miningPos != null) {
            matrixStackIn.push();
            double d0 = MathHelper.lerp(partialTicks, entityIn.prevX, entityIn.getX());
            double d1 = MathHelper.lerp(partialTicks, entityIn.prevY, entityIn.getY());
            double d2 = MathHelper.lerp(partialTicks, entityIn.prevZ, entityIn.getZ());

            matrixStackIn.translate((double) miningPos.getX() - d0, (double) miningPos.getY() - d1, (double) miningPos.getZ() - d2);
            int progress = Math.round((DESTROY_TYPES.size() - 1) * MathHelper.clamp(entityIn.getMiningProgress(), 0F, 1.0F));
            var posestack$pose = matrixStackIn.peek();
            var vertexconsumer1 = new OverlayVertexConsumer(bufferIn.getBuffer(DESTROY_TYPES.get(progress)), posestack$pose.getPositionMatrix(), posestack$pose.getNormalMatrix(), 1.0F);

            MinecraftClient.getInstance().getBlockRenderManager().renderDamage(entityIn.getWorld().getBlockState(miningPos), miningPos, entityIn.getWorld(), matrixStackIn, vertexconsumer1);
            matrixStackIn.pop();
        }
    }

    private void renderUnderminerModel(MatrixStack matrixStackIn, VertexConsumerProvider source, RenderLayer defRenderType, float partialTicks, int packedLightIn, int overlayColors, float alphaIn, EntityUnderminer entityIn) {
        boolean hurt = Math.max(entityIn.hurtTime, entityIn.deathTime) > 0;
        this.model.render(matrixStackIn, source.getBuffer(defRenderType), packedLightIn, LivingEntityRenderer.getOverlay(entityIn, 0.0F), hurt ? 0.4F : 1.0F, hurt ? 0.8F : 1.0F, hurt ? 0.7F : 1.0F, alphaIn);
    }


    @Nullable
    @Override
    protected RenderLayer getRenderLayer(EntityUnderminer farseer, boolean normal, boolean invis, boolean outline) {
        Identifier Identifier = this.getTexture(farseer);
        return outline ? RenderLayer.getOutline(Identifier) : AMRenderLayers.getUnderminer(Identifier);
    }

    @Override
    public Identifier getTexture(EntityUnderminer entity) {
        return entity.isDwarf() ? TEXTURE_DWARF : entity.getVariant() == 0 ? TEXTURE_0 : TEXTURE_1;
    }

}
