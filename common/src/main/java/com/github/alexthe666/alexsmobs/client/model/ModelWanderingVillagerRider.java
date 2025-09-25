package com.github.alexthe666.alexsmobs.client.model;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.VillagerResemblingModel;
import net.minecraft.entity.Entity;

public class ModelWanderingVillagerRider<T extends Entity> extends VillagerResemblingModel<T> {
    private ModelPart rightLegRider;
    private ModelPart leftLegRider;


    public ModelWanderingVillagerRider(ModelPart part) {
        super(part);
        this.rightLegRider = part.getChild("right_leg");
        this.leftLegRider = part.getChild("left_leg");
    }

    @Override
    public void setAngles(T entity, float f, float f1, float f2, float f3, float f4) {
        super.setAngles(entity, f, f1, f2, f3, f4);
        if (riding) {
            this.rightLegRider.pitch = -1.4137167F;
            this.rightLegRider.yaw = 0.31415927F;
            this.rightLegRider.roll = 0.07853982F;
            this.leftLegRider.pitch = -1.4137167F;
            this.leftLegRider.yaw = -0.31415927F;
            this.leftLegRider.roll = -0.07853982F;
        } else {
            this.rightLegRider.yaw = 0F;
            this.rightLegRider.roll = 0F;
            this.leftLegRider.yaw = 0F;
            this.leftLegRider.roll = 0F;
        }
    }
}