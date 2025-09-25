package com.github.alexthe666.alexsmobs.client.model.layered;

import com.github.alexthe666.alexsmobs.entity.util.Maths;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.*;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class ModelAMElytra<T extends LivingEntity> extends BipedEntityModel<T> {
    private final ModelPart rightWing;
    private final ModelPart leftWing;

    public ModelAMElytra(ModelPart part) {
        super(part);
        this.leftWing = part.getChild("body").getChild("left_wing");
        this.rightWing = part.getChild("body").getChild("right_wing");
    }

    public static TexturedModelData createLayer(Dilation deformation) {
        var ModelData = BipedEntityModel.getModelData(deformation, 0.0F);
        var partdefinition = ModelData.getRoot().getChild("body");
        var Dilation = new Dilation(1.0F);
        partdefinition.addChild("left_wing", ModelPartBuilder.create().uv(32, 32).cuboid(-10.0F, 0.0F, 0.0F, 10.0F, 20.0F, 2.0F, Dilation), ModelTransform.of(5.0F, 0.0F, 0.0F, 0.2617994F, 0.0F, -0.2617994F));
        partdefinition.addChild("right_wing", ModelPartBuilder.create().uv(32, 32).mirrored().cuboid(0.0F, 0.0F, 0.0F, 10.0F, 20.0F, 2.0F, Dilation), ModelTransform.of(-5.0F, 0.0F, 0.0F, 0.2617994F, 0.0F, 0.2617994F));
        return TexturedModelData.of(ModelData, 64, 64);
    }

    public ModelAMElytra<?> withAnimations(LivingEntity entity){
        if(entity != null) {
            final float partialTick = MinecraftClient.getInstance().getTickDelta();
            final float limbSwingAmount = entity.limbAnimator.getSpeed(partialTick);
            final float limbSwing = entity.limbAnimator.getPos() + partialTick;
            setAngles(entity, limbSwing, limbSwingAmount, entity.age + partialTick, 0, 0);
        }
        return  this;
    }

    @Override
    public void setAngles(LivingEntity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float f = 0.2617994F;
        float f1 = -0.2617994F;
        float f2 = 0.0F;
        float f3 = 0.0F;
        if (entityIn.isFallFlying()) {
            float f4 = 1.0F;
            Vec3d vector3d = entityIn.getVelocity();
            if (vector3d.y < 0.0D) {
                Vec3d vector3d1 = vector3d.normalize();
                f4 = 1.0F - (float)Math.pow(-vector3d1.y, 1.5D);
            }

            f = f4 * 0.34906584F + (1.0F - f4) * f;
            f1 = f4 * (-MathHelper.HALF_PI) + (1.0F - f4) * f1;
        } else if (entityIn.isInSneakingPose()) {
            f = 0.6981317F;
            f1 = -Maths.QUARTER_PI;
            f2 = -1.0F;
            f3 = 0.08726646F;
        }

        this.leftWing.pivotX = 5.0F;
        this.leftWing.pivotY = f2;
        if (entityIn instanceof AbstractClientPlayerEntity abstractclientplayerentity) {
            abstractclientplayerentity.elytraPitch = (float)((double)abstractclientplayerentity.elytraPitch + (double)(f - abstractclientplayerentity.elytraPitch) * 0.1D);
            abstractclientplayerentity.elytraYaw = (float)((double)abstractclientplayerentity.elytraYaw + (double)(f3 - abstractclientplayerentity.elytraYaw) * 0.1D);
            abstractclientplayerentity.elytraRoll = (float)((double)abstractclientplayerentity.elytraRoll + (double)(f1 - abstractclientplayerentity.elytraRoll) * 0.1D);
            this.leftWing.pitch = abstractclientplayerentity.elytraPitch;
            this.leftWing.yaw = abstractclientplayerentity.elytraYaw;
            this.leftWing.roll = abstractclientplayerentity.elytraRoll;
        } else {
            this.leftWing.pitch = f;
            this.leftWing.roll = f1;
            this.leftWing.yaw = f3;
        }

        this.rightWing.pivotX = -this.leftWing.pivotX;
        this.rightWing.yaw = -this.leftWing.yaw;
        this.rightWing.pivotY = this.leftWing.pivotY;
        this.rightWing.pitch = this.leftWing.pitch;
        this.rightWing.roll = -this.leftWing.roll;
    }
}
