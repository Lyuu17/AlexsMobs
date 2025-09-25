package com.github.alexthe666.alexsmobs.client.model;

import com.google.common.collect.ImmutableList;
import com.iafenvoy.uranus.client.model.AdvancedEntityModel;
import com.iafenvoy.uranus.client.model.AdvancedModelBox;
import com.iafenvoy.uranus.client.model.basic.BasicModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;

public class ModelShieldOfTheDeep extends AdvancedEntityModel<Entity> {
	private final AdvancedModelBox shield;
	private final AdvancedModelBox handle;

	public ModelShieldOfTheDeep() {
		texWidth = 64;
		texHeight = 64;

		shield = new AdvancedModelBox(this, "shield");
		shield.setPos(-2.0F, 16.0F, 0.0F);
		shield.setTextureOffset(0, 0);
		shield.addBox(-1.0F, -4.0F, -6.0F, 1.0F, 12.0F, 12.0F, 0.0F, false);
		shield.setTextureOffset(17, 15);
		shield.addBox(-3.0F, -3.0F, -5.0F, 2.0F, 10.0F, 10.0F, 0.0F, false);
		shield.setTextureOffset(27, 0);
		shield.addBox(-4.0F, -1.0F, -3.0F, 3.0F, 6.0F, 6.0F, 0.0F, false);

		handle = new AdvancedModelBox(this, "handle");
		handle.setPos(8.0F, 8.0F, -8.0F);
		shield.addChild(handle);
		handle.setTextureOffset(0, 25);
		handle.addBox(-8.0F, -8.5F, 7.0F, 5.0F, 5.0F, 2.0F, 0.0F, false);
	}

	@Override
	public Iterable<AdvancedModelBox> getAllParts() {
		return ImmutableList.of(handle, shield);
	}

	@Override
	public void setAngles(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch){
		//previously the render function, render code was moved to a method below
	}

	@Override
	public void render(MatrixStack matrixStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha){
		shield.render(matrixStack, buffer, packedLight, packedOverlay);
	}

	@Override
	public Iterable<BasicModelPart> parts() {
		return ImmutableList.of(shield);
	}
}