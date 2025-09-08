package com.github.alexthe666.alexsmobs.model;

import com.github.alexthe666.alexsmobs.entity.EntityBoneSerpentPart;
import com.google.common.collect.ImmutableList;
import com.iafenvoy.uranus.client.model.AdvancedEntityModel;
import com.iafenvoy.uranus.client.model.AdvancedModelBox;
import com.iafenvoy.uranus.client.model.basic.BasicModelPart;
import net.minecraft.util.math.MathHelper;

public class ModelBoneSerpentBody extends AdvancedEntityModel<EntityBoneSerpentPart> {
    private final AdvancedModelBox root;
    private final AdvancedModelBox middle_section;

    public ModelBoneSerpentBody() {
        texWidth = 128;
        texHeight = 128;
        root = new AdvancedModelBox(this, "root");
        root.setPos(0.0F, 24.0F, 0.0F);
        middle_section = new AdvancedModelBox(this, "middle_section");
        middle_section.setPos(0.0F, -7.75F, 0.0F);
        root.addChild(middle_section);
        middle_section.setTextureOffset(2, 50);
        middle_section.addBox(-2.0F, -9.25F, -8.0F, 4.0F, 2.0F, 16.0F, 0.0F, false);
        middle_section.setTextureOffset(0, 0);
        middle_section.addBox(-9.0F, -7.25F, -8.0F, 18.0F, 15.0F, 16.0F, 0.0F, false);
        this.updateDefaultPose();
    }

    @Override
    public void setAngles(EntityBoneSerpentPart entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.resetToDefaultPose();
        this.middle_section.rotateAngleX = headPitch * MathHelper.RADIANS_PER_DEGREE;
        this.middle_section.rotateAngleY = netHeadYaw * MathHelper.RADIANS_PER_DEGREE;
        float walkSpeed = 0.35F;
        float walkDegree = 3F;
        float idleDegree = 0.7F;
        float idleSpeed = 0.2F;
        double walkOffset = (entityIn.getBodyIndex() + 1 ) * Math.PI * 0.5F;
        this.middle_section.rotationPointY += (float)(Math.sin( (double)(limbSwing * walkSpeed) - walkOffset) * (double)limbSwingAmount * (double)walkDegree - (double)(limbSwingAmount * walkDegree) );
        this.middle_section.rotationPointY += (float)(Math.sin( (double)(ageInTicks * idleSpeed) - walkOffset) * (double)1 * (double)idleDegree - (double)(1 * idleDegree) );
    }

    @Override
    public Iterable<BasicModelPart> parts() {
        return ImmutableList.of(root);
    }

    @Override
    public Iterable<AdvancedModelBox> getAllParts() {
        return ImmutableList.of(root, middle_section);
    }
}