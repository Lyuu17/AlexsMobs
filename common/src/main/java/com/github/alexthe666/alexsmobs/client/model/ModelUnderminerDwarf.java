package com.github.alexthe666.alexsmobs.client.model;

import com.github.alexthe666.alexsmobs.entity.EntityUnderminer;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.google.common.collect.ImmutableList;
import com.iafenvoy.uranus.client.model.AdvancedEntityModel;
import com.iafenvoy.uranus.client.model.AdvancedModelBox;
import com.iafenvoy.uranus.client.model.basic.BasicModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;

public class ModelUnderminerDwarf extends AdvancedEntityModel<EntityUnderminer> {
    private final AdvancedModelBox body;
    private final AdvancedModelBox head;
    private final AdvancedModelBox helmet;
    private final AdvancedModelBox beard;
    private final AdvancedModelBox leftArm;
    private final AdvancedModelBox rightArm;
    private final AdvancedModelBox leftLeg;
    private final AdvancedModelBox rightLeg;
    public BipedEntityModel.ArmPose leftArmPose = BipedEntityModel.ArmPose.EMPTY;
    public BipedEntityModel.ArmPose rightArmPose = BipedEntityModel.ArmPose.EMPTY;
    public boolean crouching;
    public float swimAmount;

    public ModelUnderminerDwarf() {
        texWidth = 128;
        texHeight = 128;

        body = new AdvancedModelBox(this, "body");
        body.setRotationPoint(0.0F, 12.0F, 0.0F);
        body.setTextureOffset(0, 36);
        body.addBox(-5.0F, -10.0F, -3.0F, 10.0F, 11.0F, 6.0F, 0.0F, false);

        head = new AdvancedModelBox(this, "head");
        head.setRotationPoint(0.0F, -10.02F, 0.0F);
        body.addChild(head);
        head.setTextureOffset(30, 24);
        head.addBox(-5.0F, -8.0F, -5.0F, 10.0F, 8.0F, 9.0F, 0.0F, false);
        head.setTextureOffset(0, 15);
        head.addBox(-5.0F, -8.0F, -5.0F, 10.0F, 8.0F, 9.0F, 0.1F, false);

        helmet = new AdvancedModelBox(this, "helmet");
        helmet.setRotationPoint(0.0F, 0.0F, 0.0F);
        head.addChild(helmet);
        helmet.setTextureOffset(0, 0);
        helmet.addBox(-6.0F, -10.0F, -5.5F, 12.0F, 4.0F, 10.0F, 0.1F, false);

        beard = new AdvancedModelBox(this, "beard");
        beard.setRotationPoint(0.0F, 0.1F, -4.1F);
        head.addChild(beard);
        beard.setTextureOffset(0, 54);
        beard.addBox(-5.0F, 0.0F, -1.0F, 10.0F, 9.0F, 2.0F, 0.0F, false);

        leftArm = new AdvancedModelBox(this, "leftArm");
        leftArm.setRotationPoint(7.0F, -9.0F, 0.0F);
        body.addChild(leftArm);
        leftArm.setTextureOffset(45, 0);
        leftArm.addBox(-2.0F, -1.0F, -2.5F, 4.0F, 13.0F, 5.0F, 0.0F, false);

        rightArm = new AdvancedModelBox(this, "rightArm");
        rightArm.setRotationPoint(-7.0F, -9.0F, 0.0F);
        body.addChild(rightArm);
        rightArm.setTextureOffset(45, 0);
        rightArm.addBox(-2.0F, -1.0F, -2.5F, 4.0F, 13.0F, 5.0F, 0.0F, true);

        leftLeg = new AdvancedModelBox(this, "leftLeg");
        leftLeg.setRotationPoint(2.0F, 2.0F, 0.0F);
        body.addChild(leftLeg);
        leftLeg.setTextureOffset(33, 42);
        leftLeg.addBox(-2.0F, -1.0F, -3.0F, 5.0F, 11.0F, 6.0F, 0.0F, false);

        rightLeg = new AdvancedModelBox(this, "rightLeg");
        rightLeg.setRotationPoint(-2.0F, 2.0F, 0.0F);
        body.addChild(rightLeg);
        rightLeg.setTextureOffset(33, 42);
        rightLeg.addBox(-3.0F, -1.0F, -3.0F, 5.0F, 11.0F, 6.0F, 0.0F, true);

        this.updateDefaultPose();
    }

