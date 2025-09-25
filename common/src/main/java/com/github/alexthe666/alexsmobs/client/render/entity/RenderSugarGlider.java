package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.client.model.ModelSugarGlider;
import com.github.alexthe666.alexsmobs.entity.EntitySugarGlider;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;

public class RenderSugarGlider extends MobEntityRenderer<EntitySugarGlider, ModelSugarGlider> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/sugar_glider.png");

    public RenderSugarGlider(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelSugarGlider(), 0.35F);
    }

    private Direction rotate(Direction attachmentFacing){
        return attachmentFacing.getAxis() == Direction.Axis.Y ? Direction.UP : attachmentFacing;
    }

    @Override
    protected void setupTransforms(EntitySugarGlider entityLiving, MatrixStack matrixStackIn, float ageInTicks, float rotationYaw, float partialTicks) {
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
            float prevProg = (entityLiving.prevAttachChangeProgress + (entityLiving.attachChangeProgress - entityLiving.prevAttachChangeProgress) * partialTicks);
            float yawMul = 0F;
            if(entityLiving.prevAttachDir == entityLiving.getAttachmentFacing() && entityLiving.getAttachmentFacing().getAxis() == Direction.Axis.Y){
                yawMul = 1.0F;
            }
            matrixStackIn.multiply(RotationAxis.POSITIVE_Y.rotationDegrees ( (180.0F - yawMul * rotationYaw)));

            if(entityLiving.getAttachmentFacing() == Direction.DOWN){
                matrixStackIn.translate(0.0D, trans, 0.0D);
                if(entityLiving.prevY <= entityLiving.getY()){
                    matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90 * prevProg));
                }else{
                    matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90 * prevProg));
                }
                matrixStackIn.translate(0.0D, -trans, 0.0D);
            }

            matrixStackIn.translate(0.0D, trans, 0.0D);
            Quaternionf current = rotate(entityLiving.getAttachmentFacing()).getRotationQuaternion();
            current.mul(1F - prevProg);
            matrixStackIn.multiply(current);
            //Quaternionf prev = rotate(entityLiving.prevAttachDir).getRotation();
            //prev.mul(prevProg);
            //matrixStackIn.multiply(prev);
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
        //} else if (pose == EntityPose.SLEEPING) {
        } else if (entityLiving.hasCustomName() ) {
            String s = Formatting.strip(entityLiving.getName().getString());
            if (("Dinnerbone".equals(s) || "Grumm".equals(s))) {
                matrixStackIn.translate(0.0D, (double)(entityLiving.getHeight() + 0.1F), 0.0D);
                matrixStackIn.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
            }
        }
    }

    @Override
    protected void scale(EntitySugarGlider mob, MatrixStack matrixStackIn, float partialTickTime) {
        if(mob.hasVehicle() && mob.getVehicle() != null) {
            if (mob.getVehicle() instanceof PlayerEntity mount) {
                var playerRender = MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(mount);
                if(MinecraftClient.getInstance().player == mount && MinecraftClient.getInstance().options.getPerspective() == Perspective.FIRST_PERSON){
                    //handled via event
                }else if (playerRender instanceof LivingEntityRenderer && ((LivingEntityRenderer) playerRender).getModel() instanceof BipedEntityModel) {
                    matrixStackIn.translate(0.0F, 0.5F, 0.0F);
                    ((BipedEntityModel) ((LivingEntityRenderer) playerRender).getModel()).head.rotate(matrixStackIn);
                    matrixStackIn.translate(0.0F, -0.5F, 0.0F);
                }
            }
        }
    }

    @NotNull
    @Override
    public Identifier getTexture(EntitySugarGlider entity) {
        return TEXTURE;
    }
}

