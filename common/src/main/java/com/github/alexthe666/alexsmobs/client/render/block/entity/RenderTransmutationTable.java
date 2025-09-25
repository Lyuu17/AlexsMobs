package com.github.alexthe666.alexsmobs.client.render.block.entity;

import com.github.alexthe666.alexsmobs.block.TransmutationTableBlock;
import com.github.alexthe666.alexsmobs.block.entity.TransmutationTableBlockEntity;
import com.github.alexthe666.alexsmobs.client.model.ModelTransmutationTable;
import com.github.alexthe666.alexsmobs.client.render.entity.AMRenderLayers;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class RenderTransmutationTable<T extends TransmutationTableBlockEntity> implements BlockEntityRenderer<T> {

    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/farseer/transmutation_table.png");
    private static final Identifier OVERLAY = new Identifier("alexsmobs:textures/entity/farseer/transmutation_table_overlay.png");
    private static final Identifier GLOW_TEXTURE = new Identifier("alexsmobs:textures/entity/farseer/transmutation_table_glow.png");
    private static final ModelTransmutationTable MODEL = new ModelTransmutationTable(0F);
    private static final ModelTransmutationTable OVERLAY_MODEL = new ModelTransmutationTable(0.01F);

    public RenderTransmutationTable(BlockEntityRendererFactory.Context rendererDispatcherIn) {
    }

    @Override
    public void render(T tileEntityIn, float partialTicks, MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int combinedLightIn, int combinedOverlayIn) {
        matrixStackIn.push();
        var dir = tileEntityIn.getCachedState().get(TransmutationTableBlock.FACING);
        switch (dir) {
            case NORTH -> matrixStackIn.translate(0.5, 1.5F, 0.5F);
            case EAST -> matrixStackIn.translate(0.5F, 1.5F, 0.5F);
            case SOUTH -> matrixStackIn.translate(0.5, 1.5F, 0.5F);
            case WEST -> matrixStackIn.translate(0.5F, 1.5F, 0.5F);
        }
        float ageInTicks = partialTicks + tileEntityIn.ticksExisted;
        
        matrixStackIn.multiply(dir.getOpposite().getRotationQuaternion());
        matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
        matrixStackIn.push();
        MODEL.animate(tileEntityIn, partialTicks);
        MODEL.render(matrixStackIn, bufferIn.getBuffer(RenderLayer.getEntityTranslucent(TEXTURE)), combinedLightIn, combinedOverlayIn, 1, 1, 1, 1);
        MODEL.render(matrixStackIn, bufferIn.getBuffer(AMRenderLayers.getEyesAlphaEnabled(GLOW_TEXTURE)), 240, combinedOverlayIn, 1, 1, 1, 0.5F + (float)Math.sin(ageInTicks * 0.05F) * 0.25F);
        var staticyOverlay = AMRenderLayers.createMergedVertexConsumer(bufferIn.getBuffer(AMRenderLayers.STATIC_PORTAL), bufferIn.getBuffer(RenderLayer.getEntityCutoutNoCull(OVERLAY)));
        OVERLAY_MODEL.animate(tileEntityIn, partialTicks);
        OVERLAY_MODEL.render(matrixStackIn, staticyOverlay, combinedLightIn, OverlayTexture.DEFAULT_UV, 1, 1, 1, 1);
        matrixStackIn.pop();
        matrixStackIn.pop();
    }


    private static void vertex(VertexConsumer p_114090_, Matrix4f p_114091_, Matrix3f p_114092_, int p_114093_, float p_114094_, float p_114095_, int p_114096_, int p_114097_) {
        p_114090_.vertex(p_114091_, p_114094_, p_114095_, 0.0F).color(255, 255, 255, 100).texture((float) p_114096_, (float) p_114097_).overlay(OverlayTexture.DEFAULT_UV).light(p_114093_).normal(p_114092_, 0.0F, 1.0F, 0.0F).next();
    }

}
