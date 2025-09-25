package com.github.alexthe666.alexsmobs.client.model;

import com.github.alexthe666.alexsmobs.client.render.item.AMItemstackRenderer;
import com.google.common.collect.ImmutableList;
import com.iafenvoy.uranus.client.model.AdvancedEntityModel;
import com.iafenvoy.uranus.client.model.AdvancedModelBox;
import com.iafenvoy.uranus.client.model.basic.BasicModelPart;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

public class ModelMysteriousWorm extends AdvancedEntityModel<Entity> {
    private final AdvancedModelBox root;
    private final AdvancedModelBox head;
    private final AdvancedModelBox body1;
    private final AdvancedModelBox body2;
    private final AdvancedModelBox body3;

    public ModelMysteriousWorm() {
        texWidth = 32;
        texHeight = 32;

        root = new AdvancedModelBox(this, "root");
        root.setPos(0.0F, 24.0F, 0.0F);

        head = new AdvancedModelBox(this, "head");
        head.setPos(0.0F, -2.0F, -6.0F);
        root.addChild(head);
        head.setTextureOffset(14, 0);
        head.addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 2.0F, 0.0F, false);
        head.setTextureOffset(0, 19);
        head.addBox(-1.0F, -1.0F, -4.0F, 2.0F, 2.0F, 2.0F, 0.0F, false);

        body1 = new AdvancedModelBox(this, "body1");
        body1.setPos(0.0F, 0.0F, 0.0F);
        head.addChild(body1);
        body1.setTextureOffset(0, 11);
        body1.addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 5.0F, 0.0F, false);

        body2 = new AdvancedModelBox(this, "body2");
        body2.setPos(0.0F, 0.0F, 5.0F);
        body1.addChild(body2);
        body2.setTextureOffset(10, 14);
        body2.addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 5.0F, 0.1F, false);

        body3 = new AdvancedModelBox(this, "body3");
        body3.setPos(0.0F, 0.0F, 5.0F);
        body2.addChild(body3);
        body3.setTextureOffset(0, 0);
        body3.addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 7.0F, 0.0F, false);
        this.updateDefaultPose();
    }

    @Override
    public Iterable<BasicModelPart> parts() {
        return ImmutableList.of(root);
    }

    @Override
    public Iterable<AdvancedModelBox> getAllParts() {
        return ImmutableList.of(root, head, body1, body2, body3);
    }

    @Override
    public void setAngles(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch){
        this.resetToDefaultPose();
    }

    @Override
    public void render(MatrixStack matrixStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha){
        root.render(matrixStack, buffer, packedLight, packedOverlay);
    }

    public void setRotationAngle(AdvancedModelBox AdvancedModelBox, float x, float y, float z) {
        AdvancedModelBox.rotateAngleX = x;
        AdvancedModelBox.rotateAngleY = y;
        AdvancedModelBox.rotateAngleZ = z;
    }

    public void animateStack(ItemStack itemStackIn) {
        this.resetToDefaultPose();
        float partialTick = MinecraftClient.getInstance().getTickDelta();
        float tick = MinecraftClient.getInstance().player == null ? 0 : partialTick + MinecraftClient.getInstance().player.age;
        if(MinecraftClient.getInstance().isPaused()){
            tick = AMItemstackRenderer.getTicks();
        }
        AdvancedModelBox[] tail = new AdvancedModelBox[]{head, body1, body2, body3};
        this.chainSwing(tail, 0.7F, 0.2F, -3, tick, 1);
        this.chainFlap(tail, 0.7F, 0.2F, -3, tick, 1);
        this.chainWave(tail, 0.7F, 0.2F, -3, tick, MathHelper.clamp((float)(1.0F + Math.sin(tick * 0.04)), 0, 0.5F) * 2);

    }
}