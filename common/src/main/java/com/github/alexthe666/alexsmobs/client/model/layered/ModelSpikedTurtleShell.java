package com.github.alexthe666.alexsmobs.client.model.layered;

import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;

public class ModelSpikedTurtleShell<T extends LivingEntity> extends BipedEntityModel<T> {

	public ModelSpikedTurtleShell(ModelPart p_170677_) {
		super(p_170677_);
	}

	public static TexturedModelData createArmorLayer(Dilation deformation) {
		var ModelData = BipedEntityModel.getModelData(deformation, 0.0F);
		var partdefinition = ModelData.getRoot();
		var head = partdefinition.getChild("head");

		head.addChild("spikes1", ModelPartBuilder.create().uv(34, 15).cuboid(0.0F, -33F, -4.5F, 4.0F, 1.0F, 9.0F, deformation), ModelTransform.pivot(0, 24.0F, 0));
		head.addChild("spikes2", ModelPartBuilder.create().uv(34, 15).cuboid(-4.0F, -33F, -4.5F, 4.0F, 1.0F, 9.0F, deformation), ModelTransform.pivot(0, 24.0F, 0));

		return TexturedModelData.of(ModelData, 64, 32);
	}
}