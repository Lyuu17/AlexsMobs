package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.client.model.ModelPotoo;
import com.github.alexthe666.alexsmobs.entity.EntityPotoo;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public class RenderPotoo extends MobEntityRenderer<EntityPotoo, ModelPotoo> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/potoo.png");

    public RenderPotoo(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelPotoo(), 0.35F);
    }

    @Override
    public boolean shouldRender(EntityPotoo bird, Frustum p_225626_2_, double p_225626_3_, double p_225626_5_, double p_225626_7_) {
        if( bird.hasVehicle() && bird.getVehicle() instanceof PlayerEntity && MinecraftClient.getInstance().player == bird.getVehicle() && MinecraftClient.getInstance().options.getPerspective() == Perspective.FIRST_PERSON){
            return false;
        }
        return super.shouldRender(bird, p_225626_2_, p_225626_3_, p_225626_5_, p_225626_7_);
    }

    @Override
    protected void scale(EntityPotoo eagle, MatrixStack matrixStackIn, float partialTickTime) {
        if(eagle.hasVehicle() && eagle.getVehicle() != null) {
            if (eagle.getVehicle() instanceof PlayerEntity mount) {
                boolean leftHand = false;
                if(mount.getStackInHand(Hand.MAIN_HAND).getItem() == AMItemRegistry.FALCONRY_GLOVE.get()){
                    leftHand = mount.getMainArm() == Arm.LEFT;
                }else if(mount.getStackInHand(Hand.OFF_HAND).getItem() == AMItemRegistry.FALCONRY_GLOVE.get()){
                    leftHand = mount.getMainArm() != Arm.LEFT;
                }
                var playerRender = MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(mount);
                if(MinecraftClient.getInstance().player == mount && MinecraftClient.getInstance().options.getPerspective() == Perspective.FIRST_PERSON){
                    //handled via event
                }else if (playerRender instanceof LivingEntityRenderer && ((LivingEntityRenderer) playerRender).getModel() instanceof BipedEntityModel<?>) {
                    if(leftHand){
                        matrixStackIn.translate(-0.3F, -0.7F, 0.5F);
                        ((BipedEntityModel) ((LivingEntityRenderer) playerRender).getModel()).leftArm.rotate(matrixStackIn);
                        matrixStackIn.translate(-0.1F, 0.6F, -0.1F);
                        matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(55F));
                        matrixStackIn.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(70F));
                    }else{
                        matrixStackIn.translate(0.3F, -0.7F, 0.5F);
                        ((BipedEntityModel) ((LivingEntityRenderer) playerRender).getModel()).rightArm.rotate(matrixStackIn);
                        matrixStackIn.translate(0.1F, 0.6F, -0.1F);
                        matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(55F));
                        matrixStackIn.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-70F));
                    }
                }
            }
        }
    }
    
    @Override
    public Identifier getTexture(EntityPotoo entity) {
        return TEXTURE;
    }
}
