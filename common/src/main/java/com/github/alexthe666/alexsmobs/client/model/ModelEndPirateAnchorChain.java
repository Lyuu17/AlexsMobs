package com.github.alexthe666.alexsmobs.client.model;

import com.github.alexthe666.alexsmobs.block.entity.EndPirateAnchorBlockEntity;
import com.google.common.collect.ImmutableList;
import com.iafenvoy.uranus.client.model.AdvancedEntityModel;
import com.iafenvoy.uranus.client.model.AdvancedModelBox;
import com.iafenvoy.uranus.client.model.basic.BasicModelPart;
import net.minecraft.entity.Entity;

public class ModelEndPirateAnchorChain extends AdvancedEntityModel<Entity> {
    private final AdvancedModelBox root;
    private final AdvancedModelBox chain;

    public ModelEndPirateAnchorChain() {
        texWidth = 32;
        texHeight = 32;

        root = new AdvancedModelBox(this, "root");
        root.setRotationPoint(0.0F, 24.0F, 0.0F);


        chain = new AdvancedModelBox(this, "chain");
        chain.setRotationPoint(0.0F, -12.5F, 0.0F);
        root.addChild(chain);
        chain.setTextureOffset(0, 7);
        chain.addBox(-5.5F, 9.5F, -1.5F, 11.0F, 3.0F, 3.0F, 0.0F, false);
        chain.setTextureOffset(0, 0);
        chain.addBox(-5.5F, -1.5F, -1.5F, 11.0F, 3.0F, 3.0F, 0.0F, false);
        chain.setTextureOffset(13, 14);
        chain.addBox(2.5F, 1.5F, -1.5F, 3.0F, 8.0F, 3.0F, 0.0F, false);
        chain.setTextureOffset(0, 14);
        chain.addBox(-5.5F, 1.5F, -1.5F, 3.0F, 8.0F, 3.0F, 0.0F, false);
        this.updateDefaultPose();
    }

    @Override
    public Iterable<BasicModelPart> parts() {
        return ImmutableList.of(root);
    }

    @Override
    public Iterable<AdvancedModelBox> getAllParts() {
        return ImmutableList.of(root, chain);
    }

    @Override
    public void setAngles(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }

    public void renderAnchor(EndPirateAnchorBlockEntity anchor, float partialTick, boolean east) {
        this.resetToDefaultPose();
    }
}