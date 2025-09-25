package com.github.alexthe666.alexsmobs.client.render.item;

import com.github.alexthe666.alexsmobs.client.model.ModelEndPirateAnchor;
import com.github.alexthe666.alexsmobs.client.model.ModelEndPirateAnchorWinch;
import com.github.alexthe666.alexsmobs.client.model.ModelEndPirateShipWheel;
import com.github.alexthe666.alexsmobs.item.IItemRender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public class AMItemstackRenderer extends BuiltinModelItemRenderer {

    private static int ticksExisted = 0;
    private static final ModelEndPirateAnchor ANCHOR_MODEL = new ModelEndPirateAnchor();
    private static final Identifier ANCHOR_TEXTURE = new Identifier("alexsmobs:textures/entity/end_pirate/anchor.png");
    private static final ModelEndPirateAnchorWinch WINCH_MODEL = new ModelEndPirateAnchorWinch();
    private static final Identifier WINCH_TEXTURE = new Identifier("alexsmobs:textures/entity/end_pirate/anchor_winch.png");
    private static final ModelEndPirateShipWheel SHIP_WHEEL_MODEL = new ModelEndPirateShipWheel();
    private static final Identifier SHIP_WHEEL_TEXTURE = new Identifier("alexsmobs:textures/entity/end_pirate/ship_wheel.png");

    public AMItemstackRenderer() {
        super(null, null);
    }

    public static void incrementTick() {
        ticksExisted++;
    }

    public static int getTicks() {
        if (MinecraftClient.getInstance().player == null || MinecraftClient.getInstance().isPaused()) {
            return ticksExisted;
        } else {
            return MinecraftClient.getInstance().player.age;
        }
    }

    @Override
    public void render(ItemStack itemStackIn, ModelTransformationMode transformType, MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int combinedLightIn, int combinedOverlayIn) {

        if (itemStackIn.getItem() instanceof IItemRender itemRender) {
            itemRender.render(itemStackIn, transformType, matrixStackIn, bufferIn, combinedLightIn, combinedOverlayIn);
        }

        //TODO reimplement
        /*
        if(itemStackIn.getItem() == AMBlockRegistry.END_PIRATE_ANCHOR.get().asItem()){
            matrixStackIn.push();
            matrixStackIn.translate(1F, 0F, 0);
            matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-180));
            matrixStackIn.scale(0.75F, 0.75F, 0.75F);
            ANCHOR_MODEL.animateStack(itemStackIn);
            ANCHOR_MODEL.renderToBuffer(matrixStackIn, bufferIn.getBuffer(RenderLayer.getEntityCutoutNoCull(ANCHOR_TEXTURE)), combinedLightIn, combinedOverlayIn, 1.0F, 1.0F, 1.0F, 1.0F);
            matrixStackIn.pop();
        }
        if(itemStackIn.getItem() == AMBlockRegistry.END_PIRATE_ANCHOR_WINCH.get().asItem()){
            matrixStackIn.push();
            matrixStackIn.translate(1, -1F, 0);
            matrixStackIn.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-180));
            WINCH_MODEL.animateStack(itemStackIn);
            WINCH_MODEL.renderToBuffer(matrixStackIn, bufferIn.getBuffer(RenderLayer.getEntityCutoutNoCull(WINCH_TEXTURE)), combinedLightIn, combinedOverlayIn, 1.0F, 1.0F, 1.0F, 1.0F);
            matrixStackIn.pop();
        }
        if(itemStackIn.getItem() == AMBlockRegistry.END_PIRATE_SHIP_WHEEL.get().asItem()){
            matrixStackIn.push();
            matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90));
            matrixStackIn.scale(0.8F, 0.8F, 0.8F);
            SHIP_WHEEL_MODEL.resetToDefaultPose();
            SHIP_WHEEL_MODEL.renderToBuffer(matrixStackIn, bufferIn.getBuffer(RenderLayer.getEntityCutoutNoCull(SHIP_WHEEL_TEXTURE)), combinedLightIn, combinedOverlayIn, 1.0F, 1.0F, 1.0F, 1.0F);
            matrixStackIn.pop();
        }
        */
    }

}
