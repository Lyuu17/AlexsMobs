package com.github.alexthe666.alexsmobs.renderer;

import com.github.alexthe666.alexsmobs.entity.EntityVoidWormPart;
import com.github.alexthe666.alexsmobs.model.ModelVoidWormBody;
import com.github.alexthe666.alexsmobs.model.ModelVoidWormTail;
import com.github.alexthe666.alexsmobs.renderer.layer.LayerVoidWormGlow;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.jetbrains.annotations.NotNull;

public class RenderVoidWormBody extends LivingEntityRenderer<EntityVoidWormPart, EntityModel<EntityVoidWormPart>> {
    private static final Identifier TEXTURE_BODY = new Identifier("alexsmobs:textures/entity/void_worm/void_worm_body.png");
    private static final Identifier TEXTURE_BODY_HURT = new Identifier("alexsmobs:textures/entity/void_worm/void_worm_body_hurt.png");
    private static final Identifier TEXTURE_BODY_GLOW = new Identifier("alexsmobs:textures/entity/void_worm/void_worm_body_glow.png");
    private static final Identifier TEXTURE_TAIL = new Identifier("alexsmobs:textures/entity/void_worm/void_worm_tail.png");
    private static final Identifier TEXTURE_TAIL_HURT = new Identifier("alexsmobs:textures/entity/void_worm/void_worm_tail_hurt.png");
    private static final Identifier TEXTURE_TAIL_GLOW = new Identifier("alexsmobs:textures/entity/void_worm/void_worm_tail_glow.png");
    private final ModelVoidWormBody bodyModel = new ModelVoidWormBody(0.0F);
    private final ModelVoidWormTail tailModel = new ModelVoidWormTail(0.0F);

    public RenderVoidWormBody(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelVoidWormBody(0.0F), 1F);
        this.addFeature(new LayerVoidWormGlow<>(this, renderManagerIn.getResourceManager(), new ModelVoidWormBody(0.0F)){
            public Identifier getGlowTexture(LivingEntity worm){
                return ((EntityVoidWormPart)worm).isTail() ? TEXTURE_TAIL_GLOW : TEXTURE_BODY_GLOW;
            }
            public boolean isGlowing(LivingEntity worm){
                return !((EntityVoidWormPart)worm).isHurt();
            }
            public float getAlpha(LivingEntity livingEntity){
                EntityVoidWormPart worm = (EntityVoidWormPart) livingEntity;
                return (float) MathHelper.clamp((worm.getHealth() - worm.getHealthThreshold()) / (worm.getMaxHealth() - worm.getHealthThreshold()), 0, 1F);
            }
        });
    }

    @Override
    public boolean shouldRender(EntityVoidWormPart worm, Frustum camera, double camX, double camY, double camZ) {
        return worm.getPortalTicks() <= 0 && super.shouldRender(worm, camera, camX, camY, camZ);
    }

    @NotNull
    @Override
    public Identifier getTexture(EntityVoidWormPart entity) {
        if (entity.isHurt()) {
            return entity.isTail() ? TEXTURE_TAIL_HURT : TEXTURE_BODY_HURT;
        } else {
            return entity.isTail() ? TEXTURE_TAIL : TEXTURE_BODY;
        }
    }

    @Override
    protected void setupTransforms(EntityVoidWormPart entityLiving, MatrixStack matrixStackIn, float ageInTicks, float rotationYaw, float partialTicks) {
        var pose = entityLiving.getPose();
        if (pose != EntityPose.SLEEPING) {
            matrixStackIn.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - entityLiving.getWormYaw(partialTicks)));
        }
        if (entityLiving.deathTime > 0) {
            float f = ((float) entityLiving.deathTime + partialTicks - 1.0F) / 20.0F * 1.6F;
            f = MathHelper.sqrt(f);
            if (f > 1.0F) {
                f = 1.0F;
            }
            matrixStackIn.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(f * this.getLyingAngle(entityLiving)));
        }

    }

    @Override
    protected void scale(EntityVoidWormPart entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
        this.model = entitylivingbaseIn.isTail() ? tailModel : bodyModel;
        matrixStackIn.scale(entitylivingbaseIn.getWormScale(), entitylivingbaseIn.getWormScale(), entitylivingbaseIn.getWormScale());
    }

    @Override
    protected boolean hasLabel(EntityVoidWormPart entity) {
        return super.hasLabel(entity) && (entity.shouldRenderName() || entity.hasCustomName() && entity == this.dispatcher.targetedEntity);
    }
}
