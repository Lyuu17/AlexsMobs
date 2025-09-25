package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.entity.EntityTerrapin;
import com.github.alexthe666.alexsmobs.entity.util.TerrapinTypes;
import com.github.alexthe666.alexsmobs.client.model.ModelTerrapin;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityPose;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.jetbrains.annotations.NotNull;

public class RenderTerrapin extends MobEntityRenderer<EntityTerrapin, ModelTerrapin> {

    private static final Identifier[] SHELL_TEXTURES = {
            new Identifier("alexsmobs:textures/entity/terrapin/overlay/terrapin_shell_pattern_0.png"),
            new Identifier("alexsmobs:textures/entity/terrapin/overlay/terrapin_shell_pattern_1.png"),
            new Identifier("alexsmobs:textures/entity/terrapin/overlay/terrapin_shell_pattern_2.png"),
            new Identifier("alexsmobs:textures/entity/terrapin/overlay/terrapin_shell_pattern_3.png"),
            new Identifier("alexsmobs:textures/entity/terrapin/overlay/terrapin_shell_pattern_4.png"),
            new Identifier("alexsmobs:textures/entity/terrapin/overlay/terrapin_shell_pattern_5.png")
    };
    private static final Identifier[] SKIN_PATTERN_TEXTURES = {
            new Identifier("alexsmobs:textures/entity/terrapin/overlay/terrapin_skin_pattern_0.png"),
            new Identifier("alexsmobs:textures/entity/terrapin/overlay/terrapin_skin_pattern_1.png"),
            new Identifier("alexsmobs:textures/entity/terrapin/overlay/terrapin_skin_pattern_2.png"),
            new Identifier("alexsmobs:textures/entity/terrapin/overlay/terrapin_skin_pattern_3.png")
    };

    public RenderTerrapin(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelTerrapin(), 0.3F);
        this.addFeature(new TurtleOverlayLayer(this, 0));
        this.addFeature(new TurtleOverlayLayer(this, 1));
        this.addFeature(new TurtleOverlayLayer(this, 2));
    }

    @Override
    protected void scale(EntityTerrapin entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
    }


    @NotNull
    @Override
    public Identifier getTexture(EntityTerrapin entity) {
        if(entity.isKoopa()){
            return TerrapinTypes.KOOPA.getTexture();
        }
        return entity.getTurtleType().getTexture();
    }

    @Override
    protected void setupTransforms(EntityTerrapin entity, MatrixStack stack, float pitchIn, float yawIn, float partialTickTime) {
        if (this.isShaking(entity)) {
            yawIn += (float)(Math.cos((double)entity.age * 3.25D) * Math.PI * (double)0.4F);
        }
        var pose = entity.getPose();
        if (pose != EntityPose.SLEEPING && !entity.isSpinning()) {
            stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - yawIn));
        }

        if (entity.deathTime > 0) {
            float f = ((float)entity.deathTime + partialTickTime - 1.0F) / 20.0F * 1.6F;
            f = MathHelper.sqrt(f);
            if (f > 1.0F) {
                f = 1.0F;
            }

            stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(f * this.getLyingAngle(entity)));
        } else if (entity.isUsingRiptide()) {
            stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0F - entity.getPitch()));
            stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(((float)entity.age + partialTickTime) * -75.0F));
        } else if (pose == EntityPose.SLEEPING) {
        } else if (shouldFlipUpsideDown(entity)) {
            stack.translate(0.0D, entity.getHeight() + 0.1F, 0.0D);
            stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
        }
    }

    static class TurtleOverlayLayer extends FeatureRenderer<EntityTerrapin, ModelTerrapin> {

        private final int layer;

        public TurtleOverlayLayer(RenderTerrapin render, int layer) {
            super(render);
            this.layer = layer;
        }

        @Override
        public void render(MatrixStack matrixStackIn, VertexConsumerProvider buffer, int packedLightIn, EntityTerrapin turtle, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            if(turtle.getTurtleType() == TerrapinTypes.OVERLAY && !turtle.isKoopa()){
                Identifier tex = layer == 0 ? this.getTexture(turtle) : layer == 1 ? SHELL_TEXTURES[turtle.getShellType() % SHELL_TEXTURES.length] : SKIN_PATTERN_TEXTURES[turtle.getSkinType() % SKIN_PATTERN_TEXTURES.length];
                int color = layer == 0 ? turtle.getTurtleColor() : layer == 1 ? turtle.getShellColor() : turtle.getSkinColor();
                float r = (float) (color >> 16 & 255) / 255.0F;
                float g = (float) (color >> 8 & 255) / 255.0F;
                float b = (float) (color & 255) / 255.0F;
                renderModel(getContextModel(), tex, matrixStackIn, buffer, packedLightIn, turtle, r, g, b);
            }
        }
    }

}
