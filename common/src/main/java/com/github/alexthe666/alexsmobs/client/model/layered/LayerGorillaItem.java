package com.github.alexthe666.alexsmobs.client.model.layered;

import com.github.alexthe666.alexsmobs.client.model.ModelGorilla;
import com.github.alexthe666.alexsmobs.client.render.entity.RenderGorilla;
import com.github.alexthe666.alexsmobs.entity.EntityGorilla;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;

public class LayerGorillaItem extends FeatureRenderer<EntityGorilla, ModelGorilla> {

    public LayerGorillaItem(RenderGorilla render) {
        super(render);
    }

    @Override
    public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntityGorilla entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        ItemStack itemstack = entitylivingbaseIn.getEquippedStack(EquipmentSlot.MAINHAND);
        String name = entitylivingbaseIn.getName().getString().toLowerCase();
        var renderer = MinecraftClient.getInstance().getEntityRenderDispatcher().getHeldItemRenderer();
        if(name.contains("harambe")){
            ItemStack haloStack = new ItemStack(AMItemRegistry.HALO.get());
            matrixStackIn.push();
            this.getContextModel().root.translateAndRotate(matrixStackIn);
            this.getContextModel().body.translateAndRotate(matrixStackIn);
            this.getContextModel().chest.translateAndRotate(matrixStackIn);
            this.getContextModel().head.translateAndRotate(matrixStackIn);
            float f = 0.1F * (float) Math.sin((entitylivingbaseIn.age + partialTicks) * 0.1F) + (entitylivingbaseIn.isBaby() ? 0.2F : 0F);
            matrixStackIn.translate(0.0F, -0.7F - f, -0.2F);
            matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90F));
            matrixStackIn.scale(1.3F, 1.3F, 1.3F);
            renderer.renderItem(entitylivingbaseIn, haloStack, ModelTransformationMode.GROUND, false, matrixStackIn, bufferIn, packedLightIn);
            matrixStackIn.pop();
        }
        matrixStackIn.push();
        if(entitylivingbaseIn.isBaby()){
            matrixStackIn.scale(0.35F, 0.35F, 0.35F);
            matrixStackIn.translate(-0.1D, 2D, -1.15D);
            translateToHand(false, matrixStackIn);
            matrixStackIn.translate(-0.4F, 0.75F, -0.0F);
            matrixStackIn.scale(2.8F, 2.8F, 2.8F);
        }else{
            translateToHand(false, matrixStackIn);
            matrixStackIn.translate(-0.4F, 0.75F, -0.0F);
        }
        matrixStackIn.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-2.5F));
        matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90F));
        if(itemstack.getItem() instanceof BlockItem){
            matrixStackIn.scale(2, 2, 2);
        }
        renderer.renderItem(entitylivingbaseIn, itemstack, ModelTransformationMode.GROUND, false, matrixStackIn, bufferIn, packedLightIn);
        matrixStackIn.pop();
    }

    protected void translateToHand(boolean left, MatrixStack matrixStack) {
        this.getContextModel().root.translateAndRotate(matrixStack);
        this.getContextModel().body.translateAndRotate(matrixStack);
        this.getContextModel().chest.translateAndRotate(matrixStack);
        this.getContextModel().leftArm.translateAndRotate(matrixStack);
    }
}
