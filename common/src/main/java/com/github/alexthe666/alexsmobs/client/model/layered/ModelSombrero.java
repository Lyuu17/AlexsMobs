package com.github.alexthe666.alexsmobs.client.model.layered;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public class ModelSombrero<T extends LivingEntity> extends BipedEntityModel<T> {
    public ModelPart sombrero;

    public ModelSombrero(ModelPart p_170677_) {
        super(p_170677_);
    }

    public static TexturedModelData createArmorLayer(Dilation deformation) {
        var meshdefinition = BipedEntityModel.getModelData(deformation, 0.0F);
        var partdefinition = meshdefinition.getRoot();
        var head = partdefinition.getChild("head");

        head.addChild("sombrero", ModelPartBuilder.create()
                .uv(0, 64)
                .cuboid(-4.0F, -11.0F, -4.0F, 8.0F, 6.0F, 8.0F, deformation), ModelTransform.pivot(0, 0, 0));
        head.addChild("sombrero2", ModelPartBuilder.create()
                .uv(22, 73)
                .cuboid(-11.0F, -8.0F, -11.0F, 22.0F, 3.0F, 22.0F, deformation), ModelTransform.pivot(0, 0, 0));

        return TexturedModelData.of(meshdefinition, 128, 128);
    }

    public static TexturedModelData createArmorLayerAprilFools(Dilation deformation) {
        var meshdefinition = BipedEntityModel.getModelData(deformation, 0.0F);
        var partdefinition = meshdefinition.getRoot();
        var head = partdefinition.getChild("head");

        head.addChild("sombrero", ModelPartBuilder.create()
                .uv(0, 64)
                .cuboid(-4.0F, 7.0F, -4.0F, 8.0F, 6.0F, 8.0F, deformation), ModelTransform.of(0, 0, 0, MathHelper.PI, 0, MathHelper.PI * 0.1F));
        head.addChild("sombrero2", ModelPartBuilder.create()
                .uv(22, 73)
                .cuboid(-11.0F, 10.0F, -11.0F, 22.0F, 3.0F, 22.0F, deformation), ModelTransform.of(0, 0, 0, MathHelper.PI, 0, MathHelper.PI * 0.1F));

        return TexturedModelData.of(meshdefinition, 128, 128);
    }

}
