package com.github.alexthe666.alexsmobs.client.model.layered;


import com.github.alexthe666.alexsmobs.entity.util.FlyingFishBootsUtil;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;

public class ModelFlyingFishBoots<T extends LivingEntity> extends BipedEntityModel<T> {
    private final ModelPart rightFish;
    private final ModelPart leftFish;
    private final ModelPart rightWingOuter;
    private final ModelPart leftWingOuter;
    private final ModelPart rightWingInner;
    private final ModelPart leftWingInner;

    public ModelFlyingFishBoots(ModelPart root) {
        super(root);
        this.rightFish = root.getChild("right_leg").getChild("RBoot");
        this.leftFish = root.getChild("left_leg").getChild("LBoot");
        this.rightWingOuter = rightFish.getChild("RwingR");
        this.leftWingOuter = leftFish.getChild("LwingL");
        this.rightWingInner = rightFish.getChild("RwingL");
        this.leftWingInner = leftFish.getChild("LwingR");
    }

    public static TexturedModelData createArmorLayer(Dilation deformation) {
        var ModelData = BipedEntityModel.getModelData(deformation, 1.0F);
        var partdefinition = ModelData.getRoot();

        var leftleg = partdefinition.getChild("left_leg");
        var rightleg = partdefinition.getChild("right_leg");

        var RBoot = rightleg.addChild("RBoot", ModelPartBuilder.create().uv(18, 12).mirrored().cuboid(-1.9F, -3.0F, -2.0F, 4.0F, 3.0F, 4.0F, new Dilation(0.3F)).mirrored(false)
                .uv(0, 25).mirrored().cuboid(0.0F, -2.0F, 2.0F, 0.0F, 4.0F, 5.0F, new Dilation(0.0F)).mirrored(false)
                .uv(0, 0).mirrored().cuboid(-2.5F, 0.0F, -5.0F, 5.0F, 2.0F, 9.0F, new Dilation(0.0F)).mirrored(false), ModelTransform.pivot(-0.1F, 10.0F, 0.0F));
        RBoot.addChild("RwingR", ModelPartBuilder.create().uv(9, 47).mirrored().cuboid(0.0F, -3.0F, 0.0F, 0.0F, 4.0F, 8.0F, new Dilation(0.0F)).mirrored(false), ModelTransform.of(-2.5F, 1.0F, -3.0F, 0.0F, -0.5672F, 0.0F));
        RBoot.addChild("RwingL", ModelPartBuilder.create().uv(0, 42).mirrored().cuboid(0.0F, -3.0F, 0.0F, 0.0F, 4.0F, 8.0F, new Dilation(0.0F)).mirrored(false), ModelTransform.of(2.5F, 1.0F, -3.0F, 0.0F, 0.5672F, 0.0F));

        var LBoot = leftleg.addChild("LBoot", ModelPartBuilder.create().uv(18, 12).cuboid(-2.1F, -3.0F, -2.0F, 4.0F, 3.0F, 4.0F, new Dilation(0.3F))
                .uv(0, 25).cuboid(0.0F, -2.0F, 2.0F, 0.0F, 4.0F, 5.0F, new Dilation(0.0F))
                .uv(0, 0).cuboid(-2.5F, 0.0F, -5.0F, 5.0F, 2.0F, 9.0F, new Dilation(0.0F)), ModelTransform.pivot(0.1F, 10.0F, 0.0F));
        LBoot.addChild("LwingL", ModelPartBuilder.create().uv(9, 47).cuboid(0.0F, -3.0F, 0.0F, 0.0F, 4.0F, 8.0F, new Dilation(0.0F)), ModelTransform.of(2.5F, 1.0F, -3.0F, 0.0F, 0.5672F, 0.0F));
        LBoot.addChild("LwingR", ModelPartBuilder.create().uv(0, 42).cuboid(0.0F, -3.0F, 0.0F, 0.0F, 4.0F, 8.0F, new Dilation(0.0F)), ModelTransform.of(-2.5F, 1.0F, -3.0F, 0.0F, -0.5672F, 0.0F));
        return TexturedModelData.of(ModelData, 64, 64);
    }

    public ModelFlyingFishBoots<T> withAnimations(LivingEntity entity){
        if(entity != null) {
            float partialTick = MinecraftClient.getInstance().getTickDelta();
            float ageInTicks = entity.age + partialTick;
            float fly = MathHelper.cos(ageInTicks * 0.2F) * 0.1F;
            float fly2 = fly * 0.35F;
            boolean flying = FlyingFishBootsUtil.getBoostTicks(entity) > 0;
            if (flying) {
                fly = (1 + MathHelper.sin(ageInTicks * 1.2F)) * 0.8F;
                fly2 = fly;
            }
            rightWingOuter.yaw = -0.5672F - fly;
            leftWingOuter.yaw = 0.5672F + fly;
            rightWingInner.yaw = 0.5672F + fly2;
            leftWingInner.yaw = -0.5672F - fly2;
            if (flying || entity.getPose() == EntityPose.SWIMMING) {
                leftFish.pitch = Maths.rad(-45);
                rightFish.pitch = Maths.rad(-45);
                rightFish.pivotY = 11.0F;
                leftFish.pivotY = 11.0F;
                rightFish.pivotZ = -1.5F;
                leftFish.pivotZ = -1.5F;
            } else if (entity.getPose() == EntityPose.CROUCHING) {
                leftFish.pitch = 0F;
                rightFish.pitch = 0F;
                rightFish.pivotY = 8.0F;
                leftFish.pivotY = 8.0F;
                rightFish.pivotZ = 0.0F;
                leftFish.pivotZ = 0.0F;
            } else {
                leftFish.pitch = 0F;
                rightFish.pitch = 0F;
                rightFish.pivotY = 10.0F;
                leftFish.pivotY = 10.0F;
                rightFish.pivotZ = 0.0F;
                leftFish.pivotZ = 0.0F;
            }
        }
        return this;
    }
}
