package com.github.alexthe666.alexsmobs.client.model.layered;

import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;

public class ModelRockyChestplate<T extends LivingEntity> extends BipedEntityModel<T> {
    private final ModelPart Body;
    private final ModelPart LeftArm;
    private final ModelPart RightArm;

    public ModelRockyChestplate(ModelPart root) {
        super(root);
        this.Body = root.getChild("body").getChild("BodyRocky");
        this.LeftArm = root.getChild("left_arm").getChild("LeftArmRocky");
        this.RightArm = root.getChild("right_arm").getChild("RightArmRocky");
    }

    public static TexturedModelData createArmorLayer(Dilation deformation) {
        var ModelData = BipedEntityModel.getModelData(new Dilation(0.25F), 0.0F);
        var partdefinition = ModelData.getRoot();
        var playerBody = partdefinition.getChild("body");
        var playerLeftArm = partdefinition.getChild("left_arm");
        var playerRightArm = partdefinition.getChild("right_arm");

        var bodyRocky = playerBody.addChild("BodyRocky", ModelPartBuilder.create().uv(0, 0).cuboid(-5.0F, -0.5F, -2.0F, 10.0F, 13.0F, 9.0F, deformation)
                .uv(0, 23).cuboid(-4.0F, 0.5F, 6.0F, 8.0F, 11.0F, 4.0F, deformation)
                .uv(25, 34).cuboid(-2.0F, -0.5F, 6.0F, 4.0F, 13.0F, 4.0F, deformation), ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        var leftArmRocky = playerLeftArm.addChild("LeftArmRocky", ModelPartBuilder.create().uv(25, 23).cuboid(-1.0F, -5F, -2.1F, 6.0F, 4.0F, 6.0F, deformation)
                .uv(0, 39).cuboid(0.0F, -7.1F, -1.1F, 7.0F, 6.0F, 4.0F, deformation), ModelTransform.pivot(-1.0F, 2.0F, 0.0F));

        var rightArmRocky = playerRightArm.addChild("RightArmRocky", ModelPartBuilder.create().uv(25, 23).mirrored().cuboid(-5.0F, -5F, -2.1F, 6.0F, 4.0F, 6.0F, deformation).mirrored(false)
                .uv(0, 39).mirrored().cuboid(-7.0F, -7.1F, -1.1F, 7.0F, 6.0F, 4.0F, deformation).mirrored(false), ModelTransform.pivot(1.0F, 2.0F, 0.0F));

        return TexturedModelData.of(ModelData, 64, 64);
    }

    @Override
    public void setAngles(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

    }
}