    @Override
    public Iterable<BasicModelPart> parts() {
        return ImmutableList.of(body);
    }

    @Override
    public void setAngles(EntityUnderminer entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.resetToDefaultPose();
        setupHumanoidAnims(entityIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
    }

    @Override
    public Iterable<AdvancedModelBox> getAllParts() {
        return ImmutableList.of(body, head, beard, helmet, rightLeg, leftLeg, rightArm, leftArm);
    }

    /*
        From here on out copied from BipedEntityModel
     */
    public void setupHumanoidAnims(EntityUnderminer entityIn, float p_102867_, float p_102868_, float p_102869_, float p_102870_, float p_102871_) {
        boolean flag = entityIn.getRoll() > 4;
        boolean flag1 = entityIn.isInSwimmingPose();
        this.head.rotateAngleY = p_102870_ * MathHelper.RADIANS_PER_DEGREE;
        if (flag) {
            this.head.rotateAngleX = (-Maths.QUARTER_PI);
        } else if (this.swimAmount > 0.0F) {
            if (flag1) {
                this.head.rotateAngleX = this.rotlerpRad(this.swimAmount, this.head.rotateAngleX, -Maths.QUARTER_PI);
            } else {
                this.head.rotateAngleX = this.rotlerpRad(this.swimAmount, this.head.rotateAngleX, p_102871_ * MathHelper.RADIANS_PER_DEGREE);
            }
        } else {
            this.head.rotateAngleX = p_102871_ * MathHelper.RADIANS_PER_DEGREE;
        }
        float f = 1.0F;
        if (flag) {
            f = (float)entityIn.getVelocity().lengthSquared();
            f /= 0.2F;
            f *= f * f;
        }

        if (f < 1.0F) {
            f = 1.0F;
        }

        this.rightArm.rotateAngleX = MathHelper.cos(p_102867_ * 0.6662F + MathHelper.PI) * 2.0F * p_102868_ * 0.5F / f;
        this.leftArm.rotateAngleX = MathHelper.cos(p_102867_ * 0.6662F) * 2.0F * p_102868_ * 0.5F / f;
        this.rightArm.rotateAngleZ = 0.0F;
        this.leftArm.rotateAngleZ = 0.0F;
        this.rightLeg.rotateAngleX = MathHelper.cos(p_102867_ * 0.6662F) * 1.4F * p_102868_ / f;
        this.leftLeg.rotateAngleX = MathHelper.cos(p_102867_ * 0.6662F + MathHelper.PI) * 1.4F * p_102868_ / f;
        this.rightLeg.rotateAngleY = 0.0F;
        this.leftLeg.rotateAngleY = 0.0F;
        this.rightLeg.rotateAngleZ = 0.0F;
        this.leftLeg.rotateAngleZ = 0.0F;
        if (this.riding) {
            this.rightArm.rotateAngleX += (-(float)Math.PI / 5F);
            this.leftArm.rotateAngleX += (-(float)Math.PI / 5F);
            this.rightLeg.rotateAngleX = -1.4137167F;
            this.rightLeg.rotateAngleY = ((float)Math.PI / 10F);
            this.rightLeg.rotateAngleZ = 0.07853982F;
            this.leftLeg.rotateAngleX = -1.4137167F;
            this.leftLeg.rotateAngleY = (-(float)Math.PI / 10F);
            this.leftLeg.rotateAngleZ = -0.07853982F;
        }

        this.rightArm.rotateAngleY = 0.0F;
        this.leftArm.rotateAngleY = 0.0F;
        boolean flag2 = entityIn.getMainArm() == Arm.RIGHT;
        if (entityIn.isUsingItem()) {
            boolean flag3 = entityIn.getActiveHand() == Hand.MAIN_HAND;
            if (flag3 == flag2) {
                this.poseRightArm(entityIn);
            } else {
                this.poseLeftArm(entityIn);
            }
        } else {
            boolean flag4 = flag2 ? this.leftArmPose.isTwoHanded() : this.rightArmPose.isTwoHanded();
            if (flag2 != flag4) {
                this.poseLeftArm(entityIn);
                this.poseRightArm(entityIn);
            } else {
                this.poseRightArm(entityIn);
                this.poseLeftArm(entityIn);
            }
        }

        this.setupAttackAnimation(entityIn, p_102869_);
        if (this.crouching) {
            this.body.rotateAngleX = 0.5F;
            this.rightArm.rotateAngleX += 0.4F;
            this.leftArm.rotateAngleX += 0.4F;
        }

        if (this.rightArmPose != BipedEntityModel.ArmPose.SPYGLASS) {
            this.rightArm.rotateAngleZ += 1.0F * (MathHelper.cos(p_102869_ * 0.09F) * 0.05F + 0.05F);
            this.rightArm.rotateAngleX += 1.0F * MathHelper.sin(p_102869_ * 0.067F) * 0.05F;
        }

        if (this.leftArmPose != BipedEntityModel.ArmPose.SPYGLASS) {
            this.leftArm.rotateAngleZ += -1.0F * (MathHelper.cos(p_102869_ * 0.09F) * 0.05F + 0.05F);
            this.leftArm.rotateAngleX += -1.0F * MathHelper.sin(p_102869_ * 0.067F) * 0.05F;
        }

        if (this.swimAmount > 0.0F) {
            final float f5 = p_102867_ % 26.0F;
            var humanoidarm = this.getAttackArm(entityIn);
            final float f1 = humanoidarm == Arm.RIGHT && this.handSwingProgress > 0.0F ? 0.0F : this.swimAmount;
            final float f2 = humanoidarm == Arm.LEFT && this.handSwingProgress > 0.0F ? 0.0F : this.swimAmount;
            if (!entityIn.isUsingItem()) {
                if (f5 < 14.0F) {
                    this.leftArm.rotateAngleX = this.rotlerpRad(f2, this.leftArm.rotateAngleX, 0.0F);
                    this.rightArm.rotateAngleX = MathHelper.lerp(f1, this.rightArm.rotateAngleX, 0.0F);
                    this.leftArm.rotateAngleY = this.rotlerpRad(f2, this.leftArm.rotateAngleY, MathHelper.PI);
                    this.rightArm.rotateAngleY = MathHelper.lerp(f1, this.rightArm.rotateAngleY, MathHelper.PI);
                    this.leftArm.rotateAngleZ = this.rotlerpRad(f2, this.leftArm.rotateAngleZ, MathHelper.PI + 1.8707964F * this.quadraticArmUpdate(f5) / this.quadraticArmUpdate(14.0F));
                    this.rightArm.rotateAngleZ = MathHelper.lerp(f1, this.rightArm.rotateAngleZ, MathHelper.PI - 1.8707964F * this.quadraticArmUpdate(f5) / this.quadraticArmUpdate(14.0F));
                } else if (f5 >= 14.0F && f5 < 22.0F) {
                    float f6 = (f5 - 14.0F) / 8.0F;
                    this.leftArm.rotateAngleX = this.rotlerpRad(f2, this.leftArm.rotateAngleX, MathHelper.HALF_PI * f6);
                    this.rightArm.rotateAngleX = MathHelper.lerp(f1, this.rightArm.rotateAngleX, MathHelper.HALF_PI * f6);
                    this.leftArm.rotateAngleY = this.rotlerpRad(f2, this.leftArm.rotateAngleY, MathHelper.PI);
                    this.rightArm.rotateAngleY = MathHelper.lerp(f1, this.rightArm.rotateAngleY, MathHelper.PI);
                    this.leftArm.rotateAngleZ = this.rotlerpRad(f2, this.leftArm.rotateAngleZ, 5.012389F - 1.8707964F * f6);
                    this.rightArm.rotateAngleZ = MathHelper.lerp(f1, this.rightArm.rotateAngleZ, 1.2707963F + 1.8707964F * f6);
                } else if (f5 >= 22.0F && f5 < 26.0F) {
                    float f3 = (f5 - 22.0F) / 4.0F;
                    this.leftArm.rotateAngleX = this.rotlerpRad(f2, this.leftArm.rotateAngleX, MathHelper.HALF_PI - MathHelper.HALF_PI * f3);
                    this.rightArm.rotateAngleX = MathHelper.lerp(f1, this.rightArm.rotateAngleX, MathHelper.HALF_PI - MathHelper.HALF_PI * f3);
                    this.leftArm.rotateAngleY = this.rotlerpRad(f2, this.leftArm.rotateAngleY, MathHelper.PI);
                    this.rightArm.rotateAngleY = MathHelper.lerp(f1, this.rightArm.rotateAngleY, MathHelper.PI);
                    this.leftArm.rotateAngleZ = this.rotlerpRad(f2, this.leftArm.rotateAngleZ, MathHelper.PI);
                    this.rightArm.rotateAngleZ = MathHelper.lerp(f1, this.rightArm.rotateAngleZ, MathHelper.PI);
                }
            }

            this.leftLeg.rotateAngleX = MathHelper.lerp(this.swimAmount, this.leftLeg.rotateAngleX, 0.3F * MathHelper.cos(p_102867_ * 0.33333334F + MathHelper.PI));
            this.rightLeg.rotateAngleX = MathHelper.lerp(this.swimAmount, this.rightLeg.rotateAngleX, 0.3F * MathHelper.cos(p_102867_ * 0.33333334F));
        }
    }

    private void poseRightArm(EntityUnderminer p_102876_) {
        switch (this.rightArmPose) {
            case EMPTY:
                this.rightArm.rotateAngleY = 0.0F;
                break;
            case BLOCK:
                this.rightArm.rotateAngleX = this.rightArm.rotateAngleX * 0.5F - 0.9424779F;
                this.rightArm.rotateAngleY = (-(float)Math.PI / 6F);
                break;
            case ITEM:
                this.rightArm.rotateAngleX = this.rightArm.rotateAngleX * 0.5F - ((float)Math.PI / 10F);
                this.rightArm.rotateAngleY = 0.0F;
                break;
            case THROW_SPEAR:
                this.rightArm.rotateAngleX = this.rightArm.rotateAngleX * 0.5F - MathHelper.PI;
                this.rightArm.rotateAngleY = 0.0F;
                break;
            case BOW_AND_ARROW:
                this.rightArm.rotateAngleY = -0.1F + this.head.rotateAngleY;
                this.leftArm.rotateAngleY = 0.1F + this.head.rotateAngleY + 0.4F;
                this.rightArm.rotateAngleX = (-MathHelper.HALF_PI) + this.head.rotateAngleX;
                this.leftArm.rotateAngleX = (-MathHelper.HALF_PI) + this.head.rotateAngleX;
                break;
            case CROSSBOW_CHARGE: // fallthrough
            case CROSSBOW_HOLD:
                break;
            case SPYGLASS:
                this.rightArm.rotateAngleX = MathHelper.clamp(this.head.rotateAngleX - 1.9198622F - (p_102876_.isInSneakingPose() ? 0.2617994F : 0.0F), -2.4F, 3.3F);
                this.rightArm.rotateAngleY = this.head.rotateAngleY - 0.2617994F;
                break;
            case TOOT_HORN:
                this.rightArm.rotateAngleX = MathHelper.clamp(this.head.rotateAngleX, -1.2F, 1.2F) - 1.4835298F;
                this.rightArm.rotateAngleY = this.head.rotateAngleY - ((float)Math.PI / 6F);
        }

    }

    private void poseLeftArm(EntityUnderminer p_102879_) {
        switch (this.leftArmPose) {
            case EMPTY:
                this.leftArm.rotateAngleY = 0.0F;
                break;
            case BLOCK:
                this.leftArm.rotateAngleX = this.leftArm.rotateAngleX * 0.5F - 0.9424779F;
                this.leftArm.rotateAngleY = ((float)Math.PI / 6F);
                break;
            case ITEM:
                this.leftArm.rotateAngleX = this.leftArm.rotateAngleX * 0.5F - ((float)Math.PI / 10F);
                this.leftArm.rotateAngleY = 0.0F;
                break;
            case THROW_SPEAR:
                this.leftArm.rotateAngleX = this.leftArm.rotateAngleX * 0.5F - MathHelper.PI;
                this.leftArm.rotateAngleY = 0.0F;
                break;
            case BOW_AND_ARROW:
                this.rightArm.rotateAngleY = -0.1F + this.head.rotateAngleY - 0.4F;
                this.leftArm.rotateAngleY = 0.1F + this.head.rotateAngleY;
                this.rightArm.rotateAngleX = (-MathHelper.HALF_PI) + this.head.rotateAngleX;
                this.leftArm.rotateAngleX = (-MathHelper.HALF_PI) + this.head.rotateAngleX;
                break;
            case CROSSBOW_CHARGE:
                break;
            case CROSSBOW_HOLD:
                break;
            case SPYGLASS:
                this.leftArm.rotateAngleX = MathHelper.clamp(this.head.rotateAngleX - 1.9198622F - (p_102879_.isInSneakingPose() ? 0.2617994F : 0.0F), -2.4F, 3.3F);
                this.leftArm.rotateAngleY = this.head.rotateAngleY + 0.2617994F;
                break;
            case TOOT_HORN:
                this.leftArm.rotateAngleX = MathHelper.clamp(this.head.rotateAngleX, -1.2F, 1.2F) - 1.4835298F;
                this.leftArm.rotateAngleY = this.head.rotateAngleY + ((float)Math.PI / 6F);
        }

    }

    protected void setupAttackAnimation(EntityUnderminer p_102858_, float p_102859_) {
        if (!(this.handSwingProgress <= 0.0F)) {
            var humanoidarm = this.getAttackArm(p_102858_);
            AdvancedModelBox modelpart = this.getArm(humanoidarm);
            float f = this.handSwingProgress;
            this.body.rotateAngleY = MathHelper.sin(MathHelper.sqrt(f) * MathHelper.TAU) * 0.2F;
            if (humanoidarm == Arm.LEFT) {
                this.body.rotateAngleY *= -1.0F;
            }
            this.head.rotateAngleY -= this.body.rotateAngleY;
            this.leftLeg.rotateAngleY -= this.body.rotateAngleY;
            this.rightLeg.rotateAngleY -= this.body.rotateAngleY;
            this.leftArm.rotateAngleX += this.body.rotateAngleY;
            f = 1.0F - this.handSwingProgress;
            f *= f;
            f *= f;
            f = 1.0F - f;
            final float f1 = MathHelper.sin(f * MathHelper.PI);
            final float f2 = MathHelper.sin(this.handSwingProgress * MathHelper.PI) * -(this.head.rotateAngleX - 0.7F) * 0.75F;
            modelpart.rotateAngleX -= f1 * 1.2F + f2;
            modelpart.rotateAngleY += this.body.rotateAngleY * 2.0F;
            modelpart.rotateAngleZ += MathHelper.sin(this.handSwingProgress * MathHelper.PI) * -0.4F;
        }
    }

    protected float rotlerpRad(float p_102836_, float p_102837_, float p_102838_) {
        float f = (p_102838_ - p_102837_) % MathHelper.TAU;
        if (f < -MathHelper.PI) {
            f += MathHelper.TAU;
        }

        if (f >= MathHelper.PI) {
            f -= MathHelper.TAU;
        }

        return p_102837_ + p_102836_ * f;
    }

    private float quadraticArmUpdate(float p_102834_) {
        return -65.0F * p_102834_ + p_102834_ * p_102834_;
    }

    public void setArmAngle(Arm p_102854_, MatrixStack p_102855_) {
        this.getArm(p_102854_).translateAndRotate(p_102855_);
    }

    protected AdvancedModelBox getArm(Arm p_102852_) {
        return p_102852_ == Arm.LEFT ? this.leftArm : this.rightArm;
    }

    public AdvancedModelBox getHead() {
        return this.head;
    }

    private Arm getAttackArm(EntityUnderminer p_102857_) {
        var humanoidarm = p_102857_.getMainArm();
        return p_102857_.preferredHand == Hand.MAIN_HAND ? humanoidarm : humanoidarm.getOpposite();
    }
}