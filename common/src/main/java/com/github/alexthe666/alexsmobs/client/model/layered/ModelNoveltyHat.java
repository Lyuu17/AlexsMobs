package com.github.alexthe666.alexsmobs.client.model.layered;

import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;

public class ModelNoveltyHat<T extends LivingEntity> extends BipedEntityModel<T> {

    public ModelNoveltyHat(ModelPart part) {
        super(part);
    }

    public static TexturedModelData createArmorLayer(Dilation deformation) {
        var ModelData = BipedEntityModel.getModelData(deformation, 0.0F);
        var partdefinition = ModelData.getRoot();
        var head = partdefinition.getChild("head");

        var hat = head.addChild("hat", ModelPartBuilder.create().uv(0, 30).cuboid(-4.5F, -5.0F, -4.5F, 9.0F, 6.0F, 9.0F, new Dilation(0.0F))
                .uv(31, 55).cuboid(4.5F, -2.0F, -2.5F, 4.0F, 5.0F, 4.0F, new Dilation(0.0F))
                .uv(31, 55).mirrored().cuboid(-8.5F, -2.0F, -2.5F, 4.0F, 5.0F, 4.0F, new Dilation(0.0F)).mirrored(false)
                .uv(0, 46).cuboid(-5.5F, 1.0F, -9.5F, 11.0F, 0.0F, 8.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, -6.0F, 0.0F));

        var pipes = hat.addChild("pipes", ModelPartBuilder.create().uv(0, 55).cuboid(-7.5F, 0.0F, 0.0F, 15.0F, 9.0F, 0.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 2.0F, -0.5F, -0.6545F, 0.0F, 0.0F));

        return TexturedModelData.of(ModelData, 64, 64);
    }
}