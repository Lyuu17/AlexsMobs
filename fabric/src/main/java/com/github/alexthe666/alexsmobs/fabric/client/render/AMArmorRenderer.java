package com.github.alexthe666.alexsmobs.fabric.client.render;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.function.Function;

public class AMArmorRenderer implements ArmorRenderer {

    private BipedEntityModel<LivingEntity> model;

    private final Identifier texture;
    private final Function<ModelPart, BipedEntityModel<LivingEntity>> modelFactory;
    private final EntityModelLayer modelLayer;
    private final EquipmentSlot slot;

    public AMArmorRenderer(String path, Function<ModelPart, BipedEntityModel<LivingEntity>> modelFactory, EntityModelLayer modelLayer, EquipmentSlot slot) {
        this.texture = Identifier.of(AlexsMobs.MOD_ID, path);
        this.modelFactory = modelFactory;
        this.modelLayer = modelLayer;
        this.slot = slot;
    }

    private void setPartVisibility(BipedEntityModel<LivingEntity> model, EquipmentSlot slot) {
        model.setVisible(false);
        switch (slot) {
            case HEAD -> {
                model.head.visible = true;
                model.hat.visible = true;
            }
            case CHEST -> {
                model.body.visible = true;
                model.rightArm.visible = true;
                model.leftArm.visible = true;
            }
            case LEGS -> {
                model.body.visible = true;
                model.rightLeg.visible = true;
                model.leftLeg.visible = true;
            }
            case FEET -> {
                model.rightLeg.visible = true;
                model.leftLeg.visible = true;
            }
        }
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, ItemStack stack, LivingEntity entity, EquipmentSlot slot, int light, BipedEntityModel<LivingEntity> contextModel) {
        if (model == null) {
            model = modelFactory.apply(MinecraftClient.getInstance().getEntityModelLoader().getModelPart(modelLayer));
        }
        contextModel.copyBipedStateTo(model);
        model.setVisible(false);
        switch (this.slot) {
            case HEAD -> {
                model.head.visible = true;
                model.hat.visible = true;
            }
            case CHEST -> {
                model.body.visible = true;
                model.rightArm.visible = true;
                model.leftArm.visible = true;
            }
            case LEGS -> {
                model.body.visible = true;
                model.rightLeg.visible = true;
                model.leftLeg.visible = true;
            }
            case FEET -> {
                model.rightLeg.visible = true;
                model.leftLeg.visible = true;
            }
        }
        ArmorRenderer.renderPart(matrices, vertexConsumers, light, stack, model, this.texture);
    }
}
