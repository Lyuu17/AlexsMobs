package com.github.alexthe666.alexsmobs.client.model.layered;

import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;

public class ModelFedora<T extends LivingEntity> extends BipedEntityModel<T> {

    public ModelFedora(ModelPart part) {
        super(part);
    }

    public static TexturedModelData createArmorLayer(Dilation deformation) {
        var ModelData = BipedEntityModel.getModelData(deformation, 0.0F);
        var partdefinition = ModelData.getRoot();
        var head = partdefinition.getChild("head");

        head.addChild("fedora", ModelPartBuilder.create().uv(0, 44).cuboid(-3.0F, -3.55F, -3.0F, 6.0F, 4.0F, 6.0F, deformation), ModelTransform.pivot(0, -8, 0));
        head.addChild("fedora_shade", ModelPartBuilder.create().uv(0, 32).cuboid(-5.0F, -0.5F, -5.0F, 10.0F, 1.0F, 10.0F, deformation), ModelTransform.pivot(0, -8.05F, 0));

        return TexturedModelData.of(ModelData, 64, 64);
    }

   /* public ModelFedora(float modelSize) {
        super(modelSize, 0, 64, 64);
        texWidth = 64;
        texHeight = 64;
        fedora = new ModelPart(this);
        fedora.setPos(0.0F, 8F, 0.0F);
        fedora.setTextureOffset(0, 44).addBox(-3.0F, -3.55F, -3.0F, 6.0F, 4.0F, 6.0F, modelSize, false);
        fedora_shade = new ModelPart(this);
        fedora_shade.setPos(0.0F, -0.05F, 0.0F);
        fedora_shade.setTextureOffset(0, 32).addBox(-5.0F, -0.5F, -5.0F, 10.0F, 1.0F, 10.0F, modelSize, false);
        head.addChild(fedora);
        fedora.addChild(fedora_shade);
    }*/
}