package com.github.alexthe666.alexsmobs.client.model;

import com.github.alexthe666.alexsmobs.entity.EntityOrca;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.google.common.collect.ImmutableList;
import com.iafenvoy.uranus.animation.IAnimatedEntity;
import com.iafenvoy.uranus.client.model.AdvancedEntityModel;
import com.iafenvoy.uranus.client.model.AdvancedModelBox;
import com.iafenvoy.uranus.client.model.ModelAnimator;
import com.iafenvoy.uranus.client.model.basic.BasicModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class ModelOrca extends AdvancedEntityModel<EntityOrca> {
    private final AdvancedModelBox root;
    private final AdvancedModelBox body;
    private final AdvancedModelBox fintop;
    private final AdvancedModelBox fin_left;
    private final AdvancedModelBox fin_right;
    private final AdvancedModelBox tail1;
    private final AdvancedModelBox tail2;
    private final AdvancedModelBox tailend;
    private final AdvancedModelBox head;
    private final AdvancedModelBox jaw;
    private final ModelAnimator animator;

    public ModelOrca() {
        texWidth = 256;
        texHeight = 256;

        root = new AdvancedModelBox(this, "root");
        root.setPos(0.0F, 24.0F, 0.0F);

        body = new AdvancedModelBox(this, "body");
        body.setPos(0.0F, -1.3333F, -0.0833F);
        root.addChild(body);
        body.setTextureOffset(0, 0);
        body.addBox(-9.0F, -14.6667F, -16.9167F, 18.0F, 16.0F, 33.0F, 0.0F, false);

        fintop = new AdvancedModelBox(this, "fintop");
        fintop.setPos(0.0F, -14.6667F, -2.4167F);
        body.addChild(fintop);
        setRotationAngle(fintop, -0.2182F, 0.0F, 0.0F);
        fintop.setTextureOffset(0, 0);
        fintop.addBox(-1.0F, -16.0F, -1.5F, 2.0F, 18.0F, 8.0F, 0.0F, false);

        fin_left = new AdvancedModelBox(this, "fin_left");
        fin_left.setPos(8.5F, -0.1667F, -8.9167F);
        body.addChild(fin_left);
        setRotationAngle(fin_left, -0.6109F, 1.2217F, 0.0F);
        fin_left.setTextureOffset(0, 92);
        fin_left.addBox(-7.5F, -1.5F, -3.0F, 12.0F, 2.0F, 17.0F, 0.0F, false);

        fin_right = new AdvancedModelBox(this, "fin_right");
        fin_right.setPos(-8.5F, -0.1667F, -8.9167F);
        body.addChild(fin_right);
        setRotationAngle(fin_right, -0.6109F, -1.2217F, 0.0F);
        fin_right.setTextureOffset(0, 92);
        fin_right.addBox(-4.5F, -1.5F, -3.0F, 12.0F, 2.0F, 17.0F, 0.0F, true);

        tail1 = new AdvancedModelBox(this, "tail1");
        tail1.setPos(0.0F, -5.9167F, 15.5833F);
        body.addChild(tail1);
        tail1.setTextureOffset(70, 0);
        tail1.addBox(-7.0F, -6.75F, 0.5F, 14.0F, 13.0F, 18.0F, 0.0F, false);

        tail2 = new AdvancedModelBox(this, "tail2");
        tail2.setPos(0.0F, 0.25F, 16.5F);
        tail1.addChild(tail2);
        tail2.setTextureOffset(43, 97);
        tail2.addBox(-5.0F, -4.0F, 2.0F, 10.0F, 9.0F, 16.0F, 0.0F, false);

        tailend = new AdvancedModelBox(this, "tailend");
        tailend.setPos(0.0F, 0.5F, 16.5F);
        tail2.addChild(tailend);
        tailend.setTextureOffset(0, 50);
        tailend.addBox(-16.0F, -1.0F, -2.5F, 32.0F, 2.0F, 13.0F, 0.0F, false);

        head = new AdvancedModelBox(this, "head");
        head.setPos(0.0F, -5.6667F, -16.9167F);
        body.addChild(head);
        head.setTextureOffset(71, 71);
        head.addBox(-8.0F, -8.0F, -17.0F, 16.0F, 8.0F, 17.0F, 0.0F, false);
        head.setTextureOffset(96, 97);
        head.addBox(-7.0F, 0.0F, -15.0F, 14.0F, 1.0F, 15.0F, 0.0F, false);

        jaw = new AdvancedModelBox(this, "jaw");
        jaw.setPos(0.0F, 1.0F, 0.0F);
        head.addChild(jaw);
        jaw.setTextureOffset(73, 50);
        jaw.addBox(-7.0F, -2.0F, -15.0F, 14.0F, 1.0F, 18.0F, 0.0F, false);
        jaw.setTextureOffset(0, 66);
        jaw.addBox(-8.0F, -1.0F, -16.0F, 16.0F, 6.0F, 19.0F, 0.0F, false);

        this.updateDefaultPose();
        animator = ModelAnimator.create();
    }

    private static double horizontalMag(Vec3d vec) {
        return vec.x * vec.x + vec.z * vec.z;
    }

    public void animate(IAnimatedEntity entity, float f, float f1, float f2, float f3, float f4) {
        this.resetToDefaultPose();
        animator.update(entity);
        animator.setAnimation(EntityOrca.ANIMATION_BITE);
        animator.startKeyframe(5);
        animator.move(body, 0, 0, -5);
        animator.rotate(head, Maths.rad(-25), 0, 0);
        animator.rotate(jaw, Maths.rad(60), 0, 0);
        animator.endKeyframe();
        animator.resetKeyframe(3);
        animator.setAnimation(EntityOrca.ANIMATION_TAILSWING);
        animator.startKeyframe(5);
        animator.move(body, 0, -6, 15);
        animator.rotate(body, Maths.rad(-140), 0, 0);
        animator.rotate(tail1, Maths.rad(-35), 0, 0);
        animator.rotate(tail2, Maths.rad(-35), 0, 0);
        animator.rotate(tailend, Maths.rad(-25), 0, 0);
        animator.endKeyframe();
        animator.setStaticKeyframe(3);
        animator.resetKeyframe(12);

    }

    public void render(MatrixStack matrixStackIn, VertexConsumer bufferIn, int packedLightIn, int packedOverlayIn, float red, float green, float blue, float alpha) {
        if (this.child) {
            matrixStackIn.push();
            matrixStackIn.scale(0.35F, 0.35F, 0.35F);
            matrixStackIn.translate(0.0D, 2.75D, 0.125D);
            parts().forEach((p_228292_8_) -> {
                p_228292_8_.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha);
            });
            matrixStackIn.pop();
        } else {
            matrixStackIn.push();
            parts().forEach((p_228290_8_) -> {
                p_228290_8_.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha);
            });
            matrixStackIn.pop();
        }

    }

    @Override
    public void setAngles(EntityOrca entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.resetToDefaultPose();
        animate(entityIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        float swimSpeed = 0.2F;
        float swimDegree = 0.4F;
        AdvancedModelBox[] tailBoxes = new AdvancedModelBox[]{tail1, tail2, tailend};
        this.walk(body, swimSpeed, swimDegree * 0.2F, true, 0F, 0F, limbSwing, limbSwingAmount);
        this.bob(body, swimSpeed, swimDegree * 5F, false, limbSwing, limbSwingAmount);
        this.chainWave(tailBoxes, swimSpeed, swimDegree, 0.2F, limbSwing, limbSwingAmount);
        this.swing(fin_left, swimSpeed, swimDegree * 0.2F, true, 0F, 0F, limbSwing, limbSwingAmount);
        this.swing(fin_right, swimSpeed, swimDegree * 0.2F, true, 0F, 0F, limbSwing, limbSwingAmount);
        this.flap(fin_left, swimSpeed, swimDegree * 1.4F, true, 3F, 0F, limbSwing, limbSwingAmount);
        this.flap(fin_right, swimSpeed, swimDegree * 1.4F, false, 3F, 0F, limbSwing, limbSwingAmount);
        this.body.rotateAngleX += headPitch * MathHelper.RADIANS_PER_DEGREE;
        this.body.rotateAngleY += netHeadYaw * MathHelper.RADIANS_PER_DEGREE;
        if (horizontalMag(entityIn.getVelocity()) > 1.0E-7D) {
            this.body.rotateAngleX += -0.05F + -0.05F * MathHelper.cos(ageInTicks * 0.3F);
            this.tail1.rotateAngleX += -0.1F * MathHelper.cos(ageInTicks * 0.3F);
            this.tailend.rotateAngleX += -0.2F * MathHelper.cos(ageInTicks * 0.3F);
        }

    }

    @Override
    public Iterable<BasicModelPart> parts() {
        return ImmutableList.of(root);
    }

    @Override
    public Iterable<AdvancedModelBox> getAllParts() {
        return ImmutableList.of(root, body, fintop, head, fin_left, fin_right, jaw, tail1, tail2, tailend);
    }

    public void setRotationAngle(AdvancedModelBox AdvancedModelBox, float x, float y, float z) {
        AdvancedModelBox.rotateAngleX = x;
        AdvancedModelBox.rotateAngleY = y;
        AdvancedModelBox.rotateAngleZ = z;
    }
}