package com.github.alexthe666.alexsmobs.model.layered;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.entity.LivingEntity;

@Environment(EnvType.CLIENT)
public class ModelRoadrunnerBoots<T extends LivingEntity> extends net.minecraft.client.render.entity.model.BipedEntityModel<T> {

    public ModelRoadrunnerBoots(ModelPart p_170677_) {
        super(p_170677_);
    }

    public static TexturedModelData createArmorLayer(Dilation deformation) {
        var ModelData = net.minecraft.client.render.entity.model.BipedEntityModel.getModelData(deformation, 1.0F);
        var partdefinition = ModelData.getRoot();
        var leftleg = partdefinition.getChild("left_leg");
        var rightleg = partdefinition.getChild("right_leg");

        rightleg.addChild("featherr", ModelPartBuilder.create().uv(20, 22).cuboid(-3.0F, -7.5F, 0.0F, 3.0F, 8.0F, 0.0F,  new Dilation(0)), ModelTransform.of(-1.5F, 9.5F, 0.4F, 0.0F, 0.9773843811168246F, -0.3127630032889644F));
        leftleg.addChild("featherl", ModelPartBuilder.create().uv(20, 22).mirrored().cuboid(0.0F, -7.4F, 0.0F, 3.0F, 8.0F, 0.0F,  new Dilation(0)), ModelTransform.of(1.5F, 9.5F, -0.4F, 0.0F, -0.9773843811168246F, 0.3127630032889644F));

        return TexturedModelData.of(ModelData, 64, 32);
    }
}
