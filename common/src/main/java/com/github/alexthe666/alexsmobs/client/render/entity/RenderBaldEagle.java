package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.entity.EntityBaldEagle;
import com.github.alexthe666.alexsmobs.client.model.ModelBaldEagle;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.jetbrains.annotations.NotNull;

public class RenderBaldEagle extends MobEntityRenderer<EntityBaldEagle, ModelBaldEagle> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/bald_eagle.png");
    private static final Identifier TEXTURE_CAP = new Identifier("alexsmobs:textures/entity/bald_eagle_hood.png");

    public RenderBaldEagle(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelBaldEagle(), 0.3F);
        this.addFeature(new CapLayer(this));
    }

    @Override
    public boolean shouldRender(EntityBaldEagle baldEagle, Frustum p_225626_2_, double p_225626_3_, double p_225626_5_, double p_225626_7_) {
        if( baldEagle.hasVehicle() && baldEagle.getVehicle() instanceof PlayerEntity && MinecraftClient.getInstance().player == baldEagle.getVehicle() && MinecraftClient.getInstance().options.getPerspective() == Perspective.FIRST_PERSON){
            return false;
        }
        return super.shouldRender(baldEagle, p_225626_2_, p_225626_3_, p_225626_5_, p_225626_7_);
    }

    @Override
    protected void scale(EntityBaldEagle eagle, MatrixStack matrixStackIn, float partialTickTime) {
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
                }else if (playerRender instanceof LivingEntityRenderer && ((LivingEntityRenderer) playerRender).getModel() instanceof BipedEntityModel) {
                    if(leftHand){
                        matrixStackIn.translate(-0.3F, -0.7F, 0.5F);
                        ((BipedEntityModel) ((LivingEntityRenderer) playerRender).getModel()).leftArm.rotate(matrixStackIn);
                        matrixStackIn.translate(-0.2F, 0.5F, -0.18F);
                        matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(40F));
                        matrixStackIn.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(70F));
                    }else{
                        matrixStackIn.translate(0.3F, -0.7F, 0.5F);
                        ((BipedEntityModel) ((LivingEntityRenderer) playerRender).getModel()).rightArm.rotate(matrixStackIn);
                        matrixStackIn.translate(0.2F, 0.5F, -0.18F);
                        matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(40F));
                        matrixStackIn.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-70F));
                    }
                }
            }
        }
    }
    
    @NotNull
    @Override
    public Identifier getTexture(EntityBaldEagle entity) {
        return TEXTURE;
    }

    static class CapLayer extends FeatureRenderer<EntityBaldEagle, ModelBaldEagle> {

        public CapLayer(RenderBaldEagle p_i50928_1_) {
            super(p_i50928_1_);
        }

        @Override
        public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntityBaldEagle entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            if (entitylivingbaseIn.hasCap()) {
                var lead = bufferIn.getBuffer(RenderLayer.getEntityTranslucent(TEXTURE_CAP));
                this.getContextModel().render(matrixStackIn, lead, packedLightIn, LivingEntityRenderer.getOverlay(entitylivingbaseIn, 0), 1.0F, 1.0F, 1.0F, 1.0F);
            }
        }
    }
}
