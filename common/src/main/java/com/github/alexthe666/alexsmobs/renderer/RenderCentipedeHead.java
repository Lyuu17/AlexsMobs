package com.github.alexthe666.alexsmobs.renderer;

import com.github.alexthe666.alexsmobs.entity.EntityCentipedeHead;
import com.github.alexthe666.alexsmobs.model.ModelCaveCentipede;
import com.github.alexthe666.alexsmobs.renderer.layer.LayerCentipedeHeadEyes;
import com.iafenvoy.uranus.client.model.AdvancedEntityModel;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityPose;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class RenderCentipedeHead extends MobEntityRenderer<EntityCentipedeHead, AdvancedEntityModel<EntityCentipedeHead>> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/cave_centipede.png");

    public RenderCentipedeHead(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelCaveCentipede<>(0), 0.5F);
        this.addFeature(new LayerCentipedeHeadEyes(this));
    }

    @Override
    protected void setupTransforms(EntityCentipedeHead entity, MatrixStack stack, float pitchIn, float yawIn, float partialTickTime) {
        if (this.isShaking(entity)) {
            yawIn += (float) (Math.cos((double) entity.age * 3.25D) * Math.PI * (double) 0.4F);
        }

        var pose = entity.getPose();
        if (pose != EntityPose.SLEEPING) {
            stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - yawIn));
        }

        if (entity.deathTime > 0) {
            float f = ((float) entity.deathTime + partialTickTime - 1.0F) / 20.0F * 1.6F;
            f = MathHelper.sqrt(f);
            if (f > 1.0F) {
                f = 1.0F;
            }
            stack.translate(0, f * 1F, 0);
            stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(f * this.getLyingAngle(entity)));
        } else if (entity.hasCustomName()) {
            var s = Formatting.strip(entity.getName().getString());
            if (("Dinnerbone".equals(s) || "Grumm".equals(s))) {
                stack.translate(0.0D, (double) (entity.getHeight() + 0.1F), 0.0D);
                stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
            }
        }
    }

    @Override
    protected float getLyingAngle(EntityCentipedeHead centipede) {
        return 180.0F;
    }

    @Override
    public Identifier getTexture(EntityCentipedeHead entity) {
        return TEXTURE;
    }
}
