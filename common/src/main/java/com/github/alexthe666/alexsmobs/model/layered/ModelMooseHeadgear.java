package com.github.alexthe666.alexsmobs.model.layered;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;

@Environment(EnvType.CLIENT)
public class ModelMooseHeadgear<T extends LivingEntity> extends BipedEntityModel<T> {

    public ModelMooseHeadgear(ModelPart p_170677_) {
        super(p_170677_);
    }

    public static TexturedModelData createArmorLayer(Dilation deformation) {
        var ModelData = BipedEntityModel.getModelData(deformation, 0.0F);
        var partdefinition = ModelData.getRoot();
        var head = partdefinition.getChild("head");

        head.addChild("hornL", ModelPartBuilder.create().uv(3, 17).cuboid(0.0F, -5.5F, -4.0F, 10.0F, 6.0F, 8.0F, deformation), ModelTransform.pivot(5.0F, -8.0F, 1.0F));
        head.addChild("hornR", ModelPartBuilder.create().uv(3, 17).mirrored().cuboid(-10.0F, -5.5F, -4.0F, 10.0F, 6.0F, 8.0F, deformation), ModelTransform.pivot(-5.0F, -8.0F, 1.0F));

        return TexturedModelData.of(ModelData, 64, 32);
    }
}
