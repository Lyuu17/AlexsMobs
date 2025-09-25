package com.github.alexthe666.alexsmobs.client.model;

import com.github.alexthe666.alexsmobs.entity.EntityMurmurHead;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.google.common.collect.ImmutableList;
import com.iafenvoy.uranus.client.model.AdvancedEntityModel;
import com.iafenvoy.uranus.client.model.AdvancedModelBox;
import com.iafenvoy.uranus.client.model.basic.BasicModelPart;
import net.minecraft.util.math.MathHelper;

public class ModelMurmurHead extends AdvancedEntityModel<EntityMurmurHead> {
    private final AdvancedModelBox root;
    private final AdvancedModelBox head;
    private final AdvancedModelBox backHair;
    private final AdvancedModelBox leftHair;
    private final AdvancedModelBox rightHair;

    public ModelMurmurHead() {
        texWidth = 128;
        texHeight = 128;

        root = new AdvancedModelBox(this, "root");
        root.setRotationPoint(0.0F, 24.0F, 0.0F);


        head = new AdvancedModelBox(this, "head");
        head.setRotationPoint(0.0F, -1.0F, 0.0F);
        root.addChild(head);
        head.setTextureOffset(37, 41);
        head.addBox(-4.5F, -8.0F, -4.5F, 9.0F, 9.0F, 9.0F, 0.0F, false);
        head.setTextureOffset(0, 41);
        head.addBox(-4.5F, -8.0F, -4.5F, 9.0F, 9.0F, 9.0F, 0.2F, false);

        backHair = new AdvancedModelBox(this, "backHair");
        backHair.setRotationPoint(0.0F, -5.0F, 5.0F);
        head.addChild(backHair);
        backHair.setTextureOffset(49, 0);
        backHair.addBox(-5.5F, -2.0F, -1.5F, 11.0F, 20.0F, 3.0F, 0.0F, false);

        leftHair = new AdvancedModelBox(this, "leftHair");
        leftHair.setRotationPoint(4.5F, -5.0F, 1.0F);
        head.addChild(leftHair);
        leftHair.setTextureOffset(17, 60);
        leftHair.addBox(-1.0F, -2.0F, -2.5F, 2.0F, 16.0F, 5.0F, 0.0F, false);

        rightHair = new AdvancedModelBox(this, "rightHair");
        rightHair.setRotationPoint(-4.5F, -5.0F, 1.0F);
        head.addChild(rightHair);
        rightHair.setTextureOffset(17, 60);
        rightHair.addBox(-1.0F, -2.0F, -2.5F, 2.0F, 16.0F, 5.0F, 0.0F, true);

     this.updateDefaultPose();
    }

    @Override
    public Iterable<AdvancedModelBox> getAllParts() {
        return ImmutableList.of(root, head, leftHair, rightHair, backHair);
    }

    @Override
    public Iterable<BasicModelPart> parts() {
        return ImmutableList.of(root);
    }

    public void animateHair(float ageInTicks){
        float idleSpeed = 0.05F;
        float idleDegree = 0.1F;
        this.walk(backHair, idleSpeed, idleDegree * 0.5F, false, 0F, -0.05F, ageInTicks, 1);
        this.flap(rightHair, idleSpeed, idleDegree * 0.5F, false, 1F, 0.05F, ageInTicks, 1);
        this.flap(leftHair, idleSpeed, idleDegree * 0.5F, true, 1F, 0.05F, ageInTicks, 1);
    }

    @Override
    public void setAngles(EntityMurmurHead entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.resetToDefaultPose();
        float partialTicks = ageInTicks - entity.age;
        float angerProgress = entity.prevAngerProgress + (entity.angerProgress - entity.prevAngerProgress) * partialTicks;
        //hair physics
        if(ageInTicks > 5F){
            float hairAnimateScale = Math.min(1F, (ageInTicks - 5F) / 10F);
            double d0 = MathHelper.lerp((double)partialTicks, entity.prevXHair, entity.xHair) - MathHelper.lerp((double)partialTicks, entity.prevX, entity.getX());
            double d1 = MathHelper.lerp((double)partialTicks, entity.prevYHair, entity.yHair) - MathHelper.lerp((double)partialTicks, entity.prevY, entity.getY());
            double d2 = MathHelper.lerp((double)partialTicks, entity.prevZHair, entity.zHair) - MathHelper.lerp((double)partialTicks, entity.prevZ, entity.getZ());
            float f = entity.prevBodyYaw + (entity.bodyYaw - entity.prevBodyYaw);
            double d3 = (double)MathHelper.sin(f * MathHelper.RADIANS_PER_DEGREE);
            double d4 = (double)(-MathHelper.cos(f * MathHelper.RADIANS_PER_DEGREE));
            float f1 = (float)d1 * 10.0F;
            f1 = MathHelper.clamp(f1, -6.0F, 32.0F) * hairAnimateScale;
            float f2 = (float)(d0 * d3 + d2 * d4) * 100.0F;
            f2 = MathHelper.clamp(f2, 0.0F, 150.0F) * hairAnimateScale;
            float f3 = (float)(d0 * d4 - d2 * d3) * 100.0F;
            f3 = MathHelper.clamp(f3, -20.0F, 20.0F) * hairAnimateScale;
            if (f2 < 0.0F) {
                f2 = 0.0F;
            }
            f1 += MathHelper.sin(MathHelper.lerp(partialTicks, entity.prevHorizontalSpeed, entity.horizontalSpeed) * 6.0F) * 32.0F * 1F;
            float hairX = Maths.rad(6.0F + f2 / 2.0F + f1 - 180);
            float hairY = Maths.rad(f3 / 2.0F);
            float hairZ = Maths.rad(180.0F - f3 / 2.0F);
            this.backHair.rotateAngleX -= hairX;
            this.backHair.rotateAngleY -= hairY;
            this.backHair.rotateAngleZ -= hairZ;
            this.rightHair.rotateAngleX -= hairX;
            this.rightHair.rotateAngleY -= hairY;
            this.rightHair.rotateAngleZ -= hairZ;
            this.leftHair.rotateAngleX -= hairX;
            this.leftHair.rotateAngleY -= hairY;
            this.leftHair.rotateAngleZ -= hairZ;
        }
        this.animateHair(ageInTicks);
        this.faceTarget(netHeadYaw, headPitch, 1, head);
        progressRotationPrev(this.backHair, angerProgress, Maths.rad(-20F), 0, 0, 5F);
        progressRotationPrev(this.rightHair, angerProgress, Maths.rad(-10F), 0, Maths.rad(25F), 5F);
        progressRotationPrev(this.leftHair, angerProgress, Maths.rad(-10F), 0, Maths.rad(-25F), 5F);

    }
}
