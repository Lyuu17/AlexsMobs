package com.github.alexthe666.alexsmobs.client.model.layered;

import com.github.alexthe666.alexsmobs.entity.util.Maths;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public class ModelFrontierCap<T extends LivingEntity> extends BipedEntityModel<T> {
    public ModelPart tail;
    public ModelPart hat;

    public ModelFrontierCap(ModelPart p_170677_) {
        super(p_170677_);
        this.hat = p_170677_.getChild("head").getChild("frontierhat");
        this.tail = hat.getChild("tail");
    }

    public static TexturedModelData createArmorLayer(Dilation deformation) {
        var ModelData = BipedEntityModel.getModelData(deformation, 0.0F);
        var partdefinition = ModelData.getRoot();
        var head = partdefinition.getChild("head");

        var front = head.addChild("frontierhat", ModelPartBuilder.create().uv(32, 32).cuboid(-4.0F, -10.5F, -4.0F, 8.0F, 4.0F, 8.0F, deformation), ModelTransform.pivot(0, 0, 0));
        front.addChild("tail", ModelPartBuilder.create().uv(36, 46).cuboid(-1.5F, -0.3F, -1.5F, 3.0F, 13.0F, 3.0F, deformation), ModelTransform.of(4.4F, -7.5F, 4.5F, 0.1956514098143546F, -0.03909537541112055F, -0.11728612207217244F));

        return TexturedModelData.of(ModelData, 64, 64);
    }

    public ModelFrontierCap<?> withAnimations(LivingEntity entity){
        if(entity != null){
            float partialTick = MinecraftClient.getInstance().getTickDelta();
            float limbSwingAmount = entity.limbAnimator.getSpeed(partialTick);
            float limbSwing = entity.limbAnimator.getPos() + partialTick;
            tail.pitch = 0.1956514098143546F + limbSwingAmount * Maths.rad(80) + MathHelper.cos(limbSwing * 0.3F) * 0.2F * limbSwingAmount;
            tail.yaw = -0.03909537541112055F + limbSwingAmount * Maths.rad(10) - MathHelper.cos(limbSwing * 0.4F) * 0.3F * limbSwingAmount;
            tail.roll = -0.11728612207217244F + limbSwingAmount * Maths.rad(10);
        }
        return  this;
    }

}
