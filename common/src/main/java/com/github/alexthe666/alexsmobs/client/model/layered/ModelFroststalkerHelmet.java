package com.github.alexthe666.alexsmobs.client.model.layered;

import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;

public class ModelFroststalkerHelmet<T extends LivingEntity> extends BipedEntityModel<T> {

    public ModelFroststalkerHelmet(ModelPart part) {
        super(part);
    }

    public static TexturedModelData createArmorLayer(Dilation deformation) {
        var ModelData = BipedEntityModel.getModelData(deformation, 0.0F);
        var partdefinition = ModelData.getRoot();
        var head = partdefinition.getChild("head");
        head.addChild("frost", ModelPartBuilder.create().uv(0, 17).cuboid(-3.0F, -10.2F, -2.8F, 6.0F, 4.0F, 9.0F, deformation), ModelTransform.pivot(0, 0, 0));
        head.addChild("horn", ModelPartBuilder.create().uv(29, 29).cuboid(-1.0F, -7.0F, 2.0F, 2.0F, 8.0F, 2.0F, new Dilation(0.2F)), ModelTransform.of(0.0F, -6.0F, -3.0F, 1.0472F, 0.0F, 0.0F));
        return TexturedModelData.of(ModelData, 64, 64);
    }
}