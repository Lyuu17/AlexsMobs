package com.github.alexthe666.alexsmobs.client.model.layered;

import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;

public class ModelUnsettlingKimono<T extends LivingEntity> extends BipedEntityModel<T> {
    private final ModelPart body;
    private final ModelPart left_arm;
    private final ModelPart right_arm;

    public ModelUnsettlingKimono(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.left_arm = root.getChild("left_arm");
        this.right_arm = root.getChild("right_arm");
    }

    public static TexturedModelData createArmorLayer(Dilation deformation) {
        var ModelData = BipedEntityModel.getModelData(new Dilation(0.25F), 0.0F);
        var partdefinition = ModelData.getRoot();
        var playerBody = partdefinition.getChild("body");
        var playerLeftArm = partdefinition.getChild("left_arm");
        var playerRightArm = partdefinition.getChild("right_arm");

        var body = playerBody.addChild("body", ModelPartBuilder.create().uv(0, 0).cuboid(-4.0F, 0.0F, -2.0F, 8.0F, 17.0F, 4.0F, new Dilation(0.75F)), ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        var left_arm = playerLeftArm.addChild("left_arm", ModelPartBuilder.create().uv(21, 18).cuboid(-1.0F, -2.0F, -2.0F, 4.0F, 16.0F, 4.0F, new Dilation(0.6F)), ModelTransform.pivot(-0.5F, 0.0F, 0.0F));
        var right_arm = playerRightArm.addChild("right_arm", ModelPartBuilder.create().uv(21, 18).mirrored().cuboid(-3.0F, -2.0F, -2.0F, 4.0F, 16.0F, 4.0F, new Dilation(0.6F)).mirrored(false), ModelTransform.pivot(0.5F, 0.0F, 0.0F));

        return TexturedModelData.of(ModelData, 64, 64);
    }

    @Override
    public void setAngles(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

    }
}