package com.github.alexthe666.alexsmobs.client.model;

import com.github.alexthe666.alexsmobs.block.entity.EndPirateFlagBlockEntity;
import com.google.common.collect.ImmutableList;
import com.iafenvoy.uranus.client.model.AdvancedEntityModel;
import com.iafenvoy.uranus.client.model.AdvancedModelBox;
import com.iafenvoy.uranus.client.model.basic.BasicModelPart;
import net.minecraft.entity.Entity;

public class ModelEndPirateFlag extends AdvancedEntityModel<Entity> {
    private final AdvancedModelBox root;
    private final AdvancedModelBox rod;
    private final AdvancedModelBox flag1;
    private final AdvancedModelBox flag2;

    public ModelEndPirateFlag() {
        texWidth = 64;
        texHeight = 64;
        root = new AdvancedModelBox(this, "root");
        root.setRotationPoint(0.0F, 24.0F, 0.0F);

        rod = new AdvancedModelBox(this, "rod");
        rod.setRotationPoint(0.0F, 0.0F, 0.0F);
        root.addChild(rod);
        rod.setTextureOffset(29, 0);
        rod.addBox(-1.0F, -16.0F, -1.0F, 2.0F, 16.0F, 2.0F, 0.0F, false);

        flag1 = new AdvancedModelBox(this, "flag1");
        flag1.setRotationPoint(0.0F, -8.0F, 0.0F);
        rod.addChild(flag1);
        flag1.setTextureOffset(0, 17);
        flag1.addBox(1.0F, -8.0F, 0.0F, 14.0F, 16.0F, 0.0F, 0.0F, false);

        flag2 = new AdvancedModelBox(this, "flag2");
        flag2.setRotationPoint(15.0F, 0.0F, 0.0F);
        flag1.addChild(flag2);
        flag2.setTextureOffset(0, 0);
        flag2.addBox(0.0F, -8.0F, 0.0F, 14.0F, 16.0F, 0.0F, 0.0F, false);
        this.updateDefaultPose();
    }

    @Override
    public Iterable<BasicModelPart> parts() {
        return ImmutableList.of(root);
    }

    @Override
    public Iterable<AdvancedModelBox> getAllParts() {
        return ImmutableList.of(root, rod, flag1, flag2);
    }

    @Override
    public void setAngles(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }

    public void renderFlag(EndPirateFlagBlockEntity wheel, float partialTick) {
        this.resetToDefaultPose();
        float f = wheel.ticksExisted + partialTick;
        float speed = (float) (0.6F + Math.sin(f * 0.1F) * 0.5F);
        this.swing(flag1, 0.4F, 0.5F, false, 0.0F, 0F, f, speed);
        this.swing(flag2, 0.4F, 0.5F, false, -2.0F, 0F, f, speed);
    }
}
