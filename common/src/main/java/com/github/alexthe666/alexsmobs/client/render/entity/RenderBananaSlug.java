package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.client.model.ModelBananaSlug;
import com.github.alexthe666.alexsmobs.entity.EntityBananaSlug;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityPose;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.jetbrains.annotations.NotNull;

public class RenderBananaSlug extends MobEntityRenderer<EntityBananaSlug, ModelBananaSlug> {
    private static final Identifier TEXTURE_0 = new Identifier("alexsmobs:textures/entity/banana_slug/banana_slug_0.png");
    private static final Identifier TEXTURE_1 = new Identifier("alexsmobs:textures/entity/banana_slug/banana_slug_1.png");
    private static final Identifier TEXTURE_2 = new Identifier("alexsmobs:textures/entity/banana_slug/banana_slug_2.png");
    private static final Identifier TEXTURE_3 = new Identifier("alexsmobs:textures/entity/banana_slug/banana_slug_3.png");
    private static final Identifier TEXTURE_SLIME = new Identifier("alexsmobs:textures/entity/banana_slug/banana_slug_slime.png");

    public RenderBananaSlug(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelBananaSlug(), 0.2F);
        this.addFeature(new LayerSlime());
    }

    @Override
    protected void scale(EntityBananaSlug entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
        matrixStackIn.scale(0.9F, 0.9F, 0.9F);
    }

    private Direction rotate(Direction attachmentFacing){
        return attachmentFacing.getAxis() == Direction.Axis.Y ? Direction.UP : attachmentFacing;
    }

    private void rotateForAngle(MatrixStack matrixStackIn, Direction rotate, float f){
        if(rotate.getAxis() != Direction.Axis.Y){
            matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F * f));
        }
        switch (rotate) {
            case DOWN, NORTH:
                matrixStackIn.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F * f));
                break;
            case UP, SOUTH:
                break;
            case WEST:
                matrixStackIn.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90F * f));
                break;
            case EAST:
                matrixStackIn.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-90F * f));
                break;
        }
    }

    @Override
    protected void setupTransforms(EntityBananaSlug entityLiving, MatrixStack matrixStackIn, float ageInTicks, float rotationYaw, float partialTicks) {
        if(entityLiving.hasVehicle()){
            super.setupTransforms(entityLiving, matrixStackIn, ageInTicks, rotationYaw, partialTicks);
            return;
        }
        if (this.isShaking(entityLiving)) {
            rotationYaw += (float)(Math.cos((double)entityLiving.age * 3.25D) * Math.PI * (double)0.4F);
        }
        float trans = entityLiving.isBaby() ? 0.2F : 0.4F;
        var pose = entityLiving.getPose();
        if (pose != EntityPose.SLEEPING) {
            float progress = (entityLiving.prevAttachChangeProgress + (entityLiving.attachChangeProgress - entityLiving.prevAttachChangeProgress) * partialTicks) * 0.2F;
            float yawMul = 0F;
            if(entityLiving.prevAttachDir == entityLiving.getAttachmentFacing() && entityLiving.getAttachmentFacing().getAxis() == Direction.Axis.Y){
                yawMul = 1.0F;
            }
            matrixStackIn.multiply(RotationAxis.POSITIVE_Y.rotationDegrees ( (180.0F - yawMul * rotationYaw)));
            matrixStackIn.translate(0.0D, trans, 0.0D);
            float prevProg = 1F - progress;
            rotateForAngle(matrixStackIn, rotate(entityLiving.prevAttachDir), prevProg);
            rotateForAngle(matrixStackIn, rotate(entityLiving.getAttachmentFacing()), progress);
            if(entityLiving.getAttachmentFacing() != Direction.DOWN){
                matrixStackIn.translate(0.0D, trans, 0.0D);
                if(entityLiving.getVelocity().y <= -0.001F){
                    matrixStackIn.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(180 * progress));
                }
                matrixStackIn.translate(0.0D, -trans, 0.0D);
            }
            matrixStackIn.translate(0.0D, -trans, 0.0D);
        }

        if (entityLiving.deathTime > 0) {
            float f = ((float)entityLiving.deathTime + partialTicks - 1.0F) / 20.0F * 1.6F;
            f = MathHelper.sqrt(f);
            if (f > 1.0F) {
                f = 1.0F;
            }

            matrixStackIn.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(f * this.getLyingAngle(entityLiving)));
        } else if (entityLiving.isUsingRiptide()) {
            matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0F - entityLiving.getPitch()));
            matrixStackIn.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(((float)entityLiving.age + partialTicks) * -75.0F));
        } else if (pose == EntityPose.SLEEPING) {

        } else if (entityLiving.hasCustomName() ) {
            String s = Formatting.strip(entityLiving.getName().getString());
            if (("Dinnerbone".equals(s) || "Grumm".equals(s))) {
                matrixStackIn.translate(0.0D, entityLiving.getHeight() + 0.1F, 0.0D);
                matrixStackIn.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
            }
        }
    }

    @NotNull
    @Override
    public Identifier getTexture(EntityBananaSlug entity) {
        return switch (entity.getVariant()) {
            case 1 -> TEXTURE_1;
            case 2 -> TEXTURE_2;
            case 3 -> TEXTURE_3;
            default -> TEXTURE_0;
        };
    }

    class LayerSlime extends FeatureRenderer<EntityBananaSlug, ModelBananaSlug> {

        public LayerSlime() {
            super(RenderBananaSlug.this);
        }

        @Override
        public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntityBananaSlug entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            float alpha = entitylivingbaseIn.prevTrailVisability + (entitylivingbaseIn.trailVisability - entitylivingbaseIn.prevTrailVisability) * partialTicks;
            if(alpha > 0){
                var ivertexbuilder = bufferIn.getBuffer(RenderLayer.getEntityTranslucent(TEXTURE_SLIME));
                this.getContextModel().render(matrixStackIn, ivertexbuilder, packedLightIn, LivingEntityRenderer.getOverlay(entitylivingbaseIn, 0.0F), 1.0F, 1.0F, 1.0F, alpha);
            }
        }
    }
}
