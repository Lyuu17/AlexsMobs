package com.github.alexthe666.alexsmobs.renderer;

import com.github.alexthe666.alexsmobs.entity.EntityAnacondaPart;
import com.github.alexthe666.alexsmobs.entity.util.AnacondaPartIndex;
import com.github.alexthe666.alexsmobs.model.ModelAnaconda;
import com.iafenvoy.uranus.client.model.AdvancedEntityModel;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityPose;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.jetbrains.annotations.NotNull;

public class RenderAnacondaPart extends LivingEntityRenderer<EntityAnacondaPart, AdvancedEntityModel<EntityAnacondaPart>> {
    private final ModelAnaconda<EntityAnacondaPart> neckModel = new ModelAnaconda<>(AnacondaPartIndex.NECK);
    private final ModelAnaconda<EntityAnacondaPart> bodyModel = new ModelAnaconda<>(AnacondaPartIndex.BODY);
    private final ModelAnaconda<EntityAnacondaPart> tailModel = new ModelAnaconda<>(AnacondaPartIndex.TAIL);

    public RenderAnacondaPart(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelAnaconda<>(AnacondaPartIndex.NECK), 0.3F);
    }

    @Override
    protected void setupTransforms(EntityAnacondaPart entity, MatrixStack stack, float pitchIn, float yawIn, float partialTickTime) {
        float newYaw = entity.headYaw;
        if (this.isShaking(entity)) {
            newYaw += (float)(Math.cos((double)entity.age * 3.25D) * Math.PI * (double)0.4F);
        }

        var pose = entity.getPose();
        if (pose != EntityPose.SLEEPING) {
         //   stack.multiply(Axis.YP.rotationDegrees(180.0F - yawIn));
            stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - newYaw));
            stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(entity.getPitch()));
        }

        if (entity.deathTime > 0) {
            float f = ((float)entity.deathTime + partialTickTime - 1.0F) / 20.0F * 1.6F;
            f = MathHelper.sqrt(f);
            if (f > 1.0F) {
                f = 1.0F;
            }

            stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(f * this.getLyingAngle(entity)));
         } else if (entity.hasCustomName()) {
            String s = Formatting.strip(entity.getName().getString());
            if (("Dinnerbone".equals(s) || "Grumm".equals(s))) {
                stack.translate(0.0D, (double)(entity.getHeight() + 0.1F), 0.0D);
                stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
            }
        }

    }

    @Override
    protected boolean hasLabel(EntityAnacondaPart entity) {
        return super.hasLabel(entity) && (entity.shouldRenderName() || entity.hasCustomName() && entity == this.dispatcher.targetedEntity);
    }

    @Override
    protected void scale(EntityAnacondaPart entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
        this.model = getModelForType(entitylivingbaseIn.getPartType());
        matrixStackIn.scale(entitylivingbaseIn.getScaleFactor(), entitylivingbaseIn.getScaleFactor(), entitylivingbaseIn.getScaleFactor());
    }

    private AdvancedEntityModel<EntityAnacondaPart> getModelForType(AnacondaPartIndex partType) {
        return switch (partType) {
            case BODY -> bodyModel;
            case NECK -> neckModel;
            case TAIL -> tailModel;
            default -> bodyModel;
        };
    }

    @NotNull
    @Override
    public Identifier getTexture(EntityAnacondaPart entity) {
        return RenderAnaconda.getAnacondaTexture(entity.isYellow(), entity.isShedding());
    }
}
