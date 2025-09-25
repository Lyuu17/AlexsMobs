package com.github.alexthe666.alexsmobs.client.render.block.entity;


import com.github.alexthe666.alexsmobs.block.entity.CapsidBlockEntity;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.joml.Quaternionf;

import java.util.Random;

public class RenderCapsid<T extends CapsidBlockEntity> implements BlockEntityRenderer<T> {

    private final Random random = new Random();
    public RenderCapsid(BlockEntityRendererFactory.Context rendererDispatcherIn) {
    }

    protected int getModelCount(ItemStack stack) {
        int i = 1;
        if (stack.getCount() > 48) {
            i = 5;
        } else if (stack.getCount() > 32) {
            i = 4;
        } else if (stack.getCount() > 16) {
            i = 3;
        } else if (stack.getCount() > 1) {
            i = 2;
        }

        return i;
    }

    @Override
    public void render(T entity, float partialTicks, MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int combinedLightIn, int combinedOverlayIn) {
        ItemStack stack = entity.getStack(0);
        if (!stack.isEmpty()) {
            int i = Item.getRawId(stack.getItem()) + stack.getDamage();
            this.random.setSeed(i);
            float floatProgress = entity.prevFloatUpProgress + (entity.floatUpProgress - entity.prevFloatUpProgress) * partialTicks;
            float yaw = entity.prevYawSwitchProgress + (entity.yawSwitchProgress - entity.prevYawSwitchProgress) * partialTicks;
            int j = this.getModelCount(stack);
            matrixStackIn.push();
            matrixStackIn.translate(0.5F, 0.5F + floatProgress, 0.5F);
            matrixStackIn.multiply((new Quaternionf()).rotateY(Maths.rad(entity.getBlockAngle() + yaw)));
            matrixStackIn.push();
            matrixStackIn.translate(0, -0.1F, 0);
            if(entity.vibratingThisTick && entity.getWorld() != null){
                float vibrate = 0.05F;
                matrixStackIn.translate((entity.getWorld().random.nextFloat() - 0.5F)* vibrate, (entity.getWorld().random.nextFloat() - 0.5F) * vibrate, (entity.getWorld().random.nextFloat() - 0.5F)* vibrate);
            }
            matrixStackIn.scale(1.3F, 1.3F, 1.3F);
            var ibakedmodel = MinecraftClient.getInstance().getItemRenderer().getModel(stack, entity.getWorld(), null, 0);
            boolean flag = ibakedmodel.hasDepth();
            if (!flag) {
                float f7 = -0.0F * (float)(j - 1) * 0.5F;
                float f8 = -0.0F * (float)(j - 1) * 0.5F;
                float f9 = -0.09375F * (float)(j - 1) * 0.5F;
                matrixStackIn.translate(f7, f8, (double)f9);
            }

            for(int k = 0; k < j; ++k) {
                matrixStackIn.push();
                if (k > 0) {
                    if (flag) {
                        float f11 = (this.random.nextFloat() * 2.0F - 1.0F) * 0.15F;
                        float f13 = (this.random.nextFloat() * 2.0F - 1.0F) * 0.15F;
                        float f10 = (this.random.nextFloat() * 2.0F - 1.0F) * 0.15F;
                        matrixStackIn.translate(f11, f13, f10);
                    } else {
                        float f12 = (this.random.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
                        float f14 = (this.random.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
                        matrixStackIn.translate(f12, f14, 0.0D);
                    }
                }

                MinecraftClient.getInstance().getItemRenderer().renderItem(stack, ModelTransformationMode.GROUND, false, matrixStackIn, bufferIn, combinedLightIn, OverlayTexture.DEFAULT_UV, ibakedmodel);
                matrixStackIn.pop();
                if (!flag) {
                    matrixStackIn.translate(0.0, 0.0, 0.09375F);
                }
            }


            matrixStackIn.pop();
            matrixStackIn.pop();
        }

    }
}
